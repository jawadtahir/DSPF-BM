package de.tum.in.msrg.kafka;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Wrapper {
    private static String KAFKA_BOOTSTRAP;
    private static String PROCESSING_GUARANTEE;
    private static String APP_ID;
    private static String NUM_STANDBY;
    private static int NUM_STREAMS;

    private static final Logger LOGGER = LogManager.getLogger(Wrapper.class);

    public static void main (String[] args) throws Exception {

        Options cliOpts = createCLI();
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(cliOpts, args);

        KAFKA_BOOTSTRAP = cmd.getOptionValue("kafka", "kafka:9092");
        LOGGER.info(String.format("Kafka bootstrap server: %s", KAFKA_BOOTSTRAP));


        PROCESSING_GUARANTEE = cmd.getOptionValue("guarantee", "exactly_once_beta");
        LOGGER.info(String.format("Configured processing guarantee: %s", PROCESSING_GUARANTEE));

        APP_ID = cmd.getOptionValue("appid", "streams-eventcount");
        LOGGER.info(String.format("Application ID: %s", APP_ID));

        NUM_STANDBY = cmd.getOptionValue("standby", "0");
        LOGGER.info(String.format("Number of standby replicas: %s", NUM_STANDBY));

        NUM_STREAMS = Integer.parseInt(cmd.getOptionValue("streams", "1"));
        LOGGER.info(String.format("Number of streams: %s", NUM_STREAMS));

        if (NUM_STREAMS == 1){
            EventCount eventCount = new EventCount();
            eventCount.run(KAFKA_BOOTSTRAP, PROCESSING_GUARANTEE, APP_ID, NUM_STANDBY);
        } else {
            EventCount1 eventCount1 = new EventCount1();
            eventCount1.run(KAFKA_BOOTSTRAP, PROCESSING_GUARANTEE, APP_ID, NUM_STANDBY);
        }


    }

    private static Options createCLI(){
        Options opts = new Options();

        Option serverOptn = Option.builder("kafka")
                .argName("bootstrap")
                .hasArg()
                .desc("Kafka bootstrap server")
                .build();


        Option pgOptn = Option.builder("guarantee")
                .argName("guarantee")
                .hasArg()
                .desc("Processing guarantee")
                .build();

        Option idOptn = Option.builder("appid")
                .argName("id")
                .hasArg()
                .desc("Application ID")
                .build();

        Option standbyOptn = Option.builder("standby")
                .argName("num")
                .hasArg()
                .desc("Number of standby replicas")
                .build();

        Option streamsOptn = Option.builder("streams")
                .argName("count")
                .hasArg()
                .desc("Number of streams")
                .build();



        opts
                .addOption(serverOptn)
                .addOption(pgOptn)
                .addOption(idOptn)
                .addOption(standbyOptn)
                .addOption(streamsOptn);

        return opts;

    }
}
