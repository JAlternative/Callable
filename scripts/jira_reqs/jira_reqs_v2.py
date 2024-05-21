import requests
import pandas as pd
import time


url = "https://jira.atlassian.com/rest/api/2/search?"
maxResults = 1
startAt = 99
jql = "jql=project = JRASERVER AND resolution = Unresolved and type = Bug ORDER BY createdDate DESC"

done = False

l_key = []
l_com = []
l_rep = []
l_cd = []
l_up = []   

mon = None
months = []
while mon != 0:
    mon = int(input("add month number (0 if done, 13 to remove) = "))
    if mon != 0 and mon != 13:
        months.append(mon)
        print("current months to search : ", months)
    elif mon == 13:
        rem = int(input("what month to remove? : "))
        months.remove(rem)
        print("current monts to search : ", months)
year = int(input("What year to search? (yyyy format) : "))
months.sort()

integer = 100
start_time = time.time()

while done == False and integer != 1:
    URL_REQ = url + "maxResults="+str(maxResults) + "&" + "startAt="+str(startAt) + "&" + jql
    r = requests.get(URL_REQ)
    data_json = r.json()
    for issue in data_json["issues"]:
        cd = issue["fields"]["created"]
        cd = cd.split("T")
        cd = cd[0].split("-")
    if int(cd[0]) > year:
        startAt += int(integer)
    else:
        if int(cd[1]) > months[-1] and int(cd[0]) == year:
            startAt += int(integer)
        else:
            if integer % 2 == 0:
                integer /= 2
            else:
                integer += 1
                integer /= 2
            startAt -= int(integer)
    if integer == 1:
        maxResults = 10
        startAt -= 1
        print(months[-1])
        while int(cd[1]) >= months[0] and int(cd[0]) >= year:
            URL_REQ = url + "maxResults="+str(maxResults) + "&" + "startAt="+str(startAt) + "&" + jql
            r = requests.get(URL_REQ)
            data_json = r.json()
            for issue in data_json["issues"]:
                cd = issue["fields"]["created"]
                cd = cd.split("T")
                cd = cd[0].split("-")
                if (int(cd[0]) == year and int(cd[1]) in months):
                
                    key = issue["key"]
                    l_key.append(key)
                
                    component = issue["fields"]["components"][0]["name"]
                    l_com.append(component)
                
                    reporter = issue["fields"]["reporter"]["displayName"]
                    l_rep.append(reporter)
                
                    creation_date = issue["fields"]["created"]
                    l_cd.append(creation_date)
                
                    update_date = issue["fields"]["updated"]
                    l_up.append(update_date)
            startAt += 10
        done = True


data_graph = {
        "key" : l_key,
        "component" : l_com,
        "reporter" : l_rep,
        "creation date" : l_cd,
        "update date" : l_up
    }

# Не забудьте поменять расположение файла для сохранение здесь, и в graph.py (строки 192 и 205)!
df = pd.DataFrame(data_graph,columns=["key", "component", "reporter", "creation date", "update date"])
df.to_csv(r"C:\Users\danil\Documents\Work\jira_reqs\data_csv.csv",index=False)




print(df)
print("--- %s seconds ---" % (time.time() - start_time))