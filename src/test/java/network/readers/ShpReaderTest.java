package network.readers;

import network.core.NetworkElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShpReaderTest {
    final String networkFile = "../data/testNetwork2Others/test_equil.shp";

    @Test
    void testReadShp() {
        ShpReader reader = new ShpReader("");
        reader.read(networkFile);

        assertEquals(25, reader.getRawLinks().size());
        assertEquals(15, reader.getRawNodes().size());

        // Check the attribute of link 20
        NetworkElement.Link link20 = reader.getRawLinks().values().stream().filter(link -> link.getKeyValuePairs().get("linkId").equals("20")).findFirst().orElse(null);
        assertNotNull(link20);

        // Since the geojson reader will index the nodes from 0, the node id in the shapefile will be 1 less than the node id in the geojson file
        assertEquals("11", link20.getFromNode().getId());
        assertEquals("12", link20.getToNode().getId());

        assertEquals("2.0", link20.getKeyValuePairs().get("lanes"));
        assertEquals("27.78", link20.getKeyValuePairs().get("freespeed"));
        assertEquals("36000.0", link20.getKeyValuePairs().get("capacity"));
        assertEquals("10000.0", link20.getKeyValuePairs().get("length"));
        assertEquals("bike, car, pt", link20.getKeyValuePairs().get("modes"));
    }

}