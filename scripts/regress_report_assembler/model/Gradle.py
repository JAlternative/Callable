import re
from typing import List


class Gradle:
    name: str
    path: str
    file: str

    def __init__(self, lines: List[str]):
        self._init_helper(lines, 'name')
        self._init_helper(lines, 'file')

    def __str__(self):
        return self.name

    def _init_helper(self, lines: List[str], param: str):
        for line in lines:
            if param == 'name':
                try:
                    self.name = re.findall('^task ([\w]+)\(type: \w+\)', line)[0]
                except IndexError:
                    return
            else:
                if 'suites' in line:
                    self.path = re.findall("^options\.suites '([\w/\.\+\-, ]+)'", line)[0]
                    self.file = re.findall('[\w/]+/([\w, ]+\.xml)', self.path)[0]
