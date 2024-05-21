import csv
import os
import sys
from datetime import date

from faker import Faker

from generate_users_xlsx import generate_snils

NUMBER = 0


def parse_args():
    if len(sys.argv) != 2:
        sys.exit("Отсутствует обязательный параметр. Использование скрипта:\n"
                 "python generate_users.py (количество пользователей, которое нужно сгенерировать)")
    global NUMBER
    NUMBER = int(sys.argv[1])


def main():
    parse_args()
    fake = Faker('ru-RU')
    if not os.path.exists('output'):
        os.mkdir('output')
    today = date.today()
    with open('output/users.csv', 'w', newline='') as csv_file:
        writer = csv.writer(csv_file, delimiter='\t',
                            quotechar='|', quoting=csv.QUOTE_MINIMAL)
        for _ in range(NUMBER):
            name = fake.name()
            last, first, patronymic = name.replace('г-н ', '').replace('тов. ', '').replace('г-жа ', '').split(" ")
            writer.writerow([generate_snils(),
                             fake.ascii_email(),
                             last,
                             first,
                             patronymic,
                             fake.phone_number(),
                             fake.date_between(start_date=today.replace(month=1, day=1, year=today.year - 2),
                                               end_date=today)])


main()
