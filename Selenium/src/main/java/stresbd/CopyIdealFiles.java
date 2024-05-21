package stresbd;

import org.apache.uima.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stresbd.forfile.OUfile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CopyIdealFiles {

    private static final String IDEAL_PATH = "db/ideal/";
    private static final String TO_SAVE = "db/generation/";
    private static final Logger LOG = LoggerFactory.getLogger(CopyIdealFiles.class);

    public static void main(String[] args) {
        File omFileName = SuperMain.getFile(OUfile.class);
        ArrayList<String> uniqueOmList = new ArrayList<>();
        try {
            List<String> omList = Files.readAllLines(omFileName.toPath(), StandardCharsets.UTF_8);
            for (int i = 1; i < omList.size(); i++) {
                String omOuterId = omList.get(i).split(",")[10];
                uniqueOmList.add(omOuterId);
            }
        } catch (IOException e) {
            LOG.info("Не смогли прочитать строчку из файла", e);
        }
        File idealFolder = new File(IDEAL_PATH);
        File[] idealFiles = idealFolder.listFiles();

        for (File oneIdeal : idealFiles != null ? idealFiles : new File[0]) {
            for (String oneOm : uniqueOmList) {
                try {
                    FileUtils.copyFile(oneIdeal, new File(TO_SAVE));
                    File copiedFile = searchByName(oneIdeal.getName());
                    if (copiedFile != null) {
                        String tempType = copiedFile.getName().split("__")[1].split("\\.")[0];
                        if (copiedFile.renameTo(new File(TO_SAVE + oneOm + "__" + tempType + ".csv"))) {
                            break;
                        }
                    }

                } catch (IOException e) {
                    LOG.info("Не смогли обработать файл", e);
                }
            }
        }
        fileRename(TO_SAVE);
    }

    private static void fileRename(String path) {
        File[] idealFiles = new File(IDEAL_PATH).listFiles();
        String tempName = (idealFiles != null ? idealFiles[0].getName().split("__") : new String[0])[0];
        File folderWithFiles = new File(path);
        File[] files = folderWithFiles.listFiles();
        for (File one : files != null ? files : new File[0]) {
            StringBuilder stringBuilder = new StringBuilder();
            try (FileWriter fileWrite = new FileWriter(one);
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWrite)) {
                List<String> allLines = Files.readAllLines(one.toPath(), StandardCharsets.UTF_8);
                String header = allLines.get(0);
                stringBuilder.append(header).append("\n");
                for (int i = 1; i < allLines.size(); i++) {
                    String tempString = allLines.get(i).replaceAll(tempName, one.getName().split("__")[0]);
                    stringBuilder.append(tempString).append("\n");
                }
                String some = Convert1003to1004.removeUTF8BOM(stringBuilder.toString());
                bufferedWriter.write(some);
            } catch (IOException e) {
                LOG.info("Не смогли переименовать файл", e);
            }
        }
    }

    private static File searchByName(String fileName) {
        File saveFolder = new File(TO_SAVE);
        File[] saveFiles = saveFolder.listFiles();
        for (File one : saveFiles != null ? saveFiles : new File[0]) {
            if (one.getName().equals(fileName)) {
                return one;
            }
        }
        return null;
    }
}
