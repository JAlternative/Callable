package wfm.demo.scenarios;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static commons.ProtocolBuilders.BASIC_PROTOCOL;
import static commons.Requests.sendGetRequest;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static wfm.demo.links.Back.BUILD_INFO;
import static wfm.demo.links.Front.FAVICON;

public class BuildInfo {
    private BuildInfo() {

    }

    public static final HttpProtocolBuilder httpProtocol = BASIC_PROTOCOL;

    public static final ScenarioBuilder info = scenario("BuildInfo")
            .group("Build info").on(exec(sendGetRequest(BUILD_INFO).resources(sendGetRequest(FAVICON))));
}
