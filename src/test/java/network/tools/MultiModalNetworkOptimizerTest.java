package network.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

class MultiModalNetworkOptimizerTest {
    private final static Logger LOG = LogManager.getLogger(MultiModalNetworkOptimizerTest.class);

    @Test
    void optimize() {
        LOG.info("Testing network optimizer");
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        // Read the MATSim network
        MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario.getNetwork());
        matsimNetworkReader.readFile("../../data/clean/network/GemeenteLeuvenMultimodalNetwork.xml.gz");
        MultiModalNetworkOptimizer networkOptimizer = new MultiModalNetworkOptimizer.Builder().setNetwork(scenario.getNetwork()).build();
        Assertions.assertDoesNotThrow(networkOptimizer::optimize);
        NetworkUtils.writeNetwork(networkOptimizer.getNetwork(), "../../data/clean/network/GemeenteLeuvenMultimodalNetworkOptimized.xml.gz");
    }
}