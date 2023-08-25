package de.tum.in.msrg.flink.connector;

import org.apache.flink.api.connector.source.SourceOutput;

import org.apache.flink.connector.base.source.reader.RecordEmitter;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplitState;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Meter;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.IOException;

public class CustomKafkaRecordEmitter<T> implements RecordEmitter<ConsumerRecord<byte[], byte[]>, T, KafkaPartitionSplitState> {

    private final KafkaRecordDeserializationSchema<T> deserializationSchema;
    private final SourceOutputWrapper<T> sourceOutputWrapper = new SourceOutputWrapper<>();
    private final Counter outCounter;


    public CustomKafkaRecordEmitter(KafkaRecordDeserializationSchema<T> deserializationSchema, Counter outCounter) {
        this.deserializationSchema = deserializationSchema;
        this.outCounter = outCounter;
    }

    @Override
    public void emitRecord(
            ConsumerRecord<byte[], byte[]> element,
            SourceOutput<T> output,
            KafkaPartitionSplitState splitState)
            throws Exception {
        try {
            sourceOutputWrapper.setSourceOutput(output);
            sourceOutputWrapper.setTimestamp(element.timestamp());
            deserializationSchema.deserialize(element, sourceOutputWrapper);
            outCounter.inc();
            splitState.setCurrentOffset(element.offset() + 1);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize consumer record due to", e);
        }
    }


    private static class SourceOutputWrapper<T> implements Collector<T> {

        private SourceOutput<T> sourceOutput;
        private long timestamp;

        @Override
        public void collect(T record) {
            sourceOutput.collect(record, timestamp);
        }

        @Override
        public void close() {}

        private void setSourceOutput(SourceOutput<T> sourceOutput) {
            this.sourceOutput = sourceOutput;
        }

        private void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
