package network.run;

import network.config.NetworkConverterConfigGroup;
import network.core.NetworkConverter;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.io.NetworkWriter;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RunNetworkConversionTest {

    @Test
    void testRunNetworkConversionFromOsmFile() {
        // Load test config file
        String configUrl = "../data/testRunNetworkConversionFromOsmFile/testConfig.xml";
        NetworkConverterConfigGroup config = NetworkConverterConfigGroup.loadConfigFile(configUrl);

        // Run network conversion
        NetworkConverter networkConverter = new NetworkConverter(config);
        networkConverter.convert();
        Network network = networkConverter.getNetwork();

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

        // Write the network to a matsim xml, shapefile and geojson
        networkConverter.writeNetwork();
    }

    @Test
    void testRunNetworkConversionFromShp(){
        // Load test config file
        String configUrl = "../data/testRunNetworkConversionFromShp/testConfig.xml";
        NetworkConverterConfigGroup config = NetworkConverterConfigGroup.loadConfigFile(configUrl);

        // Run network conversion
        NetworkConverter networkConverter = new NetworkConverter(config);
        networkConverter.convert();
        Network network = networkConverter.getNetwork();

        // Check the network
        //  The reader will process bidirectional links, so that there should be 23 links in total
        // Also, the Node 7 will be removed as it is not connected to any link, and thus there will be 14 nodes in total
        assertEquals(23, network.getLinks().size());
        assertEquals(14, network.getNodes().size());
        // Check the attribute of link reversed link 6 -> link "6_r"
        Link link6_r  = network.getLinks().values().stream().filter(link -> link.getAttributes().getAttribute("linkId").equals("6_r_0")).findFirst().orElse(null);
        assertNotNull(link6_r);
        assertTrue(link6_r.getAllowedModes().contains(TransportMode.car));

        // Write the network to a matsim xml, shapefile and geojson
        networkConverter.writeNetwork();
    }

    @Test
    void testRunNetworkConversionFromGeojson(){
        // Load test config file
        String configUrl = "../data/testRunNetworkConversionFromGeoJson/testConfig.xml";
        NetworkConverterConfigGroup config = NetworkConverterConfigGroup.loadConfigFile(configUrl);

        // Run network conversion
        NetworkConverter networkConverter = new NetworkConverter(config);
        networkConverter.convert();
        Network network = networkConverter.getNetwork();

        // Check the network
        //  The reader will process bidirectional links, so that there should be 23 links in total
        // Also, the Node 7 will be removed as it is not connected to any link, and thus there will be 14 nodes in total
        assertEquals(23, network.getLinks().size());
        assertEquals(14, network.getNodes().size());
        // Check the attribute of link 23
        Link link23  = network.getLinks().values().stream().filter(link -> link.getAttributes().getAttribute("linkId").equals("23_0")).findFirst().orElse(null);
        assertNotNull(link23);
        assertTrue(link23.getAllowedModes().containsAll(Set.of(TransportMode.car, TransportMode.bike, TransportMode.pt)));

        // Write the network to a matsim xml, shapefile and geojson
        networkConverter.writeNetwork();
    }
}
