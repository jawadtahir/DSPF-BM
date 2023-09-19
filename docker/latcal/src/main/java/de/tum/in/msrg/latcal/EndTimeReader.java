package de.tum.in.msrg.latcal;

import de.tum.in.msrg.datamodel.PageStatistics;
import io.prometheus.client.Gauge;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;

public class EndTimeReader implements Runnable{


    Properties kafkaProperties;
    final Map<PageTSKey, Date> eventInTimeMap;
    final Map<PageTSKey, Date> eventOutTimeMap;

    final Gauge latencyGauge;

    private static final Logger LOGGER = LogManager.getLogger(EndTimeReader.class);

    public EndTimeReader(Properties kafkaProperties, Map eventInTimeMap, Map eventOutTimeMap, Gauge latencyGauge){
        this.kafkaProperties = (Properties) kafkaProperties.clone();

        this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "latcalOutputReader");
        this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PageStatisticsDeserializer.class.getCanonicalName());
        LOGGER.info(String.format("Kafka Properties: %s", this.kafkaProperties.toString()));

        this.eventInTimeMap = eventInTimeMap;
        this.eventOutTimeMap = eventOutTimeMap;
        this.latencyGauge = latencyGauge;

    }


    @Override
    public void run() {
        LOGGER.info("Running thread...");
        LOGGER.info("Starting Kafka consumer and prometheus server...");
        try (
                KafkaConsumer<String, PageStatistics> consumer = new KafkaConsumer<String, PageStatistics>(this.kafkaProperties);
        ) {
            consumer.subscribe(Arrays.asList("output"));

            while (true){
//                Thread.sleep(500);
                ConsumerRecords<String, PageStatistics> records = consumer.poll(Duration.ofMillis(500));
                LOGGER.debug(String.format("Polled %d messages", records.count()));

                for (TopicPartition topicPartition : records.partitions()){
                    for (ConsumerRecord<String, PageStatistics> record : records.records(topicPartition)){
                        PageTSKey key = new PageTSKey(record.value().getPage(), record.value().getWindowStart());
                        Date outTime = new Date(record.timestamp());
                        if (eventOutTimeMap.get(key) == null) {
                            eventOutTimeMap.put(key, outTime);
                            Date inTime = eventInTimeMap.get(key);
                            if (inTime != null){
                                long latency = outTime.getTime() - inTime.getTime();
                                latencyGauge.labels(key.getPage()).set(latency);
                            } else {
                                consumer.seek(new TopicPartition(record.topic(), record.partition()), record.offset());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

}
