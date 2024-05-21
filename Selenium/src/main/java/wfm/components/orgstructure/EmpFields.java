package wfm.components.orgstructure;

public enum EmpFields {
    LAST_NAME("@id='last-name'", "Фамилия"),
    FIRST_NAME("@id='first-name'", "Имя"),
    PATRONYMIC_NAME("@id='patronymic-name'", "Отчество"),
    GENDER("@id='gender'", "Пол"),
    MALE("Мужской", "Мужской"),
    FEMALE("Женский", "Женский"),
    DATE_OF_BIRTH("Дата рождения", "Дата рождения"),
    END_WORK_DATE("Дата окончания работы", "Дата окончания работы"),
    TAGS("@class = 'mdl-chip__input au-target'", "Теги");

    private final String fieldName;
    private final String name;

    EmpFields(final String fieldName, final String name) {
        this.fieldName = fieldName;
        this.name = name;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getName() {
        return name;
    }
}
