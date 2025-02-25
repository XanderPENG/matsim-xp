package freight_collaboration;

import jakarta.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

class CollaboratorControllerListener implements IterationStartsListener {

    @Inject
    private Scenario sc;

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (event.getIteration() == 0) {


        }
    }
}
