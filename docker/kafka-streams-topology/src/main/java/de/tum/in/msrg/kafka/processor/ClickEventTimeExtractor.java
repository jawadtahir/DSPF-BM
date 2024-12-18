package de.tum.in.msrg.kafka.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

public class ClickEventTimeExtractor implements TimestampExtractor {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        try {
            ClickEvent event = mapper.readValue( (String) record.value(), ClickEvent.class);
            return event.getTimestamp().getTime();
        } catch (JsonProcessingException e) {
            return -1;
        }
    }
}
