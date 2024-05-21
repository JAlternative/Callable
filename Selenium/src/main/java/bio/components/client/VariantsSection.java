package bio.components.client;

/**
 * Выбор раздела
 */
public enum VariantsSection {
    TERMINAL("Терминалы"),
    PERSONAL("Персонал"),
    ORG_STRUCTURE("Оргструктура"),
    USERS("Пользователи"),
    JOURNAL("Журнал событий"),
    JOURNAL_TASKS("Журнал заданий"),
    JOURNAL_CORR("Журнал корреспонденции"),
    LICENSING("Лицензирование"),
    SETTINGS("Настройки");

    private final String sectionName;

    VariantsSection(String section) {
        this.sectionName = section;
    }

    public String getSectionName() {
        return sectionName;
    }
}
