import configparser
import re

from graph_drawing import graph_latency

RESPONSE_CODE = 'responseCode'
LABEL = 'label'
LATENCY = 'Latency'
FRONT = 'front'
BACK = 'back'
NAME = 'name'
ID = 'id'
SHOW_NAME = 'show_name'


def read_config():
    p = configparser.RawConfigParser()
    p.read('config', encoding='utf-8')
    return p


def as_text(value):
    if value is None:
        return ""
    return str(value)


def convert_code(code):
    try:
        return int(code)
    except ValueError:
        return code


def get_data_for_page(parser, profile, scenario, front: bool, df, latency):
    requests = df[df['URL'].notnull()]
    if front is None:
        scenario_requests_df = requests
    else:
        if front:
            replacement = 'F'
        else:
            replacement = 'B'
        scenario_requests_df = requests[requests[LABEL].str.fullmatch(parser['REGEX']['request'].replace('[FB]', replacement))]
    result = []
    for r in scenario_requests_df[LABEL].unique():
        request_df = scenario_requests_df[scenario_requests_df[LABEL] == r]
        if not len(request_df):
            print(f"Нет данных: {scenario}-{front}, \"{r}\"")
            continue
        if latency:
            result.append(get_report_line_for_latency(request_df, r))
            draw_latency_for_request(request_df, r, profile, scenario)
        else:
            result.extend(get_report_line_for_status_code(request_df, r))
    return result


def draw_latency_for_request(request_df, label, profile, scenario):
    if not request_df.empty:
        graph_latency(request_df, label,
                      profile,
                      scenario,
                      file_name=label,
                      x_tick=int(request_df['time'].max() / 10),
                      y_tick=int(request_df[LATENCY].max() / 10))


def get_report_line_for_latency(request_df, label):
    return {'Label': label,
            'Min': request_df[LATENCY].min(),
            'Max': request_df[LATENCY].max(),
            'Median': request_df[LATENCY].median(),
            'Mean': request_df[LATENCY].mean()}


def get_report_line_for_status_code(df, label):
    temp_dict = {}
    result = []
    df_length = len(df)
    for i in df[RESPONSE_CODE].unique():
        temp = df[(df[RESPONSE_CODE] == i)]
        percent = 100 * len(temp) / df_length
        result.append({'Label': label,
                       'Code': i,
                       '%': percent,
                       'Count': len(temp),
                       'Total': df_length})
        temp_dict[i] = temp[LABEL].unique()
    return result


def get_suffix(latency: bool, front: bool, parser, scenario, profile):
    ws_data_suffix, link_data_name = get_latency_strings(latency)
    if scenario:
        ws_area_suffix, link_area_name = get_front_back_strings(front)
        sheet_name = f"{parser[profile]['short_name']}_{parser[scenario]['short_name']}_{ws_area_suffix}_{ws_data_suffix}"
    else:
        sheet_name = f'{parser[profile].name}_{ws_data_suffix}'
    result = {'profile': profile,
              'sheet': sheet_name}
    if scenario:
        result[SHOW_NAME] = f"{profile}. {parser[scenario][NAME]}, {link_area_name} ({link_data_name})"
        result['scenario'] = scenario,
        result[ID] = f"{parser[scenario]['short_name']}_{ws_area_suffix}_{ws_data_suffix}"
    else:
        result[SHOW_NAME] = sheet_name
    return result


def get_latency_strings(latency: bool):
    if latency:
        ws_data_suffix = 'lat'
        link_data_name = "задержка"
    else:
        ws_data_suffix = 'resp'
        link_data_name = "статус-коды"
    return ws_data_suffix, link_data_name


def get_front_back_strings(front: bool):
    if front:
        ws_area_suffix = 'F'
        link_area_name = "фронт"
    else:
        ws_area_suffix = 'B'
        link_area_name = "бэк"
    return ws_area_suffix, link_area_name


def assemble_title_worksheet_data(profiles: list, scenarios: list, sheet_names: list, p):
    profile_names, durations, users, output_csvs, scenario_list_title, errors = ([] for _ in range(6))
    scenario_list, link_list = ({} for _ in range(2))
    for profile in profiles:
        profile_names.extend(["Номер сценария", profile[-1:], "", ""])
        durations.extend(["Продолжительность (с/мин)", p[profile]['duration'], "", ""])
        users.extend(["Количество пользователей", p[profile]['user_count'], "", ""])
        output_csvs.extend(["Название файла с данными", p[profile]['output_csv'], "", ""])
        scenario_list_title.extend(['Сценарий', 'Кол-во сценариев', 'Кол-во полностью успешных сценариев', ""])
        profile_links = [x for x in sheet_names if x['profile'] == profile]
        errors.extend([f"{profile} - запросы с ошибками", "", "", ""])
        for s in scenarios:
            try:
                scenario_list[s].extend([p[s][NAME], "", "", ""])
            except (KeyError, AttributeError):
                scenario_list[s] = [p[s][NAME], "", "", ""]
            items = [pl for pl in profile_links if pl['scenario'] == s]
            for item in items:
                try:
                    link_list[item[ID]].extend([item[SHOW_NAME], "", "", ""])
                except (KeyError, AttributeError):
                    link_list[item[ID]] = [item[SHOW_NAME], "", "", ""]

    title = [profile_names, durations, users, output_csvs, [], scenario_list_title]
    title.extend(scenario_list.values())
    title.append([])
    title.extend(link_list.values())
    title.append(errors)
    return title


def check_for_non_compliant_names(p, df):
    print("Запросы ниже не названы по правилам, описанным в конфиге посредством регулярных выражений и ключевых слова, "
          "и не будут включены в отчетность:")
    for label in df[LABEL].unique():
        line = df[df[LABEL] == label]
        if line.sample()['URL'].isnull().bool():
            if not line_matches_pattern_or_has_keywords(label, p['REGEX']['controller'], p['KEYWORDS']['controller']):
                print(label)
        else:
            if not line_matches_pattern_or_has_keywords(label, p['REGEX']['request'], p['KEYWORDS']['request']):
                print(label)


def line_matches_pattern_or_has_keywords(ln: str, regex_string, keywords_string):
    regex = regex_string.split('\n')
    keywords = keywords_string.split('\n')
    return any(re.fullmatch(rg, ln) for rg in regex) or any(kw in ln for kw in keywords)
