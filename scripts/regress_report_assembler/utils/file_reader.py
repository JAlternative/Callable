import re
import os

ROOT_PATH = re.findall('[\w:/\\\]+testit', os.getcwd())[0]
SELENIUM_PATH = os.path.join(ROOT_PATH, 'Selenium')
TEST_PATH = os.path.join(SELENIUM_PATH, 'src', 'test')
TEST_CLASS_PATH = os.path.join(TEST_PATH, 'java')
ENCODING = ''


def get_xml_lines(xml_path: str):
    """Читает xml"""
    split = xml_path.split('/')
    with open(os.path.join(SELENIUM_PATH, split[0], split[1], split[2], split[3], split[4], split[5])) as xml:
        exclude = ['DOCTYPE', 'encoding', 'listener', 'method-selectors', 'method-selector', 'script', 'return', 'classes>', 'groups>', 'run>']
        lines = xml.readlines()
        result = []
        for line in lines:
            if not re.findall('|'.join(exclude), line) and line != '\n':
                if 'name' not in line:
                    if 'groups.' in line:
                        line.replace("\n","")
                        searchResult = re.search('(?:^\s*(.?groups\.containsKey)\(\"([a-zA-Z0-9\s]*)\"\))', line)
                        matches = searchResult.groups()
                        if "!" in matches[0]:
                            replacement = "exclude"
                        else:
                            replacement = "include"
                        matches = [replacement, matches[1]]
                        result.append(matches[0] + '=' + matches[1])
                    else:
                        result.append(re.search("^\s*<(/\w+)>\s?$", line).groups()[0])
                    continue
                try:
                    matches = re.search('(?:^\s*<(\w{2}clude) name="([a-zA-Z\d\-\. ]*)"/>\s$)|'
                                        '(?:^\s*<(\w+) name="([а-яА-яa-zA-Z, \.\d\-]+)[a-z\d \-"=]*>\s$)|'
                                        '(?:^\s*<(\w+) name="([a-zA-Z\.]*)"/>\s$)|'
                                        '^\s*<(\w+) name="([^a-z]+ \([a-z]+\))"[a-z\d \-"=]*>$|'
                                        '^\s*<(\w+) name="([^a-z]+\s[]\s\([a-z]+\))"[a-z\d \-"=]*>$', line).groups()
                except AttributeError:
                    matches = re.search('^\s*<(\w+) name="([^a-z]+)"[a-z\d \-"=]*>$|'
                                        '^\s*<(\w+) name="(.*)">$', line).groups()
                    matches = list(filter(None, matches))
                    decoded = decode(line, matches[1])
                    matches = [matches[0], decoded]
                matches = list(filter(None, matches))
                result.append(matches[0] + '=' + matches[1])
    return result


def decode(line, match):
    """Декодирует русские названия на системах Windows"""
    decoded = None
    for enc in ['cp1251', 'iso8859_5', 'koi8_r']:
        try:
            decoded = match.encode(enc).decode()
            global ENCODING
            ENCODING = enc
        except (UnicodeDecodeError, UnicodeEncodeError):
            print(f'Не удалось декодировать строку {line} из кодировки {enc}')
        else:
            break
    if not decoded:
        decoded = match
        print(f'Не удалось декодировать строку {line}, выполнение скрипта продолжится с абракадаброй :(')
    return decoded


def get_test_lines(class_name: str):
    """
    Читает тестовый класс и записывает для каждого теста
    строки от аннотации @Test до объявления тестового метода включительно
    """
    parts = class_name.split('.')
    name = os.path.join(TEST_CLASS_PATH, parts[0], parts[1] + '.java')
    with open(name, encoding='utf-8') as test_class_file:
        lines = test_class_file.readlines()
        results = []
        record = False
        for line in lines:
            if ('@Test' in line or '@Ignore' in line) and not record:
                record = True
                test = []
            if 'void' in line and record:
                test.append(line.strip())
                record = False
                results.append(test)
            if record:
                test.append(line.strip())
    return results


def get_gradle_task_lines():
    """Читает build.gradle и возвращает строки с тасками"""
    build = os.path.join(SELENIUM_PATH, 'build.gradle')
    with open(build) as file:
        lines = file.readlines()
        record = False
        results = []
        for line in lines:
            if re.match("^\s*task \w+ ?\(type:\s*Test\)\s*\{", line):
                result = []
                record = True
            if '}' in line and record:
                results.append(result)
                record = False
            elif record:
                result.append(line.strip())
        return results


def group_reader():
    """Читает файл с переменными, в которые зашиты названия групп"""
    path = os.path.join(TEST_CLASS_PATH, "common", "Groups.java")
    with open(path) as file:
        lines = file.readlines()
        record = False
        for line in lines:
            if '}' in line and record:
                record = False
            if record:
                result.append(line.strip())
            if '{' in line:
                result = []
                record = True
    return result


def get_encoding():
    return ENCODING
