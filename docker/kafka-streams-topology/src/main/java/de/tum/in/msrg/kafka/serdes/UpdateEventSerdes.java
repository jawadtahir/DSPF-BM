package de.tum.in.msrg.kafka.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Map;

public class UpdateEventSerdes implements Serde<UpdateEvent>, Serializer<UpdateEvent>, Deserializer<UpdateEvent> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Override
    public UpdateEvent deserialize(String topic, byte[] data) {
        try {
            return OBJECT_MAPPER.readValue(data, UpdateEvent.class);
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
    public Serializer<UpdateEvent> serializer() {
        return this;
    }

    @Override
    public Deserializer<UpdateEvent> deserializer() {
        return this;
    }

    @Override
    public byte[] serialize(String topic, UpdateEvent data) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }
}
