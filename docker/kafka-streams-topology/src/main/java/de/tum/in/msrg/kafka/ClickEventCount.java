package de.tum.in.msrg.kafka;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.kafka.processor.ClickEventProcessorSupplier;
import de.tum.in.msrg.kafka.processor.ClickEventTimeExtractor;
import de.tum.in.msrg.kafka.processor.GroupByProcessor;
import de.tum.in.msrg.kafka.serdes.ClickEventListSerde;
import de.tum.in.msrg.kafka.serdes.ClickEventSerde;
import de.tum.in.msrg.kafka.serdes.ClickEventStatsSerde;
import org.apache.commons.cli.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.TimestampedWindowStore;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

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

        Properties properties = getProps();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP);

        Topology topology = createTopology(INPUT_TOPIC, OUTPUT_TOPIC);

        KafkaStreams kafkaStreams = new KafkaStreams(topology, properties);

        final CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(()-> {
            kafkaStreams.close();
            latch.countDown();
        }, "streams-shutdown"));

        try {
            kafkaStreams.start();
            latch.await();
        } catch (InterruptedException e) {
            System.exit(1);
        }

        System.exit(0);

    }

    public static Properties getProps(){
        Properties props = new Properties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-eventcount");

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        return props;
    }

    public static Topology createTopology(String input, String output){

        Topology topology = new Topology();

        StoreBuilder<WindowStore<String, ClickEvent>> groupByStoreBuilder = Stores.windowStoreBuilder(
                Stores.persistentWindowStore(
                        "windowed-groupby-store",
                        Duration.of(90, ChronoUnit.SECONDS),
                        Duration.of(60, ChronoUnit.SECONDS),
                        false),
                Serdes.String(),
                new ClickEventSerde()).withLoggingEnabled(new HashMap<>());

        topology.addSource(
                Topology.AutoOffsetReset.EARLIEST,
                "kafka-stream-source",
                new ClickEventTimeExtractor(),
                new StringDeserializer(),
                new StringDeserializer(),
                input);

        topology.addProcessor(
                "kafka-stream-stateless",
                new ClickEventProcessorSupplier(),
                "kafka-stream-source");
        topology.addProcessor(
                "kafka-stream-windowing",
                GroupByProcessor::new,
                "kafka-stream-stateless");

        topology.addStateStore(groupByStoreBuilder, "kafka-stream-windowing");

        topology.addSink(
                "kafka-stream-sink",
                output,
                new StringSerializer(),
                new ClickEventStatsSerde(),"kafka-stream-windowing");

        return topology;
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
