package network.core;

import org.matsim.api.core.v01.TransportMode;

import java.util.Map;
import java.util.Set;

public final class TransMode {
    private final Mode mode;
    private final ModeKeyValueMapping keyValueMapping;
    private final Set<Map<String, String>> onewayKeyValueMapping;

    private final double defaultMaxSpeed;
    private final double defaultEmissionFactor;
    //    private final double defaultLaneCapacity;
    private final double defaultLaneWidth;
    private final double defaultLanes;


    public TransMode(Mode mode, ModeKeyValueMapping keyValueMapping, Set<Map<String, String>> onewayKeyValueMapping,
                     double defaultMaxSpeed, double defaultEmissionFactor,
                     double defaultLaneWidth, double defaultLanes) {
        this.mode = mode;
        this.keyValueMapping = keyValueMapping;
        this.defaultMaxSpeed = defaultMaxSpeed;
        this.defaultEmissionFactor = defaultEmissionFactor;
//        this.defaultLaneCapacity = defaultLaneCapacity;
        this.defaultLaneWidth = defaultLaneWidth;
        this.defaultLanes = defaultLanes;
        this.onewayKeyValueMapping = onewayKeyValueMapping;
    }


    public Mode getMode() {
        return this.mode;
    }

    public ModeKeyValueMapping getModeKeyValueMapping() {
        return this.keyValueMapping;
    }

    public Set<Map<String, String>> getOnewayKeyValueMapping() {
        return this.onewayKeyValueMapping;
    }

    public double getDefaultMaxSpeed() {
        return this.defaultMaxSpeed;
    }

    public double getDefaultEmissionFactor() {
        return this.defaultEmissionFactor;
    }

    public double getDefaultLaneWidth() {
        return this.defaultLaneWidth;
    }

    public double getDefaultLanes() {
        return this.defaultLanes;
    }

    public boolean matchLinkTransMode(NetworkElement.Link link) {
        return matchLinkKeyValues(link, this.keyValueMapping.getKeyValueMapping());
    }

    public boolean matchLinkOneway(NetworkElement.Link link) {
        return matchLinkKeyValues(link, this.onewayKeyValueMapping);
    }

    private boolean matchLinkKeyValues(NetworkElement.Link link, Set<Map<String, String>> mappings){
        Map<String, String> keyValuePairs = link.getKeyValuePairs();
        final boolean[] match = {false};
        // outer loop for each mapping
        for (Map<String, String> mapping : mappings) {
            // inner loop for each key-value pair in the mapping
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String key = entry.getKey().trim();
                String value = entry.getValue().trim();
                // if both key and value are "*"
                if (key.equals("*") && value.equals("*")) {
                    match[0] = true;
                    break;
                } else if (key.equals("*")) {
                    if (keyValuePairs.containsValue(value)) {
                        match[0] = true;
                    } else {
                        match[0] = false;
                        break;
                    }
                } else if (value.equals("*")) {
                    if (keyValuePairs.containsKey(key)) {
                        match[0] = true;
                    } else {
                        match[0] = false;
                        break;
                    }
                } else {
                    if (keyValuePairs.containsKey(key) && keyValuePairs.get(key).equals(value)) {
                        match[0] = true;
                    } else {
                        match[0] = false;
                        break;
                    }
                }
            }
            // if the link keyValuePairs match successfully with the mapping (any one of the keyValueMapping), break the loop
            if (match[0]) {
                break;
            }
        }
        return match[0];
    }

    public enum Mode {

        CAR(TransportMode.car),
        PT(TransportMode.pt),
        TRAIN(TransportMode.train),
        BIKE(TransportMode.bike),
        WALK(TransportMode.walk),
        SHIP(TransportMode.ship),
        OTHER("other");

        public final String name;


        Mode(String name) {
            this.name = name;
        }
    }

}

