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

import de.tum.in.msrg.kafka.processor.ClickEventMapper;
import de.tum.in.msrg.kafka.processor.ClickEventTimeExtractor;
import de.tum.in.msrg.kafka.serdes.ClickEventSerde;
import org.apache.commons.cli.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

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

    public static void main(String[] args) throws Exception {

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
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, ClickEventTimeExtractor.class.getCanonicalName());

        final StreamsBuilder builder = new StreamsBuilder();


        builder.<String, String>stream(INPUT_TOPIC, Consumed.with(new ClickEventTimeExtractor()))
               .flatMapValues(new ClickEventMapper())
                .groupBy((key, value) -> value.getPage(), Grouped.with(Serdes.String(), new ClickEventSerde()))
                .windowedBy(TimeWindows.of(Duration.of(60, ChronoUnit.SECONDS))).count().toStream();
//                .aggregate()
//               .groupBy((key, value) -> value)
//               .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("counts-store"))
//               .toStream()
//               .to(OUTPUT_TOPIC,  Produced.with(Serdes.String(), Serdes.Long()));

        final Topology topology = builder.build();
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
