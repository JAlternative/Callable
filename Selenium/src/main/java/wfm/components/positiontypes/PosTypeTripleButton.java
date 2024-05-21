package wfm.components.positiontypes;

public enum PosTypeTripleButton {
    VIEW("Просмотреть"),
    EDIT("Редактировать"),
    DELETE("Удалить");

    private final String variant;

    PosTypeTripleButton(String variant) {
        this.variant = variant;
    }

    public String getVariant() {
        return variant;
    }
}
