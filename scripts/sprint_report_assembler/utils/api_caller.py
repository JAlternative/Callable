import requests
import pathlib
import os
from requests.auth import HTTPBasicAuth
import time

BASE_PATH = "https://jira.goodt.me/rest/greenhopper/1.0/"
LOGIN = ""
PASSWORD = ""
RAPID_VIEW = 262


def parse_credentials():
    file = open(os.path.join(pathlib.Path(__file__).parent.parent.absolute().__str__(), 'jira login'))
    lines = file.readlines()
    global LOGIN
    LOGIN = lines[0].replace("\n", "")
    global PASSWORD
    PASSWORD = lines[1].replace("\n", "")


def api_call(endpoint, params):
    return requests.get(
        BASE_PATH + endpoint,
        params=params,
        auth=HTTPBasicAuth(LOGIN, PASSWORD)
    )


def get_sprints():
    parse_credentials()
    params = {
        'includeHistoricSprints': 'False',
        '_': time.time()
    }
    response = api_call(f"sprintquery/{RAPID_VIEW}", params)
    if response.status_code != 200:
        raise Exception("Не удалось получить данные о спринтах. "
                        f"Код ошибки {response.status_code}")
    return response.json()["sprints"]


def get_sprint(sprint):
    params = {
        "rapidViewId": RAPID_VIEW,
        "sprintId": sprint.id
    }
    return api_call("rapid/charts/sprintreport", params)


def get_sprint_dates(sprint):
    response = get_sprint(sprint)
    json = response.json()
    return json['sprint']['startDate'], json['sprint']['endDate']


def get_issues_from_sprint(sprint):
    response = get_sprint(sprint)
    json = response.json()
    return json["contents"]["completedIssues"], json["contents"]["issuesNotCompletedInCurrentSprint"]


def get_additional_info(issue_id):
    params = {
        'fields': 'labels, aggregateprogress'
    }
    response = requests.get(
        f'https://jira.goodt.me/rest/api/2/issue/{issue_id}',
        params=params,
        auth=HTTPBasicAuth(LOGIN, PASSWORD)).json()
    try:
        labels = response['fields']['labels']
    except KeyError:
        labels = []
    return response['fields']['aggregateprogress']['progress'], labels
