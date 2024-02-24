package de.tum.in.msrg.storm.translator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.storm.kafka.spout.KafkaTuple;
import org.apache.storm.kafka.spout.RecordTranslator;
import org.apache.storm.tuple.Fields;

import java.util.Arrays;
import java.util.List;

public class EventTranslator implements RecordTranslator<String, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private Fields fields = new Fields("key", "value");
    private String stream = "default";

    @Override
    public List<Object> apply(ConsumerRecord<String, String> record) {
        try {
            String key = OBJECT_MAPPER.readValue(record.key(), String.class);
            byte [] bKey = key.getBytes();
            String value = record.value();
            KafkaTuple tuple = new KafkaTuple(bKey, value);
            return tuple.routedTo(stream);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Fields getFieldsFor(String stream) {
        return fields;
    }

    @Override
    public List<String> streams() {
        return Arrays.asList(stream);
    }
}
