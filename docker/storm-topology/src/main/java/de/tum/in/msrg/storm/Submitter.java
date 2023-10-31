package de.tum.in.msrg.storm;


import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.datamodel.UpdateEvent;
import de.tum.in.msrg.storm.topology.StormTopology;
import de.tum.in.msrg.storm.topology.StormTopology1;
import org.apache.commons.cli.*;
import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.serialization.*;
import org.apache.storm.serialization.types.ArrayListSerializer;
import org.apache.storm.serialization.types.HashMapSerializer;
import org.apache.storm.serialization.types.HashSetSerializer;
import org.apache.storm.serialization.types.ListDelegateSerializer;
import org.apache.storm.spout.CheckPointState;
import org.apache.storm.task.WorkerTopologyContext;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.TupleImpl;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Hello world!
 *
 */
public class Submitter
{
    public static void main( String[] args ) throws ParseException, AuthorizationException, InvalidTopologyException, AlreadyAliveException {

        Options cliOpts = createOptns();
        DefaultParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(cliOpts, args);

        Config stormConfig = getStormConfig(cmdLine.getOptionValue("nimbus", "nimbus"));

        String kafkaBroker = cmdLine.getOptionValue("kafka", "kafka:9092");
        Integer numStreams = Integer.parseInt(cmdLine.getOptionValue("streams", "1")) ;

        TopologyBuilder tpBuilder = new StormTopology(kafkaBroker).getTopologyBuilder();

        if (numStreams == 1){
            tpBuilder = new StormTopology(kafkaBroker).getTopologyBuilder();
        } else {
            tpBuilder = new StormTopology1(kafkaBroker).getTopologyBuilder();
        }



        StormSubmitter.submitTopologyWithProgressBar("storm-click-count-job", stormConfig, tpBuilder.createTopology());



    }

    public static Config getStormConfig(String nimbus){
        Config configuration = new Config();

        configuration.put(Config.NIMBUS_SEEDS, Arrays.asList(nimbus));
        configuration.registerSerialization(ClickEvent.class);
        configuration.registerSerialization(UpdateEvent.class);
        configuration.registerSerialization(ClickUpdateEvent.class);
        configuration.registerSerialization(PageStatistics.class);
        configuration.registerSerialization(Date.class);
        configuration.registerSerialization(CheckPointState.Action.class);
        configuration.registerSerialization(TupleImpl.class);
        configuration.registerSerialization(WorkerTopologyContext.class);
        configuration.registerSerialization(Fields.class);
        configuration.registerSerialization(ArrayListSerializer.class);
        configuration.registerSerialization(HashMapSerializer.class);
        configuration.registerSerialization(HashSetSerializer.class);
        configuration.registerSerialization(ListDelegateSerializer.class);
        configuration.registerSerialization(GzipBridgeThriftSerializationDelegate.class);
        configuration.registerSerialization(GzipSerializationDelegate.class);
        configuration.registerSerialization(KryoTupleDeserializer.class);
        configuration.registerSerialization(KryoTupleSerializer.class);
        configuration.registerSerialization(SerializableSerializer.class);
        configuration.registerSerialization(ThriftSerializationDelegate.class);
        configuration.registerSerialization(ThreadPoolExecutor.class);
//        config.registerSerialization(ObjectMapper.class);

        return configuration;
    }

    public static Options createOptns(){
        Options opts = new Options();

        Option nimbusOpt = Option.builder("nimbus")
                .argName("seed")
                .hasArg()
                .desc("Nimbus seed")
                .build();

        Option kafkaOpt = Option.builder("kafka")
                .argName("seed")
                .hasArg()
                .desc("Kafka bootstrap server")
                .build();

        Option streamsOptn = Option.builder("streams")
                .argName("count")
                .hasArg()
                .desc("Number of streams")
                .build();


        opts.addOption(kafkaOpt)
                .addOption(nimbusOpt)
                .addOption(streamsOptn);

        return opts;

    }

}
