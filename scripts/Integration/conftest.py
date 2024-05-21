import psycopg2


def pytest_addoption(parser):
    parser.addoption("--host", action="store", default="87.239.107.157")
    parser.addoption("--password", action="store", default="kf*9ZQUh6b+K")
    parser.addoption("--username", action="store", default="test_db")
    parser.addoption("--database", action="store", default="russian_post_integration")
