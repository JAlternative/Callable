import re

from model.Suite import Suite
from typing import List


class Xml:
    name: str
    suites: List[Suite]
    file_name: str

    def __init__(self, lines: List[str], file_name: str):
        self.name = re.findall('^suite=([A-Za-zА-Яа-я ,]+)', lines[0])[0]
        self._find_suites(lines)
        self.file_name = file_name

    def __str__(self):
        return self.file_name

    def get_all_test_classes(self):
        results = []
        for suite in self.suites:
            if suite.class_names not in results:
                results.extend(suite.class_names)
        return results

    def _find_suites(self, lines: List[str]):
        result = []
        suite = []
        for line in lines:
            if re.fullmatch('(test|\w{2}clude|class)=[\w а-яА-Я,\.]+|'
                            'test="([^a-z]+ \([a-z]+\))|'
                            'test=([^a-z]+\s[]\s\([a-z]+\))', line):
                suite.append(line)
            if '/test' in line:
                result.append(suite)
                suite = []
        self.suites = []
        for item in result:
            self.suites.append(Suite(item))
