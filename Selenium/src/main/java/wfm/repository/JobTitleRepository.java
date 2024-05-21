package wfm.repository;

import org.json.JSONObject;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.RequestFormers;
import wfm.models.JobTitle;

import java.net.URI;
import java.util.List;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.JOB_TITLES;
import static utils.tools.CustomTools.getRandomFromList;

public class JobTitleRepository {

    private JobTitleRepository() {}

    public static JobTitle randomJobTitle() {
        List<JobTitle> tempList = getAllJobTitles();
        return getRandomFromList(tempList);
    }

    public static List<JobTitle> getAllJobTitles() {
        URI uri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, JOB_TITLES);
        JSONObject object = RequestFormers.getJsonFromUri(Projects.WFM, uri);
        return CustomTools.getListFromJsonObject(object, JobTitle.class);
    }

    public static JobTitle getJob(String jobName) {
        List<JobTitle> allJobs = getAllJobTitles();
        return allJobs.stream().filter(job -> job.getFullName().equals(jobName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "Не удалось найти должность с именем: " + jobName));
    }

}
