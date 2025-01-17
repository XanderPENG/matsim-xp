package network.gis;

import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimSomeWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Network2Shp implements MatsimSomeWriter {

    private final String crsCode;
    private final Network network;

    public Network2Shp(String crsCode, Network network) {
        this.crsCode = crsCode;
        this.network = network;
    }

    public void write(String file) {
        // Write the network to a shapefile using MATSim's GeoFileWriter (which is based on GeoTools)
        Network2SimpleFeatures network2SimpleFeatures = new Network2SimpleFeatures(this.crsCode, this.network);
        Collection<SimpleFeature> features = network2SimpleFeatures.convertToSimpleFeatures();

//        GeoFileWriter.writeGeometries(features, file);  // The GeoFileWriter does not work properly
        write(file, features);
    }

    private void write(String filePath, Collection<SimpleFeature> features) {
        if (features == null || features.isEmpty()) {
            throw new IllegalArgumentException("The features collection is null or empty. Nothing to write.");
        }

        try {
            // Create the shapefile
            File file = new File(filePath);
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

            Map<String, Serializable> params = new HashMap<>();
            params.put("url", file.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            // Create the Shapefile DataStore
            ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

            // Get the feature type from the first feature in the collection
            SimpleFeatureType featureType = features.iterator().next().getFeatureType();

            // Define the schema
            dataStore.createSchema(featureType);

            // Set charset to UTF-8 for attribute encoding
            dataStore.setCharset(StandardCharsets.UTF_8);

            // Write features to the shapefile
            try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
                         dataStore.getFeatureWriterAppend(dataStore.getTypeNames()[0], Transaction.AUTO_COMMIT)) {
                for (SimpleFeature feature : features) {
                    SimpleFeature toWrite = writer.next();
                    toWrite.setAttributes(feature.getAttributes());
                    writer.write();
                }
            }

            System.out.println("Shapefile written successfully to " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error writing Shapefile: " + e.getMessage());
        }
    }

}
