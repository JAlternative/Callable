import psycopg2
import configparser

config = configparser.ConfigParser()
config.read('connection_config')


def requests_auth():
    return config.get('requests_auth', 'user'), config.get('requests_auth', 'password')


def wfm_address():
    return config.get('wfm_address', 'address')


def conn_intgr():
    return psycopg2.connect(database=config.get('conn_intgr', 'database'),
                            user=config.get('conn_intgr', 'user'),
                            password=config.get('conn_intgr', 'password'),
                            host=config.get('conn_intgr', 'host') ,
                            port=config.get('conn_intgr', 'port'))


# ПОДКЛЮЧЕНИЕ К БД ВФМ
def conn_wfm():
    return psycopg2.connect(database=config.get('conn_wfm', 'database'),
                            user=config.get('conn_wfm', 'user'),
                            password=config.get('conn_wfm', 'password'),
                            host=config.get('conn_wfm', 'host'),
                            port=config.get('conn_wfm', 'port'))
