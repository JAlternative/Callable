import csv
import os
from typing import List
import json

import numpy as np
import pandas as p

from model.ReportLine import ReportLine
from utils.file_reader import get_encoding

GROUP = 'Group'
TEST_ID = 'ID'
NAME = 'Name'
GRADE = 'Grade'
SECTION = 'Section'
METHOD = 'Method'
STATUS = 'Status'


def to_csv(results: List[List[ReportLine]], output_file: str):
    """Записывает список результатов тестов в csv."""
    output_dir = 'output/'
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    encoding = get_encoding()
    if not encoding:
        encoding = 'utf-8'
    fieldnames = [GROUP, TEST_ID, NAME, GRADE, SECTION, METHOD, STATUS]
    with open(output_dir + output_file + ".csv", "w", encoding=encoding) as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
        writer.writeheader()
        for lst in results:
            write_entry_to_file(lst, writer)
            writer.writerow({GROUP: '', TEST_ID: '', NAME: '', GRADE: '', SECTION: '', METHOD: '', STATUS: ''})
    print('---Сохранение отчета---')
    print(f"Отчет сохранен в {output_dir}{output_file}.csv")
    output_csv = f"{output_dir}{output_file}.csv"


def write_entry_to_file(results: List[ReportLine], writer: csv.DictWriter):
    """Записывает переданную строку в файл"""
    for result in sorted(results):
        writer.writerow(result.get_dict())


def to_json(results: List[List[ReportLine]], output_file: str):
    output_dir = 'output/'
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    encoding = get_encoding()
    if not encoding:
        encoding = 'utf-8'
    with open(output_dir + output_file + ".json", "w", encoding=encoding) as json_file:
        for lst in results:
            for res in sorted(lst):
                json_file.write(json.dumps(res.get_dict()))
                json_file.write('\n')


def gather_test_stats(df: p.DataFrame):
    str_list = []
    str_list.append("|| ||Всего тестов||Passed||Failed||Broken||Skipped||\n")
    print('---Статистика по регрессу (грейды)---')
    statuses = ['passed', 'failed', 'broken', 'skipped']
    count_by_statuses = []
    for status in statuses:
        query = f"Grade != 'G3' & Status == '{status}'"
        temp = df.query(query).shape[0]
        count_by_statuses.append(temp)
    str_list.append(f"| |{sum(count_by_statuses)}|{count_by_statuses[0]}|{count_by_statuses[1]}|{count_by_statuses[2]}|{count_by_statuses[3]}|\n")
    for i in range(3):
        stats = list()
        grade = "G" + str(i)
        for status in statuses:
            query = f"Grade == '{grade}' & Status == '{status}'"
            temp = df.query(query).shape[0]
            stats.append(temp)
        count_all = sum(stats)
        print(grade, f"Всего: {count_all}, passed: {stats[0]}, failed: {stats[1]}, broken: {stats[2]}, skipped: {stats[3]}")
        str_list.append(f"|{grade}|{count_all}|{stats[0]}|{stats[1]}|{stats[2]}|{stats[3]}|\n")
    # собираем отношение пройденных тестов к непройденным (пока по группам, но потом можно и не по ним)
    print('---Статистика по регрессу (разделы)---')
    sections = df["Section"].dropna().unique()
    for section in sections:
        df_sections = df.query(f"Section == '{section}'")
        count_all = df_sections.shape[0]
        passed = df.query(f"Section == '{section}' & Status == 'passed'").shape[0]
        print(f"{section} ({passed}/{count_all})")
    print('---Статистика по регрессу (группы)---')
    groups = df["Group"].dropna().unique()
    for group in groups:
        df_groups = df.query(f"Group == '{group}'")
        suite_name = df_groups["Section"].unique()[0]
        count_all = df_groups.shape[0]
        passed = df.query(f"Group == '{group}' & Status == 'passed'").shape[0]
        print(f"{suite_name} {group} ({passed}/{count_all})")
    return str_list

def to_confluence_markup(output_file: str):
    encoding = get_encoding()
    if not encoding:
        encoding = 'utf-8'
    df = p.read_csv(output_file, encoding='windows-1251')
    with open(output_file[:-3] + "txt", "w", encoding=encoding) as txt_file:
        txt_file.writelines(gather_test_stats(df))
        txt_file.write("\n")
        temp = df.query("Grade == 'G0'")
        if temp is not None:
            count_all = temp.shape[0]
            passed = temp.query("Status == 'passed'").shape[0]
            txt_file.write(f"h1. Грейд super0 ({passed}/{count_all})\n")
            txt_file.write("||#||Group||Id||Name||Grade||Section||Method||Status|| ||\n")
            txt_file.writelines(rows_to_strings(temp))
            txt_file.write("\n")
        temp = df.query("Grade != 'G0'")
        groups = temp["Group"].dropna().unique()
        for group in groups:
            df_groups = temp.query(f"Group == '{group}'")
            suite_name = df_groups["Section"].unique()[0]
            count_all = df_groups.shape[0]
            passed = df_groups.query(f"Status == 'passed'").shape[0]
            txt_file.write(f"h1. {suite_name} {group} ({passed}/{count_all})\n")
            txt_file.write("||#||Group||Id||Name||Grade||Section||Method||Status|| ||\n")
            txt_file.writelines(rows_to_strings(df_groups))
            txt_file.write("\n")

def rows_to_strings(df: p.DataFrame):
    str_list = []
    df.index = np.arange(1, len(df) + 1)
    for row in df.itertuples():
        string = f"|{row.Index}|{row.Group}|{row.ID}|{row.Name}|{row.Grade}|{row.Section}|{row.Method}|{row.Status}| |\n"
        str_list.append(string)
    return str_list
