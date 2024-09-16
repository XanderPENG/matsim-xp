package network.gis;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.matsim.api.core.v01.network.Network;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

public class Network2GeoJson {

    private final String crsCode;
    private final Network network;

    public Network2GeoJson(String crsCode, Network network) {
        this.crsCode = crsCode;
        this.network = network;
    }


    // Method to write SimpleFeatureCollection to GeoJSON
    void writeGeoJSON(SimpleFeatureCollection featureCollection, String outputFilePath) throws IOException {
        // Create a FeatureJSON object which will handle the conversion
        FeatureJSON featureJSON = new FeatureJSON();

        // Create a FileWriter to write the output to a GeoJSON file
        try (FileWriter writer = new FileWriter(new File(outputFilePath))) {
            // Write the feature collection to the GeoJSON file
            featureJSON.writeFeatureCollection(featureCollection, writer);
        }
    }

    void writeGeoJson(Collection<SimpleFeature> features, String outputFilePath) throws IOException {
        // Create a FeatureJSON object which will handle the conversion
        FeatureJSON featureJSON = new FeatureJSON();
        // Create a FileWriter to write the output to a GeoJSON file
        try (FileWriter writer = new FileWriter(new File(outputFilePath))) {
            // Write the feature collection to the GeoJSON file
            for (SimpleFeature feature : features) {
                featureJSON.writeFeature(feature, writer);
            }
        }
    }

    public void write(String file) {
        // Write the network to a GeoJSON file
        Network2SimpleFeatures network2SimpleFeatures = new Network2SimpleFeatures(this.crsCode, this.network);
        Collection<SimpleFeature> features = network2SimpleFeatures.convertToSimpleFeatures();
        try {
            writeGeoJson(features, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
