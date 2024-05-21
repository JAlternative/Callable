import argparse
import configparser
import os
import re

from utils import line_matches_pattern_or_has_keywords

parser = argparse.ArgumentParser()
parser.add_argument("--file", help="Название .jmx-файла, из которого надо вытащить запросы. Без расширения!",
                    required=True)
parser = parser.parse_args()


def line_has_keywords(ln: str, keywords):
    return any(kw in ln for kw in keywords)


def line_matches_regex(ln: str, regex):
    return any(re.fullmatch(rg, ln) for rg in regex)


p = configparser.RawConfigParser()
p.read('config')

with open(os.path.join('jmx', parser.file + ".jmx"), encoding="UTF-8") as file:
    lines = file.readlines()
result = []
print("Названия запросов и контроллеров, которые не соответствуют регулярному выражению:")
for line in lines:
    line = line.replace("&quot;", '"')
    found = re.findall('testclass="HTTPSamplerProxy" testname="(.*?)" enabled="true"', line)
    if found and not line_matches_pattern_or_has_keywords(found[0], p['REGEX']['request'], p['KEYWORDS']['request']):
        print(found[0])
    found = re.findall('testclass="TransactionController" testname="(.*?)" enabled="true"', line)
    if found and not line_matches_pattern_or_has_keywords(found[0], p['REGEX']['controller'],
                                                          p['KEYWORDS']['controller']):
        print(found[0])
