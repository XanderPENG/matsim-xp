package freight_collaboration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;

public interface Collaborator<PL extends BasicPlan, AG extends HasPlansAndId<PL, AG>> {

    public Id<AG> getId();

    public boolean isPotentialCollaborator();

    public boolean isCollaborating();

    public Role getRole();

    public PL getPlan();

    public AG getPerson();

    public void setPotentialCollaborator(boolean isPotentialCollaborator);

    public void setCollaborating(boolean isCollaborating);

    public enum Role {
        SHIPPER,
        LSP,
        CARRIER,
        RECEIVER,
        OTHER
    }
}
