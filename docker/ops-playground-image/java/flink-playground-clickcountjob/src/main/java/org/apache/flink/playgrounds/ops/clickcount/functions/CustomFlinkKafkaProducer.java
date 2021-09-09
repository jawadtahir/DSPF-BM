package org.apache.flink.playgrounds.ops.clickcount.functions;

import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventStatistics;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaException;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

public class CustomFlinkKafkaProducer<IN>  extends FlinkKafkaProducer<IN> {

    private transient Meter throughputMeter;
    private transient Date windowDate;
    private transient Date arrivalTime;

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomFlinkKafkaProducer.class);

    public CustomFlinkKafkaProducer(String brokerList, String topicId, SerializationSchema<IN> serializationSchema) {
        super(brokerList, topicId, serializationSchema);
    }

    public CustomFlinkKafkaProducer(String topicId, SerializationSchema<IN> serializationSchema, Properties producerConfig) {
        super(topicId, serializationSchema, producerConfig);
    }

    public CustomFlinkKafkaProducer(String topicId, SerializationSchema<IN> serializationSchema, Properties producerConfig, Optional<FlinkKafkaPartitioner<IN>> customPartitioner) {
        super(topicId, serializationSchema, producerConfig, customPartitioner);
    }

    public CustomFlinkKafkaProducer(String topicId, SerializationSchema<IN> serializationSchema, Properties producerConfig, @Nullable FlinkKafkaPartitioner<IN> customPartitioner, Semantic semantic, int kafkaProducersPoolSize) {
        super(topicId, serializationSchema, producerConfig, customPartitioner, semantic, kafkaProducersPoolSize);
    }

    public CustomFlinkKafkaProducer(String brokerList, String topicId, KeyedSerializationSchema<IN> serializationSchema) {
        super(brokerList, topicId, serializationSchema);
    }

    public CustomFlinkKafkaProducer(String topicId, KeyedSerializationSchema<IN> serializationSchema, Properties producerConfig) {
        super(topicId, serializationSchema, producerConfig);
    }

    public CustomFlinkKafkaProducer(String topicId, KeyedSerializationSchema<IN> serializationSchema, Properties producerConfig, Semantic semantic) {
        super(topicId, serializationSchema, producerConfig, semantic);
    }

    public CustomFlinkKafkaProducer(String defaultTopicId, KeyedSerializationSchema<IN> serializationSchema, Properties producerConfig, Optional<FlinkKafkaPartitioner<IN>> customPartitioner) {
        super(defaultTopicId, serializationSchema, producerConfig, customPartitioner);
    }

    public CustomFlinkKafkaProducer(String defaultTopicId, KeyedSerializationSchema<IN> serializationSchema, Properties producerConfig, Optional<FlinkKafkaPartitioner<IN>> customPartitioner, Semantic semantic, int kafkaProducersPoolSize) {
        super(defaultTopicId, serializationSchema, producerConfig, customPartitioner, semantic, kafkaProducersPoolSize);
    }

    public CustomFlinkKafkaProducer(String defaultTopic, KafkaSerializationSchema<IN> serializationSchema, Properties producerConfig, Semantic semantic) {
        super(defaultTopic, serializationSchema, producerConfig, semantic);
    }

    public CustomFlinkKafkaProducer(String defaultTopic, KafkaSerializationSchema<IN> serializationSchema, Properties producerConfig, Semantic semantic, int kafkaProducersPoolSize) {
        super(defaultTopic, serializationSchema, producerConfig, semantic, kafkaProducersPoolSize);
    }

    @Override
    public void open(Configuration configuration) throws Exception {
        super.open(configuration);
        this.throughputMeter = getRuntimeContext().getMetricGroup().meter("customNumRecordsIn", new MeterView(1));
        this.arrivalTime = Date.from(Instant.now());
        getRuntimeContext().getMetricGroup().gauge("endToEndLatency", new Gauge<Long>() {
            @Override
            public Long getValue() {
                Long latency = arrivalTime.getTime() - windowDate.getTime();
                return latency;
            }
        });
    }

    @Override
    public void invoke(KafkaTransactionState transaction, IN next, Context context) throws FlinkKafkaException {
        super.invoke(transaction, next, context);
        this.arrivalTime = Date.from(Instant.now());
        this.throughputMeter.markEvent();
        this.windowDate = ((ClickEventStatistics) next).getFirstMsgTS();
    }
}
