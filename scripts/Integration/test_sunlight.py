import json

import allure
import psycopg2
import pytest


@pytest.fixture()
def con_db(pytestconfig):
    return psycopg2.connect(host=pytestconfig.getoption("host"),
                            database=pytestconfig.getoption("database"),
                            user=pytestconfig.getoption("username"),
                            password=pytestconfig.getoption("password"))


org_file_name = 'OrganizationUnit_1.txt'
emp_pos_file_name = 'employeeposition (2).txt'
demand_test_org_1 = [
    'name',
    'active',
    'parentOuterId',
    'availableForCalculation',
    'dateFrom',
    'zoneId',
    'organizationUnitTypeOuterId'
]


def load_data_from_file(file_name):
    file = open(file_name, 'r', encoding='utf-8')
    concat_string = file.read()
    file.close()
    return json.loads(concat_string)


def test_emp_pos_1_len():
    i = len(load_data_from_file(emp_pos_file_name))
    print(f'Количество объектов {i}')
    assert i != 0


def test_org_test_1_len():
    i = len(load_data_from_file(org_file_name))
    print(f'Количество объектов {i}')
    assert i != 0


def helper_test_org_3_req(args, email, date_to):
    s = """
select *
from public.organizationunit o
         join organizationunittype t on o.organizationunittype_id = t.id
where o.name = '{0}'                                        
  and o.active = {1}                                        
  and o.outerid = '{2}'                                      
  and o.parent_id in
      (select id from organizationunit where outerid = '{3}') 
  and o.availableforcalculation = {4}                        
  and o.datefrom = '{5}'                                      
  and o.time_zone = '{6}'                                     
  and t.outer_id = '{7}'
    """.format(*args)
    if email is not None:
        s += (' and o.email= \'{0}\' '.format(email))
    if date_to is not None:
        s += (' and o.dateto= \'{0}\' '.format(date_to))
    else:
        s += (' and o.dateto is null')
    return s + ';'


def test_emp_pos_1_required_fields(fields):
    emp_pos_list = load_data_from_file(emp_pos_file_name)
    emp_fields_map = {
        'outerId': '',
        'firstName': '',
        'lastName': '',
        'patronymicName': '',
        'endWorkDate': ''
    }
    pos_org_fields_map = {
        'outerId': ''
    }
    pos_postype_fields_map = {
        'name': '',
        'outerId': ''
    }
    pos_posgroup_fields_map = {
        'name': ''
    }
    pos_poscat_fields_map = {
        'name': '',
        'outerId': '',
        'calculationMode': ''
    }
    pos_fields_map = {
        'id': '',
        'name': '',
        'outerId': '',
        'chief': '',
        'organizationUnit': pos_org_fields_map,
        'positionType': pos_postype_fields_map,
        'positionGroup': pos_posgroup_fields_map,
        'positionCategory': pos_poscat_fields_map,
    }
    emp_pos_fields_map = {
        'startWorkDate': '',
        'endWorkDate': '',
        'number': '',
        'employee': emp_fields_map,
        'position': pos_fields_map,
        'rate': ''
    }
    print(load_data_from_file(emp_pos_file_name)[0])


def test_org_test_1_required_fields(fields=None):
    error_list = []
    if fields is None:
        fields = demand_test_org_1
        for i in load_data_from_file(org_file_name):
            if not all(item in i.keys() for item in fields):
                error_list.append(i['outerId'])
    print(error_list)
    assert len(error_list) == 0


@allure.description('Проверка уникальности записи оргструктуры')
@pytest.mark.parametrize("object", load_data_from_file(org_file_name))
def test_org_test_3_required_fields(con_db, object):
    cursor = con_db.cursor()
    param_list = [
        object['name'],
        object['active'],
        object['outerId'],
        object['parentOuterId'],
        object['availableForCalculation'],
        object['dateFrom'],
        object['zoneId'],
        object['organizationUnitTypeOuterId']
    ]
    email = ''
    date_to = ''

    if 'email' in object.keys():
        email = object['email']

    if 'dateTo' in object.keys():
        date_to = object['dateTo']

    cursor.execute(helper_test_org_3_req(param_list, email, date_to))
    res = cursor.fetchall()
    try:
        assert len(res) == 1
    except AssertionError:
        with open('test_org_test_3_required_fields.txt', 'a+') as f:
            f.write(object['outerId'] + '\n')
