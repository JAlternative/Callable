import re
import argparse
import os

parser = argparse.ArgumentParser()
parser.add_argument("--file", help="Название скрипта из папки jmx, в котором нужно переименовать запросы. "
                                   "Без расширения!", required=True)
parser = parser.parse_args()

with open(os.path.join('jmx', parser.file + ".jmx")) as file:
    lines = file.readlines()
    request_counter = 1
    result = []
    for line in lines:
        controller = re.findall('testclass="TransactionController" testname="(.*?)"', line)
        request = re.findall('testclass="HTTPSamplerProxy" testname="(.*?)"', line)
        if controller and (re.match('c\d+_s\d+ - [а-яА-Я&quot; ]', controller[0])
                           or re.fullmatch('c\d+_s\d+', controller[0])):
            index = re.findall('(c\d+_s\d+)', controller[0])[0]
            result.append(line)
            request_counter = 1
        elif controller and "авторизация" in controller:
            index = 'c0_s0'
            request_counter = 1
            result.append(line)
        elif request and re.match('c\d+_s\d+_r(\d+)_[FB](_.*)?', request[0]):
            request_counter = int(re.findall('c\d+_s\d+_r(\d+)_[FB](_.*)?', request[0])[0][0]) + 1
            result.append(line)
        elif request and not re.match('c\d+_s\d+_r\d+_[FB](_.*)?', request[0].strip()):
            try:
                line_index = lines.index(line)
                related_lines = lines[line_index:line_index + 50]
                for rl in related_lines:
                    if "HTTPSampler.path" in rl:
                        path = re.findall('<stringProp name="HTTPSampler.path">(.*)</stringProp>', rl)[0]
                        break
                result.append(line.replace(request[0], f'{index}_r{request_counter}_FB_{path}'))
                request_counter += 1
            except NameError:
                result.append(line)
        else:
            result.append(line)
with open(f"{parser.file}_renamed.jmx", mode="w") as file:
    for r in result:
        file.write(r)

