package de.tum.in.msrg.kafka.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.MetricConfig;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.stats.WindowedCount;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class UpdateEventMapper implements ValueMapper<String, Iterable<UpdateEvent>> {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger LOGGER = LogManager.getLogger(UpdateEventMapper.class);




    @Override
    public Iterable<UpdateEvent> apply(String value) {
        try {

            UpdateEvent event = mapper.readValue(value, UpdateEvent.class);
            LOGGER.debug(event.toString());

            return Arrays.asList(event);

        } catch (JsonProcessingException e) {
           e.printStackTrace();
           return null;
        }
    }
}
