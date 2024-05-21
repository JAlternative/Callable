# coding=utf-8
import argparse
import sys
from datetime import datetime

import pandas as pd

APPLY_TEMPLATE_DATES = 'template applications dates'
EXCEPTIONS = 'exceptions'

ARGS = None
ERROR_MESSAGE = 'Выполнение скрипта прервано. Проверьте правильность введенного значения "{}" в параметре "{}" и ' \
                'повторите попытку. '


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("-l", help="Логин ВКП", required=True)
    parser.add_argument("-p", help="Пароль ВКП", required=True)
    parser.add_argument("-t", help="Название шаблона. Он должен быть заранее создан через UI ВКП", required=True)
    parser.add_argument("-d", help="Период, к которому нужно применить шаблон. Начало и конец периода должны быть "
                                   "разделены двоеточием. Формат даты: ГГГГ-ММ-ДД. Если в датах указанного периода "
                                   "уже стоят какие-то отметки, они будут утеряны", required=True)
    parser.add_argument("-e", help="Даты-исключения. Формат даты: ГГГГ-ММ-ДД. Возможно ввести несколько дат и "
                                   "периодов через пробел. При указании периода разделите даты начала и конца периода "
                                   "двоеточием.")
    global ARGS
    ARGS = parser.parse_args()


def get_credentials():
    parse_args()
    return ARGS.l, ARGS.p


def get_template():
    return ARGS.t


def get_dates():
    dates = ARGS.d
    try:
        start, end = dates.split(':')
    except ValueError:
        sys.exit(ERROR_MESSAGE.format(dates, APPLY_TEMPLATE_DATES))
    else:
        print("Шаблон будет применен с {} по {}".format(start, end))
        return pd.date_range(start=start, end=end).to_pydatetime().tolist()


def get_exceptions():
    exceptions = ARGS.e
    if not exceptions:
        print("Дат-исключений не найдено. Шаблон будет применен ко всем датам, кроме выходных.")
        return []
    result = []
    if ' ' in exceptions:
        split = exceptions.split(' ')
        for s in split:
            checked = check_if_period(s)
            add_to_list(checked, result, s)
    else:
        checked = check_if_period(exceptions)
        add_to_list(checked, result, exceptions)
    print("Распознанные даты-исключения: {}".format(str(list(map(lambda d: d.strftime('%Y-%m-%d'), result)))))
    return result


def check_if_period(s):
    if ':' in s:
        start, end = s.split(':')
        try:
            return pd.date_range(start=start, end=end).to_pydatetime().tolist()
        except ValueError:
            sys.exit(ERROR_MESSAGE.format(s, EXCEPTIONS))
    else:
        return None


def add_to_list(checked, result, s):
    if checked:
        result.extend(checked)
    else:
        try:
            result.append(datetime.strptime(s, '%Y-%m-%d'))
        except ValueError:
            sys.exit(ERROR_MESSAGE.format(s, EXCEPTIONS))
