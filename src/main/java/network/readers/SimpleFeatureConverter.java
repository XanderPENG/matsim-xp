package network.readers;

import network.core.NetworkElement;
import network.tools.Utils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
//import org.opengis.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeature;

import java.util.HashMap;
import java.util.Map;

class SimpleFeatureConverter {

    static void convert(SimpleFeature feature, Map<String, NetworkElement.Node> rawNodes, Map<String, NetworkElement.Link> rawLinks, String CRS) {
        // Get the key-value pairs of the feature
        Map<String, String> keyValuePairs = getKeyValuePairs(feature);
        // Get the geometry of the feature
        Geometry geometry = (Geometry) feature.getDefaultGeometry();
        // Remove the filename prefix from the feature ID
        String featureId = feature.getID().substring(feature.getID().indexOf('.') + 1);
        // Here, we only consider the LineString and MultiLineString; the other types of geometry are not considered
        // TODO: Add the support for other types of geometry (e.g., Points)
        if (geometry instanceof MultiLineString || geometry instanceof LineString) {
            handleLinks(geometry, featureId, keyValuePairs, rawNodes, rawLinks, CRS);
        }
    }

    // Get the key-value pairs of the feature
    static Map<String, String> getKeyValuePairs(SimpleFeature feature){
        Map<String, String> KeyValuePairs = new HashMap<>();
        for(int i = 0; i < feature.getAttributeCount(); i++){
            String attributeName = feature.getFeatureType().getDescriptor(i).getLocalName();
            Object attributeValue = feature.getAttribute(i);
            if(attributeName.equals("the_geom")){
                continue;
            }else{
                KeyValuePairs.put(attributeName, attributeValue.toString());
            }
        }
        return KeyValuePairs;
    }

    static NetworkElement.Node getOrCreateNode(Coord coord, String method, Map<String, NetworkElement.Node> rawNodes){
        double threshold = 0.05; // The threshold (meter) to judge if the node is the same node as the existing node in the rawNodes (as there might be some floating point errors)
        // Find whether the node is already in the rawNodes
        for (NetworkElement.Node node : rawNodes.values()){
            switch (method){
                case "euclidean":
                    if (CoordUtils.calcEuclideanDistance(node.getCoord(), coord) < threshold){
                        return node;
                    }
                    break;
                case "haversine":
                    if (Utils.calculateHaversineDist(node.getCoord(), coord) < threshold){
                        return node;
                    }
                    break;
                case "elevation":
                    if (Utils.calculateDistWithElevation(node.getCoord(), coord) < threshold){
                        return node;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("The method should be one of 'euclidean','haversine',or 'elevation'.");
            }
        }
        // If the node is not in the rawNodes, create a new node
        NetworkElement.Node rawNode = new NetworkElement.Node(String.valueOf(rawNodes.size()), coord);
        rawNodes.put(rawNode.getId(), rawNode);
        return rawNode;
    }

    static void handleLinks(Geometry geometry, String featureId, Map<String, String> keyValuePairs,
                            Map<String, NetworkElement.Node> rawNodes, Map<String, NetworkElement.Link> rawLinks, String CRS){

        Coordinate[] coordinates = geometry.getCoordinates();
        NetworkElement.Node previousNode = null;
        for (int i = 0; i < coordinates.length; i++) {
            Coordinate coordinate = coordinates[i];
            // Judge if the node is already in the rawNodes, based on the coordinate and the distance of this node to the existing nodes
            NetworkElement.Node rawNode;
            if (Double.isNaN(coordinate.getZ())){
                Coord coord = CoordUtils.createCoord(coordinate.getX(), coordinate.getY());
                if (CRS.equals("EPSG:4326")){
                    rawNode = getOrCreateNode(coord, "haversine", rawNodes);
                } else {
                    rawNode = getOrCreateNode(coord, "euclidean", rawNodes);
                }

            } else {
                Coord coord = CoordUtils.createCoord(coordinate.getX(), coordinate.getY(), coordinate.getZ());
                if (CRS.equals("EPSG:4326")){
                    rawNode = getOrCreateNode(coord, "elevation", rawNodes);
                } else {
                    rawNode = getOrCreateNode(coord, "euclidean", rawNodes);
                }
            }
            // Create the link segment between the previous node and the current node
            // Here, we do not need to add the composed nodes to the link segment, as we have already split the link into multiple segments
            if (i != 0) {
                NetworkElement.Link rawLink = new NetworkElement.Link(featureId + "_" + i, previousNode, rawNode);
                rawLink.setKeyValuePairs(keyValuePairs);
                rawLinks.put(rawLink.getId(), rawLink);
            }
            previousNode = rawNode;
        }
    }


}
