package network;

import network.readers.Reader;
import network.tools.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ShpReaderTest {
    private final static Logger LOG = LogManager.getLogger(ShpReaderTest.class);

    @Test
    public void testRead(){
        LOG.info("Testing ShpReader");
        Reader shpReader = new network.readers.ShpReader("EPSG:4326");
        shpReader.read(Utils.INPUT_HK_ROAD_NETWORK);

        Assertions.assertFalse(shpReader.getRawLinks().isEmpty());
    }
}
