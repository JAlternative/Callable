package wfm.components.utils;

/**
 * Для определения тестируемой БД через главный ОМ
 */
public enum OmDbType {

    POCHTA(27, "Почта России"),
    SHELL(5, "Shell"),
    INVENTIV(5, "IRG"),
    MAGNIT(6, "Центральный офис");

    private final int organizationUnitTypeId;
    private final String mainOmName;

    OmDbType(int organizationUnitTypeId, String mainOmName) {
        this.organizationUnitTypeId = organizationUnitTypeId;
        this.mainOmName = mainOmName;
    }

    public int getOrganizationUnitTypeId() {
        return organizationUnitTypeId;
    }

    public String getMainOmName() {
        return mainOmName;
    }

}
