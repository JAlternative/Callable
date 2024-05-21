from typing import List

from model.Suite import Suite
from model.Test import Test
from model.Xml import Xml
from utils import file_reader
from utils.list_intersections import intersection


def get_all_tests_from_class(class_name: str, section_name: str):
    """Возвращает все тесты, которые есть в заданном тестовом классе, за исключением тестов с тегом @Ignore"""
    results = []
    from_file = file_reader.get_test_lines(class_name)
    for line in from_file:
        results.append(Test(line, section_name, class_name))
    return [r for r in results if not r.ignored]


def get_all_tests_for_suite(tests: List[Test], suite: Suite):
    """Возвращает тесты, которые должны пройти в рамках заданного сьюта"""
    results = []
    for test in tests:
        has_groups = test.groups
        has_no_excluded_groups = not intersection(test.groups, suite.exclude)
        has_any_included_group = intersection(test.groups, suite.include)
        if "(" in suite.name or (", грейд super0" in suite.name and "SCHED" in suite.name):
            if has_groups and has_no_excluded_groups and set(has_any_included_group) == set(suite.include):
                results.append(test)
        else:
            if has_groups and has_no_excluded_groups and has_any_included_group:
                results.append(test)
    print(f'Для сьюта "{suite}" найдено тестов: {len(results)}.')
    return results


def get_all_tests_from_suite_by_class(xml: Xml):
    """Возвращает все тесты из всех классов, которые задействованы в конкретном сьюте"""
    test_classes = xml.get_all_test_classes()
    results = {}
    for test_class in test_classes:
        results[test_class] = get_all_tests_from_class(test_class, xml.name)
    return results
