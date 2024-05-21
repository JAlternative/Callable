import glob
import json
import os
import shutil

from model.Result import Result


def get_results():
    """Возвращает список объектов типа Result, собранных из файлов в папке 'allure-results"""
    results_path = os.path.join(os.getcwd(), "allure-results")
    result_files = glob.glob(os.path.join(results_path, "*-result.json"))
    results = []
    for file in result_files:
        results.append(read_result_file(file))
    return results


def read_result_file(file_name: str):
    """Инициализирует объект класса Result из указанного файла."""
    with open(file_name, encoding='utf-8') as file:
        read = file.read()
        if read:
            json_file = json.loads(read)
            return Result(json_file)


def clean_up(filename: str):
    """
    Подчищает allure-results и скачанный архив.

    :param filename: название архива (без расширения)
    """
    print('---Удаление временных файлов---')
    print('Удаляем папку allure-results')
    shutil.rmtree('allure-results')
    print(f"Удаляем {filename}.zip")
    os.remove(f"{filename}.zip")