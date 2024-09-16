package network;

import network.tools.MultiModalNetworkOptimizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkOptimizerTest {
    private final static Logger LOG = LogManager.getLogger(NetworkOptimizerTest.class);

    @Test
    public void testNetworkOptimizer(){
        LOG.info("Testing network optimizer");
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        // Read the MATSim network
        MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario.getNetwork());
        matsimNetworkReader.readFile("../../data/intermediate/test/GemeenteLeuvenTest.xml.gz");
        // Optimize the network
        MultiModalNetworkOptimizer networkOptimizer = new MultiModalNetworkOptimizer(scenario.getNetwork(), 3, 100);
        networkOptimizer.optimize();

        // write the optimized network
        NetworkUtils.writeNetwork(networkOptimizer.getNetwork(), "../../data/intermediate/test/GemeenteLeuvenOptimized.xml.gz");
        networkOptimizer.getNetwork().getLinks().values().forEach(link -> {
            Assertions.assertTrue(link.getLength() >= 3 && link.getLength() <= 100);
        });


    }

}
