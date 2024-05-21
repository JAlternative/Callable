package wfm.components.orgstructure;

public enum PhoneTypes {
    HOME("Домашний"),
    MOBILE("Мобильный"),
    WORK("Рабочий"),
    ANOTHER("Другой");
    private final String phoneName;

    PhoneTypes(String phoneName) {
        this.phoneName = phoneName;
    }

    public String getPhoneName() {
        return phoneName;
    }
}
