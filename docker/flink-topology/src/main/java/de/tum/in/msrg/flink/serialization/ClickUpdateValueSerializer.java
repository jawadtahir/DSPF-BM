package de.tum.in.msrg.flink.serialization;

import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

public class ClickUpdateValueSerializer implements SerializationSchema<ClickUpdateEvent> {
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(ClickUpdateEvent element) {
        try {
            return objectMapper.writeValueAsBytes(element);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
