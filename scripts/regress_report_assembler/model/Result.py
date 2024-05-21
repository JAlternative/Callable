import re
from typing import List, Dict, Union

from utils import group_converter
from utils.config_helper import get_prefixes

GROUP_ALIAS = group_converter.get_alias()
EXCEPTIONS = ['Lambda']
TEST_CLASS = 'testClass'
SUITE = 'suite'
PARENT_SUITE = 'parentSuite'
LABELS = 'labels'
PARAMETERS = 'parameters'
TEST_METHOD = 'testMethod'
NAME = 'name'
SEVERITY_DICT = {'critical': 0,
                 'normal': 1,
                 'minor': 2,
                 'trivial': 3}


def json_to_dict(json_labels: List[Dict[str, str]]):
    """Конвертирует json в dict."""
    labels = {}
    if json_labels:
        for label in json_labels:
            current_label_name = label['name']
            current_label_value = label['value']
            if current_label_name not in labels:
                labels[current_label_name] = current_label_value
            elif isinstance(labels[current_label_name], list):
                labels[current_label_name].append(current_label_value)
            else:
                labels[current_label_name] = [labels[current_label_name], current_label_value]
    return labels


def get_tags(labels: Dict[str, Union[str, List[str]]]):
    """Смотрит в лейблы теста, если там есть его айди, возвращает его"""
    try:
        tags = labels['tag']
    except KeyError:
        return '', ''
    group = labels['suite']
    if "," in group:
        group = re.findall("(.*),", group)[0]
    if " (" in group:
        group = re.findall('(\w+)', group)[0]
    if not tags:
        return '', group
    is_list = isinstance(tags, list)
    if is_list:
        try:
            tags.remove(group)
        except ValueError:
            # на случай, если группы из таска нет в тегах, пробуем найти хоть какую-то группу
            try:
                all_groups = list(GROUP_ALIAS.values())
                new_group = [value for value in tags if value in all_groups][0]
                tags.remove(new_group)
            except ValueError:
                print(f"\n!!!\nОшибка при попытке вычислить ID теста. У теста должен был быть тег '{group}', "
                    f"но в отчете найдены только теги {tags}. Проверьте правильность проставленных тегов и групп.\n!!!\n")
        # конец попыток находить хоть какую-то группу в тегах
        if len(tags) >= 2:
            test_id = ' '.join(tags)
            return test_id, group
        elif len(tags) == 1:
            return tags[0], group
    else:
        if any(substring in tags for substring in get_prefixes()):
            return tags, ''
        else:
            return '', tags
    return '', ''


class Result:
    name: str
    status: str
    params: List[str]
    category: str
    grade: int
    section: str
    method: str
    history_id: str
    start: int
    id: str

    def __init__(self, json_file: Dict[str, Union[str, list]]):
        self.name = json_file[NAME]
        try:
            self.status = json_file['status']
        except KeyError:
            self.status = "unknown"
        params = json_to_dict(json_file[PARAMETERS])
        self._check_parameters(params)
        labels = json_to_dict(json_file[LABELS])
        suite_info = labels[SUITE]
        self.section = labels[PARENT_SUITE]
        self.test_class = labels[TEST_CLASS][labels[TEST_CLASS].index('.') + 1:]
        self.method = labels[TEST_METHOD]
        self.history_id = json_file['historyId']
        self.start = json_file['start']
        self.id, self.group = get_tags(labels)
        self.category = suite_info
        self.grade = 3
        self._get_severity(labels)

    def __eq__(self, other):
        return self.history_id == other.history_id

    def __hash__(self):
        return hash(self.history_id)

    def _check_parameters(self, params: Dict[str, str]):
        """Удаляет параметры, содержащие строки из списка в переменной EXCEPTIONS."""
        new_params = {}
        items = params.items()
        if len(params.keys()) == 1 and list(items)[0][1] in ['true', 'false']:
            self.params = []
            return
        for key, value in items:
            for exception in EXCEPTIONS:
                if exception not in value:
                    new_params[key] = value
        self.params = list(new_params.values())

    def _get_severity(self, labels: Dict[str, Union[str, List[str]]]):
        """Смотрит в лейблы теста, если там есть его грейд (severity), возвращает его"""
        try:
            value = labels['severity']
        except KeyError:
            self.grade = 3
            return
        severity = SEVERITY_DICT[value]
        if severity or severity == 0:
            self.grade = severity
