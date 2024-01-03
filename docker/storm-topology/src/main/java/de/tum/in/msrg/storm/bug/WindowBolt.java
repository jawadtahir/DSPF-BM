package de.tum.in.msrg.storm.bug;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;

import java.util.Map;

public class WindowBolt extends BaseWindowedBolt {
    OutputCollector collector;
    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector){
        this.collector = collector;
    }
    @Override
    public void execute(TupleWindow inputWindow) {
        int sum = 0;
        for (Tuple event : inputWindow.get()){
            sum++;
        }
        collector.emit(new Values(inputWindow.getStartTimestamp(), inputWindow.getEndTimestamp(), sum));
    }
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("start", "end", "sum"));
    }
}
