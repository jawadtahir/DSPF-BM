package de.tum.in.msrg.storm.bolt;

import com.codahale.metrics.Counter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.Map;

public class ClickUpdateJoinParserBolt extends BaseRichBolt {
    private Counter counter;
    OutputCollector collector;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        counter = context.registerCounter("customThroughput");
    }

    @Override
    public void execute(Tuple input) {
        String msg = input.getStringByField("value");
        byte[] page = input.getBinaryByField("key");
        try {
            ClickEvent clickEvent = this.mapper.readValue(msg, ClickEvent.class);
            ClickUpdateEvent clickUpdateEvent = new ClickUpdateEvent(clickEvent, null);
            collector.emit(input, new Values(page, clickUpdateEvent.getClickTimestamp().getTime(), clickUpdateEvent));
            collector.ack(input);
            counter.inc();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("key", "eventTimestamp", "clickUpdateEvent"));

    }
}
