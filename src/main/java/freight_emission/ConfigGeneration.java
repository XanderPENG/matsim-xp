package freight_emission;

import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;

public class ConfigGeneration {

    private final String configOutputPath;  // the root path is the script directory
    private final int lastIteration;
    private final String inputNetworkPath;
    private final String scenarioOutputPath;
    private final String vehicleFilePath;
    private final EmissionsConfigGroup.HbefaVehicleDescriptionSource hbefaVehicleDescriptionSource;
    private final String averageWarmEmissionFactorsFilePath;
    private final String averageColdEmissionFactorsFilePath;
    private final String detailedWarmEmissionFactorsFilePath;
    private final String detailedColdEmissionFactorsFilePath;
    private final EmissionsConfigGroup.DetailedVsAverageLookupBehavior detailedVsAverageLookupBehavior;
    private final EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel hbefaTableConsistencyCheckingLevel;
    private final boolean handlesHighAverageSpeeds;
    private final boolean writingEmissionsEvents;
    private final String carrierPlanFile;

    ConfigGeneration(String configOutputPath, int lastIteration,
                     String inputNetworkPath, String scenarioOutputPath, String vehicleFilePath,
                     EmissionsConfigGroup.HbefaVehicleDescriptionSource hbefaVehicleDescriptionSource,
                     String averageWarmEmissionFactorsFilePath, String averageColdEmissionFactorsFilePath,
                     String detailedWarmEmissionFactorsFilePath, String detailedColdEmissionFactorsFilePath,
                     EmissionsConfigGroup.DetailedVsAverageLookupBehavior detailedVsAverageLookupBehavior,
                     EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel hbefaTableConsistencyCheckingLevel,
                     boolean handlesHighAverageSpeeds, boolean writingEmissionsEvents, String carrierPlanFile) {
        this.configOutputPath = configOutputPath;
        this.lastIteration = lastIteration;
        this.inputNetworkPath = inputNetworkPath;
        this.scenarioOutputPath = scenarioOutputPath;
        this.vehicleFilePath = vehicleFilePath;
        this.hbefaVehicleDescriptionSource = hbefaVehicleDescriptionSource;
        this.averageWarmEmissionFactorsFilePath = averageWarmEmissionFactorsFilePath;
        this.averageColdEmissionFactorsFilePath = averageColdEmissionFactorsFilePath;
        this.detailedWarmEmissionFactorsFilePath = detailedWarmEmissionFactorsFilePath;
        this.detailedColdEmissionFactorsFilePath = detailedColdEmissionFactorsFilePath;
        this.detailedVsAverageLookupBehavior = detailedVsAverageLookupBehavior;
        this.hbefaTableConsistencyCheckingLevel = hbefaTableConsistencyCheckingLevel;
        this.handlesHighAverageSpeeds = handlesHighAverageSpeeds;
        this.writingEmissionsEvents = writingEmissionsEvents;
        this.carrierPlanFile = carrierPlanFile;
    }

    public void writeConfig(){
        Config config = ConfigUtils.createConfig();
        config.controller().setOutputDirectory(scenarioOutputPath);
        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(lastIteration);
        config.network().setInputFile(inputNetworkPath);
        config.vehicles().setVehiclesFile(vehicleFilePath);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        // add emissions config group
        EmissionsConfigGroup ecg = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
        ecg.setHbefaVehicleDescriptionSource(hbefaVehicleDescriptionSource);
        ecg.setAverageWarmEmissionFactorsFile(averageWarmEmissionFactorsFilePath);
        ecg.setAverageColdEmissionFactorsFile(averageColdEmissionFactorsFilePath);
        ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFilePath);
        ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFilePath);
        ecg.setDetailedVsAverageLookupBehavior(detailedVsAverageLookupBehavior);
        ecg.setHbefaTableConsistencyCheckingLevel(hbefaTableConsistencyCheckingLevel);
        ecg.setHandlesHighAverageSpeeds(handlesHighAverageSpeeds);
        ecg.setWritingEmissionsEvents(writingEmissionsEvents);
        // add freight carriers config group
        FreightCarriersConfigGroup fccg = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
        fccg.setCarriersFile(carrierPlanFile);
        fccg.setCarriersVehicleTypesFile(vehicleFilePath);
        // write the config
        ConfigUtils.writeConfig(config, configOutputPath);
    }

    public static class Builder {
        private String configOutputPath;
        private int lastIteration = 5;
        private String inputNetworkPath;
        private String scenarioOutputPath;
        private String vehicleFilePath;
        private EmissionsConfigGroup.HbefaVehicleDescriptionSource hbefaVehicleDescriptionSource = EmissionsConfigGroup.HbefaVehicleDescriptionSource.asEngineInformationAttributes;
        private String averageWarmEmissionFactorsFilePath;
        private String averageColdEmissionFactorsFilePath;
        private String detailedWarmEmissionFactorsFilePath;
        private String detailedColdEmissionFactorsFilePath;
        private EmissionsConfigGroup.DetailedVsAverageLookupBehavior detailedVsAverageLookupBehavior = EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort;
        private EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel hbefaTableConsistencyCheckingLevel = EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.none;
        private boolean handlesHighAverageSpeeds = true;
        private boolean writingEmissionsEvents = true;
        private String carrierPlanFile;

        public Builder setConfigOutputPath(String configOutputPath) {
            this.configOutputPath = configOutputPath;
            return this;
        }

        public Builder setLastIteration(int lastIteration) {
            this.lastIteration = lastIteration;
            return this;
        }

        public Builder setInputNetworkPath(String inputNetworkPath) {
            this.inputNetworkPath = inputNetworkPath;
            return this;
        }

        public Builder setScenarioOutputPath(String scenarioOutputPath) {
            this.scenarioOutputPath = scenarioOutputPath;
            return this;
        }

        public Builder setVehicleFilePath(String vehicleFilePath) {
            this.vehicleFilePath = vehicleFilePath;
            return this;
        }

        public Builder setHbefaVehicleDescriptionSource(EmissionsConfigGroup.HbefaVehicleDescriptionSource hbefaVehicleDescriptionSource) {
            this.hbefaVehicleDescriptionSource = hbefaVehicleDescriptionSource;
            return this;
        }

        public Builder setAverageWarmEmissionFactorsFilePath(String averageWarmEmissionFactorsFilePath) {
            this.averageWarmEmissionFactorsFilePath = averageWarmEmissionFactorsFilePath;
            return this;
        }

        public Builder setAverageColdEmissionFactorsFilePath(String averageColdEmissionFactorsFilePath) {
            this.averageColdEmissionFactorsFilePath = averageColdEmissionFactorsFilePath;
            return this;
        }

        public Builder setDetailedWarmEmissionFactorsFilePath(String detailedWarmEmissionFactorsFilePath) {
            this.detailedWarmEmissionFactorsFilePath = detailedWarmEmissionFactorsFilePath;
            return this;
        }

        public Builder setDetailedColdEmissionFactorsFilePath(String detailedColdEmissionFactorsFilePath) {
            this.detailedColdEmissionFactorsFilePath = detailedColdEmissionFactorsFilePath;
            return this;
        }

        public Builder setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior detailedVsAverageLookupBehavior) {
            this.detailedVsAverageLookupBehavior = detailedVsAverageLookupBehavior;
            return this;
        }

        public Builder setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel hbefaTableConsistencyCheckingLevel) {
            this.hbefaTableConsistencyCheckingLevel = hbefaTableConsistencyCheckingLevel;
            return this;
        }

        public Builder setHandlesHighAverageSpeeds(boolean handlesHighAverageSpeeds) {
            this.handlesHighAverageSpeeds = handlesHighAverageSpeeds;
            return this;
        }

        public Builder setWritingEmissionsEvents(boolean writingEmissionsEvents) {
            this.writingEmissionsEvents = writingEmissionsEvents;
            return this;
        }

        public Builder setCarrierPlanFile(String carrierPlanFile) {
            this.carrierPlanFile = carrierPlanFile;
            return this;
        }

        public ConfigGeneration build() {
            return new ConfigGeneration(configOutputPath, lastIteration, inputNetworkPath, scenarioOutputPath, vehicleFilePath,
                                       hbefaVehicleDescriptionSource, averageWarmEmissionFactorsFilePath, averageColdEmissionFactorsFilePath,
                                       detailedWarmEmissionFactorsFilePath, detailedColdEmissionFactorsFilePath, detailedVsAverageLookupBehavior,
                                       hbefaTableConsistencyCheckingLevel, handlesHighAverageSpeeds, writingEmissionsEvents, carrierPlanFile);
        }
    }
}
