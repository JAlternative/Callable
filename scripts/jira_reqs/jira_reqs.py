import requests
import pandas as pd


# Метод для api запрос с jira и сохранения полученного файла в csv формате
def create_csv(savePath, bc):
    # Тестовый api запрос для последнего бага :
    # URL_REQ = "https://jira.atlassian.com/rest/api/2/search?maxResults=1&startAt=0&jql=project = JRASERVER AND resolution = Unresolved and type = Bug ORDER BY createdDate DESC"

    if bc == "AWFM":
        jql = "jql= project = AWFM and type = Bug ORDER BY createdDate DESC"
    elif bc == "VID":
        jql = "jql= project = VID and type = Bug ORDER BY createdDate DESC"
    else:
        jql = "jql= project = ABCHR and type = Bug ORDER BY createdDate DESC"

    url = "https://jira.goodt.me/rest/api/2/search?"

    log = input("your login : ")
    pas = input("your password : ")

    choice = int(input("1 - search by date \n" \
                       "2 - search with maxResult/startAt input by user \n" \
                       "? = "))

    if choice == 1:

        choice_date = int(input("1 - search by months/year \n" \
                                "2 - search by quarter/year \n" \
                                "3 - search by year \n" \
                                "? = "))

        months = []

        if choice_date == 1:
            mon = None
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

        elif choice_date == 2:
            quarter = int(input("quarter (1-4) = "))
            year = int(input("year (yyyy format) = "))

            if quarter == 1:
                months = [1, 2, 3]
            elif quarter == 2:
                months = [4, 5, 6]
            elif quarter == 3:
                months = [7, 8, 9]
            else:
                months = [10, 11, 12]

        elif choice_date == 3:
            year = int(input("year (yyyy format) = "))

        maxResults = 1
        startAt = 9

        done = False

        l_key = []
        # l_com = []
        l_rep = []
        l_cd = []
        l_up = []

        months.sort()

        integer = 100

        while done == False and integer != 1:
            URL_REQ = url + "maxResults=" + str(maxResults) + "&" + "startAt=" + str(startAt) + "&" + jql
            # print(URL_REQ)
            r = requests.get(URL_REQ, auth=(log, pas))
            data_json = r.json()
            for issue in data_json["issues"]:
                cd = issue["fields"]["created"]
                cd = cd.split("T")
                cd = cd[0].split("-")
            if int(cd[0]) > year:
                startAt += int(integer)
            elif choice_date != 3:
                if int(cd[1]) > months[-1] and int(cd[0]) == year:
                    startAt += int(integer)
                else:
                    if integer % 2 == 0:
                        integer /= 2
                    else:
                        integer += 1
                        integer /= 2
                    startAt -= int(integer)
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
                while int(cd[0]) >= year:
                    URL_REQ = url + "maxResults=" + str(maxResults) + "&" + "startAt=" + str(startAt) + "&" + jql
                    r = requests.get(URL_REQ, auth=(log, pas))
                    data_json = r.json()
                    for issue in data_json["issues"]:
                        cd = issue["fields"]["created"]
                        cd = cd.split("T")
                        cd = cd[0].split("-")
                        if (int(cd[0]) == year and int(cd[1]) in months):
                            key = issue["key"]
                            l_key.append(key)

                            # component = issue["fields"]["components"][0]["name"]
                            # l_com.append(component)

                            reporter = issue["fields"]["reporter"]["displayName"]
                            l_rep.append(reporter)

                            creation_date = issue["fields"]["created"]
                            l_cd.append(creation_date)

                            update_date = issue["fields"]["updated"]
                            l_up.append(update_date)
                    if choice_date != 3:
                        if int(cd[1]) < months[0]:
                            break
                    startAt += 10
                done = True

    elif choice == 2:

        maxResults = int(input("Max results = "))
        startAt = int(input("Start at = "))

        URL_REQ = url + "maxResults=" + str(maxResults) + "&" + "startAt=" + str(startAt) + "&" + jql

        r = requests.get(URL_REQ, auth=(log, pas))

        # Status code :
        # print(r)

        # Создание датафрейма из нужных параметров
        data_json = r.json()

        l_key = []
        # l_com = []
        l_rep = []
        l_cd = []
        l_up = []

        for issue in data_json["issues"]:
            key = issue["key"]
            # print(key)
            l_key.append(key)

            # component = issue["fields"]["components"][0]["name"]
            # print(component)
            # l_com.append(component)

            reporter = issue["fields"]["reporter"]["displayName"]
            # print(reporter)
            l_rep.append(reporter)

            creation_date = issue["fields"]["created"]
            # print(creation_date)
            l_cd.append(creation_date)

            update_date = issue["fields"]["updated"]
            # print(update_date)
            l_up.append(update_date)

    data_graph = {
        "key": l_key,
        # "component" : l_com,
        "reporter": l_rep,
        "creation date": l_cd,
        "update date": l_up
    }

    # df = pd.DataFrame(data_graph,columns=["key", "component", "reporter", "creation date", "update date"])
    df = pd.DataFrame(data_graph, columns=["key", "reporter", "creation date", "update date"])
    df.to_csv(savePath + "\data_csv(" + str(bc) + ").csv", index=False)