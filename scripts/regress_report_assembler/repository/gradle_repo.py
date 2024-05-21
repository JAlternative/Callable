import sys
from typing import List

from model.Gradle import Gradle
from utils import file_reader


def get_tasks():
    """Собирает все таски из build.gradle"""
    from_file = file_reader.get_gradle_task_lines()
    result = []
    for line in from_file:
        result.append(Gradle(line))
    print(f"В файле build.gradle найдено тасков: {len(result)}.")
    return result


def get_task_by_name(all_tasks: List[Gradle], name: str):
    """Возвращает таск с заданным именем"""
    try:
        return list(filter(lambda t: t.name == name, all_tasks))[0]
    except IndexError:
        sys.exit(f"в файле build.gradle не найден таск {name}.")
