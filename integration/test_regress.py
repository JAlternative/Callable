import datetime

from sql_scripts import *
from send_requests import *
from create_unic_id import create_unic_id


# IntegrRegress - 1 - Создания оргЮнита напрямую в ВФМ
def test_org_unit():
    is_main_org_unit_valid = True

    unic_id = create_unic_id()

    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_org_unit(is_main_org_unit_valid, unic_id)

    assert '"success":true' in imprt_res.text

    assert '"callType":"ORGANIZATION_UNIT_IMPORT"' in imprt_res.text
    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]

    assert max_of_integrationcallresult_2[0][1] is True
    res = find_org_unit_in_wfm(unic_id, is_main_org_unit_valid)

    assert len(res) == 1


# IntegrRegress - 2 - Создание ошибочной записи оргЮнита при импорте напрямую в ВФМ (с привязкой к несуществующему вышестоящему подразделению)
def test_org_unit_error():
    is_main_org_unit_valid = False

    unic_id = create_unic_id()

    max_of_integrationcallresult = get_max_of_integrationcallresult()  # запоминает последнюю запись импорта, чтобы исследовать запись импорту, созданную в тесте

    imprt_res = create_org_unit(is_main_org_unit_valid, unic_id)

    assert '"logref":"error"' in imprt_res.text  # шаг 1 - верный ответ на запрос
    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()  # снова получаем самую новую запись импорта в таблице

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]

    assert max_of_integrationcallresult_2[0][1] is False  # шаг 3 - если сформированная запись неуспешна
    res = is_import_res_succes(max_of_integrationcallresult_2[0][0])  # получаем запись с месседжем об ошибке по нашей записи импорта

    assert res[0][0] == 'Parent Org Unit of outer id {0} was not found Stop-on-error is false'.format('amogusOrgUnit')  # шаг 4 если верный месседж в ошибке
    res = find_org_unit_in_wfm(unic_id, is_main_org_unit_valid)  # ищем оргЮнит в Бд по указанным параметрам

    assert len(res) == 0  # шаг 5 ошибочный оргЮнит не создан в БД


# IntegrRegress - 3 - Создание нового сотрудника и назначения напрямую в ВФМ
def test_create_ep():
    unic_id = create_unic_id()

    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_ep(is_org_unit_valid=True, unic_id=unic_id)

    assert '"success":true' in imprt_res.text

    assert '"callType":"EMPLOYEE_POSITION_FULL_IMPORT"' in imprt_res.text # шаг 1 - если получен верный ответ
    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()  # снова получаем самую новую запись импорта в таблице

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]

    assert max_of_integrationcallresult_2[0][1] is True
    res = find_ep_in_wfm(unic_id, is_valid_org_unit=True)  # ищем нашего сотрудника в БД по параметрам

    assert len(res) == 1  # если сотрудник сформирован в ПБД

    create_removed_entity(unic_id, data_type='EMPLOYEE_POSITION')


# IntegrRegress - 4 - Закрытие существующего назначения напрямую в ВФМ
def test_close_ep():
    unic_id = create_unic_id()

    create_ep(is_org_unit_valid=True, unic_id=unic_id)

    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_ep(unic_id=unic_id, is_org_unit_valid=True, period_to=str(datetime.date.today()))

    assert '"success":true' in imprt_res.text

    assert '"callType":"EMPLOYEE_POSITION_FULL_IMPORT"' in imprt_res.text  # шаг 1-получен верный ответ
    max_of_integrationcallresult_2 = get_max_of_integrationcallresult() # снова получаем самую новую запись импорта в таблице

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]  # шаг 2-создана запись импорта

    assert max_of_integrationcallresult_2[0][1] is True  # шаг 3 - запись импорта - успешно
    res = find_ep_in_wfm(unic_id, is_valid_org_unit=True, period_to=datetime.date.today())  # ищем наше закрытое назначение в БД по параметрам

    assert len(res) == 1  # шаг 4 - создано закрытое назначение в БД

    create_removed_entity(unic_id, data_type='EMPLOYEE_POSITION')


# IntegrRegress - 5 - Создание нового сотрудника и назначения, когда в ВФМ нет оргЮнита, для которого импортируется сотрудник напрямую в ВФМ
def test_create_ep_without_orgunit():

    unic_id = create_unic_id()

    # запоминает последнюю запись импорта, чтобы исследовать запись импорту, созданную в тесте
    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_ep(is_org_unit_valid=False, unic_id=unic_id)

    assert '"success":false' in imprt_res.text

    assert '"callType":"EMPLOYEE_POSITION_FULL_IMPORT"' in imprt_res.text  # шаг 1 - верный ответ
    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()  # снова получаем самую новую запись импорта в таблице

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]  # шаг 2 создана запись импорта

    assert max_of_integrationcallresult_2[0][1] is False
    res = is_import_res_succes(max_of_integrationcallresult_2[0][0])  # для нашей записи импорта запоминаем месседж об ошибке

    assert res[0][0] == 'Organization Unit of outer id amogusep was not found'.format(unic_id)  # шаг 3 верный месседж об ошибке
    res = find_ep_in_wfm(unic_id, is_valid_org_unit=False)  # ищем ошибочное назначение по параметрам в БД

    assert len(res) == 0


# IntegrRegress - 6 - Создание нового отсутствия напрямую в ВФМ
def test_create_sr():

    unic_id = create_unic_id()

    create_ep(unic_id=unic_id, is_org_unit_valid=True)

    # запоминает последнюю запись импорта, чтобы исследовать запись импорту, созданную в тесте
    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_sr(unic_id)

    assert '"success":true' in imprt_res.text

    assert '"callType":"SCHEDULE_REQUESTS_IMPORT"' in imprt_res.text  # шаг 1 - верный ответ
    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()  # снова получаем самую новую запись импорта в таблице

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]  # шаг 2 создана новая запись импорта

    assert max_of_integrationcallresult_2[0][1] is True  # шаг 3 если запись импорта - успешно
    res = find_sr_in_wfm(unic_id, is_ep_valid=True)  # ищем наше отсутствие в БД по параметрам

    assert len(res) == 1

    create_removed_entity(unic_id, data_type='SCHEDULE_REQUEST')
    create_removed_entity(unic_id, data_type='EMPLOYEE_POSITION')



# IntegrRegress - 7 - Создание нового отсутствия с привязкой к назначению, которого нет в ВФМ, прямой импорт в ВФМ
def test_create_sr_with_invalid_ep():

    unic_id = 'amogussr'

    # запоминает последнюю запись импорта, чтобы исследовать запись импорту, созданную в тесте
    max_of_integrationcallresult = get_max_of_integrationcallresult()

    max_of_sr = get_max_of_schedule_request()  # кол-во записей отсутствий до отправки запроса

    imprt_res = create_sr(unic_id)  # создаю отсутствие с невалидным сотрудником

    max_of_sr_2 = get_max_of_schedule_request()  # кол-во записей отсутствий после отправки запроса

    assert '"success":false' in imprt_res.text

    assert '"callType":"SCHEDULE_REQUESTS_IMPORT"' in imprt_res.text  # шаг 1 - верный ответ
    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()  # снова получаем самую новую запись импорта в таблице

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]  # шаг 2 создана новая запись импорта

    assert max_of_integrationcallresult_2[0][1] is False  # шаг 3 новая запись импорта - не успешно
    res = is_import_res_succes(max_of_integrationcallresult_2[0][0])  # для нашей записи импорта запоминаем месседж об ошибке

    assert res[0][0] == 'Position of the outer id {0} was not found'.format(unic_id)  # шаг 4 верный месседж об ошибке

    assert max_of_sr_2[0][0] == max_of_sr[0][0]  # шаг 5 отсутствие не создано


# IntegrRegress - 8 - Создание записи удаления по назначению, которое есть в ВФМ, прямой импорт в ВФМ
def test_removed_ep():
    unic_id = create_unic_id()

    create_ep(is_org_unit_valid=True, unic_id=unic_id)

    # запоминает последнюю запись импорта, чтобы исследовать запись импорту, созданную в тесте
    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_removed_entity(unic_id, data_type='EMPLOYEE_POSITION')

    assert '"success":true' in imprt_res.text

    assert '"callType":"REMOVED_OBJECTS"' in imprt_res.text  # шаг 1 верный ответ
    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()  # снова получаем самую новую запись импорта в таблице

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]  # шаг 2 создана запись импорта

    assert max_of_integrationcallresult_2[0][1] is True  # шаг 3 запись импорта - успешно
    res = find_removed_entity_in_wfm(unic_id, data_type='EMPLOYEE_POSITION')

    assert len(res) == 1  # шаг 4 создана запись удаления в БД
    res = find_ep_in_wfm(unic_id, is_valid_org_unit=True)

    assert len(res) == 0  # шаг 5 назначение удалено фактически в БД


# IntegrRegress - 9 - Создание записи удаления по отсутствию, которое есть в ВФМ, прямой импорт в ВФМ
def test_removed_sr():
    unic_id = create_unic_id()

    create_ep(is_org_unit_valid=True, unic_id=unic_id)

    create_sr(unic_id)

    # запоминает последнюю запись импорта, чтобы исследовать запись импорту, созданную в тесте
    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_removed_entity(unic_id, data_type='SCHEDULE_REQUEST')

    assert '"success":true' in imprt_res.text

    assert '"callType":"REMOVED_OBJECTS"' in imprt_res.text  # шаг 1 верный ответ
    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()  # снова получаем самую новую запись импорта в таблице

    assert max_of_integrationcallresult_2[0][0] == max_of_integrationcallresult[0][0] + 1  # шаг 2 создана новая запись импорта

    assert max_of_integrationcallresult_2[0][1] is True  # шаг 3 запись импорта - успешно
    res = find_removed_entity_in_wfm(unic_id, data_type='SCHEDULE_REQUEST')

    assert len(res) == 1  # шаг 4 создана запись удаления в БД
    res = find_sr_in_wfm(unic_id, is_ep_valid=True)

    assert len(res) == 0  # шаг 5 - отсутствие удалено в БД

    create_removed_entity(unic_id, data_type='EMPLOYEE_POSITION')


#  IntegrRegress - 10 - Создание записи удаления по назначению, которого нет в ВФМ, прямой импорт в ВФМ
def test_removed_invalid_ep():
    unic_id = create_unic_id()

    # запоминает последнюю запись импорта, чтобы исследовать запись импорту, созданную в тесте
    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_removed_entity(unic_id, data_type='EMPLOYEE_POSITION')  # отправляем запрос на удаление по указанным параметрам

    assert '"success":false' in imprt_res.text

    assert '"callType":"REMOVED_OBJECTS"' in imprt_res.text  # шаг 1 - проверка ответа на запрос
    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()  # снова получаем самую новую запись импорта в таблице

    assert max_of_integrationcallresult_2[0][0] == max_of_integrationcallresult[0][0] + 1  # Шаг 2 - проверка, что сформирована новая запись импорта в БД

    assert max_of_integrationcallresult_2[0][1] is False  # шаг 3 - сформированная запись импорта - неуспешно
    res = is_import_res_succes(max_of_integrationcallresult_2[0][0])

    # шаг 4 проверка верного сообщения об ошибке в БД  # Шаг 4 - в бд не создалось записи удаленной сущености по несуществ. назначению
    assert res[0][0] == 'There is no employee with outer id: {0}'.format(unic_id)
    res = find_removed_entity_in_wfm(unic_id, data_type='EMPLOYEE_POSITION')

    assert len(res) == 0  # Шаг 5 - в бд не создалось записи удаленной сущености по несуществ. назначению


# IntegrRegress - ??? - Повторная отправка, ранее загруженного отсутствия в ВФМ (не происходит дублирование записей)
def test_create_sr_double():

    unic_id = create_unic_id()

    create_ep(unic_id=unic_id, is_org_unit_valid=True)

    # запоминает последнюю запись импорта, чтобы исследовать запись импорту, созданную в тесте
    max_of_integrationcallresult = get_max_of_integrationcallresult()

    for _ in range(2):
        imprt_res = create_sr(unic_id)
        if '"success":true' not in imprt_res.text:
            break

    assert '"success":true' in imprt_res.text

    assert '"callType":"SCHEDULE_REQUESTS_IMPORT"' in imprt_res.text  # шаг 1 - верный ответ
    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()  # снова получаем самую новую запись импорта в таблице

    assert max_of_integrationcallresult[0][0] + 2 == max_of_integrationcallresult_2[0][0]  # шаг 2 создана новая запись импорта

    assert max_of_integrationcallresult_2[0][1] is True  # шаг 3 если запись импорта - успешно
    res = find_sr_in_wfm(unic_id, is_ep_valid=True)  # ищем наши отсутствия в БД по параметрам

    assert len(res) == 1  # шаг 4 если отсутствие не дублировалось

    create_removed_entity(unic_id, data_type='SCHEDULE_REQUEST')
    create_removed_entity(unic_id, data_type='EMPLOYEE_POSITION')


#  IntegrRegress - 11 - Создание записи удаления по отсутствию, которого нет в ВФМ, прямой импорт в ВФМ
def test_removed_invalid_sr():
    unic_id = create_unic_id()

    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_removed_entity(unic_id, data_type='SCHEDULE_REQUEST')

    res_json = imprt_res.json()

    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()

    assert res_json['success'] is False

    assert res_json['callType'] == 'REMOVED_OBJECTS'

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]

    assert max_of_integrationcallresult_2[0][1] is False
    res = is_import_res_succes(max_of_integrationcallresult_2[0][0])

    assert res[0][0] == 'There is no employee with outer id: {0}'.format(unic_id)
    res = find_removed_entity_in_wfm(unic_id, data_type='SCHEDULE_REQUEST')

    assert len(res) == 0


# IntegrRegress - 12 - Создание бизнес-драйвера (kpi) по интеграции, прямой импорт в ВФМ
def test_create_kpi():
    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_kpi(valid_kpi=True, valid_org_unit=True)

    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()

    # Нужно завести задачу на доработку
    #res_json = imprt_res.json()

    #assert res_json['success'] is True

    # какой ответ ожидать?
    # assert res_json['callType'] == 'Kpi'

    assert imprt_res.ok

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]

    assert max_of_integrationcallresult_2[0][1] is True
    res = find_kpi_in_wfm(kpi_id=True, org_unit_id=True)

    # вариант, когда сравниваем две даты
    #assert res[0][0] == datetime.datetime.today().replace(day=1, hour=0, minute=0, second=0, microsecond=0)

    # вариант, когда сравниваем две строки, получившиеся из дат
    assert str(res[0][0]) == str(datetime.date.today().replace(day=1)) + ' 00:00:00'

    assert int(res[0][1]) == int(str(datetime.date.today().day) + '0')

    assert res[0][2] == 16

    assert res[0][3] == 387

# IntegrRegress - 13 - Создание бизнес-драйвера на стороне ВФМ, с привязкой к подразделению, которого нет в ВФМ, прямой импорт в ВФМ
def test_kpi_invalid_orgunit():
    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_kpi(valid_kpi=True, valid_org_unit=False)

    # res_json = imprt_res.json()

    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()

    assert imprt_res.ok

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]

    assert max_of_integrationcallresult_2[0][1] is False

    assert is_import_res_succes(max_of_integrationcallresult_2[0][0]) == 'There is no org unit with outer id {0}'.format('amoguskpi')
    res = find_kpi_in_wfm(kpi_id=True, org_unit_id=False)

    assert len(res) == 0


# IntegrRegress - 14 - Создание бизнес-драйвера на стороне ВФМ, с привязкой к kpi.outerid, которого нет в ВФМ, прямой импорт в ВФМ
def test_kpi_invalid_kpi_id():
    max_of_integrationcallresult = get_max_of_integrationcallresult()

    imprt_res = create_kpi(valid_kpi=False, valid_org_unit=True)

    # res_json = imprt_res.json()

    max_of_integrationcallresult_2 = get_max_of_integrationcallresult()

    assert imprt_res.status_code == 400

    assert max_of_integrationcallresult[0][0] + 1 == max_of_integrationcallresult_2[0][0]

    assert max_of_integrationcallresult_2[0][1] is False

    assert is_import_res_succes(max_of_integrationcallresult_2[0][0]) == 'Entity with class Kpi not found by id {0}'.format('amoguskpi')
    res = find_kpi_in_wfm(kpi_id=False, org_unit_id=True)

    assert len(res) == 0


# IntegrRegress - 15 - Создание учетной записи и роли в ВФМ, прямой импорт в ВФМ
def test_create_role():
    unic_id = create_unic_id()

    create_ep(unic_id=unic_id, is_org_unit_valid=True)

    max_import_line = get_max_of_integrationcallresult()

    imprt_res = create_role(unic_id, valid_org_unit=True)

    max_import_line_2 = get_max_of_integrationcallresult()

    imprt_json = imprt_res.json()

    assert imprt_json['success'] is True

    assert imprt_json['callType'] == 'EXTERNAL_USER_IMPORT'

    assert max_import_line[0][0] + 1 == max_import_line_2[0][0]

    assert max_import_line_2[0][1] is True
    res = find_role_in_wfm(unic_id, valid_org_unit=True)

    assert len(res) == 1

    create_removed_entity(unic_id, data_type='EMPLOYEE_POSITION')


# IntegrRegress - 16 - Создание учетной записи и роли в ВФМ, для физ. лица, которого нет в ВФМ, прямой импорт в ВФМ
def test_create_role_invalid_ep():

    unic_id = create_unic_id()

    max_import_line = get_max_of_integrationcallresult()

    imprt_res = create_role(unic_id, valid_org_unit=True)

    max_import_line_2 = get_max_of_integrationcallresult()

    imprt_json = imprt_res.json()

    assert imprt_json['success'] is False

    assert imprt_json['callType'] == 'EXTERNAL_USER_IMPORT'

    assert max_import_line[0][0] + 1 == max_import_line_2[0][0]

    assert max_import_line_2[0][1] is False
    res = find_role_in_wfm(unic_id, valid_org_unit=True)

    assert len(res) == 0


# IntegrRegress - 17 - Закрытие учетной записи и роли в ВФМ, прямой импорт в ВФМ
def test_close_role():
    unic_id = create_unic_id()

    create_ep(unic_id=unic_id, is_org_unit_valid=True)

    max_import_line = get_max_of_integrationcallresult()

    imprt_res = create_role(unic_id, valid_org_unit=True, period_to=str(datetime.date.today()))

    max_import_line_2 = get_max_of_integrationcallresult()

    imprt_json = imprt_res.json()

    assert imprt_json['success'] is True

    assert imprt_json['callType'] == 'EXTERNAL_USER_IMPORT'

    assert max_import_line[0][0] + 1 == max_import_line_2[0][0]

    assert max_import_line_2[0][1] is True
    res = find_role_in_wfm(unic_id, valid_org_unit=True, period_to=datetime.date.today())

    assert len(res) == 1

    create_removed_entity(unic_id, data_type='EMPLOYEE_POSITION')


# IntegrRegress - 18 - Закрытие учетной роли в ВФМ при двух имеющихся ролях по физ лицу, прямой импорт в ВФМ
def test_close_role_having_2_roles():
    unic_id = create_unic_id()

    create_ep(unic_id=unic_id, is_org_unit_valid=True)
    create_role(unic_id, valid_org_unit=True)
    create_role(unic_id, valid_org_unit=True, role_name='ROLE_MANAGER_NEW')

    max_import_line = get_max_of_integrationcallresult()

    imprt_res = create_role(unic_id, valid_org_unit=True, role_name='ROLE_MANAGER_NEW', period_to=str(datetime.date.today()))
    imprt_json = imprt_res.json()

    max_import_line_2 = get_max_of_integrationcallresult()

    assert imprt_json['success'] is True

    assert imprt_json['callType'] == 'EXTERNAL_USER_IMPORT'

    assert max_import_line[0][0] + 1 == max_import_line_2[0][0]

    assert max_import_line_2[0][1] is True
    res = find_role_in_wfm(unic_id, valid_org_unit=True)
    res1 = find_role_in_wfm(unic_id, valid_org_unit=True, period_to=datetime.date.today(), role_name='ROLE_MANAGER_NEW')

    assert len(res) == 1 and len(res1) == 1

    create_removed_entity(unic_id, data_type='EMPLOYEE_POSITION')
