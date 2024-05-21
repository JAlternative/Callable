package wfm.components.schedule;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public enum PositionTypes {
    OUT_SOURCE("43", "Аутсорсеры"),
    ENGINEER("42", "Инженер"),
    STORE_MANAGER("30", "Управляющий (Директор магазина)"),
    SELLER("31", "Продавец"),
    STOREKEEPER("34", "Кладовщик"),
    VISUAL_MERCHANDISE("35", "Визульный мерчендайзер"),
    CASHIER("33", "Кассир"),
    SENIOR_SELLER_STORE_ADMINISTRATOR("32", "Старший продавец (Администратор магазина)"),
    RECEIVER("41", "Приемщик"),
    VIRTUAL_EMPLOYEE("40", "Виртуальный сотрудник"),
    DIVISION_DIRECTOR("38", "Директор дивизиона"),
    RT_SELLER("39", "Продавец PT"),
    TERRITORIAL_DIRECTOR("6", "Территориальный директор"),
    FORMAT_DIRECTOR("7", "Директор формата"),
    DIRECTOR("8", "Директор");

    private final String id;
    private final String name;

    PositionTypes(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
