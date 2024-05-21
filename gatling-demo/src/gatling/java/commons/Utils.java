package commons;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class Utils {
    private Utils() {
    }

    /**
     * Возвращение строки из конфига
     *
     * @return повторно возвращает строку
     */
    public static String propertyReturner(String key) {
        Properties properties = new Properties();
        try (FileInputStream fileInput = new FileInputStream("src/gatling/resources/simulation.properties")) {
            properties.load(fileInput);
        } catch (IOException e) {
            System.out.println(e.getStackTrace());
        }
        return properties.getProperty(key);
    }

    public static String makePath(Object... parts) {
        String delimiter = "/";
        StringBuilder path = new StringBuilder();
        path.append(delimiter);
        for (int i = 0; i < parts.length; i++) {
            if (i < parts.length - 1) {
                path.append(parts[i]).append(delimiter);
            } else {
                path.append(parts[i]);
            }
        }
        return path.toString();
    }

    static String replacePlaceholders(String url) {
        return url.replaceAll("\\#\\{.*\\}", "_");
    }
}
