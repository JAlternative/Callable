package wfm.components.schedule;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public enum KPIOrFTE {
    KPI_HISTORY("Исторические данные"),
    KPI_FORECAST("Прогноз"),
    FTE("Ресурсная потребность"),
    KPI_OR_FTE(""),
    ;
    private final String name;

    KPIOrFTE(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
