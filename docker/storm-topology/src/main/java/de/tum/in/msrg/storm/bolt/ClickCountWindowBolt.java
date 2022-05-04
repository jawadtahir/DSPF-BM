package de.tum.in.msrg.storm.bolt;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickEventStatistics;
import org.apache.storm.state.KeyValueState;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseStatefulWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClickCountWindowBolt extends BaseStatefulWindowedBolt<KeyValueState<String, ClickEventStatistics>> {
    private OutputCollector collector;
    private KeyValueState<String, ClickEventStatistics> state;

    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("stats"));
    }

    @Override
    public void execute(TupleWindow inputWindow) {
        Map<String, ClickEventStatistics> statsMap = new HashMap<>();
        Iterator<Tuple> iterator = inputWindow.getIter();
        Date windowStart = new Date(inputWindow.getStartTimestamp());
        Date windowEnd = new Date(inputWindow.getEndTimestamp());

        while (iterator.hasNext()){
            Tuple tuple = iterator.next();
            String page = tuple.getStringByField("page");
            ClickEvent clickEvent = (ClickEvent) tuple.getValueByField("clickEvent");
            ClickEventStatistics stats = statsMap.getOrDefault(page, null);
            if (stats == null){
                Date firstMsgTS = clickEvent.getCreationTimestamp();
                ClickEventStatistics tempStat = new ClickEventStatistics(windowStart, windowEnd, firstMsgTS, page, 1);
                statsMap.put(page, tempStat);
            }else{
                stats.setCount(stats.getCount()+1);
            }

        }

        statsMap.forEach((s, clickEventStatistics) -> {
            this.collector.emit(new Values(
                    clickEventStatistics
            ));
        });


    }

    @Override
    public void initState(KeyValueState<String, ClickEventStatistics> entries) {
        this.state = state;
    }
}
