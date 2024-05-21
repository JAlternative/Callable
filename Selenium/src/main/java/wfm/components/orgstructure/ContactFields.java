package wfm.components.orgstructure;

import java.util.Random;

public enum ContactFields {
    PHONE_NUMBER("phone-number"),
    PHONE_TYPE("phone-type"),
    EMAIL("email"),
    FAX("fax"),
    POSTAL_CODE("postal-code"),
    REGION("region"),
    CITY("city"),
    NOTE("note"),
    ADDRESS_TYPE("address-type"),
    COUNTRY("country"),
    ADDRESS_STREET("address-street"),
    ADDRESS_BUILDING("address-building");
    private final String fieldName;

    ContactFields(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
    private static final Random random = new Random();

    public static ContactFields randomContact()  {
        ContactFields[] contactFields = values();
        return contactFields[random.nextInt(contactFields.length)];
    }
}
