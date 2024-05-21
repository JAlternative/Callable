from utils.api_caller import get_additional_info


ASSIGNEE = "Assignee"
TASK_NAME = "Task"
DONE = "Done"
IN_PROGRESS = "WIP"
TIME = "Time (min)"
FIX = "Fix"
BASE_ISSUE_PATH = "https://jira.goodt.me/browse/"


class Issue:
    def __init__(self, json, done):
        self.complete = done
        self.id = json["id"]
        self.key = json["key"]
        self.priority_name = json["priorityName"]
        self.progress, self.labels = get_additional_info(self.id)
        self.fix = 'fix' in self.labels
        self.research = 'rnd' in self.labels
        self.done = None
        self.wip = None
        if not self.fix:
            try:
                current_estimate = int(json["currentEstimateStatistic"]["statFieldValue"]["value"])
            except KeyError:
                current_estimate = None
            try:
                estimate = int(json["estimateStatistic"]["statFieldValue"]["value"])
            except KeyError:
                estimate = None
            if done:
                self.done = current_estimate
            else:
                if current_estimate:
                    self.wip = current_estimate
                try:
                    self.done = estimate - current_estimate
                except TypeError:
                    self.done = None
        try:
            self.assignee = json["assignee"]
        except KeyError:
            self.assignee = ""

    def __str__(self):
        return self.key

    def print_to_csv(self):
        return {ASSIGNEE: self.assignee,
                TASK_NAME: f'=ГИПЕРССЫЛКА("{BASE_ISSUE_PATH + self.key}";"{self.key}")',
                DONE: self.done,
                IN_PROGRESS: self.wip,
                FIX: self.fix,
                TIME: self.progress / 60}
