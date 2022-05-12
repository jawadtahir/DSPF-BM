package de.tum.in.msrg.kafka.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.MetricConfig;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.stats.WindowedCount;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.streams.kstream.ValueMapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ClickEventMapper implements ValueMapper<String, Iterable<ClickEvent>> {
    private static final ObjectMapper mapper = new ObjectMapper();
    private WindowedCount throughput;
    private MetricConfig config = new MetricConfig();

    public ClickEventMapper() {
        super();
    }

    public ClickEventMapper(Metrics metrics) {
        this();
        this.throughput = new WindowedCount();

        if (metrics != null){
            MetricName throughputName = metrics.metricName("customThroughput", "stream-custom-metrics", new HashMap<>(Map.of("machine-id", getID())));
            this.config = metrics.config();
            metrics.addMetric(throughputName, this.throughput);
        }

    }

    private static String getID(){
        String uid = null;

        try{
            uid = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            uid = Thread.currentThread().getName();
            e.printStackTrace();
        }

        return uid;
    }



    @Override
    public Iterable<ClickEvent> apply(String value) {
        try {
            this.throughput.record(this.config, 1.0, Time.SYSTEM.milliseconds());
            return Arrays.asList( mapper.readValue(value, ClickEvent.class));

        } catch (JsonProcessingException e) {
           e.printStackTrace();
           return null;
        }
    }
}
