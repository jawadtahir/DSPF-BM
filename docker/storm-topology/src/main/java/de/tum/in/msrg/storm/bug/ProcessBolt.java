package de.tum.in.msrg.storm.bug;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.Map;

public class ProcessBolt extends BaseRichBolt {
    OutputCollector collector;
    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        int id = input.getIntegerByField("id");
        long timestamp = input.getLongByField("time");
//        System.out.println(String.format("processing event. id=%d, ts=%d",
//                id,
//                timestamp));
        collector.emit(input, new Values(id, timestamp));
        collector.ack(input);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("id", "time"));
    }
}
