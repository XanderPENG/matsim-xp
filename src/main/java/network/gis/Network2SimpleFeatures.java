package network.gis;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

class Network2SimpleFeatures {

    private final String crsCode;
    private final Network network;

    public Network2SimpleFeatures(String crsCode, Network network) {
        this.crsCode = crsCode;
        this.network = network;
    }

    Collection<SimpleFeature> convertToSimpleFeatures() {
        // Convert the MATSim network to a collection of SimpleFeatures
        Collection<SimpleFeature> features = new ArrayList<>();
        // Get the link attributes fields
        Link iterLink = this.network.getLinks().values().iterator().next();
        Set<String> attrKeys = iterLink.getAttributes().getAsMap().keySet();
        // Create the SimpleFeatureType
        SimpleFeatureType featureType = createFeatureType(attrKeys);
        // Create the GeometryFactory
        GeometryFactory geometryFactory = new GeometryFactory();
        // Create SimpleFeatures from MATSim Links
        for (Link link : this.network.getLinks().values()) {
            SimpleFeature feature = createFeatureFromLink(link, featureType, geometryFactory);
            features.add(feature);
        }
        return features;
    }

    // Define the schema for SimpleFeatures
    private SimpleFeatureType createFeatureType(Set<String> attrKeys) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("NetworkLink");
        // Set the CRS of the shapefile
        try{
            CoordinateReferenceSystem crs = CRS.decode(this.crsCode);
            typeBuilder.setCRS(crs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        typeBuilder.add("geometry", LineString.class);  // Geometries (LineString)
        typeBuilder.add("linkId", String.class);        // Link ID
        typeBuilder.add("capacity", Double.class);      // Link capacity
        typeBuilder.add("freespeed", Double.class);     // Free speed
        typeBuilder.add("length", Double.class);        // Link length
        typeBuilder.add("lanes", Double.class);         // Number of lanes
        typeBuilder.add("modes", Set.class);  // Allowed modes
        // Add the link attributes fields
        for (String attrKey : attrKeys) {
            typeBuilder.add(attrKey, String.class);
        }

        return typeBuilder.buildFeatureType();
    }

    // Create SimpleFeature from MATSim Link
    private SimpleFeature createFeatureFromLink(Link link, SimpleFeatureType featureType, GeometryFactory geometryFactory) {
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

        // Create LineString from link's start and end nodes
        Node fromNode = link.getFromNode();
        Node toNode = link.getToNode();
        Coordinate[] coordinates = new Coordinate[] {
                new Coordinate(fromNode.getCoord().getX(), fromNode.getCoord().getY()),
                new Coordinate(toNode.getCoord().getX(), toNode.getCoord().getY())
        };
        LineString lineString = geometryFactory.createLineString(coordinates);

        // Convert allowed modes set to a comma-separated string
        String allowedModes = String.join(", ", link.getAllowedModes());

        // Set geometry and attributes
        featureBuilder.add(lineString);                          // Geometry (LineString)
        featureBuilder.add(link.getId().toString());             // Link ID
        featureBuilder.add(link.getCapacity());                  // Capacity
        featureBuilder.add(link.getFreespeed());                 // Free speed
        featureBuilder.add(link.getLength());                    // Length
        featureBuilder.add(link.getNumberOfLanes());             // Number of lanes
        featureBuilder.add(allowedModes);              // Allowed modes
        // Add the link attributes fields
        for (String attrKey : link.getAttributes().getAsMap().keySet()) {
            featureBuilder.add(link.getAttributes().getAttribute(attrKey).toString());
        }
        return featureBuilder.buildFeature(link.getId().toString());
    }

}
