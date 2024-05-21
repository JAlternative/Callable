package wfm.components.schedule;

import wfm.repository.CommonRepository;

import java.util.Objects;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public
enum VariantsOfFunctions {
    CALCULATION_RECALCULATE_SHIFTS("views.shifts.actions.calculation", "Расчёт/перерасчет смен"),
    PUBLICATION("views.shifts.actions.publication", "Публикация"),
    EMPLOYEE_FILTER("views.shifts.employeeFilter", "Фильтр сотрудников"),//элемент не актуален?
    RECALCULATE_SHIFTS("views.shifts.actions.recalculateShiftsExchange", "Пересчитать биржу смен"),
    FTE_CALCULATION("views.shifts.actions.fteCalculation", "Расчёт ресурсной потребности"),
    SUBDIVISION_PROPERTIES("views.shifts.OUProp", "Свойства подразделения"),
    CREATE_EVENT("views.shifts.actions.addEvent", "Создать событие"),
    COMMENTS_ON_DAYS("views.shifts.actions.comments", "Комментарии к дням"),
    COMMENTS_TO_THE_VERSIONS_OF_THE_CALCULATION("views.shifts.actions.rostersComments", "Комментарии к версиям расчёта"),
    FOR_APPROVAL("views.shifts.actions.notifyPublicationReadiness", "На утверждение"),
    DOWNLOAD_XLSX("views.shifts.actions.downloadXLSX", "Скачать (xlsx)"),
    PRINT("common.essentials.print", "Печать"),
    PRINT_NORMALIZED_SHIFTS("", "Печать (нормализованный график)"),//элемент не актуален?
    REPORT_T_13_FORM("", "Отчет по форме Т-13"),
    DOWNLOAD_PLANNED_SCHEDULE("menu__icon mdi", "Плановый график"),
    UPLOAD_IN_1C("common.essentials.download1c", "Выгрузка в 1С"),
    SCHEDULE_WIZARD("Мастер планирования", "Мастер планирования"),
    APPROVE_TABLE("views.shifts.actions.approveTimeSheet", "Утвердить табель");

    private final String function;
    private String name;
    private String backup;

    VariantsOfFunctions(String divStructure, String backup) {
        this.function = divStructure;
        this.name = null;
        this.backup = backup;
        //todo попросить на фронте, чтобы сделали нормальные единообразные атрибуты t для всех пунктов этой менюшки.
        // Тогда костыли с try-catch из getName и pagemodel.ScheduleBoard.chooseFunction можно будет убрать.
    }

    public String getVariant() {
        return function;
    }

    public String getName() {
        if (Objects.isNull(name)) {
            try {
                name = CommonRepository.getLocalizedName(function);
            } catch (java.lang.IllegalArgumentException e) {
                return backup;
            }
        }
        return name;
    }

    public String getBackup() {
        return backup;
    }
}
