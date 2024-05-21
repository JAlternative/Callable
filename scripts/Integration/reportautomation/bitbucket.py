import requests

base_url = 'https://git.goodt.me/rest/api/1.0/projects/ABC/repos/hrportal/commits'
headers = {'Content-type': 'application/json'}
log = 'логин'
pas = 'пароль'


def json_returner(url):
    get_req = requests.get(url, auth=(log, pas))
    return get_req.json()


big_json_dictonary = json_returner(base_url)['values']
print(big_json_dictonary)
