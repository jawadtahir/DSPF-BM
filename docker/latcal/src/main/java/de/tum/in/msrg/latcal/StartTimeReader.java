package de.tum.in.msrg.latcal;

import de.tum.in.msrg.datamodel.ClickEvent;
import io.prometheus.client.Gauge;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

public class StartTimeReader implements Runnable{
    Properties kafkaProperties;
    final Map<PageTSKey, Date> eventInTimeMap;
    final Map<PageTSKey, Date> eventOutTimeMap;
    final Gauge latencyGauge;


    private static final Logger LOGGER = LogManager.getLogger(StartTimeReader.class);

    public StartTimeReader(Properties kafkaProperties, Map inTimeMap, Map outTimeMap, Gauge latencyGauge){

        this.kafkaProperties = (Properties) kafkaProperties.clone();
        this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "latcalInputReader");
        this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ClickEventDeserializer.class.getCanonicalName());
        LOGGER.info(String.format("Kafka properties: %s", this.kafkaProperties.toString()));

        this.eventInTimeMap = inTimeMap;
        this.eventOutTimeMap = outTimeMap;
        this.latencyGauge = latencyGauge;

    }

    @Override
    public void run() {

        LOGGER.info("Running thread...");
        LOGGER.info("Creating kafka consumer");
        try (KafkaConsumer<String, ClickEvent> consumer = new KafkaConsumer<String, ClickEvent>(this.kafkaProperties)) {

            LOGGER.info("Subscribing to the click topic");
            consumer.subscribe(Arrays.asList("click"));

            while (true){
                ConsumerRecords<String, ClickEvent> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, ClickEvent> record : records) {
                    PageTSKey key = new PageTSKey(record.value().getPage(), record.value().getTimestamp());
                    Date ingestionTime = eventInTimeMap.getOrDefault(key, null);
                    if (ingestionTime == null) {
                        ingestionTime = new Date(record.timestamp());
                        eventInTimeMap.put(key, ingestionTime);
                        LOGGER.debug(String.format("Inserted %s with %s", key, eventInTimeMap.get(key)));

//                        if (eventOutTimeMap.get(key) != null){
//                            Date outDate = eventOutTimeMap.get(key);
//                            long latency = outDate.getTime() - ingestionTime.getTime();
//                            latencyGauge.labels(key.getPage()).set(latency);
//                        }
                    }
                }
            }

        } catch (Exception ex){
            ex.printStackTrace();
        }

    }

}
