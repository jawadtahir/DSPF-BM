package de.tum.in.msrg.flink.functions;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaFetcher;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartitionState;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.util.SerializedValue;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CustomKafkaFetcher<T> extends KafkaFetcher<T> {

//    private final AtomicLong recordCounter;
    private  transient Meter customMeter;



    public CustomKafkaFetcher(SourceFunction.SourceContext<T> sourceContext, Map<KafkaTopicPartition, Long> assignedPartitionsWithInitialOffsets, SerializedValue<WatermarkStrategy<T>> watermarkStrategy, ProcessingTimeService processingTimeProvider, long autoWatermarkInterval, ClassLoader userCodeClassLoader, String taskNameWithSubtasks, KafkaDeserializationSchema<T> deserializer, Properties kafkaProperties, long pollTimeout, MetricGroup subtaskMetricGroup, MetricGroup consumerMetricGroup, boolean useMetrics, Meter meter) throws Exception {
        super(sourceContext,
                assignedPartitionsWithInitialOffsets,
                watermarkStrategy,
                processingTimeProvider,
                autoWatermarkInterval,
                userCodeClassLoader,
                taskNameWithSubtasks,
                deserializer,
                kafkaProperties,
                pollTimeout,
                subtaskMetricGroup,
                consumerMetricGroup,
                useMetrics);
//        this.recordCounter = new AtomicLong(0);
        this.customMeter = meter;
    }


    @Override
    protected void partitionConsumerRecordsHandler(List<ConsumerRecord<byte[], byte[]>> partitionRecords, KafkaTopicPartitionState<T, TopicPartition> partition) throws Exception {
//        recordCounter.set(partitionRecords.size());
        super.partitionConsumerRecordsHandler(partitionRecords, partition);
        this.customMeter.markEvent(partitionRecords.size());
    }

//    public AtomicLong getRecordCounter() {
//        return recordCounter;
//    }
}