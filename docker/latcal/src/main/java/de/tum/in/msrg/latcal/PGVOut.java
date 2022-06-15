package de.tum.in.msrg.latcal;


import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.file.Paths.get;

public class PGVOut implements Runnable {

    private Map<Long, Boolean> idMap;
    private Properties kafkaProperties;
    private final Path reportRoot;

    private long processedCounter = 0L;
    private long duplicateCounter = 0L;

    private static final Logger LOGGER = LogManager.getLogger(PGVOut.class);


    public PGVOut (Properties kafkaProperties, Map idMap, Path reportRoot){
        this.kafkaProperties = (Properties) kafkaProperties.clone();

        this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvOutputReader");
        this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PageStatisticsDeserializer.class.getCanonicalName());
        LOGGER.info(String.format("Kafka Properties: %s", this.kafkaProperties.toString()));

        this.idMap = idMap;

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

        try (KafkaConsumer<String, PageStatistics> consumer = new KafkaConsumer<String, PageStatistics>(this.kafkaProperties)) {

            LOGGER.info("Subscribing to the out topic");
            consumer.subscribe(Arrays.asList("output"));

            int polledMsgs = 0;

            while (true) {
                ConsumerRecords<String, PageStatistics> records = consumer.poll(Duration.ofMillis(100));
                polledMsgs = records.count();
                LOGGER.debug(String.format("Polled %d messages", polledMsgs));

                records.forEach((record) -> {
                    record.value().getIds().forEach((statID) -> {
                        if (this.idMap.remove(statID) == null){
                            duplicateCounter++;
                        }else {
                            processedCounter++;
                        }

                    });
                });

                if (polledMsgs != 0){
                    long unprocessedCounter = idMap.size();
                    FileWriter fileWriter = new FileWriter(createdRunDir.resolve("pgvreport.txt").toFile(), true);
                    BufferedWriter writer = new BufferedWriter(fileWriter);

                    LOGGER.info(String.format("Writing stats to %s", createdRunDir.resolve("pgvreport.txt").toString()));
                    LOGGER.info(String.format("Processed: %d", processedCounter));
                    LOGGER.info(String.format("Duplicates: %d", duplicateCounter));
                    LOGGER.info(String.format("Unprocessed: %d", unprocessedCounter));

                    writer.write(String.format("Processed: %d\n", processedCounter));
                    writer.write(String.format("Duplicates: %d\n", duplicateCounter));
                    writer.write(String.format("Unprocessed: %d\n", unprocessedCounter));

                    writer.flush();
                    writer.close();

                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
