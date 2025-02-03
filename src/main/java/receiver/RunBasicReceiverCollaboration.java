package receiver;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.contrib.freightreceiver.*;
import org.matsim.contrib.freightreceiver.collaboration.CollaborationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.usecases.analysis.CarrierScoreStats;

public class RunBasicReceiverCollaboration {

    public static void main(String[] args) {
        // Get Config
        Config config = MyUtils.createExampleConfigWithDefaultNetwork();
        // Create scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);
        // Generate carriers
        CarrierGeneration carrierGeneration = new CarrierGeneration();
        carrierGeneration.generateCarriers();
        Carriers carriers = carrierGeneration.getCarriers();
        // Add carriers into scenario
        CarriersUtils.addOrGetCarriers(scenario);

        // Add receiver module (config group)
        ReceiverConfigGroup receiverConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), ReceiverConfigGroup.class);
        receiverConfigGroup.setReplanningType(ReceiverReplanningType.timeWindow);
        // Generate receivers
        ReceiverGeneration receiverGeneration = new ReceiverGeneration();
        receiverGeneration.generateReceivers();
        Receivers receivers = receiverGeneration.getReceivers();
        // Add receivers into scenario
        ReceiverUtils.setReceivers(receivers, scenario);

        // Generate receiver orders and plans and carrier shipments
        ReceiverOrderGeneration receiverOrderGeneration = new ReceiverOrderGeneration(receivers, carriers);
        receiverOrderGeneration.generateAllReceiverOrders();

        // Ensure that the receivers are linked to the carriers
        CollaborationUtils.linkReceiverOrdersToCarriers(ReceiverUtils.getReceivers(scenario), CarriersUtils.getCarriers(scenario));

        // Create coalition and add carriers and receivers into the coalition
        CollaborationUtils.createCoalitionWithCarriersAndAddCollaboratingReceivers(scenario);

        // Run the scenario
        Controler controler = new Controler(scenario);
        ReceiverModule receiverModule = new ReceiverModule(ReceiverUtils.createFixedReceiverCostAllocation(100.0));
        receiverModule.setReplanningType(ReceiverReplanningType.timeWindow);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(ReceiverModule.class).toInstance(receiverModule);
            }
        });
        CarrierScoreStats scoreStats = new CarrierScoreStats(CarriersUtils.getCarriers(controler.getScenario()), controler.getScenario().getConfig().controller().getOutputDirectory() + "/carrier_scores", true);
        controler.addControlerListener(scoreStats);
        controler.run();

    }
}
