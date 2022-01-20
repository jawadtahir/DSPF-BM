package org.apache.flink.playgrounds.ops.clickcount;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventStatistics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

public class E2EMetrics {

    private static HTTPServer promServer;
    protected static final Gauge throughput = Gauge.build("e2eThroughput", "End-to-End throughput").register();
    protected static final Gauge latency = Gauge.build("e2eLatency", "End-to-End latency").register();

    private static final Logger LOGGER = LogManager.getLogger();

    public static void main (String[] args) throws IOException {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);
        Properties kafkaProps =  createKafkaProperties(parameterTool);

        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<byte[], byte[]>(kafkaProps);

        String topic = kafkaProps.getProperty("topic","output");
        consumer.subscribe(Arrays.asList(topic));
        promServer = new HTTPServer(9249);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            promServer.stop();
//            consumer.close();
        }));



        while (true){
                ConsumerRecords<byte[], byte[]> flinkOutputs = consumer.poll(Duration.ofMillis(10));
                Long throughput = 0L;
                for (ConsumerRecord<byte[], byte[]> record: flinkOutputs){
                    ObjectMapper object = new ObjectMapper();
                    ClickEventStatistics stats = object.readValue(record.value(), ClickEventStatistics.class);
                    LOGGER.error(stats);
                    throughput += stats.getCount();
                    LOGGER.error(throughput);

                    double latency = calculateLatency(stats);
                    LOGGER.error(latency);
                    E2EMetrics.latency.set(latency);

                }

                E2EMetrics.throughput.set((double) throughput);
            }

    }

    private static double calculateLatency(ClickEventStatistics stats) {
        Date firstMsgTS = stats.getFirstMsgTS();
        Date now = Date.from(Instant.now());

        long latency = now.getTime() - firstMsgTS.getTime();
        return (double) latency;
    }

    protected static Properties createKafkaProperties(final ParameterTool params) {
        String brokers = params.get("bootstrap.servers", "localhost:9092");
        Properties kafkaProps = new Properties();

        kafkaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getCanonicalName());
        kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getCanonicalName());
        kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, "metric");

        return kafkaProps;
    }

}
