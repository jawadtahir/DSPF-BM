package de.tum.in.msrg.latcal;


import de.tum.in.msrg.datamodel.PageStatistics;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class PGVOutNew implements Runnable {

    private Map<PageTSKey, List<Long>> windowIdMap;
    private Properties kafkaProperties;
    private final Path reportRoot;

//    private long processedCounter = 0L;
//    private long duplicateCounter = 0L;

    private static final Logger LOGGER = LogManager.getLogger(PGVOutNew.class);


    public PGVOutNew(Properties kafkaProperties, Map<PageTSKey, List<Long>> windowIdMap, Path reportRoot){
        this.kafkaProperties = (Properties) kafkaProperties.clone();

        this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvOutputReader");
        this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PageStatisticsDeserializer.class.getCanonicalName());
        LOGGER.info(String.format("Kafka Properties: %s", this.kafkaProperties.toString()));

        this.windowIdMap = windowIdMap;

        this.reportRoot = reportRoot;
    }
    @Override
    public void run() {

        LOGGER.info("Running thread...");
        LOGGER.info("Creating kafka consumer");

        Path runDir = Paths.get(reportRoot.toString(), Instant.now().toString());
        Path createdRunDir = null;
        try {
            createdRunDir = Files.createDirectories(runDir);
            LOGGER.info(String.format("Created report folder: %s", createdRunDir.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (
                KafkaConsumer<String, PageStatistics> consumer = new KafkaConsumer<String, PageStatistics>(this.kafkaProperties);
                HTTPServer promServer = new HTTPServer(52923);) {

            LOGGER.info("Subscribing to the out topic");
            consumer.subscribe(Arrays.asList("output"));

            Counter processedCounter = Counter.build("de_tum_in_msrg_pgv_processed", "Total processed events").register();
            Counter duplicateCounter = Counter.build("de_tum_in_msrg_pgv_duplicate", "Duplicate processed events").register();
            Gauge unprocessedGauge = Gauge.build("de_tum_in_msrg_pgv_unprocessed", "unprocessed events").register();

            int polledMsgs = 0;

            while (true) {
                ConsumerRecords<String, PageStatistics> records = consumer.poll(Duration.ofMillis(100));
                polledMsgs = records.count();
                LOGGER.debug(String.format("Polled %d messages", polledMsgs));

                records.forEach((record) -> {
                    PageTSKey window = new PageTSKey(record.value().getPage(), record.value().getWindowStart());
                    List<Long> events = windowIdMap.getOrDefault(window, null);

                    if (events != null){
                        record.value().getIds().forEach((id)->{
                            boolean found = events.remove(id);
                            if (found){
                                processedCounter.inc();
                            }else {
                                duplicateCounter.inc();
                            }
                        });
                    }
                });

                if (polledMsgs != 0){
                    AtomicReference<Long> unp = new AtomicReference<>(0L);
                    windowIdMap.values().forEach((eventList)->{
                        if (eventList.size() != 0){
                            unp.updateAndGet(val -> val + 1);
                        }
                    });
                    unprocessedGauge.set((double) unp.get());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
