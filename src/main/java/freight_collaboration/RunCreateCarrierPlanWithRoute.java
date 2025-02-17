package freight_collaboration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.CarrierPlanWriter;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;

import java.util.concurrent.ExecutionException;

import static org.matsim.freight.carriers.CarriersUtils.getCarriers;

class RunCreateCarrierPlanWithRoute {

    Network network;

    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("../../data/raw/test/freightCollaboration/grid9x9.xml");
        RunCreateCarrierPlanWithRoute runCreateCarrierPlanWithRoute = new RunCreateCarrierPlanWithRoute();
        runCreateCarrierPlanWithRoute.network = scenario.getNetwork();
        runCreateCarrierPlanWithRoute.generateAndWriteCarrierPlanWithRoutes(
                "../../data/raw/test/freightCollaboration/carrierPlanWithoutRoute.xml",
                "../../data/raw/test/freightCollaboration/vehicleTypes.xml");
    }

    void generateAndWriteCarrierPlanWithRoutes(String path4PlanWithoutRoutes, String path4VehicleTypes) {
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
}

