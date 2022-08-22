package de.tum.in.msrg.storm.bolt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ParserBolt extends BaseRichBolt {
    private OutputCollector collector;
    private ObjectMapper mapper;
    @Override
    public void prepare(Map<String, Object> map, TopologyContext topologyContext, OutputCollector outputCollector) {
        collector = outputCollector;
        mapper = new ObjectMapper();

    }

    @Override
    public void execute(Tuple tuple) {
//        System.out.println(tuple.getValueByField("value").toString());
        try {
            ClickEvent event = mapper.readValue(
            tuple.getValueByField("value").toString().getBytes(StandardCharsets.UTF_8),
                    ClickEvent.class);
//            System.out.println(event);

            collector.ack(tuple);
            collector.emit(tuple, new Values(
                    event.getTimestamp().getTime(),
                    event.getPage(),
                    event.getCreationTimestamp().getTime()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("timestamp", "page", "creationTimestamp"));

    }
}
