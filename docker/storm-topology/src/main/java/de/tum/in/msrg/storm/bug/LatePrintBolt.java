package de.tum.in.msrg.storm.bug;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;

import java.util.Map;

public class LatePrintBolt extends BaseRichBolt {
    OutputCollector collector;
    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        Tuple tuple = (Tuple)input.getValueByField("late_tuple");
        int id = tuple.getIntegerByField("id");
        long ts = tuple.getLongByField("time");
        System.out.println("received late event. id=%d, ts=%d");
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {

    }
}
