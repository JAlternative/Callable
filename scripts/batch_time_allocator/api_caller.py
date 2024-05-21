# coding=utf-8
import sys

import requests
import json
import config_handler as config

BASE_PATH = 'https://corport-demo.goodt.me/api/'
COOKIE = ''


def auth():
    a = requests.options(BASE_PATH + "character/login").headers.get("Set-Cookie")
    global COOKIE
    COOKIE = a.split(';')[0].split('=')[1]
    email, password = config.get_credentials()
    r = requests.post(BASE_PATH + "character/login",
                      data={"email": email, "password": password},
                      cookies={"GMFSESSID": COOKIE})
    wrong_creds_message = "Не удалось авторизоваться. Проверьте правильность введенных логина и пароля."
    try:
        if json.loads(r.text)['result'] != 1:
            sys.exit(wrong_creds_message)
    except KeyError:
        sys.exit(wrong_creds_message)


def get_template_id(name):
    get_templates = requests.get(BASE_PATH + "project/listTemplate",
                                 cookies={"GMFSESSID": COOKIE})
    templates = json.loads(get_templates.text)['list_template']
    result = None
    for template in templates:
        if template['name'] == name:
            result = template['id']
            return result
    if not result:
        sys.exit('Не найден шаблон "{}". Убедитесь, что шаблон с таким названием существует в вашем ЛК в ВКП.'.format(name))


def assign_time(template_id, date):
    post = requests.post(BASE_PATH + "project/setTemplateDay",
                         cookies={"GMFSESSID": COOKIE},
                         data={"id": template_id,
                               "year": date.year,
                               "month": date.month,
                               "day": date.day})
    if json.loads(post.text)['result'] == 1:
        return True
    else:
        return False
