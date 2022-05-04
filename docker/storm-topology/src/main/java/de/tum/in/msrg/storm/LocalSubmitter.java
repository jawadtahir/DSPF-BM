package de.tum.in.msrg.storm;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickEventStatistics;
import de.tum.in.msrg.storm.topology.StormTopology;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;

public class LocalSubmitter {

    public static void main(String args[]) throws Exception {

        LocalCluster cluster = new LocalCluster();

        Config config = new Config();

        config.registerSerialization(ClickEvent.class);
        config.registerSerialization(ClickEventStatistics.class);

        cluster.submitTopology(
                "local-storm-click-count",
                config,
                new StormTopology(
                        "localhost:9092",
                        "input",
                        "output")
                .getTopologyBuilder().createTopology());

    }
}
