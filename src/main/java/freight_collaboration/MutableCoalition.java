package freight_collaboration;

import java.util.Collection;
import java.util.Collections;

public class MutableCoalition implements Coalition {

    private final Collection<Collaborator> collaborators;

    public MutableCoalition() {
        collaborators = Collections.emptyList();
    }

    public MutableCoalition(Collection<Collaborator> collaborators) {
        this.collaborators = collaborators;
    }

    @Override
    public void addCollaborator(Collaborator collaborator) {
        collaborators.add(collaborator);
    }

    @Override
    public void removeCollaborator(Collaborator collaborator) {
        collaborators.remove(collaborator);
    }

    @Override
    public void addCollaborators(Collection<Collaborator> collaborators) {
        collaborators.addAll(collaborators);
    }

    @Override
    public void removeCollaborators(Collection<Collaborator> collaborators) {
        collaborators.removeAll(collaborators);
    }

    @Override
    public Collection<Collaborator> getCollaborators() {
        return collaborators;
    }
}
