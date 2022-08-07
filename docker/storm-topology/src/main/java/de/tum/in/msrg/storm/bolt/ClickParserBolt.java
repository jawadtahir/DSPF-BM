package de.tum.in.msrg.storm.bolt;

import com.codahale.metrics.Counter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.Map;

public class ClickParserBolt extends BaseRichBolt {
    private Counter counter;
    OutputCollector collector;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        counter = context.registerCounter("customThroughout");
    }

    @Override
    public void execute(Tuple input) {
        String msg = input.getStringByField("value");
        try {
            ClickEvent clickEvent = this.mapper.readValue(msg, ClickEvent.class);
            collector.emit(input, new Values(clickEvent.getPage(), clickEvent.getTimestamp().getTime(), clickEvent));
            collector.ack(input);
            counter.inc();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("page", "eventTimestamp", "clickEvent"));

    }
}
