import zipfile

import requests
from requests.auth import HTTPBasicAuth

from utils import config_helper
import sys


def download_results(dl_link: str):
    """
    Скачивает архив с результатами прогона по указанной ссылке

    :return: название скачанного файла (без расширения)
    """
    username, password = config_helper.get_credentials()
    r = requests.get(dl_link, auth=HTTPBasicAuth(username, password))
    if r.status_code != 200:
        sys.exit(f"Не удалось скачать файл с результатами: ошибка {r.status_code}. "
                 f"Убедитесь, что в файле config указаны данные для подключения к серверу.")
    filename = dl_link[dl_link.rfind("/") + 1:]
    print("---Подготовка файлов с результатами прогона---")
    print(f"Скачиваем архив {filename} по адресу {dl_link}")
    with open(filename, 'wb') as f:
        for chunk in r:
            f.write(chunk)
    unzip_folder = 'allure-results'
    print(f"Распаковываем {filename} в папку {unzip_folder}")
    with zipfile.ZipFile(filename, 'r') as zip_ref:
        zip_ref.extractall(unzip_folder)
    return filename[:filename.rfind('.')]
