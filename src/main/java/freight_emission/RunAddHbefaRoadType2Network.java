package freight_emission;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

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

    void outputNetwork(String path){
        new NetworkWriter(network).write(path);
    }

    public static void main(String[] args) {
        RunAddHbefaRoadType2Network addHbefaRoadType2Network = new RunAddHbefaRoadType2Network();
        addHbefaRoadType2Network.run("../../data/clean/network/GemeenteLeuvenOptimized.xml.gz");
        addHbefaRoadType2Network.outputNetwork("../../data/intermediate/test/GemeenteLeuvenWithHbefaType.xml.gz");
    }
}
