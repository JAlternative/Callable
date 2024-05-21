from model.Result import Result
from model.Test import Test
from typing import Optional, List


class ReportLine:
    group: str
    id: str
    status: str
    grade: int
    params: List[str]
    method: str
    section: str
    description: str

    def __init__(self, test: Optional[Test], result: Optional[Result]):
        """
        Возможна инициализация с разными входными данными
        (только тест или только результат, или оба),
        поэтому необходимо проверять типы параметров метода
        """
        self.id = 'NO_TEST'
        self.status = 'NO_STATUS'
        self.grade = 3
        if test:
            self.id = test.id + ' (NO TAG)'
            self.method = test.method
            self.grade = int(test.grade)
            self.section = test.section
            self.description = test.description
            self.group = test.group
            self.params = []
        if result:
            self.description = result.name
            if result.grade and result.grade != 3 or result.grade == 0:
                self.grade = result.grade
            self.section = result.section
            self.status = result.status
            self.method = result.method
            self.params = result.params
            if result.id:
                self.id = result.id
            self.group = result.group

    def __lt__(self, other):
        """ Сортирует: модуль -> категория -> грейд -> id -> описание теста"""
        return (self.section, self.group, self.grade, self.id, self.description, self.params) < \
               (other.section, other.group, other.grade, other.id, other.description, other.params)

    def __str__(self):
        return self.method

    def get_dict(self):
        if not self.params:
            full_title = self.description
        else:
            full_title = f"{self.description} ({', '.join(self.params)})"
        return {'Group': self.group,
                'ID': self.id,
                'Name': full_title,
                'Grade': f'G{self.grade}',
                'Section': self.section,
                'Method': self.method,
                'Status': self.status}
