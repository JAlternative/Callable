package stresbd.forfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stresbd.SuperMain;
import stresbd.models.Positions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class PositionsFile {

    private static final Logger LOG = LoggerFactory.getLogger(PositionsFile.class);
    private static final String EX_PROD = "CREATE,or0e2,Продавец,or0,or0p2,seller,seller,Продавец,2010-09-27";
    private static final String EX_ADM = "CREATE,or0e2,Старший продавец (Администратор магазина),or0,or0p2,seller,seller,Старший продавец (Администратор магазина),2010-09-27";
    private static final String HEADER = "action,employeeOuterId,name,organizationUnitOuterId,outerId,positionCategoryOuterId,positionGroupName,positionTypeOuterId,startDate";
    private static final int MIDDLE = 51;

    public static void main(String[] args) {
        Positions a = new Positions(EX_PROD);
        File file = SuperMain.fileToSaveCsv(SuperMain.SAVE_PATH, PositionsFile.class.getName());
        File omFileName = SuperMain.getFile(OUfile.class);
        File empFileName = SuperMain.getFile(EmployeesFile.class);
        try (FileWriter fileWrite = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWrite)) {
            bufferedWriter.write(HEADER);
            bufferedWriter.newLine();
            List<String> omList = Files.readAllLines(omFileName.toPath(), StandardCharsets.UTF_8);
            List<String> empList = Files.readAllLines(empFileName.toPath(), StandardCharsets.UTF_8);
            for (int i = 1; i < omList.size(); i++) {
                String omOuterId = omList.get(i).split(",")[10];
                for (int g = 1; g < empList.size(); g++) {
                    String empOuterId = empList.get(g).split(",")[8];
                    bufferedWriter.write(a.getAction());
                    bufferedWriter.write(",");
                    bufferedWriter.write(empOuterId);
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getName());
                    bufferedWriter.write(",");
                    bufferedWriter.write(omOuterId);
                    bufferedWriter.write(",");
                    bufferedWriter.write(empOuterId);
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getPositionCategoryOuterId());
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getPositionGroupName());
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getPositionTypeOuterId());
                    bufferedWriter.write(",");
                    bufferedWriter.write(a.getStartDate());
                    bufferedWriter.write(",");
                    bufferedWriter.newLine();
                }
            }
        } catch (Exception ex) {
            LOG.info("Позиции для сотрудников не были сформированы", ex);
        }
    }

}
