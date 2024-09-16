package network.readers;

import network.core.NetworkElement;

import java.util.HashMap;
import java.util.Map;

/**
 * This interface is used to read the network with different file format (e.g., shp, osm, geojson.)
 *
 */

public abstract class Reader {
    // The raw nodes and links from the OSM file
    protected final Map<String, NetworkElement.Node> rawNodes = new HashMap<>();
    protected final Map<String, NetworkElement.Link> rawLinks = new HashMap<>();

    public abstract void read(String file);

    public Map<String, NetworkElement.Node> getRawNodes(){
        return rawNodes;
    }

    public Map<String, NetworkElement.Link> getRawLinks(){
        return rawLinks;
    }

}