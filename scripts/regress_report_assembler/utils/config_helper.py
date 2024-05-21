import configparser


def read_config():
    """Возвращает объект парсера конфиг-файла"""
    parser = configparser.RawConfigParser()
    parser.read('config', encoding='utf-8')
    return parser


def get_credentials():
    """Дергает из конфига логин и пароль для доступа к серверу с результатами прогона"""
    host = 'storage'
    parser = read_config()
    login = parser[host]['login']
    password = parser[host]['password']
    return login, password


def get_tasks_for_job(job_name: str):
    """Дергает из конфига список тасков для указанной джобы в дженкинсе"""
    parser = read_config()
    return parser['tasks'][job_name].split(" ")


def get_prefixes():
    """Дергает из конфига возможные префиксы, на которые могут начинаться айди тестов"""
    parser = read_config()
    return parser['prefixes']['prefixes'].split('\n')


def get_groups():
    """Дергает из конфига возможные группы тестов"""
    parser = read_config()
    return parser['prefixes']['groups'].split('\n')

