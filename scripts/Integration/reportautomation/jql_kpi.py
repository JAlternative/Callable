import requests

base_url = 'https://jira.goodt.me/rest/api/2'
headers = {'Content-type': 'application/json'}
log = 'логин'
pas = 'пароль'

JQ_WFM = 'project = ABC_HRPORTAL_ABCCONSULTING and type = bug and Sprint in (604, 606, 609, 614, 616, 617)  and creator ='
JQ_BIO = 'project = VID and createdDate >= \'2020-12-30\' and createdDate  < \'2021-04-05\' and type = Bug and creator ='
JQ_AWFM = 'project = AWFM and createdDate >= \'2020-12-30\' and createdDate  < \'2021-04-05\' and type = Bug and creator ='

JQ_WFM_NO = 'project = ABC_HRPORTAL_ABCCONSULTING and type = bug and Sprint in (604, 606, 609, 614, 616, 617)  and creator not in '
JQ_BIO_NO = 'project = VID and createdDate >= \'2020-12-30\' and createdDate  < \'2021-04-05\' and type = Bug and creator not in '
JQ_AWFM_NO = 'project = AWFM and createdDate >= \'2020-12-30\' and createdDate  < \'2021-04-05\' and type = Bug and creator not in '

creators = [
    'v.voronina',
    'g.ericyan',
    'e.kazova',
    'o.palehova',
    'a.svalova'
]


def jq_creator(creator, str):
    return str + creator


def correct_url(jql):
    return base_url + '/search?jql=' + jql + '&maxResults=-1'


def json_returner(jql):
    get_req = requests.get(jql, auth=(log, pas))
    return get_req.json()


def printer(jql, status='Cancelled'):
    b = json_returner(jql)['issues']
    canc_counter = 0
    mark_dict = {
        'Highest': 0,
        'High': 0,
        'Medium': 0,
        'Low': 0,
        'Lowest': 0
    }
    for x in b:
        if x['fields']['status']['name'] == status:
            canc_counter += 1
        else:
            mark_dict[x['fields']['priority']['name']] += 1
    print(len(b) - canc_counter, len(b), sep='\t')
    print(mark_dict)


# for f in creators:
#     print(f)
#     printer((correct_url(jq_creator(f, JQ_BIO))), status='Canceled')

creators_list = '(v.voronina, g.ericyan, e.kazova, o.palehova, a.svalova)'

# printer((correct_url(jq_creator(creators_list, JQ_BIO_NO))), status='Canceled')
# printer((correct_url(jq_creator(creators_list, JQ_WFM_NO))), status='Cancelled')

JQ_WFM_DONE = 'project = ABC_HRPORTAL_ABCCONSULTING and  Sprint in (604, 606, 609, 614, 616, 617) and sprint not in (619) and status = Done  and assignee was '
JQ_WFM_DONE_NO = 'project = ABC_HRPORTAL_ABCCONSULTING and  Sprint in (604, 606, 609, 614, 616, 617) and sprint not in (619) and status = Done  and assignee was not in '

# for d in creators:
#     print(d)
#     printer((correct_url(jq_creator(d, JQ_WFM_DONE))), status='Cancelled')

# printer((correct_url(jq_creator(creators_list, JQ_WFM_DONE_NO))), status='Cancelled')

JQ_BIO_DONE = 'project = VID and createdDate >= \'2020-12-30\' and createdDate  < \'2021-04-05\' and status = Done and updated < \'2021-04-05\' and assignee  was '
JQ_BIO_DONE_NO = 'project = VID and createdDate >= \'2020-12-30\' and createdDate  < \'2021-04-05\' and status = Done and updated < \'2021-04-05\' and assignee  was not in '

# for d in creators:
#     print(d)
#     printer((correct_url(jq_creator(d, JQ_BIO_DONE))), status='Canceled')
#
printer((correct_url(jq_creator(creators_list, JQ_BIO_DONE_NO))), status='Canceled')
