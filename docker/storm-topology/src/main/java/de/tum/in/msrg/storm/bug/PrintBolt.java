package de.tum.in.msrg.storm.bug;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;

import java.util.Map;

public class PrintBolt extends BaseRichBolt {
    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
    }

    @Override
    public void execute(Tuple input) {
        System.out.println(String.format("Start: %d, End: %d, Sum:%d", input.getLongByField("start"), input.getLongByField("end"), input.getIntegerByField("sum")));
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }
}
