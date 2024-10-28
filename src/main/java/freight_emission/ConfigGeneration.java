package freight_emission;

import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;

public class ConfigGeneration {

    private final static String outputDir = "../../data/intermediate/test/freight_emission/freightEmissionConfig.xml";

    public static void main(String[] args) {
        // Create and output a default emissions config
        Config config = ConfigUtils.createConfig();

        // Set the output directory and the first and last iteration
        config.controller().setOutputDirectory("./");
        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(5);

        // Set the plan calc score config group
        ScoringConfigGroup.ActivityParams homeP = new ScoringConfigGroup.ActivityParams("home");
        homeP.setTypicalDuration(12 * 3600);
        config.scoring().addActivityParams(homeP);
        ScoringConfigGroup.ActivityParams workP = new ScoringConfigGroup.ActivityParams("work");
        workP.setTypicalDuration(8 * 3600);
        config.scoring().addActivityParams(workP);

        // Strategy
        ReplanningConfigGroup scg = config.replanning();
        ReplanningConfigGroup.StrategySettings strategySettings = new ReplanningConfigGroup.StrategySettings();
        strategySettings.setStrategyName("ChangeExpBeta");
        strategySettings.setWeight(1.0);
        scg.addStrategySettings(strategySettings);

        // Network
        config.network().setInputFile("GemeenteLeuvenWithHbefaType.xml.gz");

        // plans
//        config.plans().setInputFile("../../raw/test/emissions/sample_population.xml");

        // Emissions Config Group
        EmissionsConfigGroup ecg = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);

        config.vehicles().setVehiclesFile("emissionVanTest.xml");

        ecg.setHbefaVehicleDescriptionSource(EmissionsConfigGroup.HbefaVehicleDescriptionSource.asEngineInformationAttributes);
        ecg.setAverageWarmEmissionFactorsFile("EFA_HOT_vehcat_2025average.csv");
        ecg.setAverageColdEmissionFactorsFile("EFA_ColdStart_vehcat_2025average.csv");
        ecg.setDetailedWarmEmissionFactorsFile("EFA_HOT_SubSegm_2025detailed.csv");
        ecg.setDetailedColdEmissionFactorsFile("EFA_ColdStart_SubSegm_2025detailed.csv");
        ecg.setHandlesHighAverageSpeeds(true);

        // Freight Config Group
        FreightCarriersConfigGroup fccg = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
        fccg.setCarriersFile("testCarrierPlanWithRoute.xml");
        fccg.setCarriersVehicleTypesFile("testCarrierVehicleTypes.xml");

        // Output the config to the directory
        ConfigUtils.writeConfig(config, outputDir);

    }
}
