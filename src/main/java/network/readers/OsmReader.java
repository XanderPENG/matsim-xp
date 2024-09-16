package network.readers;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfReader;
import network.config.NetworkConverterConfigGroup;
import network.core.NetworkElement;
import network.core.TransMode;
import network.tools.Utils;
import org.matsim.api.core.v01.TransportMode;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to read the OSM file (.pbf format) and convert it to raw nodes and links.
 */
public final class OsmReader extends Reader implements OsmHandler {

    private final Set<Map<String, String>> ptModeKeyValuePairs;
    private final Set<String> reservedKeyValues;

    public OsmReader(Set<Map<String, String>> ptModeKeyValuePairs, Set<String> reservedKeyValues) {
        this.ptModeKeyValuePairs = ptModeKeyValuePairs;
        this.reservedKeyValues = reservedKeyValues;
    }

    public OsmReader(NetworkConverterConfigGroup config) {
        this.ptModeKeyValuePairs = config.getModeParamSets().get(TransportMode.pt).KEY_VALUE_MAPPING;
        this.reservedKeyValues = config.getLinkAttrParamSet().RESERVED_LINK_FIELDS;
    }

    public OsmReader() {
        this.ptModeKeyValuePairs = null;
        this.reservedKeyValues = null;
    }

    private void handleNode(OsmNode osmNode){
        // Convert the OsmNode to NetworkElement.Node
        NetworkElement.Node rawNode = new NetworkElement.Node(osmNode.getId(), osmNode.getLongitude(), osmNode.getLatitude());
        rawNodes.put(Utils.id2String(osmNode.getId()), rawNode);
    }

    // Convert the OsmWay to NetworkElement.Link
    private void handleWay(OsmWay osmWay){

        Map<String, String> tagValuePairs = OsmModelUtil.getTagsAsMap(osmWay);
        // Get all the node ids of the way
        int numNodes = osmWay.getNumberOfNodes();
        Set<Long> nodeIds = new LinkedHashSet<>();
        for(int i = 0; i < numNodes; i++){
            nodeIds.add(osmWay.getNodeId(i));
        }
        // Create the link
        NetworkElement.Link rawLink = new NetworkElement.Link(osmWay.getId(),
                rawNodes.get(Utils.id2String(osmWay.getNodeId(0))), rawNodes.get(Utils.id2String(osmWay.getNodeId(numNodes-1))));

        // Add the composed nodes to the link if there are more than 2 nodes
        if (nodeIds.size() > 2) {
            // filter out the first and last node
            nodeIds.remove(osmWay.getNodeId(0));
            nodeIds.remove(osmWay.getNodeId(numNodes - 1));
            nodeIds.forEach(nodeId -> rawLink.addComposedNode(rawNodes.get(Utils.id2String(nodeId))));
        }

        rawLink.setKeyValuePairs(tagValuePairs);
        rawLinks.put(Utils.id2String(osmWay.getId()), rawLink);
    }

    // Process the OsmRelation (mainly for pt) and add the pt-related information to the rawLinks
    private void handleRelation(OsmRelation osmRelation) {
        // If the ptModeKeyValuePairs is null (which means the PT mode is not defined), return directly
        if (this.ptModeKeyValuePairs == null) {
            return;
        }
        // Firstly, get the tags of the relation
        Map<String, String> tagValuePairs = OsmModelUtil.getTagsAsMap(osmRelation);
        final boolean[] match = {false};
        // Match the tag and PtKeyValuePairs, to judge if the relation is a pt-related one
        for (Map<String, String> ptModeKeyValue : this.ptModeKeyValuePairs) {
            // inner loop to check if the tagValuePairs contains the ptModeKeyValue
            for (Map.Entry<String, String> entry : ptModeKeyValue.entrySet()) {
                String key = entry.getKey().trim();
                String value = entry.getValue().trim();
                // if both key and value are "*"
                if (key.equals("*") && value.equals("*")) {
                    match[0] = true;
                    break;
                } else if (key.equals("*")) {
                    if (tagValuePairs.containsValue(value)) {
                        match[0] = true;
                    } else {
                        match[0] = false;
                        break;
                    }
                } else if (value.equals("*")) {
                    if (tagValuePairs.containsKey(key)) {
                        match[0] = true;
                    } else {
                        match[0] = false;
                        break;
                    }
                } else {
                    if (tagValuePairs.containsKey(key) && tagValuePairs.get(key).equals(value)) {
                        match[0] = true;
                    } else {
                        match[0] = false;
                        break;
                    }
                }
            }
            // if the link keyValuePairs match successfully with the mapping (any one of the keyValueMapping), break the loop
            if (match[0]) {
                break;
            }
        }
        // If the relation is a pt-related one, add the pt-related information to the rawLinks
        if (match[0]) {
            // Get the member ways
            int numberMembers = osmRelation.getNumberOfMembers();
            for (int i = 0; i < numberMembers; i++) {
                if (osmRelation.getMember(i).getType().equals(EntityType.Way)) {
                    long ptLinkId = osmRelation.getMember(i).getId();
                    NetworkElement.Link ptLink = rawLinks.get(Utils.id2String(ptLinkId));
                    if (ptLink != null) {
                        ptLink.addAllowedMode(TransMode.Mode.PT);
                        // add related tag-values into the ptLink
                        for (Map.Entry<String, String> entry : tagValuePairs.entrySet()) {
                            if (this.reservedKeyValues.contains(entry.getKey())) {
                                ptLink.addKeyValuePair(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void read(String file) {

        // Read the PBF file
        try (InputStream inputStream = new FileInputStream(file)) {
            PbfReader reader = new PbfReader(inputStream, false);
            reader.setHandler(this);
            reader.read();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to read PBF file", e);
        }
    }

    // Override the methods from OsmHandler
    @Override
    public void handle(OsmBounds osmBounds) {

    }

    @Override
    public void handle(OsmNode osmNode) {
        handleNode(osmNode);
    }

    @Override
    public void handle(OsmWay osmWay) {
        handleWay(osmWay);
    }

    @Override
    public void handle(OsmRelation osmRelation) {
        handleRelation(osmRelation);
    }

    @Override
    public void complete() {

    }
}

