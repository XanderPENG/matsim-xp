package network.core;

import org.junit.jupiter.api.Test;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NetworkElementTest {


    @Test
    void testAddAndGetRelatedLinks(){
        // create a sample network
        Tuple<Set<NetworkElement.Node>, Set<NetworkElement.Link>> networkElements = createSampleNetworkElements();
        // add related links to Node(0,0) -> Link(00-22), Link(04-00), Link(40-00)
        // get Node(0,0)
        NetworkElement.Node node00 = networkElements.getFirst().stream().filter(node -> node.getId().equals("(0,0)")).findFirst().orElse(null);
        assertNotNull(node00);

        assertDoesNotThrow(
            () -> {
                node00.addRelatedLink(networkElements.getSecond().stream().filter(link -> link.getId().equals("00-22")).findFirst().orElse(null));
                node00.addRelatedLink(networkElements.getSecond().stream().filter(link -> link.getId().equals("04-00")).findFirst().orElse(null));
                node00.addRelatedLink(networkElements.getSecond().stream().filter(link -> link.getId().equals("40-00")).findFirst().orElse(null));
            }
        );

        // get related links from Node(0,0)
        Map<String, NetworkElement.Link> relatedLinks = node00.getRelatedLinks();
        assertEquals(3, relatedLinks.size());
        assertEquals("00-22", relatedLinks.get("00-22").getId());
        assertEquals("04-00", relatedLinks.get("04-00").getId());
        assertEquals("40-00", relatedLinks.get("40-00").getId());
    }

    @Test
    void testSetAndGetRelatedLinks(){
        // create a sample network
        Tuple<Set<NetworkElement.Node>, Set<NetworkElement.Link>> networkElements = createSampleNetworkElements();
        // create a set of relatedLink ([Link(00-22), Link(22,44), Link(22,40), Link(22,04)]) to Node(2,2)
        NetworkElement.Node node22 = networkElements.getFirst().stream().filter(node -> node.getId().equals("(2,2)")).findFirst().orElse(null);
        assertNotNull(node22);

        Set<NetworkElement.Link> relatedLinks = new HashSet<>();
        assertDoesNotThrow(
            () -> {
                relatedLinks.add(networkElements.getSecond().stream().filter(link -> link.getId().equals("00-22")).findFirst().orElse(null));
                relatedLinks.add(networkElements.getSecond().stream().filter(link -> link.getId().equals("22-44")).findFirst().orElse(null));
                relatedLinks.add(networkElements.getSecond().stream().filter(link -> link.getId().equals("22-40")).findFirst().orElse(null));
                relatedLinks.add(networkElements.getSecond().stream().filter(link -> link.getId().equals("22-04")).findFirst().orElse(null));
            }
        );
        node22.addRelatedLinks(relatedLinks);

        // get related links from Node(2,2)
        Map<String, NetworkElement.Link> relatedLinksFromNode22 = node22.getRelatedLinks();
        assertEquals(4, relatedLinksFromNode22.size());
        assertEquals("00-22", relatedLinksFromNode22.get("00-22").getId());
        assertEquals("22-44", relatedLinksFromNode22.get("22-44").getId());
        assertEquals("22-40", relatedLinksFromNode22.get("22-40").getId());
        assertEquals("22-04", relatedLinksFromNode22.get("22-04").getId());
    }

    @Test
    void testIfAllNodesAreSetCorrectForLink(){
        // create a sample network
        Tuple<Set<NetworkElement.Node>, Set<NetworkElement.Link>> networkElements = createSampleNetworkElements();
        // Take the Link(04-00) as an example
        NetworkElement.Link link04To00 = networkElements.getSecond().stream().filter(link -> link.getId().equals("04-00")).findFirst().orElse(null);
        assertNotNull(link04To00);

        // Check if the From and To nodes are set correctly
        assertEquals("(0,4)", link04To00.getFromNode().getId());
        assertEquals("(0,0)", link04To00.getToNode().getId());

        // Add composed nodes->Node(0,1),Node(0,2),Node(0,3) to the link
        NetworkElement.Node node01 = networkElements.getFirst().stream().filter(node -> node.getId().equals("(0,1)")).findFirst().orElse(null);
        NetworkElement.Node node02 = networkElements.getFirst().stream().filter(node -> node.getId().equals("(0,2)")).findFirst().orElse(null);
        NetworkElement.Node node03 = networkElements.getFirst().stream().filter(node -> node.getId().equals("(0,3)")).findFirst().orElse(null);
        assertNotNull(node01);
        assertNotNull(node02);
        assertNotNull(node03);

        assertDoesNotThrow(
            () -> {
                link04To00.addComposedNode(node01);
                link04To00.addComposedNode(node02);
                link04To00.addComposedNode(node03);
            }
        );

        // Check if the composed nodes are set correctly
        assertEquals(3, link04To00.getComposedNodes().size());
        assertEquals(link04To00.getComposedNodes().get("(0,1)"), node01);
        assertEquals(link04To00.getComposedNodes().get("(0,2)"), node02);
        assertEquals(link04To00.getComposedNodes().get("(0,3)"), node03);

        // clear the composed nodes
        link04To00.getComposedNodes().clear();
        assertEquals(0, link04To00.getComposedNodes().size());

        // check the sequence of the composed nodes
        int i = 0;
        for (NetworkElement.Node node : link04To00.getComposedNodes().values()) {
            switch (i) {
                case 0:
                    assertEquals(node01, node);
                    break;
                case 1:
                    assertEquals(node02, node);
                    break;
                case 2:
                    assertEquals(node03, node);
                    break;
                default:
                    fail("Unexpected node");
            }
            i++;
        }

    }

    @Test
    void setAndGetAllowedModesForLink(){
        // create a sample network
        Tuple<Set<NetworkElement.Node>, Set<NetworkElement.Link>> networkElements = createSampleNetworkElements();
        // Take the Link(04-00) as an example
        NetworkElement.Link link04To00 = networkElements.getSecond().stream().filter(link -> link.getId().equals("04-00")).findFirst().orElse(null);
        assertNotNull(link04To00);

        // Add allowed modes to the link
        assertDoesNotThrow(
            () -> {
                link04To00.addAllowedMode(TransMode.Mode.CAR);
                link04To00.addAllowedMode(TransMode.Mode.BIKE);
                link04To00.addAllowedModes(Set.of(TransMode.Mode.WALK, TransMode.Mode.PT));
            }
        );

        // Check if the allowed modes are set correctly
        assertEquals(4, link04To00.getAllowedModes().size());
        assertTrue(link04To00.getAllowedModes().contains(TransMode.Mode.CAR));
        assertTrue(link04To00.getAllowedModes().contains(TransMode.Mode.BIKE));
        assertTrue(link04To00.getAllowedModes().contains(TransMode.Mode.WALK));
        assertTrue(link04To00.getAllowedModes().contains(TransMode.Mode.PT));
    }

    @Test
    void setAndGetKeyValuePairsForLink(){
        // create a sample network
        Tuple<Set<NetworkElement.Node>, Set<NetworkElement.Link>> networkElements = createSampleNetworkElements();
        // Take the Link(04-00) as an example
        NetworkElement.Link link04To00 = networkElements.getSecond().stream().filter(link -> link.getId().equals("04-00")).findFirst().orElse(null);
        assertNotNull(link04To00);

        // Add key-value pairs to the link
        assertDoesNotThrow(
            () -> {
                link04To00.setKeyValuePairs(Map.of("highway", "truck", "oneway", "yes", "bicycle", "no"));
                link04To00.addKeyValuePair("max_speed", "30");
                link04To00.addKeyValuePair("lanes", "2");
            }
        );

        // Check if the key-value pairs are set correctly
        assertEquals(5, link04To00.getKeyValuePairs().size());
        assertEquals("truck", link04To00.getKeyValuePairs().get("highway"));
        assertEquals("yes", link04To00.getKeyValuePairs().get("oneway"));
        assertEquals("no", link04To00.getKeyValuePairs().get("bicycle"));
        assertEquals("30", link04To00.getKeyValuePairs().get("max_speed"));
        assertEquals("2", link04To00.getKeyValuePairs().get("lanes"));
    }



    /**
     * <p>
     * Create a simple network as below with：
     *  <li> 5 Start/End nodes </li>
     *  <li> 16 Composed nodes </li>
     *  <li> 8 Links </li>
     * </p>
     * <pre>
     * <table border="1" cellspacing="0" cellpadding="5" style="border-collapse:collapse; text-align:center;">
     *   <tr>
     *     <td>·</td>
     *     <td>→</td>
     *     <td>→</td>
     *     <td>→</td>
     *     <td>·</td>
     *   </tr>
     *   <tr>
     *     <td>↓</td>
     *     <td>↖</td>
     *     <td></td>
     *     <td>↗</td>
     *     <td>↓</td>
     *   </tr>
     *   <tr>
     *     <td>↓</td>
     *     <td></td>
     *     <td>·</td>
     *     <td></td>
     *     <td>↓</td>
     *   </tr>
     *   <tr>
     *     <td>↓</td>
     *     <td>↗</td>
     *     <td></td>
     *     <td>↘</td>
     *     <td>↓</td>
     *   </tr>
     *   <tr>
     *     <td>·</td>
     *     <td>←</td>
     *     <td>←</td>
     *     <td>←</td>
     *     <td>·</td>
     *   </tr>
     * </table>
     * </pre>
     *
     * <p>
     *
     * <ul>
     *   <li>Each <b>·</b> denotes a Start/End Node</li>
     *   <li>Each <b> arrow </b> denotes both the direction and a composed Node (Not a specific link) </li>
     *   <li> There is a Link between two specific Nodes with the direction shown as the arrow</li>
     * </ul>
     * </p>
     */
    Tuple<Set<NetworkElement.Node>, Set<NetworkElement.Link>> createSampleNetworkElements() {
        Set<NetworkElement.Node> nodes = new HashSet<>();
        Set<NetworkElement.Link> links = new HashSet<>();

        // Create Start/End Nodes
        String[] startEndNodeIds = {"(0,0)", "(0,4)", "(4,0)", "(4,4)", "(2,2)"};
        int[][] startEndNodeCoords = {{0, 0}, {0, 4}, {4, 0}, {4, 4}, {2, 2}};
        NetworkElement.Node[] intersectionNodes = new NetworkElement.Node[startEndNodeIds.length];
        for (int i = 0; i < startEndNodeIds.length; i++) {
            intersectionNodes[i] = new NetworkElement.Node(startEndNodeIds[i], startEndNodeCoords[i][0], startEndNodeCoords[i][1]);
            nodes.add(new NetworkElement.Node(startEndNodeIds[i], startEndNodeCoords[i][0], startEndNodeCoords[i][1]));
        }

        // Create Composed Nodes
        String[] composedNodeIds = {"(0,1)", "(0,2)", "(0,3)", "(1,0)", "(2,0)", "(3,0)", "(4,1)", "(4,2)", "(4,3)", "(1,4)", "(2,4)", "(3,4)", "(1,1)", "(1,3)", "(3,1)", "(3,3)"};
        int[][] composedNodeCoords = {{0, 1}, {0, 2}, {0, 3}, {1, 0}, {2, 0}, {3, 0}, {4, 1}, {4, 2}, {4, 3}, {1, 4}, {2, 4}, {3, 4}, {1, 1}, {1, 3}, {3, 1}, {3, 3}};
        for (int i = 0; i < composedNodeIds.length; i++) {
            nodes.add(new NetworkElement.Node(composedNodeIds[i], composedNodeCoords[i][0], composedNodeCoords[i][1]));
        }


        // Create Links
        String[] linkIds = {"00-22", "22-44", "22-40", "22-04", "04-00", "04-44", "40-00", "44-40"};
        NetworkElement.Node[][] linkNodes = {
                {intersectionNodes[0], intersectionNodes[4]},
                {intersectionNodes[4], intersectionNodes[3]},
                {intersectionNodes[4], intersectionNodes[2]},
                {intersectionNodes[4], intersectionNodes[1]},
                {intersectionNodes[1], intersectionNodes[0]},
                {intersectionNodes[1], intersectionNodes[3]},
                {intersectionNodes[2], intersectionNodes[0]},
                {intersectionNodes[3], intersectionNodes[2]}
        };
        for (int i = 0; i < linkIds.length; i++) {
            links.add(new NetworkElement.Link(linkIds[i], linkNodes[i][0], linkNodes[i][1]));
        }

        return new Tuple<>(nodes, links);
    }

}