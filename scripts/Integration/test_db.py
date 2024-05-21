import argparse

import allure
import psycopg2
import pytest


@pytest.fixture()
def conn(pytestconfig):
    return psycopg2.connect(host=pytestconfig.getoption("host"),
                            database=pytestconfig.getoption("database"),
                            user=pytestconfig.getoption("username"),
                            password=pytestconfig.getoption("password"))


@allure.description('Нет физических лиц с двумя одинаковыми outerId')
def test_1(conn):
    cursor = conn.cursor()
    cursor.execute(
        "select outer_id, rw from (select outer_id, row_number() over (partition by outer_id) rw from person ) t where t.rw <> 1;")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У разных физических лиц нет одинаковых account.login (т.е. нет двух повторющихся логинов)')
def test_2(conn):
    cursor = conn.cursor()
    cursor.execute(
        """select login, rw
            from (
                     select login,
                            row_number() over (partition by login) rw
                     from account         ) t
            where t.rw <> 1;
        """
    )
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('Нет сотрудников с совпадающими outerId')
def test_5(conn):
    cursor = conn.cursor()
    cursor.execute("""select outer_id, rw
    from (
             select outer_id,
                    row_number() over (partition by outer_id) rw
             from employee e2         ) t
    where t.rw <> 1;
""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У сотрудника есть дата приема на работу')
def test_6(conn):
    cursor = conn.cursor()
    cursor.execute("select *  from employee_position where start_work_date is null;")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У сотрудника есть привязка к записи физического лица - person')
def test_7(conn):
    cursor = conn.cursor()
    cursor.execute("select * from employee where person_outer_id is null")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У сотрудника стоит дата увольнения (end_work_date), если все его назначения являются закрытыми   '
                    '                                                                                                 '
                    '                   1) Проверяем, что нет открытых записей employee (с end_work_date is null), '
                    'если существует незакрытые назначения (employee_position.end_work_date is not null)  ')
def test_8(conn):
    cursor = conn.cursor()
    cursor.execute("""SELECT     e.tab_id "e.tab_id",
        e.end_work_date "e.end_work_date",
        e.outer_id "e.outer_id",
        e_p.employee_emb_end_work_date "e_p.employee_emb_end_work_date",
                e_p.start_work_date "e_p.start_work_date"
FROM employee AS e
JOIN employee_position AS e_p ON e.outer_id = e_p.employee_emb_outer_id
WHERE e.end_work_date IS NULL AND e_p.employee_emb_end_work_date IS NOT NULL and 
(e_p.employee_emb_outer_id, e_p.start_work_date) IN(
    SELECT e_p.employee_emb_outer_id,  MAX(e_p.start_work_date)
    FROM employee_position e_p
    GROUP BY e_p.employee_emb_outer_id
)
ORDER BY e.tab_id;""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('Если у физ лица стоит дата увольнения (employee_emb_end_work_date), то у всех назначений должны '
                    'быть проставлены даты закрытия (end_work_date) ')
def test_9(conn):
    cursor = conn.cursor()
    cursor.execute("""SELECT * FROM employee_position ep
WHERE employee_emb_end_work_date IS NOT NULL
and end_work_date is null 
""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('Если у физ лица стоит дата увольнения (employee_emb_end_work_date), то у всех назначений должны '
                    'быть проставлены даты закрытия (end_work_date) ')
def test_10(conn):
    cursor = conn.cursor()
    cursor.execute("""select * from employee_position ep 
where employee_emb_end_work_date is not null and end_work_date is null """)
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('Записи назначений в рамках одного табельного номера/физ лица с привязкой к одной должности, '
                    'одному подразделению и с одной ставкой должны схлапываться в одну запись. Не должно быть в '
                    'рамках одного физ лица (person), одного сотрудника (employee), одного подразделения, '
                    'одной должности и одной ставки (rate) нескольких записей назначений (employee_position). ')
def test_12(conn):
    cursor = conn.cursor()
    cursor.execute("""select employee_emb_person_outer_id, employee_emb_outer_id, org_unit_outer_id, position_outer_id, rate
from employee_position 
where start_work_date != start_work_date and end_work_date != end_work_date
group by employee_emb_person_outer_id, employee_emb_outer_id, org_unit_outer_id, position_outer_id, rate
having count (*) > 1
order by employee_emb_person_outer_id;""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У записи назначения есть табельный номер (если табельного номера нет, то необходимо определить '
                    'причину его отсутствия)')
def test_15(conn):
    cursor = conn.cursor()
    cursor.execute("select * from employee_position where number is null")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У сотрудника есть значение ставки (если ставки нет, то необходимо определить причину ее '
                    'отсутствия)')
@allure.issue(url='https://jira.goodt.me/browse/ABCHR-3629', name='ABCHR-3629')
def test_16(conn):
    cursor = conn.cursor()
    cursor.execute("select * from employee_position where rate is null")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У сотрудника есть дата назначения (если даты назначения нет, то необходимо определить причину ее '
                    'отсутствия)')
def test_17(conn):
    cursor = conn.cursor()
    cursor.execute("select * from employee_position where start_work_date is null")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У сотрудника нет двух активных назначений в рамках одного табельного номера')
def test_18(conn):
    cursor = conn.cursor()
    cursor.execute("""select employee_outer_id, number
from employee_position
where (number, employee_outer_id) in 
(select number, employee_outer_id from employee_position where end_work_date is null group by number, employee_outer_id having count (*) >1)
""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У каждой employee_position есть ссылка на employee_id, position_id')
def test_20(conn):
    cursor = conn.cursor()
    cursor.execute("select * from employee_position where employee_outer_id is null or position_outed_id is null")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У каждой позиции есть внешний идентификатор - outerId')
def test_21(conn):
    cursor = conn.cursor()
    cursor.execute("select * from position where  outer_id is null")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У позиции есть position_code, position_org_unit_id, category, type, group для учитываемых типов '
                    'позиций согласно Учитываемые типы должностей ')
def test_23(conn):
    cursor = conn.cursor()
    cursor.execute("""select * from position where code is null or org_unit_type is null or position_category_name is null  or position_group_name is null or position_category_outer_id is null or position_category_calculation_mode is null
or position_type_name is null or position_type_outer_id is null;""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.issue(url='https://jira.goodt.me/browse/ABCHR-3763', name='ABCHR-3763')
@allure.description("Флаг “position_chief” в назначениях (employee_position) должен проставляться только сотрудникам "
                    "с наименованием должности “Начальник ОПС”")
def test_37(conn):
    cursor = conn.cursor()
    cursor.execute("""select * from employee_position where
position_target_group = 'true'
and
position_chief = 'false' and
position_name like '%Начальник%'
order by updated desc, employee_emb_outer_id, start_work_date;""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.issue(url='https://jira.goodt.me/browse/ABCHR-3763', name='ABCHR-3763')
@allure.description('Роль с типом “ROLE_MANAGER_NEW” (account_role.name) должна присваиваться только сотрудникам с '
                    'наименованием должности “Начальник ОПС" и “Заместитель')
def test_36_nach(conn):
    cursor = conn.cursor()
    cursor.execute("""select * from employee_position where
position_target_group = 'true'
and
position_manager = 'false'
 and
 position_name like '%Начальник%'
order by employee_emb_outer_id, start_work_date;""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.issue(url='https://jira.goodt.me/browse/ABCHR-3763', name='ABCHR-3763')
@allure.description('Роль с типом “ROLE_MANAGER_NEW” (account_role.name) должна присваиваться только сотрудникам с '
                    'наименованием должности “Начальник ОПС" и “Заместитель')
def test_36_zam(conn):
    cursor = conn.cursor()
    cursor.execute("""select * from employee_position where
position_target_group = 'true'
and
position_manager = 'false'
 and  position_name like '%Заместитель%' and position_target_group = 'true'
order by employee_emb_outer_id, start_work_date;""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('Роль сотрудника имеет привязку к подраделению по Индексу')
def test_35(conn):
    cursor = conn.cursor()
    cursor.execute("""select created, updated, org_unit_outer_id, length(org_unit_outer_id) as num
from account_role_org_unit aroui 
where length(org_unit_outer_id) = (select max(length(org_unit_outer_id)) from account_role_org_unit) and length(org_unit_outer_id) >6""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.issue(url='https://jira.goodt.me/browse/ABCHR-3270', name='ABCHR-3270')
@allure.description('У всех записей обязательно есть непустое значение в поле person_outer_id')
def test_34(conn):
    cursor = conn.cursor()
    cursor.execute("select * from schedule_request where person_outer_id is null")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('Дата окончания (end_date_time) отсутствия не может быть меньше даты начала отсутствия ('
                    'start_date_time)')
@allure.issue(url='https://jira.goodt.me/browse/ABCHR-3938', name='ABCHR-3938')
def test_33(conn):
    cursor = conn.cursor()
    cursor.execute("""select * from schedule_request where
start_date_time > end_date_time
""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У отсутствия отличаются даты его начала и окончания (start_date_time и end_date_time)')
@allure.issue(url='https://jira.goodt.me/browse/ABCHR-3938', name='ABCHR-3938')
def test_32(conn):
    cursor = conn.cursor()
    cursor.execute("""select * from schedule_request where
start_date_time = end_date_time
""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.issue(url='https://jira.goodt.me/browse/ABCHR-3270', name='ABCHR-3270')
@allure.description('Отсутствия должны создаваться только для активных назначений employee_position (которые не имеют '
                    'даты окончания end_work_date)')
def test_31(conn):
    cursor = conn.cursor()
    cursor.execute("""SELECT *
FROM schedule_request sr
JOIN employee_position ep ON ep.position_outer_id = sr.position_outer_id
WHERE ( ep.position_outer_id, ep.end_work_date)in(
SELECT  ep.position_outer_id, MAX(ep.end_work_date)
FROM employee_position ep
group by ep.position_outer_id)
and sr.start_date_time > ep.end_work_date and ep.end_work_date is not null and deleted is null 
order by ep.position_outer_id;
""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.issue(url='https://jira.goodt.me/browse/ABCHR-3270', name='ABCHR-3270')
@allure.description('Записи могут принимать только следующие значения: \nВ ежегодном отпуске - VACATION; \nв отпуске'
                    'по беременности и родам/в отпуске по уходу за ребенком - MATERNITY_LEAVE;\nкомандировка - '
                    'BUSINESS_TRIP;\n в учебном оплачиваемом отпуске/в учебном неоплачиваемом отпуске - TRAINING')
def test_30_1(conn):
    cursor = conn.cursor()
    cursor.execute("""select * from schedule_request
where type <> 'VACATION'
and type <> 'MATERNITY_LEAVE'
and type <> 'BUSINESS_TRIP'
and type <> 'TRAINING'; 
""")
    res = cursor.fetchall()
    assert len(res) == 0


def test_30_2(conn):
    cursor = conn.cursor()
    cursor.execute("""select ep.employee_emb_first_name, ep.employee_emb_last_name, ep.employee_outer_id, ep.rate, ep.period, er.rate, er.start_date, er.end_date from employee_position ep
inner join employee_rate er on ep.employee_outer_id = er.employee_outer_id
where ep.rate is null and ep.period > '2020-11-16' and er.rate is not null;
""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.issue(url='https://jira.goodt.me/browse/ABCHR-3270', name='ABCHR-3270')
@allure.description('В рамках одного employee_position_internal_id нет активных записей (то есть deleted не принимает '
                    'значение true) scheduleRequest с пересекающимися интервалами start_date_time и end_date_time ('
                    'когда по одному назначению сотрудника отпуска наслаиваются друг на друга)')
def test_29(conn):
    cursor = conn.cursor()
    cursor.execute("""SELECT     
                a.deleted,
                                b.deleted,
                a.tab_id "a.tab_id",
                                a.employee_position_internal_id,
        a.start_date_time,
        a.end_date_time,
        a.type,
        a.title,
        b.tab_id "b.tab_id", 
        b.start_date_time,
        b.end_date_time,
        b.type,
        b.title
FROM schedule_request a
JOIN schedule_request b ON a.employee_position_internal_id = b.employee_position_internal_id
WHERE a.deleted is null and b.deleted is null and a.tab_id != b.tab_id 
    AND (a.start_date_time >= b.start_date_time AND a.start_date_time <= b.end_date_time
         OR a.end_date_time >= b.start_date_time AND a.end_date_time <= b.end_date_time)
                 order by a.employee_position_internal_id;""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('У каждой записи scheduleRequest обязательно указаны значения в полях employee_outer_id, '
                    'employee_position_internal_id, position_outer_id, start_date_time, end_date_time')
def test_28(conn):
    cursor = conn.cursor()
    cursor.execute("""select * from schedule_request where
employee_outer_id is null or
employee_position_internal_id is null or
position_outer_id is null or
start_date_time is null or
end_date_time is null;""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.issue(url='https://jira.goodt.me/browse/ABCHR-3270', name='ABCHR-3270')
@allure.description('Отсутствую дубликаты записей, у которых совпадают значения в полях employee_outer_id, '
                    'employee_position_internal_id, position_outer_id, start_date_time и end_date_time')
def test_27(conn):
    cursor = conn.cursor()
    cursor.execute("""select 
        employee_outer_id,
        employee_position_internal_id,
        position_outer_id,
        start_date_time,
        end_date_time,
count (*) as "count"
from
        schedule_request
where deleted is null
group by
        (employee_outer_id,
        employee_position_internal_id,
        position_outer_id,
        start_date_time,
        end_date_time)
having 
        count (*) > 1
order by employee_outer_id;""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('В position нет записей с одинаковым outer_id')
def test_26(conn):
    cursor = conn.cursor()
    cursor.execute("""
select 
        outer_id,
count (*) as "count"
from
        position
group by
        outer_id 
having 
        count (*) > 1""")
    res = cursor.fetchall()
    assert len(res) == 0


@allure.description('В структуре сущности есть поле person_outer_id (раньше было employee_outer_id)')
def test_25(conn):
    cursor = conn.cursor()
    cursor.execute("select *  from account where person_outer_id is null")
    res = cursor.fetchall()
    assert len(res) == 0
