package network.tools;

import network.core.TransMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hsqldb.persist.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Set;

class MultimodalNetworkCleanerTest {
    private final Logger LOG = LogManager.getLogger(MultimodalNetworkCleanerTest.class);
    NetworkCalcTopoType networkTopoCalculator = new NetworkCalcTopoType();

    @Test
    void clean() {
        LOG.info("Testing multimodal network cleaner");
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        // Read the MATSim network
        MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario.getNetwork());
        matsimNetworkReader.readFile("../../data/intermediate/test/GemeenteLeuvenOptimizedV2.xml.gz");
        // Network Stats of the bike subnetwork
        Network bikeNetwork = deriveBikeNetwork(scenario.getNetwork());
        LOG.info("Network Stats (pre-cleaning): ");
        networkTopoCalculator.run(bikeNetwork);

        MultimodalNetworkOrganizer networkCleaner = new MultimodalNetworkOrganizer(scenario.getNetwork());
        LOG.info("Cleaning the network");
        Assertions.assertDoesNotThrow(() -> networkCleaner.clean(TransMode.Mode.BIKE, Set.of(TransMode.Mode.CAR, TransMode.Mode.PT)));
        LOG.info("Network Stats (post-cleaning): ");
        bikeNetwork = deriveBikeNetwork(scenario.getNetwork());
        networkTopoCalculator.run(bikeNetwork);
        //write the cleaned network
//        NetworkUtils.writeNetwork(scenario.getNetwork(), "../../data/intermediate/test/GemeenteLeuvenCleanedV1.xml.gz");
    }

    Network deriveBikeNetwork(Network network) {
        Network bikeNetwork = NetworkUtils.createNetwork();
        for (Link link : network.getLinks().values()) {
            if (link.getAllowedModes().contains(TransMode.Mode.BIKE.name)) {
                if (!bikeNetwork.getNodes().containsValue(link.getFromNode())) {
                    bikeNetwork.addNode(link.getFromNode());
                }
                if (!bikeNetwork.getNodes().containsValue(link.getToNode())) {
                    bikeNetwork.addNode(link.getToNode());
                }
                if (!bikeNetwork.getLinks().containsValue(link)) {
                    bikeNetwork.addLink(link);
                }
            }
        }
        return bikeNetwork;
    }
}