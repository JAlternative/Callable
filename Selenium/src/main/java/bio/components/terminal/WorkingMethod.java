package bio.components.terminal;

/**
 * @author Evgeny Gurkin 14.08.2020
 */
public enum WorkingMethod {
    SIMPLE ("Работа с одним устройством (метод SIMPLE)"),
    ONE_CAMERA_FAST("Работа с одним устройством с использованием антифрода (метод ONE_CAMERA_FAST)"),
    TWO_CAMERA_FAST("Работа с двух устройств с использованием антифрода (метод TWO_CAMERAS_FAST)");

    private final String methodName;

    WorkingMethod(String methodName) {
     this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }
}
