from typing import List

from model.HistoryItem import HistoryItem
from model.Result import Result
from utils import file_handler


def remove_retries(results: List[Result]) -> List[Result]:
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


def collect_results() -> List[Result]:
    """Возвращает список объектов типа Result из текущего прогона"""
    dicts = file_handler.get_result_dicts()
    results = []
    for d in dicts:
        results.append(Result(d))
    return remove_retries(results)


def collect_history_items() -> List[HistoryItem]:
    test_results = collect_results()
    items = file_handler.read_history()
    result = []
    for r in test_results:
        history_item = items[r.history_id]
        result.append(HistoryItem(r, history_item))
    return result
