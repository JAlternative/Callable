package stresbd.forfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stresbd.SuperMain;
import stresbd.models.Employees;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class EmployeesFile {

    private static final String EXAMPLE = "CREATE,true,2012-09-19,email.emp@unit.ru,Иван,MALE,234-234-234-33,Иванов,Aemp1,Иванович,23423433";
    private static final String HEADER = "action,active,birthday,email,firstName,gender,inn,lastName,outerId,patronymicName,snils";
    private static final Logger LOG = LoggerFactory.getLogger(EmployeesFile.class);
    private static final int MIDDLE = 51;

    public static void main(String[] args) {
        Employees a = new Employees(EXAMPLE);
        File file = SuperMain.fileToSaveCsv(SuperMain.SAVE_PATH, EmployeesFile.class.getName());
        File omFileName = SuperMain.getFile(OUfile.class);
        try (FileWriter fileWrite = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWrite)) {
            bufferedWriter.write(HEADER);
            bufferedWriter.newLine();
            List<String> omList = Files.readAllLines(omFileName.toPath(), StandardCharsets.UTF_8);
            for (int i = 1; i < omList.size(); i++) {
                String omOuterId = omList.get(i).split(",")[10];
                for (int g = 0; g < MIDDLE; g++) {
                    String empUniqueOuterId = omOuterId + a.getOuterId() + g;
                    bufferedWriter.write(a.getAction());
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getActive());
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getBirthday());
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getEmail());
                    bufferedWriter.write(",");
                    bufferedWriter.write(empUniqueOuterId);
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getGender());
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getInn());
                    bufferedWriter.write(",");
                    bufferedWriter.write(empUniqueOuterId);
                    bufferedWriter.write(",");
                    bufferedWriter.write(empUniqueOuterId);
                    bufferedWriter.write(",");
                    bufferedWriter.write(empUniqueOuterId);
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getSnils());
                    bufferedWriter.write(",");
                    bufferedWriter.newLine();
                }
            }
        } catch (Exception ex) {
            LOG.info("Сотрудники не были сформированы", ex);
        }
    }

}
