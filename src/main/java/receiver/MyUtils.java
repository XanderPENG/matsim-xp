package receiver;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

public class MyUtils {

    public static Config createExampleConfigWithDefaultNetwork() {
        URL context = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");
        Config config = ConfigUtils.createConfig();
        config.setContext(context);
        config.network().setInputFile("grid9x9.xml");
        config.controller().setOutputDirectory("../../data/intermediate/test/receiver/");
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(10);
        return config;
    }
}
