package network.tools;

import network.core.ModeKeyValueMapping;
import network.core.TransMode;

import java.util.Map;

public class TransModeFactory {
    public static TransMode CAR = new TransMode(TransMode.Mode.CAR, new ModeKeyValueMapping.Builder()
            .setMode(TransMode.Mode.CAR)
            .addKeyValueMapping(Map.of("highway", "motorway"))
            .addKeyValueMapping(Map.of("highway", "trunk"))
            .addKeyValueMapping(Map.of("highway", "secondary"))
            .addKeyValueMapping(Map.of("highway", "primary"))
            .addKeyValueMapping(Map.of("highway", "tertiary"))
            .addKeyValueMapping(Map.of("highway", "unclassified"))
            .addKeyValueMapping(Map.of("highway", "road"))
            .addKeyValueMapping(Map.of("highway", "residential"))
            .addKeyValueMapping(Map.of("highway", "motorway_link"))
            .addKeyValueMapping(Map.of("highway", "trunk_link"))
            .addKeyValueMapping(Map.of("highway", "primary_link"))
            .addKeyValueMapping(Map.of("highway", "tertiary_link"))
            .addKeyValueMapping(Map.of("highway", "service"))
            .addKeyValueMapping(Map.of("highway", "living_street"))
            .addKeyValueMapping(Map.of("highway", "track"))
            .addKeyValueMapping(Map.of("highway", "primary"))
            .build(), 130 / 3.6, 0.242, 3.5, 2);

    public static TransMode PT = new TransMode(TransMode.Mode.PT, new ModeKeyValueMapping.Builder()
            .setMode(TransMode.Mode.PT)
            .addKeyValueMapping(Map.of("route", "bus"))
            .addKeyValueMapping(Map.of("route", "trolleybus"))
            .addKeyValueMapping(Map.of("route", "train"))
            .addKeyValueMapping(Map.of("route", "light_rail"))
            .addKeyValueMapping(Map.of("route", "subway"))
            .addKeyValueMapping(Map.of("route", "tram"))
            .addKeyValueMapping(Map.of("*", "busway"))
            .build(), 40 / 3.6, 0.142, 3.5, 1);

    public static TransMode TRAIN = new TransMode(TransMode.Mode.TRAIN, new ModeKeyValueMapping.Builder()
            .setMode(TransMode.Mode.TRAIN)
            .addKeyValueMapping(Map.of("route", "train"))
            .build(), 100 / 3.6, 0.542, 5, 1);

    public static TransMode BIKE = new TransMode(TransMode.Mode.BIKE, new ModeKeyValueMapping.Builder()
            .setMode(TransMode.Mode.BIKE)
            .addKeyValueMapping(Map.of("bicycle", "yes"))
            .addKeyValueMapping(Map.of("highway", "*", "cycleway", "lane"))
            .addKeyValueMapping(Map.of("highway", "*", "cycleway:left", "lane"))
            .addKeyValueMapping(Map.of("highway", "*", "cycleway:right", "lane"))
            .addKeyValueMapping(Map.of("highway", "*", "cycleway:both", "lane"))
            .addKeyValueMapping(Map.of("highway", "cycleway"))
            .addKeyValueMapping(Map.of("highway", "*", "cycleway:right", "track"))
            .addKeyValueMapping(Map.of("highway", "tertiary"))
            .addKeyValueMapping(Map.of("highway", "living_street"))
            .addKeyValueMapping(Map.of("highway", "service"))
            .addKeyValueMapping(Map.of("highway", "unclassified"))
            .addKeyValueMapping(Map.of("highway", "track"))
            .addKeyValueMapping(Map.of("highway", "residential"))
            .addKeyValueMapping(Map.of("highway", "path"))
            .addKeyValueMapping(Map.of("highway", "footway"))
            .addKeyValueMapping(Map.of("highway", "pedestrian"))
            .addKeyValueMapping(Map.of("ramp:bicycle", "yes"))
            .addKeyValueMapping(Map.of("ramp", "yes"))
            .addKeyValueMapping(Map.of("bicycle", "designated"))
            .addKeyValueMapping(Map.of("bicycle", "optional_sidepath"))
            .addKeyValueMapping(Map.of("bicycle", "permissive"))
            .addKeyValueMapping(Map.of("bicycle", "destination"))
            .addKeyValueMapping(Map.of("bicycle", "private"))
            .addKeyValueMapping(Map.of("bicycle", "customers"))
            .addKeyValueMapping(Map.of("segregated", "*"))
            .addKeyValueMapping(Map.of("cycleway", "shared_lane"))
            .addKeyValueMapping(Map.of("cycleway", "shared"))
            .addKeyValueMapping(Map.of("cycleway", "shared_busway"))
            .addKeyValueMapping(Map.of("cycleway", "crossing"))
            .addKeyValueMapping(Map.of("cycleway:left", "*"))
            .addKeyValueMapping(Map.of("cycleway:right", "*"))
            .addKeyValueMapping(Map.of("cycleway:both", "*"))
            .addKeyValueMapping(Map.of("cyclestreet", "yes"))
            .build(), 20 / 3.6, 0, 2, 1);

    public static TransMode WALK = new TransMode(TransMode.Mode.WALK, new ModeKeyValueMapping.Builder()
            .setMode(TransMode.Mode.WALK)
            .addKeyValueMapping(Map.of("highway", "footway"))
            .addKeyValueMapping(Map.of("highway", "pedestrian"))
            .addKeyValueMapping(Map.of("highway", "steps"))
            .addKeyValueMapping(Map.of("highway", "path"))
            .addKeyValueMapping(Map.of("highway", "track"))
            .addKeyValueMapping(Map.of("highway", "service"))
            .addKeyValueMapping(Map.of("highway", "living_street"))
            .build(), 5 / 3.6, 0, 1, 1);

    public static TransMode SHIP = new TransMode(TransMode.Mode.SHIP, new ModeKeyValueMapping.Builder()
            .setMode(TransMode.Mode.SHIP)
            .addKeyValueMapping(Map.of("route", "ferry"))
            .build(), 20 / 3.6, 0.142,  10, 1);

    public static TransMode OTHER = new TransMode(TransMode.Mode.OTHER, new ModeKeyValueMapping.Builder()
            .setMode(TransMode.Mode.OTHER)
            .addKeyValueMapping(Map.of("highway", "*"))
            .build(), 20 / 3.6, 0.142, 3.5, 1);
}
