package network.config;

import network.core.ModeKeyValueMapping;
import network.core.TransMode;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModeParamSet extends ReflectiveConfigGroup implements MatsimParameters {

    public static final String GROUP_NAME = "modeParamSet";

    @Parameter
    @Comment("The name of the mode, which should be one of the following: \n" +
            "\t\t\t[car, pt, train, bike, walk, ship, other.]. see @TransMode.Mode for more details.")
    public String MODE_NAME;

    @Parameter
    @Comment("The max speed of the mode-related link (unit: keep consistent with the @INPUT_PARAM_UNIT in @linkAttrParamSet")
    public double FREE_SPEED;

    @Parameter
    @Comment("The emission factor of the mode.")
    public double EMISSION_FACTOR;

    @Parameter
    @Comment("The default single-lane width (unit: meter)")
    public double LANE_WIDTH;

    @Parameter
    public double LANES;

    @Comment("The key-value mapping for the specific mode")
    public Set<Map<String, String>> KEY_VALUE_MAPPING = new HashSet<>();

    @Comment("The key-value mapping for the oneway")
    public Set<Map<String, String>> ONEWAY_KEY_VALUE_MAPPING = new HashSet<>();

    public ModeParamSet() {
        super(GROUP_NAME);
    }

    // Constructor for the default ModeParamSet, using the default values of the mode
    public ModeParamSet(TransMode transMode) {
        super(GROUP_NAME);
        this.MODE_NAME = transMode.getMode().name;
        this.FREE_SPEED = transMode.getDefaultMaxSpeed();
        this.EMISSION_FACTOR = transMode.getDefaultEmissionFactor();
        this.LANE_WIDTH = transMode.getDefaultLaneWidth();
        this.LANES = transMode.getDefaultLanes();
        this.KEY_VALUE_MAPPING = transMode.getModeKeyValueMapping().getKeyValueMapping();
        this.ONEWAY_KEY_VALUE_MAPPING = transMode.getOnewayKeyValueMapping();
    }


    // Constructor for totally customized params
    public ModeParamSet(TransMode.Mode mode, double freeSpeed, double emissionFactor,
                        double laneWidth, double lanes,
                        Set<Map<String, String>> keyValueMapping, Set<Map<String, String>> onewayKeyValueMapping) {
        super(GROUP_NAME);
        this.MODE_NAME = mode.name;
        this.FREE_SPEED = freeSpeed;
        this.EMISSION_FACTOR = emissionFactor;
        this.LANE_WIDTH = laneWidth;
        this.LANES = lanes;
        this.KEY_VALUE_MAPPING = keyValueMapping;
        this.ONEWAY_KEY_VALUE_MAPPING = onewayKeyValueMapping;
    }

    // @StringGetter and @StringSetter for the KEY_VALUE_MAPPING
    @StringGetter("KEY_VALUE_MAPPING")
    public String getKeyValMappingString() {
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> map : KEY_VALUE_MAPPING) {
            sb.append("{");

            map.forEach((key, value) -> sb.append(key).append("=").append(value).append(","));
            if (!map.isEmpty()) {
                sb.deleteCharAt(sb.length() - 1); // delete the last comma
            }
            sb.append("}; ");
        }
        return sb.toString().trim(); // remove the last space
    }

    @StringSetter("KEY_VALUE_MAPPING")
    public void setKeyValMappingString(String keyValMappingString) {
        // Create a new HashSet to store the parsed key-value mappings
        Set<Map<String, String>> set = new HashSet<>();
        // Split the input string by semicolons to get individual map strings
        String[] mapStrings = keyValMappingString.split(";");

        for (String mapString : mapStrings) {
            // Check if the map string is not empty after trimming whitespace
            if (!mapString.trim().isEmpty()) {
                // Remove curly braces and trim whitespace from the map string
                mapString = mapString.trim().replace("{", "").replace("}", "");
                // Split the map string by commas to get individual key-value pairs
                String[] keyValuePairs = mapString.split(",");
                Map<String, String> map = new HashMap<>();

                for (String keyValue : keyValuePairs) {
                    // Split the key-value pair by colon to separate key and value
                    String[] keyValueArray = keyValue.split("=");
                    // Check if the key-value pair has exactly two elements (key and value)
                    if (keyValueArray.length == 2) {
                        map.put(keyValueArray[0].trim(), keyValueArray[1].trim());
                    }
                }
                set.add(map);
            }
        }
        this.KEY_VALUE_MAPPING = set;
    }

    @StringGetter("ONEWAY_KEY_VALUE_MAPPING")
    public String getOnewayKeyValMappingString() {
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> map : ONEWAY_KEY_VALUE_MAPPING) {
            sb.append("{");

            map.forEach((key, value) -> sb.append(key).append("=").append(value).append(","));
            if (!map.isEmpty()) {
                sb.deleteCharAt(sb.length() - 1); // delete the last comma
            }
            sb.append("}; ");
        }
        return sb.toString().trim(); // remove the last space
    }

    @StringSetter("ONEWAY_KEY_VALUE_MAPPING")
    public void setOnewayKeyValMappingString(String onewayKeyValMappingString) {
        // Create a new HashSet to store the parsed key-value mappings
        Set<Map<String, String>> set = new HashSet<>();
        // Split the input string by semicolons to get individual map strings
        String[] mapStrings = onewayKeyValMappingString.split(";");

        for (String mapString : mapStrings) {
            // Check if the map string is not empty after trimming whitespace
            if (!mapString.trim().isEmpty()) {
                // Remove curly braces and trim whitespace from the map string
                mapString = mapString.trim().replace("{", "").replace("}", "");
                // Split the map string by commas to get individual key-value pairs
                String[] keyValuePairs = mapString.split(",");
                Map<String, String> map = new HashMap<>();

                for (String keyValue : keyValuePairs) {
                    // Split the key-value pair by colon to separate key and value
                    String[] keyValueArray = keyValue.split("=");
                    // Check if the key-value pair has exactly two elements (key and value)
                    if (keyValueArray.length == 2) {
                        map.put(keyValueArray[0].trim(), keyValueArray[1].trim());
                    }
                }
                set.add(map);
            }
        }
        this.ONEWAY_KEY_VALUE_MAPPING = set;
    }


    public TransMode getTransMode() {
        switch (MODE_NAME) {
            case "car" -> {
                return new TransMode(TransMode.Mode.CAR, new ModeKeyValueMapping.Builder()
                        .setMode(TransMode.Mode.CAR)
                        .setKeyValueMapping(KEY_VALUE_MAPPING)
                        .build(), ONEWAY_KEY_VALUE_MAPPING,
                        FREE_SPEED, EMISSION_FACTOR, LANE_WIDTH, LANES);
            }
            case "pt" -> {
                return new TransMode(TransMode.Mode.PT, new ModeKeyValueMapping.Builder()
                        .setMode(TransMode.Mode.PT)
                        .setKeyValueMapping(KEY_VALUE_MAPPING)
                        .build(), ONEWAY_KEY_VALUE_MAPPING,
                        FREE_SPEED, EMISSION_FACTOR, LANE_WIDTH, LANES);
            }
            case "train" -> {
                return new TransMode(TransMode.Mode.TRAIN, new ModeKeyValueMapping.Builder()
                        .setMode(TransMode.Mode.TRAIN)
                        .setKeyValueMapping(KEY_VALUE_MAPPING)
                        .build(), ONEWAY_KEY_VALUE_MAPPING,
                        FREE_SPEED, EMISSION_FACTOR, LANE_WIDTH, LANES);
            }
            case "bike" -> {
                return new TransMode(TransMode.Mode.BIKE, new ModeKeyValueMapping.Builder()
                        .setMode(TransMode.Mode.BIKE)
                        .setKeyValueMapping(KEY_VALUE_MAPPING)
                        .build(), ONEWAY_KEY_VALUE_MAPPING,
                        FREE_SPEED, EMISSION_FACTOR, LANE_WIDTH, LANES);
            }
            case "walk" -> {
                return new TransMode(TransMode.Mode.WALK, new ModeKeyValueMapping.Builder()
                        .setMode(TransMode.Mode.WALK)
                        .setKeyValueMapping(KEY_VALUE_MAPPING)
                        .build(), ONEWAY_KEY_VALUE_MAPPING,
                        FREE_SPEED, EMISSION_FACTOR, LANE_WIDTH, LANES);
            }
            case "ship" -> {
                return new TransMode(TransMode.Mode.SHIP, new ModeKeyValueMapping.Builder()
                        .setMode(TransMode.Mode.SHIP)
                        .setKeyValueMapping(KEY_VALUE_MAPPING)
                        .build(), ONEWAY_KEY_VALUE_MAPPING,
                        FREE_SPEED, EMISSION_FACTOR, LANE_WIDTH, LANES);
            }
            case "other" -> {
                return new TransMode(TransMode.Mode.OTHER, new ModeKeyValueMapping.Builder()
                        .setMode(TransMode.Mode.OTHER)
                        .setKeyValueMapping(KEY_VALUE_MAPPING)
                        .build(), ONEWAY_KEY_VALUE_MAPPING,
                        FREE_SPEED, EMISSION_FACTOR, LANE_WIDTH, LANES);
            }
        }
        throw new IllegalArgumentException("Unsupported mode: " + MODE_NAME);
    }
}
