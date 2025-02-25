package freight_collaboration;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.GenericStrategyManagerImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.PlanSelector;

import java.util.List;

public class CollaborationStrategyMangerImpl<PL extends BasicPlan, AG extends HasPlansAndId<PL, AG>> implements CollaborationStrategyManager<PL, AG>{

    final GenericStrategyManager<PL, AG> delegate = new GenericStrategyManagerImpl<>();

    @Override
    public void addStrategy(GenericPlanStrategy strategy, String subpopulation, double weight) {
        delegate.addStrategy(strategy, subpopulation, weight);
    }

    @Override
    public void setMaxPlansPerAgent(int maxPlansPerAgent) {
        delegate.setMaxPlansPerAgent(maxPlansPerAgent);
    }

    @Override
    public void addChangeRequest(int iteration, GenericPlanStrategy strategy, String subpopulation, double newWeight) {
        delegate.addChangeRequest(iteration, strategy, subpopulation, newWeight);
    }

    @Override
    public void setPlanSelectorForRemoval(PlanSelector planSelector) {
        delegate.setPlanSelectorForRemoval(planSelector);
    }

    @Override
    public List<GenericPlanStrategy<PL, AG>> getStrategies(String subpopulation) {
        return List.of();
    }

    @Override
    public List<Double> getWeights(String subpopulation) {
        return List.of();
    }

    @Override
    public void run(Iterable persons, int iteration, ReplanningContext replanningContext) {
        delegate.run(persons, iteration, replanningContext);
    }
}
