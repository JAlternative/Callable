package stresbd.forfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stresbd.SuperMain;
import stresbd.models.BusinessHours;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class BusinessHoursFile {

    private static final String EXAMPLE = "CREATE,2010-05-28,2023-02-05,22:00,10:00,22:00,10:00,22:00,10:00,22:00,10:00,22:00,10:00,22:00,10:00,22:00,10:00,unit1";
    private static final String HEADER = "action,dateInterval.endDate,dateInterval.startDate,day1TimeInterval.endTime,day1TimeInterval.startTime,day2TimeInterval.endTime,day2TimeInterval.startTime,day3TimeInterval.endTime,day3TimeInterval.startTime,day4TimeInterval.endTime,day4TimeInterval.startTime,day5TimeInterval.endTime,day5TimeInterval.startTime,day6TimeInterval.endTime,day6TimeInterval.startTime,day7TimeInterval.endTime,day7TimeInterval.startTime,organizationUnitOuterId";
    private static final Logger LOG = LoggerFactory.getLogger(BusinessHoursFile.class);

    public static void main(String[] args) {
        BusinessHours a = new BusinessHours(EXAMPLE);
        File file = SuperMain.fileToSaveCsv(SuperMain.SAVE_PATH, BusinessHoursFile.class.getName());
        File omFileName = SuperMain.getFile(OUfile.class);
        try (FileWriter fileWrite = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWrite)) {
            bufferedWriter.write(HEADER);
            bufferedWriter.newLine();
            List<String> omList = Files.readAllLines(omFileName.toPath(), StandardCharsets.UTF_8);
            for (int i = 1; i < omList.size(); i++) {
                String omOuterId = omList.get(i).split(",")[10];
                bufferedWriter.write(a.getAction());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getStartDate());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getEndDate());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay1TimeIntervalEndTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay1TimeIntervalStartTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay2TimeIntervalEndTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay2TimeIntervalStartTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay3TimeIntervalEndTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay3TimeIntervalStartTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay4TimeIntervalEndTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay4TimeIntervalStartTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay5TimeIntervalEndTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay5TimeIntervalStartTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay6TimeIntervalEndTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay6TimeIntervalStartTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay7TimeIntervalEndTime());
                bufferedWriter.write(",");
                bufferedWriter.write(a.getDay7TimeIntervalStartTime());
                bufferedWriter.write(",");
                bufferedWriter.write(omOuterId);
                bufferedWriter.write(",");
                bufferedWriter.newLine();
            }

        } catch (Exception ex) {
            LOG.info("График работы не сформирован", ex);
        }
    }
}
