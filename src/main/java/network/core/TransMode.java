package network.core;

import org.matsim.api.core.v01.TransportMode;

import java.util.Map;

public final class TransMode {
    private final Mode mode;
    private final ModeKeyValueMapping keyValueMapping;

    private final double defaultMaxSpeed;
    private final double defaultEmissionFactor;
    //    private final double defaultLaneCapacity;
    private final double defaultLaneWidth;
    private final double defaultLanes;


    public TransMode(Mode mode, ModeKeyValueMapping keyValueMapping,
                     double defaultMaxSpeed, double defaultEmissionFactor,
                     double defaultLaneWidth, double defaultLanes) {
        this.mode = mode;
        this.keyValueMapping = keyValueMapping;
        this.defaultMaxSpeed = defaultMaxSpeed;
        this.defaultEmissionFactor = defaultEmissionFactor;
//        this.defaultLaneCapacity = defaultLaneCapacity;
        this.defaultLaneWidth = defaultLaneWidth;
        this.defaultLanes = defaultLanes;
    }


    public Mode getMode() {
        return this.mode;
    }

    public ModeKeyValueMapping getModeKeyValueMapping() {
        return this.keyValueMapping;
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
        Map<String, String> keyValuePairs = link.getKeyValuePairs();
        final boolean[] match = {false};
        // outer loop for each mapping
        for (Map<String, String> mapping : this.keyValueMapping.getKeyValueMapping()) {
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

