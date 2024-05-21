from repository import xml_repo, test_repo, gradle_repo
from utils import config_helper
from utils.group_converter import get_alias


GROUP_ALIAS = {}


def collect(job_name: str):
    """Находит все тесты, которые должны пройти в рамках заданного джоба"""
    global GROUP_ALIAS
    GROUP_ALIAS = get_alias()
    task_names = config_helper.get_tasks_for_job(job_name)
    result = []
    tasks = gradle_repo.get_tasks()
    for task_name in task_names:
        one_task = gradle_repo.get_task_by_name(tasks, task_name)
        xml = xml_repo.get_xml(one_task)
        all_tests_by_class = test_repo.get_all_tests_from_suite_by_class(xml)
        print()
        print(f"В таске {task_name} найдено сьютов: {len(xml.suites)}.")
        for suite in xml.suites:
            for class_name in suite.class_names:
                result.extend(test_repo.get_all_tests_for_suite(all_tests_by_class[class_name], suite))
    return set(result)
