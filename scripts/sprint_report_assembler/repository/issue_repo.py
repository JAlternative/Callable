from models.Issue import Issue
import utils.api_caller as api


def get_issues_from_sprint(sprint):
    completed, unfinished = api.get_issues_from_sprint(sprint)
    result = []
    for issue in completed:
        result.append(Issue(issue, True))
    for issue in unfinished:
        result.append(Issue(issue, False))
    return result
