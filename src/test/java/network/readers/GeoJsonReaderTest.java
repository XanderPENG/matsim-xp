package network.readers;

import network.core.NetworkElement;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.api.data.DataStoreFinder;
//import org.geotools.data.DataStoreFinder;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GeoJsonReaderTest {

    final String networkFile = "../data/testNetwork2Others/test_equil.geojson";

    @Test
    void testReadGeoJson() {
        GeoJsonReader reader = new GeoJsonReader("");
        reader.read(networkFile);

        assertEquals(25, reader.getRawLinks().size());
        assertEquals(15, reader.getRawNodes().size());

        // Check the attribute of link 1
        NetworkElement.Link link1 = reader.getRawLinks().values().stream().filter(link -> link.getKeyValuePairs().get("linkId").equals("1")).findFirst().orElse(null);
        assertNotNull(link1);
        // Since the reader will index the nodes from 0, the node id in the shapefile will be 1 less than the node id in the geojson file
        assertEquals("0", link1.getFromNode().getId());
        assertEquals("1", link1.getToNode().getId());

        assertEquals("2.0", link1.getKeyValuePairs().get("lanes"));
        assertEquals("27.78", link1.getKeyValuePairs().get("freespeed"));
        assertEquals("36000.0", link1.getKeyValuePairs().get("capacity"));
        assertEquals("10000.0", link1.getKeyValuePairs().get("length"));
        assertEquals("bike, car, pt", link1.getKeyValuePairs().get("modes"));


    }

    @Test
    void test(){
        Iterator<DataStoreFactorySpi> availableFactories = DataStoreFinder.getAvailableDataStores();
        while (availableFactories.hasNext()) {
            DataStoreFactorySpi factory = availableFactories.next();
            System.out.println(factory.getDisplayName());
        }
    }
}