package de.tum.in.msrg.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.common.ClickUpdateEventDeserializer;
import de.tum.in.msrg.common.Constants;
import de.tum.in.msrg.common.PageStatisticsDeserializer;
import de.tum.in.msrg.common.PageTSKey;
import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VerifyLate implements Runnable{

    String bootstrap;
    Map<PageTSKey, Map<Long, Boolean>> inputIdMap;
    Map<PageTSKey, Map<Long, Boolean>> processedMap;
    Counter processedCounter;
    Counter duplicateCounter;
    Counter lateCounter;
    Counter receivedInputCounter;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LogManager.getLogger(VerifyLate.class);

    public VerifyLate(
            String bootstrap,
            Map<PageTSKey, Map<Long, Boolean>> inputIdMap,
            Map<PageTSKey, Map<Long, Boolean>> processedMap,
            Counter processedCounter,
            Counter duplicateCounter,
            Counter receivedInputCounter) {

        this.bootstrap = bootstrap;
        this.inputIdMap = inputIdMap;
        this.processedMap = processedMap;
        this.processedCounter = processedCounter;
        this.duplicateCounter = duplicateCounter;
        this.receivedInputCounter = receivedInputCounter;
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(10);
        lateCounter = Counter.build("de_tum_in_msrg_pgv_late", "Dropped events due to late arrival").labelNames("key").register();

        LOGGER.debug(String.format("Already processed: %s", Arrays.deepToString(processedMap.keySet().toArray())));
        try (KafkaConsumer<byte[], String> kafkaConsumer = new KafkaConsumer<byte[], String>(getKafkaProperties())){
            LOGGER.info(String.format("Subscribing to %s topic...", Constants.LATE_OUTPUT_TOPIC));
            kafkaConsumer.subscribe(Arrays.asList(Constants.LATE_OUTPUT_TOPIC));
            while (true) {
                ConsumerRecords<byte[], String> records = kafkaConsumer.poll(Duration.ofMillis(100));
                LOGGER.debug(String.format("Polled %d messages.", records.count()));

                for (ConsumerRecord<byte[], String> record : records) {
                    String page = MAPPER.readValue(record.key(), String.class);

                    Date eventTimestamp = null;
                    Long clickId = 0L;
                    Long updateId = 0L;

                    if (record.value().contains("id") && record.value().contains("updatedBy")){
                        try {
                            UpdateEvent event = MAPPER.readValue(record.value(), UpdateEvent.class);
                            eventTimestamp = event.getTimestamp();
                            updateId = event.getId();
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                            continue;
                        }
                    } else if (record.value().contains("id") && record.value().contains("creationTimestamp")){
                        try {
                            ClickEvent event = MAPPER.readValue(record.value(), ClickEvent.class);
                            eventTimestamp = event.getTimestamp();
                            clickId = event.getId();
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                            continue;
                        }
                    } else if (record.value().contains("clickTimestamp") ){
                        try {
                            ClickUpdateEvent event = MAPPER.readValue(record.value(), ClickUpdateEvent.class);
                            eventTimestamp = event.getClickTimestamp();
                            clickId = event.getClickId();
                            updateId = event.getUpdateId();
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                            continue;
                        }
                    } else {
                        LOGGER.error("Event type is unknown");
                        System.exit(-1);
                    }


                    PageTSKey key = new PageTSKey(page, eventTimestamp);


                    LOGGER.debug(String.format("Key: %s",key.toString() ));

                    LOGGER.debug(String.format("Map contains key: %s", processedMap.containsKey(key)));
                    Map<Long, Boolean> previousIds = processedMap.getOrDefault(key, new ConcurrentHashMap<>());
//                    if (previousIds == null) {
//                        LOGGER.warn(String.format("Late event's corresponding processed not found. Seeking to topic %s, part %d, offset %d...", record.topic(), record.partition(), record.offset()));
//                        kafkaConsumer.seek(new TopicPartition(record.topic(), record.partition()), record.offset());
//                        break;
//                    }

                    LOGGER.debug("Updating counters...");
                    lateCounter.labels(page).inc();
                    receivedInputCounter.labels(page).inc();

                    if (previousIds.containsKey(clickId)){
                        duplicateCounter.labels(key.getPage()).inc();
                    } else {
                        processedCounter.labels(key.getPage()).inc();
                        previousIds.put(clickId, true);
                    }

                    if (updateId != 0L){
                        if (previousIds.containsKey(updateId)){
                            duplicateCounter.labels(key.getPage()).inc();
                        } else {
                            processedCounter.labels(key.getPage()).inc();
                            previousIds.put(updateId, true);
                        }
                    }


                    LOGGER.debug("Updating processed...");
                    processedMap.put(key, previousIds);

                }
            }
        } catch (StreamReadException e) {
            e.printStackTrace();
        } catch (DatabindException e) {
            e.printStackTrace();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    Properties getKafkaProperties(){
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "lateOutputVerifier");

        return properties;
    }
}
