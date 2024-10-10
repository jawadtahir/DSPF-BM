package de.tum.in.msrg.latcal;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.Map;

public class ClickUpdateEventDeserializer implements Deserializer<ClickUpdateEvent> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

    @Override
    public ClickUpdateEvent deserialize(String topic, byte[] data) {
        try {
            return OBJECT_MAPPER.readValue(data, ClickUpdateEvent.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ClickUpdateEvent deserialize(String topic, Headers headers, byte[] data) {
        return deserialize(topic, data);
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}
