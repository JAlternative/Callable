from datetime import datetime, timedelta

import requests

base_url = 'https://jira.goodt.me/rest/api/2'
headers = {'Content-type': 'application/json'}
log = 'логин'
pas = 'пароль'

current_date = datetime.now()
date_format = '%Y-%m-%d'
cd_str = current_date.strftime(date_format)
cd_str_minus_day = (current_date - timedelta(days=3)).strftime(date_format)

sql_done = 'project = ABCHR AND issuetype = Bug AND status = Done AND resolved >= ' + cd_str_minus_day + ' AND resolved <= ' + cd_str + ' ORDER BY priority DESC, updated DESC'
sql_creation = 'project = ABCHR AND issuetype = Bug AND created >= ' + cd_str_minus_day + ' AND created <= ' + cd_str + ' ORDER BY priority DESC, updated DESC'
sql_cancelled = 'project = ABCHR AND issuetype = Bug AND status = Cancelled AND updated >= ' + cd_str_minus_day + ' AND updated <= ' + cd_str + ' ORDER BY priority DESC, updated DESC'


def correct_url(jql):
    return base_url + '/search?jql=' + jql + '&maxResults=-1'


def json_returner(jql):
    get_req = requests.get(jql, auth=(log, pas))
    return get_req.json()


def extract_issue_information(json):
    ar = json['issues']
    print_list = []
    counter = 0
    for i in ar:
        print_list.append(i['key'])
        counter += 1
    print('Количество задач=' + str(counter))
    print(print_list, sep='\t')


json_done = json_returner(correct_url(sql_done))
json_creation = json_returner(correct_url(sql_creation))
json_cancelled = json_returner(correct_url(sql_cancelled))

print('Готовые')
extract_issue_information(json_done)
print('Созданные')
extract_issue_information(json_creation)
print('Отмененные')
extract_issue_information(json_cancelled)
