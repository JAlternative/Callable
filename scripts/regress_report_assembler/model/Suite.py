import re
from typing import List


def init_helper(xml_lines: List[str], param: str):
    return_value = []
    for line in xml_lines:
        if param in line:
            if param in ['test']:
                return re.search('^\w+=(.*)', line).groups()[0]
            else:
                return_value.append(re.search('^\w+=(.*)', line).groups()[0])
    return return_value


class Suite:
    name: str
    class_names: List[str]
    include: List[str]
    exclude: List[str]

    def __init__(self, lines: List[str]):
        self.name = init_helper(lines, 'test')
        self.include = init_helper(lines, 'include')
        self.exclude = init_helper(lines, 'exclude')
        self.class_names = init_helper(lines, 'class')

    def __str__(self):
        return self.name
