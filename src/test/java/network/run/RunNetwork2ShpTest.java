package network.run;

import network.gis.Network2SimpleFeaturesTest;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Network;

import static org.junit.jupiter.api.Assertions.*;

class RunNetwork2ShpTest {
    @Test
    void testWriteNetwork2Shp(){
        // Load the test network
        Network network = Network2SimpleFeaturesTest.loadTestNetwork();
        // Write the network to a shapefile
        String file = "src/test/data/test_network.shp";
        assertDoesNotThrow(() -> new network.gis.Network2Shp("", network).write(file));

    }
}
