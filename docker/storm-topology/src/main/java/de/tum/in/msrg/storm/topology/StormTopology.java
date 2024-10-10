package de.tum.in.msrg.storm.topology;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.storm.bolt.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.storm.bolt.JoinBolt;
import org.apache.storm.kafka.bolt.KafkaBolt;
import org.apache.storm.kafka.spout.FirstPollOffsetStrategy;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.state.KeyValueState;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseStatefulWindowedBolt;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;

import java.util.Properties;

public class StormTopology {
    private String kafkaBroker;
    private static final String INPUT_TOPIC = "click";
    private static final String OUTPUT_TOPIC = "output";
    private static final String LATE_TOPIC = "lateOutput";

    public StormTopology(String kafkaBroker){
        this.kafkaBroker = kafkaBroker;

    }

    public TopologyBuilder getTopologyBuilder(){
        KafkaSpoutConfig<byte[], String> clickSpoutConfig = new  KafkaSpoutConfig.Builder<byte[], String>(kafkaBroker, INPUT_TOPIC)
                .setProcessingGuarantee(KafkaSpoutConfig.ProcessingGuarantee.AT_LEAST_ONCE)
                .setFirstPollOffsetStrategy(FirstPollOffsetStrategy.EARLIEST)
                .setProp(ConsumerConfig.GROUP_ID_CONFIG, "click-spout")
                .setProp(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getCanonicalName())
                .setProp(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName())
                .build();


        KafkaSpout<byte[], String> clickSpout = new KafkaSpout<byte[], String>(clickSpoutConfig);

        ClickUpdateJoinParserBolt clickUpdateJoinParserBolt = new ClickUpdateJoinParserBolt();



        BaseWindowedBolt windowBolt = new ClickWindowBoltTest()
                .withTumblingWindow(BaseWindowedBolt.Duration.seconds(60))
                .withTimestampField("eventTimestamp");


        KafkaBolt<byte[], String> kafkaBolt = new KafkaBolt<byte[], String>()
                .withTopicSelector(OUTPUT_TOPIC)
                .withTupleToKafkaMapper(new StatsToKafkaMapper())
                .withProducerProperties(getKafkaBoltProps());

//        KafkaBolt<byte[], String> lateKafkaBolt = new KafkaBolt<byte[], String>()
//                .withTopicSelector(LATE_TOPIC)
//                .withTupleToKafkaMapper(new LateToKafkaMapper())
//                .withProducerProperties(getKafkaBoltProps());

        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("storm-click-spout", clickSpout, 3);

        builder.setBolt("storm-click-parser", clickUpdateJoinParserBolt, 3)
                .fieldsGrouping("storm-click-spout", new Fields("key"));


        builder.setBolt("storm-window-bolt", windowBolt, 3)
                .fieldsGrouping("storm-click-parser", new Fields("page"));

        builder.setBolt("storm-kafka-bolt", kafkaBolt, 3)
                .fieldsGrouping("storm-window-bolt", new Fields("page"));

//        builder.setBolt("storm-late-bolt", lateKafkaBolt, 3)
//                .shuffleGrouping("storm-window-bolt", "lateEvents");

        return builder;
    }

    private Properties getKafkaBoltProps(){
        Properties props = new Properties();

        props.put("bootstrap.servers", this.kafkaBroker);
        props.put("transaction.timeout.ms", "3000");
        props.put("key.serializer", ByteArraySerializer.class.getCanonicalName());
        props.put("value.serializer", StringSerializer.class.getCanonicalName());

        return props;
    }
}
