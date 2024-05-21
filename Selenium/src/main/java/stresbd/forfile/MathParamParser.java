package stresbd.forfile;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class MathParamParser {

    private static final Logger LOG = LoggerFactory.getLogger(MathParamParser.class);

    public static void main(String[] args) {
        File optionsFile = new File("db/options.json");
        File file = new File("db/" + MathParamParser.class.getName() + ".csv");
        try (FileWriter fileWrite = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWrite)) {
            List<String> allLines = Files.readAllLines(optionsFile.toPath(), StandardCharsets.UTF_8);
            //Уникальный числа связаны с расположением параметров в файле
            for (int i = 5; i < 123; i++) {
                String[] lineSep = allLines.get(i).split(":");
                String key = lineSep[0].replaceAll("\"", "").trim();
                String value = lineSep[1].replaceAll("\"", "").replaceAll(",", "").trim();
                bufferedWriter.write(key + "," + value);
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            LOG.info("Матпараметры не была распаршены", e);
        }
    }

}
