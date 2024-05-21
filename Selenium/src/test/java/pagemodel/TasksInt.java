package pagemodel;

import com.google.inject.Inject;
import guice.TestModule;
import io.qameta.allure.Step;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.TasksPage;
import reporting.TestListener;
import testutils.DatePicker;
import utils.Links;
import utils.Projects;
import utils.tools.CustomTools;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import static utils.authorization.CookieRW.getCookieWithCheck;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class TasksInt {

    private final static String URL_TASKS = Links.getTestProperty("release") + "/tasks";

    @Inject
    TasksPage ts;

    private final WebDriverWait wait = new WebDriverWait(ts.getWrappedDriver(), 20, 500);

    private final JavascriptExecutor js = (JavascriptExecutor) ts.getWrappedDriver();

    private void driverConfig() {
        ts.getWrappedDriver().manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    @BeforeMethod(alwaysRun = true)
    private void setUp() {
        driverConfig();
    }

    @AfterTest
    private void closeDriver() {
        ts.getWrappedDriver().close();
    }

    @Step("Go to URL: {0}")
    private void goToTasks(String url) {
        setUp();
        Cookie cookie = getCookieWithCheck(Projects.WFM);
        ts.open(url);
        ts.getWrappedDriver().manage().addCookie(cookie);
        ts.open(url);
        //ts.isAt(Matchers.equalTo(url));
        WebDriverWait waitForTask = new WebDriverWait(ts.getWrappedDriver(), 30);
        waitForTask.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='mdl-list']//div[26]")));
    }

    @Step("create new task")
    private void newTask() {
        ts.makerNewTak().createNewTaskButton().click();
    }

    @Step("enter task name")
    private void taskName(String param) {
        ts.makerNewTak().taskNamField().sendKeys(param + CustomTools.stringGenerator());
    }

    private String valueFromElement(WebElement element) {
        return element.getAttribute("value");
    }

    @Step("enter short Description")
    private void shortDescription(String param) {
        ts.makerNewTak().taskShortDiscriptionField().sendKeys(param + CustomTools.stringGenerator());
    }

    @Step("enter full Description")
    private void fullDescription(String param) {
        ts.makerNewTak().taskFullDiscriptionField().sendKeys(param + CustomTools.stringGenerator());
    }

    @Step("enter tag Description")
    private void tagDescription(String param) {
        ts.makerNewTak().TagField().sendKeys(param + CustomTools.stringGenerator());
    }

    @Step("choose all day or not")
    private void notAllDay() {
        ts.makerNewTak().AllDayOnOffSwitch().click();
    }

    @Step("chose start time of task ")
    private void endTimeTaskClick() {
        ts.makerNewTak().taskEndTimeButton().click();
    }

    @Step("chose start time of task ")
    private void startTimeTaskClick() {
        ts.makerNewTak().taskStartTimeButton().click();
    }

    /**
     * В момент начала срока выполнения
     * За 5 минут до начала
     * За 15 минут до начала
     * За 30 минут до начала
     * За час до начала
     * За 3 часа до начала
     * За день до начала
     * За 2 дня до начала
     */
    @Step("choose remind")
    private void remindOfTask(String param) {
        ts.makerNewTak().taskRemindButton().click();
        for (int i = 0; i <= ts.makerNewTak().variantsOfRemind().size() - 1; i++) {
            if (ts.makerNewTak().variantsOfRemind().get(i).getText().contains(param)) {
                ts.makerNewTak().variantsOfRemind().get(i).click();
            }
        }
    }

    @Step("choose type of executor Organizational module")
    private void typeOrganizationalModule() {
        ts.makerNewTak().executorType().click();
        ts.makerNewTak().variantsOfExecutorTypes().get(0).click();
    }

    @Step("choose type of executor Employee")
    private void typeEmployee() {
        ts.makerNewTak().executorType().click();
        ts.makerNewTak().variantsOfExecutorTypes().get(1).click();
    }

    @Step("save task")
    private void saveTask() {
        ts.makerNewTak().saveButtonForTask().click();
    }

    @Step()
    private void cancelTask() {
        ts.makerNewTak().cancelButtonForTask().click();
    }

    @Step
    private void chooseEmployeeOrModule(String... param) {
        for (int j = 0; j <= param.length - 1; j++) {
            ts.makerNewTak().manyExecutorsOrModuleButtons().get(j).click();
            ts.makerNewTak().searchForEmployee().sendKeys(param[j]);
            ts.makerNewTak().listOfAllExecutors().get(0).click();
        }
    }

    @Step
    private void choosePlaceForEmployee(String... param) {
        for (int j = 0, h = 0; j <= param.length * 2 - 1; j += 2, h++) {
            ts.makerNewTak().placesForWorker().get(j).click();
            ts.makerNewTak().searchForEmployee().sendKeys(param[h]);
            ts.makerNewTak().listOfAllPlaces().get(0).click();
        }
    }

    @Step("make many executors")
    private void makeManyExecutorsOrModules() {
        ts.makerNewTak().oneOrManyExecutors().click();
    }

    @Step
    private void uploadFile() {
        ts.makerNewTak().uploadFile().sendKeys("E:\\TESTS\\tes.jpg");
    }

    @Step
    private void insertStatus() {
        ts.leftBarFilter().statusButton().click();
    }

    @Step
    private void insertDate() {
        ts.leftBarFilter().dateButton().click();
    }

    @Step
    private void insertEmployee() {
        ts.leftBarFilter().employeeButton().click();
    }

    @Step
    private void insertTag() {
        ts.leftBarFilter().tagButton().click();
    }

    @Step
    private void insertSubunit() {
        ts.leftBarFilter().subunitButton().click();
    }

    /**
     * Любая
     * Сегодня
     * Просрочено
     */
    @Step
    private void clickOnAnyDate(String param) {
        for (int i = 0; i <= ts.leftBarFilter().buttonsToFindDate().size() - 1; i++) {
            if (ts.leftBarFilter().buttonsToFindDate().get(i).getText().contains(param)) {
                ts.leftBarFilter().choseDate().get(i).click();
                break;
            }
        }
    }

    /**
     * Выполнена неподтверждена
     * В процессе
     * Отклонена
     * Новая
     * Выполнена подтверждена
     * Отменена
     * Нет исполнителя
     */
    @Step
    private void clickOnAnyStatus(String... param) {
        for (int j = 0; j <= param.length - 1; j++) {
            for (int i = 0; i <= ts.leftBarFilter().buttonsToFindStatus().size() - 1; i++) {
                if (ts.leftBarFilter().buttonsToFindStatus().get(i).getText().contains(param[j])) {
                    ts.leftBarFilter().chooseStatus().get(i).click();
                    break;
                }
            }
        }
    }

    @Step
    private void clickOnAnyEmployee(String... param) {
        for (int j = 0; j <= param.length - 1; j++) {
            for (int i = 0; i <= ts.leftBarFilter().buttonsToFindEmployee().size() - 1; i++) {
                if (ts.leftBarFilter().buttonsToFindEmployee().get(i).getAttribute("title").contains(param[j])) {
                    ts.leftBarFilter().choseEmployee().get(i).click();
                    break;
                }
            }
        }
    }

    @Step
    private void clickOnAnyTag(String... param) {
        for (int j = 0; j <= param.length - 1; j++) {
            for (int i = 0; i <= ts.leftBarFilter().buttonsToFindTag().size() - 1; i++) {
                if (ts.leftBarFilter().buttonsToFindTag().get(i).getAttribute("title").contains(param[j])) {
                    ts.leftBarFilter().choseTag().get(i).click();
                    break;
                }
            }
        }
    }

    @Step
    private void clickOnAnySubunit(String... param) {
        for (int j = 0; j <= param.length - 1; j++) {
            for (int i = 0; i <= ts.leftBarFilter().buttonsToFindSubunit().size() - 1; i++) {
                if (ts.leftBarFilter().buttonsToFindSubunit().get(i).getAttribute("title").contains(param[j])) {
                    ts.leftBarFilter().chooseSection().get(i).click();
                    break;
                }
            }
        }
    }

    @Step
    private void deleteEmployeeOrModule(String param, String[] empSize) {
        for (int j = 0; j <= empSize.length - 1; j++) {
            if (ts.makerNewTak().manyExecutorsOrModuleButtons().get(j).getAttribute("value").equals(param)) {
                ts.makerNewTak().deleteEmployeeOrModule().get(j).click();
                break;
            }
        }
    }

    @Step
    private void deletePlaceForEmployee(String param, String[] omSize) {
        for (int j = 0; j <= omSize.length - 1; j++) {
            if (ts.makerNewTak().placesForWorker().get(j).getAttribute("value").equals(param)) {
                ts.makerNewTak().deletePlace().get(j).click();
                break;
            }
        }

    }

    @Step
    private void chooseStatusOfTask(String param) {
        ts.listOfTasks().statusOfTaskButton().click();
        for (int i = 1; i <= ts.listOfTasks().statusesOfTask().size(); i++) {
            if (ts.listOfTasks().statusesOfTask().get(i).getText().contains(param)) {
                ts.listOfTasks().statusesOfTask().get(i).click();
                break;
            }
        }
    }

    @Step
    private void cancelDeleteTask() {
        ts.listOfTasks().cancelPopUpWindow().click();
    }

    @Step
    private void cancelMakingTask() {
        ts.makerNewTak().cancelMakingTask().click();
    }

    @Step
    private void chooseDate(String date) {
        ts.makerNewTak().taskDate().click();
        //TODO ???
    }

    @Step
    private void chooseTime(LocalTime time) {
        DatePicker timePicker = new DatePicker(ts.timePickerForm());
        timePicker.pickTime(time);
    }

    @Step
    private void comparisonStatus(String... param) {
        for (int j = 0; j <= param.length - 1; j++) {
            for (int i = 0; i <= ts.listOfTasks().comparisonForStatus().size() - 1; i++) {
                Assert.assertTrue(ts.listOfTasks().comparisonForStatus().get(i).getAttribute("class").contains(param[j]));
            }
        }
    }

    @Step
    private void assertInsertStatusOff() {
        for (int j = 0; j <= ts.leftBarFilter().insertStatusComp().size() - 1; j++) {
            Assert.assertTrue(ts.leftBarFilter().insertStatusComp().get(j).getAttribute("class").contains("hide"));
        }
    }

    @Step
    private void assertInsertDateOff() {
        for (int j = 0; j <= ts.leftBarFilter().insertDateComp().size() - 1; j++) {
            Assert.assertTrue(ts.leftBarFilter().insertDateComp().get(j).getAttribute("class").contains("hide"));
        }
    }

    @Step
    private void assertInsertSubunitOff() {
        for (int j = 0; j <= ts.leftBarFilter().insertSubunitComp().size() - 1; j++) {
            Assert.assertTrue(ts.leftBarFilter().insertSubunitComp().get(j).getAttribute("class").contains("hide"));
        }
    }

    @Step
    private void assertInsertEmployeeOff() {
        for (int j = 0; j <= ts.leftBarFilter().insertEmployeeComp().size() - 1; j++) {
            Assert.assertTrue(ts.leftBarFilter().insertEmployeeComp().get(j).getAttribute("class").contains("hide"));
        }
    }

    @Step
    private void assertInsertTagOff() {
        for (int j = 0; j <= ts.leftBarFilter().insertTagComp().size() - 1; j++) {
            Assert.assertTrue(ts.leftBarFilter().insertTagComp().get(j).getAttribute("class").contains("hide"));
        }
    }

    @Step
    private void assertCheckboxStatusNotChecked(String param) {
        for (int i = 0; i <= ts.leftBarFilter().buttonsToFindStatus().size() - 1; i++) {
            if (ts.leftBarFilter().buttonsToFindStatus().get(i).getText().contains(param)) {
                Assert.assertFalse(ts.leftBarFilter().onOrOffStatusCheckbox().get(i).getAttribute("class").contains("is-checked"));
            }
        }
    }

    @Step
    private void assertCheckboxSubunitNotChecked(String param) {
        for (int i = 0; i <= ts.leftBarFilter().buttonsToFindSubunit().size() - 1; i++) {
            if (ts.leftBarFilter().buttonsToFindSubunit().get(i).getText().contains(param)) {
                Assert.assertFalse(ts.leftBarFilter().onOrOffSubunitCheckbox().get(i).getAttribute("class").contains("is-checked"));
            }
        }
    }

    @Step
    private void assertCheckboxTagNotChecked(String param) {
        for (int i = 0; i <= ts.leftBarFilter().buttonsToFindTag().size() - 1; i++) {
            if (ts.leftBarFilter().buttonsToFindTag().get(i).getText().contains(param)) {
                Assert.assertFalse(ts.leftBarFilter().onOrOffTagCheckbox().get(i).getAttribute("class").contains("is-checked"));
            }

        }
    }

    @Step
    private void assertCheckboxStatusChecked(String param) {
        for (int i = 0; i <= ts.leftBarFilter().buttonsToFindStatus().size() - 1; i++) {
            if (ts.leftBarFilter().buttonsToFindStatus().get(i).getText().contains(param)) {
                Assert.assertTrue(ts.leftBarFilter().onOrOffStatusCheckbox().get(i).getAttribute("class").contains("is-checked"));
            }
        }
    }

    @Step
    private void assertCheckboxDateChecked(String param) {
        for (int i = 0; i <= ts.leftBarFilter().buttonsToFindDate().size() - 1; i++) {
            if (ts.leftBarFilter().buttonsToFindDate().get(i).getText().contains(param)) {
                Assert.assertTrue(ts.leftBarFilter().onOrOffDateCheckbox().get(i).getAttribute("class").contains("is-checked"));
            }
        }
    }

    @Step
    private void assertCheckboxSubunitChecked(String param) {
        for (int i = 0; i <= ts.leftBarFilter().buttonsToFindSubunit().size() - 1; i++) {
            if (ts.leftBarFilter().buttonsToFindSubunit().get(i).getText().contains(param)) {
                Assert.assertTrue(ts.leftBarFilter().onOrOffSubunitCheckbox().get(i).getAttribute("class").contains("is-checked"));
            }
        }
    }

    @Step
    private void assertCheckboxEmployeeChecked(String param) {
        for (int i = 0; i <= ts.leftBarFilter().buttonsToFindEmployee().size() - 1; i++) {
            if (ts.leftBarFilter().buttonsToFindEmployee().get(i).getText().contains(param)) {
                Assert.assertTrue(ts.leftBarFilter().onOrOffEmployeeCheckbox().get(i).getAttribute("class").contains("is-checked"));
            }
        }
    }

    @Step
    private void assertCheckboxTagChecked(String param) {
        for (int i = 0; i <= ts.leftBarFilter().buttonsToFindTag().size() - 1; i++) {
            if (ts.leftBarFilter().buttonsToFindTag().get(i).getText().contains(param)) {
                Assert.assertTrue(ts.leftBarFilter().onOrOffTagCheckbox().get(i).getAttribute("class").contains("is-checked"));
            }
        }
    }

    @Step
    private void assertForMakingTasks() {
        int size = ts.listOfTasks().allTasks().size();
        for (int i = 0; i <= size - 1; i++) {
            if (i == size - 1) {
                js.executeScript("document.getElementsByTagName('td')[" + size + "].scrollIntoView()");
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//table[@class='mdl-data-table au-target mdl-js-data-table']//tbody/tr/td[4]/span[1]")));
            }
        }
    }

    @Test(groups = "TSK-1.1", description = "Просмотр пунктов в разделе Статус")
    private void viewItemsInStatusSection() {
        goToTasks(URL_TASKS);
        insertStatus();
        insertStatus();
        assertInsertStatusOff();
    }

    @Test(groups = "TSK-1.2.1", description = "Просмотр задач со статусом Выполнена не подтверждена")
    private void viewTasksWithTheStatusCompletedNotConfirmed() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Выполнена не");
        assertCheckboxStatusChecked("Выполнена не");

    }

    @Test(groups = "TSK-1.2.2", description = "Просмотр задач со статусом В процессе")
    private void viewTasksWithStatusInProcess() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("В процессе");
        assertCheckboxStatusChecked("В процессе");
    }

    @Test(groups = "TSK-1.2.3", description = "Просмотр задач со статусом Отклонена")
    private void viewTasksWithStatusRejected() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Отклонена");
        assertCheckboxStatusChecked("Отклонена");
    }

    @Test(groups = "TSK-1.2.4", description = "Просмотр задач со статусом Новая")
    private void viewTasksWithStatusNew() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Новая");
        comparisonStatus("green");
        assertCheckboxStatusChecked("Новая");
    }

    @Test(groups = "TSK-1.2.5", description = "Просмотр задач со статусом Выполнена")
    private void viewTasksWithStatusCompletedConfirmed() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Выполнена подтверждена");
        assertCheckboxStatusChecked("Выполнена подтверждена");
    }

    @Test(groups = "TSK-1.2.6", description = "Просмотр задач со статусом Отменена")
    private void viewTasksWithStatusCanceled() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Отменена");
        assertCheckboxStatusChecked("Отменена");

    }

    @Test(groups = "TSK-1.2.7", description = "Просмотр задач со статусом Нет исполнителя")
    private void viewTasksWithStatusNoPerformer() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Нет исполнителя");
        assertCheckboxStatusChecked("Нет исполнителя");
    }

    @Test(groups = "TSK-1.3.1", description = "Отключение фильтра Выполнена не подтверждена")
    private void shutdownTasksWithTheStatusCompletedNotConfirmed() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Выполнена не");
        clickOnAnyStatus("Выполнена не");
        assertCheckboxStatusNotChecked("Выполнена не");
    }

    @Test(groups = "TSK-1.3.2", description = "Отключение фильтра В процессе")
    private void shutdownTasksWithStatusInProcess() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("В процессе");
        clickOnAnyStatus("В процессе");
        assertCheckboxStatusNotChecked("В процессе");
    }

    @Test(groups = "TSK-1.3.3", description = "Отключение фильтра Отклонена")
    private void shutdownTasksWithStatusRejected() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Отклонена");
        clickOnAnyStatus("Отклонена");
        assertCheckboxStatusNotChecked("Отклонена");
    }

    @Test(groups = "TSK-1.3.4", description = "Отключение фильтра Новая")
    private void shutdownTasksWithStatusNew() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Новая");
        clickOnAnyStatus("Новая");
        assertCheckboxStatusNotChecked("Новая");
    }

    @Test(groups = "TSK-1.3.5", description = "Отключение фильтра Выполнена подтверждена")
    private void shutdownTasksWithStatusCompletedConfirmed() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Выполнена подтверждена");
        clickOnAnyStatus("Выполнена подтверждена");
        assertCheckboxStatusNotChecked("Выполнена подтверждена");
    }

    @Test(groups = "TSK-1.3.6", description = "Отключение фильтра Отменена")
    private void shutdownTasksWithStatusCanceled() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Отменена");
        clickOnAnyStatus("Отменена");
        assertCheckboxStatusNotChecked("Отменена");
    }

    @Test(groups = "TSK-1.3.7", description = "Отключение фильтра Нет исполнителя")
    private void shutdownTasksWithStatusNoPerformer() {
        goToTasks(URL_TASKS);
        insertStatus();
        clickOnAnyStatus("Нет исполнителя");
        clickOnAnyStatus("Нет исполнителя");
        assertCheckboxStatusNotChecked("Нет исполнителя");
    }

    @Test(groups = "TSK-2.1", description = "Просмотр пунктов в разделе Дата исполнения")
    private void viewItemsInThePerformanceDateSection() {
        goToTasks(URL_TASKS);
        insertDate();
        insertDate();
        assertInsertDateOff();
    }

    @Test(groups = "TSK-2.2", description = "Просмотр просроченных задач")
    private void viewOverdueTasks() {
        goToTasks(URL_TASKS);
        insertDate();
        clickOnAnyDate("Просрочено");
        assertCheckboxDateChecked("Просрочено");
    }

    @Test(groups = "TSK-2.3", description = "Переключение фильтров разделе Дата исполнения")
    private void switchingFiltersSectionExecutionDate() {
        goToTasks(URL_TASKS);
        insertDate();
        clickOnAnyDate("Просрочено");
        clickOnAnyDate("Сегодня");
        clickOnAnyDate("Любая");
        assertCheckboxDateChecked("Любая");
    }

    @Test(groups = "TSK-3.1", description = "Просмотр пунктов в разделе Подразделение")
    private void viewItemsInSection() {
        goToTasks(URL_TASKS);
        insertSubunit();
        insertSubunit();
        assertInsertSubunitOff();
    }

    @Test(groups = "TSK-3.2", description = "Просмотр задач подразделения")
    private void viewTasksOfSection() {
        goToTasks(URL_TASKS);
        insertSubunit();
        clickOnAnySubunit("Атриум_2");
        assertCheckboxSubunitChecked("Атриум_2");
    }

    @Test(groups = "TSK-3.3", description = "Отмена сортировки задач по подразделению")
    private void cancelSortTasksBySection() {
        goToTasks(URL_TASKS);
        insertSubunit();
        clickOnAnySubunit("Атриум_2");
        clickOnAnySubunit("Атриум_2");
        assertCheckboxSubunitNotChecked("Атриум_2");
    }

    @Test(groups = "TSK-4.1", description = "Просмотр пунктов в разделе Сотрудник")
    private void viewItemsInEmployee() {
        goToTasks(URL_TASKS);
        insertEmployee();
        insertEmployee();
        assertInsertEmployeeOff();
    }

    @Test(groups = "TSK-4.2", description = "Просмотр задач определенного сотрудника")
    private void viewTheTasksOfSpecificEmployee() {
        goToTasks(URL_TASKS);
        insertEmployee();
        clickOnAnyEmployee("Буров Георгий");
        assertCheckboxEmployeeChecked("Буров Георгий");
    }

    @Test(groups = "TSK-4.3", description = "Отмена сортировки задач по сотрудникам")
    private void cancelSortingTasksByEmployees() {
        goToTasks(URL_TASKS);
        insertEmployee();
        clickOnAnyEmployee("Буров Георгий");
        clickOnAnyEmployee("Буров Георгий");
        assertCheckboxSubunitNotChecked("Буров Георгий");
    }

    @Test(groups = "TSK-5.1", description = "Просмотр пунктов в разделе Тег")
    private void viewItemsInTheTagSection() {
        goToTasks(URL_TASKS);
        insertTag();
        insertTag();
        assertInsertTagOff();
    }

    @Test(groups = "TSK-5.2", description = "Просмотр задач по тегу")
    private void viewPostByTag() {
        goToTasks(URL_TASKS);
        insertTag();
        clickOnAnyTag("ddbgdgfhg");
        assertCheckboxTagChecked("ddbgdgfhg");
    }

    @Test(groups = "TSK-5.3", description = "Отмена сортировки задач по тегу")
    private void cancelSortingTasksByTag() {
        goToTasks(URL_TASKS);
        insertTag();
        clickOnAnyTag("ddbgdgfhg");
        clickOnAnyTag("ddbgdgfhg");
        assertCheckboxTagNotChecked("ddbgdgfhg");
    }

    @Test(groups = "TSK-6")
    private void deleteTask65() {
        goToTasks(URL_TASKS);
        //        clickOnTask(30);
        chooseStatusOfTask("Удалить");
    }

    @Test(groups = "TSK-6")
    private void cancelDelete() {
        goToTasks(URL_TASKS);
        //        clickOnTask(8);
        chooseStatusOfTask("Удалить");
        cancelDeleteTask();
    }

    @Test(groups = "TSK-6.1", description = "Создание новой задачи для одиночного подразделения с продолжительностью Весь день, отсутствием напоминания и добавлением файла")
    private void creatingANewTaskForSingleUnitWithDurationAllDayLackOfRemindersAndAddingAFile() {
        goToTasks(URL_TASKS);
        newTask();
        taskName("tsk name");
        String name = valueFromElement(ts.makerNewTak().taskNamField());
        shortDescription("tsk short dskr");
        fullDescription("tsk full dskr");
        tagDescription("tsk tag dskr");
        chooseDate("12.09.2020");
        remindOfTask("Не напоминать");
        typeOrganizationalModule();
        chooseEmployeeOrModule("Атриум_2");
        uploadFile();
        saveTask();
        assertForMakingTasks();
    }

    @Test(groups = "TSK-6.2", description = "Создание новой задачи для одного сотрудника с продолжительностью неполный день и наличием напоминания")
    private void creatingNewTaskForOneEmployeeWithDurationPartTimeAndTheAvailabilityOfReminders() {
        goToTasks(URL_TASKS);
        newTask();
        taskName("tsk name");
        String name = valueFromElement(ts.makerNewTak().taskNamField());
        shortDescription("tsk short dskr");
        fullDescription("tsk full dskr");
        tagDescription("tsk tag dskr");
        notAllDay();
        chooseDate("07.06.2019");
        startTimeTaskClick();
        chooseTime(LocalTime.of(6, 5));
        endTimeTaskClick();
        chooseTime(LocalTime.of(9, 5));
        remindOfTask("В момент начала срока выполнения");
        typeEmployee();
        chooseEmployeeOrModule("Яшнова Татьяна");
        choosePlaceForEmployee("Атриум_2");
        saveTask();
        assertForMakingTasks();
    }

    @Test(groups = "TSK-6.2.1", description = "Переключение элементов списка Уведомление о сроке окончания задачи")
    private void switchingListItemsNotificationEndDateTask() {
        goToTasks(URL_TASKS);
        newTask();
        remindOfTask("В момент начала срока выполнения");
        remindOfTask("За 5 минут до начала");
        remindOfTask("За 15 минут до начала");
        remindOfTask("За 30 минут до начала");
        remindOfTask("За час до начала");
        remindOfTask("За 3 часа до начала");
        remindOfTask("За день до начала");
        remindOfTask("За 2 дня до начала");
    }

    //пока сломаны
    @Test(groups = "TSK-6.5.1", description = "Создание новой задачи для одного сотрудника с продолжительностью Весь день и напоминанием В момент начала срока выполнения")
    private void creatingNewTaskForOneEmployeeWithDurationAllDayAndReminder() {
        goToTasks(URL_TASKS);
        newTask();
        taskName("tsk name");
        String name = valueFromElement(ts.makerNewTak().taskNamField());
        chooseDate("20.06.2021");
        remindOfTask("В момент начала срока выполнения");
        chooseEmployeeOrModule("Колосова Инга");
        saveTask();
        assertForMakingTasks();
    }

    @Test(groups = "TSK-6.5.2", description = "Создание новой задачи для одного сотрудника с продолжительностью Весь день и напоминанием За 5 минут до начала")
    private void before5Min() {
        goToTasks(URL_TASKS);
        newTask();
        taskName("tsk name");
        String name = valueFromElement(ts.makerNewTak().taskNamField());
        chooseDate("20.06.2021");
        remindOfTask("За 5 минут до начала");
        chooseEmployeeOrModule("Колосова Инга");
        choosePlaceForEmployee("B2 Проверка KDRS-ckqlrmfvhs");
        saveTask();
        assertForMakingTasks();
    }

    @Test(groups = "TSK-6.5.3", description = "Создание новой задачи для одного сотрудника с продолжительностью Весь день и напоминанием За 15 минут до начала")
    private void before15Min() {
        goToTasks(URL_TASKS);
        newTask();
        taskName("tsk name");
        String name = valueFromElement(ts.makerNewTak().taskNamField());
        chooseDate("20.06.2021");
        remindOfTask("15 минут до начала");
        chooseEmployeeOrModule("Колосова Инга");
        choosePlaceForEmployee("B2 Проверка KDRS-ckqlrmfvhs");
        saveTask();
        assertForMakingTasks();
    }

    @Test(groups = "TSK-6.5.4", description = "Создание новой задачи для одного сотрудника с продолжительностью Весь день и напоминанием За 30 минут до начала")
    private void before30Min() {
        goToTasks(URL_TASKS);
        newTask();
        taskName("tsk name");
        String name = valueFromElement(ts.makerNewTak().taskNamField());
        chooseDate("20.06.2021");
        remindOfTask("30 минут до начала");
        chooseEmployeeOrModule("Колосова Инга");
        choosePlaceForEmployee("B2 Проверка KDRS-ckqlrmfvhs");
        saveTask();
        assertForMakingTasks();
    }

    @Test(groups = "TSK-6.5.5", description = "Создание новой задачи для одного сотрудника с продолжительностью Весь день и напоминанием За час до начала")
    private void before1Hour() {
        goToTasks(URL_TASKS);
        newTask();
        taskName("tsk name");
        String name = valueFromElement(ts.makerNewTak().taskNamField());
        chooseDate("20.06.2021");
        remindOfTask("час до начала");
        chooseEmployeeOrModule("Колосова Инга");
        choosePlaceForEmployee("B2 Проверка KDRS-ckqlrmfvhs");
        saveTask();
        assertForMakingTasks();
    }

    @Test(groups = "TSK-6.5.6", description = "Создание новой задачи для одного сотрудника с продолжительностью Весь день и напоминанием За 3 часа до начала")
    private void before3Hour() {
        goToTasks(URL_TASKS);
        newTask();
        taskName("tsk name");
        String name = valueFromElement(ts.makerNewTak().taskNamField());
        chooseDate("20.06.2021");
        remindOfTask("3 часа до начала");
        chooseEmployeeOrModule("Колосова Инга");
        choosePlaceForEmployee("B2 Проверка KDRS-ckqlrmfvhs");
        saveTask();
        assertForMakingTasks();
    }

    @Test(groups = "TSK-6.5.7", description = "Создание новой задачи для одного сотрудника с продолжительностью Весь день и напоминанием За день до начала")
    private void before1Day() {
        goToTasks(URL_TASKS);
        newTask();
        taskName("tsk name");
        String name = valueFromElement(ts.makerNewTak().taskNamField());
        chooseDate("20.06.2021");
        remindOfTask("день до начала");
        chooseEmployeeOrModule("Колосова Инга");
        choosePlaceForEmployee("B2 Проверка KDRS-ckqlrmfvhs");
        saveTask();
        assertForMakingTasks();
    }

    @Test(groups = "TSK-6.5.8", description = "Создание новой задачи для одного сотрудника с продолжительностью Весь день и напоминанием За 2 дня до начала")
    private void before2Day() {
        goToTasks(URL_TASKS);
        newTask();
        taskName("tsk name");
        String name = valueFromElement(ts.makerNewTak().taskNamField());
        chooseDate("20.06.2021");
        remindOfTask("2 дня до начала");
        chooseEmployeeOrModule("Колосова Инга");
        choosePlaceForEmployee("B2 Проверка KDRS-ckqlrmfvhs");
        saveTask();
        assertForMakingTasks();
    }

    @Test(groups = "TSK-6.6", description = "Сохранение пустой формы")
    private void saveInvalidTask() {
        goToTasks(URL_TASKS);
        newTask();
        saveTask();
    }

    @Test(groups = "TSK-6.7", description = "Закрытие формы")
    private void closeNewTask() {
        goToTasks(URL_TASKS);
        newTask();
        cancelMakingTask();
    }

    @Test(groups = "TSK-6.8", description = "Отмена создания задачи")
    private void cancelNewTask() {
        goToTasks(URL_TASKS);
        newTask();
        taskName("tsk name");
        String name = valueFromElement(ts.makerNewTak().taskNamField());
        chooseDate("05.06.2021");
        typeEmployee();
        chooseEmployeeOrModule("Шангин Фёдор");
        cancelTask();
    }

    @Test(groups = "TSK-6.9", description = "Отмена выбора даты")
    private void cancelTsk() {
        goToTasks(URL_TASKS);
        newTask();
        chooseDate("19.07.2019");
        cancelTask();
    }

    @Test(groups = "TSK-6.10.1", description = "Удаление одиночного исполнителя Сотрудник")
    private void deleteEmployee() {
        goToTasks(URL_TASKS);
        newTask();
        String[] empSize = {"Гурин Иван", "Батурин Илья"};
        chooseEmployeeOrModule(empSize);
        String[] omSize = {"B2 Проверка KDRS-ckqlrmfvhs", "Атриум 2"};
        choosePlaceForEmployee(omSize);
        deleteEmployeeOrModule("Батурин Илья", empSize);
        deletePlaceForEmployee("Атриум 2", omSize);
    }

    @Test(groups = "TSK-6.10.2", description = "Удаление одиночного исполнителя Организационный модуль")
    private void singleEmployeeRemovalOrganizationalModule() {
        goToTasks(URL_TASKS);
        newTask();
        typeOrganizationalModule();
        String[] empSize = new String[]{"Атриум_2"};
        chooseEmployeeOrModule(empSize);
        deleteEmployeeOrModule("Атриум 2", empSize);
    }

    @Test(groups = "TSK-6.10.3", description = "Удаление выбранного исполнителя Сотрудник")
    private void removingManyEmployee() {
        goToTasks(URL_TASKS);
        newTask();
        makeManyExecutorsOrModules();
        chooseEmployeeOrModule("Батурин Илья", "Шангин Фёдор");
        choosePlaceForEmployee("Атриум_2", "B2 Проверка KDRS-ckqlrmfvhs");
    }

    @Test(groups = "TSK-6.10.4", description = "Удаление выбранного исполнителя Организационный модуль")
    private void removingManyModules() {
        goToTasks(URL_TASKS);
        newTask();
        makeManyExecutorsOrModules();
        typeOrganizationalModule();
        String[] empSize = {"Атриум_2", "B2 Проверка KDRS-ckqlrmfvhs"};
        chooseEmployeeOrModule(empSize);
        deleteEmployeeOrModule("Атриум_2", empSize);
    }

    @Test(groups = "TSK-7", description = "Изменение порядка отображения задач")
    private void changingOrderOfDisplayingTasks() {
        goToTasks(URL_TASKS);
        insertDate();
    }

}

