package de.tum.in.msrg.latcal;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class PGVNew {
    private static final Logger LOGGER = LogManager.getLogger(PGVNew.class);


    private static String bootstrap;
    private static String reportFolder;
    private static int eventsPerWindow;

    public static void main(String[] args) throws ParseException, IOException {
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(getCLIOptns(), args);

        bootstrap = cmd.getOptionValue("kafka", "kafka1:9092");
        LOGGER.info(String.format("Kafka bootstrap server: %s", bootstrap));

        eventsPerWindow = Integer.parseInt(cmd.getOptionValue("events", "5000"));
        LOGGER.info(String.format("Events per window: %d", eventsPerWindow));


        Properties kafkaProperties = getKafkaProperties();
        kafkaProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);

        HTTPServer promServer = new HTTPServer(52923);
        Gauge unprocessedGauge = Gauge.build("de_tum_in_msrg_pgv_unprocessed", "Unprocessed events").labelNames("key").register();

        Integer cpuCount = Runtime.getRuntime().availableProcessors();
        LOGGER.info(String.format("Found %d cores. Creating thread pool", cpuCount));

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            threadPoolExecutor.shutdown();
            promServer.close();
        }));

        ConcurrentHashMap<PageTSKey, List<Long>> idHashMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<PageTSKey, Map<Long, Long>> processedIdHashMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<PageTSKey, Integer> unprocessedCountMap = new ConcurrentHashMap<>();

        PGVInNew pgvin = new PGVInNew(
                kafkaProperties,
                idHashMap,
                processedIdHashMap,
                unprocessedCountMap,
                unprocessedGauge,
                eventsPerWindow);

        PGVOutNew pgvOut = new PGVOutNew(
                threadPoolExecutor,
                kafkaProperties,
                idHashMap,
                processedIdHashMap,
                unprocessedCountMap,
                unprocessedGauge,
                eventsPerWindow);
//        PGVUnproc pgvUnproc = new PGVUnproc(idHashMap, processedIdHashMap, eventsPerWindow);

        threadPoolExecutor.submit(pgvin);
        threadPoolExecutor.submit(pgvOut);
//        threadPoolExecutor.submit(pgvUnproc);

    }

    protected static Options getCLIOptns(){
        Options cliOptions = new Options();

        Option kafkaOptn = Option.builder("kafka")
                .hasArg(true)
                .argName("bootstrap")
                .desc("Bootstrap kafka server")
                .build();

        Option epwOptn = Option.builder("events")
                .hasArg(true)
                .argName("perWindow")
                .desc("Events per window")
                .build();


        cliOptions.addOption(kafkaOptn);
        cliOptions.addOption(epwOptn);

        return  cliOptions;
    }

    protected static Properties getKafkaProperties(){
        Properties properties = new Properties();

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
//        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,PageStatisticsDeserializer.class.getCanonicalName());
//        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "latcal");

        return properties;
    }
}
