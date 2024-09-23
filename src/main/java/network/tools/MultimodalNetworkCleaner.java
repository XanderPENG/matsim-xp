package network.tools;

import network.core.TransMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;

import java.util.Set;

public class MultimodalNetworkCleaner {
    private final Network network;
    private final NetworkCleaner networkCleaner = new NetworkCleaner();


    public MultimodalNetworkCleaner(Network network) {
        this.network = network;
    }

    private Network clean(TransMode.Mode mode) {
        // create a new  network with only the links that match the given mode
        Network tmpNetwork = NetworkUtils.createNetwork();
        for (Link link : this.network.getLinks().values()) {
            if (link.getAllowedModes().contains(mode.name)) {
                tmpNetwork.addLink(link);
                // add the nodes
                tmpNetwork.addNode(link.getFromNode());
                tmpNetwork.addNode(link.getToNode());
            }
        }
        // clean the network
        this.networkCleaner.run(tmpNetwork);
        return tmpNetwork;
    }

    public Network clean(Set<TransMode.Mode> modes){
        Network network = NetworkUtils.createNetwork();
        for (TransMode.Mode mode : modes){
            Network currentModeNetwork = this.clean(mode);

            // add the links and nodes to the network
            currentModeNetwork.getLinks().values().forEach(network::addLink);
            currentModeNetwork.getNodes().values().forEach(network::addNode);
        }
        return network;
    }

}
