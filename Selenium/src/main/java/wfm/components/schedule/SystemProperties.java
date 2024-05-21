package wfm.components.schedule;

public enum SystemProperties {

    FACT_SHIFT_COMMENTS_REQUIRED("fact.shift.comments.required", "false"),
    FACT_SHIFT_COMMENTS_DELETE_REQUIRED("fact.shift.comments.delete.required", "false"),
    PLAN_SHIFT_COMMENTS_DELETE_REQUIRED_FUTURE("plan.shift.comments.delete.required.future", "false"),
    PLAN_SHIFT_COMMENTS_DELETE_REQUIRED_PAST("plan.shift.comments.delete.required.past", "false"),
    PLAN_SHIFT_COMMENTS_REQUIRED_FUTURE("plan.shift.comments.required.future", "false"),
    PLAN_SHIFT_COMMENTS_REQUIRED_PAST("plan.shift.comments.required.past", "false"),
    SHOW_SCHEDULE_REQUEST_CHANGE_LIST("shift.showScheduleRequestChangeList", "false"),
    PLAN_SHIFT_COMMENTS_REQUIRED("plan.shift.comments.required", "false"),
    KPI_FORECAST_SUM_PREDICTION_METHOD("kpi.forecast.sumPredictionMethod", "LEAST_SQUARES_LINEAR_FUNCTION"),
    /**
     * KPI N/A
     */
    KPI_FORECAST_FACT_VALUE_COUNT_FOR_SUM_PREDICTION("kpi.forecast.factValueCountForSumPrediction", "6"),
    /**
     * KPI N/A
     */
    KPI_FORECAST_DAY_OF_MONTH_DISTRIBUTION_TYPE("kpi.forecast.dayOfMonthDistributionType", "SLIDING_WINDOW_WEEK_AVERAGE"),
    /**
     * KPI N/A
     */
    KPI_FORECAST_RANDOM_HOUR_VALUES("kpi.forecast.randomHourValues", "false"),
    /**
     * KPI N/A
     */
    KPI_FORECAST_RANDOM_DAY_VALUES("kpi.forecast.randomDayValues", "false"),
    /**
     * KPI N/A
     */
    KPI_FORECAST_RANDOM_INTERVAL_PROPORTION("kpi.forecast.randomIntervalProportion", "0.0"),
    /**
     * KPI N/A
     */
    KPI_VISIBLE("kpi.visible", ""),
    /**
     * KPI N/A
     */
    KPI_FORECAST_MIN_FILLED_VALUES_PROPORTION("kpi.forecast.minFilledValuesProportion", "0.65"),

    /**
     * KPI N/A
     */
    KPI_FORECAST_REMOTE_CALCULATION("forecast.calculation.remote", "false"),
    /**
     * KPI N/A
     */
    KPI_FORECAST_REMOTE_CALCULATION_URL("forecast.calculation.remote.url", "http://localhost:5000/"),
    //endregion

    //region Fte
    /**
     * Fte N/A
     */
    FTE_SOURCE("fte.source", "CONVERSION_AND_PRODUCTIVITY_2"),
    /**
     * Fte N/A
     */
    FTE_ROUND("fte.fteRound", "FTE_ROUND_NORMAL"),
    /**
     * Fte N/A
     */
    FTE_CONSIDER_BREAKS("fte.considerBreaks", "true"),
    /**
     * Fte N/A
     */
    FTE_DEFAULT_STRATEGY("fte.defaultStrategy", "B"),
    /**
     * Fte N/A
     */
    FTE_USE_WORKED_SHIFTS("fte.useWorkedShifts", "true"),
    /**
     * Fte N/A
     */
    FTE_CONVERSION_FORECAST("fte.conversionForecast", "true"),
    /**
     * Fte N/A
     */
    FTE_TEST("fte.test", "false"),
    /**
     * Fte N/A
     */
    FTE_CHECK_TRAFFIC_QUALITY("fte.checkTrafficQuality", "false"),
    /**
     * Fte N/A
     */
    FTE_KPI("fte.fteKpi", "operations"),
    FTE_SPACES_PROPORTION("fte.spacesProportion", "0.5"),
    /**
     * Способ вычисления расчётной численности
     */
    FTE_NC_ALGORITHM("fte.nc.algorithm", "DEFAULT"),
    //endregion

    //region Разное
    /**
     * Количество дней в течение которых можно редактировать смены
     */
    WORKED_SHIFT_CAN_EDIT_DAYS("workedShift.can.edit.days", "5"),
    /**
     * Ограничение количества сессий пользователя
     */
    AUTH_USER_MAX_SESSIONS("auth.user.max.sessions", "0"),
    /**
     * Количество неудачных попыток авторизаций перед блокировкой
     */
    AUTH_USER_ATTEMPTS_COUNT("auth.user.attempts.count", "5"),
    /**
     * Время блокировки пользователя в минутах
     */
    AUTH_USER_ATTEMPTS_TIMEOUT("auth.user.attempts.timeout", "5"),
    /**
     * Разрешить менеджеру публикацию графика после публикации менеджером куста
     */
    MANAGER_SECONDARY_PUBLICATION_ALLOW("manager.secondary.publication.allow", "false"),
    /**
     * Функция отображения индикатора по количеству открывающих и закрывающих смен в разделе "Расписание"
     */
    SCHEDULE_BOARD_SHIFTS_INDICATOR("scheduleBoardShiftsIndicator", "false"),
    /**
     * Разрешить менеджеру назначение на должность из всех сотрудников
     */
    MANAGER_ALL_EMPLOYEES_ALLOW("manager.all.employees.allow", "false"),
    /**
     * Создавать невиртуальных сотрудников
     */
    EMPLOYEES_NOT_VIRTUAL("employees.not.virtual", "false"),
    /**
     * Отключить уведомление о публикациии смены на бирже
     */
    SHIFT_EXCHANGE_NOTIFICATION_OFF("shift.exchange.notification.off", "false"),
    /**
     * email поддержки клиента
     */
    SUPPORT_EMAIL("support.email", "@goodt.me"),
    CLIENT_CODE("client.code", "R_POST"),
    /**
     * Копирование смен в табеле
     */
    WORKED_ROSTER_COPY_SHIFTS("workedRoster.copyShifts", "true"),
    /**
     * Предел меток в часах для импорта биометрии
     */
    BIO_LIMIT_ALLOWED("bio.limit.allowed", "1"),
    /**
     * Количество потоков пакетных расчетов
     */
    BATCH_CALCULATIONS_THREAD_COUNT("batchCalculations.threadCount", "4"),
    /**
     * Проверка использования предыдущих паролей
     */
    CHECK_LAST_USED_PASSWORDS("check.last.used.passwords", "0"),
    /**
     * Требовать смену паролья при выставлении соотвю флага
     */
    CHANGE_PASSWORD_ON_ENTER("change.password.on.enter", "false"),
    PASSWORD_CHANGE_IN_SPECIFIED_PERIOD("password.change.in.specified.period", "0"),
    /**
     * Подсчёт часов за период с учётом начала/окончания дня в 00:00
     */
    SPLIT_SHIFTS_FOR_DEVIATIONS_CALCULATION("deviations.split.shifts", "false"),
    /**
     * Отпуск и больничный вычитаются из нормы часов на месяц
     */
    SUBTRACT_ABSENCES_FROM_STANDARD("deviations.subtractAbsencesFromStandard", "false"),

    /**
     * Корневой каталог для загрузки изображений
     */
    ROOT_DIR_PATH("root.dir.path", "/hrportal/images"),
    /**
     * Подкаталог для загрузки файлов сотрудников
     */
    EMPLOYEES_DIR_PATH("employees.dir.path", "employees"),
    /**
     * Подкаталог для загрузки файлов задач
     */
    TASK_DIR_PATH("task.dir.path", "tasks"),

    DATAFILES_IMPORT_PATH("datafiles.import.path", "/hrportal/import"),

    DATAFILES_UPLOAD_PATH("datafiles.upload.path", "/hrportal/upload"),

    DATAFILES_MANUAL_PATH("datafiles.manual.path", "/hrportal/manual"),

    DATAFILES_KPI_PATH("datafiles.kpi.path", "/hrportal/kpi"),

    DATAFILES_KPI_IGNORE_ZEROS("datafiles.kpi.ignoreZeros", "true"),

    DATAFILES_KPI_DATETIME_FORMAT("datafiles.kpi.datetimeFormat", "dd.MM.yyyy HH:mm"),

    DATAFILES_KPI_DATE_FORMAT("datafiles.kpi.dateFormat", "yyyy-MM-dd"),
    //endregion

    //region Mail
    /**
     *
     */
    MAIL_HOST("mail.host", "smtp.gmail.com"),

    MAIL_PORT("mail.port", "465"),

    MAIL_PROTOCOL("mail.protocol", "smtps"),

    MAIL_BOX("mail.box", "info@domain.com"),

    MAIL_USER("mail.user", "info@domain.com"),

    MAIL_PASSWORD("mail.password", "password"),

    MAIL_SMTP_AUTH("mail.smtp.auth", "true"),

    MAIL_SMTP_STARTTLS_ENABLE("mail.smtp.starttls.enable", "false"),

    MAIL_DEBUG("mail.debug", "true"),
    //endregion

    //region Notifications
    ROSTER_READY_NOTIFICATION_REPORT_URL("roster.ready.notification.report.url", "http://dev.wfm.zozowfm.com/report-publication-status/date"),

    NOTIFICATION_ROSTER_READY_TO_PUBLISH_ENABLE("notification.roster.ready.to.publish.enable", "false"),

    NOTIFICATION_FRONT_URL("notification.front.url", "http://dev.wfm.zozowfm.com/"),
    //endregion

    /**
     * Использовать трафик для расчёта численности при расчёте деперсонализированных смен
     */
    FTE_USE_TRAFFIC_FOR_DEPERSONALIZED_ROSTER("fte.useTrafficForDepersonalizedRoster", "false"),
    /**
     * Использовать численность с прдыдущего месяца при расчёте деперсонализированных смен
     */
    ROSTER_FIX_NUMBER_OF_EMPLOYEES("roster.fixNumberOfEmployees", "false"),
    /**
     * Округлять до часов время смен для входных данных алгоритмов расчёта смен
     */
    ROSTER_ROUND_TIME_INTERVALS("roster.roundTimeIntervals", "true"),

    /**
     * Пост обработка для био
     */
    BIO_POST_PROCESS("bio.post.process", ""),

    BIO_POST_PROCESS_PREF_JSON("bio.post.process.pref.json", "{}"),

    //region Расписание заданий
    CRON_BIO_UPDATE("cron.bio.update", "0 59 23 * * ?"),

    CRON_SHIFT_AUTO_APPROVE("cron.shift.auto.approve", "* 59 23 */1 * ?"),

    CRON_EMPLOYEE_CLEAR_EXPIRED_TRAINEE_PROGRAM("cron.employee.clear.expired.trainee.program", "1 1 1 * * ?"),

    CRON_HANDLE_EMPLOYEE_AFTER_DISMISSAL("cron.handle.employee.after.dismissal", "0 0 0 * * *"),

    CRON_HANDLE_EMPLOYEE_AFTER_TRANSFER("cron.handle.employee.after.transfer", "0 0 0 * * *"),

    CRON_SUBSCRIPTIONS_TASK("cron.subscriptions.task", "0 * * * * *"),

    CRON_SUBSCRIPTIONS_ROSTER("cron.subscriptions.roster", "5 * * * * *"),

    CRON_SUBSCRIPTIONS_FTE("cron.subscriptions.roster", "10 * * * * *"),

    CRON_INTEGRATION("integration.cron", "0 0 3 * * ?"),
    //endregion

    /**
     * Расчет обеда по правилам после расчета в математике
     */
    CALC_LUNCH_AFTER_MATH("calc.lunch.after.math", "false"),

    ALLOW_CHANGE_OUTER_ID("allow.change.outer.id", "false"),

    /**
     * Показывать подсказку при наведении на ФИО сотрудника
     */
    SCHEDULE_BOARD_FULL_NAME_HINT("scheduleBoardFullNameHint", "false"),

    /**
     * Учитывать кол-во переработок/недоработок при подсчете часов в поп-ап
     */
    SCHEDULE_BOARD_SHOW_SHIFT_HOURS("scheduleBoardShowShiftHours", "true"),
    /**
     * Показывать кол-во переработок/недоработок за год
     */
    SCHEDULE_BOARD_SHOW_YEAR_OVERTIME_UNDERTIME("scheduleBoardShowYearOvertimeUnderTime", "true"),

    /**
     * Показывать ли кол-во выходных при наведении на рабочие часы сотрудника в расписании
     */
    SCHEDULE_BOARD_SHOW_DAYS_OFF_COUNT("scheduleBoardShowDaysOffCount", "true"),

    /**
     * Показывать OuterId в расписании
     */
    SCHEDULE_BOARD_SHOW_OUTER_ID("scheduleBoardShowOuterId", "false"),
    /**
     * Контроль планового времени в текущем месяце на расписании
     */
    SCHEDULE_BOARD_DEVIATION_FROM_STANDARD_PLAN("scheduleBoardDeviationFromStandardPlan", "false"),
    /**
     * Показывать ставку в расписании
     */
    SCHEDULE_BOARD_SHOW_RATE("scheduleBoardShowRate", "false"),
    /**
     * Запрет изменения названия позиции при редактировании должности сотрудника
     */
    EMPLOYEE_POSITION_JOB_TITLE_DISABLED("employeePositionJobTitleDisabled", "false"),
    /**
     * Автоматически проставлять дату окончания должности равной окончанию назначения
     */
    EMPLOYEE_POSITION_AUTO_CLOSE_POSITION("employeePositionAutoClosePosition", "false"),
    /**
     * Разрешать одновременное назначение сотрудников на одну должность
     */
    POSITION_EMPLOYEE_POSITIONS_ALLOW_INTERSECTIONS("position.employee_positions.allow_intersections", "true"),
    /**
     * Индикатор ночных часов в расписании
     */
    SCHEDULE_BOARD_NIGHT_HOURS_INDICATOR("scheduleBoardNightHoursIndicator", "false"),
    /**
     * Норматив по полумесяцу
     */
    SCHEDULE_BOARD_CRESCENT_STANDARD("scheduleBoardCrescentStandard", "false"),
    /**
     * Печать нормализованного графика
     */
    SCHEDULE_BOARD_NORMALIZED_PRINT("scheduleBoardNormalizedPrint", ""),

    /**
     * Наличие возможности выгрузки в 1С в расписании
     */
    SCHEDULE_BOARD_DOWNLOAD_1C("scheduleBoardDownload1CLink", ""),

    //JASPER
    /**
     * Возможность выгрузки планового графика в меню троеточия расписания
     */
    JASPER_REPORTS_SHIFTS_PLAN_SHOW("jasper.reports.shiftsPlan.show", "false"),

    /**
     * Jasper. Разрешить выбор формата	jasper
     */
    JASPER_ALLOW_FORMAT_SELECTION("jasper.allowFormatSelection", "true"),

    /**
     * Jasper. Формат по умолчанию
     */
    JASPER_DEFAULT_FORMAT("jasper.defaultFormat", "pdf"),

    /**
     * Jasper. Базовый URL	jasper
     */
    JASPER_SERVER_URL("jasper.serverUrl", "https://jasper-dev.goodt.me/jasperserver"),//"http://34.74.233.212:8080/jasperserver"),

    /**
     * Jasper. Пароль для входа
     */
    JASPER_PASSWORD("jasper.password", "123456"),

    /**
     * Jasper. Логин для входа
     */
    JASPER_USERNAME("jasper.username", "abc"),

    //Jasper Отчет "Плановый график (с ростером)"

    /**
     * Jasper. Плановый график (с ростером). Путь
     */
    JASPER_REPORTS_ROSTER_MAIN_PATH("jasper.reports.rosterMain.path", "/reports/RosterPrintForm_main"),

    /**
     * Jasper. Плановый график (с ростером). Видимость
     */
    JASPER_REPORTS_ROSTER_MAIN_SHOW("jasper.reports.rosterMain.show", "true"),

    /**
     * Jasper. Плановый график (с ростером). Название
     */
    JASPER_REPORTS_ROSTER_MAIN_CAPTION("jasper.reports.rosterWithLunch.caption", "Плановый график"),

    //Jasper Отчет "Отчет Т-12"
    /**
     * Jasper. Отчёт Т-12. Путь
     */

    JASPER_REPORTS_T12_PATH("jasper.reports.t12.path", "/reports/T12_main"),
    /**
     * Jasper. Отчёт Т-12. Видимость
     */
    JASPER_REPORTS_T12_SHOW("jasper.reports.t12.show", "true"),

    //Jasper Отчет "Отчет Т-13"
    /**
     * Jasper. Отчёт Т-13. Путь
     */
    JASPER_REPORTS_T13_PATH("jasper.reports.t13.path", "/reports/T13_main"),

    /**
     * Jasper.Отчёт Т-13. Видимость
     */
    JASPER_REPORTS_T13_SHOW("jasper.reports.t13.show", "true"),

    TWO_FACTOR_AUTH("two.factor.auth", "false"),

    MAX_SHIFT_LENGTH("maxShiftLength", "960"),

    MIN_SHIFT_LENGTH("minShiftLength", "240"),

    ROSTER_PUBLISH_WITHOUT_NORMS_LACK("roster.publish.without_norms_lack_both", "true"),

    ROSTER_QUIT_TAB_NOTICE("roster.quit.tab.notice", "true"),

    ROSTER_PUBLISH_WITHOUT_EXCEEDING_NORMS("roster.publish.without_exceeding_norms", "true"),
    ROSTER_PUBLISH_CHECK_INTERSECTIONS("roster.publish.check_intersections", "false"),
    ROSTER_PUBLISH_CHECK_INHERITANCE("roster.publish.check_inheritance", "false"),
    ROSTER_PUBLISH_WITHOUT_EXCEEDING_NORMS_CALCULATE_EXCHANGE_SEPARATELY("roster.publish.without_exceeding_norms.calculate_exchange_separately", "true"),
    ROSTER_PUBLISH_WITHOUT_CONFLICTS("roster.publish.without_conflicts", "true"),
    ROSTER_PUBLISH_CALC_TOTAL_LIMIT_EMPLOYEE("roster.publish.calc.total_limit_employee", "true"),
    SHIFT_CHECK_IN_WORKED_ROSTER_BEFORE_CREATING("shift.check.in.worked.roster.before.creating", "true"),

    BATCH_CALCULATION_USE_CALCULATION_API_FOR_SHIFTS("batchCalculationUseCalculationApiForShifts", "false"),
    SCHEDULE_BOARD_OUT_STAFF_ROWS("scheduleBoardOutStaffRows", "true"),
    SCHEDULE_BOARD_PERSONAL_TYPE_FILTER("scheduleBoardPersonalTypeFilter", "true"),
    APP_DEFAULT_LOCALE("appDefaultLocale", "ru"),
    SCHEDULE_BOARD_PLAN_FACT_MERGE("scheduleBoardPlanFactMerge", "false"),
    SCHEDULE_BOARD_CHECK_WORKED_DIFF_PLAN("check.worked.diff.plan", "false"),
    SCHEDULE_BOARD_HELP_INDICATOR("scheduleBoardHelpIndicator", "true"),
    SCHEDULE_BOARD_ATTACH_AGREEMENT("scheduleBoardAttachAgreement", "true"),
    SCHEDULE_REQUEST_SHOW_CREATE_TIME("scheduleRequestShowCreateTime", "true"),
    ALLOW_SHIFT_OVERTIME("allow.shift.overtime", "false"),
    ALLOW_OVERTIME_DUTY("allow.overtime.duty", "false"),
    WORKED_SHIFTS_EQUAL_PLAN("workedShift.cut.to.plan", "true"),
    EMPLOYEE_EXCLUDE_GROUPS("employeeExcludeGroups", "1 2 3 4 5 9"),
    PLAN_EDIT_FUTURE_DAYS("plan.edit.future.days", "0"),
    PLAN_EDIT_FUTURE_STRONG("plan.edit.future.strong", "false"),
    SCHEDULE_BOARD_NIGHT_HOURS_LIMITS("scheduleBoardNightHoursLimits", "[22, 6]"),
    /**
     * Функциональные роли, отображаемые в бейджах. [4] - back-оператор на почте
     */
    SCHEDULE_BOARD_BADGE_POSITION_GROUPS("scheduleBoardBadgePositionGroups", "[4]"),
    /**
     * Ограничение на просмотр табеля в месяцах
     */
    SCHEDULE_BOARD_TIMESHEET_PAST_MONTHS("scheduleBoardTimesheetPastMonths", "0"),
    /**
     * Включение блокировки табеля при выгрузке в 1С
     */
    CHECK_SHIFTS_ON_LOCK("check.shifts.on.lock", "false"),
    PLAN_EDIT_PAST_DAYS("plan.edit.past.days", "0"),
    SHIFT_EXCHANGE_ONLY_EXTERNAL("shift.exchange.only.external", "false"),
    SHIFT_EXCHANGE_ONLY_EXCHANGE("shift.exchange.only.exchange", "false"),
    SHIFT_EXCHANGE_SHOW_HIRING_REASON("shiftExchangeShowHiringReason", "false"),
    ADDITIONAL_INFORMATION_ON_SHIFT_EXCHANGE("additional.information.on.shift.exchange", "false"),
    ON_FREE_SHIFTS("onFreeShifts", "false"),
    SHIFT_EXCHANGE_USE_JOB_TITLE("shiftExchangeUseJobTitle", "false"),
    WORKED_SHIFT_CUT_TO_PLAN("workedShift.cut.to.plan", "false"),
    SCHEDULE_BOARD_SHIFTS_FROM_EXCHANGE_INDICATE("scheduleBoardShiftsFromExchangeIndicate", "true"),
    SHIFT_EXCHANGE_CREATE_VERSION_ON_ASSIGN("shift.exchange.create.version.on.assign", "true"),
    SHIFT_EXCHANGE_OUTSTAFF_WITH_ACCEPT("shift.exchange.outstaff.with.accept", "true"),
    /**
     * Сохранять смены под запросами расписания
     */
    KEEP_SHIFTS_UNDER_REQUESTS("keep.shifts.under.requests", "true"),
    SCHEDULE_BOARD_FRONT_BACK_INDICATOR("scheduleBoardFrontBackIndicator", "true"),
    SHIFT_ADDITIONAL_WORK_ENABLED("shift.additionalWorkEnabled", "false"),
    PAYOUTS_DAYS("payouts.days", ""),
    SHIFT_ADDITIONAL_WORK_ACTIVE_STATUSES("shift.shiftAdditionalWorkActiveStatuses", ""),
    SHIFT_WIZARD_DISABLE_DAY_OFF("shift.wizard.disable_day_off", "false"),
    REQUEST_COMMENTS_ALLOW("schedule.request.comments.allow", "true"),
    ENABLE_MULTIPLE_WORK_BREAKS("enableMultipleWorkBreaks", "true"),
    DEFAULT_LUNCH("defaultLunch", ""),
    SHIFT_COMMENT_REQUIRED_FOR_DELETING_WORKED_SHIFT("shift.comments.delete.required.fact", "false"),
    FULL_INTERVAL_NORMS("norm.fullIntervalNorms", ""),
    ENABLE_START_TIME_CHECK_FOR_WORKED_SHIFTS("shift.fact.start.time.check.enable", "false"),
    ROSTER_PUBLISH_WITHOUT_YEAR_OVERTIME_LIMIT_VIOLATION("roster.publish.without_year_overtime_limit_violation", "false"),
    LIMIT_CHECK_OUTSTAFF_ENABLED("limit.check.outstaff.enabled", "false"),
    SCHEDULE_BOARD_SHOW_LIMIT_CHANGE_NOTIFICATION("scheduleBoardShowLimitChangeNotification", "false"),
    ON_CONFLICTS("onConflicts", "false"),
    MULTIPLE_SHIFTS_IN_DAY("scheduleBoardMultipleShiftsInDay", "false"),
    WORKED_DAYS_APPROVE("worked.days.approve", ""),
    WORKED_OR_PUBLISHED_ROSTER("scheduleBoardWorkedOrPublishedRoster", "true"),
    MANAGER_PUBLICATION_NOTIFICATION_ALLOW("manager.publication.notification.allow", "false"),
    SHIFT_EXCHANGE_FREE_NOTIFICATION("shift.exchange.free.notification", "false"),
    SHIFT_EXCHANGE_MOBILE_APPROVE("shift.exchange.mobile.approve", "false"),

    /**
     * Отключить расчёт конфликтов на эфесе
     */
    DISABLE_CALCULATE_CONFLICTS("disable.calculate.conflicts", "false"),
    SCHEDULE_WIZARD_ENABLE_BREAKS("scheduleWizard.enableBreaks", "true"),
    ROSTER_SINGLE_EDITED_VERSION("roster.single_edited_version", "true"),
    BPMN_URL("bpmn.url", "http://localhost:8080"),
    BPMN_PROCESS_DEFINITION_SHIFT_EXCHANGE("bpmn.process_definition.shift_exchange", ""),
    /**
     * Отключить проверку лимитов на магните
     */
    ENABLE_TYPED_LIMITS_CHECK("enable.typed.limits.check", "false"),
    TIMESHEET_RULE_START_END_DATE_REQUIRED("timesheet.rule.start.and.end.date.required", "false"),
    /**
     * Отображать "Восстановление пароля" на странице входа
     */
    SHOW_PASSWORD_RECOVERY_ON_LOGIN("showPasswordRecoveryOnLogin", "false"),
    PASSWORD_COMPOSITION_REQUIREMENTS_MAXIMAL_LENGTH("password.composition.requirements.maximal.length", "20"),
    SHOW_BUTTON_TO_PUBLISH_ROSTER("showButtonToPublishRoster", "false"),
    LINK_IN_THE_PERSONAL_ACCOUNT_OF_THE_COUNTERPARTY("linkInThePersonalAccountOfTheCounterparty", "test"),
    DISPLAY_EMPLOYEES_FILTER_BY_ROLE("displayEmployeesFilterByRole", "false"),
    SCHEDULE_BOARD_TEMPORARY_SEPARATE("scheduleBoardTemporarySeparate", "false"),
    NORM_WORKED_SHIFT_ON_ROSTER_JOINT("norm.workedShiftOnRosterJoint", "false"),
    /**
     * Настройки друида
     */
    KPI_FORECAST_STORE_IS_DRUID("kpi.forecast.store.is.druid", "false"),
    KPI_STORE_IS_DRUID("kpi.store.is.druid", "false"),
    DRUID_ADAPTER_URL("druid.adapter.url", "false"),
    ENABLE_DRAG_AND_DROP_FUNCTION("enableDragAndDropFunction", "true"),
    FILTER_ALIAS_BY_ORG_UNIT_FORMAT("filter.alias.by.orgUnitFormat", "true"),
    NEW_DISPLAY_OF_WORK_PLACE("new.display.of.work.place", "false"),
    DISPLAY_THE_NEAREST_PARENT_OF_ORGUNIT("displayTheNearestParentOfOrgUnit", "false"),
    /**
     * Скрыть
     */
    HIDE_UI_FIELDS_IN_DESTINATION_CARDS("hideUIFieldsInDestinationCards", "NONE");

    private final String key;

    private final String defaultValue;

    SystemProperties(String key, String defaultValue) {
        this.defaultValue = defaultValue;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
