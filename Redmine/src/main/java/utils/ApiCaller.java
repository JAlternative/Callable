package utils;

import com.taskadapter.redmineapi.*;
import com.taskadapter.redmineapi.bean.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static utils.Utils.getStatusNameById;

public class ApiCaller {
    private static final Logger LOG = LoggerFactory.getLogger(ApiCaller.class);
    private IssueManager issueManager;
    private TimeEntryManager timeEntryManager;
    private String dateFrom;
    private String dateTo;
    private List<String> testers;
    private String testersParam;
    private final String link = "https://task.goodt.tech/issues/";
    private final List<Integer> otherTasks = new ArrayList<>(Arrays.asList(71700, 118486));
    private final List<String> projects = new ArrayList<>(Arrays.asList("122", "127", "137", "140"));

    public ApiCaller(RedmineManager redmineManager, String dateFrom, String dateTo, List<String> testers) {
        this.issueManager = redmineManager.getIssueManager();
        this.timeEntryManager = redmineManager.getTimeEntryManager();
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.testers = testers;
        testersParam = testers.stream().collect(Collectors.joining("|"));
    }

    public String reportBugsByQa() throws RedmineException {
        StringBuilder bf = new StringBuilder();
        bf.append(Utils.assembleHeaders("1_2"));
        for (String tester : testers) {
            Map<String, String> params = new HashMap<>();
            params.put("project_id", "137");
            params.put("limit", "100");
            params.put("author_id", tester);
            params.put("status_id", "*");
            params.put("tracker_id", "38");
            params.put("created_on", "><" + dateFrom + "|" + dateTo);
            params.put("sort", "created_on:desc,id:desc,author");
            List<Issue> issues = getIssuesAllPages(issueManager, params);
            for (Issue issue : issues) {
                bf.append(issue.getId() + ","
                                  + link + issue.getId() + ","
                                  + issue.getAuthorName() + ","
                                  + issue.getAssigneeName() + ","
                                  + Utils.convertDate(issue.getCreatedOn()) + ","
                                  + issue.getPriorityText() + ","
                                  + issue.getStatusName() + ","
                                  + issue.getCustomFieldByName("Severity").getValue() + "\n");
            }
        }
        return bf.append("\n").toString();
    }

    public String reportBugsByNotQa() throws RedmineException {
        Map<String, String> params = new HashMap<>();
        params.put("project_id", "137");
        params.put("limit", "100");
        params.put("author_id", "!" + testersParam);
        params.put("status_id", "*");
        params.put("tracker_id", "38");
        params.put("created_on", "><" + dateFrom + "|" + dateTo);
        params.put("sort", "created_on:desc,id:desc,author");
        List<Issue> issues = getIssuesAllPages(issueManager, params);
        StringBuilder bf = new StringBuilder();
        for (Issue issue : issues) {
            bf.append(issue.getId() + ","
                              + link + issue.getId() + ","
                              + issue.getAuthorName() + ","
                              + issue.getAssigneeName() + ","
                              + Utils.convertDate(issue.getCreatedOn()) + ","
                              + issue.getPriorityText() + ","
                              + issue.getStatusName() + ","
                              + issue.getCustomFieldByName("Severity").getValue() + "\n");
        }

        return bf.toString();
    }

    public String reportTasks(boolean byQa) throws RedmineException {
        StringBuilder bf = new StringBuilder();
        bf.append(Utils.assembleHeaders("3"));
        for (String projectId : projects) {
            Map<String, String> params = new HashMap<>();
            String performers;
            if (byQa) {
                performers = testersParam;
            } else {
                performers = "!" + testersParam;
            }
            params.put("project_id", projectId);//projectsParam
            params.put("limit", "100");
            params.put("assigned_to_id", performers);
            params.put("status_id", "57");
            params.put("tracker_id", "!38");
            params.put("closed_on", "><" + dateFrom + "|" + dateTo);
            params.put("sort", "author,id:desc");
            List<Issue> issues = getIssuesAllPages(issueManager, params);
            for (Issue issue : issues) {
                Float total = (float) timeEntryManager.getTimeEntriesForIssue(issue.getId()).stream()
                        .filter(entry -> !entry.getUpdatedOn().before(Utils.parseDate(dateFrom)) && !entry.getUpdatedOn().after(Utils.parseDate(dateTo)))
                        .filter(entry -> entry.getUserId().equals(issue.getAssigneeId()))
                        .map(entry -> entry.getHours())
                        .mapToDouble(Float::floatValue).sum();
                bf.append(issue.getId() + ","
                                  + issue.getCustomFieldById(171).getValue() + ","
                                  + link + issue.getId() + ","
                                  + issue.getAuthorName() + ","
                                  + issue.getAssigneeName() + ","
                                  + Utils.convertDate(issue.getCreatedOn()) + ","
                                  + Utils.convertDate(issue.getClosedOn()) + ","
                                  + issue.getPriorityText() + ","
                                  + issue.getStatusName() + ","
                                  + total + ","
                                  + issue.getProjectName() + "\n");
            }
        }
        bf.append("\n");
        return bf.toString();
    }

    public String reportBugsWithHours() throws RedmineException {
        StringBuilder bf = new StringBuilder();
        bf.append(Utils.assembleHeaders("Bugs"));
        Map<String, String> params = new HashMap<>();
        params.put("project_id", "137");
        params.put("limit", "100");
        params.put("tracker_id", "38");
        params.put("status_id", "*");
        List<Issue> issues = getIssuesAllPages(issueManager, params);
        for (Issue issue : issues) {
            List<TimeEntry> timeEntryList = timeEntryManager.getTimeEntriesForIssue(issue.getId());
            Map<String, Float> timeEntries = timeEntryList.stream()
                    .filter(entry -> testers.contains(entry.getUserId().toString()))
                    .filter(entry -> !entry.getSpentOn().before(Utils.parseDate(dateFrom)) && !entry.getSpentOn().after(Utils.parseDate(dateTo)))
                    .collect(Collectors.groupingBy(
                            TimeEntry::getUserName,
                            Collectors.reducing(
                                    Float.valueOf("0.0"),
                                    TimeEntry::getHours,
                                    Float::sum)));
            for (Map.Entry<String, Float> entry : timeEntries.entrySet()) {
                bf.append(issue.getId() + ","
                                  + link + issue.getId() + ","
                                  + entry.getValue() + ","
                                  + issue.getAuthorName() + ","
                                  + entry.getKey() + ","
                                  + Utils.convertDate(issue.getCreatedOn()) + ","
                                  + issue.getPriorityText() + ","
                                  + issue.getStatusName() + ","
                                  + issue.getSubject().replace(",", "") + "\n");
            }

        }

        return bf.toString();
    }

    public String reportOther() throws RedmineException {
        StringBuilder bf = new StringBuilder();
        bf.append(Utils.assembleHeaders("Other"));
        Map<String, String> params = new HashMap<>();
        params.put("set_filter", "1");
        params.put("f[]", "subject");
        params.put("op[subject]", "~");
        params.put("v[subject][]", "Поддержка и формирование ПТ");
        params.put("project_id", "141");
        params.put("status_id", "*");
        params.put("limit", "100");
        List<Integer> issues = getIssuesAllPages(issueManager, params)
                .stream()
                .filter(issue -> issue.getSubject().contains("Поддержка и формирование ПТ"))
                .map(issue -> issue.getId())
                .collect(Collectors.toList());
        otherTasks.addAll(issues);
        for (Integer issueId : otherTasks) {
            Issue issue = issueManager.getIssueById(issueId);
            List<TimeEntry> timeEntryList = timeEntryManager.getTimeEntriesForIssue(issueId);
            Map<String, Float> timeEntries = timeEntryList.stream()
                    .filter(entry -> testers.contains(entry.getUserId().toString()))
                    .filter(entry -> !entry.getSpentOn().before(Utils.parseDate(dateFrom)) && !entry.getSpentOn().after(Utils.parseDate(dateTo)))
                    .collect(Collectors.groupingBy(
                            TimeEntry::getUserName,
                            Collectors.reducing(
                                    Float.valueOf("0.0"),
                                    TimeEntry::getHours,
                                    Float::sum)));
            for (Map.Entry<String, Float> entry : timeEntries.entrySet()) {
                bf.append(issueId + ","
                                  + issue.getSubject() + ","
                                  + entry.getKey() + ","
                                  + entry.getValue() + ","
                                  + issue.getProjectName() + "\n");
            }
        }
        return bf.toString();
    }

    public List<Issue> getIssuesAllPages(IssueManager manager, Map<String, String> params) throws RedmineException {
        params.put("limit", "100");
        final List<Issue> result = new ArrayList<>();
        for (; ; ) {
            List<Issue> found = manager.getIssues(params).getResults();
            if (found.isEmpty()) {
                return result;
            }
            result.addAll(found);
            params.put("offset", String.valueOf(result.size()));
        }
    }

    public String reportStatusUpdate() throws RedmineException {
        List<IssueStatus> issueStatuses = issueManager.getStatuses();
        projects.add("141");
        StringBuilder bf = new StringBuilder();
        bf.append(Utils.assembleHeaders("StatusUpdate"));
        for (String projectId : projects) {
            Map<String, String> params = new HashMap<>();
            params.put("project_id", projectId);//projectsParam
            params.put("status_id", "*");
            params.put("limit", "100");
            params.put("updated_on", "><" + dateFrom + "|" + dateTo);
            params.put("sort", "author,id:desc");
            try{
            List<Integer> issuesId = getIssuesAllPages(issueManager, params).stream().map(Issue::getId).collect(Collectors.toList());
            for (Integer issueId : issuesId) {
                Issue issue = issueManager.getIssueById(issueId, Include.journals);
                Float total = issue.getSpentHours();
                List<Journal> journals = issue.getJournals().stream()
                        .filter(journal -> journal.getCreatedOn().after(Utils.parseDate(dateFrom))
                                && journal.getCreatedOn().before(Utils.parseDate(dateTo))
                                && testersParam.contains(journal.getUser().getId().toString()))
                        .collect(Collectors.toList());
                for (Journal journal : journals) {
                    List<JournalDetail> journalDetails = journal.getDetails().stream().filter(journalDetail -> journalDetail.getName().equals("status_id")).collect(Collectors.toList());
                    for (JournalDetail journalDetail : journalDetails) {
                        String oldStatus = Objects.isNull(journalDetail.getOldValue()) ? "" : getStatusNameById(issueStatuses, journalDetail.getOldValue());
                        String newStatus = Objects.isNull(journalDetail.getNewValue()) ? "" : getStatusNameById(issueStatuses, journalDetail.getNewValue());
                        String severity = Objects.isNull(issue.getCustomFieldByName("Severity")) ? "": issue.getCustomFieldByName("Severity").getValue();
                        bf.append(issue.getId() + ";"
                                          + issue.getSubject() + ";"
                                          + link + issue.getId() + ";"
                                          + issue.getAssigneeName() + ";"
                                          + Utils.convertDate(journal.getCreatedOn()) + ";"
                                          + journal.getUser().getFullName() + ";"
                                          + oldStatus + ";"
                                          + newStatus + ";"
                                          + issue.getCustomFieldById(171).getValue() + ";"
                                          + issue.getStatusName() + ";"
                                          + total + ";"
                                          + severity + ";"
                                          + issue.getProjectName() + "\n");
                    }
                }
            }
            }
            catch (NotAuthorizedException notAuthorizedException){
                LOG.info("У пользователя нет доступа к проекту с id " + projectId);
            }
        }
        return bf.toString();
    }
}
