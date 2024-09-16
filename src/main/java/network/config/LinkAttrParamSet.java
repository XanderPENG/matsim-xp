package network.config;

import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to configure the Tags/Keys of link-related parameters (e.g., freespeed, lanes, capacity, etc.) in the KeyValuePais.
 */
public class LinkAttrParamSet extends ReflectiveConfigGroup implements MatsimParameters {

    public static final String GROUP_NAME = "linkAttrParamSet";

    @Parameter
    public String MAX_SPEED_FIELD;

    @Parameter
    public String CAPACITY_FIELD;

    @Parameter
    public String LANES_FIELD;

    @Parameter
    public String LANE_WIDTH_FIELD;

    @Parameter
    public String LENGTH_FIELD;

    public Map<String, String> INPUT_PARAM_UNIT = new HashMap<>();

    @Parameter
    @Comment("The reserved fields in the link attributes, the value of these fields will be reserved as link attributes in the output network.")
    public Set<String> RESERVED_LINK_FIELDS;

    public LinkAttrParamSet() {
        super(GROUP_NAME);
    }

    public LinkAttrParamSet(String maxSpeedField, String capacityField, String lanesField,
                            String widthField, String lengthField, Set<String> reservedLinkFields, Map<String, String> inputParamUnit) {
        super(GROUP_NAME);
        this.MAX_SPEED_FIELD = maxSpeedField;
        this.CAPACITY_FIELD = capacityField;
        this.LANES_FIELD = lanesField;
        this.LANE_WIDTH_FIELD = widthField;
        this.LENGTH_FIELD = lengthField;
        this.RESERVED_LINK_FIELDS = reservedLinkFields;
        this.INPUT_PARAM_UNIT = inputParamUnit;
    }

    @StringGetter("INPUT_PARAM_UNIT")
    public String getInputParamUnitString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : this.INPUT_PARAM_UNIT.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1); // delete the last comma
        return sb.toString().trim(); // remove the last space
    }

    @StringSetter("INPUT_PARAM_UNIT")
    public void setInputParamUnitString(String inputParamUnitString) {
        String[] entries = inputParamUnitString.split(",");
        for (String entry : entries) {
            String[] keyValue = entry.split(":");
            this.INPUT_PARAM_UNIT.put(keyValue[0], keyValue[1]);
        }
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put("INPUT_PARAM_UNIT", "The unit of the input parameters; which could be (m or km; m/s or km/h); " +
                "However, It is highly recommended to convert them to the meter-based units (i.e., m, m/s.) before running the converter.");
        return map;
    }
}


