package network.run;

import network.config.NetworkConverterConfigGroup;
import network.core.NetworkConverter;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.io.NetworkWriter;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RunNetworkConversionTest {

    @Test
    void testRunNetworkConversion() {
        // Load test config file
        String configUrl = "../data/testOsmReader/config.xml";
        NetworkConverterConfigGroup config = NetworkConverterConfigGroup.loadConfigFile(configUrl);

        // Run network conversion
        NetworkConverter networkConverter = new NetworkConverter(config);
        Network network = networkConverter.convert();

        // Check the network
        //  The reader will process bidirectional links, so that there should be 23 links in total
        // Also, the Node 7 will be removed as it is not connected to any link, and thus there will be 14 nodes in total
        assertEquals(23, network.getLinks().size());
        assertEquals(14, network.getNodes().size());
        // Check the attribute of link reversed link 6 -> link "6_r"
        assertEquals(Id.createNodeId("12"), network.getLinks().get(Id.createLinkId("6_r_0")).getFromNode().getId());
        assertEquals(Id.createNodeId("2"), network.getLinks().get(Id.createLinkId("6_r_0")).getToNode().getId());
        assertTrue(network.getLinks().get(Id.createLinkId("6_r_0")).getAllowedModes().contains("car"));
        // Check the allowed modes of link 1
        assertTrue(network.getLinks().get(Id.createLinkId("1_0")).getAllowedModes().containsAll(Set.of("car", "bike", "pt")));

        assertDoesNotThrow(() -> new NetworkWriter(network).write(config.OUTPUT_NETWORK_FILE));

    }
}
