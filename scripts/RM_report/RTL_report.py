import utils
from api_caller import get_issues
from models.Issue import Issue


def assemble_rtl_reports():
    filtered = get_issues()
    tested_issues = []
    not_tested_issues = []
    for issue in filtered:
        wrapped_issue = Issue(issue)
        if wrapped_issue.tested:
            tested_issues.append(wrapped_issue)
        else:
            not_tested_issues.append(wrapped_issue)
    went_through_qa = list(filter(lambda a: a.issue.status.id in utils.CONFIG.non_testing_statuses, tested_issues))
    complete_without_qa = list(filter(lambda a: str(a.issue.status.id) in ['4', '23'], not_tested_issues))
    utils.write_to_file(went_through_qa, f'{utils.CONFIG.date_from}-{utils.CONFIG.date_to}_went_through_qa')
    utils.write_to_file(complete_without_qa, f'{utils.CONFIG.date_from}-{utils.CONFIG.date_to}_complete_without_qa')


assemble_rtl_reports()
