package network.run;

import network.config.ConnectedNetworkParamSet;
import network.config.LinkAttrParamSet;
import network.config.ModeParamSet;
import network.config.NetworkConverterConfigGroup;
import network.core.ModeKeyValueMapping;
import network.core.TransMode;
import network.tools.TransModeFactory;
import network.tools.Utils;

import java.util.Map;
import java.util.Set;

/**
 * Run this class to create a customized/default config file for the network converter.
 */
class RunCreateConfig {
//    NetworkConverterConfigGroup config;

    public static void main(String[] args) {
        // create a default config
//        NetworkConverterConfigGroup config = NetworkConverterConfigGroup.createDefaultConfig();
        // create a customized config (just for example)
        NetworkConverterConfigGroup config = new NetworkConverterConfigGroup("osm", "EPSG:4326", Utils.outputCrs, Utils.FILE_TEST_NETWORK_PBF,
                true, true,
                Utils.FILE_TEST_NETWORK_XML,
                "NA", "NA",
                true,  Map.of("oneway", "yes"));

        // Create the default TransModes quickly
        TransMode bikeMode = TransModeFactory.BIKE;
        TransMode carMode = TransModeFactory.CAR;
        TransMode ptMode = TransModeFactory.PT;
        TransMode walkMode = TransModeFactory.WALK;
        TransMode otherMode = TransModeFactory.OTHER;

        // create customized ModeParamSets
        ModeParamSet bikeParamSet = new ModeParamSet(bikeMode);
        ModeParamSet carParamSet = new ModeParamSet(carMode);
        ModeParamSet ptParamSet = new ModeParamSet(ptMode);
        ModeParamSet walkParamSet = new ModeParamSet(walkMode);
        ModeParamSet otherParamSet = new ModeParamSet(otherMode);

        // create a customized ConnectedNetworkParamSet
        Set<TransMode.Mode> modes = Set.of(TransMode.Mode.CAR, TransMode.Mode.PT, TransMode.Mode.BIKE, TransMode.Mode.WALK, TransMode.Mode.OTHER);
        ConnectedNetworkParamSet connectedNetworkParamSet = new ConnectedNetworkParamSet(false, modes, "reduce");

        // create a customized LinkAttrParamSet
        LinkAttrParamSet linkAttrParamSet = new LinkAttrParamSet("max_speed", "capacity", "lanes", "width", "length",
                Set.of("surface", "lit", "highway"), Map.of("MAX_SPEED_FIELD", "km/h", "WIDTH_FIELD", "m", "LENGTH_FIELD", "m"));

        // Add customized Parameter sets
        config.addParameterSet(bikeParamSet);
        config.addParameterSet(carParamSet);
        config.addParameterSet(ptParamSet);
        config.addParameterSet(walkParamSet);
        config.addParameterSet(otherParamSet);
        config.addParameterSet(connectedNetworkParamSet);
        config.addParameterSet(linkAttrParamSet);

        // write the config file
        config.writeConfigFile(Utils.FILE_TEST_CONFIG);
    }







}

