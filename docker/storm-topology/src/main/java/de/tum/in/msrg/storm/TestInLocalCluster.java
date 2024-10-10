package de.tum.in.msrg.storm;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.datamodel.UpdateEvent;
import de.tum.in.msrg.storm.topology.StormTopology;
import de.tum.in.msrg.storm.topology.StormTopology1;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.serialization.*;
import org.apache.storm.serialization.types.ArrayListSerializer;
import org.apache.storm.serialization.types.HashMapSerializer;
import org.apache.storm.serialization.types.HashSetSerializer;
import org.apache.storm.serialization.types.ListDelegateSerializer;
import org.apache.storm.spout.CheckPointState;
import org.apache.storm.task.WorkerTopologyContext;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.TupleImpl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public class TestInLocalCluster {

    public static void main (String[] args) throws Exception {
        Config configuration = new Config();
        configuration.put(Config.TOPOLOGY_ENABLE_MESSAGE_TIMEOUTS, false);
        configuration.put(Config.TOPOLOGY_TASKS, 3);
//        configuration.put(Config.TOPOLOGY_TESTING_ALWAYS_TRY_SERIALIZE, true);
//        configuration.put(Config.TOPOLOGY_STATE_PROVIDER, "org.apache.storm.redis.state.RedisKeyValueStateProvider");
//        configuration.put(Config.TOPOLOGY_STATE_PROVIDER_CONFIG, "{ \"jedisPoolConfig\":{\"host\":\"localhost\", \"port\":56379}}");
//        configuration.put(Config.TOPOLOGY_SKIP_MISSING_KRYO_REGISTRATIONS, true);
//        configuration.put(Config.TOPOLOGY_FALL_BACK_ON_JAVA_SERIALIZATION, true);
//        configuration.put(Config.TOPOLOGY_STATE_CHECKPOINT_INTERVAL, 1000);

//        configuration.registerSerialization(ClickEvent.class);
//        configuration.registerSerialization(UpdateEvent.class);
//        configuration.registerSerialization(ClickUpdateEvent.class);
//        configuration.registerSerialization(PageStatistics.class);
//        configuration.registerSerialization(Date.class);
//        configuration.registerSerialization(CheckPointState.Action.class);
//        configuration.registerSerialization(TupleImpl.class);
//        configuration.registerSerialization(WorkerTopologyContext.class);
//        configuration.registerSerialization(Fields.class);
//        configuration.registerSerialization(ArrayListSerializer.class);
//        configuration.registerSerialization(HashMapSerializer.class);
//        configuration.registerSerialization(HashSetSerializer.class);
//        configuration.registerSerialization(ListDelegateSerializer.class);
//        configuration.registerSerialization(GzipBridgeThriftSerializationDelegate.class);
//        configuration.registerSerialization(GzipSerializationDelegate.class);
//        configuration.registerSerialization(KryoTupleDeserializer.class);
//        configuration.registerSerialization(KryoTupleSerializer.class);
//        configuration.registerSerialization(SerializableSerializer.class);
//        configuration.registerSerialization(ThriftSerializationDelegate.class);
//        configuration.registerSerialization(ThreadPoolExecutor.class);


        LocalCluster cluster = new LocalCluster.Builder()
                .withSupervisors(3)
                .withPortsPerSupervisor(1)
                .withDaemonConf(configuration)
                .withNimbusDaemon()
                .build();

        try (LocalCluster.LocalTopology topology = cluster.submitTopology("test", configuration, new StormTopology("node1:9094").getTopologyBuilder().createTopology())) {

            System.in.readAllBytes();
        }
        cluster.shutdown();
    }
}
