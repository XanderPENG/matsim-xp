package network.readers;

import network.core.NetworkElement;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
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

        // Check the attribute of link 20
        NetworkElement.Link link20 = reader.getRawLinks().get("20");
        assertNotNull(link20);
        assertEquals("12", link20.getFromNode().getId());
        assertEquals("13", link20.getToNode().getId());

        assertEquals("1", link20.getKeyValuePairs().get("lanes"));
        assertEquals("27.78", link20.getKeyValuePairs().get("maxspeed"));
        assertEquals("36000", link20.getKeyValuePairs().get("capacity"));
        assertEquals("10000", link20.getKeyValuePairs().get("length"));
        assertEquals("bike, car, pt", link20.getKeyValuePairs().get("modes"));


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