package freight_emission;

import com.graphhopper.jsprit.analysis.toolbox.StopWatch;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListeners;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.speedy.SpeedyDijkstraFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controler.CarrierControlerUtils;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.vehicles.VehicleType;

import javax.management.InvalidAttributeValueException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static org.matsim.freight.carriers.CarriersUtils.*;

final class CarrierPlanGeneration {
    // Create a new Carrier
    private final Carrier carrier = CarriersUtils.createCarrier(Id.create("Carrier1", Carrier.class));
    private final Network network;
    private static final Logger log = LogManager.getLogger(CarrierPlanGeneration.class);

    public CarrierPlanGeneration(Network network) {
        this.network = network;
    }

    void setCarrier() {

        // Set Carrier Attributes
        CarriersUtils.setJspritIterations(carrier, 100);
        // Set Carrier Capabilities
        CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance()
                .setFleetSize(CarrierCapabilities.FleetSize.INFINITE)
                .build();
        carrier.setCarrierCapabilities(carrierCapabilities);

        // Generate random shipments
        RandomDemandGeneration randomDemandGeneration = new RandomDemandGeneration(network);
        Set<CarrierShipment> shipments = randomDemandGeneration.generateDemandWithoutTimeWindow(Set.of(TransportMode.bike, TransportMode.car));

        // Add the shipment to the carrier
        for (CarrierShipment shipment : shipments) {
            CarriersUtils.addShipment(carrier, shipment);
        }

        // Generate 2 vehicle types
        VehicleType van = CarrierVehicleType.Builder.newInstance(Id.create("van", VehicleType.class))
                .setCapacity(3000)
                .setFixCost(1168)
                .setCostPerDistanceUnit(4.22E-3)
                .setCostPerTimeUnit(0.089)
                .build();

        VehicleType cargoBike = CarrierVehicleType.Builder.newInstance(Id.create("cargoBike", VehicleType.class))
                .setCapacity(125)
                .setFixCost(300)
                .setCostPerDistanceUnit(3E-3)
                .setCostPerTimeUnit(0.058)
                .build();

        Set<Link> depotLinks = randomDemandGeneration.findDepotLinks(Set.of(TransportMode.bike, TransportMode.car));
        int vehicleIdx = 0;
        for (Link depotLink : depotLinks) {
            CarrierVehicle cv = CarrierVehicle.Builder.newInstance(Id.createVehicleId("van_"+vehicleIdx), depotLink.getId(), van).build();
            CarriersUtils.addCarrierVehicle(carrier, cv);
            vehicleIdx++;
        }

    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        // Read the network
        new MatsimNetworkReader(scenario.getNetwork()).readFile("../../data/clean/network/GemeenteLeuvenOptimized.xml.gz");
        Network network = scenario.getNetwork();
        CarrierPlanGeneration cpg = new CarrierPlanGeneration(network);
        cpg.setCarrier();
        Carriers carriers = new Carriers(Set.of(cpg.carrier));
        // Write the carrierPlan to a file
        CarrierPlanWriter carrierPlanWriter = new CarrierPlanWriter(carriers);
        carrierPlanWriter.write("../../data/intermediate/test/testCarrierPlanWithoutRoute4Van.xml");
        // Write vehicle types to a file
        CarrierVehicleTypes vehicleTypes = CarrierVehicleTypes.getVehicleTypes(carriers);
        CarrierVehicleTypeWriter carrierVehicleTypeWriter = new CarrierVehicleTypeWriter(vehicleTypes);
        carrierVehicleTypeWriter.write("../../data/intermediate/test/testVanTypes.xml");

        // Generate Carrier plan with routes
        FreightCarriersConfigGroup fccg = new FreightCarriersConfigGroup();
        fccg.setCarriersFile("../../data/intermediate/test/testCarrierPlanWithoutRoute4Van.xml");
        fccg.setCarriersVehicleTypesFile("../../data/intermediate/test/testVanTypes.xml");
        scenario.getConfig().addModule(fccg);
        CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
        CarriersUtils.runJsprit(scenario);
        Carriers CarriersWithRoutes = getCarriers(scenario);
        CarrierPlanWriter carrierPlanWriterWithRoutes = new CarrierPlanWriter(CarriersWithRoutes);
        carrierPlanWriterWithRoutes.write("../../data/intermediate/test/testCarrierPlanWithRoute4Van.xml");

    }

    @Deprecated(since = "we do not need to customize the @LeastCostPathCalculatorFactory currently", forRemoval = true)
    static void runJsprit(Scenario scenario) throws ExecutionException, InterruptedException {
        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule( scenario.getConfig(), FreightCarriersConfigGroup.class );

        final NetworkBasedTransportCosts netBasedCosts = NetworkBasedTransportCosts.Builder.newInstance(
                scenario.getNetwork(), getCarrierVehicleTypes(scenario).getVehicleTypes().values())
                .setThreadSafeLeastCostPathCalculatorFactory(new SpeedyDijkstraFactory())
                .build();

        Carriers carriers = getCarriers(scenario);

        HashMap<Id<Carrier>, Integer> carrierActivityCounterMap = new HashMap<>();

        // Fill carrierActivityCounterMap -> basis for sorting the carriers by number of activities before solving in parallel
        for (Carrier carrier : carriers.getCarriers().values()) {
            carrierActivityCounterMap.put(carrier.getId(), carrierActivityCounterMap.getOrDefault(carrier.getId(), 0) + carrier.getServices().size());
            carrierActivityCounterMap.put(carrier.getId(), carrierActivityCounterMap.getOrDefault(carrier.getId(), 0) + carrier.getShipments().size());
        }

        HashMap<Id<Carrier>, Integer> sortedMap = carrierActivityCounterMap.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        ArrayList<Id<Carrier>> tempList = new ArrayList<>(sortedMap.keySet());
        ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        forkJoinPool.submit(() -> tempList.parallelStream().forEach(carrierId -> {
            Carrier carrier = carriers.getCarriers().get(carrierId);

            double start = System.currentTimeMillis();
            int serviceCount = carrier.getServices().size();
            log.info("Start tour planning for " + carrier.getId() + " which has " + serviceCount + " services");

            VehicleRoutingProblem problem = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork()).setRoutingCost(netBasedCosts).build();
            VehicleRoutingAlgorithm algorithm = MatsimJspritFactory.loadOrCreateVehicleRoutingAlgorithm(scenario, freightCarriersConfigGroup, netBasedCosts, problem);

            algorithm.getAlgorithmListeners().addListener(new StopWatch(), VehicleRoutingAlgorithmListeners.Priority.HIGH);
            int jspritIterations = getJspritIterations(carrier);
            try {
                if (jspritIterations > 0) {
                    algorithm.setMaxIterations(jspritIterations);
                } else {
                    throw new InvalidAttributeValueException(
                            "Carrier has invalid number of jsprit iterations. They must be positive! Carrier id: "
                                    + carrier.getId().toString());}
            } catch (Exception e) {
                throw new RuntimeException(e);
//				e.printStackTrace();
            }

            VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());

            log.info("tour planning for carrier " + carrier.getId() + " took " + (System.currentTimeMillis() - start) / 1000 + " seconds.");

            CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution);
            // yy In principle, the carrier should know the vehicle types that it can deploy.

            log.info("routing plan for carrier " + carrier.getId());
            NetworkRouter.routePlan(newPlan, netBasedCosts);
            log.info("routing for carrier " + carrier.getId() + " finished. Tour planning plus routing took " + (System.currentTimeMillis() - start) / 1000 + " seconds.");

            carrier.setSelectedPlan(newPlan);
        })).get();
    }

}
