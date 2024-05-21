package utils.downloading;

import utils.Links;

import static utils.Links.*;

public enum TypeOfBatch {
    FORECAST("1", KPI_FORECAST),
    FTE("2", Links.FTE),
    SHIFTS("3", ROSTERING),
    TARGET_NUMBER("4", CALCULATE_NUMBER_OF_EMPLOYEES); //На UI ошибка и отправлялся неверный запрос.

    private final String columnNumber;
    private final String name;

    TypeOfBatch(String columnNumber, String name) {
        this.columnNumber = columnNumber;
        this.name = name;
    }

    public String getColumnNumber() {
        return columnNumber;
    }

    public String getName() {
        return name;
    }
}
