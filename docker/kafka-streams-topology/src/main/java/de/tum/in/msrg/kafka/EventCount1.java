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

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import de.tum.in.msrg.kafka.processor.*;
import de.tum.in.msrg.kafka.serdes.ClickEventSerde;
import de.tum.in.msrg.kafka.serdes.ClickUpdateSerdes;
import de.tum.in.msrg.kafka.serdes.PageStatisticsSerdes;
import de.tum.in.msrg.kafka.serdes.UpdateEventSerdes;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.metrics.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class EventCount1 {


    private static final Logger LOGGER = LogManager.getLogger(EventCount1.class);

    public void run(String kafkaBootstrap, String processingGuarantee, String appId, String numStdby) throws Exception {




        Properties props = getProperties();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, processingGuarantee);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "3");
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, appId);
        props.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, numStdby);

        LOGGER.info("Creating custom metrics...");
        Metrics customMetrics = getMetrics();

        LOGGER.info("Creating topology...");
        final Topology topology = getTopology(customMetrics);
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

        LOGGER.info("Starting stream");
        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        LOGGER.info("Kafka Streams shutting down gracefully");
        System.exit(0);
    }

    public static Topology getTopology(Metrics metrics) {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<byte[], ClickEvent> clickStream = builder.stream("click",
                        Consumed.<byte[], String>with(new ClickEventTimeExtractor())
                                .withKeySerde(new Serdes.ByteArraySerde())
                                .withValueSerde(new Serdes.StringSerde()))
                .flatMapValues(new ClickEventMapper(metrics));

        KStream<byte[], UpdateEvent> updateStream = builder.stream("update",
                Consumed.<byte[], String>with(new UpdateEventTimeExtractor())
                        .withKeySerde(new Serdes.ByteArraySerde())
                        .withValueSerde(new Serdes.StringSerde()))
                .flatMapValues(new UpdateEventMapper());

        KStream<byte[], ClickUpdateEvent> joinStream = clickStream.leftJoin(
                updateStream,
                new ClickUpdateJoiner(),
                JoinWindows.ofTimeDifferenceAndGrace(Duration.ofSeconds(60), Duration.ofMillis(2000)),
                StreamJoined.<byte[], ClickEvent, UpdateEvent>with(new Serdes.ByteArraySerde(), new ClickEventSerde(), new UpdateEventSerdes()));



        joinStream
                .groupBy((key, value) -> key, Grouped.with(new Serdes.ByteArraySerde(), new ClickUpdateSerdes()))
                .windowedBy(
                        TimeWindows.ofSizeAndGrace(Duration.of(60, ChronoUnit.SECONDS),
                                Duration.ofMillis(2000)))
                .aggregate(
                        new PageStatisticsInitializer(),
                        new PageStatisticsAggregator(),
                        Materialized.with(
                                new Serdes.ByteArraySerde(),
                                new PageStatisticsSerdes()))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream((k, v) -> {
                    v.setWindowStart(new Date(k.window().start()));
                    v.setWindowEnd(new Date(k.window().end()));
                    return k.key();
                }).to("output", Produced.with(new Serdes.ByteArraySerde(), new PageStatisticsSerdes()));


        Topology topology = builder.build();
        return topology;
    }

    public static Properties getProperties() {
        Properties props = new Properties();

//        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-eventcount");

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, ClickEventTimeExtractor.class.getCanonicalName());
        props.put(StreamsConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG, "1000");
        props.put(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "INFO");
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, "2");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "10737418240");
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "3");

        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 3000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 1000);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kstreams-consumer");

//        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, "0.0.0.0:12346");

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


}
