package utils.authorization;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import utils.Projects;

import static utils.authorization.CookieRW.getBasicCookieWithCheck;
import static utils.authorization.CookieTools.settingsConnectionManager;

public class ClientReturners {

    private ClientReturners() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Клиент для выбранного проекта с учетом ролевой модели
     *
     * @param project - id проекта
     */
    public static HttpClient httpClientReturner(Projects project) {
        BasicClientCookie clientCookie = getBasicCookieWithCheck(project);
        BasicCookieStore cookieStore = new BasicCookieStore();
        cookieStore.addCookie(clientCookie);
        return HttpClients.custom()
                .setConnectionManager(settingsConnectionManager())
                .setDefaultCookieStore(cookieStore)
                .build();
    }
}
