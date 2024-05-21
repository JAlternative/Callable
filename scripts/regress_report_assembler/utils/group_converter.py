import re
from utils import file_reader


def get_alias():
    """Преобразует содержимое файла с группами в словарь для удобства обращения"""
    lines = file_reader.group_reader()
    result = {}
    for line in lines:
        if line and "//" not in line:
            matches = re.search('^\s*String ([\w]+) ?= ?"(.*)";$', line).groups()
            result[matches[0]] = matches[1]
    return result
