package utils.tools;

import org.apache.http.NameValuePair;
import utils.Params;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static utils.Links.CALCULATION_MODE;
import static utils.Links.*;
import static utils.Links.JOB_TITLE_ID;
import static utils.Links.ORGANIZATION_UNIT_ID;
import static utils.Params.PERSON_GROUPS;
import static utils.Params.*;
import static utils.tools.RequestFormers.newNameValue;

/**
 * Билдер сделан для того, чтобы строить pairs для указания их в запросе
 */
public class Pairs {

    private final List<NameValuePair> pairs = new ArrayList<>();

    private Pairs() {
    }

    public static Builder newBuilder() {
        return new Pairs().new Builder();
    }

    public static DownloadBuilder newDownloadBuilder() {
        return new Pairs().new DownloadBuilder();
    }

    public static BioBuilder newBioBuilder() {
        return new Pairs().new BioBuilder();
    }

    public static IntegrationBuilder newIntegrationBuilder() {
        return new Pairs().new IntegrationBuilder();
    }

    public class BaseBuilder {
        private BaseBuilder() {
        }

        /**
         * Объединяет любые билдеры в один
         *
         * @param builder - билдер значения которого добавятся к текущему
         */
        public void doMergeBuilders(BaseBuilder... builder) {
            Stream.of(builder).forEach(b -> Pairs.this.pairs.addAll(b.build()));
        }

        /**
         * Объединяет список билдеров в один
         *
         * @param builders - билдеры значения которого добавятся к текущему
         * @return - текущий экземпляр билдера с добавленными значениями
         */
        public <T extends BaseBuilder> T doMergeBuilders(List<? extends BaseBuilder> builders) {
            builders.forEach(b -> Pairs.this.pairs.addAll(b.build()));
            return (T) this;
        }

        public List<NameValuePair> build() {
            return Pairs.this.pairs;
        }

        public Map<String, String> buildMap() {
            Map<String, String> map = new HashMap<>();
            for (NameValuePair pair : Pairs.this.pairs) {
                map.put(pair.getName(), pair.getValue());
            }
            return map;
        }
    }

    /**
     * Билдер общий для WFM
     */
    public class Builder extends BaseBuilder {

        private Builder() {
        }

        public Builder fullName(String fullName) {
            Pairs.this.pairs.add(newNameValue(FULL_NAME, fullName));
            return this;
        }

        public Builder includeDate(LocalDate date) {
            Pairs.this.pairs.add(newNameValue(INCLUDE_DATE, date));
            return this;
        }

        public Builder page(int page) {
            Pairs.this.pairs.add(newNameValue(PAGE, page));
            return this;
        }

        public Builder size(int size) {
            Pairs.this.pairs.add(newNameValue(SIZE, size));
            return this;
        }

        public Builder sort(String sort) {
            Pairs.this.pairs.add(newNameValue(SORT, sort));
            return this;
        }

        public Builder comment(String comment) {
            Pairs.this.pairs.add(newNameValue(Params.COMMENT, comment));
            return this;
        }

        public Builder withUnattached(boolean withUnattached) {
            Pairs.this.pairs.add(newNameValue(WITH_UNATTACHED, withUnattached));
            return this;
        }

        public Builder from(LocalDate from) {
            Pairs.this.pairs.add(newNameValue(FROM, from));
            return this;
        }

        public Builder to(LocalDate to) {
            Pairs.this.pairs.add(newNameValue(TO, to));
            return this;
        }

        public Builder rerostering(boolean rerostering) {
            Pairs.this.pairs.add(newNameValue(REROSTERING, rerostering));
            return this;
        }

        public Builder onlyNow(boolean onlyNow) {
            Pairs.this.pairs.add(newNameValue(ONLY_NOW, onlyNow));
            return this;
        }

        public Builder withMinDeviation(boolean withMinDeviation) {
            Pairs.this.pairs.add(newNameValue(WITH_MIN_DEVIATION, withMinDeviation));
            return this;
        }

        public Builder onlyActive(boolean onlyActive) {
            Pairs.this.pairs.add(newNameValue(ONLY_ACTIVE, onlyActive));
            return this;
        }

        public Builder calculateConstraints(boolean calculateConstraints) {
            Pairs.this.pairs.add(newNameValue(CALCULATE_CONSTRAINTS, calculateConstraints));
            return this;
        }

        public Builder force(boolean force) {
            Pairs.this.pairs.add(newNameValue("force", force));
            return this;
        }

        public Builder includeChief(boolean includeChief) {
            Pairs.this.pairs.add(newNameValue(INCLUDE_CHIEF, includeChief));
            return this;
        }

        public Builder orgUnitIds(java.io.Serializable orgUnitIds) {
            Pairs.this.pairs.add(newNameValue(ORG_UNIT_IDS, orgUnitIds));
            return this;
        }

        public Builder orgUnitId(java.io.Serializable orgUnitId) {
            Pairs.this.pairs.add(newNameValue(ORGANIZATION_UNIT_ID, orgUnitId));
            return this;
        }

        public Builder outerId(java.io.Serializable outerId) {
            Pairs.this.pairs.add(newNameValue("outer-id", outerId));
            return this;
        }

        public Builder includeOld(boolean includeOld) {
            Pairs.this.pairs.add(newNameValue(INCLUDE_OLD, includeOld));
            return this;
        }

        public Builder calculated(boolean calculated) {
            Pairs.this.pairs.add(newNameValue(CALCULATED, calculated));
            return this;
        }

        public Builder excludeOther(boolean excludeOther) {
            Pairs.this.pairs.add(newNameValue("exclude-other", excludeOther));
            return this;
        }

        public Builder entity(String entity) {
            Pairs.this.pairs.add(newNameValue(ENTITY, entity));
            return this;
        }

        public Builder withChildren(boolean withChildren) {
            Pairs.this.pairs.add(newNameValue(WITH_CHILDREN, withChildren));
            return this;
        }

        public Builder name(String name) {
            Pairs.this.pairs.add(newNameValue(NAME, name));
            return this;
        }

        public Builder openAfterDate(LocalDate openAfterDate) {
            Pairs.this.pairs.add(newNameValue(OPEN_AFTER_DATE, openAfterDate));
            return this;
        }

        public Builder closeBeforeDate(LocalDate closeBeforeDate) {
            Pairs.this.pairs.add(newNameValue(CLOSE_BEFORE_DATE, closeBeforeDate));
            return this;
        }

        public Builder timeUnit(String timeUnit) {
            Pairs.this.pairs.add(newNameValue(TIME_UNIT, timeUnit));
            return this;
        }

        public Builder level(int level) {
            Pairs.this.pairs.add(newNameValue(LEVEL, level));
            return this;
        }

        public Builder kpiId(int kpiId) {
            Pairs.this.pairs.add(newNameValue(KPI_ID, kpiId));
            return this;
        }

        public Builder year(int year) {
            Pairs.this.pairs.add(newNameValue("year", year));
            return this;
        }

        public Builder month(int month) {
            Pairs.this.pairs.add(newNameValue("month", month));
            return this;
        }

        public Builder eventIds(int eventIds) {
            Pairs.this.pairs.add(newNameValue("event-ids", eventIds));
            return this;
        }

        public Builder kpiIds(int kpiIds) {
            Pairs.this.pairs.add(newNameValue("kpi-ids", kpiIds));
            return this;
        }

        public Builder positionGroupIds(int positionGroupIds) {
            Pairs.this.pairs.add(newNameValue("position-group-ids", positionGroupIds));
            return this;
        }

        public Builder withAccount(boolean withAccount) {
            Pairs.this.pairs.add(newNameValue(WITH_ACCOUNT, withAccount));
            return this;
        }

        public Builder orgUnitTypeIds(int orgUnitTypeIds) {
            Pairs.this.pairs.add(newNameValue(ORG_UNIT_TYPE_IDS, orgUnitTypeIds));
            return this;
        }

        public Builder calculationMode(String calculationMode) {
            Pairs.this.pairs.add(newNameValue(CALCULATION_MODE, calculationMode));
            return this;
        }

        public Builder roleIds(int roleIds) {
            Pairs.this.pairs.add(newNameValue("role-ids", roleIds));
            return this;
        }

        public Builder positionNames(String positionNames) {
            Pairs.this.pairs.add(newNameValue(POSITION_NAMES, positionNames));
            return this;
        }

        public Builder tagIds(String tagIds) {
            Pairs.this.pairs.add(newNameValue(TAG_IDS, tagIds));
            return this;
        }

        public Builder orgUnitIdsSelf(Integer orgUnitIdsSelf) {
            Pairs.this.pairs.add(newNameValue(ORG_UNIT_IDS_SELF, orgUnitIdsSelf));
            return this;
        }

        public Builder orgUnitIdsChildren(String orgUnitIdsChildren) {
            Pairs.this.pairs.add(newNameValue(ORG_UNIT_IDS_CHILDREN, orgUnitIdsChildren));
            return this;
        }

        public Builder exclude(String constraintViolation) {
            Pairs.this.pairs.add(newNameValue(EXCLUDE, constraintViolation));
            return this;
        }

        public Builder excludeIgnored(boolean excludeIgnored) {
            Pairs.this.pairs.add(newNameValue(EXCLUDE_IGNORED, excludeIgnored));
            return this;
        }

        public Builder employeePositionIds(String employeePositionIds) {
            Pairs.this.pairs.add(newNameValue("employee-position-ids", employeePositionIds));
            return this;
        }

        public Builder accuracyInMinutes(int minutes) {
            Pairs.this.pairs.add(newNameValue("accuracy-in-minutes", minutes));
            return this;
        }

        public Builder type(String typeOfBatch) {
            Pairs.this.pairs.add(newNameValue(TYPE, typeOfBatch));
            return this;
        }

        public Builder received(boolean received) {
            Pairs.this.pairs.add(newNameValue("received", received));
            return this;
        }

        public Builder deleted(boolean deleted) {
            Pairs.this.pairs.add(newNameValue("deleted", deleted));
            return this;
        }

        public Builder clientZoneOffset(String offset) {
            Pairs.this.pairs.add(newNameValue("clientZoneOffset", offset));
            return this;
        }

        public Builder expired_millis(int ms) {
            Pairs.this.pairs.add(newNameValue("expired-millis", ms));
            return this;
        }

        public Builder jobTitleId(int jobTitleId) {
            Pairs.this.pairs.add(newNameValue(JOB_TITLE_ID, jobTitleId));
            return this;
        }

        public Builder stopOnError(boolean stopOnError) {
            Pairs.this.pairs.add(newNameValue("stop-on-error", stopOnError));
            return this;
        }

        public Builder deleteIntersections(boolean deleteIntersections) {
            Pairs.this.pairs.add(newNameValue("delete-intersections", deleteIntersections));
            return this;
        }

        public Builder splitRequests(boolean splitRequests) {
            Pairs.this.pairs.add(newNameValue("split-requests", splitRequests));
            return this;
        }

        public Builder processShifts(String operation) {
            Pairs.this.pairs.add(newNameValue("process-shifts", operation));
            return this;
        }

        public Builder startDateShiftFilter(boolean startDateShiftFilter) {
            Pairs.this.pairs.add(newNameValue("start-date-shift-filter", startDateShiftFilter));
            return this;
        }

        public Builder openPrevEmployeePosition(boolean openPrevEmployeePosition) {
            Pairs.this.pairs.add(newNameValue("open-prev-employee-position", openPrevEmployeePosition));
            return this;
        }
    }

    /**
     * Билдер для БИО
     */
    public class BioBuilder extends BaseBuilder {
        private BioBuilder() {
        }

        public BioBuilder ids(String ids) {
            Pairs.this.pairs.add(newNameValue("ids", ids));
            return this;
        }

        public BioBuilder size(int size) {
            Pairs.this.pairs.add(newNameValue(SIZE, size));
            return this;
        }

        public BioBuilder active(boolean active) {
            Pairs.this.pairs.add(newNameValue(ACTIVE, active));
            return this;
        }

        public BioBuilder photoUrl(boolean photoUrl) {
            Pairs.this.pairs.add(newNameValue(PHOTO_URL, photoUrl));
            return this;
        }

        public BioBuilder personGroups(String personGroups) {
            Pairs.this.pairs.add(newNameValue(PERSON_GROUPS, personGroups));
            return this;
        }

        public BioBuilder from(String from) {
            Pairs.this.pairs.add(newNameValue(FROM, from));
            return this;
        }

        public BioBuilder to(String to) {
            Pairs.this.pairs.add(newNameValue(TO, to));
            return this;
        }

        public BioBuilder purposes(String purposes) {
            Pairs.this.pairs.add(newNameValue(PURPOSES, purposes));
            return this;
        }

        public BioBuilder exception(String exception) {
            Pairs.this.pairs.add(newNameValue(EXCEPTION, exception));
            return this;
        }

        public BioBuilder personId(String personId) {
            Pairs.this.pairs.add(newNameValue(PERSON_ID, personId));
            return this;
        }

    }

    public class IntegrationBuilder extends BaseBuilder {
        private IntegrationBuilder() {
        }

        public IntegrationBuilder formatTt(String formatTt) {
            Pairs.this.pairs.add(newNameValue("format-tt", formatTt));
            return this;
        }

        public IntegrationBuilder period(LocalDate period) {
            Pairs.this.pairs.add(newNameValue("period", period.withDayOfMonth(1)));
            return this;
        }

        public IntegrationBuilder updatedFrom(LocalDateTime updatedFrom) {
            Pairs.this.pairs.add(newNameValue("updated-from", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").format(updatedFrom)));
            return this;
        }

        public IntegrationBuilder updatedTo(LocalDateTime updatedTo) {
            Pairs.this.pairs.add(newNameValue("updated-to", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").format(updatedTo)));
            return this;
        }

        public IntegrationBuilder concreteDates(boolean concreteDates) {
            Pairs.this.pairs.add(newNameValue("concrete-dates", concreteDates));
            return this;
        }

        public IntegrationBuilder processShifts(String processShifts) {
            Pairs.this.pairs.add(newNameValue("process-shifts", processShifts));
            return this;
        }

        public IntegrationBuilder forecast(Boolean forecast) {
            Pairs.this.pairs.add(newNameValue("forecast", forecast));
            return this;
        }
    }

    /**
     * Билдер для составления URL в download чекерах
     */
    public class DownloadBuilder extends BaseBuilder {
        private DownloadBuilder() {
        }

        public DownloadBuilder from(java.io.Serializable from) {
            Pairs.this.pairs.add(newNameValue(FROM, from));
            return this;
        }

        public DownloadBuilder to(LocalDate to) {
            Pairs.this.pairs.add(newNameValue(TO, to));
            return this;
        }

        public DownloadBuilder orgUnitIds(java.io.Serializable orgUnitIds) {
            Pairs.this.pairs.add(newNameValue(ORG_UNIT_IDS, orgUnitIds));
            return this;
        }

        public DownloadBuilder organizationUnitId(java.io.Serializable orgUnitIds) {
            Pairs.this.pairs.add(newNameValue("organization-unit-id", orgUnitIds));
            return this;
        }

        public DownloadBuilder date(LocalDate date) {
            Pairs.this.pairs.add(newNameValue(DATE, date));
            return this;
        }

        public DownloadBuilder year(Integer year) {
            Pairs.this.pairs.add(newNameValue("year", year));
            return this;
        }

        public DownloadBuilder month(Integer month) {
            Pairs.this.pairs.add(newNameValue(MONTH, month));
            return this;
        }

        public DownloadBuilder zip(boolean zip) {
            Pairs.this.pairs.add(newNameValue(ZIP, zip));
            return this;
        }

        public DownloadBuilder format(String format) {
            Pairs.this.pairs.add(newNameValue(FORMAT, format));
            return this;
        }

        public DownloadBuilder orgUnitIdsSelf(String orgUnitIdsSelf) {
            Pairs.this.pairs.add(newNameValue(ORG_UNIT_IDS_SELF, orgUnitIdsSelf));
            return this;
        }

        public DownloadBuilder orgUnitIdsChildren(String orgUnitIdsChildren) {
            Pairs.this.pairs.add(newNameValue(ORG_UNIT_IDS_CHILDREN, orgUnitIdsChildren));
            return this;
        }

        public DownloadBuilder useMathParam(boolean useMathParam) {
            Pairs.this.pairs.add(newNameValue(USE_MATH_PARAM, useMathParam));
            return this;
        }

        public DownloadBuilder strategy(String strategy) {
            Pairs.this.pairs.add(newNameValue(STRATEGY, strategy));
            return this;
        }

        public DownloadBuilder altAlgorithm(int altAlgorithm) {
            Pairs.this.pairs.add(newNameValue(ALT_ALGORITHM, altAlgorithm));
            return this;
        }

        public DownloadBuilder jUsername(String jUsername) {
            Pairs.this.pairs.add(newNameValue("j_username", jUsername));
            return this;
        }

        public DownloadBuilder jPassword(Object jPassword) {
            Pairs.this.pairs.add(newNameValue("j_password", jPassword));
            return this;
        }

        public DownloadBuilder departmentId(int departmentId) {
            Pairs.this.pairs.add(newNameValue("DepartmentId", departmentId));
            return this;
        }

        public DownloadBuilder startDate(LocalDate startDate) {
            Pairs.this.pairs.add(newNameValue("StartDate", startDate));
            return this;
        }

        public DownloadBuilder endDate(LocalDate endDate) {
            Pairs.this.pairs.add(newNameValue("EndDate", endDate));
            return this;
        }

        public DownloadBuilder personIds(String personIds) {
            Pairs.this.pairs.add(newNameValue(PERSON_IDS, personIds));
            return this;
        }

        public DownloadBuilder photoUrl(boolean photoUrl) {
            Pairs.this.pairs.add(newNameValue(PHOTO_URL, photoUrl));
            return this;
        }

        public DownloadBuilder size(int size) {
            Pairs.this.pairs.add(newNameValue(SIZE, size));
            return this;
        }

        public DownloadBuilder employeePositionIds(String employeePositionIds) {
            Pairs.this.pairs.add(newNameValue("EmployeePositionIds", employeePositionIds));
            return this;
        }

        public DownloadBuilder employeeIds(String employeeIds) {
            Pairs.this.pairs.add(newNameValue("EmployeeIds", employeeIds));
            return this;
        }

        public DownloadBuilder excludeEmployeesFromOtherOrganizations(Boolean exclude) {
            Pairs.this.pairs.add(newNameValue("excludeEmployeesFromOtherOrganizations", exclude));
            return this;
        }

        public DownloadBuilder week(boolean week) {
            Pairs.this.pairs.add(newNameValue(WEEK, week));
            return this;
        }

        public DownloadBuilder planOnly(boolean planOnly) {
            Pairs.this.pairs.add(newNameValue(PLAN_ONLY, planOnly));
            return this;
        }

        public DownloadBuilder normalized(boolean normalized) {
            Pairs.this.pairs.add(newNameValue("normalized", normalized));
            return this;
        }

        public DownloadBuilder positionTypeIds(String positionTypeIds) {
            Pairs.this.pairs.add(newNameValue(POSITION_TYPE_IDS, positionTypeIds));
            return this;
        }

        public DownloadBuilder rosterId(String rosterId) {
            Pairs.this.pairs.add(newNameValue("RosterId", rosterId));
            return this;
        }

        public DownloadBuilder roster_Id(String rosterId) {
            Pairs.this.pairs.add(newNameValue("roster-id", rosterId));
            return this;
        }

    }

}
