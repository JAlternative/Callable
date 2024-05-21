package wfm.repository;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import utils.Projects;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import wfm.PresetClass;
import wfm.components.schedule.GraphStatus;
import wfm.components.schedule.ShiftTimePosition;
import wfm.components.systemlists.LimitType;
import wfm.models.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.*;
import static utils.Params.EMBEDDED;
import static utils.Params.ID;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.*;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class EmployeePositionRepository {

    private static final Random RANDOM = new Random();
    private static final Logger LOG = LoggerFactory.getLogger(EmployeePositionRepository.class);

    private EmployeePositionRepository() {
    }

    /**
     * Возвращает позицию по айди
     */
    public static EmployeePosition getEmployeePositionById(int id) {
        String urlEnding = makePath(EMPLOYEE_POSITIONS, id);
        JSONObject position = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        EmployeePosition ep = getClassObjectFromJson(EmployeePosition.class, position);
        PresetClass.setDateIntervalForPositionIfNeeded(ep);
        return ep;
    }

    /**
     * Из списка сотрудников, у которых есть позиции на текущий месяц, убираются все сотрудники, у которых есть
     * длинные шеделуреквесты и/или статусы, а также сотрудники с несколькими ставками. Затем из этого списка выбирается рандомный сотрудник.
     *
     * @param omId      - id текущего ОМ
     * @param jobTitle  - название должности
     * @param inclChief - включать/не включать в выборку руководителя подразделения
     * @return - объект wfm.models.EmployeePosition
     */
    public static EmployeePosition getRandomEmployeeWithCheckByApi(int omId, JobTitle jobTitle, boolean inclChief) {
        List<EmployeePosition> employeePositions = getAllEmployeesWithCheckByApi(omId, jobTitle, inclChief);
        Assert.assertFalse(employeePositions.isEmpty(), NO_TEST_DATA + "В подразделении нет подходящих сотрудников");
        return getRandomEmployeeFromList(employeePositions);
    }

    public static EmployeePosition getRandomEmployeeFromList(List<EmployeePosition> employeePositions) {
        int size = employeePositions.size();
        EmployeePosition ep;
        if (size == 0) {
            throw new AssertionError(NO_TEST_DATA + "В подразделении нет подходящих сотрудников");
        }
        if (size <= 5) {
            ep = getRandomFromList(employeePositions);
        } else {
            ep = employeePositions.get(RANDOM.nextInt(size / 2));
        }
        PresetClass.setDateIntervalForPositionIfNeeded(ep);
        LOG.info("Выбран сотрудник с именем: {} и ID № {}", ep.getEmployee().getShortName(), ep.getId());
        return ep;
    }

    public static List<EmployeePosition> getAllEmployeesWithCheckByApi(int omId, JobTitle jobTitle, boolean inclChief) {
        List<EmployeePosition> employeePositions = getEmployeesWithPosIds(omId, jobTitle, true);
        //TODO добавить проверку через ставки на почте, на данный момент исключены все сотрудники с несколькими ставками
        List<Employee> employees = employeePositions.stream()
                .map(EmployeePosition::getEmployee)
                .collect(Collectors.groupingBy(Employee::getId))
                .values().stream().filter(employeeList -> employeeList.size() == 1)
                .map(employeeList -> employeeList.get(0))
                .collect(Collectors.toList());
        List<Employee> broken = EmployeeRepository.getEmployeesWithLongRequests(omId);
        if (!broken.isEmpty()) {
            List<ScheduleRequest> requests = ScheduleRequestRepository.getLongScheduleRequests(omId);
            for (ScheduleRequest request : requests) {
                PresetClass.deleteRequest(request);
            }
            broken = EmployeeRepository.getEmployeesWithLongRequests(omId);
        }
        List<EmployeeStatus> statuses = EmployeeRepository.getEmployeesStatuses(omId);
        for (EmployeeStatus status : statuses) {
            try {
                if (status.getId() != null) {
                    PresetClass.deleteEmployeeStatus(status);
                }
            } catch (AssertionError e) {
                LOG.info("У сотрудника не удалось удалить статус");
            }
        }
        List<Employee> hasStatuses = EmployeeRepository.getEmployeesWithCurrentStatuses(omId);
        broken.addAll(hasStatuses);
        for (Employee emp : employees) {
            if (OrgUnitRepository.getOrgUnitList(emp, new DateInterval()).size() > 1) {
                broken.add(emp);
            }
        }
        for (Employee employee : broken) {
            employeePositions.removeIf(employeePosition -> employeePosition.getEmployee().equals(employee));
        }
        if (!inclChief) {
            Position chief = PositionRepository.getChief(omId);
            if (chief != null) {
                employeePositions
                        .stream()
                        .filter(ep -> ep.getPosition().getId() == chief.getId())
                        .map(EmployeePosition::getEmployee)
                        .findFirst()
                        .ifPresent(chiefEmployee -> employeePositions
                                .removeIf(ep -> ep.getEmployee().getId() == chiefEmployee.getId()));
            }
        }
        return employeePositions.stream()
                .filter(employeePosition -> employees.contains(employeePosition.getEmployee()))
                .collect(Collectors.toList());
    }

    public static List<EmployeePosition> getSeveralRandomEmployeesWithCheckByApi(int omId, int howMany, boolean inclChief) {
        List<EmployeePosition> employeePositions = getEmployeesWithPosIds(omId, null, inclChief);
        List<Employee> employees = employeePositions.stream()
                .map(EmployeePosition::getEmployee)
                .collect(Collectors.groupingBy(Employee::getId))
                .values().stream().filter(employeeList -> employeeList.size() == 1)
                .map(employeeList -> employeeList.get(0))
                .collect(Collectors.toList());
        List<Employee> broken = EmployeeRepository.getEmployeesWithLongRequests(omId);
        List<Employee> hasStatuses = EmployeeRepository.getEmployeesWithCurrentStatuses(omId);
        broken.addAll(hasStatuses);
        for (Employee employee : broken) {
            employeePositions.removeIf(employeePosition -> employeePosition.getEmployee().equals(employee));
        }
        employeePositions = employeePositions.stream()
                .filter(employeePosition -> employees.contains(employeePosition.getEmployee()))
                .collect(Collectors.toList());

        int size = employeePositions.size();
        EmployeePosition employee;
        if (size < howMany) {
            throw new AssertionError(NO_TEST_DATA + "В оргюните нет подходящих сотрудников");
        }
        List<EmployeePosition> selectedPositions = new ArrayList<>();
        while (selectedPositions.size() < howMany) {
            employee = getRandomFromList(employeePositions);
            employeePositions.remove(employee);
            selectedPositions.add(employee);
        }
        LOG.info("Выбраны сотрудники: {}", selectedPositions);
        return selectedPositions;
    }

    /**
     * Берет позицию сотрудника
     *
     * @param name         - имя сотрудника
     * @param dateInterval - временной интервал для поиска сотрудника
     * @param orgId        - айди оргюнита в котором будем искать сотрудника
     */
    public static EmployeePosition getEmployeePosition(String name, DateInterval dateInterval, int orgId) {
        return getEmployeePositions(dateInterval, orgId).stream()
                .filter(employeePosition -> name.contains(employeePosition.getEmployee().getLastName())
                        && name.contains(employeePosition.getEmployee().getFirstName()))
                .findAny()
                .orElse(null);
    }

    /**
     * Возвращает список позиций сотрудника в подразделении на дату
     *
     * @param name  - имя сотрудника
     * @param date  - дата, на которую позиция должна быть действующей
     * @param orgId - id оргюнита, в котором ищем позицию сотрудника
     */
    public static List<EmployeePosition> getEmployeePositionsOnDate(String name, LocalDate date, int orgId) {
        return getEmployeePositions(new DateInterval(date), orgId).stream()
                .filter(employeePosition -> name.contains(employeePosition.getEmployee().getLastName())
                        && name.contains(employeePosition.getEmployee().getFirstName()))
                .collect(Collectors.toList());
    }

    public static EmployeePosition getEmployeePosition(String name, List<EmployeePosition> epList) {
        return epList.stream()
                .filter(employeePosition -> name.contains(employeePosition.getEmployee().getLastName())
                        && name.contains(employeePosition.getEmployee().getFirstName()))
                .findAny()
                .orElse(null);
    }

    public static List<EmployeePosition> getEmployeePositions(DateInterval dateInterval, int orgId) {
        String urlEnding = makePath(ORGANIZATION_UNITS, orgId, EMPLOYEE_POSITIONS);
        LocalDate endDate = dateInterval.endDate;
        if (dateInterval.endDate == null) {
            endDate = LocalDateTools.now().withYear(LocalDateTools.now().getYear() + 10);
        }
        List<NameValuePair> pairs = Pairs.newBuilder().from(dateInterval.startDate).to(endDate).includeChief(true).build();
        JSONObject temp = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, pairs);
        return getListFromJsonObject(temp, EmployeePosition.class);
    }

    /**
     * Ищет позицию сотрудника за текующий месяц, от первого до последнего числа
     *
     * @param name - имя сотрудника
     * @param omId - айди оргюнита в котором будем искать сотрудника
     */
    public static EmployeePosition getEmployeePosition(String name, int omId) {
        return getEmployeePosition(name, new DateInterval(), omId);
    }

    /**
     * Возвращает массив employeePositions без сотрудников, уволенных по состоянию на заданное число с руководителем или без него
     */
    public static List<EmployeePosition> getEmployeePositionsWithoutFiredEmployees(int currentOM, LocalDate date, boolean includeChief) {
        String urlEnding = makePath(ORGANIZATION_UNITS, currentOM, EMPLOYEE_POSITIONS);
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .includeChief(includeChief)
                .from(date)
                .size(1000)
                .to(date)
                .build();
        JSONObject somePositions = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, nameValuePairs);
        return getListFromJsonObject(somePositions, EmployeePosition.class);
    }

    /**
     * Выбираем employeePositions за последнее число текущего месяца, т.к. иначе могут попасть сотрудники
     * у которых есть серые ячейки в расписании
     *
     * @param omId      - id текущего ОМ
     * @param jobTitle  - название должности
     * @param inclChief - включать/не включать в выборку руководителя подразделения
     * @return список индентификаторов сотрудников в виде "фамилия имя"
     */
    public static List<EmployeePosition> getEmployeesWithPosIds(int omId, JobTitle jobTitle, boolean inclChief) {
        List<EmployeePosition> employeePositionList = getEmployeePositionsWithoutFiredEmployees(omId, LocalDateTools.getLastDate(), inclChief).stream()
                .filter(ep -> ep.getDateInterval().startDate != null
                        && ep.getDateInterval().startDate.isBefore(LocalDateTools.getFirstDate().minusDays(1))
                        && ep.getPosition().getDateInterval() != null)
                .collect(Collectors.toList());
        if (jobTitle == null) {
            return employeePositionList;
        }
        return employeePositionList.stream()
                .filter(employeePosition -> employeePosition.getPosition().getName().equals(jobTitle.getFullName()))
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список существующих актуальных(работающих) позиций сотрудников по списку позиций
     *
     * @param positions - список позиций
     */
    public static List<EmployeePosition> getEmployeePositions(List<Position> positions) {
        List<EmployeePosition> employeePositions = new ArrayList<>();
        for (Position position : positions) {
            JSONObject object = getJsonFromUri(Projects.WFM, URI.create(position.getLink(REL_EMPLOYEE_POSITIONS).split("\\{")[0]));
            employeePositions.addAll(getListFromJsonObject(object, EmployeePosition.class));
        }
        LocalDate localDate = LocalDate.now().plusDays(1);
        return employeePositions.stream()
                .filter(employeePosition -> employeePosition.getDateInterval().includesDate(localDate))
                .collect(Collectors.toList());
    }

    /**
     * Из списка сотрудников, у которых есть позиции на текущий месяц, убираются все сотрудники, у которых нет
     * длинных wfm.models.ScheduleRequest и те, у кого есть статус (например, декрет). Затем из этого списка выбирается два разных случайных сотрудника
     * на расстоянии не более 3-х строчек друг от друга
     *
     * @param omId id текущего ОМ
     * @return объект org.apache.commons.lang3.tuple.ImmutablePair содержащий два wfm.models.EmployeePosition,
     * первая позиция сотрудника вызывается через left.
     */
    public static ImmutablePair<EmployeePosition, EmployeePosition> getTwoRandomEmployeeWithCheckByApi(int omId) {
        List<EmployeePosition> employeePositions = getEmployeesWithPosIds(omId, null, false);
        List<Employee> broken = EmployeeRepository.getEmployeesWithLongRequests(omId);
        List<Employee> hasStatuses = EmployeeRepository.getEmployeesWithCurrentStatuses(omId);
        broken.addAll(hasStatuses);
        for (EmployeePosition ep : employeePositions) {
            if (OrgUnitRepository.getOrgUnitList(ep.getEmployee(), new DateInterval()).size() > 1) {
                broken.add(ep.getEmployee());
            }
        }
        for (Employee employee : broken) {
            employeePositions.removeIf(employeePosition -> employeePosition.getEmployee().equals(employee));
        }
        List<String> allName = new ArrayList<>();
        employeePositions.forEach(employeePosition -> allName.add(employeePosition.getEmployee().getShortName()));
        List<String> temp = allName.stream().distinct().collect(Collectors.toList());
        HashMap<Integer, String> names = new HashMap<>();
        String name;
        String name2;
        for (int i = 0; i < temp.size(); i++) {
            names.put(i, temp.get(i));
        }
        Assert.assertTrue(temp.size() >= 2, "Невозможно выбрать двух сотрудников так " +
                "как в оргюните остался один подходящий сотрудник");
        if (temp.size() <= 5) {
            List<Integer> keysAsArray = new ArrayList<>(names.keySet());
            int randomKey = keysAsArray.get(RANDOM.nextInt(keysAsArray.size()));
            name = names.get(randomKey);
            keysAsArray.remove(randomKey);
            randomKey = keysAsArray.get(RANDOM.nextInt(keysAsArray.size()));
            name2 = names.get(randomKey);
            while (name.equals(name2)) {
                keysAsArray.remove(randomKey);
                randomKey = keysAsArray.get(RANDOM.nextInt(keysAsArray.size()));
                name2 = names.get(randomKey);
            }
        } else {
            int random = RANDOM.nextInt(names.size());
            name = names.get(random);
            names.keySet().remove(random);
            int anotherRandom = names.keySet().stream()
                    .filter(integer -> Math.abs(integer - random) < 4)
                    .findAny()
                    .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "Второй подходящий сотрудник не найден"));
            name2 = names.get(anotherRandom);
        }
        EmployeePosition first = employeePositions.stream().filter(employeePosition -> employeePosition.getEmployee().getShortName()
                .equals(name)).findFirst().orElseThrow(() -> new AssertionError(NO_TEST_DATA + "Не был найден сотрудник"));
        String finalName = name2;
        EmployeePosition second = employeePositions.stream().filter(employeePosition -> employeePosition.getEmployee().getShortName()
                .equals(finalName)).findFirst().orElseThrow(() -> new AssertionError(NO_TEST_DATA + "Не был найден сотрудник"));
        LOG.info("Выбраны сотрудники {}, {}", name, name2);
        Allure.addAttachment("Сотрудник", "Выбраны сотрудники " + name + ", " + name2);
        return new ImmutablePair<>(first, second);
    }

    /**
     * Берет массив employeePositions в апи с учетом размера по запросу
     *
     * @return массив EmployeePosition с данными сотрудников, оргюнитов и позиций
     */
    public static List<EmployeePosition> getEmployeePositions(int omNumber) {
        String urlPath = makePath(ORGANIZATION_UNITS, omNumber, EMPLOYEE_POSITIONS);
        LocalDate localDate = LocalDate.now();
        LocalDate from = localDate.withDayOfMonth(1);
        LocalDate to = localDate.withDayOfMonth(localDate.lengthOfMonth());
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .page(0)
                .size(1000)
                .includeChief(true)
                .from(from)
                .to(to)
                .build();
        JSONObject somePositions = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlPath, nameValuePairs);
        return getListFromJsonObject(somePositions, EmployeePosition.class);
    }

    /**
     * Берет актуальный (на текущуюу дату) список сотрудников подразделенеия вместе с руководителем подразделения
     *
     * @param currentOM - айди оргюнита
     */
    public static List<EmployeePosition> getActualEmployeePositionsWithChief(int currentOM) {
        String urlEnding = makePath(ORGANIZATION_UNITS, currentOM, EMPLOYEE_POSITIONS);
        LocalDate localDate = LocalDate.now();
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .from(localDate)
                .size(1000)
                .to(localDate)
                .includeChief(true)
                .build();
        JSONObject somePositions = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, nameValuePairs);
        return getListFromJsonObject(somePositions, EmployeePosition.class);
    }

    /**
     * Находит должность для сотрудника по его имени
     *
     * @param employeeName - имя сотрудника в тройном формате
     * @return объект wfm.models.EmployeePosition
     */
    public static EmployeePosition getEmployeePosition(OrgUnit orgUnit, String employeeName) {
        List<EmployeePosition> employeePositions = getEmployeePositionsWithoutFiredEmployees(orgUnit.getId(), LocalDateTools.getLastDate(), false);
        return employeePositions.stream()
                .filter(ep -> ep.getEmployee().getFullName().equals(employeeName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "Сотрудник не был найден по айди"));
    }

    /**
     * Выбирает рандомного сотрудника из списка
     *
     * @return объект wfm.models.EmployeePosition
     */
    public static EmployeePosition getRandomEmployeePosition(OrgUnit orgUnit) {
        List<EmployeePosition> employeePositions = getEmployeePositionsWithoutFiredEmployees(orgUnit.getId(), LocalDateTools.getLastDate(), false);
        return employeePositions.get(RANDOM.nextInt(employeePositions.size()));
    }

    /**
     * Выбирает эмплоепозишен у которого есть юзер и есть должность
     * FIXME этот метод НЕ РАБОТАЕТ: у эндпоинтов, судя по всему, нет параметров, с которыми мы обращаемся, и фильтрации не происходит.
     */
    public static EmployeePosition getRandomEmployeeWorkingWithUser() {
        List<Employee> employeeListWithAcc = EmployeeRepository
                .getEmployees(getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES),
                                             EmployeeRepository.getEmployeeAccountPairsList().build()));
        LocalDate localDate = LocalDate.now();
        List<NameValuePair> nameValuePairs = Pairs.newBuilder().size(1000).from(localDate).to(localDate).build();
        JSONObject somePositions = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, EMPLOYEE_POSITIONS, nameValuePairs);
        List<EmployeePosition> employeePositions = getListFromJsonObject(somePositions, EmployeePosition.class);
        Collections.shuffle(employeePositions);
        EmployeePosition employeePosition = employeePositions.stream()
                .filter(ep -> employeeListWithAcc.contains(ep.getEmployee()))
                .findFirst()
                .orElse(null);
        if (employeePosition == null) {
            employeePosition = getRandomFromList(employeePositions);
            PresetClass.addUser(employeePosition.getEmployee());
            employeePosition.refreshEmployeePosition();
        }

        return employeePosition;
    }

    public static EmployeePosition getEmployeeByScheduleRequest(ScheduleRequest request, OrgUnit unit) {
        List<EmployeePosition> allEmployeePositions = getEmployeePositionsWithoutFiredEmployees(unit.getId(), LocalDateTools.getLastDate(), true);
        List<EmployeePosition> certainEmployeePositions = allEmployeePositions.stream()
                .filter(ep1 -> ep1.getEmployee().getId() == request.getEmployee().getId())
                .collect(Collectors.toList());
        List<EmployeePosition> finalEmployeePositions = new ArrayList<>();
        DateTimeInterval epDateConvert = null;
        for (EmployeePosition ep : certainEmployeePositions) {
            try {
                epDateConvert = new DateTimeInterval(
                        ep.getDateInterval().startDate.atStartOfDay(),
                        ep.getDateInterval().endDate.atStartOfDay());
            } catch (NullPointerException e) {
                epDateConvert = new DateTimeInterval(
                        ep.getDateInterval().startDate.atStartOfDay(),
                        null);
            } finally {
                DateTimeInterval requestInterval = request.getDateTimeInterval();
                if (epDateConvert.includesDate(requestInterval.getStartDateTime())
                        || epDateConvert.includesDate(requestInterval.getEndDateTime())) {
                    finalEmployeePositions.add(ep);
                }
            }
        }
        Optional<EmployeePosition> optionalEmployeePosition = finalEmployeePositions.stream().findAny();
        return optionalEmployeePosition.orElse(null);
    }

    /**
     * Возвращает всех сотрудников в подразделении с заданной функциональной ролью
     */
    public static List<EmployeePosition> getAllEmployeesByPositionGroup(int omId, PositionGroup posGroup) {
        return getActualEmployeePositionsWithChief(omId).stream()
                .filter(ep -> ep.getPosition().getPositionGroupId() == posGroup.getId())
                .collect(Collectors.toList());
    }

    public static EmployeePosition getRandomEmployeeWithPositionGroup(int omId) {
        List<EmployeePosition> employees = getAllEmployeesWithCheckByApi(omId, null, true).stream()
                .filter(ep -> ep.getPosition().getPositionGroupId() != 0)
                .collect(Collectors.toList());
        return getRandomFromList(employees);
    }

    public static EmployeePosition getEmployeePositionForLimits(int omId, LimitType limitType) {
        if (limitType.equals(LimitType.GENERAL)) {
            List<EmployeePosition> employeePositions = getAllEmployeesWithCheckByApi(omId, null, true);
            List<Integer> posIds = LimitsRepository.getPosGroupIdsByLimitType(LimitType.ADD_WORK);
            List<EmployeePosition> epsWithAddWorkLimit = employeePositions.stream().filter(e -> posIds.contains(e.getPosition().getPositionGroupId())).collect(Collectors.toList());
            employeePositions.removeAll(epsWithAddWorkLimit);
            return getRandomEmployeeFromList(employeePositions);
        } else {
            return getRandomEmployeeWithCheckByApi(omId, null, true);
        }
    }

    /**
     * Возвращает список назначений для сотрудника
     */
    public static List<EmployeePosition> getEmployeePositionsFromEmployee(Employee employee) {
        DateInterval interval = new DateInterval(LocalDate.now());
        return getEmployeePositionsFromEmployee(employee, interval);
    }

    public static List<EmployeePosition> getEmployeePositionsFromEmployee(Employee employee, DateInterval interval) {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .from(interval.getStartDate())
                .to(interval.getEndDate())
                .build();
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, employee.getId(), EMPLOYEE_POSITIONS), pairs);
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        return getListFromJsonObject(json, EmployeePosition.class);
    }

    /**
     * Возвращает первое активное назначение сотрудника
     */
    public static EmployeePosition getFirstActiveEmployeePositionFromEmployee(Employee employee) {
        return getEmployeePositionsFromEmployee(employee)
                .stream()
                .filter(ep -> ep.getDateInterval().getEndDate() == null || !ep.getDateInterval().getEndDate().isBefore(LocalDate.now()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Возвращает случайное назначение для сотрудника с табельным номером
     */
    public static EmployeePosition getEmployeePositionWithCardNumber(int omId, JobTitle jobTitle, boolean inclChief) {
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, jobTitle, inclChief);
        if (ep.getCardNumber() == null || ep.getCardNumber().equals("")) {
            PresetClass.changeCardNumber(ep, RandomStringUtils.randomNumeric(6));
        }
        return ep;
    }

    /**
     * Вернуть из родительского оргюнита смену сотрудника без лишних типов, отпусков, мобильных смен и тд,
     * если такой не находится в подразделении, то создаётся новая плановая смена
     *
     * @return сотрудник подходящий для назначения на мобильную смену и его смена из собственного подразделения
     */

    public static ImmutablePair<EmployeePosition, Shift> getEmployeePositionAndShift(OrgUnit unit) {
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositions(unit.getParentId())
                .stream()
                .filter(e -> !e.isHidden())
                .collect(Collectors.toList());
        for (EmployeePosition employeePosition : employeePositions) {
            List<Shift> shifts = PresetClass.filterShiftsOnTypes(
                    ShiftRepository.getEmployeeSelfShifts(employeePosition.getEmployee().getId(), ShiftTimePosition.FUTURE.getShiftsDateInterval()),
                    unit, employeePosition, ShiftTimePosition.FUTURE);

            if (!shifts.isEmpty()) {
                return new ImmutablePair<>(employeePosition, shifts.get(new Random().nextInt(shifts.size())));
            } else {
                LocalDate localDate = ShiftRepository.emptySells(employeePosition).stream().findAny().orElse(null);
                if (Objects.isNull(localDate)) {
                    continue;
                }
                Shift newShift = PresetClass.presetForMakeShiftDateTime(employeePosition, localDate.atTime(14, 0, 0),
                                                                        localDate.atTime(22, 0, 0), ShiftTimePosition.FUTURE);
                PresetClass.publishGraphPreset(GraphStatus.PUBLISH, OrgUnitRepository.getOrgUnit(unit.getParentId()));
                return new ImmutablePair<>(employeePosition, newShift);
            }
        }
        throw new AssertionError("В подразделении №: " + unit.getParentId() + " нет плановых смен и нельзя создать новые");
    }

    /**
     * Выбирает рандомного сотрудника из работающих на дату с проверкой в api
     *
     * @param omId      id орг юнита
     * @param inclChief включать ли в выборку начальника
     * @param date      дата, на которую должен работать сотрудник
     * @return объект wfm.models.EmployeePosition
     */
    public static EmployeePosition getRandomEmployeeWithCheckByApiNotFiredOnDate(int omId, boolean inclChief, LocalDate date) {
        List<EmployeePosition> epsNotFired = getEmployeePositionsWithoutFiredEmployees(omId, date, inclChief);
        List<EmployeePosition> eps = getAllEmployeesWithCheckByApi(omId, null, inclChief);
        return getRandomEmployeeFromList(eps.stream().filter(epsNotFired::contains).collect(Collectors.toList()));
    }

    @Step("Получить всех доступных сотрудников, которых можно назначить на свободную смену")
    public static List<EmployeePosition> getAvailableEmployeesForShiftFreeAssignment(int shiftId) {
        String urlEnding = makePath(EMPLOYEE_POSITIONS, PLAIN, SHIFT, shiftId);
        JSONObject somePositions = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        if (somePositions.isEmpty()){
            String message = "Не найдены доступные сотрудники для назначения на свободную смену.";
            Allure.addAttachment("Не найдены доступные сотрудники для назначения на свободную смену.",
                    "Нет данных о сотрудниках для назначения на свободную смену по ссылке: " + urlEnding);
            LOG.error("{} {}", message, urlEnding);
            return Collections.emptyList();
        }
        JSONArray jsonArray = somePositions.getJSONObject(EMBEDDED).getJSONArray("plainEmployeePositionResList");
        List<EmployeePosition> employeePositions = new ArrayList<>();
        for (int a = 0; a < jsonArray.length(); a++) {
            employeePositions.add(EmployeePositionRepository.getEmployeePositionById(jsonArray.getJSONObject(a).getInt(ID)));
        }
        return employeePositions;
    }

    public static List<EmployeePosition> getAllEmployeePositionsFromEmployee(Employee employee) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, employee.getId(), EMPLOYEE_POSITIONS));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        return getListFromJsonObject(json, EmployeePosition.class);
    }
}
