import csv
import os
from typing import List

from model.HistoryItem import HistoryItem

GROUP = 'group'
METHOD = 'method'
NAME = 'name'
RATE = 'success rate'
TOTAL = 'total'
PASSED = 'passed'
FAILED = 'failed'
BROKEN = 'broken'
SKIPPED = 'skipped'
UNKNOWN = 'unknown'
LAST_LAUNCH = 'last launch'
API = 'api test'


def to_csv(results: List[HistoryItem], output_file: str):
    """Записывает список результатов тестов в csv."""
    output_dir = '../output/'
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    fieldnames = [GROUP, METHOD, NAME, API, RATE, TOTAL, PASSED, FAILED, BROKEN, SKIPPED, UNKNOWN, LAST_LAUNCH]
    with open(output_dir + output_file + ".csv", "w") as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
        writer.writeheader()
        for result in sorted(results):
            writer.writerow(result.get_dict())
    print('---Сохранение отчета---')
    print(f"Отчет сохранен в {output_dir}{output_file}.csv")
