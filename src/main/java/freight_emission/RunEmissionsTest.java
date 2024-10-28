package freight_emission;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunEmissionsTest {

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("../../data/intermediate/test/testEmissionsConfig.xml");
        System.out.println("Config loaded successfully");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        System.out.println("Scenario loaded successfully");

        Controler controler = new Controler(scenario);
        // add EmissionModule
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(EmissionModule.class).asEagerSingleton();
            }
        });

        controler.run();
    }
}
