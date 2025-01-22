package network.run;

import network.config.NetworkConverterConfigGroup;
import network.core.NetworkConverter;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.io.NetworkWriter;

/**
 * Run this class to convert the multimodal network to the MATSim network, based on the config file.
 */
class RunNetworkConversion {
    public static String configUrl = "../../data/clean/network/GemeenteLeuvenMultimodalNetworkConverterConfig.xml";

    public static void main(String[] args) {
        NetworkConverterConfigGroup config = NetworkConverterConfigGroup.loadConfigFile(configUrl);

        NetworkConverter networkConverter = new NetworkConverter(config);
        networkConverter.convert();
        Network network = networkConverter.getNetwork();
        new NetworkWriter(network).write(config.OUTPUT_NETWORK_FILE);

    }
}
