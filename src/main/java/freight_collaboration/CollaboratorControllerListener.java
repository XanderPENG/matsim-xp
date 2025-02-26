package freight_collaboration;

import jakarta.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.freight.carriers.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

class CollaboratorControllerListener implements IterationStartsListener {

    @Inject
    private Scenario sc;

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {

        // Create a coalition of collaborators
        MutableCoalition grandCoalition = new MutableCoalition();
        // Add the collaborators to the coalition
        Collaborators collaborators = (Collaborators) sc.getScenarioElement("collaborators");

        for (Collaborator collaborator : collaborators.getCollaborators()) {
            if (collaborator.isPotentialCollaborator() && collaborator.isCollaborating()) {
                grandCoalition.addCollaborator(collaborator);
            }
        }

        // Create a new coalition of collaborative carriers
        MutableCoalition carrierCoalition = new MutableCoalition();
        // Add the carriers to the coalition
        for (Collaborator collaborator : grandCoalition.getCollaborators()) {
            if (collaborator.getRole() == Collaborator.Role.CARRIER) {
                carrierCoalition.addCollaborator(collaborator);
            }
        }

        // clean the carrier plan if this is the first iteration
        if (event.getIteration() == 0) {
            for (Collaborator collaborator : carrierCoalition.getCollaborators()) {
                Carrier carrier = (Carrier) collaborator.getPerson();
                carrier.clearPlans();
            }
        }

        /*
         * The following code snippet is a simplified version of the code that would be used to assign a shipment to a carrier.
         * 1. Extract the shipment from each carrier, and add to a public pool of shipments.
         * 2. Extract the depots from each carrier, and add to a public pool of depots.
         * 3. For-loop through the shipments, and assign each shipment to the nearest depot/carrier.
         * 4. Create a new Carrier Plan without routes.
         * 5. Run Jsprit to calculate the routes for each carrier.
         */
        Map<Id<Link>, CarrierService> shipments = new HashMap<>();
        Map<Id<Link>, Carrier> depots = new HashMap<>();
        for (Collaborator collaborator : carrierCoalition.getCollaborators()) {
            Carrier carrier = (Carrier) collaborator.getPerson();
            for (CarrierService service : carrier.getServices().values()) {
                shipments.put(service.getLocationLinkId(), service);
            }
            for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
                Id<Link> depot = vehicle.getLinkId();
                depots.put(depot, carrier);
            }
        }
        // step 3
        // clean services from carriers
        for (Collaborator collaborator : carrierCoalition.getCollaborators()) {
            Carrier carrier = (Carrier) collaborator.getPerson();
            carrier.getServices().clear();
        }
        for (CarrierService service : shipments.values()) {
            Id<Link> shipmentLocation = service.getLocationLinkId();
            Carrier nearestCarrier = null;
            double minDistance = Double.MAX_VALUE;
            for (Id<Link> depot : depots.keySet()) {
                double distance = NetworkUtils.getEuclideanDistance(sc.getNetwork().getLinks().get(depot).getToNode().getCoord(), sc.getNetwork().getLinks().get(shipmentLocation).getToNode().getCoord());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestCarrier = depots.get(depot);
                }
            }
            if (nearestCarrier != null) {
                nearestCarrier.getServices().put(service.getId(), service);
            }
        }

        // Run Jsprit to calculate the routes for each carrier
        try {
            CarriersUtils.runJsprit(sc);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // set scores for each new plan
        for (Collaborator collaborator : carrierCoalition.getCollaborators()) {
            Carrier carrier = (Carrier) collaborator.getPerson();
            carrier.getSelectedPlan().setScore(carrier.getSelectedPlan().getJspritScore());
        }

    }
}
