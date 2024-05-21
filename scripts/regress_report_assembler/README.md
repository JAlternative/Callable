### Что делает скрипт

- Скачивает результаты джобы, смотрит, какие тесты должны были пройти в ней и какие прошли.
- Собирает в csv данные в формате отчета о регрессе. В том же csv внизу выводит данные о том, какие тесты не были запущены (у них будет `NO_STATUS` в
  колонке `Статус`).  
  Отдельной категорией в том же csv идут тесты, которых в прогоне не должно было быть (их можно узнать по приписке `NO_TEST` в колонке `ID`).
- Парсит полученный csv файл, считает статистику прохождения тестов в прогоне и генерирует txt файл, представляющий собой отчёт по регрессу в разметке
  Confluence markup

Результат сохраняется в папке `output`.

### Как запустить

1. Прогнать [джобу](https://builder.goodt.me/view/Maintenance/job/SUPPORT_download_regress_test_files/),
   выставив в качестве параметра нужную регрессовую джобу.
2. Достать из консоли билда ссылку на архив, например:
   `Скачать по ссылке: https://repo.goodt.me/download-db/ice/2022-07-29_50_58_WFM_regress3.zip`
3. Открыть в терминале папку со скриптом.
4. Указать в файле config логин и пароль для доступа к файловому хранилищу, откуда будем скачивать архив.
5. Ввести команду `python main.py https://repo.goodt.me/download-db/ice/2022-07-29_50_58_WFM_regress3.zip`,
   заменив ссылку на актуальную из п.2.

### Как импортировать результат скрипта в Confluence

1. Найти в папке `output` сгенерированный скриптом txt файл, скопировать из него всё содержимое
2. Создать страницу в Confluence
3. Нажать на + ("Вставить прочий контент")
4. Выбрать в раскрывающемся списке `Разметка`
5. Убедиться, что в поле `Вставить` выбрано значение "Confluence wiki"
6. Нажать кнопку `Вставить`

> Примечания:
> 1. Необходимо поддерживать `config`  в актуальном состоянии: в нем хранятся таски, которые запускаются в конкретных джобах.
     Если список тасков меняется, его нужно обновить и в конфиге, чтобы получить наиболее точные результаты сравнения.
> 2. Если у ID тестов добавляется новый префикс (как "ABCHR", "SE" и т.д.), его нужно добавить в раздел `prefixes` файла `config`).
> 3. При открытии сформированного отчета в MS Excel может возникнуть проблема с парсингом: строки не будут разбиты на ячейки.
     В этом случае воспользуйтесь [данной инструкцией](https://webhelp.optimizely.com/latest/en/campaign/analytics/deep/csv-excel.htm).
     Четвертый шаг может оказаться опционален.
> 4. Если в колонке ID есть приписка `(NO_TAG)` это значит, что ID был взят из кода, а не из тега,
     проставленного средствами Allure. Такие ID могут быть выбраны неверно, поэтому рекомендуется проставлять
     над тестами аннотацию @Tag.
> 5. Если у теста выставлен 3 грейд, это значит, что грейд этого теста не был распознан (его нет ни в группах, ни в аллюровском Severity),
     либо это не тест вообще (упавшие before/after-методы попадают в отчет).