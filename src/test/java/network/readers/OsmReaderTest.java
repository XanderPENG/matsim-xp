package network.readers;

import network.config.NetworkConverterConfigGroup;
import network.core.NetworkElement;
import network.core.TransMode;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OsmReaderTest {
    @Test
    void testReadPbfNetworkWithoutPT(){
        OsmReader reader = new OsmReader();
        reader.read("../data/testOsmReader/test_equil.pbf");
        Map<String, NetworkElement.Node> rawNodes =  reader.getRawNodes();
        Map<String, NetworkElement.Link> rawLinks =  reader.getRawLinks();

        // The reader will not process bidirectional links
        assertEquals(22, rawLinks.size());
        assertEquals(15, rawNodes.size());

        // Check the attribute of link 16
        NetworkElement.Link link16 = rawLinks.get("16");
        assertNotNull(link16);
        assertEquals("8", link16.getFromNode().getId());
        assertEquals("12", link16.getToNode().getId());
        assertEquals("1", link16.getKeyValuePairs().get("lanes"));
        assertEquals("100", link16.getKeyValuePairs().get("maxspeed"));
        assertEquals("500", link16.getKeyValuePairs().get("capacity"));
    }

    @Test
    void testReadPbfNetworkWithPT(){
        Set<Map<String, String>> ptModeKeyValuePairs = Set.of(Map.of("route", "bus"));
        Set<String> ptReservedKeyValues = Set.of("type");
        OsmReader reader = new OsmReader(ptModeKeyValuePairs, ptReservedKeyValues);
        reader.read("../data/testOsmReader/test_equil.pbf");
        Map<String, NetworkElement.Link> rawLinks =  reader.getRawLinks();

        // Check the number of links (should be 22 as it won't add additional links for PT-supported links)
        assertEquals(22, rawLinks.size());

        // check the attribute of link 16
        NetworkElement.Link link16 = rawLinks.get("16");
        assertNotNull(link16);

        assertEquals("8", link16.getFromNode().getId());
        assertEquals("12", link16.getToNode().getId());
        assertTrue(link16.getAllowedModes().contains(TransMode.Mode.PT));
        assertEquals("route", link16.getKeyValuePairs().get("type"));
    }

    @Test
    void testReadPbfNetworkViaConfig(){
        NetworkConverterConfigGroup config = NetworkConverterConfigGroup.loadConfigFile("../data/testOsmReader/testConfig.xml");
        OsmReader reader = new OsmReader(config);
        reader.read(config.INPUT_NETWORK_FILE);

        Map<String, NetworkElement.Node> rawNodes =  reader.getRawNodes();
        Map<String, NetworkElement.Link> rawLinks =  reader.getRawLinks();

        assertEquals(22, rawLinks.size());
        assertEquals(15, rawNodes.size());

        // Check the attribute of link 16
        NetworkElement.Link link16 = rawLinks.get("16");
        assertNotNull(link16);
        assertEquals("8", link16.getFromNode().getId());
        assertEquals("12", link16.getToNode().getId());
        assertTrue(link16.getAllowedModes().contains(TransMode.Mode.PT));
        assertEquals("1", link16.getKeyValuePairs().get("lanes"));
        assertEquals("100", link16.getKeyValuePairs().get("maxspeed"));
        assertEquals("500", link16.getKeyValuePairs().get("capacity"));
    }
}