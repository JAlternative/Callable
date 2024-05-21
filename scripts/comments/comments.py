import requests
import re
import pandas as pd
import pathlib

savePath = str(pathlib.Path(__file__).parent.resolve())

base_url = 'https://jira.goodt.me/rest/api/2'
headers = {'Content-type': 'application/json'}

log = input("login : ")
pas = input("password : ")
maxres = str(input("max results : "))

base_jql = 'project = AWFM and status != Cancelled'

URL = base_url + '/search?jql=' + base_jql + '&maxResults=' + maxres

# Список с логинами тестировщиков, добавлять сюда другие логины при необходимости
testers = ["m.druzhinin@goodt.me", "f.shabalkin@goodt.me", "a.kolganov@goodt.me", "a.svalova@goodt.me", "e.voronina@goodt.me", "e.kazova@goodt.me"]

get_req = requests.get(URL, auth=(log, pas))

json_data = get_req.json()

values = []
keys = []

for issue in json_data['issues']:

    key = issue["key"]

    COMM_URL = "https://jira.goodt.me/rest/api/2/issue/" + issue["id"] + "/comment"

    comm_req = requests.get(COMM_URL, auth=(log, pas))

    comm_data = comm_req.json()
    # print(COMM_URL)

    for comment in comm_data["comments"]:

        value = None

        # print(comment["body"])
        # print(comment["author"]["name"])
        text = comment["body"]
        name = comment["author"]["emailAddress"]
        if name in testers:

            if "t: " in str(comment["body"]):
                search_in = text.partition("t:")[2]
                # print(re.search(r'\d+', search_in).group())
                # print("############################ FOUND ############################")
                value = re.search(r'\d+', search_in).group()

            elif "t - " in str(comment["body"]):
                search_in = text.partition("t -")[2]
                # print(re.search(r'\d+', search_in).group())
                # print("############################ FOUND ############################")
                value = re.search(r'\d+', search_in).group()

            elif "=" in str(comment["body"]):
                search_in = text.partition("=")[2]
                # print(re.search(r'\d+', search_in).group())
                # print("############################ FOUND ############################")
                value = re.search(r'\d+', search_in).group()

        if value != None:
            values.append(value)
            keys.append(key)

output = {
    "Ключ задачи": keys,
    "Сложность": values
}

df = pd.DataFrame(output, columns=["Ключ задачи", "Сложность"])
df.to_csv(savePath + "\difficulty.csv", index=False)