package freight_collaboration;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;

public class Collaborator<PL extends BasicPlan, AG extends HasPlansAndId<PL, AG>>{

    private final Role role;
    private final AG person;

    private boolean potentialCollaborator;
    private boolean isCollaborating;

    public Collaborator(Role role, AG person) {
        this.role = role;
        this.person = person;
        this.potentialCollaborator = true;
        this.isCollaborating = true;
    }

    public Collaborator(Role role, AG person, boolean potentialCollaborator, boolean isCollaborating) {
        this.role = role;
        this.person = person;
        this.potentialCollaborator = potentialCollaborator;
        this.isCollaborating = isCollaborating;
    }

    public boolean isPotentialCollaborator() {
        return potentialCollaborator;
    }

    public void setPotentialCollaborator(boolean potentialCollaborator) {
        this.potentialCollaborator = potentialCollaborator;
    }

    public boolean isCollaborating() {
        return isCollaborating;
    }

    public void setCollaborating(boolean collaborating) {
        isCollaborating = collaborating;
    }

    public Role getRole() {
        return role;
    }

    public HasPlansAndId<PL, AG> getPerson() {
        return person;
    }

    public enum Role {
        SHIPPER,
        LSP,
        CARRIER,
        RECEIVER
    }
}
