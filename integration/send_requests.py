from API_wrapper import *


def create_org_unit(is_main_org_unit_valid, unic_id):
    if is_main_org_unit_valid:
        main_org_unit_id = 'MainOrgUnit'
    else:
        main_org_unit_id = 'amogusOrgUnit'

    url = create_api('org-units')
    body = create_org_unit_json(unic_id, main_org_unit_id)
    query_params = {}

    imprt_res = send_post_api(url, body, query_params)
    return imprt_res


def create_ep(is_org_unit_valid, unic_id, period_from='2023-01-01', period_to=None):
    if is_org_unit_valid:
        org_unit_id = "b471e2cd-9af3-11e8-80da-42f2e9dc7849"
    else:
        org_unit_id = 'amogusep'

    url = create_api('employee-positions-full')
    body = create_ep_json(unic_id, org_unit_id, period_from, period_to)
    query_params = {'concrete-dates': 'true', 'process-shifts': 'delete', 'start-date-shift-filter': 'true'}

    imprt_res = send_post_api(url, body, query_params)
    return imprt_res


def create_sr(unic_id):

    url = create_api('schedule-requests')
    body = create_sr_json(unic_id)
    query_params = {'stop-on-error': 'false', 'delete-intersections': "true", 'split-requests': 'true', 'process-shifts': 'delete', 'start-date-shift-filter': 'true'}

    imprt_res = send_post_api(url, body, query_params)
    return imprt_res


def create_removed_entity(unic_id, data_type):
    if data_type == 'EMPLOYEE_POSITION':
        start_date = '2023-01-01'
        end_date = None
    else:
        start_date = str(datetime.date.today().replace(day=1))
        end_date = str(datetime.date.today().replace(day=10))

    url = create_api('removed')
    body = create_removed_entity_json(unic_id, data_type, start_date, end_date)
    query_params = {'stop-on-error': 'false', 'open-prev-employee-position': 'false'}

    imprt_res = send_post_api(url, body, query_params)
    return imprt_res


def create_kpi(valid_kpi, valid_org_unit):
    if valid_kpi:
        kpi_id = 'limit2'
    else:
        kpi_id = 'amoguskpi'

    if valid_org_unit:
        org_unit_id = 'b471e2cd-9af3-11e8-80da-42f2e9dc7849'
    else:
        org_unit_id = 'amoguskpi'

    url = create_api('kpi')
    body = create_kpi_json(kpi_id, org_unit_id)
    query_params = {'forecast': 'true'}

    imprt_res = send_post_api(url, body, query_params)
    return imprt_res


def create_role(user_id, valid_org_unit, period_from='2023-01-01', period_to=None, role_name='ROLE_USER_NEW'):
    if valid_org_unit:
        org_unit_id = 'b471e2cd-9af3-11e8-80da-42f2e9dc7849'
    else:
        org_unit_id = 'amogusrole'

    url = create_api('external-users')
    body = create_role_json(user_id, org_unit_id, period_from, period_to, role_name)
    query_params = {'stop-on-error': 'false'}

    imprt_res = send_post_api(url, body, query_params)
    return imprt_res
