from redminelib import Redmine

from repo import get_module_id
from utils import CONFIG

redmine = Redmine('https://redmine.goodt.me', username=CONFIG.login, password=CONFIG.password)


def get_issues():
    from utils import CONFIG, list_former
    module_ids = list(map(get_module_id, CONFIG.modules))
    return redmine.issue.filter(cf_105=list_former(*module_ids),
                                status_id=list_former(*CONFIG.all_statuses),
                                updated_on=f'><{CONFIG.date_from}|{CONFIG.date_to}')


def get_all_statuses():
    return redmine.issue_status.all()


def get_bugs_by_qa():
    from utils import CONFIG, list_former
    return redmine.issue.filter(project_id='abchr',
                                created_on=f'><{CONFIG.date_from}|{CONFIG.date_to}',
                                author_id=list_former(*CONFIG.testers),
                                tracker_id=38,
                                status_id='*',
                                sort='created_on:desc,id:desc,author')


def get_bugs_by_non_qa():
    from utils import CONFIG, list_former
    return redmine.issue.filter(project_id='abchr',
                                created_on=f'><{CONFIG.date_from}|{CONFIG.date_to}',
                                author_id='!' + list_former(*CONFIG.testers),
                                tracker_id=38,
                                status_id='*',
                                sort='created_on:desc,id:desc,author')


def get_completed_tasks_by_qa():
    from utils import CONFIG, list_former
    return redmine.issue.filter(project_id='abchr',
                                closed_on=f'><{CONFIG.date_from}|{CONFIG.date_to}',
                                assigned_to_id=list_former(*CONFIG.testers),
                                tracker_id='39|40|42',
                                status_id='*',
                                sort='author,id:desc')


def get_completed_tasks_by_non_qa():
    from utils import CONFIG, list_former
    return redmine.issue.filter(project_id='abchr',
                                closed_on=f'><{CONFIG.date_from}|{CONFIG.date_to}',
                                assigned_to_id='!' + list_former(*CONFIG.testers),
                                tracker_id='39|40|42',
                                status_id='*',
                                sort='author,id:desc')


def get_time_entries_for_issue(issue_id: int):
    from utils import CONFIG, list_former
    return redmine.time_entry.filter(project_id='abchr',
                                     spent_on=f'><{CONFIG.date_from}|{CONFIG.date_to}',
                                     issue_id=issue_id,
                                     user_id=list_former(*CONFIG.testers))


def get_tasks_for_kpi_bonus():
    from utils import CONFIG, list_former
    return redmine.issue.filter(project_id='abchr',
                                updated_on=f'><{CONFIG.date_from}|{CONFIG.date_to}',
                                author_id=list_former(*CONFIG.testers),
                                tracker_id=38,
                                status_id='*')
