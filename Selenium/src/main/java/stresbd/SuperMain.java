package stresbd;

import org.testng.Assert;
import stresbd.forfile.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SuperMain {

    public static final String SAVE_PATH = "db/";

    public static void main(String[] args) {
        OUfile.main(new String[]{""});
        BusinessHoursFile.main(new String[]{""});
        EmployeesFile.main(new String[]{""});
        PositionsFile.main(new String[]{""});
        MathParamFile.main(new String[]{""});
    }

    public static File fileToSaveCsv(String pathForSave, String className) {
        return new File(pathForSave + className + "_" + timeForSave() + ".csv");
    }

    private static String timeForSave() {
        long current = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh-mm-ss-SS");
        Date date = new Date(current);
        return simpleDateFormat.format(date);
    }

    public static File getFile(Class className) {
        File folder = new File(SAVE_PATH);
        File[] listOfFiles = folder.listFiles();
        File omFileName = new File("");

        for (File fileName : listOfFiles != null ? listOfFiles : new File[0]) {
            Assert.assertTrue(fileName.exists());
            if (fileName.getName().contains(className.getName())) {
                omFileName = fileName;
            }
        }
        return omFileName;
    }
}
