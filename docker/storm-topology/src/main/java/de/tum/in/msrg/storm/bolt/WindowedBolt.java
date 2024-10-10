package de.tum.in.msrg.storm.bolt;

import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.storm.window.OneMinuteWindow;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WindowedBolt extends BaseRichBolt {

    private Map<String, OneMinuteWindow> pageWindowMap;
    private OutputCollector collector;

    public static final Long WINDOW_LENGTH  = 60000L;

    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        pageWindowMap = new ConcurrentHashMap<String, OneMinuteWindow>();
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        String page = input.getStringByField("page");
        Long ts = input.getLongByField("eventTimestamp");
        ClickUpdateEvent event = (ClickUpdateEvent) input.getValueByField("clickUpdateEvent");

        OneMinuteWindow window = pageWindowMap.get(page);

        if (window == null){
            // add new window
            Date winStartTS = new Date(ts);
            winStartTS = DateUtils.truncate(winStartTS, Calendar.MINUTE);
            window = new OneMinuteWindow(winStartTS.getTime());
            window.getTuples().add(input);
            pageWindowMap.put(page, window);
        } else {
            if (ts <= window.getWinEnd() && ts >= window.getWinStart()){
                // tuple in the window
                window.getTuples().add(input);
            } else if (ts < window.getWinStart()) {
                // emit to late stream
                collector.emit("lateEvents", input, new Values(event));
                collector.ack(input);
            } else if (ts > window.getWinEnd()) {
                // process and emit previous window AND create a new window
                PageStatistics statistics = processWindowTuples(page, window);
                collector.emit(window.getTuples(), new Values(page, statistics));
                window.getTuples().forEach(tuple -> collector.ack(tuple));

                Date winStartTS = new Date(ts);
                winStartTS = DateUtils.truncate(winStartTS, Calendar.MINUTE);
                window = new OneMinuteWindow(winStartTS.getTime());
                window.getTuples().add(input);
                pageWindowMap.put(page, window);
            }
        }


    }

    private PageStatistics processWindowTuples(String key, OneMinuteWindow window) {
        Set<Long> clickIds = new TreeSet<>();
        Set<Long> updateIds = new TreeSet<>();

        for (Tuple tuple : window.getTuples()){
            ClickUpdateEvent event = (ClickUpdateEvent) tuple.getValueByField("clickUpdateEvent");
            clickIds.add(event.getClickId());
            if (event.getUpdateId() != 0){
                updateIds.add(event.getUpdateId());
            }
        }

        PageStatistics statistics = new PageStatistics(
                key,
                new Date(window.getWinStart()),
                new Date(window.getWinEnd()),
                new ArrayList<>(clickIds),
                new ArrayList<>(updateIds));

        return statistics;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("page", "stats"));
        declarer.declareStream("lateEvents", new Fields("late_tuple"));
    }
}
