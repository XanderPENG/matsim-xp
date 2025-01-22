package network.run;

import network.gis.Network2GeoJson;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

@Deprecated
public class RunNetwork2GeoJson {
    public static void main(String[] args) {
        String inputNetworkFile = "../../data/intermediate/test/freight_emission/GemeenteLeuvenWithHbefaType.xml.gz";
        // Read the network
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(inputNetworkFile);
        Network network = scenario.getNetwork();
        // convert the network to WGS84
        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(
                "EPSG:31370", "EPSG:4326");
        new NetworkTransform(transformation).run(network);
        Network2GeoJson network2GeoJson = new Network2GeoJson("EPSG:4326", network);
        network2GeoJson.write("../../data/intermediate/test/freight_emission/GemeenteLeuvenWithHbefaTypeWgs84.geojson.gz");

    }
}
