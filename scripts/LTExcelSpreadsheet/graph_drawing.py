import math
import os
import random
import string
from enum import Enum

import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np

RESPONSE_CODE = 'responseCode'
LABEL = 'label'
LATENCY = 'Latency'
TIME = 'time'


class TimeUnit(Enum):
    MILLISECONDS = "мс", 0.1,
    SECONDS = "с", 1,
    MINUTES = "мин", 60,
    HOURS = "ч", 60 * 60

    def __init__(self, symbol: str, divider: int) -> None:
        self.symbol = symbol
        self.divider = divider


TIME_UNIT = TimeUnit.MINUTES


def draw_delimiters(p, delimiters, start=0, color='#000000', vertical=True):
    if vertical:
        for delimiter in delimiters:
            p.axvline(x=start + delimiter, color=color)
    else:
        for delimiter in delimiters:
            p.axhline(y=delimiter, color=color)


def draw_title(p, title):
    p.title(title, fontsize=80)


def draw_ticks(p, x_start, x_end, x_tick, y_start, y_end, y_tick):
    size = 30
    p.xticks(np.arange(x_start, x_end, x_tick), size=size)
    if y_start != y_end and y_tick != 0:
        p.yticks(np.arange(y_start, y_end, y_tick), size=size)
    else:
        p.yticks(size=size)


def draw_labels(p, x_label, y_label):
    size = 60
    p.xlabel(x_label, fontsize=size)
    p.ylabel(y_label, fontsize=size)


def save_to_file(p, profile, scenario, scenario_name):
    graph_folder = 'graphs'
    if not os.path.exists(graph_folder):
        os.mkdir(graph_folder)
    if not os.path.exists(os.path.join(graph_folder, profile)):
        os.mkdir(os.path.join(graph_folder, profile))
    if scenario:
        scenario = scenario.strip()
        if not os.path.exists(os.path.join(graph_folder, profile, scenario)):
            os.mkdir(os.path.join(graph_folder, profile, scenario))
    if '?' in scenario_name:
        index = scenario_name.index('?')
    else:
        index = len(scenario_name)
    filename = scenario_name.replace('/', ' ')[:index].strip()
    if scenario:
        path = os.path.join(graph_folder, profile, scenario, filename + '.jpg')
    else:
        path = os.path.join(graph_folder, profile, filename + '.jpg')
    p.savefig(path)
    p.close()


def graph_latency(
        data_frame,
        graph_title,
        profile, scenario,
        file_name,
        v_delimiters=[],
        h_delimiters=[],
        x_tick=10,
        y_tick=5000,
        x_start=0,
        time_unit=TIME_UNIT,
        y_axis_format="%.0f"
):
    """
    Строит график по атрибуту Latency. Распределение на успешные коды зафиксировано статусом 200.
    Расширить, если дополнительно ожидаем 201.
    :param time_unit: единица измерения времени на оси X. По умолчанию берет значение глобальной переменной TIME_UNIT
    :param data_frame: data frame с данными мониторинга;
    :param graph_title: название графика;
    :param v_delimiters: x-координаты вертикальных линий для разграничения этапов НТ_скрипта;
    :param x_tick: деления по оси x;
    :param y_tick: деления по оси y;
    :param x_start: начало графика по оси x;
    :param y_axis_format: формат подписей по оси y;
    """
    non_num_responses = list(
        filter(lambda a: isinstance(a, str), data_frame.dropna()[RESPONSE_CODE].unique().tolist()))
    num_only_items = data_frame[~data_frame[RESPONSE_CODE].isin(non_num_responses)]
    good = num_only_items[(num_only_items[RESPONSE_CODE] >= 200) & (num_only_items[RESPONSE_CODE] < 300)]
    bad = data_frame[~data_frame.isin(good)]

    plt.subplots(figsize=(80, 30))
    g1 = plt.scatter(good[TIME] / time_unit.divider, good[LATENCY], alpha=0.5, color='#00ff44')
    g2 = plt.scatter(bad[TIME] / time_unit.divider, bad[LATENCY], alpha=0.5, marker='v', color='#ff0015')

    a = plt.gca()
    a.yaxis.set_major_formatter(ticker.FormatStrFormatter(y_axis_format))

    draw_delimiters(plt, start=x_start, delimiters=v_delimiters)
    draw_delimiters(plt, delimiters=h_delimiters, vertical=False)
    draw_title(plt, 'Latency, ' + graph_title)
    if x_tick != 0:
        x_tick = math.ceil(x_tick / time_unit.divider)
    else:
        x_tick = 0.1
    x_end = data_frame[TIME].max() / time_unit.divider
    draw_ticks(plt, x_start=x_start, x_end=x_end, x_tick=x_tick,
               y_start=0, y_end=data_frame[LATENCY].max() * 1.1, y_tick=y_tick)
    draw_labels(plt, x_label="Время от начала теста, " + time_unit.symbol, y_label="Latency, мс")

    plt.legend(
        (g1, g2),
        ('Успешные статус коды', 'Остальные'),
        scatterpoints=3,
        loc='upper left',
        ncol=1,
        fontsize=40,
        markerscale=4.
    )
    plt.grid()
    save_to_file(plt, profile.strip(), scenario, file_name.strip())
