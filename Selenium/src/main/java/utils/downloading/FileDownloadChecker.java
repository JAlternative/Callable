package utils.downloading;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Links;
import utils.Projects;
import wfm.components.utils.Role;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;

import static utils.Links.DATA_INPUT;
import static utils.Links.getTestProperty;

public abstract class FileDownloadChecker implements DownloadBehaviors {

    private static final Logger LOG = LoggerFactory.getLogger(FileDownloadChecker.class);
    private URI uri;
    private final Role role;
    private final TypeOfFiles typeOfFiles;
    private final Projects project;

    /**
     * Инициализирует класс FileDownloadChecker с с минимум необходимых параметров для построения и работы с запросами
     *
     * @param project     - проект, для взятия правильного URL и формирования правильных cookie
     * @param role        - роль для правильной авторизации под ролью
     * @param typeOfFiles - тип файлов который мы будем скачивать
     */
    FileDownloadChecker(Projects project, Role role, TypeOfFiles typeOfFiles) {
        this.project = project;
        switch (project) {
            case WFM:
                this.uri = URI.create(Links.getTestProperty("release"));
                break;
            case BIO:
                this.uri = URI.create(getTestProperty("central"));
                break;
        }
        this.role = role;
        this.typeOfFiles = typeOfFiles;
    }

    public Role getRole() {
        return role;
    }

    public URI getUri() {
        return uri;
    }

    public TypeOfFiles getTypeOfFiles() {
        return typeOfFiles;
    }

    /**
     * Метод для извлечения имени файла из хедера ответа на запрос
     *
     * @param httpResponse - ответ от серверана запрос
     * @return пару где левое значение это название файла, а правое это расширение
     */
    public static ImmutablePair<String, String> getFileNameExtensionFromResponse(HttpResponse httpResponse) {
        //группа хедеров с Content-Disposition хранит информацию о файле
        Header[] content = httpResponse.getHeaders("Content-Disposition");
        String filename = "";
        headerCycle:
        for (Header header : content) {
            for (HeaderElement headerElement : header.getElements()) {
                for (NameValuePair nameValuePair : headerElement.getParameters()) {
                    //в конце простая NameValuePair в которой под ключом "filename" хранится имя файла
                    if (nameValuePair.getName().equals("filename")) {
                        filename = nameValuePair.getValue();
                        break headerCycle;
                    }
                }
            }
        }
        //делим файл на расширение и название и складываем в ImmutablePair
        Iterator<String> fileParts = Arrays.asList(filename.split("\\.")).iterator();
        String name = fileParts.next();
        String extension = fileParts.next();
        return new ImmutablePair<>(name, extension);
    }

    /**
     * Берем куки для роли и под URL указанного проекта
     *
     * @param role     - роль для которой берем куки
     * @param projects - для какого проекта
     */
    public BasicCookieStore getBasicCookieStore(Role role, Projects projects) {
        String cookiesFolder;
        String stringForPaths;
        cookiesFolder = "Cookies_" + projects.toString() + "_";
        stringForPaths = DATA_INPUT + projects.getName() + "/" + cookiesFolder + role.getName() + ".data";
        String[] cookieFileContent = new String[0];
        try {
            cookieFileContent = Files.readAllLines(Paths.get(stringForPaths),
                    StandardCharsets.UTF_8).get(0).split(";");
        } catch (IOException e) {
            LOG.info("Содержимое файла не прочитано", e);
        }
        String name = cookieFileContent[0];
        String value = cookieFileContent[1];
        String path = cookieFileContent[3];
        BasicCookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setDomain(getUri().getHost());
        cookie.setPath(path);
        cookie.setExpiryDate(null);
        cookie.setSecure(false);
        cookieStore.addCookie(cookie);
        return cookieStore;
    }

    /**
     * Делаем запрос на указанный адрес с выбранным типом контента, для выбранного проекта и под переденной роль,
     * получаем ответ и возвращаем его
     * @param certainAcceptContent - тип контента в запросе
     * @param downloadUrl          - URL адрес на который будем отправлять запрос
     */
    HttpResponse getHttpResponse(Projects project, Role role, TypeOfAcceptContent certainAcceptContent,
                                 URIBuilder downloadUrl) {
        //берем куки
        BasicCookieStore cookieStore = getBasicCookieStore(role, project);
        //Формирование запроса
        HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
        HttpGet httpGet = null;
        try {
            httpGet = new HttpGet(downloadUrl.build());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        LOG.info("URI is {}", httpGet.getURI());
        if (certainAcceptContent != null) {
            //добавляем тип принимаемого контента
            httpGet.addHeader("Accept", certainAcceptContent.getAcceptContent());
        }
        try {
            //делаем запрос
            return httpClient.execute(httpGet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Метод для получения ответа на запрос при использовании подклассов данлоад чекера,
     * позволяет получать ответ передевая только роль и тип контента, чего хватает для осуществления запроса
     * при правильно инициализированном подклассе
     *
     * @param role                 - роль для которой будем получать ответ
     * @param certainAcceptContent - тип контента
     */
    public HttpResponse downloadResponse(Role role, TypeOfAcceptContent certainAcceptContent) {
        URIBuilder downloadUrlFormer = downloadUrlFormer();
        return getHttpResponse(project, role, certainAcceptContent, downloadUrlFormer);
    }

    /**
     * Метод который вернет URL адрес, используется для его сравнения с адресом в верхней части строки
     * @return uri
     */
    public URI getDownloadLink() {
        URIBuilder quarry = downloadUrlFormer();
        try {
            return quarry.build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Метод занимается тем что добавляет нижее подчеркивание между отдальными строками,
     * используется для формирования названия файла
     *
     * @param strings - строки через запятую
     */
    protected String underscoreMaker(String... strings) {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            if (i < strings.length - 1) {
                path.append(strings[i]).append("_");
            } else {
                path.append(strings[i]);
            }
        }
        return path.toString();
    }

}
