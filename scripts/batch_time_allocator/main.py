# coding=utf-8
import config_handler as config
import api_caller as api


def assign_time_to_dates(dates, exceptions, template_name):
    template_id = api.get_template_id(template_name)
    for date in dates:
        if date.weekday() not in [5, 6] and date not in exceptions:
            str_date = date.strftime('%x (%A)')
            if api.assign_time(template_id, date):
                print('К дате {} был применен шаблон "{}".'.format(str_date, template_name))
            else:
                print('Не удалось применить шаблон "{}" к дате {}.'. format(template_name, str_date))


def main():
    api.auth()
    template_name = config.get_template()
    dates = config.get_dates()
    exceptions = config.get_exceptions()
    assign_time_to_dates(dates, exceptions, template_name)


main()






