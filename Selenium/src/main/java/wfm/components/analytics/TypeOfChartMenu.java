package wfm.components.analytics;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public
enum TypeOfChartMenu {
    KPI_FORECAST("KpiForecast", "Расчёт прогноза"),
    KPI_PUBLISHED("KpiPublished", "Публикация  прогноза"),
    FTE_FORECAST("FteForecast", "Расчёт ресурсной потребности"),
    FTE_PUBLISHED("FtePublished", "Публикация  ресурсной потребности"),
    KPI_CORRECTION("kpiCorrectionSessions", "Изменения в фактических данных"),
    KPI_FORECAST_CORRECTION("kpiForecastCorrectionSessions", "Изменения в прогнозе");


    private final String clickTrigger;
    private final String name;

    TypeOfChartMenu(String clickTrigger, String name) {
        this.clickTrigger = clickTrigger;
        this.name = name;
    }

    public String getClickTrigger() {
        return clickTrigger;
    }

    public String getName() {
        return name;
    }
}
