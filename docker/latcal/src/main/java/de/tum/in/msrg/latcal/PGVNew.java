package de.tum.in.msrg.latcal;

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
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class PGVNew {
    private static final Logger LOGGER = LogManager.getLogger(PGVNew.class);


    private static String bootstrap;
    private static String reportFolder;

    public static void main(String[] args) throws ParseException, IOException {
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(getCLIOptns(), args);

        bootstrap = cmd.getOptionValue("kafka", "kafka1:9092");
        LOGGER.info(String.format("Kafka bootstrap server: %s", bootstrap));


        Properties kafkaProperties = getKafkaProperties();
        kafkaProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);

        HTTPServer promServer = new HTTPServer(52923);

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            threadPoolExecutor.shutdown();
            promServer.close();
        }));

        ConcurrentHashMap<PageTSKey, List<Long>> idHashMap = new ConcurrentHashMap<>();

        PGVInNew pgvin = new PGVInNew(kafkaProperties, idHashMap);
        PGVOutNew pgvOut = new PGVOutNew(kafkaProperties, idHashMap);

        threadPoolExecutor.submit(pgvin);
        threadPoolExecutor.submit(pgvOut);

    }

    protected static Options getCLIOptns(){
        Options cliOptions = new Options();

        Option kafkaOptn = Option.builder("kafka")
                .hasArg(true)
                .argName("bootstrap")
                .desc("Bootstrap kafka server")
                .build();


        cliOptions.addOption(kafkaOptn);

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
