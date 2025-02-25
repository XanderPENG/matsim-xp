package freight_collaboration;

import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.CarrierShipment;

import java.util.Collection;
import java.util.List;

public class CarrierCollaborativeTransportStrategy {
    private final Collection<Collaborator<CarrierPlan, Carrier>> collaborativeCarriers;

    public CarrierCollaborativeTransportStrategy(Collection<Collaborator<CarrierPlan, Carrier>> collaborativeCarriers) {
        this.collaborativeCarriers = collaborativeCarriers;
    }

    /**
     * This method is used to share distant orders of the carriers and re-route them to the nearest carrier.
     * 1. Extract the distant orders from the carriers.
     */
    private void shareOrdersAndReRouting(){

    }


}
