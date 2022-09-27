package de.tum.in.msrg.flink.serialization;

import de.tum.in.msrg.datamodel.UpdateEvent;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class UpdateEventDeserializer implements DeserializationSchema<UpdateEvent> {
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public UpdateEvent deserialize(byte[] message) throws IOException {
        return objectMapper.readValue(message, UpdateEvent.class);
    }

    @Override
    public boolean isEndOfStream(UpdateEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<UpdateEvent> getProducedType() {
        return TypeInformation.of(UpdateEvent.class);
    }
}
