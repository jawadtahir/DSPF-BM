package de.tum.in.msrg.flink.serialization;

import de.tum.in.msrg.datamodel.PageStatistics;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

public class PageStatisticsKeyValueSerializer implements SerializationSchema<PageStatistics> {
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(PageStatistics element) {
        try {
            return objectMapper.writeValueAsBytes(element.getPage());
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
