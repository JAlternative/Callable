from typing import List, Set

from model.ReportLine import ReportLine
from model.Result import Result
from model.Test import Test


def intersection(lst1: List[str], lst2: List[str]):
    """Ищет общие элементы у двух списков"""
    temp = set(lst2)
    lst3 = [value for value in lst1 if value in temp]
    return lst3


def no_intersection(methods_to_run: List[str], result_methods: List[str]):
    """Ищет уникальные элементы для каждого из переданных списков"""
    test_only = list(filter(lambda m: m not in result_methods, methods_to_run))
    result_only = list(filter(lambda m: m not in methods_to_run, result_methods))
    return test_only, result_only


def find_intersections_and_omitted_tests(tests_to_run: Set[Test], test_results: List[Result]):
    """
    Возвращает списки тестов, которые
    1. были запущены как предполагалось
    2. были пропущены
    3. не должны были быть запущены
    """
    report_both = []
    report_test_only = []
    report_result_only = []
    methods_to_run = list(map(lambda t: f"{t.class_name}.{t.method}", tests_to_run))
    result_methods = list(map(lambda tr: f"{tr.test_class}.{tr.method}", test_results))
    both = intersection(methods_to_run, result_methods)
    test_only, result_only = no_intersection(methods_to_run, result_methods)
    for item in both:
        test = [t for t in tests_to_run if f"{t.class_name}.{t.method}" == item][0]
        results = [tr for tr in test_results if f"{tr.test_class}.{tr.method}" == item]
        if test.dataprovider:
            for r in results:
                report_both.append(ReportLine(test, r))
        else:
            report_both.append(ReportLine(test, results[0]))
    for item in test_only:
        report_test_only.append(ReportLine([t for t in tests_to_run if f"{t.class_name}.{t.method}" == item][0], None))
    for item in result_only:
        report_result_only.append(ReportLine(None, [tr for tr in test_results if f"{tr.test_class}.{tr.method}" == item][0]))
    print("---Результаты сопоставления---")
    print(f"Опознано тестов: {len(report_both)}.")
    print(f"Тестов без отчета: {len(report_test_only)}.")
    print(f"Результатов без кода: {len(report_result_only)}.")
    return report_both, report_test_only, report_result_only
