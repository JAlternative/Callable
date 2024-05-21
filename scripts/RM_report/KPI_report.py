import api_caller
from utils import print_report


def assemble_kpi_report():
    qa_bugs = list(api_caller.get_bugs_by_qa())
    qa_bugs.append(None)
    qa_bugs.extend(api_caller.get_bugs_by_non_qa())
    print_report(qa_bugs, "1_2")
    completed = list(api_caller.get_completed_tasks_by_qa())
    completed.append(None)
    completed.extend(api_caller.get_completed_tasks_by_non_qa())
    print_report(completed, "3")
    kpi_bonus_issues = api_caller.get_tasks_for_kpi_bonus()
    print_report(kpi_bonus_issues, "bonus")


assemble_kpi_report()
