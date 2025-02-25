package freight_collaboration;

import java.util.Collection;

public interface Coalition {
    public Collection<Collaborator> getCollaborators();
    public void addCollaborator(Collaborator collaborator);
    public void removeCollaborator(Collaborator collaborator);
    public void addCollaborators(Collection<Collaborator> collaborators);
    public void removeCollaborators(Collection<Collaborator> collaborators);
}
