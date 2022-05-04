package de.tum.in.msrg.storm.topology;

import de.tum.in.msrg.storm.bolt.ClickCountWindowBolt;
import de.tum.in.msrg.storm.bolt.KafkaParserBolt;
import de.tum.in.msrg.storm.bolt.StatsToKafkaMapper;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.storm.kafka.bolt.KafkaBolt;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseStatefulWindowedBolt;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;

import java.util.Properties;

public class StormTopology {
    private String kafkaBroker;
    private String inputTopic;
    private String outputTopic;

    public StormTopology(String kafkaBroker, String inputTopic, String outputTopic){
        this.kafkaBroker = kafkaBroker;
        this.inputTopic = inputTopic;
        this.outputTopic = outputTopic;
    }

    public TopologyBuilder getTopologyBuilder(){
        KafkaSpoutConfig<String, String> kafkaSpoutConfig = KafkaSpoutConfig
                .builder(this.kafkaBroker, this.inputTopic)
                .setProcessingGuarantee(KafkaSpoutConfig.ProcessingGuarantee.AT_LEAST_ONCE)
                .setProp("group.id", "strom-clickcount")
                .setProp("auto.offset.reset", "earliest")
                .build();

        KafkaSpout<String, String> kafkaSpout = new KafkaSpout<String, String>(kafkaSpoutConfig);

        KafkaParserBolt parserBolt = new KafkaParserBolt();

        BaseStatefulWindowedBolt windowBolt = new ClickCountWindowBolt()
                .withTumblingWindow(BaseWindowedBolt.Duration.seconds(60))
                .withPersistence()
                .withTimestampField("eventTimestamp");

        KafkaBolt<String, String> kafkaBolt = new KafkaBolt<String, String>()
                .withTopicSelector(this.outputTopic)
                .withTupleToKafkaMapper(new StatsToKafkaMapper())
                .withProducerProperties(getKafkaBoltProps());

        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("storm-kafka-spout", kafkaSpout);

        builder.setBolt("storm-kafka-parser", parserBolt)
                .shuffleGrouping("storm-kafka-spout");

        builder.setBolt("storm-window-bolt", windowBolt, 4)
                .fieldsGrouping("storm-kafka-parser", new Fields("page"));

        builder.setBolt("storm-kafka-bolt", kafkaBolt)
                .shuffleGrouping("storm-window-bolt");

        return builder;
    }

    private Properties getKafkaBoltProps(){
        Properties props = new Properties();

        props.put("bootstrap.servers", this.kafkaBroker);
        props.put("transaction.timeout.ms", "60000");
        props.put("key.serializer", StringSerializer.class.getCanonicalName());
        props.put("value.serializer", StringSerializer.class.getCanonicalName());

        return props;
    }
}
