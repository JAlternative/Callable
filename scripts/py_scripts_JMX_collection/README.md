# README #

# Все скрипты ожидают имя файла без расширения в текущей папке проекта через переменную (пока так). #

Формат записи имён в тест-плане может меняться, это начальный вариант для теста функциональности скрипта.

1. Скрипт body_validate_argument_unquote.py убирает из json-тела лишние символы и ставит переносы, могут возникнуть 
проблемы при обработке невалидных json тел, некотоыре случаи могут быть и вовсе не учтены, параллельно декодирует все 
аргументы.

2. Скрипт params_to_args.py переносит параметры из path самплера в аргументы и декодирует их.

3. Скрипт params to udv.py создаёт переменные jmeter на основе уникальных значений url, protocol и port у самплеров,
после чего расставляет эти переменные в самплерах.

4. Скрипт rename_samplers.py именует самплеры на основе значения path, переменные джиметра вида ${x}
попадают в название как {x}, к названию так же прибавляется "<_" или "_>" в зависимости от метода (передаются данные/
вносятся изменения) - "<_" для get, options, head (дополню), "_>" для post,put,patch,delete.

5. Скрипт trees_validate.py - вспомогательный инструмент для записи самплеров с параметрами&телами - оставляет тело
и выносит параметры из названия в path.

6. Скрипт enumerate_samplers.py нумерует самплеры в порядке записи har-jmx (blazmeter), для переноса и корректной рас-
становки.

7. Скрипт name_params_to_args.py выносит параметры в аргументы из имени.
