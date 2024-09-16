package network.gis;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimSomeWriter;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

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
