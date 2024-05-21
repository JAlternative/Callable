package wfm.components.schedule;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public enum OmInfoName {
    CONTACTS("Контакты"),
    EMPLOYEES("Сотрудники"),
    SCHEDULE("Режим работы подразделения"),
    CONFLICTS("Установки конфликтов"),
    PARAMETERS("Параметры");
    private final String nameOfInformation;

    OmInfoName(String nameOfInformation) {
        this.nameOfInformation = nameOfInformation;
    }

    public String getNamesOfInformation() {
        return nameOfInformation;
    }
}
