package de.tum.in.msrg.kafka.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEventStatistics;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Map;

public class ClickEventStatsSerde implements Serde<ClickEventStatistics>, Serializer<ClickEventStatistics>, Deserializer<ClickEventStatistics> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ClickEventStatistics deserialize(String topic, byte[] data) {
        try {
            return mapper.readValue(data, ClickEventStatistics.class);
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
    public Serializer<ClickEventStatistics> serializer() {
        return this;
    }

    @Override
    public Deserializer<ClickEventStatistics> deserializer() {
        return this;
    }

    @Override
    public byte[] serialize(String topic, ClickEventStatistics data) {
        try {
            return mapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
