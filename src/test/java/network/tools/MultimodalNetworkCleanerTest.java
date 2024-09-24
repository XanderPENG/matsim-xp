package network.tools;

import network.core.TransMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
        Network bikeNetwork = deriveSubNetwork(scenario.getNetwork(), TransMode.Mode.BIKE);
        LOG.info("Network Stats (pre-cleaning): ");
        networkTopoCalculator.run(bikeNetwork);

        MultimodalNetworkOrganizer networkCleaner = new MultimodalNetworkOrganizer(scenario.getNetwork());
        LOG.info("Cleaning the network");
        Assertions.assertDoesNotThrow(() -> networkCleaner.clean(TransMode.Mode.BIKE, Set.of(TransMode.Mode.CAR, TransMode.Mode.PT)));
        LOG.info("Network Stats (post-cleaning): ");
        bikeNetwork = deriveSubNetwork(scenario.getNetwork(), TransMode.Mode.BIKE);
        networkTopoCalculator.run(bikeNetwork);
        //write the cleaned network
//        NetworkUtils.writeNetwork(scenario.getNetwork(), "../../data/intermediate/test/GemeenteLeuvenCleanedV1.xml.gz");
    }

    @Test
    void cleanAllModes(){
        LOG.info("Testing multimodal network cleaner");
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        // Read the MATSim network
        MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario.getNetwork());
        matsimNetworkReader.readFile("../../data/intermediate/test/GemeenteLeuvenOptimizedV2.xml.gz");
        // Network Stats of the bike subnetwork
        Network bikeNetwork = deriveSubNetwork(scenario.getNetwork(), TransMode.Mode.BIKE);
        LOG.info("Network Stats (pre-cleaning): ");
        networkTopoCalculator.run(bikeNetwork);

        MultimodalNetworkOrganizer cleaner = new MultimodalNetworkOrganizer(scenario.getNetwork());
        LOG.info("Cleaning the whole network");
        cleaner.clean(Set.of(TransMode.Mode.BIKE, TransMode.Mode.CAR, TransMode.Mode.PT));
        LOG.info("Network Stats (post-cleaning): ");
        bikeNetwork = deriveSubNetwork(scenario.getNetwork(), TransMode.Mode.BIKE);
        networkTopoCalculator.run(bikeNetwork);
        //write the cleaned network
        NetworkUtils.writeNetwork(scenario.getNetwork(), "../../data/intermediate/test/GemeenteLeuvenCleanedAllModesV1.xml.gz");

    }

    Network deriveSubNetwork(Network network, TransMode.Mode mode) {
        Network subNetwork = NetworkUtils.createNetwork();
        for (Link link : network.getLinks().values()) {
            if (link.getAllowedModes().contains(mode.name)) {
                if (!subNetwork.getNodes().containsValue(link.getFromNode())) {
                    subNetwork.addNode(link.getFromNode());
                }
                if (!subNetwork.getNodes().containsValue(link.getToNode())) {
                    subNetwork.addNode(link.getToNode());
                }
                if (!subNetwork.getLinks().containsValue(link)) {
                    subNetwork.addLink(link);
                }
            }
        }
        return subNetwork;
    }
}