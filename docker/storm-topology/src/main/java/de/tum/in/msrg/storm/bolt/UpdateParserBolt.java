package de.tum.in.msrg.storm.bolt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.UpdateEvent;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.Map;

public class UpdateParserBolt extends BaseRichBolt {
    private OutputCollector collector;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        String msg = input.getStringByField("value");
        try {
            UpdateEvent updateEvent = this.objectMapper.readValue(msg, UpdateEvent.class);
            collector.emit(input, new Values(updateEvent.getPage(), updateEvent.getTimestamp().getTime(), updateEvent));
            collector.ack(input);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("page", "eventTimestamp", "updateEvent"));
    }
}
