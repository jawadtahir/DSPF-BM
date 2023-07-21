package de.tum.in.msrg.latcal;

import de.tum.in.msrg.datamodel.PageStatistics;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

public class EndTimeReader implements Runnable{


    Properties kafkaProperties;
    final Map<PageTSKey, Date> pageTSKeyDateMap;

    Gauge latencyGauge;

    private static final Logger LOGGER = LogManager.getLogger(EndTimeReader.class);

    public EndTimeReader(Properties kafkaProperties, Map pageTSKeyDateMap){
        this.kafkaProperties = (Properties) kafkaProperties.clone();

        this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "latcalOutputReader");
        this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PageStatisticsDeserializer.class.getCanonicalName());
        LOGGER.info(String.format("Kafka Properties: %s", this.kafkaProperties.toString()));

        this.pageTSKeyDateMap = pageTSKeyDateMap;

    }


    @Override
    public void run() {
        LOGGER.info("Running thread...");
        LOGGER.info("Starting Kafka consumer and prometheus server...");
        try (
                KafkaConsumer<String, PageStatistics> consumer = new KafkaConsumer<String, PageStatistics>(this.kafkaProperties);
                HTTPServer promServer = new HTTPServer(52923);
        ) {
            consumer.subscribe(Arrays.asList("output"));
            latencyGauge = Gauge.build("de_tum_in_msrg_latcal_latency", "End to End latency").labelNames("key").register();

            while (true){
                ConsumerRecords<String, PageStatistics> records = consumer.poll(Duration.ofMillis(100));
                LOGGER.debug(String.format("Polled %d messages", records.count()));

                for (ConsumerRecord<String, PageStatistics> record : records){
                    PageTSKey key = new PageTSKey(record.value().getPage(), record.value().getWindowStart());
                    Date ingestionTime = this.pageTSKeyDateMap.getOrDefault(key, null);

                    assert ingestionTime != null;

                    Date egressTime = new Date(record.timestamp());
                    long latency = egressTime.getTime() - ingestionTime.getTime();
                    LOGGER.debug(String.format("The latency for %s is %d ms", key, latency));
                    latencyGauge.labels(key.getPage()).set(latency);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
