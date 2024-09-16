package network.readers;

import network.config.NetworkConverterConfigGroup;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.geotools.data.DataStore;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class GeoJsonReader extends Reader {

    private final Collection<SimpleFeature> features = new ArrayList<>();
    private SimpleFeatureCollection featureCollection;
    private final String CRS;

    public GeoJsonReader(String CRS){
        this.CRS = CRS;
    }

    public GeoJsonReader(NetworkConverterConfigGroup config){
        this.CRS = config.INPUT_CRS;
    }

    // Method to load GeoJSON file
    private void loadGeoJSON(String filePath) throws IOException {
        // Create a map to hold the connection parameters
        Map<String, Object> params = new HashMap<>();
        params.put("url", new File(filePath).toURI().toURL());

        // Get the DataStore from the GeoJSON file
        DataStore dataStore = DataStoreFinder.getDataStore(params);

        // Get the feature source from the DataStore (assuming only one type of features in the file)
        String typeName = dataStore.getTypeNames()[0]; // GeoJSON usually has one feature type
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);

        // Get all the features
        featureCollection = featureSource.getFeatures();
    }

    // Method to get each feature in the GeoJSON
    public void processFeatureCollection() {
        try (SimpleFeatureIterator iterator = featureCollection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                this.features.add(feature);
            }
        } // The iterator is automatically closed here
    }

    @Override
    public void read(String file) {
        try {
            loadGeoJSON(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        processFeatureCollection();

        for (SimpleFeature feature : features) {
            SimpleFeatureConverter.convert(feature, this.rawNodes, this.rawLinks, this.CRS);
        }
    }
}
