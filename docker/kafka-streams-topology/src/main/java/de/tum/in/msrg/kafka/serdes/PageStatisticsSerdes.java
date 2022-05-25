package de.tum.in.msrg.kafka.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.PageStatistics;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Map;

public class PageStatisticsSerdes implements Serde<PageStatistics>, Serializer<PageStatistics>, Deserializer<PageStatistics> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public PageStatistics deserialize(String topic, byte[] data) {
        try {
            return OBJECT_MAPPER.readValue(data, PageStatistics.class);
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
    public Serializer<PageStatistics> serializer() {
        return this;
    }

    @Override
    public Deserializer<PageStatistics> deserializer() {
        return this;
    }

    @Override
    public byte[] serialize(String topic, PageStatistics data) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
