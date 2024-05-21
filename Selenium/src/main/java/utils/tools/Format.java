package utils.tools;

import java.time.format.DateTimeFormatter;

public enum Format {
    API(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
    TIME(DateTimeFormatter.ofPattern("HH:mm")),
    UI(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
    UI_DOTS(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
    UI_SPACES(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
    UI_DATETIME_WITH_SPACE(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
    UI_JOINT(DateTimeFormatter.ofPattern("ddMMyyyy")),
    API_KPI_VALUE(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
    ;

    private final DateTimeFormatter format;

    Format(DateTimeFormatter format) {
        this.format = format;
    }

    public DateTimeFormatter getFormat() {
        return format;
    }
}
