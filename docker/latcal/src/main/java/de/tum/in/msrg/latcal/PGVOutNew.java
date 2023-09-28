package de.tum.in.msrg.latcal;


import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

public class PGVOutNew implements Runnable {

    private final ConcurrentHashMap<PageTSKey, List<Long>> windowIdMap;
    private final Properties kafkaProperties;
    private final int numEventsPerWindow;
    private final ConcurrentHashMap<PageTSKey, Map<Long, Long>> eventsProcessed;
    private final ConcurrentHashMap<PageTSKey, Integer> unprocessedEventsCount;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final Gauge unprocessedGauge;

    private static final Logger LOGGER = LogManager.getLogger(PGVOutNew.class);


    public PGVOutNew(ThreadPoolExecutor threadPoolExecutor, Properties kafkaProperties, ConcurrentHashMap<PageTSKey, List<Long>> windowIdMap, ConcurrentHashMap<PageTSKey, Map<Long, Long>> processedWindowIdMap, ConcurrentHashMap<PageTSKey, Integer> unprocessedEventsCount, Gauge unprocGauge, int numEventsPerWindow) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.kafkaProperties = (Properties) kafkaProperties.clone();

        this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvOutputReader");
        this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PageStatisticsDeserializer.class.getCanonicalName());
        LOGGER.info(String.format("Kafka Properties: %s", this.kafkaProperties.toString()));

        this.windowIdMap = windowIdMap;
        this.unprocessedEventsCount = unprocessedEventsCount;
        this.eventsProcessed = processedWindowIdMap;
        this.unprocessedGauge = unprocGauge;
        this.numEventsPerWindow = numEventsPerWindow;

    }

    @Override
    public void run() {

        LOGGER.info("Running thread...");


        LOGGER.info("Creating kafka consumer");
        try (KafkaConsumer<String, PageStatistics> consumer = new KafkaConsumer<String, PageStatistics>(this.kafkaProperties)) {

            LOGGER.info("Subscribing to the out topic");
            consumer.subscribe(Arrays.asList("output"));


            Counter processedCounter = Counter.build("de_tum_in_msrg_pgv_processed", "Total unique processed events").labelNames("key").register();
            Counter receivedCounter = Counter.build("de_tum_in_msrg_pgv_received", "Total received events").labelNames("key").register();
            Counter duplicateCounter = Counter.build("de_tum_in_msrg_pgv_duplicate", "Duplicate processed events").labelNames("key").register();
//            Counter lateCounter = Counter.build("de_tum_in_msrg_pgv_late", "Dropped events due to late arrival").labelNames("key").register();
            Counter correctOutputCounter = Counter.build("de_tum_in_msrg_pgv_correct_output", "Correct outputs").labelNames("key").register();
            Counter inCorrectOutputCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_output", "incorrect outputs").labelNames("key").register();
            Counter inCorrectOutputHigherCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_higher_output", "incorrect outputs, higher than expected").labelNames("key").register();
            Counter inCorrectOutputLowerCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_lower_output", "incorrect outputs, lower than expected").labelNames("key").register();
            Counter receivedWindowCounter = Counter.build("de_tum_in_msrg_pgv_received_windows", "Received windows").labelNames("key").register();
//            Gauge unprocessedGauge = Gauge.build("de_tum_in_msrg_pgv_unprocessed", "Unprocessed events").labelNames("key").register();
            Histogram outputCounterHistogram = Histogram.build("de_tum_in_msrg_pgv_output_counter", "Output counter").labelNames("key").linearBuckets(4990, 1, 11).register();


            int polledMsgs = 0;
            LOGGER.info("Created probes...");

            PGVOutLate pgvOutLate = new PGVOutLate(
                    this.eventsProcessed,
                    this.windowIdMap,
                    this.unprocessedEventsCount,
                    this.kafkaProperties,
                    receivedCounter,
                    duplicateCounter,
                    processedCounter,
                    this.unprocessedGauge);

            this.threadPoolExecutor.submit(pgvOutLate);

            while (true) {
                ConsumerRecords<String, PageStatistics> records = consumer.poll(Duration.ofMillis(100));
                polledMsgs = records.count();
                LOGGER.debug(String.format("Polled %d messages", polledMsgs));

                for (ConsumerRecord<String, PageStatistics> record : records) {

                    PageTSKey window = new PageTSKey(record.value().getPage(), record.value().getWindowStart());
//                    LOGGER.debug(String.format("Processing: %s", record.value().toString()));
                    LOGGER.debug(String.format("Window Key: %s", window.toString()));

                    receivedWindowCounter.labels(record.value().getPage()).inc();

//                    LOGGER.debug("Verifying correctness");
                    outputCounterHistogram.labels(window.getPage()).observeWithExemplar(record.value().getCount(), Map.of("start", record.value().getWindowStart().toString()));
                    if (record.value().getCount() == this.numEventsPerWindow) {
                        correctOutputCounter.labels(window.getPage()).inc();
                    } else {
                        inCorrectOutputCounter.labels(window.getPage()).inc();
                        if (record.value().getCount() > this.numEventsPerWindow) {
                            inCorrectOutputHigherCounter.labels(window.getPage()).inc();
                        } else {
                            inCorrectOutputLowerCounter.labels(window.getPage()).inc();
                        }
                    }

//                    LOGGER.debug("Verifying PGs");
//                    List<Long> expectedEvents = windowIdMap.getOrDefault(window, null);
                    Map<Long, Long> prvPrcsd = this.eventsProcessed.getOrDefault(window, new ConcurrentHashMap<Long, Long>());

//                    assert expectedEvents != null;
                    for (Long eventId : record.value().getIds()) {
                        receivedCounter.labels(window.getPage()).inc();
//                        assert expectedEvents.contains(eventId);
//                        Collections.binarySearch()
                        if (prvPrcsd.containsKey(eventId)) {
                            Long count = prvPrcsd.get(eventId);
                            prvPrcsd.put(eventId, ++count);
                            duplicateCounter.labels(window.getPage()).inc();
                        } else {
                            prvPrcsd.put(eventId, 1L);
                            processedCounter.labels(window.getPage()).inc();
                        }

                    }
                    this.eventsProcessed.put(window, prvPrcsd);
                    List<Long> expectedEvents = windowIdMap.getOrDefault(window, null);
                    if (expectedEvents != null && expectedEvents.size() == this.numEventsPerWindow) {
                        int unprocCount = expectedEvents.size() - prvPrcsd.size();
                        if (unprocCount != 0){
                            LOGGER.info(String.format("****************\nFound unprocessed events for: %s\nCount: %d\n****************", window,unprocCount));
                        }
                        unprocessedEventsCount.put(window, unprocCount);
                        PGVUnprocUpdate pgvUnprocUpdate = new PGVUnprocUpdate(unprocessedEventsCount, this.unprocessedGauge);
                        pgvUnprocUpdate.update();
                    }
                }


//                if (polledMsgs != 0){
//
//                    PGVUnproc pgvUnproc = new PGVUnproc(unprocessedGauge, windowIdMap, eventsProcessed, numEventsPerWindow);
//
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.toString());
        }
    }
}
