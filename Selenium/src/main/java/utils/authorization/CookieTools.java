package utils.authorization;

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * @author Evgeny Gurkin 20.08.2020
 */
public class CookieTools {

    private static final Logger LOG = LoggerFactory.getLogger(CookieTools.class);

    /**
     * Метод для игнорирования SSL верификации при получении HttpClient
     */
    public static SSLContext getContextIgnoreSSL() {
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            LOG.info("Да быть такого не может", e);
        }
        return sslContext;
    }

    /**
     * Обновляет дату куов
     *
     * @param file     - файл с куками
     * @param dateTime - дата на которую обновляются куки
     */
    public static void updateCookieDateTime(File file, LocalDateTime dateTime) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String strLine;
            while ((strLine = br.readLine()) != null) {
                sb.append(strLine.replace(strLine.substring(strLine.lastIndexOf(";") + 1),
                        dateTime.toString()));
            }
        } catch (IOException e) {
            LOG.warn("Не удалось прочитать содержимое файла {}", file.getName());
        }
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(sb.toString());
        } catch (IOException e) {
            LOG.warn("Не удалось обновить время в файле {} cookies", file.getName());
        }
    }

    /**
     * Настройки settingsConnectionManager для  HttpClient
     */
    public static PoolingHttpClientConnectionManager settingsConnectionManager() {
        return new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                        .register("https", new SSLConnectionSocketFactory(getContextIgnoreSSL(),
                                NoopHostnameVerifier.INSTANCE))
                        .build()
        );
    }
}
