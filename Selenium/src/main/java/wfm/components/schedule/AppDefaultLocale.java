package wfm.components.schedule;

import java.util.Arrays;

import static utils.tools.CustomTools.randomItem;

public enum AppDefaultLocale {
    EN("en", "Timesheet"),
    RU("ru", "Табель"),
    RU_RUSSIANPOST("ru-RUSSIANPOST", "Фактическое посещение");

    private String locale;
    private String text;

    public static String findTextByLocale(String locale) {
        return Arrays.stream(AppDefaultLocale.values())
                .filter(e -> e.getLocale().equals(locale))
                .collect(randomItem()).getText();
    }

    AppDefaultLocale(String locale, String text) {
        this.locale = locale;
        this.text = text;
    }

    public String getLocale() {
        return locale;
    }

    public String getText() {
        return text;
    }
}
