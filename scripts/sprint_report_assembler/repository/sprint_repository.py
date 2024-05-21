import datetime
from models.Sprint import Sprint
import utils.api_caller as api


def get_sprints():
    sprints_json = api.get_sprints()
    sprints = []
    for sprint in sprints_json:
        sprints.append(Sprint(sprint))
    return sprints


def get_last_closed_sprint():
    sprints = get_sprints()
    sprints.sort(key=lambda s: s.id)
    last_closed_sprint = sprints[-1]
    start, end = api.get_sprint_dates(last_closed_sprint)
    last_closed_sprint.start = datetime.datetime.strptime(start, "%d/%b/%y %I:%M %p").date()
    last_closed_sprint.end = datetime.datetime.strptime(end, "%d/%b/%y %I:%M %p").date()
    return last_closed_sprint
