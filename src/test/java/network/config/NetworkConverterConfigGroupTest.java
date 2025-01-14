package network.config;

import network.core.TransMode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NetworkConverterConfigGroupTest {

    @Test
    void testAllParametersAreWritten(){
        NetworkConverterConfigGroup config = new NetworkConverterConfigGroup();
        Map<String, String> params = config.getParams();
        List<String> allParameters = new ArrayList<>();
        allParameters.add("FILE_TYPE");
        allParameters.add("INPUT_CRS");
        allParameters.add("OUTPUT_CRS");
        allParameters.add("INPUT_NETWORK_FILE");
        allParameters.add("KEEP_DETAILED_LINK");
        allParameters.add("KEEP_UNDEFINED_LINK");
        allParameters.add("OUTPUT_NETWORK_FILE");
        allParameters.add("OUTPUT_SHP_FILE");
        allParameters.add("OUTPUT_GEOJSON_FILE");
        allParameters.add("ONEWAY");
        allParameters.add("ONEWAY_KEY_VALUE_PAIR");
        for (String parameter : allParameters) {
            assertTrue(params.containsKey(parameter));
        }
    }

    @Test
    void testCreateDefaultConfig() {
        NetworkConverterConfigGroup config = NetworkConverterConfigGroup.createDefaultConfig();
        // Check if the default values for all parameters are set
        checkIfAllParametersAreSet(config);
        // Check if all parameter sets are correctly added
        checkIfAllParametersSetsAreSet(config);
    }

    @Test
    void testCreateCustomizedConfig(){
        NetworkConverterConfigGroup customizedConfig = new NetworkConverterConfigGroup("osm", "EPSG:4326", "EPSG:31370",
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
        checkIfAllParametersAreSet(customizedConfig);
        assertEquals("osm", customizedConfig.getParams().get("FILE_TYPE"));
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
        checkIfAllParametersSetsAreSet(customizedConfig);
    }

    @Test
    void testWriteAndReadConfig(){
        // Test if the config file can be written
        NetworkConverterConfigGroup config4Written = NetworkConverterConfigGroup.createDefaultConfig();
        assertDoesNotThrow(() -> config4Written.writeConfigFile("testDefaultConfig.xml"));

        // Test if the config file can be read
        NetworkConverterConfigGroup config4Reading = NetworkConverterConfigGroup.loadConfigFile("testDefaultConfig.xml");

        // Check if the parameters are correctly read
        checkIfAllParametersAreSet(config4Reading);
        // Check if the parameter sets are correctly read
        checkIfAllParametersSetsAreSet(config4Reading);
    }

    void checkIfAllParametersAreSet(NetworkConverterConfigGroup config){
        Map<String, String> params = config.getParams();
        for (Map.Entry<String, String> entry : params.entrySet()){
            assertNotNull(entry.getValue());
        }
    }

    void checkIfAllParametersSetsAreSet(NetworkConverterConfigGroup config){
        // LinkedAttrParamSet
        config.getParameterSets(LinkAttrParamSet.GROUP_NAME).forEach(group-> {
            LinkAttrParamSet linkAttrParamSet = (LinkAttrParamSet) group;
            // Check if the default values for all parameters are set
            Map<String, String> linkAttrParams = linkAttrParamSet.getParams();
            for (Map.Entry<String, String> entry : linkAttrParams.entrySet()){
                assertNotNull(entry.getValue());
            }
        });

        // ModeParamSet
        config.getParameterSets(ModeParamSet.GROUP_NAME).forEach(group-> {
            ModeParamSet modeParamSet = (ModeParamSet) group;
            // Check if the default values for all parameters are set
            Map<String, String> modeParams = modeParamSet.getParams();
            for (Map.Entry<String, String> entry : modeParams.entrySet()){
                assertNotNull(entry.getValue());
            }
        });

        // ConnectedNetworkParamSet
        config.getParameterSets(ConnectedNetworkParamSet.GROUP_NAME).forEach(group-> {
            ConnectedNetworkParamSet connectedNetworkParamSet = (ConnectedNetworkParamSet) group;
            // Check if the default values for all parameters are set
            Map<String, String> connectedNetworkParams = connectedNetworkParamSet.getParams();
            for (Map.Entry<String, String> entry : connectedNetworkParams.entrySet()){
                assertNotNull(entry.getValue());
            }
        });
    }

}