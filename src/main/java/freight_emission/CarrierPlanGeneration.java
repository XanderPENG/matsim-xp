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
import org.matsim.vehicles.*;

import javax.management.InvalidAttributeValueException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static org.matsim.freight.carriers.CarriersUtils.*;

final class CarrierPlanGeneration {
    private static final Logger log = LogManager.getLogger(CarrierPlanGeneration.class);

    private final Network network;
    private final int numCarrier;
    private final int numJspritIterations;
    private final Map<Integer, Set<VehicleType>> carrierVehicleTypes;
    private final Map<Integer, Set<CarrierShipment>> carrierShipments;
    private final Map<Integer,Set<Id<Link>>> depotLinks;

    private final Carriers carriers = new Carriers();

    public CarrierPlanGeneration(Network network, int numCarrier, int numJspritIterations,
                                 Map<Integer, Set<VehicleType>> carrierVehicleTypes,
                                 Map<Integer, Set<CarrierShipment>> carrierShipments,
                                 Map<Integer, Set<Id<Link>>> depotLinks) {
        this.network = network;
        this.numCarrier = numCarrier;
        this.numJspritIterations = numJspritIterations;
        this.carrierVehicleTypes = carrierVehicleTypes;
        this.carrierShipments = carrierShipments;
        this.depotLinks = depotLinks;
    }

    void setCarriers() {
        for (int i = 0; i < this.numCarrier; i++){
            Carrier carrier = CarriersUtils.createCarrier(Id.create("carrier_"+i, Carrier.class));
            // Set Carrier Attributes
            CarriersUtils.setJspritIterations(carrier, this.numJspritIterations);
            // Set Carrier Capabilities
            CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance()
                    .setFleetSize(CarrierCapabilities.FleetSize.INFINITE)
                    .build();
            carrier.setCarrierCapabilities(carrierCapabilities);
            // add shipments
            Set<CarrierShipment> shipments = this.carrierShipments.get(i);
            for (CarrierShipment shipment : shipments) {
                CarriersUtils.addShipment(carrier, shipment);
            }
            // add carrier vehicles
            Set<VehicleType> vehicleTypes = this.carrierVehicleTypes.get(i);

            int vehicleIdx = 0;
            for (VehicleType vehicleType : vehicleTypes) {
                for (Id<Link> depotLink : this.depotLinks.get(i)) {
                    CarrierVehicle cv = CarrierVehicle.Builder.newInstance(Id.createVehicleId("carrier"+i+"_"+vehicleType.getId()+"_"+vehicleIdx),
                            depotLink, vehicleType).build();
                    CarriersUtils.addCarrierVehicle(carrier, cv);
                    vehicleIdx++;
                }
            }
            this.carriers.addCarrier(carrier);
        }
    }

    public void writeCarrierPlanWithoutRoutes(String path) {
        CarrierPlanWriter carrierPlanWriter = new CarrierPlanWriter(this.carriers);
        carrierPlanWriter.write(path);
    }

    public void writeVehicleTypes(String path) {
        CarrierVehicleTypes carrierVehicleTypes = CarrierVehicleTypes.getVehicleTypes(this.carriers);
        Map<Id<VehicleType>, VehicleType> vehicleTypes = carrierVehicleTypes.getVehicleTypes();

        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        // add vehicle types into vehicles
        vehicleTypes.forEach((vehicleTypeId, vehicleType) -> {
            vehicles.addVehicleType(vehicleType);
        });
        // add carrier vehicles into vehicles
        this.carriers.getCarriers().forEach((carrierId, carrier) -> {
            Map<Id<Vehicle>, CarrierVehicle> carrierVehicles = carrier.getCarrierCapabilities().getCarrierVehicles();
            carrierVehicles.forEach((vehicleId, carrierVehicle) -> {
//                VehicleType vehicleType = vehicleTypes.get(carrierVehicle.getVehicleTypeId());
//                Vehicle vehicle = VehicleUtils.createVehicle(vehicleId, vehicleType);
                vehicles.addVehicle(carrierVehicle);
            });
        });
        // Write the vehicle types to a file
        MatsimVehicleWriter writer = new MatsimVehicleWriter(vehicles);
        writer.writeFile(path);
//        CarrierVehicleTypeWriter vehicleTypesWriter = new CarrierVehicleTypeWriter(vehicleTypes);
//        vehicleTypesWriter.write(path);
    }

    public void generateAndWriteCarrierPlanWithRoutes(String path4PlanWithoutRoutes, String path4VehicleTypes) {
        // Create scenario and add the network
        Scenario scenario = new ScenarioUtils.ScenarioBuilder(ConfigUtils.createConfig())
                .setNetwork(this.network).build();
        // Generate Carrier plan with routes
        FreightCarriersConfigGroup fccg = new FreightCarriersConfigGroup();
        fccg.setCarriersFile(path4PlanWithoutRoutes);
        fccg.setCarriersVehicleTypesFile(path4VehicleTypes);
        //// Add the module to the scenario
        scenario.getConfig().addModule(fccg);
        //// load carriers and run jsprit to generate routes
        CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
        try {
            CarriersUtils.runJsprit(scenario);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        //// write the carrier plan with routes
        Carriers CarriersWithRoutes = getCarriers(scenario);
        CarrierPlanWriter carrierPlanWriterWithRoutes = new CarrierPlanWriter(CarriersWithRoutes);
        carrierPlanWriterWithRoutes.write(path4PlanWithoutRoutes.replace("WithoutRoute", "WithRoutes"));
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
