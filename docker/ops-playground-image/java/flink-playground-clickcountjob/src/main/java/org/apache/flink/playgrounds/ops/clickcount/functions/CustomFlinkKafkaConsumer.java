package org.apache.flink.playgrounds.ops.clickcount.functions;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.config.OffsetCommitMode;
import org.apache.flink.streaming.connectors.kafka.internals.AbstractFetcher;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition;
import org.apache.flink.util.SerializedValue;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class CustomFlinkKafkaConsumer<T> extends FlinkKafkaConsumer<T>
{
    private transient Meter throughputMeter;

    public CustomFlinkKafkaConsumer(String topic, DeserializationSchema<T> valueDeserializer, Properties props) {
        super(topic, valueDeserializer, props);
    }

    public CustomFlinkKafkaConsumer(String topic, KafkaDeserializationSchema<T> deserializer, Properties props) {
        super(topic, deserializer, props);
    }

    public CustomFlinkKafkaConsumer(List topics, DeserializationSchema<T> deserializer, Properties props) {
        super(topics, deserializer, props);
    }

    public CustomFlinkKafkaConsumer(List topics, KafkaDeserializationSchema<T> deserializer, Properties props) {
        super(topics, deserializer, props);
    }

    public CustomFlinkKafkaConsumer(Pattern subscriptionPattern, DeserializationSchema<T> valueDeserializer, Properties props) {
        super(subscriptionPattern, valueDeserializer, props);
    }

    public CustomFlinkKafkaConsumer(Pattern subscriptionPattern, KafkaDeserializationSchema<T> deserializer, Properties props) {
        super(subscriptionPattern, deserializer, props);
    }

    @Override
    public void open(Configuration configuration) throws Exception {
        super.open(configuration);
        this.throughputMeter = this.getRuntimeContext().getMetricGroup().meter("flinkRecordsConsumed", new MeterView(1));
    }

    @Override
    protected AbstractFetcher<T, ?> createFetcher(
            SourceContext<T> sourceContext,
            Map<KafkaTopicPartition, Long> assignedPartitionsWithInitialOffsets,
            SerializedValue<WatermarkStrategy<T>> watermarkStrategy,
            StreamingRuntimeContext runtimeContext,
            OffsetCommitMode offsetCommitMode,
            MetricGroup consumerMetricGroup,
            boolean useMetrics)
            throws Exception {

        // make sure that auto commit is disabled when our offset commit mode is ON_CHECKPOINTS;
        // this overwrites whatever setting the user configured in the properties
        adjustAutoCommitConfig(properties, offsetCommitMode);

        return new CustomKafkaFetcher<>(
                sourceContext,
                assignedPartitionsWithInitialOffsets,
                watermarkStrategy,
                runtimeContext.getProcessingTimeService(),
                runtimeContext.getExecutionConfig().getAutoWatermarkInterval(),
                runtimeContext.getUserCodeClassLoader(),
                runtimeContext.getTaskNameWithSubtasks(),
                deserializer,
                properties,
                pollTimeout,
                runtimeContext.getMetricGroup(),
                consumerMetricGroup,
                useMetrics,
                this.throughputMeter);
    }


}
