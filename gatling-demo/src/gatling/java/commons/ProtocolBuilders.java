package commons;

import io.gatling.javaapi.http.HttpProtocolBuilder;

import static commons.Utils.propertyReturner;
import static io.gatling.javaapi.http.HttpDsl.http;

public class ProtocolBuilders {
    private ProtocolBuilders() {

    }

    public static final HttpProtocolBuilder BASIC_PROTOCOL = http
            .baseUrl(propertyReturner("test_stand"))
            .inferHtmlResources()
            .acceptHeader("image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .acceptEncodingHeader("gzip, deflate")
            .acceptLanguageHeader("en-US,en;q=0.9,ru;q=0.8")
            .userAgentHeader("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36");
}
