package network.tools;

import org.geotools.referencing.GeodeticCalculator;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.*;

public class Utils {
    // Set the path to the input OSM.pbf file
    public final static String FILE_TEST_NETWORK_PBF = "../../data/raw/test/GemeenteLeuvenOnly.pbf";
    // Set the path to the output network.xml file
    public final static String FILE_TEST_NETWORK_XML = "../../data/intermediate/test/GemeenteLeuvenTest.xml.gz";
    // Set the path to the output config file
    public final static String FILE_TEST_CONFIG = "../../data/intermediate/test/multimodalNetworkConverterConfig.xml";
    // Set the input path of HK road network (in SHP format)
    public final static String INPUT_HK_ROAD_NETWORK = "../../data/raw/hk_road/test_road.shp";

    public final static CoordinateTransformation CT_WGS84_LAMBERT72 = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
            "EPSG:31370");

// Set the CRS of Leuven
    public static String outputCrs = "EPSG:31370";  // lambert 72

    public static String id2String(Object id){
        boolean isNumber = id instanceof Number;
        if (id instanceof String){
            return (String)id;
        }else if (isNumber){
            return String.valueOf(id);
        }else{
            throw new IllegalArgumentException("The id should be either a string or a number.");
        }
    }

    public static <K, V> LinkedHashMap<K, V> reverseLinkedHashMap(LinkedHashMap<K, V> originalMap) {
        List<Map.Entry<K, V>> entryList = new ArrayList<>(originalMap.entrySet());
        Collections.reverse(entryList);

        LinkedHashMap<K, V> reversedMap = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : entryList) {
            reversedMap.put(entry.getKey(), entry.getValue());
        }

        return reversedMap;
    }

    // Calculate the haversine distance between two coordinates using GeoTools (unit: meter)
    public static double calculateHaversineDist(Coord coord1, Coord coord2){
        GeodeticCalculator calculator = new GeodeticCalculator();
        calculator.setStartingGeographicPoint(coord1.getX(), coord1.getY());
        calculator.setDestinationGeographicPoint(coord2.getX(), coord2.getY());
        return calculator.getOrthodromicDistance();
    }

    public static double calculateDistWithElevation(Coord coord1, Coord coord2){
        double dist = calculateHaversineDist(coord1, coord2);
        return Math.sqrt(dist * dist + Math.pow(coord1.getZ() - coord2.getZ(), 2));
    }

}
