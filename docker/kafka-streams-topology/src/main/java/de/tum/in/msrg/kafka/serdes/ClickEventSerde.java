package de.tum.in.msrg.kafka.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Map;

public class ClickEventSerde implements Serde<ClickEvent>, Serializer<ClickEvent>, Deserializer<ClickEvent> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Serde.super.configure(configs, isKey);
    }

    @Override
    public void close() {
        Serde.super.close();
    }

    @Override
    public Serializer<ClickEvent> serializer() {
        return this;
    }

    @Override
    public Deserializer<ClickEvent> deserializer() {
        return this;
    }

    @Override
    public ClickEvent deserialize(String topic, byte[] data) {
        try {
            return mapper.readValue(data, ClickEvent.class);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public byte[] serialize(String topic, ClickEvent data) {
        try {
            return mapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }
}
