package de.tum.in.msrg.latcal;


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

    private Map<PageTSKey, List<Long>> windowIdMap;
    private Properties kafkaProperties;

    private static final Logger LOGGER = LogManager.getLogger(PGVOutNew.class);


    public PGVOutNew(Properties kafkaProperties, Map<PageTSKey, List<Long>> windowIdMap){
        this.kafkaProperties = (Properties) kafkaProperties.clone();

        this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvOutputReader");
        this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PageStatisticsDeserializer.class.getCanonicalName());
        LOGGER.info(String.format("Kafka Properties: %s", this.kafkaProperties.toString()));

        this.windowIdMap = windowIdMap;

    }
    @Override
    public void run() {

        LOGGER.info("Running thread...");



        LOGGER.info("Creating kafka consumer");
        try (KafkaConsumer<String, PageStatistics> consumer = new KafkaConsumer<String, PageStatistics>(this.kafkaProperties)) {

            LOGGER.info("Subscribing to the out topic");
            consumer.subscribe(Arrays.asList("output"));


            HashMap<PageTSKey, List<Long>> windowIdMapRecvd = new HashMap<>();

            Counter processedCounter = Counter.build("de_tum_in_msrg_pgv_processed", "Total unique processed events").labelNames("key").register();
            Counter receivedCounter = Counter.build("de_tum_in_msrg_pgv_received", "Total received events").labelNames("key").register();
            Counter duplicateCounter = Counter.build("de_tum_in_msrg_pgv_duplicate", "Duplicate processed events").labelNames("key").register();
            Counter debugCounter = Counter.build("de_tum_in_msrg_pgv_debug", "Wrong events").labelNames("key").register();
//            Counter duplicateWindowCounter = Counter.build("de_tum_in_msrg_pgv_duplicate_window", "Outputs with PB events").labelNames("key").register();
            Counter correctOutputCounter = Counter.build("de_tum_in_msrg_pgv_correct_output", "Correct outputs").labelNames("key").register();
            Counter inCorrectOutputCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_output", "incorrect outputs").labelNames("key").register();
            Counter inCorrectOutputHigherCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_higher_output", "incorrect outputs, higher than expected").labelNames("key").register();
            Counter inCorrectOutputLowerCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_lower_output", "incorrect outputs, lower than expected").labelNames("key").register();
//            Gauge expectedWindowGauge = Gauge.build("de_tum_in_msrg_pgv_expected_windows", "Expected windows").labelNames("key").register();
            Counter receivedWindowCounter = Counter.build("de_tum_in_msrg_pgv_received_windows", "Received windows").labelNames("key").register();
            Gauge unprocessedGauge = Gauge.build("de_tum_in_msrg_pgv_unprocessed", "unprocessed windows").labelNames("key").register();
            Histogram outputCounterHistogram = Histogram.build("de_tum_in_msrg_pgv_output_counter", "Output counter").labelNames("key").linearBuckets(4990,1,11).register();

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
                    if (record.value().getCount() == 5000){
                        correctOutputCounter.labels(window.getPage()).inc();
                    }else {
                        inCorrectOutputCounter.labels(window.getPage()).inc();
                        if (record.value().getCount() > 5000){
                            inCorrectOutputHigherCounter.labels(window.getPage()).inc();
                        }else{
                            inCorrectOutputLowerCounter.labels(window.getPage()).inc();
                        }
                    }

                    LOGGER.debug("Verifying PGs");
                    List<Long> expectedEvents = windowIdMap.getOrDefault(window, null);
                    if (expectedEvents != null){
                        for (Long eventId : record.value().getIds()){
                            receivedCounter.labels(window.getPage()).inc();
                            // First check if the received output IDs can be found in reference output
                            if (expectedEvents.contains(eventId)){
                                // If yes, remove from expected and add it to processed.
                                expectedEvents.remove(eventId);
                                List<Long> prvPrcsd = windowIdMapRecvd.getOrDefault(window, Collections.synchronizedList(new ArrayList<Long>()));
                                prvPrcsd.add(eventId);
                                processedCounter.labels(window.getPage()).inc();
                                windowIdMapRecvd.put(window, prvPrcsd);
                            } else {
                                // if not, see if we have already processed it.
                                List<Long> prvPrcsd = windowIdMapRecvd.getOrDefault(window, null);
                                if (prvPrcsd != null){
                                    if (prvPrcsd.contains(eventId)){
                                        duplicateCounter.labels(window.getPage()).inc();
                                    } else {
                                        // assert. The ID we received is neither in reference implementation nor in previously processed.
                                        debugCounter.labels(window.getPage()).inc();
                                    }
                                }else {
                                    // assert. The ID we received is neither in reference implementation nor in previously processed.
                                    debugCounter.labels(window.getPage()).inc();
                                }
                            }
                        }
                    }
                }



                if (polledMsgs != 0){
                    LOGGER.debug("Calculating unprocessed events...");
//                    Map<String, Long> keyWindowCountMap = new HashMap<String, Long>();
                    Map<String, Long> keyUnprocWindowCountMap = new HashMap<String, Long>();

                    for (Map.Entry<PageTSKey, List<Long>> entry : windowIdMap.entrySet()){
                        List<Long> eventList = entry.getValue();
                        PageTSKey key = entry.getKey();
//                        Long windowCount = keyWindowCountMap.getOrDefault(key.getPage(), 0L);
//                        keyWindowCountMap.put(key.getPage(), windowCount+1);
//                        expectedWindowGauge.labels(key.getPage()).set(windowCount);

                        if (eventList.size() != 0){
                            Long unprocWindowCount = keyUnprocWindowCountMap.getOrDefault(key.getPage(), 0L);
                            unprocWindowCount += 1;
                            keyUnprocWindowCountMap.put(key.getPage(), unprocWindowCount);
                            unprocessedGauge.labels(key.getPage()).set((double) unprocWindowCount);
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.toString());
        }
    }
}
