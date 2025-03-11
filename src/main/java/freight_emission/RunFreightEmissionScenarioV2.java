package freight_emission;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.network.io.MatsimNetworkReader;
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
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.util.*;


public class RunFreightEmissionScenarioV2 {
    private final static Logger logger = LogManager.getLogger(RunFreightEmissionScenarioV2.class);

    private static final int NUM_ITERATIONS = 200;
    private static final int NUM_CARRIERS = 1;
    private static final int NUM_JSPRIT_ITERATIONS = 100;
    private static final String inputNetworkPath = "../../data/intermediate/test/freightEmissions/GemeenteLeuvenWithHbefaType.xml.gz";

    public static void main(String[] args) {

        logger.info("Starting the generation of the freight emission scenario");
        // Read the network
        logger.info("Reading the network");
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(inputNetworkPath);
        Network network = scenario.getNetwork();

        for (int i = 100; i < NUM_ITERATIONS; i++) {
            logger.info("Generating the carrier plan for iteration {}", i);
            Map<Integer, Set<CarrierShipment>> carrierShipments = new HashMap<>();
            Map<Integer, Set<Id<Link>>> depotLinks = new HashMap<>();

            for (int j = 0; j < NUM_CARRIERS; j++) {
                Set<Id<Link>> randomDepotLinks = generateRandomDepotLinks();
                // Add depot links
                depotLinks.put(j, randomDepotLinks);
                // Generate random demand for each carrier
                RandomDemandGeneration randomDemandGeneration = new RandomDemandGeneration(network);
                randomDemandGeneration.updateDepotLinksId(depotLinks.get(j));
                Set<CarrierShipment> shipments = randomDemandGeneration.generateDemandWithTimeWindow(Set.of(TransportMode.car, TransportMode.bike));
                carrierShipments.put(j, shipments);
            }

            // Run the scenario for basic-VAN
            runFreightEmissionScenario(ScenarioType.BASIC, i, network, carrierShipments, depotLinks);
            // Run the scenario for van-circulation
            Network carNetwork = RunAddHbefaRoadType2Network.deriveSubNetwork(network, TransportMode.car);
            runFreightEmissionScenario(ScenarioType.VAN, i, carNetwork, carrierShipments, depotLinks);
            // Run the scenario for CARGO_BIKE-circulation
            Network bikeNetwork = RunAddHbefaRoadType2Network.deriveSubNetwork(network, TransportMode.bike);
            runFreightEmissionScenario(ScenarioType.CARGO_BIKE, i, bikeNetwork, carrierShipments, depotLinks);

        }
    }

    static void runFreightEmissionScenario(ScenarioType scenarioType, int iterIdx, Network network,
                                           Map<Integer, Set<CarrierShipment>> carrierShipments,
                                           Map<Integer, Set<Id<Link>>> depotLinks) {
        assert scenarioType == ScenarioType.VAN || scenarioType == ScenarioType.CARGO_BIKE || scenarioType == ScenarioType.BASIC;

        String outputScenarioDir = null;
        String hbefaKeyWord = null;
        String configInputNetworkPath = null;
        VehicleType vehicleType = null;
        Map<Integer, Set<VehicleType>> vehicleTypes = new HashMap<>();

        switch (scenarioType) {
            case BASIC:
                outputScenarioDir = "../../data/intermediate/test/freightEmissions/scenarioBasic/";
                vehicleType = FreightVehicleTypeFactory.createDefaultVan("Van");
                hbefaKeyWord = "LCV";
                configInputNetworkPath = "../../diffusedGemeenteLeuvenWithHbefaType.xml.gz";
                Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
                new MatsimNetworkReader(scenario.getNetwork()).readFile("../../data/intermediate/test/freightEmissions/diffusedGemeenteLeuvenWithHbefaType.xml.gz");
                network = scenario.getNetwork();
                break;
            case VAN:
                outputScenarioDir = "../../data/intermediate/test/freightEmissions/scenarioVan/";
                vehicleType = FreightVehicleTypeFactory.createDefaultVan("Van");
                hbefaKeyWord = "LCV";
                configInputNetworkPath = "../../carGemeenteLeuvenWithHbefaType.xml.gz";
                break;
            case CARGO_BIKE:
                outputScenarioDir = "../../data/intermediate/test/freightEmissions/scenarioCB/";
                vehicleType = FreightVehicleTypeFactory.createDefaultCargoBike("CB");
                hbefaKeyWord = "MC";
                configInputNetworkPath = "../../bikeGemeenteLeuvenWithHbefaType.xml.gz";
                break;
        }

        // Check if the dir exists in the disk, if not, create it
        boolean isPathExists = DirUtils.ensureDirectoryExists(outputScenarioDir + "iter" + iterIdx);
        assert isPathExists;

        for (int i = 0; i < NUM_CARRIERS; i++) {
            vehicleTypes.put(i, Set.of(vehicleType));
        }
        logger.info("Setting the carriers");
        CarrierPlanGeneration carrierPlanGeneration;
        if (scenarioType == ScenarioType.CARGO_BIKE) {
            carrierPlanGeneration = new CarrierPlanGeneration(network, NUM_CARRIERS, 1000, vehicleTypes, carrierShipments, depotLinks);
        }else {
            carrierPlanGeneration = new CarrierPlanGeneration(network, NUM_CARRIERS, NUM_JSPRIT_ITERATIONS, vehicleTypes, carrierShipments, depotLinks);
        }

        carrierPlanGeneration.setCarriers();
        // Write the carrierPlan without routes to a file
        logger.info("Writing the carrier plan without routes");
        carrierPlanGeneration.writeCarrierPlanWithoutRoutes(outputScenarioDir + "iter" + iterIdx + "/carrierPlanWithoutRoute.xml");
        // Write vehicle types to a file
        logger.info("Writing the vehicle types");
        carrierPlanGeneration.writeVehicleTypes(outputScenarioDir + "iter" + iterIdx + "/VehicleTypes.xml");
        // Generate Carrier plan with routes
        carrierPlanGeneration.generateAndWriteCarrierPlanWithRoutes(outputScenarioDir + "iter" + iterIdx + "/carrierPlanWithoutRoute.xml",
                outputScenarioDir + "iter" + iterIdx + "/VehicleTypes.xml");

        // create config file
        logger.info("Creating the config file");
        ConfigGeneration configGeneration = new ConfigGeneration.Builder()
                .setConfigOutputPath(outputScenarioDir + "iter" + iterIdx + "/config.xml")
                .setScenarioOutputPath(outputScenarioDir + "iter" + iterIdx + "/outputs/")
                .setCarrierPlanFile("carrierPlanWithRoutes.xml")
                .setVehicleFilePath("VehicleTypes.xml")
                .setInputNetworkPath(configInputNetworkPath)
                .setAverageWarmEmissionFactorsFilePath("../../EFA_HOT_Vehcat_2025average.csv.gz")
                .setAverageColdEmissionFactorsFilePath("../../EFA_ColdStart_Vehcat_2025average.csv.gz")
                .setDetailedWarmEmissionFactorsFilePath("../../EFA_HOT_SubSegm_"+hbefaKeyWord+"2025detailed.csv.gz")
                .setDetailedColdEmissionFactorsFilePath("../../EFA_ColdStart_SubSegm_"+hbefaKeyWord+"2025detailed.csv.gz")
                .build();
        configGeneration.writeConfig();

        // Run the scenario
        logger.info("Running the scenario{} for iteration {}", scenarioType, iterIdx);
        runScenario(outputScenarioDir + "iter" + iterIdx + "/config.xml", scenarioType);
    }

    static void runScenario(String configPath, ScenarioType scenarioType) {
        Config config = ConfigUtils.loadConfig(configPath);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        // Load carriers according to freight config
        CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

        // Get carriers and carrier vehicle types
        Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
        CarrierVehicleTypes types = CarriersUtils.getCarrierVehicleTypes(scenario);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new CarrierModule());
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(EmissionModule.class).asEagerSingleton();
//                if (scenarioType == ScenarioType.CARGO_BIKE) {
//                    bind(CarrierStrategyManager.class).toProvider(new RunFreightEmissionScenarioV2.MyBikeCarrierPlanStrategyManagerProvider(types));
//                } else {
//                    bind(CarrierStrategyManager.class).toProvider(new RunFreightEmissionScenarioV2.MyCarrierPlanStrategyManagerProvider(types));
//                }
                bind(CarrierStrategyManager.class).toProvider(new RunFreightEmissionScenarioV2.MyCarrierPlanStrategyManagerProvider(types));
                bind(CarrierScoringFunctionFactory.class).toInstance(new RunFreightEmissionScenarioV2.MyCarrierScoringFunctionFactory());

                final LegHistogram freightOnly = new LegHistogram(900).setInclPop(false);
                addEventHandlerBinding().toInstance(freightOnly);

                final LegHistogram withoutFreight = new LegHistogram(900);
                addEventHandlerBinding().toInstance(withoutFreight);

                addControlerListenerBinding().toInstance(new CarrierScoreStats(carriers, config.controller().getOutputDirectory() + "/carrier_scores", true));
                addControlerListenerBinding().toInstance(new IterationEndsListener() {

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

    enum ScenarioType {
        BASIC,  // Basic scenario-van
        VAN,
        CARGO_BIKE
    }

    private static class MyCarrierScoringFunctionFactory implements CarrierScoringFunctionFactory {
        @Inject
        private Network network;

        @Override
        public ScoringFunction createScoringFunction(Carrier carrier) {
            SumScoringFunction sf = new SumScoringFunction();
            sf.addScoringFunction(new CarrierScoringFunctionFactoryImpl.SimpleDriversLegScoring(carrier, network));
            sf.addScoringFunction(new CarrierScoringFunctionFactoryImpl.SimpleVehicleEmploymentScoring(carrier));
            sf.addScoringFunction(new CarrierScoringFunctionFactoryImpl.SimpleDriversActivityScoring());
            return sf;
        }
    }

    private static class MyCarrierPlanStrategyManagerProvider implements Provider<CarrierStrategyManager> {
        private final CarrierVehicleTypes types;
        @Inject
        private Network network;
        @Inject
        private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
        @Inject
        private Map<String, TravelTime> modeTravelTimes;

        MyCarrierPlanStrategyManagerProvider(CarrierVehicleTypes types) {
            this.types = types;
        }

        @Override
        public CarrierStrategyManager get() {
            final CarrierStrategyManager strategyManager = CarrierControlerUtils.createDefaultCarrierStrategyManager();
            strategyManager.setMaxPlansPerAgent(5);
            {
                GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanChanger.Factory<CarrierPlan, Carrier>().build());
                strategyManager.addStrategy(strategy, null, 1.0);
            }
            {
                final TravelDisutility travelDisutility = CarrierTravelDisutilities.createBaseDisutility(types, modeTravelTimes.get(TransportMode.car));
                final LeastCostPathCalculator router = leastCostPathCalculatorFactory.createPathCalculator(network, travelDisutility, modeTravelTimes.get(TransportMode.car));

                GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<>());
                strategy.addStrategyModule(new CarrierTimeAllocationMutator.Factory().build());
                strategy.addStrategyModule(new CarrierReRouteVehicles.Factory(router, network, modeTravelTimes.get(TransportMode.car)).build());
                strategyManager.addStrategy(strategy, null, 0.5);
            }
            return strategyManager;
        }
    }

    private static class MyBikeCarrierPlanStrategyManagerProvider implements Provider<CarrierStrategyManager> {
        private final CarrierVehicleTypes types;
        @Inject
        private Network network;
        @Inject
        private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
        @Inject
        private Map<String, TravelTime> modeTravelTimes;

        MyBikeCarrierPlanStrategyManagerProvider(CarrierVehicleTypes types) {
            this.types = types;
        }

        @Override
        public CarrierStrategyManager get() {
            final CarrierStrategyManager strategyManager = CarrierControlerUtils.createDefaultCarrierStrategyManager();
            strategyManager.setMaxPlansPerAgent(5);
            {
                GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanChanger.Factory<CarrierPlan, Carrier>().build());
                strategyManager.addStrategy(strategy, null, 1.0);
            }
            {
                final TravelDisutility travelDisutility = CarrierTravelDisutilities.createBaseDisutility(types, modeTravelTimes.get(TransportMode.bike));
                final LeastCostPathCalculator router = leastCostPathCalculatorFactory.createPathCalculator(network, travelDisutility, modeTravelTimes.get(TransportMode.bike));

                GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<>());
                strategy.addStrategyModule(new CarrierTimeAllocationMutator.Factory().build());
                strategy.addStrategyModule(new CarrierReRouteVehicles.Factory(router, network, modeTravelTimes.get(TransportMode.car)).build());
                strategyManager.addStrategy(strategy, null, 0.5);
            }
            return strategyManager;
        }
    }


    static class DirUtils {

        public static boolean ensureDirectoryExists(String directoryPath) {
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                return directory.mkdirs();
            }
            return true;
        }
    }

    private static Set<Id<Link>>generateRandomDepotLinks(){
        Set<Id<Link>> candidateDepotLinks = Set.of(
                // out of the ring
                Id.createLinkId("333784188_r_3"),
                Id.createLinkId("27566523_11"),
                Id.createLinkId("25806807_r_1"),
                Id.createLinkId("131757263_1-131757263_2"),
                // in the ring
                Id.createLinkId("3390195_0"),
                Id.createLinkId("893781380_r_0-48247088_0"),
                Id.createLinkId("10149534_2"),
                Id.createLinkId("150056709_r_10")
        );

        // Randomly select 2 depot links
        // Convert the set to a list for random selection
        List<Id<Link>> depotLinksList = new ArrayList<>(candidateDepotLinks);
        // Shuffle the list to randomize the order
        Collections.shuffle(depotLinksList);
        // Select the first 2 elements from the shuffled list
        return new HashSet<>(depotLinksList.subList(0, 2));
    }

}
