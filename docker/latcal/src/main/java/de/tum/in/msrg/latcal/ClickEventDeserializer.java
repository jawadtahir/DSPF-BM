package de.tum.in.msrg.latcal;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.Map;

public class ClickEventDeserializer implements Deserializer<ClickEvent> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

    @Override
    public ClickEvent deserialize(String topic, byte[] data) {
        try {
            return OBJECT_MAPPER.readValue(data, ClickEvent.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ClickEvent deserialize(String topic, Headers headers, byte[] data) {
        return Deserializer.super.deserialize(topic, headers, data);
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}
