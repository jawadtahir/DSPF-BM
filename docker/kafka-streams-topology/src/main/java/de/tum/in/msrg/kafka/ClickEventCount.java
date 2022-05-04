package de.tum.in.msrg.kafka;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.kafka.processor.ClickEventProcessorSupplier;
import de.tum.in.msrg.kafka.processor.ClickEventTimeExtractor;
import org.apache.commons.cli.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.TimestampedWindowStore;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;

public class ClickEventCount {
    private static String KAFKA_BOOTSTRAP;
    private static String INPUT_TOPIC;
    private static String OUTPUT_TOPIC;

    private static void main(String[] args) throws ParseException {
        Options cliOpts = createCLI();
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(cliOpts, args);

        KAFKA_BOOTSTRAP = cmd.getOptionValue("kafka", "kafka:9092");
        INPUT_TOPIC = cmd.getOptionValue("input", "input");
        OUTPUT_TOPIC = cmd.getOptionValue("output", "output");


        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-eventcount");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StoreBuilder<WindowStore<String, List<ClickEvent>>> groupByStoreBuilder = Stores.windowStoreBuilder(Stores.persistentWindowStore("windowed-groupby-store", Duration.of(90, ChronoUnit.SECONDS), Duration.of(60, ChronoUnit.SECONDS), false), Serdes.String(), Serdes.);


        Stores.persistentTimestampedWindowStore(
                "windowed-groupby-store",
                Duration.of(120, ChronoUnit.SECONDS),
                Duration.of(60, ChronoUnit.SECONDS),
                false);

        Topology topology = new Topology();
        topology.addSource(
                Topology.AutoOffsetReset.EARLIEST,
                "kafka-stream-source",
                new ClickEventTimeExtractor(),
                new StringDeserializer(),
                new StringDeserializer(),
                INPUT_TOPIC);

        topology.addProcessor("kafka-stream-stateless", new ClickEventProcessorSupplier(), "kafka-stream-source");


    }





    private static Options createCLI(){
        Options opts = new Options();

        Option serverOptn = Option.builder("kafka")
                .argName("bootstrap")
                .hasArg()
                .desc("Kafka bootstrap server")
                .build();

        Option inTopicOptn = Option.builder("input")
                .argName("topic")
                .hasArg()
                .desc("Input topic for the stream")
                .build();

        Option outTopicOptn = Option.builder("output")
                .argName("topic")
                .hasArg()
                .desc("Output topic for the stream")
                .build();

        opts
                .addOption(serverOptn)
                .addOption(inTopicOptn)
                .addOption(outTopicOptn);

        return opts;

    }
}
