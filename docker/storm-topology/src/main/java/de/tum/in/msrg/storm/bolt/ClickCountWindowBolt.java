package de.tum.in.msrg.storm.bolt;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.datamodel.UpdateEvent;
import org.apache.storm.state.KeyValueState;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseStatefulWindowedBolt;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;

import java.util.*;

public class ClickCountWindowBolt extends BaseWindowedBolt {
    private OutputCollector collector;

    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("page","stats"));
    }

    @Override
    public void execute(TupleWindow inputWindow) {
        Map<String, PageStatistics> statsMap = new HashMap<>();
//        Iterator<Tuple> iterator = inputWindow.getIter();
        Date windowStart = new Date(inputWindow.getStartTimestamp());
        Date windowEnd = new Date(inputWindow.getEndTimestamp());
//"page,storm-click-parser:eventTimestamp,clickEvent,storm-update-parser:eventTimestamp,updateEvent"
        for (Tuple tuple : inputWindow.get()){
//            Tuple tuple = iterator.next();
            String page = tuple.getStringByField("page");
            ClickEvent clickEvent = (ClickEvent) tuple.getValueByField("clickEvent");
            UpdateEvent updateEvent = (UpdateEvent) tuple.getValueByField("updateEvent");
            ClickUpdateEvent joinEvent = new ClickUpdateEvent(clickEvent, updateEvent);

            PageStatistics stats = statsMap.getOrDefault(page, null);
            if (stats == null){
                PageStatistics tempStat = new PageStatistics();
                tempStat.setWindowStart(windowStart);
                tempStat.setWindowEnd(windowEnd);
                tempStat.setPage(page);
                tempStat.getClickIds().add(joinEvent.getClickId());
                if (joinEvent.getUpdateId() != 0){
                    tempStat.getUpdateIds().add(joinEvent.getUpdateId());
                }
                statsMap.put(page, tempStat);
            }else{
                stats.getClickIds().add(joinEvent.getClickId());
                if (joinEvent.getUpdateId() != 0 && !stats.getUpdateIds().contains(joinEvent.getUpdateId())){
                    stats.getUpdateIds().add(joinEvent.getUpdateId());
                }
            }

        }

        statsMap.forEach((s, pageStatistics) -> {
            this.collector.emit(new Values(
                    s, pageStatistics
            ));
        });


    }
}
