package de.tum.in.msrg.storm;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.datamodel.UpdateEvent;
import de.tum.in.msrg.storm.topology.StormTopology;
import de.tum.in.msrg.storm.topology.StormTopology1;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;

public class LocalSubmitter {

    public static void main(String args[]) throws Exception {

        SingleLocalCluster cluster = new SingleLocalCluster();

        Config config = new Config();

        config.registerSerialization(ClickEvent.class);
        config.registerSerialization(UpdateEvent.class);
        config.registerSerialization(PageStatistics.class);
        config.setMessageTimeoutSecs(121);

        cluster.submitTopology(
                "local-storm-click-count",
                config,
                new StormTopology1(
                        "node1:9094")
                .getTopologyBuilder().createTopology());

    }
}
