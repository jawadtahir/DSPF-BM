package de.tum.in.msrg.storm;

import de.tum.in.msrg.storm.bolt.ClickCountWindowBolt;
import de.tum.in.msrg.storm.bolt.ParserBolt;
import de.tum.in.msrg.storm.bolt.StatsToKafkaMapper;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventStatistics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.kafka.bolt.KafkaBolt;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;


import java.util.Arrays;
import java.util.Properties;

public class StormTopology {
    private String broker = "kafka:9092";
    private static final String INPUT_TOPIC = "input";

    public StormTopology(String broker){
        if (broker != null && broker.length() > 0){
            this.broker = broker;
        }
    }

    public TopologyBuilder build(){

        TopologyBuilder tpBuilder = new TopologyBuilder();
        KafkaSpoutConfig<String, String> kafkaConfig = KafkaSpoutConfig.builder(broker, INPUT_TOPIC)
                .setProcessingGuarantee(KafkaSpoutConfig.ProcessingGuarantee.AT_LEAST_ONCE)
                .setProp(ConsumerConfig.GROUP_ID_CONFIG, "strom-clickcount")
                .setProp(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .build();

        KafkaSpout<String, String> kafkaSpout = new KafkaSpout<>(kafkaConfig);
        ParserBolt parserBolt = new ParserBolt();
        ClickCountWindowBolt clickCountWindowBolt = (ClickCountWindowBolt) new ClickCountWindowBolt()
                .withTumblingWindow(BaseWindowedBolt.Duration.seconds(60))
                .withPersistence()
                .withTimestampField("timestamp")
                .withLag(BaseWindowedBolt.Duration.of(200));

        KafkaBolt<String, String> kafkaBolt = new KafkaBolt<String, String>()
                .withTupleToKafkaMapper(new StatsToKafkaMapper())
                .withTopicSelector("output")
                .withProducerProperties(getKafkaProdConfig());

        tpBuilder.setSpout("storm-kafka-spout", kafkaSpout);

        tpBuilder.setBolt("storm-parser-bolt", parserBolt)
                .shuffleGrouping("storm-kafka-spout");

        tpBuilder.setBolt("storm-window", clickCountWindowBolt)
                .fieldsGrouping("storm-parser-bolt", new Fields("page"));

        tpBuilder.setBolt("storm-kafka-bolt", kafkaBolt)
                .shuffleGrouping("storm-window");


        return tpBuilder;
    }

    private Properties getKafkaProdConfig() {
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.broker);
        props.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, "60000");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());

        return props;
    }

    public static void main( String[] args){
        Config conf = new Config();

        conf.put(Config.NIMBUS_SEEDS, Arrays.asList(args[1]));
        conf.put(Config.NIMBUS_THRIFT_PORT, 6627);
        conf.put(Config.TOPOLOGY_FALL_BACK_ON_JAVA_SERIALIZATION, Boolean.TRUE);
        conf.registerSerialization(ClickEventStatistics.class);
        conf.registerSerialization(ClickEvent.class);


        StormTopology topology = new StormTopology(args[0]);
        try {
            StormSubmitter.submitTopologyWithProgressBar("click-count-job", conf, topology.build().createTopology());
        } catch (AlreadyAliveException | AuthorizationException | InvalidTopologyException e) {
            e.printStackTrace();
        }
    }
}
