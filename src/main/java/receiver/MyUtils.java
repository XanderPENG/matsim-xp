package receiver;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

public class MyUtils {

    public static Config createExampleConfigWithDefaultNetwork() {
        URL context = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");
        Config config = ConfigUtils.createConfig();
        config.setContext(context);
        config.network().setInputFile("grid9x9.xml");
        return config;
    }
}
