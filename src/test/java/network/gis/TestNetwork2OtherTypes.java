package network.gis;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import static org.junit.jupiter.api.Assertions.*;

public class TestNetwork2OtherTypes {

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
        features.forEach(feature -> {
            // Check that each feature has a geometry
            assertNotNull(feature.getDefaultGeometry());
            // Check that each feature has correct attributes
            assertNotNull(feature.getAttribute("linkId"));
            assertNotNull(feature.getAttribute("capacity"));
            assertNotNull(feature.getAttribute("freespeed"));
            assertNotNull(feature.getAttribute("length"));
            assertNotNull(feature.getAttribute("lanes"));
            assertNotNull(feature.getAttribute("modes"));

        });
    }

    @Test
    void testConvertToGeoJson() {
        // Load a test network
        Network network = loadTestNetwork();
        // Convert the network to GeoJson
        Network2GeoJson network2GeoJson = new Network2GeoJson("", network);

        assertDoesNotThrow(() -> network2GeoJson.write("../data/testNetwork2Others/test_equil.geojson"));
    }

    @Test
    void testConvertToShapefile() {
        // Load a test network
        Network network = loadTestNetwork();
        // Convert the network to Shapefile
        Network2Shp network2Shp = new Network2Shp("", network);

        network2Shp.write("../data/testNetwork2Others/test_equil.shp");
    }

    public static Network loadTestNetwork() {
        // Read the matsim "equil" network from a test file
        String testNetworkFile = "../data/testNetwork2Others/test_equil_raw.xml";
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(testNetworkFile);
        return scenario.getNetwork();
    }
}