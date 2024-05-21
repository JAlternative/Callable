package commons;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

import static commons.Utils.replacePlaceholders;
import static io.gatling.javaapi.http.HttpDsl.http;

public class Requests {

    private Requests() {

    }

    public static HttpRequestActionBuilder sendGetRequest(String url) {
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        return http(replacePlaceholders(url)).get(url);
    }

}
