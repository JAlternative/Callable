package utils;

/**
 * Названия параметров для квери запроса
 */
public interface Params {

    String FULL_NAME = "fullname";
    String SHORT_NAME = "shortName";
    String ID = "id";
    String CATEGORY_ID = "categoryId";
    String FOR_EXCHANGE = "forExchange";
    String POSITION_INDEX = "positionIndex";
    String FTE_POSITION_GROUP = "ftePositionGroup";
    String CALCULATION_MODE = "calculationMode";
    String INCLUDE_DATE = "include-date";
    String SIZE = "size";
    String SORT = "sort";
    String LAST_NAME = "lastName";
    String ASC = "asc";
    String WITH_UNATTACHED = "with-unattached";
    String WITH_ACCOUNT = "with-account";
    String ONLY_NOW = "only-now";
    String FROM = "from";
    String ROLES = "roles";
    String TO = "to";
    String CALCULATE_CONSTRAINTS = "calculateConstraints";
    String PERSON_IDS = "person-ids";
    String PHOTO_URL = "photo-url";
    String PAGE = "page";
    String WITH_CHILDREN = "with-children";
    String ORG_UNIT_IDS = "org-unit-ids";
    String ORGANIZATION_UNIT_ID = "organizationUnitId";
    String ORG_UNIT_IDS_SELF = "org-unit-ids-self";
    String ORG_UNIT_IDS_CHILDREN = "org-unit-ids-children";
    String ORG_UNIT_CHILDREN = "orgUnitChildren";
    String DATE = "date";
    String FORMAT = "format";
    String ZIP = "zip";
    String WEEK = "week";
    String PLAN_ONLY = "plan-only";
    String POSITION_TYPE_IDS = "position-type-ids";
    String ORG_UNIT_TYPE_IDS = "org-unit-type-ids";
    String ORG_TYPE = "orgType";
    String LEVEL = "level";
    String KPI_ID = "kpiId";
    String CLOSE_BEFORE_DATE = "close-before-date";
    String OPEN_AFTER_DATE = "open-after-date";
    String TAG_IDS = "tag-ids";
    String NAME = "name";
    String ACTIVE = "active";
    String VISIBLE = "visible";
    String USE_MATH_PARAM = "use-math-param";
    String STRATEGY = "strategy";
    String SHIFT_TYPE = "shiftType";
    String ALT_ALGORITHM = "alt-algorithm";
    String ROSTER_ID = "roster-id";
    String ORG_UNIT_OUTER_ID = "organizationUnitOuterId";
    String TIME = "time";
    String TIME_UNIT = "time-unit";
    String PERSON_GROUPS = "person-groups";
    String PURPOSES = "purposes";
    String REROSTERING = "rerostering";
    String WITH_MIN_DEVIATION = "with-min-deviation";
    String PERSON_ID = "person-id";
    String EXCEPTION = "exception";
    String INCLUDE_CHIEF = "include-chief";
    String INCLUDE_OLD = "include-old";
    String CALCULATED = "calculated";

    String DESCRIPTION = "description";
    String ONLY_ACTIVE = "onlyActive";
    String PUBLISHED = "published";
    String MONTH = "month";
    String TYPE = "type";
    String HAL_JSON = "application/hal+json";
    String START_DATE_TIME = "startDateTime";
    String END_DATE_TIME = "endDateTime";
    String DATE_TIME_INTERVAL = "dateTimeInterval";
    String LENGTH_IN_HOURS = "lengthInHours";
    String EMPLOYEE_POSITION_ID = "employeePositionId";
    String EMPLOYEE_OUTER_ID = "employeeOuterId";
    String EMPLOYEE_POSITION = "employeePosition";
    String EMPLOYEE_POSITIONS = "employeePositions";
    String STATUS_NAME = "statusName";
    String POSITION_CATEGORY_ROSTER_ID = "positionCategoryRosterId";
    String ROSTER_TYPE = "rosterType";
    String STATUS = "status";
    String SUBTYPE = "subType";
    String BREAKS = "breaks";
    String ROSTER_ID_JSON = "rosterId";
    String STATUS_TYPE = "statusType";
    String SCHEDULE_REQUEST_RULE = "scheduleRequestRule";
    String SCHEDULE_REQUEST_TYPE = "scheduleRequestType";
    String SCHEDULE_REQUEST_ALIAS = "scheduleRequestAlias";

    String EMPLOYEES_ARRAY = "employees";
    String EMPLOYEE_JSON = "employee";
    String EMBEDDED = "_embedded";
    String LINKS = "_links";
    String SELF = "self";
    String HREF = "href";
    String VERSION = "version";
    String ORG_UNIT_JSON = "orgUnit";
    String REL_ORG_UNITS = "orgUnits";
    String REL_ORG_UNIT_SELF = "orgUnitSelf";
    String CONTENT = "content";
    String TEXT = "text";
    String ORG_NAME = "orgName";
    String PERSON_GROUP_IDS = "personGroupIds";
    String REPEAT_RULE = "repeatRule";
    String REPEAT = "repeat";
    String OUTER_ID = "outerId";
    String TAGS = "tags";
    String USERNAME = "username";
    String PASSWORD = "password";
    String CHANGE_PASSWORD = "changePassword";
    String FIRST_NAME = "firstName";
    String PATRONYMIC_NAME = "patronymicName";
    String PEOPLE_VALUE = "peopleValue";
    String ORG_UNIT_EVENT_TYPE = "organizationUnitEventType";
    String COMMENT = "comment";
    String COMMENT_TEXT = "commentText";
    String EMPLOYEE_ID = "employeeId";
    String POSITION_ID = "positionId";
    String POSITION_OUTER_ID = "positionOuterId";
    String START_DATE = "startDate";
    String END_DATE = "endDate";
    String DATE_INTERVAL = "dateInterval";
    String POSITION = "position";
    String COMMENTS = "comments";
    String USER = "user";
    String POSITIONS = "positions";
    String ISO_WEEK_DAY = "isoWeekday";
    String TIME_INTERVAL = "timeInterval";
    String START_TIME = "startTime";
    String END_TIME = "endTime";
    String ORG_UNITS_REMOVE = "org-units-remove";
    String REMOVE_ORG_UNITS_ALL = "removeOrgUnitsAll";
    String ORGANIZATION_UNIT_IDS = "orgUnitIds";
    String ORG_UNIT_ID = "orgUnitId";
    String ORG_UNIT_NAME = "orgUnitName";

    String BIO_API = "bio-api";

    String DATE_FROM = "dateFrom";
    String DATE_TO = "dateTo";
    String EMAIL = "email";
    String AVAILABLE_FOR_CALCULATION = "availableForCalculation";
    String ORG_UNIT_TYPE_ID = "organizationUnitTypeId";
    String POSITION_CATEGORY = "positionCategory";
    String POSITION_CATEGORY_ID = "positionCategoryId";
    String JOB_TITLE = "jobTitle";
    String JOB_TITLE_ID = "jobTitleId";
    String POSITION_TYPE = "positionType";
    String POSITION_GROUP = "positionGroup";
    String POSITION_GROUP_ID = "positionGroupId";
    String TITLE = "title";
    String LOCAL = "local";
    String TEMPORARY = "temporary";
    String BID_CREATED = "BID_CREATED";
    String HIDDEN = "_hidden";
    String DELETED = "_deleted";
    String PERIOD = "period";
    String PERIODICITY = "periodicity";
    String NO_REPEAT = "NO_REPEAT";
    String IS_OVERTIME_WORK_MODEL = "isOvertimeWorkModel";
    String IS_SHIFT_MODEL = "isShiftModel";
    String IS_LOGISTIC_TRIP_MODEL = "isLogisticTripModel";
    String FACT = "fact";
    String STANDARD = "standard";
    String PROP_KEY = "propKey";
    String VALUE = "value";
    String PARENT_VALUE = "parentValue";
    String VALUES = "values";
    String ENTITY_PROPERTIES_KEY = "entityPropertiesKey";
    String KEY = "key";
    String EXCHANGE_RULE = "exchangeRule";
    String EXCHANGE_STATUS = "exchangeStatus";
    String FROM_EXCHANGE = "fromExchange";
    String PUT_SHIFT_TO_EXCHANGE = "putShiftToExchange";
    String HIRING_REASON_TEXT = "hiringReasonText";
    String MATH_PARAMETER = "mathParameter";
    String COMMON_NAME = "commonName";
    String EXCHANGE_DAYS_TO_EDIT = "exchangeDaysToEdit";
    String ENABLED = "enabled";
    String AUTO_APPROVE = "autoApprove";
    String REQUIRE_APPROVAL = "requireApproval";
    String MOVE_TO_EXCHANGE = "moveToExchange";
    String BIND_TO_POSITION = "bindToPosition";
    String INHERITED = "inherited";
    String ROW = "row";
    String COL = "col";
    String LOGIC_GROUP_ID = "logicGroupId";
    String VIEW_TYPE = "viewType";
    String DEEP_EDIT = "deepEdit";
    String TIME_EDIT = "timeEdit";
    String FIXED_FAYS = "fixedDays";
    String DATA_TYPE = "dataType";
    String FOR_CALCULATE = "forCalculate";
    String CONSTRAINT_VIOLATIONS = "constraintViolations";
    String DISPLAY = "display";
    String SECURED_OPERATION_DESCRIPTORS = "securedOperationDescriptors";
    String MATH_PARAMETERS = "mathParameters";
    String LUNCH_RULES = "lunchRules";
    String TABLE_MODE_CREATE = "tableModeCreate";
    String LUNCH = "lunch";
    String ADD_WORKS = "addWorks";
    String RATE = "rate";
    String CODE = "code";
    String SHIFT = "shift";
    String SHIFT_ADD_WORK = "shiftAddWork";
    String OUTSTAFF = "outstaff";
    String USER_ROLE = "userRole";
    String ORG_SELF = "orgSelf";
    String ORG_CHILD = "orgChild";
    String CARD_NUMBER = "cardNumber";
    String CALC_JOBS = "calcJobs";
    String LAST_OF_ORG_UNITS = "last-of-org-units";
    String LIMIT = "limit";
    String LIMIT_TYPE = "limitType";
    String EXTENDED = "extended";
    String INTERVAL_TYPE = "intervalType";
    String EMPLOYEE_VALUE = "employeeValue";
    String SHIFT_VALUE = "shiftValue";
    String OUTSIDE = "outside";
    String EXCHANGE_RULE_RES_LIST = "exchangeRuleResList";
    String JOB_TITLE_SHIFT = "jobTitleShift";
    String JOB_TITLE_EMPLOYEE= "jobTitleEmployee";
    String PARENT_OUTER_ID = "parentOuterId";
    String ORG_UNIT_TYPE_OUTER_ID = "organizationUnitTypeOuterId";
    String ZONE_ID = "zoneId";
    String PROPERTIES = "properties";
    String NUMBER = "number";
    String START_WORK_DATE = "startWorkDate";
    String END_WORK_DATE = "endWorkDate";
    String GENDER = "gender";
    String SNILS = "snils";
    String NEED_MENTOR = "needMentor";
    String VIRTUAL = "virtual";
    String OUT_SOURCE = "outsource";
    String IS_EDIT = "isEdit";
    String DISMISSED = "dismissed";
    String SHIFT_HIRING_REASON_LIST = "shiftHiringReasonResList";
    String OPERATIONAL_ZONE_TITLE = "operationalZoneTitle";
    String OPERATIONAL_ZONE_ID = "operationalZoneTitle";
    String SHOW = "show";
    String SUCCESS = "success";
    String CALL_TYPE = "callType";
    String EVENTS = "events";
}
