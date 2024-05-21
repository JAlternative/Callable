import csv
import os
import sys

from faker import Faker

NUMBER = 0


def parse_args():
    if len(sys.argv) != 2:
        sys.exit("Отсутствует обязательный параметр. Использование скрипта:\n"
                 "python generate_companies.py (количество компаний, которое нужно сгенерировать)")
    global NUMBER
    NUMBER = int(sys.argv[1])


def write_companies(csv_file):
    fake = Faker('ru-RU')
    writer = csv.writer(csv_file, delimiter='\t',
                        quotechar='|', quoting=csv.QUOTE_MINIMAL)
    for _ in range(NUMBER):
        writer.writerow([fake.company(),
                         fake.businesses_inn(),
                         fake.kpp(),
                         fake.address(),
                         fake.ascii_company_email(),
                         fake.last_name(),
                         fake.first_name(),
                         fake.ascii_email()])


def main():
    parse_args()
    if not os.path.exists('output'):
        os.mkdir('output')
    with open('output/companies.csv', 'w', newline='') as csv_file:
        write_companies(csv_file)
    with open('output/companies_preset.csv', 'w', newline='') as csv_file:
        write_companies(csv_file)



main()
