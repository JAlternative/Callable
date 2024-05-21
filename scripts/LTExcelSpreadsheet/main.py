import argparse
import os.path

import openpyxl
import pandas as pd

from utils import read_config, convert_code, get_suffix, assemble_title_worksheet_data, get_data_for_page, \
    check_for_non_compliant_names, draw_latency_for_request

RESPONSE_CODE = 'responseCode'
LABEL = 'label'
LATENCY = 'Latency'
TIME = 'time'
TIMESTAMP = 'timeStamp'

PARSER = read_config()
SHEET_NAMES = []


def prep_dataframe(profile):
    fields = [RESPONSE_CODE, LABEL, LATENCY, TIMESTAMP, 'URL']
    profile_df = pd.read_csv(os.path.join('csv', PARSER[profile]['output_csv'] + '.csv'),
                             sep=',', skipinitialspace=True, usecols=fields)
    profile_df[RESPONSE_CODE] = profile_df[RESPONSE_CODE].map(convert_code)

    min_value = profile_df[TIMESTAMP].min()
    profile_df[TIME] = profile_df[TIMESTAMP].map(lambda time_stamp: (time_stamp - min_value) / 1000)
    return profile_df


def write_workbook(scope, mode):
    workbook_name = f'LT report_{scope}.xlsx'
    if not os.path.exists('xlsx'):
        os.mkdir('xlsx')
    workbook_path = os.path.join('xlsx', workbook_name)
    writer = pd.ExcelWriter(workbook_path, engine='xlsxwriter')
    pd.DataFrame().to_excel(writer, sheet_name=scope)
    codes = pd.DataFrame([])
    profiles = [x for x in PARSER.sections() if x == scope]
    scenarios = [x for x in PARSER.sections() if PARSER.get(x, 'scope', fallback=None) and PARSER[x]['scope'] == scope]
    for profile in profiles:
        profile_df = prep_dataframe(profile)
        if mode == 'lazy':
            codes = add_ws_and_compute_status_codes(profile, None, writer, profile_df, None, codes)
        else:
            check_for_non_compliant_names(PARSER, profile_df)
            for s in scenarios:
                scenario_number = PARSER[s]['number']
                scenario_df = profile_df[(profile_df[LABEL].str.fullmatch(PARSER['REGEX']['controller'].split('\n')[0])) &
                                         (profile_df[LABEL].str.startswith(f"{scenario_number} "))]
                draw_latency_for_request(scenario_df, s, profile, s)
                scenario_requests_df = profile_df[profile_df[LABEL].str.startswith(f"{scenario_number}_")]
                codes = add_ws_and_compute_status_codes(profile, s, writer, scenario_requests_df, False, codes)
                codes = add_ws_and_compute_status_codes(profile, s, writer, scenario_requests_df, True, codes)
        write_error_worksheet(codes, writer, profile)
    writer.close()
    title = assemble_title_worksheet_data(profiles, scenarios, SHEET_NAMES, PARSER)
    wb = openpyxl.load_workbook(workbook_path)
    ws = wb[scope]
    for row in title:
        ws.append(row)
    wb.save(workbook_path)


def add_ws_and_compute_status_codes(profile, s, writer, profile_df, front, codes):
    add_worksheet(profile, s, writer, latency=True, front=front, profile_df=profile_df)
    return pd.concat(
        [add_worksheet(profile, s, writer, latency=False, front=front, profile_df=profile_df), codes])


def write_error_worksheet(codes, writer, profile):
    non_num_responses = list(filter(lambda a: isinstance(a, str), codes.dropna()['Code'].unique().tolist()))
    num_error_only = codes[(~codes['Code'].isin(non_num_responses))]
    num_error_only = num_error_only[num_error_only['Code'] >= 400]
    non_num_errors = codes[codes['Code'].isin(non_num_responses)]
    codes = pd.concat([num_error_only, non_num_errors])
    codes.to_excel(writer, sheet_name=f"{profile}_errors", index=False)


def add_worksheet(profile, scenario, writer, latency: bool, front: bool, profile_df):
    data = pd.DataFrame(get_data_for_page(PARSER, profile, scenario, front, profile_df, latency))
    if data.empty is False:
        sheet_name_dict = get_suffix(latency, front, PARSER, scenario, profile)
        SHEET_NAMES.append(sheet_name_dict)
        data.to_excel(writer, sheet_name=sheet_name_dict['sheet'], index=False)
    return data


parser = argparse.ArgumentParser()
parser.add_argument("--mode", help="Режим работы: free (составляет для всех запросов в файле, "
                                   "требует минимально заполнения конфига) "
                                   "или strict (максимально подробный конфиг, запросы названы по регулярке из него же)",
                    required=True)
parser.add_argument("--profile", help="Названия профилей нагрузки из конфига. Если их несколько, надо разделить их ;",
                    required=True)
parser = parser.parse_args()

for profile_name in parser.profile.split(";"):
    write_workbook(profile_name, parser.mode)
