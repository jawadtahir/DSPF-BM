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
import org.apache.storm.topology.TopologyBuilder;

import java.util.Arrays;

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
        Config config = new Config();

        config.put(Config.NIMBUS_SEEDS, Arrays.asList(nimbus));
        config.registerSerialization(ClickEvent.class);
        config.registerSerialization(UpdateEvent.class);
        config.registerSerialization(ClickUpdateEvent.class);
        config.registerSerialization(PageStatistics.class);
//        config.registerSerialization(ObjectMapper.class);

        return config;
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
