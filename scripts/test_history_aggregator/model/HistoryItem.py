from model.Result import Result


class HistoryItem:
    failed: int
    broken: int
    skipped: int
    passed: int
    unknown: int
    total: int
    last_launch: str
    description: str
    method: str

    def __init__(self, result: Result, json: dict):
        self.history_id = result.history_id
        self.failed = json['statistic']['failed']
        self.broken = json['statistic']['broken']
        self.skipped = json['statistic']['skipped']
        self.passed = json['statistic']['passed']
        self.unknown = json['statistic']['unknown']
        self.total = json['statistic']['total']
        self.last_launch = list(map(lambda e: e['reportUrl'], json['items']))[0]
        self.success_rate = self.passed / self.total
        self.method = f'{result.test_class}.{result.method}'
        self.description = result.name
        self.group = result.group
        self.api_test = 'Api' in result.test_class

    def __str__(self):
        return self.method

    def __lt__(self, other):
        """ Сортирует: группа -> метод -> общее количество прогонов -> процент успешности"""
        return (self.group, self.method, self.total, self.success_rate) < \
               (other.group, other.method, other.total, other.success_rate)

    def get_dict(self):
        return {
            'group': self.group,
            'method': self.method,
            'name': self.description,
            'success rate': self.success_rate,
            'total': self.total,
            'passed': self.passed,
            'failed': self.failed,
            'broken': self.broken,
            'skipped': self.skipped,
            'unknown': self.unknown,
            'last launch': self.last_launch,
            'api test': self.api_test
        }
