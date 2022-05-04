package de.tum.in.msrg.kafka.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.kafka.streams.kstream.ValueMapper;

import java.util.Arrays;


public class ClickEventMapper implements ValueMapper<String, Iterable<ClickEvent>> {
    private static final ObjectMapper mapper = new ObjectMapper();
    @Override
    public Iterable<ClickEvent> apply(String value) {
        try {
            return Arrays.asList( mapper.readValue(value, ClickEvent.class));
        } catch (JsonProcessingException e) {
           e.printStackTrace();
           return null;
        }
    }
}
