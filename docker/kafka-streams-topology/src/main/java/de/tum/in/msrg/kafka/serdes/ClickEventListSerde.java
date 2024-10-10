package de.tum.in.msrg.kafka.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ClickEventListSerde implements Serde<List<ClickEvent>>, Serializer<List<ClickEvent>>, Deserializer<List<ClickEvent>> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<ClickEvent> deserialize(String topic, byte[] data) {
        try {
            return mapper.readValue(data, new TypeReference<List<ClickEvent>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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
    public Serializer<List<ClickEvent>> serializer() {
        return this;
    }

    @Override
    public Deserializer<List<ClickEvent>> deserializer() {
        return this;
    }

    @Override
    public byte[] serialize(String topic, List<ClickEvent> data) {

        try {
            return mapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
