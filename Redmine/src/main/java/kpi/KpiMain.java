package kpi;

import com.taskadapter.redmineapi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ApiCaller;
import utils.Utils;

import java.io.IOException;
import java.util.*;

public class KpiMain {
    private static final Logger LOG = LoggerFactory.getLogger(KpiMain.class);

    public static void main(String[] args) {
        final String uri = "https://task.goodt.tech/";
        final String login = args[0];
        LOG.info("Логин {}", login);
        final String password = args[1];
        LOG.info("Пароль {}", password);
        final String dateFrom = args[2];
        LOG.info("От {}", dateFrom);
        final String dateTo = args[3];
        LOG.info("До {}", dateTo);
        final String testersStr = args[4];
        LOG.info("Тестировщики {}", testersStr);
        RedmineManager mgr = RedmineManagerFactory.createWithUserAuth(uri, login, password);

        List<String> testers = Arrays.asList(testersStr.split(";"));
        ApiCaller apiCaller = new ApiCaller(mgr, dateFrom, dateTo, testers);

        String outputFileNameOther = String.format("kpi_%s_%s_%s", "Other", dateFrom, dateTo);
        String outputFileNameBugs = String.format("kpi_%s_%s_%s", "Bugs", dateFrom, dateTo);
        String outputFileNameTasks = String.format("kpi_%s_%s_%s", "3", dateFrom, dateTo);
        String outputFileNameByQaAndNotQa = String.format("kpi_%s_%s_%s", "1_2", dateFrom, dateTo);

        try {
            Utils.writeToFile(outputFileNameOther, apiCaller.reportOther());
            Utils.writeToFile(outputFileNameByQaAndNotQa, apiCaller.reportBugsByQa() + apiCaller.reportBugsByNotQa());
            Utils.writeToFile(outputFileNameTasks, apiCaller.reportTasks(true) + apiCaller.reportTasks(false));
            Utils.writeToFile(outputFileNameBugs, apiCaller.reportBugsWithHours());
        } catch (IOException e) {
            LOG.info("Не удалось создать файл с отчетом");
            e.printStackTrace();
        } catch (RedmineException e) {
            LOG.info("Не удалось выгрузить данные из Redmine");
            e.printStackTrace();

        }
    }

}
