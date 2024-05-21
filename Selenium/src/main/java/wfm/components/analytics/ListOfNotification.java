package wfm.components.analytics;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public
enum ListOfNotification {
    CALCULATION_DONE("Расчёт выполнен"),
    APPROVE_FTE("FTE опубликован"),
    FAILED_FTE_APPROVE("Невозможно опубликовать FTE"),
    CALCULATION_FAILED("Не удалось выполнить расчёт"),
    APPROVE_KPI("Прогноз KPI утвержден успешно"),
    FAILED_KPI_APPROVE("Не удалось утвердить прогноз бизнес-драйверов"),
    CALCULATION_ERROR("Неизвестная ошибка");

    private final String notificationName;

    ListOfNotification(String s) {
        this.notificationName = s;
    }

    public String getNotificationName() {
        return notificationName;
    }

}
