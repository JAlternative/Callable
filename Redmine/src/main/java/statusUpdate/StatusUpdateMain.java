package statusUpdate;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ApiCaller;
import utils.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class StatusUpdateMain {
    private static final Logger LOG = LoggerFactory.getLogger(StatusUpdateMain.class);

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
        String outputFileName = String.format("statusUpdate_%s_%s", dateFrom, dateTo);
        try {
            Utils.writeToFile(outputFileName, apiCaller.reportStatusUpdate());
        } catch (IOException e) {
            LOG.info("Не удалось создать файл с отчетом");
            e.printStackTrace();
        } catch (RedmineException e) {
            LOG.info("Не удалось выгрузить данные из Redmine");
            e.printStackTrace();

        }
    }
}
