import os
import random
import string
import sys
from datetime import date

from faker import Faker
from openpyxl import load_workbook

DIGITS = string.digits
NUMBER = 5


def generate_snils():
    init_number = ''.join((random.choice(DIGITS) for i in range(9)))
    total = 0
    for i in range(9):
        total = total + int(init_number[i]) * (9 - i)
    mod = total % 101
    if mod == 100:
        mod = 0
    return f'{int(init_number):,}-{mod:02d}'.replace(',', '-')


def parse_args():
    if len(sys.argv) != 2:
        sys.exit("Отсутствует обязательный параметр. Использование скрипта:\n"
                 "python generate_users.py (количество файлов, которое нужно сгенерировать)")
    global NUMBER
    NUMBER = int(sys.argv[1])


def main():
    parse_args()
    if not os.path.exists('output'):
        os.mkdir('output')
    today = date.today()
    for counter in range(NUMBER):
        wb = load_workbook(filename='templates/upload_employee_template.xlsx')
        sheet = wb['Сотрудники']
        sheet['A3'].value = generate_snils()
        sheet['B3'].value = ''.join((random.choice(DIGITS) for i in range(6)))
        fake = Faker('ru-RU')
        name = fake.name()
        last, first, patronymic = name.replace('г-н ', '').replace('г-жа ', '').replace('тов. ', '').split(" ")
        sheet['C3'].value = last
        sheet['D3'].value = first
        sheet['E3'].value = patronymic
        sheet['H3'].value = fake.date_between(start_date=today.replace(month=1, day=1, year=today.year - 2),
                                              end_date=today)
        wb.save(f'output/employee_{counter + 1}.xlsx')


main()
