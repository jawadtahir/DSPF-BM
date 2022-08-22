package de.tum.in.msrg.kafka.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Map;

public class ClickUpdateSerdes implements Serde<ClickUpdateEvent>, Serializer<ClickUpdateEvent>, Deserializer<ClickUpdateEvent> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Override
    public ClickUpdateEvent deserialize(String topic, byte[] data) {
        try {
            return OBJECT_MAPPER.readValue(data, ClickUpdateEvent.class);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Serde.super.configure(configs, isKey);
    }

    @Override
    public void close() {
        Serde.super.close();
    }

    @Override
    public Serializer<ClickUpdateEvent> serializer() {
        return this;
    }

    @Override
    public Deserializer<ClickUpdateEvent> deserializer() {
        return this;
    }

    @Override
    public byte[] serialize(String topic, ClickUpdateEvent data) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }
}
