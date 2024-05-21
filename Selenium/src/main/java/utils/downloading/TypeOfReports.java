package utils.downloading;

import org.testng.Assert;

public enum TypeOfReports {
    PRINTED_FORMS(null, "Печатные формы", null, null, ""),
    SHIFTS("shifts", "Смены", null, null, "shifts"),
    NUMBER_OF_GRAPHS(null, "Численность  по графикам", null, "Численность по графикам", ""),
    PUBLICATION_STATUS(null, "Статус публикации графиков смен", null, "Публикация графиков", ""),
    QUALITY_HISTORICAL_DATA(null, "Качество исторических данных", null, null, "kpi"),
    HOLIDAY_QUOTAS(null, "Квоты выходных дней", "report-quotas", null, ""),
    AVERAGE_CONVERSION(null, "Анализ средней конверсии", "report-conversion-average", null, ""),
    PLAN_FACT_CONVERSION(null, "Плановая и фактическая конверсия", "report-conversion-planned", null, ""),
    NUMBER_OF_STAFF(null, "Численность персонала", "report-employees-number", null, ""),
    ATTENDANCE(null, "Посещаемость", null, null, "attendance"),
    TIME_SHEET("workedShiftsCsv", "Табель учёта рабочего времени", null, null, "worked-shifts"),
    VALUES_OF_PARAMETERS(null, "Значения используемых параметров", null, null, ""),
    TARGET_NUMBER(null, "Целевая численность", null, null, "stuff"),
    DATA_FOR_CALCULATION(null, "Целевая численность", null, null, ""),
    TECHNICAL_TABLE_UNLOADING(null, "Техническая выгрузка табеля", null, null, ""),
    EMPLOYEE_WORKING_FACT(null, "Факт работы сотрудника", null, null, ""),
    SHIFTS_EXTERNAL_EMPLOYEE(null, "Смены внешних сотрудников", null, null, "outstaff"),
    //для расписания
    PLANNED_GRAPH(null, "Выгрузить плановый график", null, null, ""),
    UPLOAD_1C(null, "Выгрузка в 1С", null, null, ""),
    NORMALIZED_SHIFTS(null, "Печать (нормализованный график)", null, null, ""),
    PRINT_SCHEDULE_WITH_COMMENT(null, "Печать", null, null, ""),
    REPORT_COMPETENCE(null, "Отчет по компетенциям", null, null, "");


    private final String showBindName;
    private final String nameOfReport;
    private final String urlName;
    private final String titleName;
    private final String filePrefix;

    TypeOfReports(String showBindName, String nameOfReport, String urlName, String titleName, String filePrefix) {
        this.showBindName = showBindName;
        this.nameOfReport = nameOfReport;
        this.urlName = urlName;
        this.titleName = titleName;
        this.filePrefix = filePrefix;
    }

    public String getUrlName() {
        if (urlName == null) {
            Assert.fail("Параметр не задан");
        }
        return urlName;
    }

    public String getShowBindName() {
        if (showBindName == null) {
            Assert.fail("Параметр не задан");
        }
        return showBindName;
    }

    public String getNameOfReport() {
        return nameOfReport;
    }

    public String getTitleName() {
        return "Отчет: " + (titleName != null ? titleName : nameOfReport);
    }

    public String getFilePrefix() {
        return filePrefix;
    }

}

