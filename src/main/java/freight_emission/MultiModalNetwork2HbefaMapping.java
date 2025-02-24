package freight_emission;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;

import java.util.HashMap;
import java.util.Map;

public class MultiModalNetwork2HbefaMapping extends HbefaRoadTypeMapping {
    Map<String, String> detailedHbefaMap = new HashMap<>();

    public MultiModalNetwork2HbefaMapping() {
        detailedHbefaMap.put("motorway", "URB/MW-Nat./80");
        detailedHbefaMap.put("trunk", "URB/Trunk-Nat./70");
        detailedHbefaMap.put("primary", "URB/Trunk-City/70");
        detailedHbefaMap.put("secondary", "URB/Distr/50");
        detailedHbefaMap.put("tertiary", "URB/Local/50");
        detailedHbefaMap.put("residential", "URB/Access/30");
        detailedHbefaMap.put("service", "URB/Access/30");
        detailedHbefaMap.put("motorway_link", "URB/MW-Nat./80");
        detailedHbefaMap.put("trunk_link", "URB/Trunk-Nat./70");
        detailedHbefaMap.put("primary_link", "URB/Trunk-City/70");
        detailedHbefaMap.put("secondary_link", "URB/Distr/50");
        detailedHbefaMap.put("tertiary_link", "URB/Local/50");
        detailedHbefaMap.put("living_street", "URB/Access/30");
        detailedHbefaMap.put("pedestrian", "URB/Access/30");
        detailedHbefaMap.put("track", "URB/Access/30");
        detailedHbefaMap.put("road", "URB/Access/30");
        detailedHbefaMap.put("unknown", "URB/Access/30");
    }

    private String getHighwayType(Link link) {
        return link.getAttributes().getAttribute("highway").toString();
    }

    @Override
    public String determineHbefaType(Link link) {
        return this.detailedHbefaMap.getOrDefault(getHighwayType(link), "URB/Access/30");
    }

    public void addHbefaMapPairs(Map<String, String> mapPair) {
        this.detailedHbefaMap.putAll(mapPair);
    }

    public void removeHbefaMapPair(String key) {
        this.detailedHbefaMap.remove(key);
    }

    public void clearHbefaMap() {
        this.detailedHbefaMap.clear();
    }

    public void updateHbefaMap(String key, String value) {
        this.detailedHbefaMap.put(key, value);
    }



}
