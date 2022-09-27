package de.tum.in.msrg.flink.serialization;

import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;

public class ClickEventDeserializer implements DeserializationSchema<ClickEvent> {
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ClickEvent deserialize(byte[] bytes) throws IOException {
        return objectMapper.readValue(bytes, ClickEvent.class);
    }

    @Override
    public boolean isEndOfStream(ClickEvent clickEvent) {
        return false;
    }

    @Override
    public TypeInformation<ClickEvent> getProducedType() {
        return TypeInformation.of(ClickEvent.class);
    }
}
