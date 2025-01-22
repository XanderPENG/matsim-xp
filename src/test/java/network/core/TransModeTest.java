package network.core;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransModeTest {

    @Test
    void testAllValuesAreSetCorrect(){
        TransMode bikeMode = createTransMode();

        assertEquals(TransMode.Mode.BIKE, bikeMode.getMode());
        assertEquals(25, bikeMode.getDefaultMaxSpeed());
        assertEquals(0.0, bikeMode.getDefaultEmissionFactor());
        assertEquals(2.0, bikeMode.getDefaultLaneWidth());
        assertEquals(1.0, bikeMode.getDefaultLanes());
        assertEquals(1, bikeMode.getOnewayKeyValueMapping().size());
        assertTrue(bikeMode.getOnewayKeyValueMapping().contains(Map.of("isOneWay", "true")));
        assertEquals(3, bikeMode.getModeKeyValueMapping().getKeyValueMapping().size());
        assertTrue(bikeMode.getModeKeyValueMapping().getKeyValueMapping().contains(Map.of("highway", "trunk")));
        assertTrue(bikeMode.getModeKeyValueMapping().getKeyValueMapping().contains(Map.of("highway", "secondary")));
        assertTrue(bikeMode.getModeKeyValueMapping().getKeyValueMapping().contains(Map.of("highway", "*", "cycleway:both", "lane")));

    }

    @Test
    void testLinkMatching(){
        TransMode bikeMode = createTransMode();

        // test if the link matches the bike mode
        NetworkElement.Node startNode = new NetworkElement.Node("startNode", 0.0, 0.0);
        NetworkElement.Node endNode = new NetworkElement.Node("endNode", 0.0, 1.0);
        NetworkElement.Link testLink = new NetworkElement.Link("testLink", startNode, endNode);

        testLink.addKeyValuePair("highway", "bicycle");
        testLink.addKeyValuePair("cycleway:both", "lane");
        assertTrue(bikeMode.matchLinkTransMode(testLink));

        testLink.addKeyValuePair("cycleway:both", "false");
        assertFalse(bikeMode.matchLinkTransMode(testLink));

        // test if the link is a oneway link
        testLink.addKeyValuePair("isOneWay", "true");
        assertTrue(bikeMode.matchLinkOneway(testLink));

        testLink.addKeyValuePair("isOneWay", "false");
        assertFalse(bikeMode.matchLinkOneway(testLink));

    }


    TransMode createTransMode(){
        // create a bike mode with some key value mappings
        ModeKeyValueMapping mapping = new ModeKeyValueMapping.Builder()
                .setMode(TransMode.Mode.BIKE)
                .addKeyValueMapping(Map.of("highway", "trunk"))
                .addKeyValueMapping(Map.of("highway", "secondary"))
                .addKeyValueMapping(Map.of("highway","*", "cycleway:both", "lane"))
                .addKeyValueMapping(Map.of("modes", "*car*"))
                .addKeyValueMapping(Map.of("modes", "cycle*"))
                .addKeyValueMapping(Map.of("modes", "*pt"))
                .build();
        Set<Map<String, String>> onewayKeyValueMapping = Set.of(Map.of("isOneWay", "true"));
        return new TransMode(TransMode.Mode.BIKE, mapping, onewayKeyValueMapping, 25, 0.0, 2.0, 1.0);
    }
}