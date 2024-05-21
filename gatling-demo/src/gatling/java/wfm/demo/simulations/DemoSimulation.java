package wfm.demo.simulations;

import io.gatling.javaapi.core.Simulation;
import wfm.demo.scenarios.ApiRoot;
import wfm.demo.scenarios.BuildInfo;

import static commons.PopulationBuilders.*;

public class DemoSimulation extends Simulation {

    public DemoSimulation() {
        setUp(BuildInfo.info.injectOpen(HOLD).protocols(BuildInfo.httpProtocol),
              ApiRoot.root.injectOpen(RAMP).protocols(ApiRoot.httpProtocol)
        );
    }
}
