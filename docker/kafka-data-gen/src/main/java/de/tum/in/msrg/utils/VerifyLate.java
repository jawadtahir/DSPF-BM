package de.tum.in.msrg.utils;

import de.tum.in.msrg.common.ClickUpdateEventDeserializer;
import de.tum.in.msrg.common.Constants;
import de.tum.in.msrg.common.PageStatisticsDeserializer;
import de.tum.in.msrg.common.PageTSKey;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;

public class VerifyLate implements Runnable{

    String bootstrap;
    Map<PageTSKey, List<Long>> inputIdMap;
    Map<PageTSKey, List<Long>> processedMap;
    Counter processedCounter;
    Counter duplicateCounter;
    Counter lateCounter;
    Counter receivedInputCounter;

    private static final Logger LOGGER = LogManager.getLogger(VerifyLate.class);

    public VerifyLate(
            String bootstrap,
            Map<PageTSKey, List<Long>> inputIdMap,
            Map<PageTSKey, List<Long>> processedMap,
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
        try (KafkaConsumer<String, ClickUpdateEvent> kafkaConsumer = new KafkaConsumer<String, ClickUpdateEvent>(getKafkaProperties())){
            LOGGER.info(String.format("Subscribing to %s topic...", Constants.LATE_OUTPUT_TOPIC));
            kafkaConsumer.subscribe(Arrays.asList(Constants.LATE_OUTPUT_TOPIC));
            while (true) {
                ConsumerRecords<String, ClickUpdateEvent> records = kafkaConsumer.poll(Duration.ofMillis(100));
                LOGGER.debug(String.format("Polled %d messages.", records.count()));

                for (ConsumerRecord<String, ClickUpdateEvent> record : records) {

                    LOGGER.debug("Updating counters...");
                    lateCounter.labels(record.value().getPage()).inc();
                    receivedInputCounter.labels(record.value().getPage()).inc();


                    PageTSKey key = new PageTSKey(record.value().getPage(), record.value().getClickTimestamp());
                    Long id = record.value().getClickId();

                    LOGGER.debug(String.format("Key: %s",key.toString() ));

                    LOGGER.debug(String.format("Map contains key: %s", processedMap.containsKey(key)));
                    List<Long> prevProcList = processedMap.getOrDefault(key, Collections.synchronizedList(new ArrayList<>()) );

//                    LOGGER.debug(String.format("Already processed: %s", Arrays.deepToString(prevProcMap.keySet().toArray())));

                    if (prevProcList.contains(id)){
                        duplicateCounter.labels(key.getPage()).inc();
                    } else {
                        processedCounter.labels(key.getPage()).inc();
                        prevProcList.add(id);
                    }

                    LOGGER.debug("Updating processed...");
                    processedMap.put(key, prevProcList);

                }
            }
        }
    }

    Properties getKafkaProperties(){
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ClickUpdateEventDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "lateOutputVerifier");

        return properties;
    }
}
