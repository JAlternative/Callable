package wfm.components.orgstructure;

public enum EmployeeInfoName {
    CONTACTS("Контакты"),
    PLACE_OF_WORK("Место работы"),
    INTERNSHIP_PROGRAM("Стажерская программа"),
    LOGIN_OPTIONS("Параметры входа в систему"),
    SKILLS("Навыки"),
    DEPUTY("Заместители"),
    OPTIONS("Параметры"),
    STATUS("Статусы"),
    ACCOUNTING("Бухгалтерия");

    private final String nameOfInformation;

    EmployeeInfoName(final String nameOfInformation) {
        this.nameOfInformation = nameOfInformation;
    }

    public final String getNameOfInformation() {
        return nameOfInformation;
    }
}
