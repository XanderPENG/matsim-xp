package freight_emission;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controler.*;
import org.matsim.freight.carriers.usecases.analysis.CarrierScoreStats;
import org.matsim.freight.carriers.usecases.analysis.LegHistogram;
import org.matsim.freight.carriers.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.freight.carriers.usecases.chessboard.CarrierTravelDisutilities;

import java.util.Map;

public class RunFreightEmissionScenario {
    private static final String rootPath = "../../data/intermediate/test/freight_emission/";
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig(rootPath+"freightEmissionConfig4CargoBike.xml");
        FreightCarriersConfigGroup fccg= ConfigUtils.addOrGetModule( config, FreightCarriersConfigGroup.class );
        System.out.println(fccg.getCarriersFile());
        Scenario scenario = ScenarioUtils.loadScenario( config );

        // Load carriers according to freight config
        CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );
        // Get carriers and carrier vehicle types
        Carriers carriers = CarriersUtils.addOrGetCarriers( scenario );
        CarrierVehicleTypes types = CarriersUtils.getCarrierVehicleTypes( scenario );

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new CarrierModule() );
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(EmissionModule.class).asEagerSingleton();
                bind( CarrierStrategyManager.class ).toProvider( new RunFreightEmissionScenario.MyCarrierPlanStrategyManagerProvider( types ) );
                bind( CarrierScoringFunctionFactory.class).toInstance( new RunFreightEmissionScenario.MyCarrierScoringFunctionFactory() );

                final LegHistogram freightOnly = new LegHistogram(900).setInclPop( false );
                addEventHandlerBinding().toInstance(freightOnly);

                final LegHistogram withoutFreight = new LegHistogram(900);
                addEventHandlerBinding().toInstance(withoutFreight);

                addControlerListenerBinding().toInstance( new CarrierScoreStats(carriers, config.controller().getOutputDirectory() +"/carrier_scores", true) );
                addControlerListenerBinding().toInstance( new IterationEndsListener() {

                    @Inject
                    private OutputDirectoryHierarchy controlerIO;

                    @Override
                    public void notifyIterationEnds(IterationEndsEvent event) {
                        String dir = controlerIO.getIterationPath(event.getIteration());
                        System.out.println("Writing carrier output to: " + dir);

                        //write plans
                        new CarrierPlanWriter(carriers).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");

                        //write stats
                        freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
                        freightOnly.reset(event.getIteration());

                        withoutFreight.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_withoutFreight.png");
                        withoutFreight.reset(event.getIteration());
                    }
                });
            }
        });

        controler.run();
    }

    private static class MyCarrierScoringFunctionFactory implements CarrierScoringFunctionFactory {
        @Inject private Network network;
        @Override public ScoringFunction createScoringFunction(Carrier carrier) {
            SumScoringFunction sf = new SumScoringFunction();
            sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleDriversLegScoring(carrier, network) );
            sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleVehicleEmploymentScoring(carrier) );
            sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleDriversActivityScoring() );
            return sf;
        }
    }

    private static class MyCarrierPlanStrategyManagerProvider implements Provider<CarrierStrategyManager> {
        @Inject private Network network;
        @Inject private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
        @Inject private Map<String, TravelTime> modeTravelTimes;
        private final CarrierVehicleTypes types;
        MyCarrierPlanStrategyManagerProvider( CarrierVehicleTypes types ) {
            this.types = types;
        }

        @Override
        public CarrierStrategyManager get() {
            final CarrierStrategyManager strategyManager = CarrierControlerUtils.createDefaultCarrierStrategyManager();
            strategyManager.setMaxPlansPerAgent(5);
            {
                GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger.Factory<CarrierPlan,Carrier>().build() );
                strategyManager.addStrategy(strategy, null, 1.0);
            }
            {
                final TravelDisutility travelDisutility = CarrierTravelDisutilities.createBaseDisutility( types, modeTravelTimes.get( TransportMode.car ) );
                final LeastCostPathCalculator router = leastCostPathCalculatorFactory.createPathCalculator(network, travelDisutility, modeTravelTimes.get(TransportMode.car ) );

                GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>( new KeepSelected<>());
                strategy.addStrategyModule(new CarrierTimeAllocationMutator.Factory().build() );
                strategy.addStrategyModule(new CarrierReRouteVehicles.Factory(router, network, modeTravelTimes.get(TransportMode.car ) ).build() );
                strategyManager.addStrategy(strategy, null, 0.5);
            }
            return strategyManager;
        }
    }
}
