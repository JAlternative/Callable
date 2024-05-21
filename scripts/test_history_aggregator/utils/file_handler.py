import glob
import json
import os
import shutil
from typing import List


def clean_up(filename: str):
    """
    Подчищает allure-results и скачанный архив.

    :param filename: название архива (без расширения)
    """
    print('---Удаление временных файлов---')
    print('Удаляем папку allure-results')
    shutil.rmtree('allure-results')
    print(f"Удаляем {filename}.zip")


def get_result_dicts() -> List[dict]:
    """Возвращает список словарей, собранных из файлов в папке 'allure-results"""
    results_path = os.path.join(os.getcwd(), "allure-results")
    result_files = glob.glob(os.path.join(results_path, "*-result.json"))
    results = []
    for file in result_files:
        results.append(read_result_file(file))
    return results


def read_result_file(file_name: str) -> dict:
    """Парсит json из указанного файла."""
    with open(file_name, encoding='utf-8') as file:
        read = file.read()
        if read:
            return json.loads(read)


def read_history() -> dict:
    """Парсит файл history.json, содержащий статистику запусков конкретных тестов"""
    results_path: str = os.path.join(os.getcwd(), "allure-results")
    file_name: str = os.path.join(results_path, "history.json")
    with open(file_name, encoding='utf-8') as file:
        read = file.read()
    if read:
        return json.loads(read)
