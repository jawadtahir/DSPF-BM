/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tum.in.msrg.flink.connector;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.*;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.synchronization.FutureCompletingBlockingQueue;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.connector.kafka.source.enumerator.KafkaSourceEnumState;
import org.apache.flink.connector.kafka.source.enumerator.KafkaSourceEnumStateSerializer;
import org.apache.flink.connector.kafka.source.enumerator.KafkaSourceEnumerator;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.enumerator.subscriber.KafkaSubscriber;
import org.apache.flink.connector.kafka.source.metrics.KafkaSourceReaderMetrics;
import org.apache.flink.connector.kafka.source.reader.KafkaRecordEmitter;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplit;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplitSerializer;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.util.UserCodeClassLoader;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The Source implementation of Kafka. Please use a {@link KafkaSourceBuilder} to construct a {@link
 * CustomKafkaSource}. The following example shows how to create a KafkaSource emitting records of <code>
 * String</code> type.
 *
 * <pre>{@code
 * KafkaSource<String> source = KafkaSource
 *     .<String>builder()
 *     .setBootstrapServers(KafkaSourceTestEnv.brokerConnectionStrings)
 *     .setGroupId("MyGroup")
 *     .setTopics(Arrays.asList(TOPIC1, TOPIC2))
 *     .setDeserializer(new TestingKafkaRecordDeserializationSchema())
 *     .setStartingOffsets(OffsetsInitializer.earliest())
 *     .build();
 * }</pre>
 *
 * <p>See {@link KafkaSourceBuilder} for more details.
 *
 * @param <OUT> the output type of the source.
 */
public class CustomKafkaSource<OUT>
        implements Source<OUT, KafkaPartitionSplit, KafkaSourceEnumState>,
                ResultTypeQueryable<OUT> {
    private static final long serialVersionUID = -8755372893283732098L;
    // Users can choose only one of the following ways to specify the topics to consume from.
    private final KafkaSubscriber subscriber;
    // Users can specify the starting / stopping offset initializer.
    private final OffsetsInitializer startingOffsetsInitializer;
    private final OffsetsInitializer stoppingOffsetsInitializer;
    // Boundedness
    private final Boundedness boundedness;
    private final KafkaRecordDeserializationSchema<OUT> deserializationSchema;
    // The configurations.
    private final Properties props;

    CustomKafkaSource(
            KafkaSubscriber subscriber,
            OffsetsInitializer startingOffsetsInitializer,
            @Nullable OffsetsInitializer stoppingOffsetsInitializer,
            Boundedness boundedness,
            KafkaRecordDeserializationSchema<OUT> deserializationSchema,
            Properties props) {
        this.subscriber = subscriber;
        this.startingOffsetsInitializer = startingOffsetsInitializer;
        this.stoppingOffsetsInitializer = stoppingOffsetsInitializer;
        this.boundedness = boundedness;
        this.deserializationSchema = deserializationSchema;
        this.props = props;
    }

    /**
     * Get a kafkaSourceBuilder to build a {@link CustomKafkaSource}.
     *
     * @return a Kafka source builder.
     */
    public static <OUT> CustomKafkaSourceBuilder<OUT> builder() {
        return new CustomKafkaSourceBuilder<>();
    }

    @Override
    public Boundedness getBoundedness() {
        return this.boundedness;
    }

    @Override
    public SourceReader<OUT, KafkaPartitionSplit> createReader(SourceReaderContext readerContext)
            throws Exception {
        return createReader(readerContext, (ignore) -> {});
    }

    @VisibleForTesting
    SourceReader<OUT, KafkaPartitionSplit> createReader(
            SourceReaderContext readerContext, Consumer<Collection<String>> splitFinishedHook)
            throws Exception {
        FutureCompletingBlockingQueue<RecordsWithSplitIds<ConsumerRecord<byte[], byte[]>>>
                elementsQueue = new FutureCompletingBlockingQueue<>();
        deserializationSchema.open(
                new DeserializationSchema.InitializationContext() {
                    @Override
                    public MetricGroup getMetricGroup() {
                        return readerContext.metricGroup().addGroup("deserializer");
                    }

                    @Override
                    public UserCodeClassLoader getUserCodeClassLoader() {
                        return readerContext.getUserCodeClassLoader();
                    }
                });
        final KafkaSourceReaderMetrics kafkaSourceReaderMetrics =
                new KafkaSourceReaderMetrics(readerContext.metricGroup());

        Meter throughputMeter = readerContext.metricGroup().meter("flinkRecordsConsumed", new MeterView(1));
        Counter outputCounter = readerContext.metricGroup().counter("outputCounterReader");

        Supplier<CustomKafkaPartitionSplitReader> splitReaderSupplier =
                () -> new CustomKafkaPartitionSplitReader(props, readerContext, throughputMeter, kafkaSourceReaderMetrics);
//        KafkaRecordEmitter<OUT> recordEmitter = new KafkaRecordEmitter<>(deserializationSchema);
        CustomKafkaRecordEmitter<OUT> recordEmitter = new CustomKafkaRecordEmitter<>(deserializationSchema, outputCounter);

        return new CustomKafkaSourceReader<>(
                elementsQueue,
                new CustomKafkaSourceFetcherManager(
                        elementsQueue, splitReaderSupplier::get, splitFinishedHook),
                recordEmitter,
                toConfiguration(props),
                readerContext,
                kafkaSourceReaderMetrics);
    }

    @Override
    public SplitEnumerator<KafkaPartitionSplit, KafkaSourceEnumState> createEnumerator(
            SplitEnumeratorContext<KafkaPartitionSplit> enumContext) {
        return new KafkaSourceEnumerator(
                subscriber,
                startingOffsetsInitializer,
                stoppingOffsetsInitializer,
                props,
                enumContext,
                boundedness);
    }

    @Override
    public SplitEnumerator<KafkaPartitionSplit, KafkaSourceEnumState> restoreEnumerator(
            SplitEnumeratorContext<KafkaPartitionSplit> enumContext,
            KafkaSourceEnumState checkpoint)
            throws IOException {
        return new KafkaSourceEnumerator(
                subscriber,
                startingOffsetsInitializer,
                stoppingOffsetsInitializer,
                props,
                enumContext,
                boundedness,
                checkpoint.assignedPartitions());
    }

    @Override
    public SimpleVersionedSerializer<KafkaPartitionSplit> getSplitSerializer() {
        return new KafkaPartitionSplitSerializer();
    }

    @Override
    public SimpleVersionedSerializer<KafkaSourceEnumState> getEnumeratorCheckpointSerializer() {
        return new KafkaSourceEnumStateSerializer();
    }

    @Override
    public TypeInformation<OUT> getProducedType() {
        return deserializationSchema.getProducedType();
    }

    // ----------- private helper methods ---------------

    private Configuration toConfiguration(Properties props) {
        Configuration config = new Configuration();
        props.stringPropertyNames().forEach(key -> config.setString(key, props.getProperty(key)));
        return config;
    }

    @VisibleForTesting
    Configuration getConfiguration() {
        return toConfiguration(props);
    }
}
