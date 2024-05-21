package utils.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import utils.Links;
import utils.Projects;
import wfm.components.utils.Role;
import wfm.repository.CommonRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CsvLoader {
    private static final String FILE_NOT_FOUND = "Файл %s не был найден";
    private static final String WRONG_FORMAT = "Файл %s не содержит строк в нужном формате. Убедитесь, что для разделения параметров используется знак табуляции";
    private static final Logger LOG = LoggerFactory.getLogger(CsvLoader.class);

    private CsvLoader() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Читает файл, где находятся пары логина и пароля для различных пользователей
     *
     * @param project - проект WFM/BIO
     * @return список пар логина и пароля
     */
    private static List<String[]> newUserData(Projects project) throws IOException {
        String projectFile = project == Projects.BIO ? "Bio" : "WFM";
        String csvFile = "datainput/setUpDataLogin" + projectFile + ".csv";
        List<String[]> lines = new ArrayList<>();
        File file = new File(csvFile);
        if (!file.createNewFile()) {
            BufferedReader br = new BufferedReader(new FileReader(csvFile));
            List<String> linesInFile = br.lines().collect(Collectors.toList());
            for (String line : linesInFile) {
                String[] lineSplit = line.split("\t");
                if (CommonRepository.URL_BASE.contains(lineSplit[0])) {
                    String[] lineSplit2 = new String[]{"superuser", lineSplit[1]};
                    lines.add(lineSplit2);
                    break;
                }
            }
            br.close();
        } else {
            Assert.fail(String.format(FILE_NOT_FOUND, file.getCanonicalPath()));
        }
        if (lines.isEmpty()) {
            Assert.fail(String.format(WRONG_FORMAT, file.getCanonicalPath()));
        }
        return lines;
    }

    /**
     * Возвращает определенную пару логина и пароля в зависимости от role пользователя
     *
     * @param project - проект WFM/BIO
     * @param role    - Роль из енама
     * @return пара логин и пароль
     */
    public static String[] loginReturner(Projects project, Role role) {
        List<String[]> login = null;
        try {
            login = CsvLoader.newUserData(project);
        } catch (IOException e) {
            LOG.warn("Не удалось получить данные для входа. Проект {}", project);
        }
        return login != null ? login.get(Integer.parseInt(Links.getTestProperty(role.toString().toLowerCase())))
                : new String[0];
    }

    /**
     * Возвращает данные для работы с базой данных
     */
    public static String[] databaseCredentialsReturner() {
        String csvFile = "datainput/setUpDataLoginDataBase.csv";
        return credentialReturner(csvFile, "базой данных");
    }

    /**
     * Возвращает данные для логина интеграции
     */
    public static String[] integrationCredentialsReturner() {
        String csvFile = "datainput/integration/integrationLogin.csv";
        return credentialReturner(csvFile, "сервисом интеграции");
    }

    private static String[] credentialReturner(String csvFile, String errorMessage) {
        String[] credentials = null;
        File file = new File(csvFile);
        try {
            if (!file.createNewFile()) {
                BufferedReader br = new BufferedReader(new FileReader(csvFile));
                List<String> linesInFile = br.lines().collect(Collectors.toList());
                for (String line : linesInFile) {
                    credentials = line.split("\t");
                }
                br.close();
            } else {
                Assert.fail(String.format(FILE_NOT_FOUND, file.getCanonicalPath()));
            }
            if (credentials == null || credentials.length == 1) {
                Assert.fail(String.format(WRONG_FORMAT, file.getCanonicalPath()));
            }
        } catch (IOException e) {
            LOG.warn("Не удалось получить данные для соединения с {}. Убедитесь, что файл {} существует", errorMessage, csvFile);
        }
        return credentials;
    }

    public static List<Map<String, String>> integrationReturner() {
        String csvFile = "datainput/integration/integrationRelations.csv";
        String base = CommonRepository.URL_BASE;
        base = base.substring(base.indexOf("//") + 2).replace("/", "");
        List<Map<String, String>> result = new ArrayList<>();
        File file = new File(csvFile);
        try {
            if (!file.createNewFile()) {
                BufferedReader br = new BufferedReader(new FileReader(csvFile));
                List<String> linesInFile = br.lines().collect(Collectors.toList());
                String [] headers = linesInFile.get(0).split("\t");
                for (int i = 1; i < linesInFile.size(); i++) {
                    String[] add = linesInFile.get(i).split("\t");
                    if (add[0].contains(base)){
                        Map<String, String> mapToAdd = new HashMap<>();
                        mapToAdd.put(headers[0], add[0]);
                        mapToAdd.put(headers[1], add[1]);
                        mapToAdd.put(headers[2], add[2]);
                        mapToAdd.put(headers[3], add[3]);
                        result.add(mapToAdd);
                    }
                }
                br.close();
            } else {
                Assert.fail(String.format(FILE_NOT_FOUND, file.getCanonicalPath()));
            }
        } catch (IOException e) {
            LOG.warn("Не удалось получить данные для соединения с {}. Убедитесь, что файл {} существует", "errorMessage", csvFile);
        }
        return result;
    }

}
