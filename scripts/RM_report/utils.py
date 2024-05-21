import argparse
import csv
import os
from datetime import date

from redminelib.exceptions import ResourceAttrError

import api_caller

VARIABLES = 'VARIABLES'


class Config:
    """
    Класс, хранящий все настройки, чтобы их не надо было постоянно парсить из файлов/дергать из апи
    """

    def __init__(self):

        self.parser = argparse.ArgumentParser()
        self.parser.add_argument("--date_from", help="Дата начала выгрузки", required=True)
        self.parser.add_argument("--date_to", help="Дата окончания выгрузки", required=True)
        self.parser.add_argument("--login", help="Логин", required=True)
        self.parser.add_argument("--password", help="Пароль", required=True)
        subparsers = self.parser.add_subparsers(dest='subparser_name')
        rtl_parser = subparsers.add_parser("rtl")
        kpi_parser = subparsers.add_parser("kpi")

        kpi_parser.add_argument("--testers", help="user_id тестировщиков", required=True)
        kpi_parser.set_defaults(func='KPI_report')

        rtl_parser.add_argument("--modules", help="Модули", required=True)
        rtl_parser.add_argument("--testing_statuses", help="Статусы тестирования", required=True)
        rtl_parser.add_argument("--non_testing_statuses", help="Статусы вне тестирования", required=True)
        rtl_parser.add_argument("--testers", help="Юзернеймы тестировщиков", required=True)
        rtl_parser.set_defaults(func='RTL_report')

        self.parser = self.parser.parse_args()
        self.login = self.parser.login
        self.password = self.parser.password
        self.date_from = date.fromisoformat(self.parser.date_from)
        self.date_to = date.fromisoformat(self.parser.date_to)
        self.testers = self.parser.testers.split(";")
        if self.parser.subparser_name == 'rtl':
            self.modules = self.parser.modules.split(";")
            self._non_testing_statuses = None
            self._testing_statuses = None
            self._all_system_statuses = None
            self._all_statuses = None

    @property
    def all_system_statuses(self):
        from api_caller import get_all_statuses
        if not self._all_system_statuses:
            self._all_system_statuses = get_all_statuses()
        return self._all_system_statuses

    @property
    def testing_statuses(self):
        if not self._testing_statuses:
            self._testing_statuses = list(map(self._get_status_id, self.parser.testing_statuses.split(";")))
        return self._testing_statuses

    @property
    def non_testing_statuses(self):
        if not self._non_testing_statuses:
            self._non_testing_statuses = list(map(self._get_status_id, self.parser.non_testing_statuses.split(";")))
        return self._non_testing_statuses

    @property
    def all_statuses(self):
        if not self._all_statuses:
            self._all_statuses = [*self.testing_statuses]
            self._all_statuses.extend(self.non_testing_statuses)
        return self._all_statuses

    def _get_status_id(self, string):
        result = list(filter(lambda a: a.name == string, self.all_system_statuses))
        if not result:
            raise ValueError(f'Статус "{string}" не найден в апи')
        return result[0].id


CONFIG = Config()


def list_former(*strs):
    """
    Формирует строку из элементов списка, разделенных символом |.
    Нужно для передачи нескольких вариантов аргументов в запрос в редмайне
    """
    result = ''
    for arg in strs:
        if not result:
            result = str(arg)
        else:
            result = result + '|' + str(arg)
    return result


def write_to_file(issues, output_file):
    """Записывает список результатов тестов в csv."""
    output_dir = 'output/'
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    with open(output_dir + output_file + ".csv", "w") as csv_file:
        fieldnames = ['Задача',
                      'Трекер',
                      'Тема',
                      'Создана',
                      'Обновлена',
                      'Модуль',
                      'Проект',
                      'Статус',
                      'Ответственный QA',
                      'Комментарий']
        writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
        writer.writeheader()
        for issue in issues:
            writer.writerow(issue.get_csv_row())
    print('---Сохранение отчета---')
    print(f"Отчет сохранен в {output_dir}{output_file}.csv")


def print_report(bugs, kpi_number: str):
    """Записывает отчет по kpi в csv"""
    bug_entries = []
    for bug in bugs:
        entry = get_dict(kpi_number, bug)
        if type(entry) == list:
            bug_entries.extend(entry)
        else:
            bug_entries.append(entry)
    if kpi_number in ["3", "bonus"]:
        bug_entries = [e for e in bug_entries if e]
        if kpi_number == "bonus":
            bug_entries = [e for e in bug_entries if e[time_spent]]
    output_dir = 'output/'
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    with open(f'{output_dir}kpi_{kpi_number}_{CONFIG.date_from}-{CONFIG.date_to}.csv', "w") as csv_file:
        fieldnames = get_fieldnames(kpi_number)
        writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
        writer.writeheader()
        for issue in bug_entries:
            writer.writerow(issue)


link = 'Ссылка'
author = 'Автор'
assigned_to = 'Исполнитель'
tester = 'Тестировщик'
created_on = 'Создано'
priority = 'Приоритет'
status = 'Статус'
issue_id = 'Номер задачи'
qa_points = 'QA поинты'
closed_on = 'Закрыта'
link_part = 'https://redmine.goodt.me/issues/'
time_spent = 'Трудозатраты'


def get_fieldnames(kpi_number: str) -> list:
    """Возвращает колонки для конкретного kpi-отчета"""
    if kpi_number == "1_2":
        return [issue_id,
                link,
                author,
                assigned_to,
                created_on,
                priority,
                status]
    elif kpi_number == "3":
        return [issue_id,
                qa_points,
                link,
                author,
                assigned_to,
                created_on,
                closed_on,
                priority,
                status,
                time_spent]
    elif kpi_number == "bonus":
        return [issue_id,
                link,
                author,
                tester,
                created_on,
                priority,
                status,
                time_spent]
    else:
        return []


def get_dict(kpi_number: str, issue) -> dict:
    """Возвращает словари, которые будут печататься в csv"""
    if not issue:
        result = {issue_id: "",
                  link: "",
                  author: "",
                  assigned_to: "",
                  created_on: "",
                  priority: "",
                  status: ""}
        if kpi_number == "3":
            result[qa_points] = ''
            result[closed_on] = ''
            result[time_spent] = ''
        elif kpi_number == 4:
            result[time_spent] = ''
        return result
    try:
        assignee = issue.assigned_to.name
    except ResourceAttrError:
        assignee = ""
    if kpi_number == "1_2":
        return {issue_id: issue.id,
                link: f"{link_part}{issue.id}",
                author: issue.author.name,
                assigned_to: assignee,
                created_on: issue.created_on.date(),
                priority: issue.priority.name,
                status: issue.status.name}
    elif kpi_number == "3":
        return decide_to_return_list_or_dict(kpi_number, issue, assignee)
    elif kpi_number == "bonus":
        return decide_to_return_list_or_dict(kpi_number, issue, assignee)
    else:
        return {}


def decide_to_return_list_or_dict(kpi_number, issue, assignee):
    total = count_total_time_spent_on_issue_by_tester(issue, kpi_number)
    base_dict = {issue_id: issue.id,
                 link: f"{link_part}{issue.id}",
                 author: issue.author.name,
                 created_on: issue.created_on.date(),
                 priority: issue.priority.name,
                 status: issue.status.name}
    if kpi_number == "3":
        base_dict[qa_points] = issue.custom_fields.get(171).value
        base_dict[closed_on] = issue.closed_on.date()
    result = []
    for name in total:
        result_dict = base_dict.copy()
        if kpi_number == "bonus":
            result_dict[tester] = name
        else:
            result_dict[assigned_to] = assignee
        result_dict[time_spent] = total[name]
        result.append(result_dict)
    return result


def count_total_time_spent_on_issue_by_tester(issue, kpi_number):
    """
    Считает, сколько тестировщики потратили времени на заданную задачу.
    Тестировщики берутся из --testers. Если над задачей работало несколько тестировщиков, их время считается отдельно,
    если kpi_number == bonus, и суммируется в остальных случаях.
    """
    time_entries = api_caller.get_time_entries_for_issue(issue.id)
    total = {}
    try:
        key = issue.assigned_to.name
    except ResourceAttrError:
        key = ""
    for entry in time_entries:
        if kpi_number == "bonus":
            key = entry.user.name
        try:
            total[key] += entry.hours
        except KeyError:
            total[key] = entry.hours
    if not total:
        total[key] = 0
    return total
