package freight_emission;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controler.CarrierControlerUtils;
import org.matsim.vehicles.VehicleType;

import java.util.Set;
import java.util.concurrent.ExecutionException;

final class CarrierPlanGeneration {
    // Create a new Carrier
    private final Carrier carrier = CarriersUtils.createCarrier(Id.create("Carrier1", Carrier.class));
    private final Network network;

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
        carrierPlanWriter.write("../../data/intermediate/test/testCarrierPlanWithoutRoute.xml");
        // Write vehicle types to a file
        CarrierVehicleTypes vehicleTypes = CarrierVehicleTypes.getVehicleTypes(carriers);
        CarrierVehicleTypeWriter carrierVehicleTypeWriter = new CarrierVehicleTypeWriter(vehicleTypes);
        carrierVehicleTypeWriter.write("../../data/intermediate/test/testCarrierVehicleTypes.xml");

        // Generate Carrier plan with routes
        FreightCarriersConfigGroup fccg = new FreightCarriersConfigGroup();
        fccg.setCarriersFile("../../data/intermediate/test/testCarrierPlanWithoutRoute.xml");
        fccg.setCarriersVehicleTypesFile("../../data/intermediate/test/testCarrierVehicleTypes.xml");
        scenario.getConfig().addModule(fccg);
        CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
        CarriersUtils.runJsprit(scenario);
        Carriers CarriersWithRoutes = CarriersUtils.getCarriers(scenario);
        CarrierPlanWriter carrierPlanWriterWithRoutes = new CarrierPlanWriter(CarriersWithRoutes);
        carrierPlanWriterWithRoutes.write("../../data/intermediate/test/testCarrierPlanWithRoute.xml");

    }

}
