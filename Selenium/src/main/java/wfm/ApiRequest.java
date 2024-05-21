package wfm;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.parsing.Parser;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.json.JSONArray;
import org.json.JSONException;
import org.testng.Assert;
import utils.Projects;
import utils.authorization.CsvLoader;
import wfm.components.utils.Role;
import wfm.models.User;
import wfm.repository.CommonRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.preemptive;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static utils.Links.DELETE_SHIFTS;
import static utils.Links.SHIFTS;
import static utils.tools.RequestFormers.makePath;

/**
 * Класс, объединяющий логику выполнения запросов через rest-assured.
 * В конструктор {@code Builder} передаются обязательные параметры: метод и адрес запроса. Все остальные параметры опциональны.
 * Если указывается ожидаемый код не из 200-х, то необходимо указать сообщение об ошибке, которое должен вернуть сервер.
 * Сборка и отправка запроса выполняются методом {@code Builder.send()}, который возвращает объект {@code ApiRequest},
 * из которого можно извлечь ответ для дальнейших проверок и манипуляций с данными.
 */
public class ApiRequest {
    private final User user;
    private final Method method;
    private final Map<String, String> headers;
    private final Object body;
    private final Map<String, String> params;
    private final String path;
    private final int status;
    private final String message;
    private final Boolean comment;
    private final ValidatableResponse response;

    private ApiRequest(Builder builder) {
        user = builder.user;
        method = builder.method;
        headers = builder.headers;
        body = builder.body;
        params = builder.params;
        path = builder.path;
        status = builder.status;
        message = builder.message;
        comment = builder.comment;
        response = builder.response;
    }

    public enum Method {
        POST,
        PUT,
        GET,
        DELETE
    }

    /**
     * Получает из ответа объект заданного класса
     */
    public <C> C returnPOJO(Class<C> objectClass) {
        if (method == Method.GET && response != null) {
            return response.extract().response().as(objectClass);
        }
        return null;
    }

    public <C> C returnPOJO() {
        if (method != Method.GET && response != null) {
            return (C) response.extract().response().as(body.getClass());
        }
        return null;
    }

    /**
     * Получает из ответа список объектов заданного класса
     */
    public <C> List<C> returnList(Class<C> objectClass, String jsonPath) {
        if (method == Method.GET && response != null && jsonPath != null) {
            return new ArrayList<>(response.extract().response().jsonPath().getList(jsonPath, objectClass));
        }
        return Collections.emptyList();
    }

    /**
     * Получает из ответа значение поля JSON, находящегося по указанному пути
     */
    public <T> T returnJsonValue(String jsonPath) {
        if (response != null && jsonPath != null) {
            return response.extract().response().jsonPath().get(jsonPath);
        }
        return null;
    }

    /**
     * Получает из ответа ссылку на созданный POST-запросом объект,
     * делает запрос туда и возвращает полученный объект указанного класса
     */
    public <C> C returnCreatedObject(Class<C> objectClass) {
        String location = response.extract().header("location");
        if (location == null) {
            return null;
        }
        return new ApiRequest.GetBuilder(location.replaceFirst("^.*/api/v\\d/", "")).send()
                .returnPOJO(objectClass);
    }

    /**
     * Получает из ответа id созданного POST-запросом объекта
     */
    public int returnCreatedObjectId() {
        String location = response.extract().header("location");
        if (location == null) {
            return 0;
        }
        return Integer.parseInt(location.replaceAll("^.*/", ""));
    }

    public <C> C returnCreatedObject() {
        return (C) returnCreatedObject(body.getClass());
    }

    /**
     * {@code ApiRequest} builder static inner class.
     */
    public static class Builder {
        private User user;
        private final Method method;
        private Map<String, String> headers;
        private Object body;
        private Map<String, String> params;
        private final String path;
        private int status;
        private String message;
        private Boolean comment;
        private String commentText;
        private ValidatableResponse response;
        private static final String TYPE = "application/hal+json";
        private static final String CONTENT_TYPE = "application/hal+json; charset=UTF-8";
        private static final String[] CREDENTIALS = CsvLoader.loginReturner(Projects.WFM, Role.ADMIN);
        private static final RequestSpecification ADMIN_REQUEST_SPEC = new RequestSpecBuilder()
                .setAccept(TYPE)
                .setContentType(CONTENT_TYPE)
                .setBaseUri(CommonRepository.URL_BASE)
                .setAuth(preemptive().basic(CREDENTIALS[0], CREDENTIALS[1]))
                .setBasePath(Projects.WFM.getApi())
                .build()
                .filter(new AllureRestAssured())
                .log()
                .all();
        private static final RequestSpecification USER_REQUEST_SPEC = new RequestSpecBuilder()
                .setAccept(TYPE)
                .setContentType(CONTENT_TYPE)
                .setBaseUri(CommonRepository.URL_BASE)
                .setBasePath(Projects.WFM.getApi())
                .build()
                .filter(new AllureRestAssured())
                .log()
                .all();
        private static final RequestSpecification USER_REQUEST_SPEC_WITHOUT_BASE_PATH = new RequestSpecBuilder()
                .setAccept(TYPE)
                .setContentType(CONTENT_TYPE)
                .setBaseUri(CommonRepository.URL_BASE)
                .build()
                .filter(new AllureRestAssured())
                .log()
                .all();

        private static final RequestSpecification NOTIFY_REQUEST_SPEC = new RequestSpecBuilder()
                .setAccept(TYPE)
                .setContentType(CONTENT_TYPE)
                .setBaseUri(CommonRepository.NOTIFY_APP)
                .build()
                .filter(new AllureRestAssured())
                .log()
                .all();

        private Builder(Method method, String path) {
            this.method = method;
            this.path = path;

            RestAssured.defaultParser = Parser.JSON;
            RestAssured.config = RestAssured.config()
                    .encoderConfig(encoderConfig().defaultContentCharset("UTF-8"));
        }

        /**
         * Задает значение {@code user} и возвращает ссылку на данный {@code Builder}
         */
        public Builder withUser(User val) {
            user = val;
            return this;
        }

        /**
         * Задает значение {@code body} и возвращает ссылку на данный {@code Builder}
         */
        public Builder withBody(Object val) {
            body = val;
            return this;
        }

        /**
         * Задает значение {@code params} и возвращает ссылку на данный {@code Builder}
         */
        public Builder withParams(Map<String, String> val) {
            params = val;
            return this;
        }

        /**
         * Задает значение {@code status} и возвращает ссылку на данный {@code Builder}
         */
        public Builder withStatus(int val) {
            status = val;
            return this;
        }

        /**
         * Задает значение {@code message} и возвращает ссылку на данный {@code Builder}
         */
        public Builder withMessage(String val) {
            message = val;
            return this;
        }

        /**
         * Задает значение {@code comment} и возвращает ссылку на данный {@code Builder}
         */
        public Builder withComment(Boolean val) {
            comment = val;
            return this;
        }

        /**
         * Задает значение {@code commentText} и возвращает ссылку на данный {@code Builder}
         */
        public Builder withCommentText(String val) {
            commentText = val;
            return this;
        }

        /**
         * Задает значение {@code header} и возвращает ссылку на данный {@code Builder}
         */
        public Builder withHeaders(Map<String, String> val) {
            headers = val;
            return this;
        }

        /**
         * Проверяет, что класс получил параметры в комбинациях, которые позволят сделать запрос выбранным методом
         */
        private void checkState() {
            if (status == 0) {
                if (method == Method.POST) {
                    status = 201;
                } else {
                    status = 200;
                }
            }
            if (!(status >= 200 && status < 300) && message == null) {
                throw new RuntimeException("Ожидается код ошибки, но не указано ожидаемое сообщение");
            }
        }

        /**
         * Смотрит на опциональные параметры и передает их в запрос, если они были заданы
         */
        private void specifyOptionalRequestParams(RequestSpecification spec) {
            if (body != null) {
                spec.body(body);
            }
            if (params != null && !params.isEmpty()) {
                spec.queryParams(params);
            }
            if (comment == null) {
                comment = true;
            }
            if (headers != null) {
                spec.headers(headers);
            }
            boolean deleteSingleShift = method == Method.DELETE && path.contains(SHIFTS) && comment && !path.contains(DELETE_SHIFTS);
            String commentBody = "{\"comment\": \"%s\"}";
            if (deleteSingleShift && commentText == null) {
                spec.body(String.format(commentBody, "смена удалена в рамках проведения автотеста"));
            } else if (deleteSingleShift) {
                spec.body(String.format(commentBody, commentText));
            }
        }

        /**
         * Собирает спецификацию запроса в зависимости от пользователя
         */
        private RequestSpecification prepareRequestSpec() {
            if (user == null) {
                return given().spec(ADMIN_REQUEST_SPEC);
            } else if (path.contains("api")) {
                return given().spec(USER_REQUEST_SPEC_WITHOUT_BASE_PATH).auth()
                        .preemptive().basic(user.getUsername(), user.getPassword());
            } else {
                return given().spec(USER_REQUEST_SPEC).auth()
                        .preemptive().basic(user.getUsername(), user.getPassword());
            }
        }

        /**
         * Передает в запрос метод
         */
        private ValidatableResponse specifyMethod(RequestSpecification spec) {
            if (method == Method.DELETE) {
                return spec.delete(path).then();
            } else if (method == Method.GET) {
                return spec.get(path).then();
            } else if (method == Method.POST) {
                return spec.post(path).then();
            } else {
                return spec.put(path).then();
            }
        }

        /**
         * Проверяет статус-код и сообщение сервера (если было указано) на соответствие ожидаемым
         */
        private void checkStatusCodeAndMessage() {
            int statusCode = response.extract().statusCode();
            if (statusCode != status && statusCode == 500) {
                String content = response.extract().header("content");
                if (content == null) {
                    String errorCode = response.extract().header("ERR_CODE");
                    if (errorCode != null) {
                        String errorMessage = new ApiRequest.GetBuilder(makePath("application-errors", errorCode))
                                .send()
                                .returnJsonValue("message").toString();
                        throw new AssertionError(String.format("Application error %s: %s", errorCode, errorMessage));
                    }
                }
            } else if (message != null) {
                try {
                    String errorMessage = new JSONArray(response.extract().body().asString()).getJSONObject(0).getString("message");
                    Assert.assertTrue(errorMessage.matches(message) || errorMessage.equals(message), "Сообщение об ошибке не соответствует ожидаемому");
                } catch (JSONException e) {
                    Assert.fail("Ответ не содержит сообщение об ошибке");
                }
            }
            response.statusCode(status);
        }

        /**
         * Возвращает {@code ApiRequest}, собранный в соответствии с ранее заданными параметрами
         */
        @Step()
        public ApiRequest send() {
            Allure.getLifecycle().updateStep(tr -> tr.setName("Отправить запрос " + method));
            checkState();
            RequestSpecification spec = prepareRequestSpec();
            specifyOptionalRequestParams(spec);
            spec = spec.when();
            response = specifyMethod(spec)
                    .log()
                    .ifValidationFails(LogDetail.BODY);
            checkStatusCodeAndMessage();
            return new ApiRequest(this);
        }
    }

    public static final class DeleteBuilder extends Builder {
        public DeleteBuilder(String path) {
            super(Method.DELETE, path);
        }

        public <T extends HasLinks> DeleteBuilder(T object) {
            super(Method.DELETE, object.getSelfLink());
        }
    }

    public static final class PostBuilder extends Builder {
        public PostBuilder(String path) {
            super(Method.POST, path);
        }
    }

    public static final class PutBuilder extends Builder {
        public PutBuilder(String path) {
            super(Method.PUT, path);
        }
    }

    public static final class GetBuilder extends Builder {
        public GetBuilder(String path) {
            super(Method.GET, path);
        }
    }
}
