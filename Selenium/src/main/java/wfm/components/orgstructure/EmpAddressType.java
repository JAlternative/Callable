package wfm.components.orgstructure;

public enum EmpAddressType {
    LEGAL("Регистрации"),
    ACTUAL("Проживания"),
    ANOTHER("Другой");
    private final String addressName;

    EmpAddressType(final String addressName) {
        this.addressName = addressName;
    }

    public String getAddressName() {
        return addressName;
    }
}
