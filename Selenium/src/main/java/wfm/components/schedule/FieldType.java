package wfm.components.schedule;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public enum FieldType {
    DATE_OPEN("Дата открытия"),
    TYPE("Тип");

    private final String nameOfType;

    FieldType(String nameOfType) {
        this.nameOfType = nameOfType;
    }

    public String getNameOfType() {
        return nameOfType;
    }
}
