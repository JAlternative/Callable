package integration;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;
import utils.authorization.CsvLoader;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.preemptive;

public class IntegrationApiHelpers {
    static String type = "application/hal+json";
    static String[] credentials = CsvLoader.integrationCredentialsReturner();
    static final RequestSpecification intRequestSpec = new RequestSpecBuilder()
            .setAccept(type)
            .setContentType(type)
            .setAuth(preemptive().basic(credentials[0], credentials[1]))
            .build()
            .log()
            .all();

    public static void getRequest(String path, Map<String, String> params, String baseUri) {
        ValidatableResponse response = given().spec(intRequestSpec)
                .baseUri(baseUri)
                .queryParams(params)
                .when()
                .get(path)
                .then()
                .log()
                .all()
                .statusCode(200);
        try {
            List<String> resp = response.extract().body().jsonPath().get("message");
            Assert.assertTrue(resp.isEmpty(), String.format("Не удалось отправить запрос на сервер интеграции. Ошибка: \"%s\"", resp.get(0)));
        } catch (JsonPathException e) {
            // Если в теле ответа не найден json с сообщением об ошибке, то все хорошо, запрос прошел успешно, можно ничего не делать.
        }
    }
}
