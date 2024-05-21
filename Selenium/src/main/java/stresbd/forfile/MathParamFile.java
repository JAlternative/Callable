package stresbd.forfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stresbd.SuperMain;
import stresbd.models.MathParam;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class MathParamFile {

    private static final String HEADER = "action,entityOuterId,entityType,mathParameterOuterId,value";
    private static final String EXAMPLE = "CREATE,RO,ORGANIZATION_UNIT,KpiValueDeviationThreshold,50";
    private static final Logger LOG = LoggerFactory.getLogger(MathParamFile.class);

    public static void main(String[] args) {
        MathParam a = new MathParam(EXAMPLE);
        File file = SuperMain.fileToSaveCsv(SuperMain.SAVE_PATH, MathParamFile.class.getName());
        File omFileName = SuperMain.getFile(OUfile.class);
        File mathParamFileName = SuperMain.getFile(MathParamParser.class);
        try (FileWriter fileWrite = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWrite)) {
            bufferedWriter.write(HEADER);
            bufferedWriter.newLine();
            List<String> omList = Files.readAllLines(omFileName.toPath(), StandardCharsets.UTF_8);
            List<String> mathList = Files.readAllLines(mathParamFileName.toPath(), StandardCharsets.UTF_8);
            for (int i = 1; i < omList.size(); i++) {
                String omOuterId = omList.get(i).split(",")[10];
                for (int g = 0; g < mathList.size(); g++) {
                    String key = mathList.get(g).split(",")[0];
                    String value = "";
                    try {
                        value = mathList.get(g).split(",")[1];
                    } catch (java.lang.ArrayIndexOutOfBoundsException ex) {
                        break;
                    }
                    bufferedWriter.write(a.getAction());
                    bufferedWriter.write(",");
                    bufferedWriter.write(omOuterId);
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getEntityType());
                    bufferedWriter.write(",");
                    bufferedWriter.write(key);
                    bufferedWriter.write(",");
                    bufferedWriter.write(value);
                    bufferedWriter.write(",");
                    bufferedWriter.newLine();
                }
            }
        } catch (Exception ex) {
            LOG.info("Матпараметры не сформированы", ex);
        }
    }


}


