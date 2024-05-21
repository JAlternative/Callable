package utils;

import com.taskadapter.redmineapi.bean.IssueStatus;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import static utils.Names.*;

public class Utils {
    public static void writeToFile(String outputFileName, String reportStr) throws IOException {
        File directory = new File("output/");
        if (!directory.exists()) {
            directory.mkdir();
        }
        File outputFile = new File(directory + "/" + outputFileName + ".csv");
        outputFile.createNewFile();
        PrintWriter pw = new PrintWriter(outputFile, "UTF-8");
        pw.println(reportStr);
        pw.close();
    }

    public static Date parseDate(String date) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String convertDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String assembleHeaders(String kpiType) {
        if (kpiType.equals("1_2")) {
            return ISSUE_ID + "," + LINK + "," + AUTHOR + "," + ASSIGNED_TO + "," + CREATED_ON + "," + PRIORITY + "," + STATUS + "," + SEVERITY + "\n";
        }
        if (kpiType.equals("3")) {
            return ISSUE_ID + "," + CF_171 + "," + LINK + "," + AUTHOR + "," + ASSIGNED_TO + "," + CREATED_ON + "," + CLOSED_ON + "," + PRIORITY + "," + STATUS + "," + SPENT_HOURS + "," + PROJECT + "\n";
        }
        if (kpiType.equals("Bugs")) {
            return ISSUE_ID + "," + LINK + "," + SPENT_HOURS + "," + AUTHOR + "," + ASSIGNED_TO + "," + CREATED_ON + "," + PRIORITY + "," + STATUS + "," + SUBJECT + "\n";
        }
        if (kpiType.equals("Other")) {
            return ISSUE_ID + "," + SUBJECT + "," + TESTER_HOURS + "," + SPENT_HOURS + "," + PROJECT + "\n";
        }
        if (kpiType.equals("StatusUpdate")) {
            return ISSUE_ID + ";" +
                    SUBJECT + ";" +
                    LINK + ";" +
                    ASSIGNED_TO + ";" +
                    "Дата изменения" + ";" +
                    "Автор изменения" + ";" +
                    "Предыдущий статус" + ";" +
                    "Новый статус" + ";" +
                    CF_171 + ";" +
                    STATUS + ";" +
                    SPENT_HOURS + ";" +
                    SEVERITY + ";" +
                    PROJECT + "\n";
        }
        return null;
    }
    public static String getStatusNameById(List<IssueStatus> issueStatuses, String id){
        return issueStatuses.stream().filter(issueStatus -> String.valueOf(issueStatus.getId()).equals(id)).findAny().orElseThrow(NoSuchElementException::new).getName();
    }
}
