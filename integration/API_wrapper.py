import requests
import json
from db_conn import requests_auth, wfm_address
import datetime

wfm_url = wfm_address()


def create_api(api_type, url='wfm'):
    if url == 'wfm':
        return "{0}/api/v1/integration-json/{1}".format(wfm_url, api_type)
    else:
        pass
        # return "{0}/api/v1/integration-json/{1}".format(intgr_url, api_type)


def send_post_api(url, body, query_params):
    headers = {'Content-Type': 'application/json', 'Accept': 'application/json'}
    return requests.post(url, data=body, auth=(requests_auth()), params=query_params, headers=headers)


def string_to_json(string):
    return json.dumps(string)


def create_org_unit_json(unic_id, main_org_unit_id):
    body = [
        {
            "outerId": unic_id,
            "active": True,
            "availableForCalculation": None,
            "dateFrom": "1970-01-01",
            "name": unic_id,
            "organizationUnitTypeOuterId": 5,
            "parentOuterId": main_org_unit_id,
            "zoneId": None,
            "properties": None
        }
    ]
    body = string_to_json(body)
    return body


def create_ep_json(unic_id, org_unit_id, period_from, period_to):
    body = [
        {
            "startWorkDate": period_from,
            "endWorkDate": period_to,
            "number": unic_id,
            "employee": {
                "outerId": unic_id,
                "firstName": "Владимир",
                "lastName": unic_id,
                "patronymicName": "Ильич",
                "birthDay": "2000-10-20",
                "gender": "MALE",
                "startDate": "2022-03-16",
                "endWorkDate": None,
                "properties": {
                    "КисКод": "CN-0104291"
                }
            },
            "position": {
                "name": "Продавец",
                "outerId": unic_id,
                "chief": None,
                "organizationUnit": {
                    "outerId": org_unit_id
                },
                "positionType": {
                    "outerId": "Продавец"
                },
                "positionGroup": {
                    "name": "РП ТЗ"
                },
                "positionCategory": {
                    "outerId": "_12b5312e-d3c3-48b2-88bc-81758457baf3"
                },
                "startWorkDate": "2021-10-05",
                "endWorkDate": None,
                "properties": None
            },
            "rate": None
        }
    ]
    body = string_to_json(body)
    return body


def create_sr_json(unic_id):
    body = [
        {
            "employeeOuterId": unic_id,
            "positionOuterId": unic_id,
            "type": "750873459435863200",
            "startDate": str(datetime.date.today().replace(day=1)),
            "startTime": "00:00",
            "endDate": str(datetime.date.today().replace(day=10)),
            "endTime": "23:59"
        }
    ]
    body = string_to_json(body)
    return body


def create_removed_entity_json(unic_id, data_type, start_date, end_date):
    body = [
        {
            "employeeOuterId": unic_id,
            "positionOuterId": unic_id,
            "type": data_type,
            "startDate": start_date,
            "endDate": end_date
        }
    ]
    body = string_to_json(body)
    return body


def create_kpi_json(kpi_id, org_unit_id):
    body = [
        {
            "organizationUnitOuterId": org_unit_id,
            "kpi": kpi_id,
            "date": str(datetime.date.today().replace(day=1)),
            "time": "00:00",
            "value": int(str(datetime.date.today().day)+'0')
        }
    ]
    body = string_to_json(body)
    return body


def create_role_json(user_id, org_unit_id, period_from, period_to, role_name):
    body = [
        {
            "employeeOuterId": user_id,
            "login": user_id,
            "password": user_id,
            "accountRoles": [
                {
                    "name": role_name,
                    "from": period_from,
                    "to": period_to,
                    "orgUnits": [
                        {
                            "outerId": org_unit_id
                        }
                    ]
                }
            ]
        }
    ]
    body = string_to_json(body)
    return body
