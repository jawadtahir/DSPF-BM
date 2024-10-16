package de.tum.in.msrg.latcal;

import de.tum.in.msrg.datamodel.ClickEvent;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;

public class PGVInNew implements Runnable {

    private final Map<PageTSKey, List<Long>> windowIdMap;
    private final Map<PageTSKey, Map<Long, Long>> procEventsMap;
    private final Map<PageTSKey, Integer> unprocCountMap;
    private final Gauge unprocGauge;
    private final int numEventsPerWindow;
    private Properties kafkaProperties;
    private static final Logger LOGGER = LogManager.getLogger(PGVInNew.class);

    public PGVInNew(
            Properties kafkaProperties,
            Map<PageTSKey, List<Long>> windowIdMap,
            Map<PageTSKey, Map<Long, Long>> procEventsMap,
            Map<PageTSKey, Integer> unprocCountMap,
            Gauge unprocGauge,
            int numEventsPerWindow) {
        this.kafkaProperties = (Properties) kafkaProperties.clone();
        this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvInputReader");
        this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ClickEventDeserializer.class.getCanonicalName());
        LOGGER.info(String.format("Kafka properties: %s", this.kafkaProperties.toString()));

        this.windowIdMap = windowIdMap;
        this.procEventsMap = procEventsMap;
        this.unprocCountMap = unprocCountMap;
        this.unprocGauge = unprocGauge;
        this.numEventsPerWindow = numEventsPerWindow;
    }

    @Override
    public void run() {

        LOGGER.info("Running thread...");
        LOGGER.info("Creating kafka consumer");

        try (KafkaConsumer<String, ClickEvent> consumer = new KafkaConsumer<String, ClickEvent>(this.kafkaProperties)) {

            LOGGER.info("Subscribing to the click topic");
            consumer.subscribe(Arrays.asList("click"));

            LOGGER.info("Creating probes...");
            Counter expectedWindowCounter = Counter.build("de_tum_in_msrg_pgv_expected_windows", "Expected windows").labelNames("key").register();
            Counter readEventsCounter = Counter.build("de_tum_in_msrg_pgv_read_events", "Read events").labelNames("key").register();

            while (true) {
                ConsumerRecords<String, ClickEvent> records = consumer.poll(Duration.ofMillis(100));
                LOGGER.debug(String.format("Polled %d messages", records.count()));


                for (ConsumerRecord<String, ClickEvent> record : records) {
                    readEventsCounter.labels(record.value().getPage()).inc();
                    PageTSKey window = new PageTSKey(record.value().getPage(), record.value().getTimestamp());
                    List<Long> eventIds = windowIdMap.getOrDefault(window, null);
                    if (eventIds == null) {
                        LOGGER.debug(String.format("No event list found for: %s", window));
                        expectedWindowCounter.labels(window.getPage()).inc();
                        List<Long> newList = Collections.synchronizedList(new ArrayList<Long>());
                        newList.add(record.value().getId());
                        windowIdMap.put(window, newList);
                    } else {
                        eventIds.add(record.value().getId());
                            if (eventIds.size() == numEventsPerWindow){
                                Map<Long, Long> procEvents = procEventsMap.getOrDefault(window, null);
                                if (procEvents != null){
                                    int unprocCount = eventIds.size() - procEvents.size();
                                    unprocCountMap.put(window, unprocCount);
                                    LOGGER.info(String.format("Found corresponding output: %s.\nUnprocessed count: %d", window, unprocCount));
                                    PGVUnprocUpdate pgvUnprocUpdate = new PGVUnprocUpdate(unprocCountMap, unprocGauge);
                                    pgvUnprocUpdate.update();
                                }
                            }
                    }
                }

            }
        }

    }
}
