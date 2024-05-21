import re
from utils.config_helper import get_prefixes, get_groups
from typing import List


def gather_groups(test_lines: List[str]):
    """Собирает все группы теста в одну строку"""
    one_line = ''
    record = False
    for line in test_lines:
        if 'groups' in line:
            record = True
        if '}' in line and record:
            one_line = one_line + line.strip()
            break
        if record:
            one_line = one_line + line.strip()
    return one_line


class Test:
    method = ''
    dataprovider = False
    description = ''
    groups: List[str]
    id: str
    grade: int
    section: str
    class_name: str
    group: str
    ignored = False

    def __init__(self, test_lines: List[str], section_name: str, class_name: str):
        self._find_attributes(test_lines)
        self._return_groups(test_lines)
        self._find_id_and_grade()
        try:
            self.group = [g for g in self.groups if g in get_groups()][0]
        except IndexError:
            self.group = ''
        self.section = section_name
        self.class_name = class_name.split('.')[1]

    def __str__(self):
        return self.method

    def __eq__(self, other):
        return self.method == other.method and self.class_name == other.class_name

    def __hash__(self):
        return hash(f"{self.class_name}.{self.method}")

    def _find_attributes(self, test_lines: List[str]):
        """Возвращает тестовый метод, описание теста и информацию о наличии у него датапровайдера"""
        data_provider_regex = '.*dataProvider ?= ?"[\w ,\-/]+"'
        description_regex = '.*description ?= ?"(.+)"'
        for line in test_lines:
            if '@Ignore' in line:
                self.ignored = True
            if re.match(data_provider_regex, line):
                self.dataprovider = True
            if re.match(description_regex, line):
                self.description = re.findall(description_regex, line)[0].replace("\\", "")
            if ' void ' in line:
                self.method = re.findall('^(?:private|public) void (\w+)\(', line)[0]

    def _return_groups(self, test_lines: List[str]):
        """Конвертирует группы, зашитые в переменные, в строки, и выдает итоговый список групп"""
        all_groups = gather_groups(test_lines)
        try:
            all_groups = re.search('groups ?= ?\{(.*)},', all_groups).groups()[0].replace('"', '')
            all_groups = re.split(', ?', all_groups)
        except AttributeError:
            self.groups = []
        else:
            from test_collector import GROUP_ALIAS
            result = []
            for group in all_groups:
                try:
                    if GROUP_ALIAS[group]:
                        result.append(GROUP_ALIAS[group])
                except KeyError:
                    result.append(group)
            self.groups = result

    def _find_id_and_grade(self):
        """
        Ищет среди групп теста грейд и айдишник теста
        (среди групп с префиксами, указанными в конфиге, выбирается сама длинная)
        """
        prefixes = get_prefixes()
        id_groups = []
        for group in self.groups:
            if re.match('^' + '|'.join(prefixes), group):
                id_groups.append(group)
            elif re.match('^grade \w*(\d)', group):
                grades = re.findall('^grade \w*(\d)', group)
                if len(grades) == 1:
                    self.grade = int(grades[0])
                else:
                    self.grade = None
        if not id_groups:
            self.id = ''
        else:
            self.id = max(id_groups, key=len)
