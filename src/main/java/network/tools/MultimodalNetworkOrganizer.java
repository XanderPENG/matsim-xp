package network.tools;

import network.core.TransMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.algorithms.NetworkCleaner;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class MultimodalNetworkOrganizer {
    private final Network network;
    private final NetworkCleaner networkCleaner = new NetworkCleaner();


    public MultimodalNetworkOrganizer(Network network) {
        this.network = network;
    }

//    private Network clean(TransMode.Mode mode) {
//        // create a new  network with only the links that match the given mode
//        Network tmpNetwork = NetworkUtils.createNetwork();
//        for (Link link : this.network.getLinks().values()) {
//            if (link.getAllowedModes().contains(mode.name)) {
//                tmpNetwork.addLink(link);
//                // add the nodes
//                tmpNetwork.addNode(link.getFromNode());
//                tmpNetwork.addNode(link.getToNode());
//            }
//        }
//        // clean the network
//        this.networkCleaner.run(tmpNetwork);
//        return tmpNetwork;
//    }
//
//    public Network clean(Set<TransMode.Mode> modes){
//        Network network = NetworkUtils.createNetwork();
//        for (TransMode.Mode mode : modes){
//            Network currentModeNetwork = this.clean(mode);
//
//            // add the links and nodes to the network
//            currentModeNetwork.getLinks().values().forEach(network::addLink);
//            currentModeNetwork.getNodes().values().forEach(network::addNode);
//        }
//        return network;
//    }

    public void clean(TransMode.Mode mode, Set<TransMode.Mode> retainModes){
        final MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(this.network);
        cleaner.run(Set.of(mode.name), retainModes.stream().map(Enum::name).collect(Collectors.toSet()));
    }

    public void clean(Set<TransMode.Mode> allModes){
        final MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(this.network);
        Set<String> allModesString = allModes.stream().map(mode-> mode.name).collect(Collectors.toSet());
        Iterator<String> iterator = allModesString.iterator();
        while (iterator.hasNext()) {
            String mode = iterator.next();
            iterator.remove();
            cleaner.run(Set.of(mode), allModesString);
        }
    }

    public Network getNetwork() {
        return this.network;
    }
}
