package bio.components.terminal;

public enum SliderType {
    MANUAL_DISPLAY_PERIOD("Период отображения результата распознавания", 3),
    CONNECT_STATUS("Отображать cтатус подключения к центральному серверу", 3),
    EMAIL_DISPLAY("Отображать адрес e-mail службы технической поддержки", 3),
    AUTO_COUNTDOWN_DURATION("Длительность обратного отсчета при автоматическом режиме управления", 2),
    AUTO_DISPLAY_PERIOD("Период отображения результата распознавания", 3),
    MASK("Использовать маску-подсказчик положения лица на видеоконтейнерах", 5),
    CAM_TWO("Повернуть по вертикали", 4),
    CAM_ONE("Повернуть по вертикали", 3),
    MASK_TIP("Подсказка", 2),
    EMAIL_TEXT("E-mail технической поддержки", 2),
    FIRST_STREAM_HREF("Ссылка на видеопоток для видеоконтейнера №1", 2),
    STREAM("Использовать stream", 2),
    DISPLAY_ONE_CAM("Отображать только 1 камеру", 0),
    ITS_NOT_ME("Возможность отклонить результат распознавания кнопкой \"это не я\"", 0);

    private final String sliderName;
    private final int position;

    SliderType(String sliderName, int position) {
        this.sliderName = sliderName;
        this.position = position;
    }

    public String getSliderName() {
        return sliderName;
    }

    public int getPosition() {
        return position;
    }
}
