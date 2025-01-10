package network.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NetworkConverterConfigGroupTest {

    @Test
    void TestAllParametersAreWritten(){
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
        assertNotNull(config);

    }
}