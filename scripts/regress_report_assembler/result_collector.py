from typing import List

from model.Result import Result
from utils import file_handler


def remove_retries(results: List[Result]):
    """
    Ищет в списке ретраи одного и того же теста (по historyId) и оставляет последний.

    :param results: список результатов
    :return: сортированный список результатов с удаленными ретраями
    """
    results_set = set(results)
    no_retries = []
    for result in results_set:
        count = results.count(result)
        if count == 1:
            no_retries.append(result)
        else:
            filtered = [r for r in results if r == result]
            no_retries.append(max(filtered, key=lambda e: e.start))
    return no_retries


def collect():
    results = file_handler.get_results()
    return remove_retries(results)

