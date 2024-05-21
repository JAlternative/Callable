package bio.components.client;

public enum LicenseType {
    ACTIVE("active", "активной", "Файл лицензии валидный"),
    EXPIRED("expired", "истекшей", "Лицензия просрочена"),
    NOT_FOUND("not_valid", "невалидной", "Файл лицензии невалидный"),
    FUTURE_ACTIVE("future", "активной в будущем", "Лицензия выпущена на будущее время");

    private final String fileName;
    private final String licenseName;
    private final String uiText;

    LicenseType(String fileName, String licenseName, String uiText) {
        this.fileName = fileName;
        this.licenseName = licenseName;
        this.uiText = uiText;
    }

    public String getFileName() {
        return fileName;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public String getUiText() {
        return uiText;
    }
}
