package network.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.util.*;

/**
 * This class is used to optimize the MATSim's multimodal network, particularly considering the following aspects:
 * 1. Split the links whose length is longer than a certain threshold (for the sake of drt mode)
 * 2. Merge the links whose length is shorter than a certain threshold (to ensure no link is too short to accommodate even one vehicle)
 * 3. Ensure the consistency of the allowed modes of the links after splitting/merging.
 */

public class MultiModalNetworkOptimizer {
    private final Network network;
    private final double minThreshold;
    private final double maxThreshold;
    private final Logger logger = LogManager.getLogger(MultiModalNetworkOptimizer.class);
    private final List<Id<Link>> skippedLink = new ArrayList<>();

    public MultiModalNetworkOptimizer(Network network, double minThreshold, double maxThreshold) {
        this.network = network;
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
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

    // Merge the links whose length is shorter than a certain threshold
    /* Now, there is a problem when merging the links at intersections. The links are merged based on the length, but the intersection is not considered.
       TODO: The merging should be based on the intersection, not only the length.
             1. When the fromNode of the current link if an intersection node, it can only be merged with the outLinks
             2. When the toNode of the current link if an intersection node, it can only be merged with the inLinks
    * */
    void mergeShortLinks() {
        List<Link> linksToBeMerged = new ArrayList<>();
        List<Id<Link>> mergedLinks= new ArrayList<>();
        // Find all the links whose length is shorter than the threshold
        for (Link link : network.getLinks().values()) {
            if (link.getLength() < minThreshold) {
                linksToBeMerged.add(link);
            }
        }
        // Merge the links
        for (Link link : linksToBeMerged) {
            if (mergedLinks.contains(link.getId())) {
                logger.info("Link {} has been merged. Skip merging.", link.getId());
                continue;
            }
            // find the adjacent links (in & out) of the current link
            Set<Link> inLinks = new HashSet<>(link.getFromNode().getInLinks().values());
            Set<Link> outLinks = new HashSet<>(link.getToNode().getOutLinks().values());

            // Filter out the adjacent links that are not supported for the allowed modes of the current link
            inLinks.removeIf(inLink -> !inLink.getAllowedModes().containsAll(link.getAllowedModes()));
            outLinks.removeIf(outLink -> !outLink.getAllowedModes().containsAll(link.getAllowedModes()));
            // Also, the inverse way should be removed from the in/out links
            Set<Link> duplicateLinks = new HashSet<>(inLinks);
            duplicateLinks.retainAll(outLinks);
            inLinks.removeAll(duplicateLinks);
            outLinks.removeAll(duplicateLinks);
            if (inLinks.isEmpty() && outLinks.isEmpty()) {
                this.logger.warn("No adjacent links containing the same allowed modes are found for link {}. Skip merging.", link.getId());
                this.skippedLink.add(link.getId());
                continue;
            }
            // Filter and get the adjacent links that have the same allowed modes as the current link
            List<Link> candidateInLinks = new ArrayList<>();
            List<Link> candidateOutLinks = new ArrayList<>();
            for (Link inLink : inLinks) {
                if (link.getAllowedModes().containsAll(inLink.getAllowedModes())) {
                    candidateInLinks.add(inLink);
                }
            }
            for (Link outLink : outLinks) {
                if (link.getAllowedModes().containsAll(outLink.getAllowedModes())) {
                    candidateOutLinks.add(outLink);
                }
            }
            // find the one with the shortest length to merge
            List<Link> mergedLinksList = new ArrayList<>();
            if (!candidateInLinks.isEmpty() && !candidateOutLinks.isEmpty()) {
                // Compare the shortest link among the inLinks and outLinks, and add to the mergedLinksList with sequence
                Link inShortestLink = candidateInLinks.stream().min(Comparator.comparingDouble(Link::getLength)).get();
                Link outShortestLink = candidateOutLinks.stream().min(Comparator.comparingDouble(Link::getLength)).get();
                if (inShortestLink.getLength() < outShortestLink.getLength()) {
                    mergedLinksList.add(inShortestLink);
                    mergedLinksList.add(link);
                } else {
                    mergedLinksList.add(link);
                    mergedLinksList.add(outShortestLink);
                }
            } else if (!candidateInLinks.isEmpty()) {
                Link inShortestLink = candidateInLinks.stream().min(Comparator.comparingDouble(Link::getLength)).get();
                mergedLinksList.add(inShortestLink);
                mergedLinksList.add(link);
            } else if (!candidateOutLinks.isEmpty()) {
                Link outShortestLink = candidateOutLinks.stream().min(Comparator.comparingDouble(Link::getLength)).get();
                mergedLinksList.add(link);
                mergedLinksList.add(outShortestLink);
            } else {
                // If there is no candidate links, merge the link with the shortest length among the adjacent links
                this.logger.warn("No candidate links with the same allowed modes found for link {}. " +
                        "Merging the link with the shortest length containing these modes .", link.getId());
                if (inLinks.isEmpty()) {
                    Link outShortestLink = outLinks.stream().min(Comparator.comparingDouble(Link::getLength)).get();
                    mergedLinksList.add(link);
                    mergedLinksList.add(outShortestLink);
                } else if (outLinks.isEmpty()) {
                    Link inShortestLink = inLinks.stream().min(Comparator.comparingDouble(Link::getLength)).get();
                    mergedLinksList.add(inShortestLink);
                    mergedLinksList.add(link);
                } else {
                    Link inShortestLink = inLinks.stream().min(Comparator.comparingDouble(Link::getLength)).get();
                    Link outShortestLink = outLinks.stream().min(Comparator.comparingDouble(Link::getLength)).get();
                    if (inShortestLink.getLength() < outShortestLink.getLength()) {
                        mergedLinksList.add(inShortestLink);
                        mergedLinksList.add(link);
                    } else {
                        mergedLinksList.add(link);
                        mergedLinksList.add(outShortestLink);
                    }
                }
            }
            // Merge the links
            logger.info("Merging the links of {}; the current link {}", mergedLinksList.stream().map(Link::getId).toArray(), link.getId());
            mergeLinks(mergedLinksList);
            mergedLinks.add(link.getId());
            mergedLinksList.remove(link);
            mergedLinks.add(mergedLinksList.get(0).getId());
        }
    }


    void mergeLinks(List<Link> links) {
        Link firstLink = links.get(0);
        Node fromNode = firstLink.getFromNode();
        Node toNode = links.get(links.size() - 1).getToNode();

        double totalLength = links.stream().mapToDouble(Link::getLength).sum();

        // Create a new link
        Link newLink = NetworkUtils.createLink(Id.createLinkId("merged_" + firstLink.getId() + "_" + links.get(links.size() - 1).getId()),
                fromNode, toNode, this.network, totalLength, firstLink.getFreespeed(), firstLink.getCapacity(), firstLink.getNumberOfLanes());
        newLink.setAllowedModes(firstLink.getAllowedModes());
        firstLink.getAttributes().getAsMap().forEach(newLink.getAttributes()::putAttribute);
        network.addLink(newLink);
        // Remove the original links
        links.forEach(link -> network.removeLink(link.getId()));
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
        // Get the links whose length is shorter than the threshold
        boolean hasShortLinks = false;
        for (Link link : network.getLinks().values()) {
            if (link.getLength() < minThreshold && !this.skippedLink.contains(link.getId())) {
                hasShortLinks = true;
                break;
            }
        }
        int count = 0;
        while (!linksToBeSplit.isEmpty() || hasShortLinks){
            // Split the long links
            linksToBeSplit.forEach(this::splitLink);

            // Merge the short links
            mergeShortLinks();

            // Update the links to be split and the existence of short links
            linksToBeSplit.clear();
            for (Link link : network.getLinks().values()) {
                if (link.getLength() > maxThreshold) {
                    linksToBeSplit.add(link);
                }
            }
            hasShortLinks = false;
            for (Link link : network.getLinks().values()) {
                if (link.getLength() < minThreshold && !this.skippedLink.contains(link.getId())) {
                    hasShortLinks = true;
                    break;
                }
            }
            count++;
            logger.info("Optimizing the multimodal network, iteration: {}", count);
        }
    }

    public List<Id<Link>> getSkippedLink() {
        return this.skippedLink;
    }

    public Network getNetwork() {
        return this.network;
    }
}
