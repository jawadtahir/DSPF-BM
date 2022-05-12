/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tum.in.msrg.kafka;

import de.tum.in.msrg.datamodel.ClickEventStatistics;
import de.tum.in.msrg.kafka.processor.ClickEventMapper;
import de.tum.in.msrg.kafka.processor.ClickEventStatsAggregator;
import de.tum.in.msrg.kafka.processor.ClickEventStatsInitializer;
import de.tum.in.msrg.kafka.processor.ClickEventTimeExtractor;
import de.tum.in.msrg.kafka.serdes.ClickEventSerde;
import de.tum.in.msrg.kafka.serdes.ClickEventStatsSerde;
import org.apache.commons.cli.*;
import org.apache.kafka.common.metrics.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * In this example, we implement a simple WordCount program using the high-level Streams DSL
 * that reads from a source topic "streams-plaintext-input", where the values of messages represent lines of text,
 * split each text line into words and then compute the word occurence histogram, write the continuous updated histogram
 * into a topic "streams-wordcount-output" where each record is an updated count of a single word.
 */
public class EventCount {

    private static String KAFKA_BOOTSTRAP;
    private static String INPUT_TOPIC;
    private static String OUTPUT_TOPIC;
    private static String PROCESSING_GUARANTEE;

    public static void main(String[] args) throws Exception {

        Options cliOpts = createCLI();
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(cliOpts, args);

        KAFKA_BOOTSTRAP = cmd.getOptionValue("kafka", "kafka:9092");
        INPUT_TOPIC = cmd.getOptionValue("input", "input");
        OUTPUT_TOPIC = cmd.getOptionValue("output", "output");
        PROCESSING_GUARANTEE = cmd.getOptionValue("guarantee", "exactly_once_beta");


        Properties props = getProperties();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, PROCESSING_GUARANTEE);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "3");

        Metrics customMetrics = getMetrics();


        final Topology topology = getTopology(INPUT_TOPIC, OUTPUT_TOPIC, customMetrics);
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);



        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

    public static Topology getTopology(String input, String output, Metrics metrics) {
        StreamsBuilder builder = new StreamsBuilder();


        builder.<String, String>stream(input, Consumed.with(new ClickEventTimeExtractor()))
               .flatMapValues(new ClickEventMapper(metrics))
                .groupBy((key, value) -> value.getPage(), Grouped.with(Serdes.String(), new ClickEventSerde()))
                .windowedBy(TimeWindows.of(Duration.of(60, ChronoUnit.SECONDS)).grace(Duration.ofMillis(0)))
                .aggregate(new ClickEventStatsInitializer(), new ClickEventStatsAggregator(),
                        Materialized.with(new Serdes.StringSerde(), new ClickEventStatsSerde()))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream().selectKey((key, value) -> {
                    value.setWindowStart(new Date(key.window().start()));
                    value.setWindowEnd(new Date(key.window().end()));
                    return value.getPage();
                })
                .to(output, Produced.with(new Serdes.StringSerde(), new ClickEventStatsSerde()));

        Topology topology = builder.build();
        return topology;
    }

    public static Properties getProperties() {
        Properties props = new Properties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-eventcount");

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, ClickEventTimeExtractor.class.getCanonicalName());
        props.put(StreamsConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG, "1000");

        return props;
    }

    public static Metrics getMetrics(){
        Metrics metrics = null;
        MetricConfig metricConfig = new MetricConfig();
        metricConfig.timeWindow(1, TimeUnit.SECONDS);
        KafkaMetricsContext metricsContext = new KafkaMetricsContext("de.tum.in.msrg.kafka");
        List<MetricsReporter> reporters = Arrays.asList(new JmxReporter());


        metrics = new Metrics(metricConfig, reporters, Time.SYSTEM, metricsContext);
        return metrics;
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

        Option pgOptn = Option.builder("guarantee")
                .argName("guarantee")
                .hasArg()
                .desc("Processing guarantee")
                .build();

        opts
                .addOption(serverOptn)
                .addOption(inTopicOptn)
                .addOption(outTopicOptn)
                .addOption(pgOptn);

        return opts;

    }
}
