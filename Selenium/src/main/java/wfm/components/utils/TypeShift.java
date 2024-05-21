package wfm.components.utils;

public enum TypeShift {

    ALL_SHIFTS("Все смены"),
    OPENING_SHIFTS("Открывающие смены"),
    CLOSING_SHIFTS("Закрывающие смены"),
    FREE_SHIFTS("Свободные смены");


    private final String typeShift;

    TypeShift(String typeShift) {
        this.typeShift = typeShift;
    }

    public String getTypeShift() {
        return typeShift;
    }
}
