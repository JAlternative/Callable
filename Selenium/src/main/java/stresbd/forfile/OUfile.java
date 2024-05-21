package stresbd.forfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stresbd.SuperMain;
import stresbd.models.OrganizationUnits;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class OUfile {

    private static final Logger LOG = LoggerFactory.getLogger(OUfile.class);
    private static final String EXAMPLE = "CREATE,true,true,,2013-07-04,2070-11-02,,,aq,4.1,aq,str2";
    private static final String HEADER = "action,active,availableForCalculation,chiefPositionOuterId,dateFrom,dateTo,email,fax,name,organizationUnitTypeOuterId,outerId,parentOuterId";

    public static void main(String[] args) {
        OrganizationUnits a = new OrganizationUnits(EXAMPLE);
        File file = SuperMain.fileToSaveCsv(SuperMain.SAVE_PATH, OUfile.class.getName());
        int someNumber = 4;
        try (FileWriter fileWrite = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWrite)) {
            bufferedWriter.write(HEADER);
            bufferedWriter.newLine();
            for (int i = 0; i < someNumber; i++) {
                bufferedWriter.write(a.isAction());
                bufferedWriter.write(",");
                bufferedWriter.write(a.isActive());
                bufferedWriter.write(",");
                bufferedWriter.write(a.isAvailableForCalculation());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getChiefPositionOuterId());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDateFrom());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDateTo());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getEmail());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getFax());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getName() + "" + i);
                bufferedWriter.write(",");
                bufferedWriter.write(a.getOrganizationUnitTypeOuterId());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getOuterId() + "" + i);
                bufferedWriter.write(",");
                bufferedWriter.write(a.getParentOuterId());
                bufferedWriter.write(",");
                bufferedWriter.newLine();
            }
        } catch (Exception ex) {
            LOG.info("Оргюниты не были сформированы", ex);
        }
    }

}
