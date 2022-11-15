package de.tum.in.msrg.latcal;

import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

public class StartTimeReader implements Runnable{
    Properties kafkaProperties;
    final Map<PageTSKey, Date> pageInsertionTimeMap;


    private static final Logger LOGGER = LogManager.getLogger(StartTimeReader.class);

    public StartTimeReader(Properties kafkaProperties, Map map){

        this.kafkaProperties = (Properties) kafkaProperties.clone();
        this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "latcalInputReader");
        this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ClickEventDeserializer.class.getCanonicalName());
        LOGGER.info(String.format("Kafka properties: %s", this.kafkaProperties.toString()));

        this.pageInsertionTimeMap = map;

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
                LOGGER.debug(String.format("Polled %d messages",records.count()));

                for (ConsumerRecord<String, ClickEvent> record : records){
                    PageTSKey  key = new PageTSKey(record.value().getPage(), record.value().getTimestamp());
                    Date ingestionTime = pageInsertionTimeMap.getOrDefault(key, null);
                    if (ingestionTime == null){
                        pageInsertionTimeMap.put(key, new Date(record.timestamp()));
                    }
                }
            }

        } catch (Exception ex){
            ex.printStackTrace();
        }

    }

}
