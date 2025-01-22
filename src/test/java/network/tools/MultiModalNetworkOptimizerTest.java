package network.tools;

import network.core.TransMode;
import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MultiModalNetworkOptimizerTest {

    private final String NETWORK_FILE = "../data/testNetwork2Others/test_equil_raw.xml";

    @Test
    void testOptimize() {

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        // Read the MATSim network
        MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario.getNetwork());
        matsimNetworkReader.readFile(NETWORK_FILE);
        Network inputNetwork = scenario.getNetwork();

        MultiModalNetworkOptimizer networkOptimizer = new MultiModalNetworkOptimizer.Builder()
                .setNetwork(inputNetwork)
                .setMinThreshold(3000)
                .setMaxThreshold(15000)  // Set the max length threshold of link to 15000, and thus the links longer than 15000 will be split into smaller links
                .build();
        Set<TransMode.Mode> allModes = Set.of(TransMode.Mode.BIKE, TransMode.Mode.CAR, TransMode.Mode.PT, TransMode.Mode.OTHER);
        networkOptimizer.optimize(allModes);

        Network optimizedNetwork = networkOptimizer.getNetwork();

        /*
          The network should have 17 nodes and 27 links after optimization,
          as the long link (link 22) with original length 35000 will be split into 3 links
          since the max length threshold is set to 15000
         */
        assertEquals(17, optimizedNetwork.getNodes().size());
        assertEquals(27, optimizedNetwork.getLinks().size());

        //write the cleaned network
//        NetworkUtils.writeNetwork(optimizedNetwork, "../data/testNetwork2Others/test_equil_optimized.xml");


    }
}