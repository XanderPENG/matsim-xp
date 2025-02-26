package freight_collaboration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;

public class CollaboratorImpl<PL extends BasicPlan, AG extends HasPlansAndId<PL, AG>> implements Collaborator<PL, AG> {

    private final AG person;
    private final Role role;

    private boolean isPotentialCollaborator;
    private boolean isCollaborating;

    public CollaboratorImpl(AG person, Role role) {
        this.person = person;
        this.role = role;
    }

    public CollaboratorImpl(AG person, Role role, boolean isPotentialCollaborator, boolean isCollaborating) {
        this.person = person;
        this.role = role;
        this.isPotentialCollaborator = isPotentialCollaborator;
        this.isCollaborating = isCollaborating;
    }

    @Override
    public Id<AG> getId() {
        return person.getId();
    }

    @Override
    public boolean isPotentialCollaborator() {
        return isPotentialCollaborator;
    }

    @Override
    public boolean isCollaborating() {
        return isCollaborating;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public PL getPlan() {
        return person.getSelectedPlan();
    }

    @Override
    public AG getPerson() {
        return person;
    }

    @Override
    public void setPotentialCollaborator(boolean isPotentialCollaborator) {
        this.isPotentialCollaborator = isPotentialCollaborator;
    }

    @Override
    public void setCollaborating(boolean isCollaborating) {
        this.isCollaborating = isCollaborating;
    }
}
