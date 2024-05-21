package utils.authorization;

import io.qameta.allure.Allure;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.openqa.selenium.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import utils.Links;
import utils.Projects;
import wfm.components.utils.Role;
import wfm.models.User;
import wfm.repository.CommonRepository;

import java.io.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.StringTokenizer;

import static utils.authorization.CookieTools.settingsConnectionManager;
import static utils.authorization.CookieTools.updateCookieDateTime;
import static utils.tools.RequestFormers.setUri;

public class CookieRW {

    private static final String PATH = "datainput/";
    private static final String WFM_PATH = "wfm/";
    private static final String BIO_PATH = "bio/";
    private static final String EXTENSION = ".data";
    private static final Logger LOG = LoggerFactory.getLogger(CookieRW.class);
    private static final String WFM_URL = Links.getTestProperty("release");
    private static final String BIO_URL = Links.getTestProperty("central");

    private CookieRW() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Один из двух методов для связи с внешним миром для суперюзера
     *
     * @return Возвращает куки класса Cookie (т. к. они подходят для вэбдрайвера)
     */
    public static Cookie getCookieWithCheck(Projects project) {
        String[] loginPair = CsvLoader.loginReturner(project, Role.ADMIN);
        Allure.addAttachment("Авторизация", String.format("Логин: %s, пароль: %s", loginPair[0], loginPair[1]));
        BasicClientCookie clientCookie = getCookieWithCheck(project, Role.ADMIN, loginPair[0], loginPair[1]);
        return clientCookie != null ? new Cookie(clientCookie.getName(), clientCookie.getValue(),
                                                 clientCookie.getDomain(), clientCookie.getPath(), null, clientCookie.isSecure()) : null;
    }

    /**
     * Второй метод для остальных ролей (кроме суперюзера)
     *
     * @param project - WFM/BIO
     * @param role    - Role енам
     * @param user    - Передается объект юзера, из него достается логин и пароль для basic auth
     * @return Возвращает куки класса Cookie (т. к. они подходят для вэбдрайвера)
     */
    public static Cookie getCookieWithCheck(Projects project, Role role, User user) {
        BasicClientCookie clientCookie = getCookieWithCheck(project, role, user.getUsername(), user.getPassword());
        return clientCookie != null ? new Cookie(clientCookie.getName(), clientCookie.getValue(),
                                                 clientCookie.getDomain(), clientCookie.getPath(), null, clientCookie.isSecure()) : null;
    }

    /**
     * Берет пароль и логин с файла
     *
     * @param project - WFM/BIO
     * @return Возвращает полностью BasicClientCookie
     */

    public static BasicClientCookie getBasicCookieWithCheck(Projects project) {
        String[] loginPair = CsvLoader.loginReturner(project, Role.ADMIN);
        return getCookieWithCheck(project, Role.ADMIN, loginPair[0], loginPair[1]);
    }

    /**
     * Обновление куки файла
     *
     * @param project  - WFM/BIO
     * @param role     - Role енам
     * @param login    - Логин
     * @param password - Пароль
     * @return - производит логин -> записывает куки в файл для определенной роли
     */
    private static BasicClientCookie updateCookieFile(Projects project, Role role, String login, String password) throws IOException {
        ImmutablePair<BasicClientCookie, LocalDateTime> cookieDate = login(login, password, project);
        writeCookie(project, role, cookieDate);
        return cookieDate.left;
    }

    /**
     * Получение и обмен куки с проверкой
     *
     * @return - 1. Получаем cookie и дату последнего использования из файла
     * 2. Если дата отличается от текущей более чем на 15 мин, если domain отличается от сайта, на котором запущен тест,
     * если файл куки пустой или не существует -> берем новую куку
     * 3. Иначе преобразуем Строку в Cookie -> возвращаем
     */
    private static BasicClientCookie getCookieWithCheck(Projects project, Role role, String login, String password) {
        try {
            if (project.equals(Projects.BIO)) {
                return updateCookieFile(project, role, login, password);
            }
            ImmutablePair<BasicClientCookie, LocalDateTime> cookieDate;
            if (role == Role.ADMIN) {
                cookieDate = readCookieFile(project, role);
            } else {
                cleanCookieFile(project, role);
                cookieDate = ImmutablePair.nullPair();
            }
            LocalDateTime dateTime = cookieDate != null ? cookieDate.right : null;
            if (dateTime == null || dateTime.until(LocalDateTime.now(), ChronoUnit.MINUTES) >= 15) {
                return updateCookieFile(project, role, login, password);
            }
            String domain = cookieDate.getLeft().getDomain();
            String actualDomain = URI.create(CommonRepository.URL_BASE).getAuthority();
            if (!domain.equals(actualDomain)) {
                return updateCookieFile(project, role, login, password);
            }
            BasicClientCookie clientCookie = cookieDate.left;
            updateCookieDateTime(new File(getFileName(project, role)), LocalDateTime.now());
            return clientCookie;
        } catch (IOException e) {
            LOG.info("Не удалось выполнить операцию");
        }
        return null;
    }

    /**
     * Отправление запроса на авторизацию
     *
     * @param login    - Логин для входа
     * @param password - Пароль для входа
     * @param project  - Проект WFM/BIO
     * @return - Отправляет запрос с basic auth, в ответе возвращаются хэдеры с cookies, метод возвращает пару
     * из LocalDateTime(Берется текущая дата) и BasicClientCookie
     */
    private static ImmutablePair<BasicClientCookie, LocalDateTime> login(String login, String password, Projects project) throws IOException {
        String url = getUrl(project);
        URI uri;
        if (project.equals(Projects.BIO)) {
            uri = Objects.requireNonNull(setUri(project, url, "login"));
        } else {
            uri = Objects.requireNonNull(setUri(project, url + "/login", ""));
        }
        HttpHost targetHost = new HttpHost(uri.getHost(), -1, uri.getScheme());
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                                           new UsernamePasswordCredentials(login, password));
        AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());
        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(credentialsProvider);
        context.setAuthCache(authCache);
        HttpClient client = HttpClientBuilder.create()
                .setConnectionManager(settingsConnectionManager())
                .build();
        HttpResponse response = client.execute(
                new HttpGet(uri.toString()), context);
        int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
            case HttpStatus.SC_UNAUTHORIZED:
                LOG.info("Логин авторизации: {}, пароль: {}", login, password);
                Allure.addAttachment("Тестируемый адрес", url);
                Assert.fail("Не удалось авторизоваться из-за проблем с аутентификацией.");
                break;
            case HttpStatus.SC_BAD_GATEWAY:
                Allure.addAttachment("Тестируемый адрес", url);
                Assert.fail("Сайт недоступен. Ошибка 502");
                break;
            case HttpStatus.SC_OK:
                break;
            default:
                LOG.info("Добавьте обработку статуса");
                Allure.addAttachment("Тестируемый адрес", url);
                Assert.fail("Не удалось отправить запрос cookie. Статус ответа: " + statusCode);
        }
        String cookieString = response.getFirstHeader("Set-cookie").getValue();
        String name = cookieString.split("=")[0];
        String value = cookieString.split("=")[1].split(";")[0];
        String path = cookieString.split("=")[2].split(";")[0];
        BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setDomain(uri.getHost());
        cookie.setPath(path);
        return new ImmutablePair<>(cookie, LocalDateTime.now());
    }

    /**
     * Чтение куки файла
     *
     * @param project - проект WFM/BIO
     * @param role    - Роль из енама
     * @return - Читает файл с cookies определённой роли, возвращает пару из BasicCookieClient и LocalDateTime
     */
    private static ImmutablePair<BasicClientCookie, LocalDateTime> readCookieFile(Projects project, Role role) throws IOException {
        File file = new File(getFileName(project, role));
        if (file.createNewFile()) {
            LOG.info("Был создан файл с именем \"{}\"", file.getName());
        }
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = bufferedReader.readLine();
        if (line != null) {
            StringTokenizer token = new StringTokenizer(line, ";");
            String name = token.nextToken();
            String value = token.nextToken();
            String domain = token.nextToken();
            String path = token.nextToken();
            //для обычной даты
            token.nextToken();
            boolean isSecure = Boolean.parseBoolean(token.nextToken());
            String d = token.nextToken();
            if (d.contains(".") && d.length() > 25) {
                d = d.substring(0, 23);
            }
            LocalDateTime dateTime = LocalDateTime.parse(d);
            BasicClientCookie cookie = new BasicClientCookie(name, value);
            cookie.setDomain(domain);
            cookie.setPath(path);
            cookie.setSecure(isSecure);
            return new ImmutablePair<>(cookie, dateTime);
        }
        return null;
    }

    /**
     * Запись куки файлов
     *
     * @param project    - проект WFM/BIO
     * @param role       - Роль из енама
     * @param cookiePair - Записывает в файл переданные в метод BasicCookieClient, в конце добавляет
     *                   LocalDateTime переданную в метод
     */
    private static void writeCookie(Projects project, Role role, ImmutablePair<BasicClientCookie, LocalDateTime> cookiePair) {
        File file = new File(getFileName(project, role));
        try (FileWriter fileWrite = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWrite)) {
            if (file.createNewFile()) {
                LOG.info("Создан файл: {}", file.getName());
            }
            BasicClientCookie JSESSIONID = cookiePair.left;
            LocalDateTime dateTime = cookiePair.right;
            StringBuilder stringBuilder = JSESSIONID != null ?
                    new StringBuilder().append(JSESSIONID.getName())
                            .append(";")
                            .append(JSESSIONID.getValue())
                            .append(";")
                            .append(JSESSIONID.getDomain())
                            .append(";")
                            .append(JSESSIONID.getPath())
                            .append(";")
                            .append(JSESSIONID.getExpiryDate())
                            .append(";")
                            .append(JSESSIONID.isSecure())
                            .append(";")
                            .append(dateTime) : new StringBuilder();
            bufferedWriter.write((stringBuilder.toString()));
            bufferedWriter.newLine();
            stringBuilder.setLength(0);
        } catch (IOException e) {
            LOG.info("Файл не найден; Файл был создан под именем {}", file.getName());
        }
    }

    /**
     * Получение имени файла
     *
     * @param project - проект WFM/BIO
     * @param role    - Роль из енама
     * @return path to the file with cookies
     **/
    private static String getFileName(Projects project, Role role) {
        String addName = "Cookies_" + project.toString() + "_";
        return PATH + (project == Projects.WFM ? WFM_PATH : BIO_PATH) + addName + role.getName() + EXTENSION;
    }

    /**
     * Получение URL сайта
     *
     * @param projects - проект WFM/BIO
     * @return при выборе BIO берет BIO_URL, аналогично с WFM
     */
    private static String getUrl(Projects projects) {
        switch (projects) {
            case BIO:
                return BIO_URL;
            case WFM:
                return WFM_URL;
        }
        return "";
    }

    /**
     * Очистка куки файлов
     *
     * @param role Роль из енама
     */
    public static void cleanCookieFile(Projects project, Role role) {
        String fileName = getFileName(project, role);
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.print("");
            writer.flush();
        } catch (Exception e) {
            LOG.info("Очистка файла Cookie не выполнилась");
        }
    }
}

