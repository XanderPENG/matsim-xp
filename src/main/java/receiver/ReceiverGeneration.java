package receiver;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.receiver.*;
import org.matsim.freight.receiver.collaboration.CollaborationUtils;
//import org.matsim.contrib.freightreceiver.Receiver;
//import org.matsim.contrib.freightreceiver.ReceiverUtils;
//import org.matsim.contrib.freightreceiver.Receivers;
//import org.matsim.contrib.freightreceiver.collaboration.CollaborationUtils;

import java.util.Set;

public class ReceiverGeneration {
    Receivers receivers = ReceiverUtils.createReceivers();

    /**
     *  Create 2 receiver types:
     *      3 receivers are in the coalition
     *      2 receivers are not
     */
    public void generateReceivers(){
        // 3 receivers in the coalition
        Set<Id<Link>> collaborativeReceiversLocations = Set.of(
                Id.createLinkId("j(4,1)R"),
                Id.createLinkId("j(8,3)R"),
                Id.createLinkId("i(9,2)"));

        for (Id<Link> location : collaborativeReceiversLocations){
            Receiver receiver = ReceiverUtils.newInstance(Id.create("collaborativeReceiver_" + location, Receiver.class));
            receiver.setLinkId(location);
            receiver.getAttributes().putAttribute(CollaborationUtils.ATTR_GRANDCOALITION_MEMBER, true);
            receiver.getAttributes().putAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS, true);
            receivers.addReceiver(receiver);
        }

        // 2 receivers not in the coalition
        Set<Id<Link>> nonCollaborativeReceiversLocations = Set.of(
                Id.createLinkId("j(1,7)"),
                Id.createLinkId("j(0,4)R"));

        for (Id<Link> location : nonCollaborativeReceiversLocations){
            Receiver receiver = ReceiverUtils.newInstance(Id.create("nonCollaborativeReceiver_" + location, Receiver.class));
            receiver.setLinkId(location);
            receiver.getAttributes().putAttribute(CollaborationUtils.ATTR_GRANDCOALITION_MEMBER, false);
            receiver.getAttributes().putAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS, false);
            receivers.addReceiver(receiver);
        }
    }

    public Receivers getReceivers() {
        return receivers;
    }
}
