package utils.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class LocaleKeys {

    protected static Properties properties;
    private static final Logger LOG = LoggerFactory.getLogger(LocaleKeys.class);

    private LocaleKeys() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Возвращение свойства определенного ключа
     *
     * @return повторно возвращает строку
     */
    private static Properties propertiesReturner() {
        try (FileInputStream fileInput = new FileInputStream("src/main/resources/localekeys.properties")) {
            properties = new Properties();
            properties.load(fileInput);
        } catch (IOException e) {
            LOG.error("Exception message", e);
        }
        return properties;
    }

    /**
     * Проверка свойства
     *
     * @param key - ключ
     * @return возвращает значение по ключу
     */
    public static String getAssertProperty(String key) {
        return propertiesReturner().getProperty(key);
    }

}
