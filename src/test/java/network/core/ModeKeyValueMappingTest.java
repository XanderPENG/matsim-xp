package network.core;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ModeKeyValueMappingTest {

    @Test
    void testBuildModeKeyValueMappingByAdd(){
        ModeKeyValueMapping mapping = new ModeKeyValueMapping.Builder()
                .setMode(TransMode.Mode.CAR)
                .addKeyValueMapping(Map.of("highway", "motorway"))
                .addKeyValueMapping(Map.of("highway", "trunk"))
                .addKeyValueMapping(Map.of("highway", "secondary"))
                .build();

        assertEquals(TransMode.Mode.CAR, mapping.getMode());

        assertEquals(3, mapping.getKeyValueMapping().size());
        assertTrue(mapping.getKeyValueMapping().contains(Map.of("highway", "motorway")));
        assertTrue(mapping.getKeyValueMapping().contains(Map.of("highway", "trunk")));
        assertTrue(mapping.getKeyValueMapping().contains(Map.of("highway", "secondary")));
    }

    @Test
    void testBuildModeKeyValueMappingBySet(){
        ModeKeyValueMapping mapping = new ModeKeyValueMapping.Builder()
                .setMode(TransMode.Mode.CAR)
                .setKeyValueMapping(Set.of(Map.of("highway", "motorway"), Map.of("highway", "trunk"), Map.of("highway", "secondary")))
                .build();

        assertEquals(TransMode.Mode.CAR, mapping.getMode());

        assertEquals(3, mapping.getKeyValueMapping().size());
        assertTrue(mapping.getKeyValueMapping().contains(Map.of("highway", "motorway")));
        assertTrue(mapping.getKeyValueMapping().contains(Map.of("highway", "trunk")));
        assertTrue(mapping.getKeyValueMapping().contains(Map.of("highway", "secondary")));
    }

}