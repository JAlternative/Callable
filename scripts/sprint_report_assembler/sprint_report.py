from repository.issue_repo import get_issues_from_sprint
from repository.sprint_repository import get_last_closed_sprint
from utils.table_handler import print_results_to_excel, issue_list_helper


def main():
    sprint = get_last_closed_sprint()
    print(sprint.name)
    issue_list = get_issues_from_sprint(sprint)
    issue_list_helper(issue_list)
    print_results_to_excel(sprint, issue_list)


main()
