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
