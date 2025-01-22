package network.run;

import network.config.ConnectedNetworkParamSet;
import network.config.LinkAttrParamSet;
import network.config.ModeParamSet;
import network.config.NetworkConverterConfigGroup;
import network.core.TransMode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DO we really need this test class? to be discussed.
 *
 */
class RunCreateCustomizedConfigTest {
    @Test
    void testCreateCustomizedConfig(){
        NetworkConverterConfigGroup customizedConfig = new NetworkConverterConfigGroup("shp", "EPSG:4326", "EPSG:31370",
                "yours/input/network/file", true, true, "yours/output/network/file",
                "NA", "NA", false, Map.of("oneway", "yes"));

        // create customized ModeParamSets
        ModeParamSet customizedBikeParamSet = new ModeParamSet(TransMode.Mode.BIKE, 50, 0.0, 2.0, 1.0,
                Set.of(Map.of("cycleway:both", "lane", "highway", "*"), Map.of("cycleway","shared_lane")),
                Set.of(Map.of("oneway", "yes")));
        customizedConfig.addParameterSet(customizedBikeParamSet);

        // create customized ConnectedNetworkParamSet
        ConnectedNetworkParamSet customizedConnectedNetworkParamSet = new ConnectedNetworkParamSet(false,
                Set.of(TransMode.Mode.BIKE), "insert");
        customizedConfig.addParameterSet(customizedConnectedNetworkParamSet);

        // create customized LinkAttrParamSet
        LinkAttrParamSet customizedLinkAttrParamSet = new LinkAttrParamSet("max_speed", "capacity", "lanes", "width", "length",
                Set.of("surface", "highway"), Map.of("MAX_SPEED_FIELD", "km/h", "WIDTH_FIELD", "m", "LENGTH_FIELD", "m"));
        customizedConfig.addParameterSet(customizedLinkAttrParamSet);

        // Check if the default values for all parameters are set
        assertEquals("shp", customizedConfig.getParams().get("FILE_TYPE"));
        assertEquals("EPSG:4326", customizedConfig.getParams().get("INPUT_CRS"));
        assertEquals("EPSG:31370", customizedConfig.getParams().get("OUTPUT_CRS"));
        assertEquals("yours/input/network/file", customizedConfig.getParams().get("INPUT_NETWORK_FILE"));
        assertEquals("true", customizedConfig.getParams().get("KEEP_DETAILED_LINK"));
        assertEquals("true", customizedConfig.getParams().get("KEEP_UNDEFINED_LINK"));
        assertEquals("yours/output/network/file", customizedConfig.getParams().get("OUTPUT_NETWORK_FILE"));
        assertEquals("NA", customizedConfig.getParams().get("OUTPUT_SHP_FILE"));
        assertEquals("NA", customizedConfig.getParams().get("OUTPUT_GEOJSON_FILE"));
        assertEquals("false", customizedConfig.getParams().get("ONEWAY"));
        assertEquals("oneway:yes", customizedConfig.getParams().get("ONEWAY_KEY_VALUE_PAIR"));

        // Check if all parameter sets are correctly added
        // LinkAttrParamSet
        assertEquals(1, customizedConfig.getParameterSets(LinkAttrParamSet.GROUP_NAME).size());


        // ConnectedNetworkParamSet
        assertEquals(1, customizedConfig.getParameterSets(ConnectedNetworkParamSet.GROUP_NAME).size());


        // ModeParamSet
        assertEquals(1, customizedConfig.getParameterSets(ModeParamSet.GROUP_NAME).size());

        assertDoesNotThrow(() -> customizedConfig.writeConfigFile("../data/testCustomizedConfig.xml"));

    }
}