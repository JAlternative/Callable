import datetime

from db_conn import conn_wfm, conn_intgr


#  запуск sql скрипта/запроса в ВФМ
def run_sql_wfm(sql_request):
    cursor = conn_wfm().cursor()
    cursor.execute(sql_request)
    return cursor.fetchall()


#  запуск sql скрипта/запроса в ПБД
def run_sql_pbd(sql_request):
    cursor = conn_intgr().cursor()
    cursor.execute(sql_request)
    return cursor.fetchall()


# получение последней записи импорта в БД ВФМ
def get_max_of_integrationcallresult():
    sql_request = """
    select i.id, i.success
    from integrationcallresult i
    order by i.id desc
    limit 1
    """  # запоминает последнюю запись импорта, чтобы исследовать запись импорту, созданную в тесте
    max_of_integrationcallresult = run_sql_wfm(sql_request)
    return max_of_integrationcallresult


# поиск оргЮнита по параметрам в ВФМ
def find_org_unit_in_wfm(unic_id, with_main_org_unit):
    if with_main_org_unit:
        main_org_unit_filter = "and o3.outerid = 'MainOrgUnit'"
    else:
        main_org_unit_filter = ''

    sql_request = """
                select * from organizationunit o
                join organizationunittype o2
                on o2.id = o.organizationunittype_id
                join organizationunit o3
                on o.parent_id = o3.id
                where o.outerid = '{0}'
                and o.name = '{0}'
                and o.datefrom = '1970-01-01'
                and o2.outer_id = '5'
                {1}
                """.format(unic_id, main_org_unit_filter)
    res = run_sql_wfm(sql_request)  # получаем оргЮнит по параметрам из запроса
    return res


def is_import_res_succes(import_id):
    sql_request = """
                select ievent.message from integrationevent ievent
                join integrationcallresult icall
                on ievent.integrationcallresult_id = icall.id
                where icall.id = {0}
                """.format(import_id)
    res = run_sql_wfm(sql_request)
    return res


def find_ep_in_wfm(unic_id, is_valid_org_unit, period_from='2023-01-01', period_to=None):
    if is_valid_org_unit:
        org_unit_id = "and o.outerid = 'b471e2cd-9af3-11e8-80da-42f2e9dc7849'"
    else:
        org_unit_id = ''

    if period_to is not None:
        period_to = f"= '{period_to}'"
    else:
        period_to = 'is null'

    sql_request = """
                select * from employeeposition ep
                join position p
                on p.id = ep.position_id
                join employee e
                on e.id = ep.employee_id
                join organizationunit o
                on o.id = p.organizationunit_id
                where ep.card_number = '{0}'
                and ep.startdate = '{3}'
                and ep.enddate {1}
                and e.outerid = '{0}'
                and e.lastname = '{0}'
                and p.outerid = '{0}'
                {2}
                """.format(unic_id, str(period_to), org_unit_id, period_from)
    res = run_sql_wfm(sql_request)
    return res


def find_sr_in_wfm(unic_id, is_ep_valid):
    if is_ep_valid:
        employee_outer_id = "and e.outerid = '{0}'".format(unic_id)
        position_outer_id = "and p.outerid = '{0}'".format(unic_id)
    else:
        employee_outer_id = ''
        position_outer_id = ''
    sql_request = f"""
                select * from schedule_request sr
                join employee e
                on e.id = sr.employee_id
                join position p
                on p.id = sr.position_id
                join schedule_request_alias sra
                on sra.id = sr.alias_id
                where sr.startdatetime = '{str(datetime.date.today().replace(day=1))} 00:00:00.000'
                and sr.enddatetime = '{str(datetime.date.today().replace(day=10))} 23:59:00.000'
                {employee_outer_id}
                {position_outer_id}
                and sra.outer_id = '750873459435863200'
                """
    return run_sql_wfm(sql_request)


def find_removed_entity_in_wfm(unic_id, data_type):
    if data_type == 'EMPLOYEE_POSITION':
        start_date = "2023-01-01"
        end_date = "is null"
    else:
        start_date = datetime.date.today().replace(day=1)
        end_date = f"= '{datetime.date.today().replace(day=10)}'"

    sql_request = """
                select * from log_jremoved
                where employee_outer_id = '{0}'
                and position_outer_id = '{0}'
                and start_date = '{1}'
                and end_date {2}
                and type = '{3}'
                """.format(unic_id, start_date, end_date, data_type)
    return run_sql_wfm(sql_request)


def get_max_of_schedule_request():
    sql_request = """
    select id
    from schedule_request
    order by id desc
    limit 1
    """
    return run_sql_wfm(sql_request)


def find_kpi_in_wfm(kpi_id, org_unit_id):
    if kpi_id:
        kpi_id = 'limit2'
    else:
        kpi_id = 'amoguskpi'

    if org_unit_id:
        org_unit_id = 'b471e2cd-9af3-11e8-80da-42f2e9dc7849'
    else:
        org_unit_id = 'amoguskpi'

    sql_request = """
    select datetime, value, kpi_id, organizationunit_id
    from kpibasevalue
    join kpi
    on kpi.id = kpibasevalue.kpi_id
    join organizationunit o
    on o.id = kpibasevalue.organizationunit_id
    where kpi.outerid = '{0}'
    and o.outerid = '{1}'
    and kpibasevalue.value = {2}
    """.format(kpi_id, org_unit_id, int(str(datetime.date.today().day)+'0'))
    return run_sql_wfm(sql_request)


def find_role_in_wfm(user_id, valid_org_unit, period_from='2023-01-01', period_to=None, role_name='ROLE_USER_NEW'):
    if valid_org_unit:
        org_unit_id = 'b471e2cd-9af3-11e8-80da-42f2e9dc7849'
    else:
        org_unit_id = 'amoguskpi'

    if period_to is not None:
        period_to = f"= '{period_to}'"
    else:
        period_to = f"is null"

    sql_request = """
    select * from externaluser e 
    join "user" u 
    on e.id = u.id 
    join org_unit_role our 
    on our.user_id = u.id 
    join "role" r 
    on r.id = our.role_id 
    join org_unit_role_org_unit_link o
    on o.org_unit_role_id = our.id
    join organizationunit o2 
    on o.org_unit_id = o2.id
    where u.username = '{0}'
    and r.system_code = '{1}'
    and our.date_from = '{2}'
    and our.date_to {3}
    and o2.outerid = '{4}'
    """.format(user_id, role_name, period_from, period_to, org_unit_id)

    return run_sql_wfm(sql_request)