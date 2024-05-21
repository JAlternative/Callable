package wfm.repository;

import io.qameta.allure.Allure;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Links;
import utils.Projects;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import wfm.PresetClass;
import wfm.models.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.*;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class EmployeeRepository {
    private static final String VIRTUAL_EMPLOYEE = "Виртуальный сотрудник";
    private static final String SELECTION_ALLURE_ATTACHMENT_TITLE = "Выбор сотрудника";
    private static final String SELECTION_LOGGER = "Выбран сотрудник: {}";
    private static final String SELECTION_ALLURE_CONTENTS = "Был выбран сотрудник ";
    private static final Random RANDOM = new Random();
    private static final Logger LOG = LoggerFactory.getLogger(EmployeeRepository.class);

    private EmployeeRepository() {
    }

    /**
     * Возвращает сотрудников с/без должностей из указанного ОМ
     *
     * @param omId id текущего ОМ
     * @return список сотрудников
     */
    public static List<Employee> getEmployeesFromOM(int omId) {
        String urlEnding = makePath(ORGANIZATION_UNITS, omId, EMPLOYEES);
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .includeDate(LocalDateTools.now())
                .size(1000)
                .build();
        JSONObject embedded = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, nameValuePairs);
        List<Employee> employees = getListFromJsonObject(embedded, Employee.class);
        urlEnding = makePath(ORGANIZATION_UNITS, omId, UNATTACHED_EMPLOYEES);
        nameValuePairs = Pairs.newBuilder()
                .includeDate(LocalDateTools.now())
                .size(1000)
                .build();
        embedded = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, nameValuePairs);
        List<Employee> unattachedEmployees = getListFromJsonObject(embedded, Employee.class);
        employees.addAll(unattachedEmployees);
        return employees;
    }

    /**
     * Производится поиск по шедулереквестам сотрудников на текущий месяц,
     * затем выбираются все сотрудники у которых есть реквесты больше определённого значения
     *
     * @param omId id текущего ОМ
     * @return список сотрудников с длинными шеделуреквестами
     */
    public static List<Employee> getEmployeesWithLongRequests(int omId) {
        try {
            List<ScheduleRequest> scheduleRequests = ScheduleRequestRepository.getScheduleRequests(omId, new DateInterval());
            return scheduleRequests.stream().filter(request -> request.getDateTimeInterval().getLengthInHours() > 40)
                    .map(ScheduleRequest::getEmployee).collect(Collectors.toList());
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Преобразуем джисон-объект с сотрудниками в список с сотрудниками, фильтруем сотрудников так,
     * чтобы в список попадали только работающие сотрудники
     *
     * @param jsonObject джисон-объект с сотрудниками
     */
    public static List<Employee> getEmployees(JSONObject jsonObject) {
        LocalDate dateNow = LocalDateTools.now();
        List<Employee> employeeList = getListFromJsonObject(jsonObject, Employee.class);
        return employeeList.stream().filter(employee ->
                                                    (employee.getEndWorkDate() == null || employee.getEndWorkDate().isAfter(dateNow))
                                                            && (employee.getStartWorkDate() != null && employee.getStartWorkDate().isBefore(dateNow)))
                .collect(Collectors.toList());
    }

    /**
     * Берем случайного сотрудника с аккаунтом, с учетом сотрудников исключений, так же можем очистить от ролей если надо
     *
     * @param clearFromRoles очищать ли найденного сотрудника от ролей
     * @param except         сотрудники исключения
     */
    public static Employee getRandomEmployeeWithAccount(boolean clearFromRoles, Employee... except) {
        List<Employee> employeeList = getEmployees(getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES),
                                                                  getEmployeeAccountPairsList().build()));
        Employee randomEmployee = getRandomFromList(employeeList);
        List<Employee> exceptEmployees = Arrays.asList(except);
        while (exceptEmployees.contains(randomEmployee)) {
            randomEmployee = getRandomFromList(employeeList);
            employeeList.remove(randomEmployee);
        }
        User user = new User(getJsonFromUri(Projects.WFM, URI.create(randomEmployee.getLink(REL_ACCOUNT))));
        Set<Integer> roles = user.getRolesIds();
        if (!roles.isEmpty() && clearFromRoles) {
            PresetClass.clearUserFromRoles(roles, user);
        }
        return randomEmployee;
    }

    /**
     * Возвращает список сотрудников, у которых есть указанная роль
     *
     * @param roleId айди роли
     */
    public static List<Employee> getEmployeesByRoleId(int roleId) {
        List<NameValuePair> nameValuePairs = getEmployeeAccountPairsList().roleIds(roleId).build();
        return getEmployees(getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES), nameValuePairs));
    }

    /**
     * Берет случайного сотрудника с аккаунтом и нужной ролью, можем определять, нужен ли нам сотрудник без заместителей
     *
     * @param roleId     айди необходимой роли
     * @param noDeputies очищать ли нам сотрудника от заместителей
     */
    public static Employee getRandomEmployeeWithAccount(int roleId, boolean noDeputies) {
        List<Employee> employeeList = getEmployeesByRoleId(roleId);
        Employee randomEmployee;
        if (!employeeList.isEmpty()) {
            if (noDeputies) {
                return employeeList.stream().filter(employee -> employee.getUser().getUserDeputies().isEmpty())
                        .collect(randomItem());
            }
            randomEmployee = getRandomFromList(employeeList);
            User user = randomEmployee.getUser();
            Set<Integer> roles = user.getRolesIds();
            if (roles.size() > 1) {
                roles.remove(roleId);
                PresetClass.clearUserFromRoles(roles, user);
            }
            return randomEmployee;
        }
        return null;
    }

    /**
     * Выбирает случайного сотрудника имя которого не повторяется в списке
     *
     * @param employees - список сотрудников
     */
    public static Employee getRandomNonRepeatEmployee(List<Employee> employees) {
        List<String> names = employees.stream().map(Employee::getFullName).distinct().collect(Collectors.toList());
        String nonRepeatName = getRandomFromList(names);
        return employees.stream().filter(employee -> employee.getFullName()
                        .equals(nonRepeatName)).findFirst()
                .orElseThrow(AssertionError::new);
    }

    /**
     * список пар значений для получения сотрудников с аккаунтов
     */
    public static Pairs.Builder getEmployeeAccountPairsList() {
        return Pairs.newBuilder()
                .withAccount(true)
                .includeDate(LocalDateTools.now())
                .size(1000);
    }

    /**
     * Выбирает случайного сотрудника из не виртуальных сотрудников без даты окончания работы или с датой окончания
     * большей чем дата начала работы
     */
    public static Employee chooseRandomEmployee(OrgUnit orgUnit) {
        LocalDate dateCurrent = LocalDateTools.now();
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(orgUnit.getId(), LocalDateTools.getLastDate(), false);
        //в списке нет человека, который сейчас управляющий руководитель, надо отфильтровать виртуальных сотрудников
        List<EmployeePosition> checkedEmployee = new ArrayList<>();
        for (EmployeePosition employeePosition : employeePositions) {
            LocalDate endDate = employeePosition.getDateInterval().endDate;
            //если дата null, значит сотрудник не уволен, если дана не null, значит дата должна быть позже текущей
            boolean dateNowAfterDateCurrent;
            if (endDate == null || !employeePosition.getPosition().getName().equals(VIRTUAL_EMPLOYEE)) {
                checkedEmployee.add(employeePosition);
                continue;
            }
            dateNowAfterDateCurrent = endDate.isAfter(dateCurrent);
            if (dateNowAfterDateCurrent || !employeePosition.getPosition().getName().equals(VIRTUAL_EMPLOYEE)) {
                checkedEmployee.add(employeePosition);
            }
        }
        Employee employee = getRandomFromList(checkedEmployee).getEmployee();
        LOG.info(SELECTION_LOGGER, employee.getFullName());
        return employee;
    }

    /**
     * Выбирает случайного сотрудника из не виртуальных и не hidden сотрудников (без тега hidden = true в json)
     *
     * @return ключ айди, значение тройное имя сотрудника
     */
    public static Employee chooseRandomEmployeeNotHidden(OrgUnit orgUnit) {
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(orgUnit.getId(), LocalDateTools.getLastDate(), false);
        //в списке нет человека, который сейчас управляющий руководитель, надо отфильтровать виртуальных сотрудников
        List<EmployeePosition> checkedEmployee = new ArrayList<>();
        for (EmployeePosition employeePosition : employeePositions) {
            if (!employeePosition.isHidden() && !employeePosition.getPosition().getName().equals(VIRTUAL_EMPLOYEE)) {
                checkedEmployee.add(employeePosition);
            }
        }
        Employee employee = getRandomFromList(checkedEmployee).getEmployee();
        LOG.info(SELECTION_LOGGER, employee.getFullName());
        return employee;
    }

    /**
     * Находится сотрудник с указанным местом работы,
     * не имеющий навык "наставничество"
     **/
    public static Employee getEmployeeWithoutIntern() {
        List<Employee> temp = getEmployees().stream().filter(employees -> !employees.isNeedMentor()).collect(Collectors.toList());
        for (Employee employee : temp) {
            String urlEnding = makePath(EMPLOYEES, employee.getId(), Links.EMPLOYEE_POSITIONS);
            List<NameValuePair> pairs = Pairs.newBuilder().size(1000).from(LocalDate.now()).build();
            JSONObject empObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, pairs);
            if (empObject.optJSONObject(EMBEDDED) != null && empObject.getJSONObject(EMBEDDED).getJSONArray(REL_EMPLOYEE_POSITIONS).length() > 0) {
                Allure.addAttachment(SELECTION_ALLURE_ATTACHMENT_TITLE, SELECTION_ALLURE_CONTENTS
                        + employee.getFullName() + ", без добавленной ему стажерской программы");
                return employee;
            }
        }
        return new Employee(new JSONObject());
    }

    /**
     * Берет 500 сотрудников которые актуальны на текущий день
     */
    public static List<Employee> getEmployees() {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .fullName("")
                .size(500)
                .sort(LAST_NAME + "," + ASC)
                .includeDate(LocalDate.now())
                .withUnattached(true)
                .build();
        JSONObject empObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, EMPLOYEES, pairs);
        return getListFromJsonObject(empObject, Employee.class);
    }

    /**
     * Возвращает случайного сотрудника без даты окончания работы
     */
    public static Employee getEmployeeWithOutEndDate() {
        List<Employee> employeesWithOutEndDate = getEmployees().stream()
                .filter(employee -> employee.getEndWorkDate() == null)
                .collect(Collectors.toList());
        return getRandomFromList(employeesWithOutEndDate);
    }

    /**
     * Возвращает текущего заместителя для оргюнита
     */
    public static Employee getCurrentDeputy(OrgUnit orgUnit) {
        String url = makePath(ORGANIZATION_UNITS, orgUnit.getId(), DEPUTY_EMPLOYEE);
        JSONObject jsonObject;
        try {
            jsonObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, url);
        } catch (JSONException e) {
            Allure.addAttachment("Заместитель", "text/plain",
                                 "У оргюнита: " + orgUnit.getName() +
                                         " было отменено назначение сотрудника как заместителя");
            return null;
        }
        URI empHref = URI.create(jsonObject.getJSONObject(LINKS).getJSONObject(EMPLOYEE_JSON).getString(HREF));
        return new Employee(new JSONObject(setUrlAndInitiateForApi(empHref, Projects.WFM)));
    }

    /**
     * Возвращает случайного сотрудника без работы, работающего на данный момент
     *
     * @param orgUnitId             - айди оргюнита
     * @param currentEmployeesNames - список сотрудников на UI
     */
    public static Employee getRandomEmployeeWithoutWork(int orgUnitId, List<String> currentEmployeesNames) {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .withUnattached(true)
                .includeDate(LocalDate.now())
                .size(1000)
                .sort(LAST_NAME + "," + ASC).build();
        JSONObject empObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, EMPLOYEES, pairs);
        List<Employee> employees = getListFromJsonObject(empObject, Employee.class);
        List<Employee> finalEmployees = employees;
        employees = employees.stream()
                .filter(employee -> finalEmployees.stream()
                        .filter(employee1 -> employee1.getFullName().equals(employee.getFullName())).count() == 1)
                .collect(Collectors.toList());
        Employee random = getRandomFromList(employees);
        while (random.getOuterId() == null || random.getFullName().split(" ").length != 3
                || CommonRepository.approvePosition(random.getId(), orgUnitId)
                || currentEmployeesNames.contains(random.getFullName())
                || random.getEndWorkDate() != null) {
            employees.remove(random);
            random = getRandomFromList(employees);
        }
        return random;
    }

    /**
     * Выбирает сотрудника в соответствии с тем что есть в поиске сотрудников при выборе сотрудника для должности
     */
    public static Employee getRandomEmployeeWithoutWorkWithUiCheck(List<String> freeEmploeesFromUi) {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .withUnattached(true)
                .includeDate(LocalDate.now())
                .size(1000)
                .sort(LAST_NAME + "," + ASC).build();
        JSONObject empObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, EMPLOYEES, pairs);
        List<Employee> employees = getListFromJsonObject(empObject, Employee.class);
        List<Employee> finalEmployees = employees;
        employees = employees.stream()
                .filter(employee -> finalEmployees.stream()
                        .filter(employee1 -> employee1.getFullName().equals(employee.getFullName())).count() == 1)
                .collect(Collectors.toList());
        Employee random = getRandomFromList(employees);
        while (!freeEmploeesFromUi.contains(random.getFullName())) {
            employees.remove(random);
            random = getRandomFromList(employees);
        }
        return random;
    }

    /**
     * Метод собирает всех сотрудников в оргюните не имеющих позицию
     *
     * @param omId - номер оргюнита
     * @return - список сотруников
     */
    public static List<Employee> getUnattachedEmployee(int omId) {
        String urlEnding = makePath(ORGANIZATION_UNITS, omId, UNATTACHED_EMPLOYEES);
        List<NameValuePair> pair = Pairs.newBuilder().size(100000).build();
        JSONObject empObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, pair);
        if (!empObject.keySet().isEmpty()) {
            JSONArray empArray = empObject.getJSONObject(EMBEDDED).getJSONArray(EMPLOYEES_ARRAY);
            List<Employee> employees = new ArrayList<>();
            for (int i = 0; i < empArray.length(); i++) {
                JSONObject object1 = empArray.getJSONObject(i);
                employees.add(new Employee(object1));
            }
            return employees.stream().filter(employee -> employee.getOuterId() != null
                    && !CommonRepository.approvePosition(employee.getId(), omId)).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Возвращает случайного сотрудника с аккаунтом
     */
    public static List<Employee> getEmployeesWithAccount() {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .fullName("")
                .size(5000)
                .sort(LAST_NAME + "," + ASC)
                .includeDate(LocalDateTools.now())
                .withUnattached(true)
                .withAccount(true).build();
        JSONObject empObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, EMPLOYEES, pairs);
        return getListFromJsonObject(empObject, Employee.class);
    }

    /**
     * Возвращает случайного сотрудника с ролью или без
     */
    public static Employee getRandomEmployee(boolean withRole) {
        List<Employee> allEmployees = getEmployeesWithAccount();
        List<String> names = allEmployees.stream().map(Employee::getFullName).distinct().collect(Collectors.toList());
        while (!names.isEmpty()) {
            int random = RANDOM.nextInt(allEmployees.size());
            Employee tempEmp = allEmployees.get(random);
            boolean condition;
            if (withRole) {
                condition = !tempEmp.getUser().getRoles().isEmpty();
            } else {
                condition = tempEmp.getUser().getRoles().isEmpty();
            }
            if (names.contains(tempEmp.getFullName()) && condition) {
                return tempEmp;
            }
            allEmployees.remove(random);
        }
        return new Employee(new JSONObject());
    }

    /**
     * Просто случайный сотрудник который не имеет похожего имени в списке сотрудников
     */
    public static Employee getRandomEmployee() {
        List<Employee> temp = getEmployees();
        List<String> shortNames = temp.stream().map(Employee::getShortName).collect(Collectors.toList());
        temp = temp.stream().filter(employee -> Collections.frequency(shortNames, employee.getShortName()) == 1)
                .collect(Collectors.toList());
        Employee employee = getRandomFromList(temp);
        Allure.addAttachment(SELECTION_ALLURE_ATTACHMENT_TITLE, SELECTION_ALLURE_CONTENTS + employee);
        return employee;
    }

    /**
     * Получить сотрудника по названию должности
     */
    public static List<Employee> getEmployeesByPositionName(String position) {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .size(100)
                .sort(LAST_NAME)
                .onlyNow(true)
                .positionNames(position)
                .withUnattached(false)
                .withAccount(false).build();
        JSONObject empObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, EMPLOYEES, pairs);
        return getListFromJsonObject(empObject, Employee.class);
    }

    /**
     * Возвращает сотрудников с по названию должности, количество сотрудников с данной должностью не должно превышать empLimit
     *
     * @param empLimit - наибольшее количество сотрудников, которое может быть для должности
     * @return пара - список сотрудников, выбранная должность
     */
    public static ImmutablePair<List<Employee>, JobTitle> getRandomEmployeeByPositionNameWithLimit(int empLimit) {
        List<JobTitle> jobTitles = JobTitleRepository.getAllJobTitles().stream().filter(job -> (!job.getFullName().contains("1 класса") && !job.getFullName().contains("2 класса"))).collect(Collectors.toList());
        Collections.shuffle(jobTitles);
        for (JobTitle jobTitle : jobTitles) {
            List<Employee> employeesList = EmployeeRepository.getEmployeesByPositionName(jobTitle.getFullName());
            if (employeesList.size() <= empLimit && employeesList.size() > 0) {
                return new ImmutablePair<>(employeesList, jobTitle);
            }
        }
        throw new AssertionError(String.format("Нет должности с количеством сотрудников менее либо равным %d", empLimit));
    }

    /**
     * Возвращает случайного сотрудника с емейлом или без
     */
    public static Employee getEmployeeEmailOption(boolean emailExist) {
        List<Employee> temp;
        List<Employee> allEmployees = getEmployees();
        if (emailExist) {
            temp = allEmployees.stream().filter(employees -> employees.getEmail() != null).collect(Collectors.toList());
        } else {
            temp = allEmployees.stream().filter(employees -> employees.getEmail() == null).collect(Collectors.toList());
        }
        Employee employeeEmail;
        String email = "";
        //если никого не найдет, то выберет случайного сотрудника и изменит его емейл, на пустой или случайный
        if (temp.isEmpty()) {
            employeeEmail = getRandomNonRepeatEmployee(allEmployees);
            if (emailExist) {
                email = generateRandomEmail();
            }
            PresetClass.makeEmailInApi(employeeEmail, email);
            Allure.addAttachment("Изменение E-mail", "В ходе действия пресета, сотруднику "
                    + employeeEmail.getFullName() + ", e-mail был изменен на: " + email);
        } else {
            employeeEmail = getRandomNonRepeatEmployee(temp);
            if (employeeEmail.getEmail() != null) {
                email = employeeEmail.getEmail();
            }
        }
        Allure.addAttachment(SELECTION_ALLURE_ATTACHMENT_TITLE, SELECTION_ALLURE_CONTENTS + employeeEmail
                + ", его текущий e-mail: " + email);
        return employeeEmail;
    }

    /**
     * Берет случайного сотрудника из оргюнита с емейлом или без, учитывая сотрудников на UI
     *
     * @param orgUnit     - оргюнит в котором ищем сотрудников
     * @param employeesUi - список имен сотрудников с UI
     * @param withEmail   - c почтой или нет
     */
    public static Employee getRandomEmployeeNameEmailStatus(OrgUnit orgUnit, List<String> employeesUi, boolean withEmail) {
        int orgID = orgUnit.getId();
        PresetClass.checkAndMakePresetEmail(EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(orgID, LocalDateTools.getLastDate(), false), employeesUi, withEmail);
        List<EmployeePosition> empPositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(orgID, LocalDateTools.getLastDate(), false);
        ArrayList<EmployeePosition> checkedEmps = new ArrayList<>();
        for (EmployeePosition employeePosition : empPositions) {
            String mail = employeePosition.getEmployee().getEmail();
            if (!withEmail) {
                if (mail == null || !mail.contains("@")) {
                    checkedEmps.add(employeePosition);
                }
            } else {
                if (mail != null && mail.contains("@")) {
                    checkedEmps.add(employeePosition);
                }
            }
        }
        Employee randomEmployee = getRandomFromList(checkedEmps).getEmployee();
        String randomEmployeeName = randomEmployee.getShortName();
        String visibleName = "";
        int counter = 0;
        while (!visibleName.equals(randomEmployeeName) && counter < 20) {
            for (String name : employeesUi) {
                visibleName = name;
                if (visibleName.equals(randomEmployeeName)) {
                    break;
                }
            }
            randomEmployee = checkedEmps.get(RANDOM.nextInt(checkedEmps.size())).getEmployee();
            randomEmployeeName = randomEmployee.getShortName();
            counter++;
        }
        Allure.addAttachment("Сотрудник", "Выбран сотрудник с именем: " + randomEmployeeName);
        LOG.info("Выбран сотрудник с e-mail {} , имя сотрудника {}", randomEmployee.getEmail(), randomEmployeeName);
        return randomEmployee;
    }

    /**
     * Случайный сотрудник без навыков наставника
     *
     * @param employeesUi - сотрудник с UI
     * @param orgUnit     - оргюнит с которым работаем
     */
    public static Employee randomEmployeeWithoutMentorSkill(List<String> employeesUi, OrgUnit orgUnit) {
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositions(orgUnit.getId());
        List<Employee> peopleWithOutSkills = employeePositions.stream()
                .filter(employeePosition -> !employeePosition.getEmployee().isNeedMentor())
                .filter(employeePosition -> employeesUi.contains(employeePosition.getEmployee().getShortName()))
                .map(EmployeePosition::getEmployee).collect(Collectors.toList());
        int random = RANDOM.nextInt(peopleWithOutSkills.size());
        return peopleWithOutSkills.get(random);
    }

    /**
     * Выбирает случайного сотрудника который не работеют
     *
     * @return - имя сотрудника в формате тройного имени
     */
    public static Employee getRandomPersonNotWorking(List<String> empInOm) {
        List<NameValuePair> pairs = Pairs.newBuilder().fullName("").includeDate(LocalDateTools.getLastDate()).size(4000).build();
        JSONObject empObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, EMPLOYEES, pairs);
        //избавляемся от повторов
        List<Employee> allEmployees = getListFromJsonObject(empObject, Employee.class)
                .stream().distinct().collect(Collectors.toList());
        //избавляемся от сотрудников которые есть сейчас в оргюните
        List<Employee> empOnUi = allEmployees.stream().filter(employee -> empInOm.contains(employee.getShortName()))
                .collect(Collectors.toList());
        allEmployees.removeAll(empOnUi);
        LOG.info("Количество всех сотрудников: {}", allEmployees.size());
        Employee employee = getRandomFromList(allEmployees);
        Allure.addAttachment(SELECTION_ALLURE_ATTACHMENT_TITLE, employee.getFullName());
        LOG.info(SELECTION_LOGGER, employee);
        return employee;
    }

    /**
     * Список сотрудников согласно правилу сортировки
     *
     * @param otherSorters - настройки сортировки
     * @param reverse      - возвращать по порядку или в обратном порядке
     */
    public static List<Employee> getEmployeesBySorter(Pairs.Builder otherSorters, boolean reverse) {
        String sort = reverse ? "desc" : ASC;
        otherSorters.size(20).sort(LAST_NAME + "," + sort);
        JSONObject object = getJsonFromUri(Projects.WFM, Links.getTestProperty("release"), EMPLOYEES, otherSorters.build());
        return getListFromJsonObject(object, Employee.class);
    }

    /**
     * Ищет всех сотрудников со статусами, например, "декрет".
     * Считаются все статусы, даты действия которых заходят на текущий месяц.
     *
     * @param omId id оргюнита, в котором ищем работников
     * @return список работников с действующими статусами
     */
    public static List<Employee> getEmployeesWithCurrentStatuses(int omId) {
        List<EmployeeStatus> statuses = getEmployeesStatuses(omId);
        List<Employee> hasStatuses = new ArrayList<>();
        for (EmployeeStatus status : statuses) {
            JSONObject json = getJsonFromUri(Projects.WFM, URI.create(status.getSelfLink().replaceAll("/status/\\d+", "")));
            hasStatuses.add(getClassObjectFromJson(Employee.class, json));
        }
        return hasStatuses;
    }

    public static List<EmployeeStatus> getEmployeesStatuses(int omId) {
        String urlEnding = makePath(ORGANIZATION_UNITS, omId, EMPLOYEES_STATUSES_LIST);
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .from(LocalDateTools.getFirstDate())
                .to(LocalDateTools.getLastDate())
                .build();
        JSONObject object = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, nameValuePairs);
        if (object.optJSONObject(EMBEDDED) == null) {
            return new ArrayList<>();
        }
        JSONArray array = object.getJSONObject(EMBEDDED).getJSONArray(STATUSES);
        return getListFromJsonArray(array, EmployeeStatus.class);
    }

    /**
     * Ищет сотрудников в орг юните, у которых есть должности и нет статусов
     * (работают на данный момент)
     *
     * @param omId - id оргюнита, в котором ищем сотрудников
     * @return - список сотрудников, работыющих на данный момент
     */
    public static List<Employee> getWorkingEmployees(int omId) {
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeesWithPosIds(omId, null, true);
        List<Employee> hasStatuses = EmployeeRepository.getEmployeesWithCurrentStatuses(omId);
        for (Employee employee : hasStatuses) {
            employeePositions.removeIf(employeePosition -> employeePosition.getEmployee().equals(employee));
        }
        return employeePositions.stream()
                .map(EmployeePosition::getEmployee)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает случайного сотрудника, работающего на данный момент
     *
     * @param omId   id оргюнита, в котором ищем сотрудника
     * @param except сотрудники-исключения
     */
    public static Employee getRandomWorkingEmployee(int omId, Employee... except) {
        List<Employee> exceptEmployees = Arrays.asList(except);
        List<Employee> workingEmployees = getWorkingEmployees(omId);
        workingEmployees.removeAll(exceptEmployees);
        return getRandomFromList(workingEmployees);
    }

    /**
     * @param id - айди сотрудника
     * @return объект Employee
     */
    public static Employee getEmployee(int id) {
        String urlEnding = makePath(EMPLOYEES, id);
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        return new Employee(someObject);
    }

    public static Employee getEmployeeWithMoreThanOneMainPosition(OrgUnit unit) {
        List<Employee> employeesFromOrgunit = getEmployeesFromOM(unit.getId());
        for (Employee employee : employeesFromOrgunit) {
            List<EmployeePosition> employeePositions = EmployeePositionRepository.getAllEmployeePositionsFromEmployee(employee);
            boolean hasMoreThanOneMainPosition = employeePositions.stream().filter(ep -> !ep.isTemporary()).count() > 1;
            if (hasMoreThanOneMainPosition) {
                return employee;
            }
        }
        throw new AssertionError(NO_TEST_DATA + "В подразделении " + unit.getName() + " нет подходящих сотрудников");
    }

    public static Employee getEmployeeWithOnlyClosedPositions(OrgUnit unit) {
        List<Employee> employeesFromOrgunit = getEmployeesFromOM(unit.getId());
        for (Employee employee : employeesFromOrgunit) {
            List<EmployeePosition> employeePositions = EmployeePositionRepository.getAllEmployeePositionsFromEmployee(employee);
            if (!employeePositions.isEmpty()) {
                long closedPositionsCount = employeePositions.stream().filter(ep -> {
                    LocalDate endDate = ep.getDateInterval().getEndDate();
                    if (endDate != null) {
                        return ep.getDateInterval().getEndDate().isBefore(LocalDate.now());
                    } else {
                        return false;
                    }
                }).count();
                if (closedPositionsCount == employeePositions.size()) {
                    return employee;
                }
            }
        }
        return getRandomFromList(employeesFromOrgunit.stream().filter(employee -> !EmployeePositionRepository.getAllEmployeePositionsFromEmployee(employee).isEmpty()).collect(Collectors.toList()));
    }
}
