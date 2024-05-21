package wfm.components.positioncategories;

public enum PosCatTripleButton {
    VIEW("Просмотр категории"),
    EDIT("Изменить"),
    DELETE("Удалить");

    private final String variant;

    PosCatTripleButton(String variant) {
        this.variant = variant;
    }

    public String getVariant() {
        return variant;
    }
}
