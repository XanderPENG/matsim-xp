package network.readers;

import network.config.NetworkConverterConfigGroup;
import org.matsim.core.utils.gis.GeoFileReader;
//import org.opengis.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeature;
import java.util.Collection;


public final class ShpReader extends Reader{

    private final GeoFileReader geoFileReader = new GeoFileReader();
    private final String CRS;

    public ShpReader(String CRS){
        this.CRS = CRS;
    }

    public ShpReader(NetworkConverterConfigGroup config){
        this.CRS = config.INPUT_CRS;
    }


    @Override
    public void read(String file) {
        // Read the shapefile using MATSim's GeoFileReader (which is based on GeoTools)
        Collection<SimpleFeature> features = geoFileReader.readFileAndInitialize(file);
        // Process the features (links) in the shapefile
        for (SimpleFeature feature : features) {
            // Convert the SimpleFeature to NetworkElement.Link
            SimpleFeatureConverter.convert(feature, this.rawNodes, this.rawLinks, this.CRS);
        }
    }

}
