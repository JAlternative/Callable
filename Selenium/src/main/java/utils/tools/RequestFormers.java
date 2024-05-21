package utils.tools;

import io.qameta.allure.Allure;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.*;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import utils.Projects;
import utils.authorization.ClientReturners;
import utils.authorization.CsvLoader;
import wfm.components.utils.Role;
import wfm.repository.CommonRepository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RequestFormers {

    public static final int LUKOIL_CUSTOM_PORT = 8444;
    private static final Logger LOG = LoggerFactory.getLogger(RequestFormers.class);

    private RequestFormers() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Метод выполняет функцию контроля порта, подставляя в адрес необходимый порт для каждого URL.
     *
     * @param pageUrl - текущий URL адрес страницы
     * @return - строка с базовым URL адресом и поротом
     */
    private static URIBuilder urlPortController(String pageUrl) {
        URIBuilder builder;
        try {
            builder = new URIBuilder(pageUrl);
        } catch (URISyntaxException e) {
            throw new AssertionError("Неправильный URL адрес: " + pageUrl, e);
        }
        //Если появятся исключения, то дописать под switch
        //такие манипуляции убирают зависимость от utils.Projects
        int defaultPort = pageUrl.contains("lukoil") ? LUKOIL_CUSTOM_PORT : -1;
        return builder.setPort(defaultPort);
    }

    /**
     * Метод объединяет все части URL адреса при этом контролирует порт, базовый URL пейджа и добавляет параметры.
     *
     * @param project        - id проекта
     * @param currentUrl     - текущий URL, где находится вебдрайвер
     * @param path           - путь URL для работы с API, который должен быть добавлен к основному URL страницы
     * @param nameValuePairs - лист параметров для добавления к адресу
     * @return - полный URI адрес для работы с апи
     */
    public static URI setUri(Projects project, String currentUrl, String path, List<NameValuePair> nameValuePairs) {
        try {
            URIBuilder baseUri = urlPortController(currentUrl);
            return baseUri.setPath(project.getApi() + "/" + path).setParameters(nameValuePairs).build();
        } catch (URISyntaxException e) {
            throw new AssertionError("schedule message. Не смогли собрать URI для запроса.", e);
        }
    }

    public static URI setUri(String project, String currentUrl, String path, List<NameValuePair> nameValuePairs) {
        try {
            URIBuilder baseUri = urlPortController(currentUrl);
            return baseUri.setPath(project + "/" + path).setParameters(nameValuePairs).build();
        } catch (URISyntaxException e) {
            throw new AssertionError("schedule message. Не смогли собрать URI для запроса.", e);
        }
    }

    /**
     * Метод объединяет все части URL адреса при этом контролирует порт и базовый URL пейджа, но уже без параметров.
     *
     * @param project    - id проекта
     * @param currentUrl - текущий URL, где находится вебдрайвер
     * @param path       - путь URL для работы с API, который должен быть добавлен к основному URL страницы
     * @return - полный URI адрес для работы с апи
     */
    public static URI setUri(Projects project, String currentUrl, String path) {
        try {
            URIBuilder baseUri = urlPortController(currentUrl);
            if (path.contains("api")) {
                return baseUri.setPath(path).build();
            } else {
                return baseUri.setPath(project.getApi() + "/" + path).build();
            }
        } catch (URISyntaxException e) {
            throw new AssertionError("schedule message. Не смогли собрать URI для запроса.", e);
        }
    }

    public static URI setUri(String project, String currentUrl, String path) {
        try {
            URIBuilder baseUri = urlPortController(currentUrl);
            if (path.contains("api")) {
                return baseUri.setPath(path).build();
            } else {
                return baseUri.setPath(project + "/" + path).build();
            }
        } catch (URISyntaxException e) {
            throw new AssertionError("schedule message. Не смогли собрать URI для запроса.", e);
        }
    }

    /**
     * Позволяет сразу взять JSON объект по запросу в апи
     *
     * @param projects       - тип проекта
     * @param currentUrl     - текущий URL пейджа
     * @param path           - добавочный фрагмент пути, который идет после порта
     * @param nameValuePairs - список параметров
     * @return - джисон объект по построенному адресу
     */
    public static JSONObject getJsonFromUri(Projects projects, String currentUrl,
                                            String path, List<NameValuePair> nameValuePairs) {
        URI uri = setUri(projects, currentUrl, path, nameValuePairs);
        return getJsonFromUri(projects, uri);
    }

    public static JSONObject getJsonFromUri(Projects projects, String api, String currentUrl,
                                            String path, List<NameValuePair> nameValuePairs) {
        URI uri = setUri(api, currentUrl, path, nameValuePairs);
        return getJsonFromUri(projects, uri);
    }

    /**
     * Позволяет сразу взять JSON объект по запросу в апи, но уже без параметров
     *
     * @param projects   - тип проекта
     * @param currentUrl - текущий URL пейджа
     * @param path       - добавочный фрагмент пути, который идет после порта
     * @return - джисон объект по построенному адресу
     */
    public static JSONObject getJsonFromUri(Projects projects, String currentUrl, String path) {
        URI uri = setUri(projects, currentUrl, path);
        return getJsonFromUri(projects, uri);
    }

    public static JSONObject getJsonFromUri(Projects projects, URI uri) {
        String entity = setUrlAndInitiateForApi(uri, projects);
        if (entity.equals("")) {
            return null;
        }
        return new JSONObject(entity);
    }

    public static JSONObject getJsonFromUri(Projects projects, String link) {
        return getJsonFromUri(projects, URI.create(link));
    }

    public static JSONArray getJsonArrayFromUri(Projects projects, URI uri) {
        String entity = setUrlAndInitiateForApi(uri, projects);
        return new JSONArray(entity);
    }

    public static JSONArray getJsonArrayFromUri(Projects projects, URI uri, ImmutablePair<String, String> header) {
        String entity = setUrlAndInitiateForApi(uri, projects, header);
        return new JSONArray(entity);
    }

    /**
     * Позволяет сразу взять JSON объект по запросу в апи, но уже без параметров
     * Сделан специально для wfm.models.User.RoleInUser#getRoleLinks(), так как getJsonFromUri() для него возвращает не JSON, а массив
     *
     * @param projects - тип проекта
     * @param uri      - текущий URL пейджa
     * @return - джисон объект по построенному адресу
     */
    public static List<Integer> getJsonFromUriForArrays(Projects projects, URI uri) {//todo доработать для работы с дженериками
        String entity = setUrlAndInitiateForApi(uri, projects);
        entity = entity.substring(1, entity.length() - 1);
        List<Integer> list = new ArrayList<>();
        if (!entity.isEmpty()) {
            for (String s : entity.split(",")) {
                int i = Integer.parseInt(s);
                list.add(i);
            }
        }
        return list;
    }

    /**
     * Метод облегчает работу создания параметров для URI, инициализирует новый объект и оборачивает параметр в строку
     *
     * @param name  - название параметра
     * @param value - параметр
     * @return - объект NameValuePair по типу name=value
     */
    public static NameValuePair newNameValue(String name, Object value) {
        return new BasicNameValuePair(name, String.valueOf(value));
    }

    /**
     * Метод проводит инициализацию для работы с API по заданному адресу.
     *
     * @param uri     - адрес страницы API с необходимыми параметрами
     * @param project - тип проекта
     * @return - JSON в виде строки
     */
    public static String setUrlAndInitiateForApi(URI uri, Projects project) {
        return setUrlAndInitiateForApi(uri, project, null);
    }

    public static String setUrlAndInitiateForApi(URI uri, Projects project, ImmutablePair<String, String> header) {
        HttpUriRequest requestValues;
        if (header != null) {
            requestValues = RequestBuilder.get().setUri(uri).setHeader(header.getLeft(), header.getRight()).build();
        } else {
            requestValues = RequestBuilder.get().setUri(uri).build();
        }
        //TODO дубликат кода utils.authorization.CookieRW.login, развязать авторизацию через api и UI @m.druzhinin
        String[] loginPair = CsvLoader.loginReturner(project, Role.ADMIN);
        uri = Objects.requireNonNull(setUri(project, uri.toString(), uri.getPath()));
        HttpHost targetHost = new HttpHost(uri.getHost(), -1, uri.getScheme());
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                                           new UsernamePasswordCredentials(loginPair[0], loginPair[1]));
        AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());
        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(credentialsProvider);
        context.setAuthCache(authCache);

        String jsonObject = null;
        try {
            HttpResponse httpResponseValues = ClientReturners.httpClientReturner(project).execute(requestValues, context);
            assertStatusCode(httpResponseValues, uri.toString());
            if (httpResponseValues != null) {
                HttpEntity values = httpResponseValues.getEntity();
                jsonObject = EntityUtils.toString(values);
                LOG.info("GET {} {}", httpResponseValues.getStatusLine().getStatusCode(), uri);
            } else {
                LOG.error("Response is null");
            }
        } catch (IOException e) {
            LOG.error("Не выполнился запрос", e);
            Allure.addAttachment("Неудачный запрос",
                                 String.format("Не удалось отправить запрос на %s: %s", uri, e.getMessage()));
        }
        return jsonObject != null ? jsonObject : "{}";//TODO fix failed tests on jenkins
    }

    /**
     * Формирует и отправляет запрос в апи по заданным параметрам
     *
     * @param uri            - адрес отправки запроса
     * @param jsonObject     - объект для отправки на адрес
     * @param requestBuilder - тип отправки запроса
     * @param contentType    - тип контента запроса
     * @return возвращает ответ сервера на запрос
     */
    public static HttpResponse requestMaker(URI uri, JSONObject jsonObject,
                                            RequestBuilder requestBuilder, ContentType contentType) {
        return requestMaker(uri, jsonObject, requestBuilder, contentType, Projects.WFM);
    }

    public static HttpResponse requestMaker(URI uri, Object jsonObject, RequestBuilder requestBuilder,
                                            ContentType contentType, Projects projects) {
        return requestMaker(uri, jsonObject, requestBuilder, contentType, projects, null);
    }

    public static HttpResponse requestMaker(URI uri, Object jsonObject, RequestBuilder requestBuilder,
                                            ContentType contentType, Projects projects, ImmutablePair<String, String> header) {
        StringEntity requestEntity = new StringEntity(jsonObject.toString(), contentType);
        HttpUriRequest some;
        if (header != null) {
            some = requestBuilder
                    .setUri(uri)
                    .setEntity(requestEntity)
                    .addHeader(header.getLeft(), header.getRight())
                    .build();
        } else {
            some = requestBuilder
                    .setUri(uri)
                    .setEntity(requestEntity)
                    .build();
        }
        HttpResponse response = null;
        try {
            response = ClientReturners.httpClientReturner(projects).execute(some);
            String method = requestBuilder.getMethod();
            LOG.info("{} {} {}", method, response.getStatusLine().getStatusCode(), uri);
        } catch (IOException e) {
            LOG.error("Не выполнился запрос", e);
            Allure.addAttachment("Неудачный запрос",
                                 String.format("Не удалось отправить запрос на %s: %s", uri, e.getMessage()));
        }
        return response;
    }

    /**
     * Соединяет фрагменты path в один путь
     *
     * @param delimiter разделитель фрагментов
     * @param parts     фрагменты пути по порядку
     */
    private static String makeGenericPath(char delimiter, Object... parts) {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i < parts.length - 1) {
                path.append(parts[i]).append(delimiter);
            } else {
                path.append(parts[i]);
            }
        }
        return path.toString();
    }

    public static String makePath(Object... parts) {
        return makeGenericPath('/', parts);
    }

    public static String makeJsonPath(Object... parts) {
        return makeGenericPath('.', parts);
    }

    /**
     * Удаляет что угодно из апи, по указанной ссылке
     *
     * @param uri - ссылка на удаление
     * @return возвращает ответ сервера
     */
    public static HttpResponse deleteMaker(URI uri) {
        RequestBuilder builder = RequestBuilder.delete().setUri(uri);
        if (uri.getPath().contains("shift")) {
            StringEntity requestEntity = new StringEntity("{\"comment\": \"смена удалена в рамках проведения автотеста\"}",
                                                          ContentType.create("application/json", Consts.UTF_8));
            builder = builder.setEntity(requestEntity);
        }
        HttpUriRequest deleteAnyThing = builder.build();
        HttpResponse response = null;
        try {
            response = ClientReturners.httpClientReturner(Projects.WFM).execute(deleteAnyThing);
            assertStatusCode(response, uri.toString());
            LOG.info("DELETE {} {}", response.getStatusLine().getStatusCode(), uri);
            Allure.addAttachment("Удаление объекта", "Удален объект по ссылке " + uri);
        } catch (IOException e) {
            LOG.debug("Удаление по запросу {} не произошло", uri, e);
            Allure.addAttachment("Неудачное удаление", String.format("Попытка удаления по адресу %s завершилась с ошибкой: %s", uri, e));
        }
        return response;
    }

    /**
     * Производит проверку статус когда на соответствие ожидаемой. Выдает сообщения при критичных ошибках
     *
     * @param response     - ответ сервера
     * @param expectedCode - код, который ожидаем
     * @param link         - адрес в который стучались
     */
    public static void assertStatusCode(HttpResponse response, int expectedCode, String link) {
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != expectedCode) {
                if (statusCode == HttpStatus.SC_BAD_GATEWAY) {
                    LOG.error("{} {}", HttpStatus.SC_BAD_GATEWAY, link);
                    Allure.addAttachment("Адрес запроса", link);
                    throw new AssertionError("Ошибка 502. Бэк не работает");
                } else {
                    String errorMessage = EntityUtils.toString(response.getEntity());
                    if (errorMessage.equals("") && statusCode == 500) {
                        if (response.getFirstHeader("ERR_CODE") != null) {
                            String errorCode = response.getFirstHeader("ERR_CODE").getValue();
                            JSONObject applicationError = getJsonFromUri(Projects.WFM, setUri(
                                    Projects.WFM, CommonRepository.URL_BASE, makePath("application-errors", errorCode)));
                            String logMessage = String.format("Application error %s: %s", errorCode, applicationError.optString("message"));
                            logError(link, logMessage, statusCode);
                        }
                        return;
                    } else if (!errorMessage.startsWith("[")) {
                        errorMessage = "[]";
                    }
                    JSONObject errorObject = new JSONArray(errorMessage).optJSONObject(0);
                    String logMessage = errorObject != null ? errorObject.optString("message") : "";
                    logError(link, logMessage, statusCode);
                }
            }
        } catch (NullPointerException e) {
            throw new AssertionError("Нет ответа от сервера");
        } catch (IOException e) {
            LOG.info("Проверка статус кода не пройдена", e);
        }
    }

    private static void logError(String link, String logMessage, int statusCode) {
        LOG.error("{} {}", link, logMessage);
        String assertionText = String.format(
                "Не удалось отправить запрос. %nСообщение: %s%nОшибка %d",
                logMessage,
                statusCode);
        LOG.error("{} {}", statusCode, link);
        Allure.addAttachment("Адрес запроса", link);
        throw new AssertionError(assertionText);
    }

    public static void assertStatusCode(HttpResponse response, String link) {
        assertStatusCode(response, HttpStatus.SC_OK, link);
    }

    public static void checkRosterActionSuccess(HttpResponse response) {
        JSONObject json;
        try {
            json = new JSONObject(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Allure.addAttachment("Ответ сервера при публикации/отправке на утверждение", String.valueOf(json));
        Assert.assertTrue(json.getBoolean("success"), "Ошибка при публикации/отправке на утверждение");
    }

    public static void uploadFile(URI uri, String path) {
        RequestBuilder builder = RequestBuilder.post().setUri(uri);
        File hintFile = new File(path);
        FormBodyPartBuilder partBuilder = FormBodyPartBuilder.create("file", new FileBody(hintFile));
        partBuilder.addField(MIME.CONTENT_TYPE, "application/pdf");
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()
                .setCharset(StandardCharsets.UTF_8)
                .addPart(partBuilder.build())
                .addTextBody("customType", "CALCULATION_HINT")
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        HttpEntity entity = entityBuilder.build();
        builder.setEntity(entity);
        HttpUriRequest postRequest = builder.build();
        try {
            HttpResponse response = ClientReturners.httpClientReturner(Projects.WFM).execute(postRequest);
            assertStatusCode(response, uri.toString());
        } catch (IOException e) {
            LOG.info("Не удалось отправить запрос на загрузку файла");
            Allure.addAttachment("Неудачная загрузка файла", String.format("Не удалось загрузить файл %s на эндпоинт %s", path, uri));
        }
    }
}
