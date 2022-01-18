package org.apache.flink.playgrounds.ops.clickcount.records;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class ClickEventStatisticsDeserializationSchema implements DeserializationSchema<ClickEventStatistics> {


    private static final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public ClickEventStatistics deserialize(byte[] message) throws IOException {
        return objectMapper.readValue(message, ClickEventStatistics.class);
    }

    @Override
    public boolean isEndOfStream(ClickEventStatistics nextElement) {
        return false;
    }

    @Override
    public TypeInformation<ClickEventStatistics> getProducedType() {
        return TypeInformation.of(ClickEventStatistics.class);
    }
}
