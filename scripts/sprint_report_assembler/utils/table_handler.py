import pandas
from utils.point_processor import count_points
import os
import csv


ASSIGNEE = "Assignee"
FOCUS_FACTOR = "FF"
DONE = "Done"
IN_PROGRESS = "WIP"
MAX_PTS = "Max pts"
OUTPUT_DIR = 'output/'


def issue_list_helper(issues):
    issue_list = []
    for issue in issues:
        if issue.assignee != "":
            issue_list.append(issue.print_to_csv())
    issue_list.sort(key=lambda i: i[ASSIGNEE], reverse=True)
    df = pandas.DataFrame(issue_list)
    make_output_folder_if_not_present()
    df.to_csv(f"{OUTPUT_DIR}issues.csv", index=False, quoting=csv.QUOTE_NONE, float_format='%.3f')


def make_output_folder_if_not_present():
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)


def get_sprint_info(sprint):
    dates = pandas.date_range(start=sprint.start, end=sprint.end).to_pydatetime().tolist()
    day_counter = 0
    for date in dates:
        if date.weekday() not in [5, 6]:
            day_counter += 1
    sprint.duration = day_counter
    sprint.max_points = day_counter * 8
    result = [{"Sprint": sprint.name.split(' ')[-1],
               MAX_PTS: sprint.max_points,
               "Sprint start": sprint.start,
               "Sprint end": sprint.end}]
    return result


def count(issue_list):
    names = set(map(lambda a: a.assignee, issue_list))
    sprint_result = []
    for name in names:
        if name:
            assignee_issues = list(filter(lambda i: i.assignee == name, issue_list))
            unfinished_points, complete_points = count_points(
                list(filter(lambda i: i.complete is False, assignee_issues)),
                list(filter(lambda i: i.complete is True, assignee_issues)))
            sprint_result.append({ASSIGNEE: name,
                                  FOCUS_FACTOR: f'={complete_points}/B3',
                                  DONE: complete_points,
                                  IN_PROGRESS: unfinished_points,
                                  "Tsk total": len(assignee_issues),
                                  "Tsk w/est": len(list(filter(lambda i: i.research is False, assignee_issues)))
                                  })
    return sprint_result


def print_results_to_excel(sprint, issue_list):
    sprint_info = get_sprint_info(sprint)
    make_output_folder_if_not_present()
    path = f"{OUTPUT_DIR}report.csv"
    for name in list(set(map(lambda a: a.assignee, issue_list))):
        sprint_info.append({"Sprint": name})
    sprint_info_pd = pandas.DataFrame(sprint_info)
    sprint_info_pd.to_csv(path, index=False, header=True)
