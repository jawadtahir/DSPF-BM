package de.tum.in.msrg.storm.topology;

import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.storm.bolt.ClickCountWindowBolt;
import de.tum.in.msrg.storm.bolt.ClickParserBolt;
import de.tum.in.msrg.storm.bolt.StatsToKafkaMapper;
import de.tum.in.msrg.storm.bolt.UpdateParserBolt;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.storm.bolt.JoinBolt;
import org.apache.storm.kafka.bolt.KafkaBolt;
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
    private String inputTopic;
    private String updateTopic;
    private String outputTopic;

    public StormTopology(String kafkaBroker, String inputTopic, String updateTopic, String outputTopic){
        this.kafkaBroker = kafkaBroker;
        this.inputTopic = inputTopic;
        this.updateTopic = updateTopic;
        this.outputTopic = outputTopic;
    }

    public TopologyBuilder getTopologyBuilder(){
        KafkaSpoutConfig<String, String> clickSpoutConfig = KafkaSpoutConfig
                .builder(this.kafkaBroker, this.inputTopic)
                .setProcessingGuarantee(KafkaSpoutConfig.ProcessingGuarantee.AT_LEAST_ONCE)
                .setProp("group.id", "click-spout")
                .setProp("auto.offset.reset", "earliest")
                .build();

        KafkaSpoutConfig<String, String> updateSpoutConfig = KafkaSpoutConfig
                .builder(this.kafkaBroker, this.updateTopic)
                .setProcessingGuarantee(KafkaSpoutConfig.ProcessingGuarantee.AT_LEAST_ONCE)
                .setProp("group.id", "update-spout")
                .setProp("auto.offset.reset", "earliest")
                .build();

        KafkaSpout<String, String> clickSpout = new KafkaSpout<String, String>(clickSpoutConfig);
        KafkaSpout<String, String> updateSpout = new KafkaSpout<String, String>(updateSpoutConfig);

        ClickParserBolt clickParserBolt = new ClickParserBolt();
        UpdateParserBolt updateParserBolt = new UpdateParserBolt();

        JoinBolt clickUpdateJoinBolt = new JoinBolt("storm-click-parser", "page")
                .leftJoin("storm-update-parser", "page", "storm-click-parser")
                .select("page,storm-click-parser:eventTimestamp,clickEvent,storm-update-parser:eventTimestamp,updateEvent")
                .withTumblingWindow(BaseWindowedBolt.Duration.seconds(60))
                .withTimestampField("eventTimestamp");

        BaseStatefulWindowedBolt windowBolt = new ClickCountWindowBolt()
                .withTumblingWindow(BaseWindowedBolt.Duration.seconds(60))
                .withPersistence()
                .withTimestampField("storm-click-parser:eventTimestamp");

        KafkaBolt<String, String> kafkaBolt = new KafkaBolt<String, String>()
                .withTopicSelector(this.outputTopic)
                .withTupleToKafkaMapper(new StatsToKafkaMapper())
                .withProducerProperties(getKafkaBoltProps());

        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("storm-click-spout", clickSpout, 3);
        builder.setSpout("storm-update-spout", updateSpout, 3);

        builder.setBolt("storm-click-parser", clickParserBolt, 3)
                .shuffleGrouping("storm-click-spout");
        builder.setBolt("storm-update-parser", updateParserBolt, 3)
                        .shuffleGrouping("storm-update-spout");

        builder.setBolt("storm-clickupdate-join", clickUpdateJoinBolt, 6)
                        .fieldsGrouping("storm-click-parser", new Fields("page"))
                        .fieldsGrouping("storm-update-parser", new Fields("page"));

        builder.setBolt("storm-window-bolt", windowBolt, 6)
                .fieldsGrouping("storm-clickupdate-join", new Fields("page"));

        builder.setBolt("storm-kafka-bolt", kafkaBolt, 3)
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
