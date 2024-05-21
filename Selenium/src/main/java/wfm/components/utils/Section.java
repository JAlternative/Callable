package wfm.components.utils;

import java.time.LocalDate;

public enum Section {
    ANALYTICS("Прогнозирование", "/analytics"),
    MATH_PARAMETERS("Математические параметры", "/math-parameters"),
    SCHEDULE_BOARD("Расписание", "/schedule-board"),
    BATCH_CALCULATION("Стратегическое планирование", "/batch-calculation"),
    ORG_STRUCTURE("Оргструктура", "/org-structure"),
    POSITION_TYPES("Типы позиций", "/position-types"),
    REPORTS("Отчеты", "/reports"),
    SYSTEM_LISTS("Системные списки", "/system-lists"),
    POSITION_GROUPS("Функциональные роли", "/position-groups"),
    WELCOME("Добро пожаловать", "/welcome"),
    LOGIN("Вход", "/login"),
    FTE_OPERATION_VALUES("Результат расчета рабочей нагрузки", "/fte-operation-values"),
    SUPPORT("Служба поддержки", "/support"),
    INSTRUCTIONS("Справка", "/instructions"),
    PROFILE("Профиль", "/profile"),
    PERSONAL_SCHEDULE_REQUESTS("Личное расписание", "/personal-schedule-requests/month/" + LocalDate.now()),
    SYSTEM_SETTINGS("Системные настройки", "/system-properties"),
    MESSAGES("Уведомления", "/messages"),
    HISTORY_MATH_PARAMETER_VALUES("История значений мат. параметров", "/history-math-parameter-values"),
    KPIS("Бизнес-драйверы", "kpis/"),
    SYSTEM_ERRORS("Системные ошибки", "/system-errors"),
    MESSAGE_SETTINGS("Управление уведомлениями", "message-settings"),
    INTEGRATION_LOG("Журнал логирования", "integration-log"),
    STAFF_NUMBER("Численность персонала", "/staff-number"),
    CALC_INSTANCES("Распределённые вычисления", "calc-instances"),
    SESSION("Сессии пользователей", "/sessions");


    private final String name;
    private final String urlEnding;

    Section(String name, String urlEnding) {
        this.name = name;
        this.urlEnding = urlEnding;
    }

    public String getName() {
        return name;
    }

    public String getUrlEnding() {
        return urlEnding;
    }
}
