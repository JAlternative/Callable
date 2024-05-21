class Issue:
    """
    Класс-обертка для редмайновых задач. Собирает инфу для итоговой таблицы и дополнительные данные
    """
    tested: bool
    broken_lc: bool
    issue: None
    tracker: str
    subject: str
    created_on: str
    updated_on: str
    module: str
    project: str
    status: str
    tester: str
    comment: str

    def __init__(self, issue):
        self.issue = issue
        self.id = issue.id
        self._tested = None
        self._broken_lc = None
        self.tracker = issue.tracker
        self.subject = issue.subject
        self.created_on = issue.created_on
        self.updated_on = issue.updated_on
        self._module = ''
        self._project = ''
        self.status = issue.status.name
        self._tester = None
        self.comment = ''

    def __str__(self):
        return self.subject

    @property
    def project(self):
        if not self._project:
            try:
                project_id = int(list(filter(lambda a: a.id == 125, self.issue.custom_fields))[0].value[0])
            except IndexError:
                return ''
            else:
                from repo import PROJECT_DICT
                self._project = PROJECT_DICT.get(project_id)
        return self._project

    @property
    def module(self):
        if not self._module:
            module_id = int(list(filter(lambda a: a.id == 105, self.issue.custom_fields))[0].value)
            from repo import MODULE_DICT
            self._module = MODULE_DICT.get(module_id)
        return self._module

    @property
    def tested(self):
        if self._tested is None:
            self._parse_journals()
        return self._tested

    @property
    def broken_lc(self):
        if self._broken_lc is None:
            self._parse_journals()
        return self._broken_lc

    @property
    def tester(self):
        if self._tester is None:
            self._parse_journals()
        return self._tester

    def get_csv_row(self):
        if self.broken_lc:
            self.comment = 'Сломан ЖЦ задачи!\n' + self.comment
        return {
            'Задача': self.id,
            'Трекер': self.tracker,
            'Тема': self.subject,
            'Создана': self.created_on,
            'Обновлена': self.updated_on,
            'Модуль': self.module,
            'Проект': self.project,
            'Статус': self.status,
            'Ответственный QA': self.tester,
            'Комментарий': self.comment,
        }

    def _parse_journals(self):
        from utils import CONFIG
        journals = self.issue.journals
        for journal in journals:
            details = journal.details
            for detail in details:
                if detail['name'] == 'status_id' \
                        and detail['new_value'] in list(map(lambda x: str(x), CONFIG.testing_statuses)):
                    self._tested = True
                    return
            if journal.user.name in CONFIG.testers:
                self._broken_lc = True
                self._tested = True
                self._tester = journal.user.name
                return
