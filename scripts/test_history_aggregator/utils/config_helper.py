import configparser
from typing import List


def read_config():
    """Возвращает объект парсера конфиг-файла"""
    parser = configparser.RawConfigParser()
    parser.read('config')
    return parser


def get_credentials() -> (str, str):
    """Дергает из конфига логин и пароль для доступа к серверу с результатами прогона"""
    host = 'storage'
    parser = read_config()
    login = parser[host]['login']
    password = parser[host]['password']
    return login, password


def get_prefixes() -> List[str]:
    """Дергает из конфига возможные префиксы, на которые могут начинаться айди тестов"""
    parser = read_config()
    return parser['prefixes']['prefixes'].split('\n')
