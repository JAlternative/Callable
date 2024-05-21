# Что это?
Скрипты, которые дергают информацию из Редмайна по разным критериям и формируют отчеты по заданной форме 
на основе полученной информации.

На момент написания тут два скрипта, написанные по задачам [60065](https://redmine.goodt.me/issues/60065) и 
[71628](https://redmine.goodt.me/issues/71628), лежат в файлах `RTL_report.py` и `KPI_report.py` соответственно.
Первый на данный момент не актуален, т.к. заказчик нашел способ получать нужные отчеты средствами Редмайна, но я не стала 
удалять наработки, т.к. они могут пригодиться при дальнейшей автоматизации отчетов.

# Запуск скриптов
Оба скрипта принимают аргументы из командной строки. Список обязательных аргументов:
* --date_from
* --date_to
* --login
* --password

После этих четырех должен быть аргумент `kpi` или `rtl` (в коде называется `mode`) в зависимости от желаемого типа отчета. 
Выбранный тип диктует дальнейшие обязательные параметры. В случае `rtl` это:
* --modules 
* --testing_statuses 
* --non_testing_statuses 
* --testers

* В случае `kpi` это:
* --testers

Обратите внимание: в --testers в случае `rtl` надо передавать список юзернеймов, а в случае `kpi` - список id пользователей.  

Пример запуска:
```python KPI_report.py --login=e.schastlivaya --password=******* --date_from=2023-04-01 --date_to=2023-04-30 kpi --testers=155;355;397;420;421;423;430;438;439;442;444;446;512;513;514```

# Реализация
Все вызовы в апи Редмайна лежат в файле `api_caller.py`. В нем используется библиотека [python-redmine](https://python-redmine.com/).
Каждая функция там равна одному запросу в апи.

В файле `utils.py` содержится все вспомогательные функции. Там же лежит класс `Config`, который является по сути оберткой
для `argparse`. Большая часть логики методов этого класса актуальна только для `RTL_report.py`.

Файлы `KPI_report.py` и `RTL_report.py` содержат основные скрипты, которые делегируют работу функциям из `utils`.

# Возможные улучшения
На данный момент функционал выбора скрипта реализован не самым изящным образом: при необходимости запустить скрипт RTL, 
надо запускать файл `RTL_report.py` (а не `KPI_report.py`, как в примере запуска выше), что делает 
параметр `mode`  лишним. 
Сейчас он позволяет только переключаться между парсерами аргументов (для разных скриптов настроены разные), 
но вполне можно сделать единую точку входа для всех скриптов: например, единый `main.py`, который будет запущен, проанализирует 
значение параметра `mode` и в зависимости от него делегирует работу соответствующему скрипту).

Навести порядок в `utils`. Поскольку `KPI_report` я делала незадолго до увольнения, у меня не было времени вдумчиво 
включить нужный для этого отчета код в код, написанный для `RTL-report`. Возможно, класс `Config`  надо вынести в 
отдельный файл и порефакторить функции, отвечающие за запись в файл. 