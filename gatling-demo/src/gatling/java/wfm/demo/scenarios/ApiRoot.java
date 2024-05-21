package wfm.demo.scenarios;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.util.Map;

import static commons.ProtocolBuilders.BASIC_PROTOCOL;
import static commons.Requests.sendGetRequest;
import static commons.Utils.makePath;
import static commons.Utils.propertyReturner;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static wfm.demo.links.Back.API;
import static wfm.demo.links.Back.V1;
import static wfm.demo.links.Front.FAVICON;

public class ApiRoot {
    private ApiRoot() {

    }

    public static final HttpProtocolBuilder httpProtocol = BASIC_PROTOCOL.basicAuth(propertyReturner("user_login"), propertyReturner("user_password"));

    private static final Map<CharSequence, String> headersAcceptAll = Map.ofEntries(
            Map.entry("accept",
                      "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
    );

    public static final ScenarioBuilder root = scenario("ApiRoot")
            .group("Api v1").on(exec(sendGetRequest(makePath(API, V1))
                                             .headers(headersAcceptAll)
                                             .resources(sendGetRequest(FAVICON))));
}
