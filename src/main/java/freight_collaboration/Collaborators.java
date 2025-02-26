package freight_collaboration;

import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.Carrier;

import java.util.*;

public class Collaborators {
    private final List<Collaborator> collaborators = new ArrayList<>();

    public Collaborators(Collection<Collaborator> collaborators) {
        makeList(collaborators);
    }

    public Collaborators() {
    }

//    private void makeMap(Collection<Collaborator> collaborators) {
//        for (Collaborator collaborator : collaborators) {
//            this.collaborators.put(collaborator.getId(), collaborator);
//        }
//    }

    private void makeList(Collection<Collaborator> collaborators) {
        this.collaborators.addAll(collaborators);
    }

    public List<Collaborator> getCollaborators() {
        return collaborators;
    }

    public void addCollaborator(Collaborator collaborator) {
        collaborators.add(collaborator);
    }
}
