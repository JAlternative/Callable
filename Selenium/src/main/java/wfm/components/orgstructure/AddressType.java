package wfm.components.orgstructure;

public enum AddressType {
    LEGAL("Юридический"),
    ACTUAL("Фактический"),
    MAILING("Почтовый"),
    ANOTHER("Другой");
    private final String addressName;

    AddressType(final String addressName) {
        this.addressName = addressName;
    }

    public String getAddressName() {
        return addressName;
    }
}
