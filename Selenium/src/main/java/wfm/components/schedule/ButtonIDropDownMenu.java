package wfm.components.schedule;

public enum ButtonIDropDownMenu {
    ADDITIONAL_INFORMATION("Дополнительная информация"),
    FRONT_INDICATOR("Front чел.ч"),
    BACK_INDICATOR("Back чел.ч"),
    EVENTS("События"),
    NUMBER_SHIFTS("Кол-во  смен"),
    NUMBER_HOURS("Кол. час.");

    private final String itemTitle;

    ButtonIDropDownMenu(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    @Override
    public String toString() {
        return itemTitle;
    }
}
