package network.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.utils.collections.Tuple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * This class is used to optimize the MATSim's multimodal network (based on {@link NetworkSimplifier}), particularly considering the following aspects:
 * 1. Split the links whose length is longer than a certain threshold (for the sake of drt mode)
 * 2. Merge the links whose length is shorter than a certain threshold (to ensure no link is too short to accommodate even one vehicle)
 * 3. Ensure the consistency of the allowed modes of the links after splitting/merging.
 */

public class MultiModalNetworkOptimizer {
    private final Network network;
    private final double minThreshold;
    private final double maxThreshold;
    private final NetworkSimplifier networkSimplifier;
    private final BiPredicate<Link, Link> customizedLinkMergeablePredicate;
    private final BiConsumer<Tuple<Link, Link>, Link> customizedLinkAttrConsumer;

    private final Logger logger = LogManager.getLogger(MultiModalNetworkOptimizer.class);

    public MultiModalNetworkOptimizer(Network network, double minThreshold, double maxThreshold, NetworkSimplifier networkSimplifier,
                                      BiPredicate<Link, Link> customizedLinkMergeablePredicate, BiConsumer<Tuple<Link, Link>, Link> customizedLinkAttrConsumer) {
        this.network = network;
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
        this.networkSimplifier = networkSimplifier;
        this.customizedLinkMergeablePredicate = customizedLinkMergeablePredicate;
        this.customizedLinkAttrConsumer = customizedLinkAttrConsumer;
    }

    // Split a long link into multiple links
    void splitLink(Link link) {
        int numberOfSplits = (int) Math.ceil(link.getLength() / this.maxThreshold);  // Calculate the number of splits

        Node fromNode = link.getFromNode();
        Node toNode = link.getToNode();
        double totalLength = link.getLength();
        double segmentLength = totalLength / numberOfSplits;


        // Get the coordinates of the intermediate nodes
        List<Coord> coords = getSplitLinkCoordinates(link, numberOfSplits);

        // Create new nodes and links
        Node startNode = fromNode;
        for (int i = 1; i < numberOfSplits; i++) {
            Node newNode = network.getFactory().createNode(Id.createNodeId("splitNode_" + link.getId() + "_" + i), coords.get(i-1));
            network.addNode(newNode);

            Link newLink = NetworkUtils.createLink(Id.createLinkId("splitLink_" + link.getId() + "_" + i), startNode, newNode, this.network,
                    segmentLength, link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
            newLink.setAllowedModes(link.getAllowedModes()); // Keep the allowed modes of the original link
            link.getAttributes().getAsMap().forEach(newLink.getAttributes()::putAttribute);  // Copy the attributes of the original link
            network.addLink(newLink);
            startNode = newNode;
        }

        // Last link segment
        Link finalLink = NetworkUtils.createLink(Id.createLinkId("splitLink_" + link.getId() + "_" + numberOfSplits), startNode, toNode, this.network,
                segmentLength, link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
        finalLink.setAllowedModes(link.getAllowedModes());
        link.getAttributes().getAsMap().forEach(finalLink.getAttributes()::putAttribute);
        network.addLink(finalLink);
        // Remove the original link
        network.removeLink(link.getId());
    }

    // Split a long link into multiple links and return the coordinates of the intermediate nodes
    List<Coord> getSplitLinkCoordinates(Link link, int numberOfSplits) {
        List <Coord> coords = new ArrayList<>();
        Node fromNode = link.getFromNode();
        Node toNode = link.getToNode();
        if (fromNode.getCoord().hasZ()){
            for (int i = 1; i < numberOfSplits; i++){
                double x = fromNode.getCoord().getX() + (toNode.getCoord().getX() - fromNode.getCoord().getX()) * i / numberOfSplits;
                double y = fromNode.getCoord().getY() + (toNode.getCoord().getY() - fromNode.getCoord().getY()) * i / numberOfSplits;
                double z = fromNode.getCoord().getZ() + (toNode.getCoord().getZ() - fromNode.getCoord().getZ()) * i / numberOfSplits;
                coords.add(new Coord(x, y, z));
            }

        } else {
            for (int i = 1; i < numberOfSplits; i++){
                double x = fromNode.getCoord().getX() + (toNode.getCoord().getX() - fromNode.getCoord().getX()) * i / numberOfSplits;
                double y = fromNode.getCoord().getY() + (toNode.getCoord().getY() - fromNode.getCoord().getY()) * i / numberOfSplits;
                coords.add(new Coord(x, y));
            }
        }
        return coords;
    }

    public void optimize(){
        logger.info("Optimizing the multimodal network...");
        // Get the links whose length is longer than the threshold
        List<Link> linksToBeSplit = new ArrayList<>();
        for (Link link : network.getLinks().values()) {
            if (link.getLength() > maxThreshold) {
                linksToBeSplit.add(link);
            }
        }
        // Split the long links
        linksToBeSplit.forEach(this::splitLink);
        // Merge the short links using NetworkSimplifier
        networkSimplifier.run(this.network, this.customizedLinkMergeablePredicate, this.customizedLinkAttrConsumer);
    }

    public Network getNetwork() {
        return this.network;
    }

    public static class Builder {
        private Network network;
        private double minThreshold;
        private double maxThreshold;
        private NetworkSimplifier networkSimplifier;
        private BiPredicate<Link, Link> customizedLinkMergeablePredicate;
        private BiConsumer<Tuple<Link, Link>, Link> customizedLinkAttrConsumer;

        // A default predicate to check if two links are mergeable, based on the allowed modes
        private BiPredicate<Link, Link> defaultLinkMergeablePredicate() {
            return (link1, link2) -> {
                if (link1.getLength() <= this.minThreshold || link2.getLength() <= this.minThreshold) {
                    Set<String> allowedModes1 = link1.getAllowedModes();
                    Set<String> allowedModes2 = link2.getAllowedModes();
                    return allowedModes1.containsAll(allowedModes2) || allowedModes2.containsAll(allowedModes1);
                } else {
                    return false;
                }
            };
        }

        // A default consumer to set the attributes of the merged link
        private BiConsumer<Tuple<Link, Link>, Link> defaultLinkAttrConsumer() {
            return (tuple, link) -> {
                Link inLink = tuple.getFirst();
                Link outLink = tuple.getSecond();
                // Set the allowed modes of the merged link
                Set<String> tmpAllowedModes = new HashSet<>();
                if (inLink.getAllowedModes().containsAll(outLink.getAllowedModes())) {
                    tmpAllowedModes.addAll(inLink.getAllowedModes());
                } else {
                    tmpAllowedModes.addAll(outLink.getAllowedModes());
                }
                link.setAllowedModes(tmpAllowedModes);

                // Set the attributes of the merged link
                for (String key : inLink.getAttributes().getAsMap().keySet()) {
                    String inLinkValue = inLink.getAttributes().getAttribute(key).toString();
                    String outLinkValue = outLink.getAttributes().getAttribute(key).toString();
                    if (inLinkValue.equals(outLinkValue)) {
                        link.getAttributes().putAttribute(key, inLinkValue);
                    } else {
                        link.getAttributes().putAttribute(key, inLinkValue + "_" + outLinkValue);
                    }
                }
            };
        }

        public Builder setNetwork(Network network) {
            this.network = network;
            return this;
        }

        public Builder setMinThreshold(double minThreshold) {
            this.minThreshold = minThreshold;
            return this;
        }

        public Builder setMaxThreshold(double maxThreshold) {
            this.maxThreshold = maxThreshold;
            return this;
        }

        public Builder setNetworkSimplifier(NetworkSimplifier networkSimplifier) {
            this.networkSimplifier = networkSimplifier;
            return this;
        }

        public BiConsumer<Tuple<Link, Link>, Link> getCustomizedLinkAttrConsumer() {
            return this.customizedLinkAttrConsumer;
        }

        public Builder setCustomizedLinkAttrConsumer(BiConsumer<Tuple<Link, Link>, Link> customizedLinkAttrConsumer) {
            this.customizedLinkAttrConsumer = customizedLinkAttrConsumer;
            return this;
        }

        public BiPredicate<Link, Link> getCustomizedLinkMergeablePredicate() {
            return this.customizedLinkMergeablePredicate;
        }

        public Builder setCustomizedLinkMergeablePredicate(BiPredicate<Link, Link> customizedLinkMergeablePredicate) {
            this.customizedLinkMergeablePredicate = customizedLinkMergeablePredicate;
            return this;
        }

        private void setDefaultValues() {
            if (this.minThreshold == 0) {
                this.minThreshold = 5;
            }
            if (this.maxThreshold == 0) {
                this.maxThreshold = 200;
            }
            if (this.networkSimplifier == null) {
                this.networkSimplifier = new NetworkSimplifier();
            }
            if (this.customizedLinkMergeablePredicate == null) {
                this.customizedLinkMergeablePredicate = this.defaultLinkMergeablePredicate();
            }
            if (this.customizedLinkAttrConsumer == null) {
                this.customizedLinkAttrConsumer = this.defaultLinkAttrConsumer();
            }
        }

        public MultiModalNetworkOptimizer build() {
            this.setDefaultValues();
            return new MultiModalNetworkOptimizer(this.network, this.minThreshold, this.maxThreshold, this.networkSimplifier,
                    this.customizedLinkMergeablePredicate, this.customizedLinkAttrConsumer);
        }

    }

}
