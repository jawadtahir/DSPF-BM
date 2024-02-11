package de.tum.in.msrg.storm.topology;

import de.tum.in.msrg.storm.bolt.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.storm.bolt.JoinBolt;
import org.apache.storm.kafka.bolt.KafkaBolt;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseStatefulWindowedBolt;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;

import java.util.Properties;

public class StormTopology1 {
    private String kafkaBroker;
    private static final String INPUT_TOPIC = "click";
    private static final String UPDATE_TOPIC = "update";
    private static final String OUTPUT_TOPIC = "output";
    private static final String LATE_TOPIC = "lateOutput";

    public StormTopology1(String kafkaBroker){
        this.kafkaBroker = kafkaBroker;

    }

    public TopologyBuilder getTopologyBuilder(){
        KafkaSpoutConfig<byte[], String> clickSpoutConfig = new KafkaSpoutConfig
                .Builder<byte[], String>(this.kafkaBroker, INPUT_TOPIC)
                .setProcessingGuarantee(KafkaSpoutConfig.ProcessingGuarantee.AT_LEAST_ONCE)
                .setProp("group.id", "click-spout")
                .setProp("auto.offset.reset", "earliest")
                .setProp(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName())
                .setProp(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName())
                .build();

        KafkaSpoutConfig<byte[], String> updateSpoutConfig = new KafkaSpoutConfig
                .Builder<byte[], String>(this.kafkaBroker, UPDATE_TOPIC)
                .setProcessingGuarantee(KafkaSpoutConfig.ProcessingGuarantee.AT_LEAST_ONCE)
                .setProp("group.id", "update-spout")
                .setProp("auto.offset.reset", "earliest")
                .setProp(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName())
                .setProp(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName())
                .build();

        KafkaSpout<byte[], String> clickSpout = new KafkaSpout<byte[], String>(clickSpoutConfig);
        KafkaSpout<byte[], String> updateSpout = new KafkaSpout<byte[], String>(updateSpoutConfig);

        ClickParserBolt clickParserBolt = new ClickParserBolt();
        UpdateParserBolt updateParserBolt = new UpdateParserBolt();

        JoinBolt clickUpdateJoinBolt = new JoinBolt("storm-click-parser", "page")
                .leftJoin("storm-update-parser", "page", "storm-click-parser")
                .select("page,storm-click-parser:eventTimestamp,clickEvent,storm-update-parser:eventTimestamp,updateEvent")
                .withTumblingWindow(BaseWindowedBolt.Duration.seconds(60))
                .withTimestampField("eventTimestamp");
//                .withLateTupleStream("lateJoinEvents");


        BaseWindowedBolt windowBolt = new ClickCountWindowBolt()
                .withTumblingWindow(BaseWindowedBolt.Duration.seconds(60))
                .withLag(BaseWindowedBolt.Duration.of(0))
                .withWatermarkInterval(BaseWindowedBolt.Duration.seconds(1))
//                .withPersistence()
                .withTimestampField("storm-click-parser:eventTimestamp");
//                .withLateTupleStream("lateEvents");


        KafkaBolt<byte[], String> kafkaBolt = new KafkaBolt<byte[], String>()
                .withTopicSelector(OUTPUT_TOPIC)
                .withTupleToKafkaMapper(new StatsToKafkaMapper())
                .withProducerProperties(getKafkaBoltProps());


        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("storm-click-spout", clickSpout, 3);
        builder.setSpout("storm-update-spout", updateSpout, 3);

        builder.setBolt("storm-click-parser", clickParserBolt, 3)
                .fieldsGrouping("storm-click-spout", new Fields("key"));
        builder.setBolt("storm-update-parser", updateParserBolt, 3)
                        .fieldsGrouping("storm-update-spout", new Fields("key"));

        builder.setBolt("storm-clickupdate-join", clickUpdateJoinBolt, 3)
                        .fieldsGrouping("storm-click-parser", new Fields("page"))
                        .fieldsGrouping("storm-update-parser", new Fields("page"));

        builder.setBolt("storm-window-bolt", windowBolt, 3)
                .fieldsGrouping("storm-clickupdate-join", new Fields("page"));

        builder.setBolt("storm-kafka-bolt", kafkaBolt, 3)
                .fieldsGrouping("storm-window-bolt", new Fields("page"));

//        builder.setBolt("storm-late-bolt", lateKafkaBolt)
//                .localOrShuffleGrouping("storm-window-bolt", "lateEvents");
//                .shuffleGrouping("storm-clickupdate-join", "lateJoinEvents");

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
