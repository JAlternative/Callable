package utils.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uwyn.jhighlight.tools.ExceptionUtils;
import io.qameta.allure.Allure;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.Reporter;
import utils.Links;
import utils.Params;
import utils.authorization.CsvLoader;
import wfm.models.Employee;
import wfm.models.OrgUnit;
import wfm.models.Position;
import wfm.models.Shift;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static utils.integration.IntegrationUtils.convertClassNameToDBType;
import static wfm.repository.CommonRepository.URL_BASE;

public class DBUtils {
    private static final String DB_URL = Links.getTestProperty("database");
    private static final Logger LOG = LoggerFactory.getLogger(DBUtils.class);
    private static final String DATA_BASE_ENTRY_ADDED_CONTEXT_PREFIX = "DataBaseEntryAddedForTest_";
    private static String intBaseName;
    private static final String ATTACHMENT_NAME_REQUEST = "Запрос в БД";
    private static final String ATTACHMENT_NAME_RESPONSE = "Ответ БД";

    public static String getPrefix() {
        return DATA_BASE_ENTRY_ADDED_CONTEXT_PREFIX;
    }

    private DBUtils() {
        throw new IllegalStateException("Utility class");
    }

    static String[] getCredentials() {
        return CsvLoader.databaseCredentialsReturner();
    }

    static String getDataBaseAddress() {
        String databaseName = URL_BASE.substring(URL_BASE.indexOf("/") + 2, URL_BASE.indexOf(".")).replace("-", "_");
        if (URL_BASE.contains("zozo")) {
            return DB_URL + "goodt_wfm_qa";
        } else if (URL_BASE.contains("magnit-birzha.t")) {
            return DB_URL + "magnit_birzha_test";
        } else if (URL_BASE.contains("pochta-wfm-release")) {
            return Links.getTestProperty("database_updated") + databaseName;
        } else if (URL_BASE.contains("pochta-wfm-qa")) {
            return Links.getTestProperty("database_master") + "hrportal";
        } else if (URL_BASE.contains("magnitqa-wfm")) {
            return Links.getTestProperty("database_magnit_qa") + "magnit_wfm_qa";
        } else if (URL_BASE.contains("magnit-master")) {
            return Links.getTestProperty("database_magnit_master") + "hrportal";
        } else if (URL_BASE.contains("efes-master")) {
            return Links.getTestProperty("database_efes_master") + "hrportal";
        } else {
            return DB_URL + databaseName;
        }

    }

    static String getIntegrationDatabaseAddress() {
        return DB_URL + intBaseName;
    }

    public static int genericExecutor(String query, Object id, boolean wfm) {
        QueryRunner runner = new QueryRunner();
        int response = 0;
        try {
            Connection conn;
            if (wfm) {
                conn = WFMDBConnectionProvider.getInstance().getConnection();
            } else {
                conn = IntDBConnectionProvider.getInstance().getConnection();
            }
            response = runner.execute(conn, query, id);
            LOG.info(query.replace("?", "{}"), id);
        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getMessage().contains("permission denied")) {
                String accessDenied = "Отказано в доступе к БД";
                Assert.fail(intBaseName == null ? accessDenied : accessDenied + " " + intBaseName);
            }
        }
        return response;
    }

    /**
     * Получает из базы данных все блокировки ростера для подразделения.
     * Возвращает список мепов, а не объекты. В теории можно было бы подружить метод с хендлером, который вернет объект, но это предполагает навешивание дополнительных
     * аннотаций на POJO
     *
     * @param omId айди подразделения
     * @return список мэпов, содержащих информацию о блокировках
     */
    public static List<Map<String, Object>> getRosterLocksForOrgUnit(int omId) {
        QueryRunner runner = new QueryRunner();
        String idSQL = "SELECT * FROM roster_lock rl WHERE org_unit_id=?;";
        List<Map<String, Object>> result = null;
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, idSQL, new MapListHandler(), omId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String log = idSQL.replace("?", "{}");
        LOG.info(log, omId);
        return result;
    }

    public static List<Shift> getShifts(int epId) {
        QueryRunner runner = new QueryRunner();
        String idSQL = "SELECT * FROM shift WHERE employee_position_id=?;";
        List<Shift> result = null;
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
            result = runner.query(conn, idSQL, new MapListHandler(), epId)
                    .stream().map(response -> objectMapper.convertValue(response, Shift.class))
                    .map(Shift::setDateTimeInterval)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String log = idSQL.replace("?", "{}");
        LOG.info(log, epId);
        return result;
    }

    /**
     * Получить список сотрудников по названию должности
     */
    public static List<Employee> getEmployeesByPositionName(String positionName) {
        //todo у элементов в списке result указываются только id и снилс, а всё остальное - null или false
        QueryRunner runner = new QueryRunner();
        String sql = String.format("SELECT employee.* " +
                                           "FROM employeeposition, employee, position " +
                                           "WHERE position.name='%s' and position.id=employeeposition.position_id and employeeposition.employee_id=employee.id " +
                                           "GROUP by employee.id",
                                   positionName);
        List<Employee> result = null;

        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
            result = runner.query(conn, sql, new MapListHandler())
                    .stream().map(response -> objectMapper.convertValue(response, Employee.class))
                    .collect(Collectors.toList());
            LOG.info("Получен список сотрудников по названию должности");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static List<Map<String, Object>> getPosGroupNameAndPosNameFromOmWithFormat(String format) {
        //todo у элементов в списке result указываются только id и снилс, а всё остальное - null или false
        QueryRunner runner = new QueryRunner();
        String sql = String.format("SELECT position_name, position_group_name " +
                                           "FROM dictionary_position_type_wfm " +
                                           "WHERE position_org_unit_entity_property=?;");
        List<Map<String, Object>> result = null;
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, sql, new MapListHandler(), format);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String log = sql.replace("?", "{}");
        LOG.info(log, format);
        return result;
    }

    public static Shift getShiftById(int id) {
        QueryRunner runner = new QueryRunner();
        String idSQL = "SELECT * FROM shift WHERE id=?;";
        Shift result = null;
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
            result = objectMapper.convertValue(runner.query(conn, idSQL, new MapHandler(), id), Shift.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String log = idSQL.replace("?", "{}");
        LOG.info(log, id);
        return result;
    }

    /**
     * Добавляет период блокировки ростера к подразделению
     *
     * @param omId     айди подразделения
     * @param epId     айди пользователя, для которого нужно заблокировать изменения. Может быть null
     * @param dateFrom дата начала блокировки. Может быть null
     * @param dateTo   дата окончания блокировки
     * @return номер добавленной строки
     */
    public static long addRosterLockForOrgUnit(int omId, Integer epId, LocalDate dateFrom, LocalDate dateTo) {
        QueryRunner runner = new QueryRunner();
        String idSQL = "INSERT INTO roster_lock (org_unit_id, employee_position_id, date_from, date_to) VALUES (?, ?, ?, ?) RETURNING id;";
        Timestamp from = null;
        if (dateFrom != null) {
            from = Timestamp.valueOf(dateFrom.atStartOfDay());
        }
        Timestamp to = Timestamp.valueOf(dateTo.atStartOfDay());
        long id = 0;
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            id = runner.insert(conn, idSQL, new ScalarHandler<>(), omId, epId, from, to);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Assert.assertNotEquals(id, 0, "Не удалось добавить период блокировки ростера в базу данных");
        String log = idSQL.replace("?", "{}");
        Allure.addAttachment("Пресет по добавлению периода блокировки ростера",
                             String.format("Ростер был заблокирован с %s по %s%n" +
                                                   "Был отправлен запрос: %n" + idSQL.replace("?", "%s"),
                                           dateFrom, dateTo, omId, epId, from, to));
        LOG.info(log, omId, epId, from, to);
        Reporter.getCurrentTestResult().getTestContext().setAttribute(DATA_BASE_ENTRY_ADDED_CONTEXT_PREFIX + id, id);
        return id;
    }

    /**
     * Удаляет период блокировки ростера подразделения по номеру id строки в БД
     *
     * @param lineId номер строки в таблице roster_lock, которая содержит информацию об удаляемой блокировке
     * @return количество удаленных строк
     */
    public static int removeRosterLockForOrgUnitById(long lineId) {
        String sql = "DELETE FROM roster_lock WHERE id=?;";
        return genericExecutor(sql, lineId, true);
    }

    public static int removeRosterLocksForOrgUnit(long omId) {
        String sql = "DELETE FROM roster_lock WHERE org_unit_id=?;";
        return genericExecutor(sql, omId, true);
    }

    /**
     * Отмечает табельную смену как смену, назначенную с биржи
     */
    public static void makeShiftFromExchange(Shift shift) {
        int id = shift.getId();
        String sql = "UPDATE shift SET extended_status = 'EXCHANGE' where id=?;";
        int response = genericExecutor(sql, id, true);
        Assert.assertNotEquals(response, 0, "Смена не была обновлена");
        Allure.addAttachment("Изменение смены через БД", "Смена за " + shift.getDateTimeInterval() + " была зарегистрирована как взятая с биржи.");
    }

    public static void resetLastUpdate(String entityAlias, String baseName) {
        String sql = "UPDATE last_update_date_time SET update_date_time='" + LocalDate.now() + " 00:00:00.000' WHERE UPPER(entity_alias) LIKE UPPER(?);";
        intBaseName = baseName;
        int response = genericExecutor(sql, "target_%" + entityAlias, false);
        Allure.addAttachment("Отправка запроса в БД", String.format("Отправлен запрос %s%nизменено %d записей", sql, response));
    }

    /**
     * Достает из интеграционной БД смену или запрос по айди в WFM
     */
    public static Map<String, Object> getIntegrationRequestById(int id) {
        QueryRunner runner = new QueryRunner();
        String idSQL = "SELECT updated, ep_fullname, wfm_id, startdatetime, enddatetime, \"type\", planned FROM shift_extended WHERE wfm_id=?;";
        Map<String, Object> result = null;
        try {
            Connection conn = IntDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, idSQL, new MapHandler(), id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String log = idSQL.replace("?", "{}");
        LOG.info(log, id);
        return result;
    }

    /**
     * Достает из интеграционной БД все запросы заданного типа, которые имели место после заданной даты
     *
     * @param date     дата, после которой ищем
     * @param type     типа запроса: смена или запрос отсутствия
     * @param baseName название БД
     */
    public static List<Map<String, Object>> getIntegrationRequests(LocalDate date, Class type, String baseName) {
        intBaseName = baseName;
        String sql = "SELECT * FROM shift_extended WHERE \"date\" > ? and \"type\"=?;";
        List<Map<String, Object>> result = null;
        QueryRunner runner = new QueryRunner();
        String typeName = convertClassNameToDBType(type);
        try {
            Connection conn = IntDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, sql, new MapListHandler(), date, typeName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String log = sql.replace("?", "{}");
        LOG.info(log, date, typeName);
        return result;
    }

    public static void deleteIntegrationShifts() {
        QueryRunner runner = new QueryRunner();
        String idSQL = "DELETE FROM shift_extended;";
        LOG.info(idSQL);
        try {
            Connection conn = IntDBConnectionProvider.getInstance().getConnection();
            int result = runner.execute(conn, idSQL);
            LOG.info(String.valueOf(result));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Object> getLatestRequest() {
        String sql = "SELECT * FROM schedule_request ORDER BY insert_time DESC LIMIT 1;";
        return executeSqlQuery(sql);
    }

    public static Map<String, Object> getCalcJobById(int id) {
        String sql = String.format("SELECT create_time, creator_id FROM calculate_job WHERE id=%s;", id);
        Map<String, Object> result = new HashMap<>();
        QueryRunner runner = new QueryRunner();
        Allure.addAttachment(ATTACHMENT_NAME_REQUEST, sql);
        LOG.info(sql);
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, sql, new MapHandler());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int getSuperuserId() {
        String sql = "SELECT * FROM public.user WHERE username='superuser';";
        Map<String, Object> result = new HashMap<>();
        QueryRunner runner = new QueryRunner();
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, sql, new MapHandler());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Allure.addAttachment(ATTACHMENT_NAME_REQUEST, sql);
        LOG.info(sql);
        return ((Number) result.get(Params.ID)).intValue();
    }

    public static List<Map<String, Object>> getEmployeePositionByOuterId(String outerId) {
        String sql = "select e2.startdate, e2.enddate, e.outerid as e_outerid, p.outerid as p_outerid, e2.card_number from employeeposition e2\n" +
                "join employee e on e2.employee_id = e.id\n" +
                "join position p on e2.position_id = p.id\n" +
                "where e.outerid = ?;";
        List<Map<String, Object>> result = null;
        QueryRunner runner = new QueryRunner();
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, sql, new MapListHandler(), outerId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String log = sql.replace("?", "{}");
        LOG.info(log, outerId);
        Allure.addAttachment(ATTACHMENT_NAME_REQUEST, sql.replace("?", outerId));
        Allure.addAttachment(ATTACHMENT_NAME_RESPONSE, String.valueOf(result));
        return result;
    }

    public static List<Map<String, Object>> checkDuplicateDescriptors(String description) {
        String sql = "SELECT *\n" +
                "FROM permission_role\n" +
                "GROUP BY permission_id , custom_role_id \n" +
                "HAVING COUNT(permission_id) >1;";
        List<Map<String, Object>> result = null;
        QueryRunner runner = new QueryRunner();
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, sql, new MapListHandler());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        LOG.info(sql);
        Allure.addAttachment(description, "Если задвоений нет, то список на следующей строке будет пустым\n" + result);
        return result;
    }

    /**
     * Вернёт true, если в подразделении содержатся лимиты на текущий месяц
     */
    public static Boolean hasLimitsInCurrentMonth(OrgUnit orgUnit, boolean checkContainingLimits) {
        if (!checkContainingLimits) {
            return true;
        }
        String sql = String.format("SELECT * FROM kpibasevalue WHERE organizationunit_id=%s AND datetime = '%s';", orgUnit.getId(), LocalDate.now().withDayOfMonth(1));
        List<Map<String, Object>> result = new ArrayList<>();
        QueryRunner runner = new QueryRunner();
        Allure.addAttachment(ATTACHMENT_NAME_REQUEST, sql);
        LOG.info(sql);
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, sql, new MapListHandler());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return !result.isEmpty();
    }

    /**
     * Вернёт true, если ростер не заблокирован (не находится в таблице roster_lock)
     */
    public static Boolean hasRosterNotLocked(OrgUnit orgUnit, boolean checkRosterLocked) {
        if (!checkRosterLocked) {
            return true;
        }
        String sql = String.format("SELECT * FROM roster_lock WHERE org_unit_id=%s", orgUnit.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        QueryRunner runner = new QueryRunner();
        Allure.addAttachment(ATTACHMENT_NAME_REQUEST, sql);
        LOG.info(sql);
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, sql, new MapListHandler());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result.isEmpty();
    }

    public static Map<String, Object> executeSqlQuery(String sql) {
        Map<String, Object> result = new HashMap<>();
        QueryRunner runner = new QueryRunner();
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, sql, new MapHandler());
        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getMessage().contains("permission denied")) {
                String accessDenied = "Отказано в доступе к БД";
                Assert.fail(intBaseName == null ? accessDenied : accessDenied + " " + intBaseName);
            } else {
                Allure.addAttachment("Стектрейс неудачного подключения к бд", ExceptionUtils.getExceptionStackTrace(e));
                Assert.fail(e.getMessage());
            }
        }
        Allure.addAttachment(ATTACHMENT_NAME_REQUEST, sql);
        LOG.info(sql);
        return result;
    }

    public static Integer getIntegrationCallResultIdByMessage(String message) {
        String sql = String.format("SELECT integrationcallresult_id FROM integrationevent WHERE message=\'%s\';", message);
        return ((Number) executeSqlQuery(sql).get("integrationcallresult_id")).intValue();
    }

    public static Integer getIntegrationeventIdByCallResultIdAndMessage(Integer integrationCallId, String message) {
        String sql = String.format("SELECT id FROM integrationevent WHERE integrationcallresult_id=\'%s\' and message=\'%s\';", integrationCallId, message);
        return ((Number) executeSqlQuery(sql).get("id")).intValue();
    }

    public static Integer getIntegrationCallResultId(String sql) {
        return ((Number) executeSqlQuery(sql).get("id")).intValue();
    }

    public static boolean getIntegrationCallResultSuccess(String sql) {
        return (boolean) executeSqlQuery(sql).get("success");
    }

    public static String getValueByKpiBaseValue(String sql) {
        return executeSqlQuery(sql).get("value").toString();
    }

    public static Map<String, Object> checkIfEntityExistsInDB(String sql) {
        Map<String, Object> result = null;
        QueryRunner runner = new QueryRunner();
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, sql, new MapHandler());
        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getMessage().contains("permission denied")) {
                String accessDenied = "Отказано в доступе к БД";
                Assert.fail(intBaseName == null ? accessDenied : accessDenied + " " + intBaseName);
            } else {
                Assert.fail(e.getMessage());
            }
        }
        Allure.addAttachment(ATTACHMENT_NAME_REQUEST, sql);
        LOG.info(sql);
        return result;
    }

    public static List<Map<String, Object>> getScheduleRequestByOuterId(String employeeOuterId, String positionOuterId, LocalDate date) {
        String sql = "SELECT sr.id FROM schedule_request sr\n" +
                "JOIN employee e ON e.id = sr.employee_id\n" +
                "JOIN position p ON p.id = sr.position_id \n" +
                "WHERE e.outerid = '" + employeeOuterId + "'\n" +
                "AND p.outerid = '" + positionOuterId + "'\n" +
                "AND DATE(startdatetime) = '" + date + "'";
        List<Map<String, Object>> result = null;
        QueryRunner runner = new QueryRunner();
        try {
            Connection conn = WFMDBConnectionProvider.getInstance().getConnection();
            result = runner.query(conn, sql, new MapListHandler());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String log = sql.replace("?", "{}");
        LOG.info(log, employeeOuterId);
        Allure.addAttachment(ATTACHMENT_NAME_RESPONSE, String.valueOf(result));
        return result;
    }

    public static int deleteDuplicatePermissionIds(int roleId) {
        String query = "DELETE FROM permission_role " +
                "WHERE ctid NOT IN " +
                "(SELECT MIN(ctid) FROM permission_role " +
                "GROUP BY permission_id, custom_role_id) " +
                "and custom_role_id = ?";
        return genericExecutor(query, roleId, true);
    }

    public static int getPositionId(Position position, OrgUnit orgUnit, LocalDate startDate) {
        //на некоторых стендах приходит неверный id в api, приходится искать через бд
        String sql = "SELECT * FROM POSITION WHERE organizationunit_id = " + orgUnit.getId()
                + " AND positioncategory_id = " + position.getPositionCategoryId()
                + " AND  positiontype_id = " + position.getPositionTypeId()
                + " AND  startdate = " + "'" + startDate + "'"
                + " ORDER BY id DESC LIMIT 1";
        Map<String, Object> result = executeSqlQuery(sql);
        return Objects.isNull(result) ? position.getId() : Integer.parseInt(result.get("id").toString());
    }

    public static int getEmployeePositionId(int positionId) {
        String sql = "SELECT * FROM employeeposition WHERE position_id = " + positionId;
        Map<String, Object> result = executeSqlQuery(sql);
        return Integer.parseInt(result.get("id").toString());
    }

    public static Integer getFtePositionGroupId(Position position) {
        String sql = String.format("SELECT ftepositiongroup_id FROM position p WHERE p.id = %d and p.outerid = '%s'",
                position.getId(), position.getOuterId());
        Object ftePositionGroupId = executeSqlQuery(sql).get("ftepositiongroup_id");
        return Objects.isNull(ftePositionGroupId) ? null : Integer.parseInt(ftePositionGroupId.toString());
    }

    public static String getSourceInSysListTimesheetEditPermission(int omId, LocalDate startDate, LocalDate endDate) {
        String sql = String.format("SELECT * FROM  sys_list_timesheet_edit_permission s " +
                        "WHERE s.org_unit_id = %d and s.date_from = '%s' and s.date_to = '%s' order by s.id desc limit 1",
                omId, startDate, endDate);
        Object ftePositionGroupId = executeSqlQuery(sql).get("source");
        return Objects.isNull(ftePositionGroupId) ? null : ftePositionGroupId.toString();
    }
}
