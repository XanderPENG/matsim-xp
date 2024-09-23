package network.tools;

import network.core.TransMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Set;

class MultimodalNetworkCleanerTest {
    private final Logger LOG = LogManager.getLogger(MultimodalNetworkCleanerTest.class);

    @Test
    void clean() {
        LOG.info("Testing multimodal network cleaner");
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        // Read the MATSim network
        MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario.getNetwork());
        matsimNetworkReader.readFile("../../data/intermediate/test/GemeenteLeuvenTest.xml.gz");
        MultimodalNetworkCleaner networkCleaner = new MultimodalNetworkCleaner(scenario.getNetwork());
        Network connectedNetwork = networkCleaner.clean(Set.of(TransMode.Mode.CAR, TransMode.Mode.BIKE));

    }
}