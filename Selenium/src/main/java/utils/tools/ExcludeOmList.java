package utils.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Инструментарий для создания заповедных оргюнитов которые не будут трогать и ломать тесты которые сильно меняют структуру
 * или настройки оргюнитов
 */
public class ExcludeOmList {

    /**
     * Берет строки из файла
     *
     * @param fileName - название файлика
     */
    private static List<String> getLinesOms(String fileName) {
        List<String> lines = null;
        {
            try {
                String pathToFile = "src/main/resources/";
                lines = Files.readAllLines(new File(pathToFile + fileName).toPath(),
                        Charset.defaultCharset());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return lines;
    }

    /**
     * Преобразует строки из файлов в названия оргюнитов и возвращает в итоге список названий оргюнитов для исключения
     */
    public static List<String> getExcludeOMs() {
        String fileSchedule = "scheduleOms.csv";
        List<String> allLines = getLinesOms(fileSchedule);
        String fileAnalytics = "analyticsOms.csv";
        allLines.addAll(getLinesOms(fileAnalytics));
        return allLines.stream()
                .filter(line -> line.contains("\t"))
                .map(line -> line.substring(line.indexOf("\t") + 1))
                .distinct()
                .collect(Collectors.toList());
    }
}
