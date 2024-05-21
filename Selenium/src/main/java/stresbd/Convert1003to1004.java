package stresbd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Convert1003to1004 {

    private static final String UTF8_BOM = "\uFEFF";
    private static final String HEADER = "organizationUnit,day,month,year,hour,value";
    private static final Logger LOG = LoggerFactory.getLogger(Convert1003to1004.class);

    static String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }

    public static void main(String[] args) {
        File file = new File("C:\\_backup\\sportmaster\\normfile\\hyper0__1004.csv");
        File fileFor1004 = new File("db/hyper0__1004.csv");
        try (FileWriter fileWrite = new FileWriter(fileFor1004);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWrite)) {
            List<String> allLines = Files.readAllLines(Paths.get(file.getPath()), StandardCharsets.UTF_8);
            StringBuilder stringBuilder = new StringBuilder();
            bufferedWriter.write(HEADER);
            bufferedWriter.newLine();
            for (int i = 1; i < allLines.size(); i++) {
                String[] splitterLine = allLines.get(i).split(",");
                String organizationUnit = splitterLine[0];
                String day = splitterLine[1];
                String month = splitterLine[2];
                String year = splitterLine[3];
                String hour = splitterLine[4];
                String value = splitterLine[5];
                Double val = Double.valueOf(value);
                stringBuilder.append(organizationUnit).append(",")
                        .append(day).append(",")
                        .append(month).append(",")
                        .append(year).append(",")
                        .append(hour).append(",")
                        .append((double) ((int) (val / 5)))
                        .append("\n");
            }
            removeUTF8BOM(stringBuilder.toString());
            bufferedWriter.write(stringBuilder.toString());
            bufferedWriter.newLine();

        } catch (IOException e) {
            LOG.info("Не смогли конвертировать файл", e);
        }
    }
}
