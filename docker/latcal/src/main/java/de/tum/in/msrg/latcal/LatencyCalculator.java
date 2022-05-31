package de.tum.in.msrg.latcal;

import de.tum.in.msrg.datamodel.PageStatistics;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Hello world!
 *
 */
public class LatencyCalculator
{

    private static final String TOPIC = "output";
    private static final Logger LOGGER = LogManager.getLogger(LatencyCalculator.class);
    protected static Map<PageTSKey, Date> pageWindowInsertionTime = new ConcurrentHashMap<>();

    private static String bootstrap;

    public static void main( String[] args ) throws ParseException, InterruptedException {
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(getCLIOptns(), args);

        bootstrap = cmd.getOptionValue("kafka", "kafka1:9092");
        LOGGER.info(String.format("Kafka bootstrap server: %s", bootstrap));

        Properties kafkaProperties = getKafkaProperties();
        kafkaProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);

        StartTimeReader startTimeReader = new StartTimeReader(kafkaProperties, pageWindowInsertionTime);
        EndTimeReader endTimeReader = new EndTimeReader(kafkaProperties, pageWindowInsertionTime);


        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

        threadPoolExecutor.submit(startTimeReader);
        threadPoolExecutor.submit(endTimeReader);


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            threadPoolExecutor.shutdown();
        }));

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