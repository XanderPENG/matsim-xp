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
        return matchLinkKeyValuesV2(link, this.keyValueMapping.getKeyValueMapping());
    }

    public boolean matchLinkOneway(NetworkElement.Link link) {
        return matchLinkKeyValuesV2(link, this.onewayKeyValueMapping);
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

    public boolean matchLinkKeyValuesV2(NetworkElement.Link link, Set<Map<String, String>> mappings) {
        Map<String, String> keyValuePairs = link.getKeyValuePairs();
        final boolean[] match = {false};

        // Outer loop for each mapping
        for (Map<String, String> mapping : mappings) {
            // Inner loop for each key-value pair in the mapping
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String key = entry.getKey().trim();
                String value = entry.getValue().trim();

                if (key.equals("*") && value.equals("*")) {
                    match[0] = true;
                    break;
                } else if (key.equals("*")) {
                    if (keyValuePairs.values().stream().anyMatch(v -> matchesPattern(v, value))) {
                        match[0] = true;
                    } else {
                        match[0] = false;
                        break;
                    }
                } else if (value.equals("*")) {
                    if (keyValuePairs.keySet().stream().anyMatch(k -> k.equals(key))) {
                        match[0] = true;
                    } else {
                        match[0] = false;
                        break;
                    }
                } else {
                    if (keyValuePairs.containsKey(key) && matchesPattern(keyValuePairs.get(key), value)) {
                        match[0] = true;
                    } else {
                        match[0] = false;
                        break;
                    }
                }
            }
            // If the link keyValuePairs match successfully with the mapping (any one of the keyValueMapping), break the loop
            if (match[0]) {
                break;
            }
        }
        return match[0];
    }

    /**
     * Helper method to determine if a value matches a given pattern.
     * Patterns can include:
     * - "*substring*" to check if the value contains the substring
     * - "substring*" to check if the value starts with the substring
     * - "*substring" to check if the value ends with the substring
     *
     * @param value  The value to check.
     * @param pattern The pattern to match against.
     * @return True if the value matches the pattern; false otherwise.
     */
    private boolean matchesPattern(String value, String pattern) {
        if (pattern.startsWith("*") && pattern.endsWith("*")) {
            String substring = pattern.substring(1, pattern.length() - 1);
            return value.contains(substring);
        } else if (pattern.startsWith("*")) {
            String substring = pattern.substring(1);
            return value.endsWith(substring);
        } else if (pattern.endsWith("*")) {
            String substring = pattern.substring(0, pattern.length() - 1);
            return value.startsWith(substring);
        } else {
            return value.equals(pattern);
        }
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

