package freight_collaboration;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.replanning.GenericStrategyManager;

public interface CollaborationStrategyManager<PL extends BasicPlan, AG extends HasPlansAndId<PL, AG>> extends GenericStrategyManager<PL, AG> {
}
