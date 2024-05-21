package utils.dropTestTools;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.xml.XmlClass;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class DropLoginTestReadWriter {
    private static final String PATH = "datainput/";
    private static final String NAME = "brokenLoginTest";
    private static final String FORMAT = ".csv";
    private static final String STATUS_FAIL = "fail";

    private static final Logger LOG = LoggerFactory.getLogger(DropLoginTestReadWriter.class);

    private DropLoginTestReadWriter() {
        throw new IllegalStateException("Utility class");
    }

    public static String fileNameReturner() {
        return PATH + NAME + FORMAT;
    }

    /**
     * Читает файл, узнает упал ли логин тест
     *
     * @return возвращает если упал true если не упал false
     */
    public static boolean getStatusLoginTest() {
        String file = fileNameReturner();
        String line = "";
        String status = "";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while ((line = br.readLine()) != null) {
                status = line;
            }
        } catch (FileNotFoundException ex) {
            LOG.info("Файл со статусом логин теста отсутствует", ex);
        } catch (IOException ex) {
            LOG.error("При чтение файла {} произошла ошибка в строчке {}", file, line, ex);
        }
        return status.equals(STATUS_FAIL);
    }

    /**
     * Добавляет в файл запись о том что логин тест упал
     */
    public static void writeBrokenLoginTest() {
        File file = new File(fileNameReturner());
        try (FileWriter fileWriter = new FileWriter(file, true)) {
            fileWriter.write(STATUS_FAIL);
        } catch (IOException e) {
            LOG.info("Файл со статусом логин теста отсутствует", e);
        }
    }

    /**
     * Копирует логи
     *
     * @param classes - выбирает XML класс из доступных
     */
    public static void writeLogs(List<XmlClass> classes) {
        File original = new File("src/test/resources/logs/thread.log");
        File copied = new File(
                "src/test/resources/logs/" + classes.stream()
                        .map(xmlClass -> xmlClass.getSupportClass().getSimpleName()).collect(Collectors.joining("_"))
                        + DateTimeFormatter.ofPattern("dd-MM'T'HH-mm-ss")
                        .format(LocalDateTime.now().atZone(ZoneId.of("UTC+5"))) + ".log");
        try {
            FileUtils.copyFile(original, copied);
        } catch (IOException e) {
            LOG.error("Не удалось скопировать логи, файл не появился");
        }
    }

}
