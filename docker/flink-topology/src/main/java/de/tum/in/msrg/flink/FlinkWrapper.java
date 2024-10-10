package de.tum.in.msrg.flink;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class FlinkWrapper {

    private static final Logger LOGGER = LogManager.getLogger(FlinkWrapper.class);

    public static void main (String[] args) throws Exception {
        LOGGER.info(Arrays.deepToString(args));
        System.out.printf("Arguments are %s%n", Arrays.deepToString(args));
        DefaultParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(getCliOptns(), args);

        String kafka = cmdLine.getOptionValue("kafka", "kafka1:9092");
        LOGGER.info(String.format("kafka: %s", kafka));

        Integer numStream = Integer.parseInt(cmdLine.getOptionValue("streams", "1"));
        LOGGER.info(String.format("Number of streams to consume: %d", numStream));

        String pg = cmdLine.getOptionValue("pg", "e1");
        LOGGER.info(String.format("kafka: %s", kafka));

        if (numStream == 1) {
            new App(kafka, pg).run();
        } else {
            new App1(kafka, pg).run();
        }


    }

    private static Options getCliOptns(){
        Options options = new Options();

        options.addOption(
                Option.builder("kafka")
                        .hasArg(true).argName("bootstrap")
                        .desc("Kafka bootstrap server")
                        .build());

        options.addOption(
                Option.builder("streams")
                        .hasArg(true).argName("numStreams")
                        .desc("Number of streams")
                        .build());

        options.addOption(
                Option.builder("pg")
                        .hasArg(true).argName("guarantee")
                        .desc("processing guarantee")
                        .build());

        return options;
    }
}
