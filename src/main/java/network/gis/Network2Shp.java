package network.gis;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimSomeWriter;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;

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
        GeoFileWriter.writeGeometries(features, file);
    }
}
