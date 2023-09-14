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

public class PGVOutNew implements Runnable {

    private final Map<PageTSKey, List<Long>> windowIdMap;
    private final Properties kafkaProperties;
    private final int numEventsPerWindow;
    private final Map<PageTSKey, List<Long>> eventsProcessed;

    private static final Logger LOGGER = LogManager.getLogger(PGVOutNew.class);


    public PGVOutNew(Properties kafkaProperties, Map<PageTSKey, List<Long>> windowIdMap, Map<PageTSKey, List<Long>> processedWindowIdMap, int numEventsPerWindow){
        this.kafkaProperties = (Properties) kafkaProperties.clone();

        this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvOutputReader");
        this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PageStatisticsDeserializer.class.getCanonicalName());
        LOGGER.info(String.format("Kafka Properties: %s", this.kafkaProperties.toString()));

        this.windowIdMap = windowIdMap;
        this.eventsProcessed = processedWindowIdMap;
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
            Counter lateCounter = Counter.build("de_tum_in_msrg_pgv_late", "Dropped events due to late arrival").labelNames("key").register();
            Counter correctOutputCounter = Counter.build("de_tum_in_msrg_pgv_correct_output", "Correct outputs").labelNames("key").register();
            Counter inCorrectOutputCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_output", "incorrect outputs").labelNames("key").register();
            Counter inCorrectOutputHigherCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_higher_output", "incorrect outputs, higher than expected").labelNames("key").register();
            Counter inCorrectOutputLowerCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_lower_output", "incorrect outputs, lower than expected").labelNames("key").register();
            Counter receivedWindowCounter = Counter.build("de_tum_in_msrg_pgv_received_windows", "Received windows").labelNames("key").register();
//            Gauge unprocessedGauge = Gauge.build("de_tum_in_msrg_pgv_unprocessed", "Unprocessed events").labelNames("key").register();
            Histogram outputCounterHistogram = Histogram.build("de_tum_in_msrg_pgv_output_counter", "Output counter").labelNames("key").linearBuckets(4990,1,11).register();

            new Thread(() -> {
                this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvLateOutputReader");
                this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ClickUpdateEventDeserializer.class.getCanonicalName());

                try (KafkaConsumer<String, ClickUpdateEvent> lateConsumer = new KafkaConsumer<String, ClickUpdateEvent>(this.kafkaProperties)){
                    lateConsumer.subscribe(Arrays.asList("lateOutput"));
                    while (true){
                        ConsumerRecords<String, ClickUpdateEvent> records = lateConsumer.poll(Duration.ofMillis(100));
                        for (ConsumerRecord<String, ClickUpdateEvent> lateRecord : records) {
                                PageTSKey windowKey = new PageTSKey(lateRecord.value().getPage(), lateRecord.value().getTimestamp());
                                Long eventId = lateRecord.value().getId();
                                lateCounter.labels(windowKey.getPage()).inc();
                                receivedCounter.labels(windowKey.getPage()).inc();
                                List<Long> prvPrcsd = this.eventsProcessed.getOrDefault(windowKey, Collections.synchronizedList(new ArrayList<Long>()));
                                if (prvPrcsd.contains(eventId)) {
                                    duplicateCounter.labels(windowKey.getPage()).inc();
                                } else {
                                    prvPrcsd.add(lateRecord.value().getId());
                                    this.eventsProcessed.put(windowKey, prvPrcsd);
                                    processedCounter.labels(windowKey.getPage()).inc();
                                }
                        }
                    }
                }
            }).start();

            int polledMsgs = 0;
            LOGGER.info("Created probes...");

             while (true) {
                 HashMap<PageTSKey, Integer> duplicateOutputKeys = new HashMap<>();
                ConsumerRecords<String, PageStatistics> records = consumer.poll(Duration.ofMillis(100));
                polledMsgs = records.count();
                LOGGER.debug(String.format("Polled %d messages", polledMsgs));

                for (ConsumerRecord<String, PageStatistics> record: records){

                    PageTSKey window = new PageTSKey(record.value().getPage(), record.value().getWindowStart());
                    LOGGER.debug(String.format("Processing: %s", record.value().toString()));
                    LOGGER.debug(String.format("Window Key: %s", window.toString()));

                    receivedWindowCounter.labels(record.value().getPage()).inc();

                    LOGGER.debug("Verifying correctness");
                    outputCounterHistogram.labels(window.getPage()).observeWithExemplar(record.value().getCount(), Map.of("start", record.value().getWindowStart().toString()));
                    if (record.value().getCount() == this.numEventsPerWindow){
                        correctOutputCounter.labels(window.getPage()).inc();
                    }else {
                        inCorrectOutputCounter.labels(window.getPage()).inc();
                        if (record.value().getCount() > this.numEventsPerWindow){
                            inCorrectOutputHigherCounter.labels(window.getPage()).inc();
                        }else{
                            inCorrectOutputLowerCounter.labels(window.getPage()).inc();
                        }
                    }

                    LOGGER.debug("Verifying PGs");
//                    List<Long> expectedEvents = windowIdMap.getOrDefault(window, null);
                        List<Long> prvPrcsd = this.eventsProcessed.getOrDefault(window, Collections.synchronizedList(new ArrayList<Long>()));

//                    assert expectedEvents != null;
                        for (Long eventId : record.value().getIds()) {
                            receivedCounter.labels(window.getPage()).inc();
//                        assert expectedEvents.contains(eventId);
                            if (prvPrcsd.contains(eventId)) {
                                duplicateCounter.labels(window.getPage()).inc();
                            } else {
                                prvPrcsd.add(eventId);
                                this.eventsProcessed.put(window, prvPrcsd);
                                processedCounter.labels(window.getPage()).inc();
                            }

                        }
                }



//                if (polledMsgs != 0){

//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.toString());
        }
    }
}
