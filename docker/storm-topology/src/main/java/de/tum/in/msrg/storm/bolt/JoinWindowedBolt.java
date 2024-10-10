package de.tum.in.msrg.storm.bolt;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.datamodel.UpdateEvent;
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

public class JoinWindowedBolt extends BaseRichBolt {

    private final String clickSource;
    private final String updateSource;

    private OutputCollector collector;
    private Map<String, OneMinuteWindow> clickWindows;
    private Map<String, OneMinuteWindow> updateWindows;

    public JoinWindowedBolt(String clickSource, String updateSource){
        this.clickSource = clickSource;
        this.updateSource = updateSource;
    }


    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        clickWindows = new ConcurrentHashMap<>();
        updateWindows = new ConcurrentHashMap<>();
    }

    @Override
    public void execute(Tuple input) {
        String page = input.getStringByField("page");
        Long eventTS = input.getLongByField("eventTimestamp");


        if (input.getSourceComponent().equalsIgnoreCase(clickSource)){
            ClickEvent event = (ClickEvent) input.getValueByField("clickEvent");
            OneMinuteWindow clickWindow = clickWindows.getOrDefault(page, null);

            if (clickWindow == null) {
                Date winStartTS = new Date(eventTS);
                winStartTS = DateUtils.truncate(winStartTS, Calendar.MINUTE);
                clickWindow = new OneMinuteWindow(winStartTS.getTime());
                clickWindow.getTuples().add(input);
                clickWindows.put(page, clickWindow);
            } else {
                if (eventTS >= clickWindow.getWinStart() && eventTS <= clickWindow.getWinEnd()){
                    clickWindow.getTuples().add(input);
                } else if (eventTS < clickWindow.getWinStart()){
                    collector.emit("lateEvents", input, new Values(event));
                } else if (eventTS > clickWindow.getWinEnd()) {
                    // process and emit AND create new window
                    OneMinuteWindow updateWindow = updateWindows.getOrDefault(page, null);

                }
            }

        } else if (input.getSourceComponent().equalsIgnoreCase("updateEvent")) {
            UpdateEvent event = (UpdateEvent) input.getValueByField("updateEvent");
            OneMinuteWindow updateWindow = updateWindows.getOrDefault(page, null);

            if (updateWindow == null){
                Date winStartTS = new Date(eventTS);
                winStartTS = DateUtils.truncate(winStartTS, Calendar.MINUTE);
                updateWindow = new OneMinuteWindow(winStartTS.getTime());
                updateWindow.getTuples().add(input);
                updateWindows.put(page, updateWindow);
            } else {
                if (eventTS >= updateWindow.getWinStart() && eventTS <= updateWindow.getWinEnd()) {
                    updateWindow.getTuples().add(input);
                } else if (eventTS < updateWindow.getWinStart()) {
                    collector.emit("lateEvents", input, new Values(event));
                } else if (eventTS > updateWindow.getWinEnd()) {
                    // process and emit AND create new window
                }
            }

        }

    }

    private PageStatistics joinWindows (String key, OneMinuteWindow clickWindow, OneMinuteWindow updateWindow){
        Set<Long> clickIds = new TreeSet<>();
        Set<Long> updateIds = new TreeSet<>();
        Long winStart = 0L;
        Long winEnd = 0L;
        PageStatistics statistics = null;

        if (updateWindow == null){
            winStart = clickWindow.getWinStart();
            winEnd = clickWindow.getWinEnd();
            for (Tuple tuple : clickWindow.getTuples()){
                ClickEvent event = (ClickEvent) tuple.getValueByField("clickEvent");
                clickIds.add(event.getId());
            }
        } else if (clickWindow == null) {
            winStart = updateWindow.getWinStart();
            winEnd = updateWindow.getWinEnd();
            for (Tuple tuple : updateWindow.getTuples()){
                UpdateEvent event = (UpdateEvent) tuple.getValueByField("updateEvent");
                updateIds.add(event.getId());
            }
        } else {
            Long clickTS = clickWindow.getWinStart();
            Long updateTS = updateWindow.getWinStart();
            if (clickTS < updateTS) {
                winStart = updateWindow.getWinStart();
                winEnd = updateWindow.getWinEnd();
                for (Tuple tuple : updateWindow.getTuples()){
                    UpdateEvent event = (UpdateEvent) tuple.getValueByField("updateEvent");
                    updateIds.add(event.getId());
                }
                for (Tuple tuple : clickWindow.getTuples()) {
                    collector.emit("lateEvents", tuple, new Values(tuple.getValueByField("clickEvents")));
                    collector.ack(tuple);
                }
            } else if (clickTS > updateTS){

                winStart = clickWindow.getWinStart();
                winEnd = clickWindow.getWinEnd();
                for (Tuple tuple : clickWindow.getTuples()){
                    ClickEvent event = (ClickEvent) tuple.getValueByField("clickEvent");
                    clickIds.add(event.getId());
                }
                for (Tuple tuple : updateWindow.getTuples()){
                    collector.emit("lateEvents", tuple, new Values(tuple.getValueByField("updateEvents")));
                    collector.ack(tuple);
                }

            } else {
                // same time window
            }
        }

        statistics = new PageStatistics(key,
                new Date(winStart),
                new Date(winEnd),
                new ArrayList<>(clickIds),
                new ArrayList<>(updateIds));

        return statistics;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("page", "eventTimestamp", "clickUpdateEvent"));
        declarer.declareStream("lateEvents", new Fields("late_tuple"));
    }
}
