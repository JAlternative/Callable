package bio.components.terminal;

public enum ControlMode {
    MANUAL_CONTROL("Ручное управление"),
    AUTO_CONTROL("Автоматическое управление");
    private final String controlType;

    ControlMode(String controlType) {
        this.controlType = controlType;
    }

    public String getControlType() {
        return controlType;
    }
}
