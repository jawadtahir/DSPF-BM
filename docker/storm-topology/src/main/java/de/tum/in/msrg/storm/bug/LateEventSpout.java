package de.tum.in.msrg.storm.bug;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LateEventSpout extends BaseRichSpout {
    private SpoutOutputCollector collector;
    private List<Long> eventTimes;
    private int currentTime = 0;
    private int id = 1;
    public LateEventSpout () {
        eventTimes = new ArrayList<>();
        for (int i = 1; i<= 61; i++) {
            eventTimes.add(Instant.EPOCH.plusSeconds(i).toEpochMilli());
        }
    }
    @Override
    public void open(Map<String, Object> conf, TopologyContext context, SpoutOutputCollector collector) {
        this.collector = collector;
    }
    @Override
    public void nextTuple() {
        int eventId = id++;
        Long eventTime = eventTimes.get(currentTime++);
        if (currentTime == eventTimes.size()){
            currentTime = 0;
        }
        collector.emit(new Values(eventId, eventTime));
    }
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("id", "time"));
    }
}
