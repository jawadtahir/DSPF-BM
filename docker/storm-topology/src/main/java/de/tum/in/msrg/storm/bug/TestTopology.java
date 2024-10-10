package de.tum.in.msrg.storm.bug;


import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.task.WorkerTopologyContext;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.TupleImpl;


public class TestTopology {
    public static void main (String[] args) throws Exception {
        Config config = new Config();
        config.put(Config.TOPOLOGY_TESTING_ALWAYS_TRY_SERIALIZE, true);
        config.put(Config.TOPOLOGY_FALL_BACK_ON_JAVA_SERIALIZATION, true);
        config.registerSerialization(TupleImpl.class);
        config.registerSerialization(WorkerTopologyContext.class);
        config.registerSerialization(Fields.class);
        LocalCluster cluster = new LocalCluster();
        try (LocalCluster.LocalTopology topology = cluster.submitTopology("testTopology", config, getTopology().createTopology())) {
            Thread.sleep(50000);}
        cluster.shutdown();
    }
    static TopologyBuilder getTopology(){
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("eventSpout", new LateEventSpout());
        builder.setBolt("windowBolt", new WindowBolt().withTumblingWindow(BaseWindowedBolt.Duration.seconds(10)).
                        withTimestampField("time").
                        withLateTupleStream("lateEvents")).
                        shuffleGrouping("eventSpout");
        builder.setBolt("latePrintBolt", new LatePrintBolt()).
                        shuffleGrouping("windowBolt", "lateEvents");
        builder.setBolt("printBolt", new PrintBolt()).shuffleGrouping("windowBolt");
        return builder;
    }
}
