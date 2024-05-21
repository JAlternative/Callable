package wfm.components.utils;

/**
 * Список для определения цвета бара на графике
 */
public enum ColorsColumns {
    GREEN("teal", "исторические данные"),
    ORANGE("deep-orange", "неполные данные"),
    GREY("grey", "отклонение от нормы"),
    PURPLE("deep-purple", "прогноз"),
    BLUE_GREY("blue-grey", "");

    private final String dateName;
    private final String colorName;

    ColorsColumns(String colorName, String dateName) {
        this.colorName = colorName;
        this.dateName = dateName;
    }

    public String getColorName() {
        return colorName;
    }

    public String getDateName() {
        return dateName;
    }
}
