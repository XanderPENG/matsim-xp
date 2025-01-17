package network.gis;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import static org.junit.jupiter.api.Assertions.*;

public class Network2SimpleFeaturesTest {

    @Test
    void testConvertToSimpleFeatures() {
        // Load a test network
        Network network = loadTestNetwork();
        // Convert the network to SimpleFeatures
        Network2SimpleFeatures network2SimpleFeatures = new Network2SimpleFeatures("", network);
        // Convert the network to SimpleFeatures
        var features = network2SimpleFeatures.convertToSimpleFeatures();
        // Check that the number of features is correct
        assertEquals(25, features.size());

    }

    public static Network loadTestNetwork() {
        // Read the matsim "equil" network from a test file
        String testNetworkFile = "src/test/data/test_network.xml";
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(testNetworkFile);
        return scenario.getNetwork();
    }
}