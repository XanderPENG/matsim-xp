package freight_emission;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashSet;
import java.util.Set;

class RunAddHbefaRoadType2Network {
    private MultiModalNetwork2HbefaMapping hbefaRoadTypeMapping = new MultiModalNetwork2HbefaMapping();
    private Network network;

    void run(String path){
        // Read the raw network
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(path);
        network = scenario.getNetwork();

        // Add the HBEFA road type to the network
        hbefaRoadTypeMapping.addHbefaMappings(network);
    }

     static Network deriveSubNetwork(Network network, String mode) {
        Network subNetwork = NetworkUtils.createNetwork();
        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
        filter.filter(subNetwork, Set.of(mode));
        return subNetwork;
    }

    void addMode(String diffusionMode, String toMode){
        for (Link link: network.getLinks().values()){
            if (link.getAllowedModes().contains(toMode)){
                Set<String> newAllowedModes = new HashSet<>(link.getAllowedModes());
                newAllowedModes.add(diffusionMode);
                link.setAllowedModes(newAllowedModes);
            }
        }
    }

    void outputNetwork(String path){
        new NetworkWriter(network).write(path);
    }

    public static void main(String[] args) {
        RunAddHbefaRoadType2Network addHbefaRoadType2Network = new RunAddHbefaRoadType2Network();
        addHbefaRoadType2Network.run("../../data/clean/network/GemeenteLeuvenMultimodalNetworkOptimized.xml.gz");

//        Network carNetworkWithHbefaRoadType = deriveSubNetwork(addHbefaRoadType2Network.network, TransportMode.car);
//        new NetworkWriter(carNetworkWithHbefaRoadType).write("../../data/intermediate/test/freightEmissions/carGemeenteLeuvenWithHbefaType.xml.gz");

//        addHbefaRoadType2Network.addMode(TransportMode.car, TransportMode.bike);  // add car mode to bike mode links
//        addHbefaRoadType2Network.outputNetwork("../../data/intermediate/test/freightEmissions/diffusedGemeenteLeuvenWithHbefaType.xml.gz");

        addHbefaRoadType2Network.outputNetwork("../../data/intermediate/test/freightEmissions/GemeenteLeuvenWithHbefaType.xml.gz");
    }
}
