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

        // Create customized TransMode
        TransMode bikeMode = new TransMode(TransMode.Mode.BIKE, new ModeKeyValueMapping.Builder()
                .addKeyValueMapping(Map.of("highway", "cycleway"))
                .addKeyValueMapping(Map.of("bicycle", "yes"))
                .build(),
                20 / 3.6, 0,  2, 1);

        TransMode carMode = new TransMode(TransMode.Mode.CAR, new ModeKeyValueMapping.Builder()
                .addKeyValueMapping(Map.of("highway", "motorway"))
                .addKeyValueMapping(Map.of("highway", "trunk"))
                .build(),
                130 / 3.6, 0.242,  3.5, 2);

        TransMode ptMode = new TransMode(TransMode.Mode.PT, new ModeKeyValueMapping.Builder()
                .addKeyValueMapping(Map.of("route", "bus"))
                .addKeyValueMapping(Map.of("route", "trolleybus"))
                .addKeyValueMapping(Map.of("route", "share_taxi"))
                .addKeyValueMapping(Map.of("route", "train"))
                .addKeyValueMapping(Map.of("route", "light_rail"))
                .addKeyValueMapping(Map.of("route", "subway"))
                .addKeyValueMapping(Map.of("route", "tram"))
                .build(),
                40 / 3.6, 0.142, 3.5, 1);

        TransMode walkMode = new TransMode(TransMode.Mode.WALK, new ModeKeyValueMapping.Builder()
                .addKeyValueMapping(Map.of("highway", "footway"))
                .addKeyValueMapping(Map.of("highway", "pedestrian"))
                .addKeyValueMapping(Map.of("highway", "steps"))
                .build(),
                5 / 3.6, 0, 1, 1);

        TransMode otherMode = new TransMode(TransMode.Mode.OTHER, new ModeKeyValueMapping.Builder()
                .addKeyValueMapping(Map.of("highway", "*"))
                .build(),
                20 / 3.6, 0, 2, 1);

        // Create the default TransModes quickly
        bikeMode = TransModeFactory.BIKE;
        carMode = TransModeFactory.CAR;
        ptMode = TransModeFactory.PT;
        walkMode = TransModeFactory.WALK;
        otherMode = TransModeFactory.OTHER;

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

