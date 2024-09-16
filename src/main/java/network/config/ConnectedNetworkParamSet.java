package network.config;

import network.core.TransMode;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashSet;
import java.util.Set;

public class ConnectedNetworkParamSet extends ReflectiveConfigGroup implements MatsimParameters {

    public static final String GROUP_NAME = "connectedNetworkParamSet";

    @Parameter
    @Comment("If true, the network will be processed to be strongly connected, which means that each node/link can be reached from any other node/link.")
    public boolean STRONGLY_CONNECTED;

    @Comment("The mode of the network, which should be any of the following: [car, pt, train, bike, walk, ship, other]. see TransMode.Mode for more details.")
    public Set<TransMode.Mode> MODE = new HashSet<>();

    @Parameter
    @Comment("""
            The method to process the network, which should be one of the following:
            \t\t\t\t1. `reduce`: remove the isolated nodes/links, and only keep the largest connected subnetwork;
            \t\t\t\t2. `insert`: connect all the isolated nodes/links to the nearest node/link;
            \t\t\t\t3. `adapt_mode`: adapt and add TransMode for some links to make the (sub)network strongly connected. (e.g., add a bike mode for the car-tagged links)""")

    public String METHOD;

    public ConnectedNetworkParamSet() {
        super(GROUP_NAME);
    }

    public ConnectedNetworkParamSet(boolean stronglyConnected, Set<TransMode.Mode> modes, String method) {
        super(GROUP_NAME);
        this.STRONGLY_CONNECTED = stronglyConnected;
        this.MODE = modes;
        this.METHOD = method;
    }

    @StringGetter("MODE")
    public String getModeString() {
        StringBuilder sb = new StringBuilder();
        for (TransMode.Mode mode : MODE) {
            sb.append(mode.name).append(", ");
        }
        return sb.toString().trim(); // remove the last comma
    }

    @StringSetter("MODE")
    public void setModeString(String modeString) {
        // Create a new HashSet to store the parsed
        Set<TransMode.Mode> set = new HashSet<>();
        // Split the input string by commas to get individual mode strings
        for (String mode : modeString.split(",")) {
            set.add(TransMode.Mode.valueOf(mode.toUpperCase().trim()));
        }
        this.MODE = set;
    }

}

