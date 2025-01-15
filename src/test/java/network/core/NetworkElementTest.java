package network.core;

import org.matsim.core.utils.collections.Tuple;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NetworkElementTest {


    /**
     * <p>
     * Create a simple network as below：
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
     *     <td>↘</td>
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
        //
    }

}