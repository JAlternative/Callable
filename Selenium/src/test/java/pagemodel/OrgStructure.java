package pagemodel;

import com.google.inject.Inject;
import com.mchange.util.AssertException;
import common.DataProviders;
import elements.orgstructure.EmployeeInfoBlock;
import elements.orgstructure.Sorter;
import elements.scheduleBoard.EmployeeParametersMenu;
import guice.TestModule;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.exception.WaitUntilException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.OrgStructurePage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.*;
import utils.Links;
import utils.Projects;
import utils.TimeType;
import utils.tools.CustomTools;
import utils.tools.Format;
import utils.tools.LocalDateTools;
import wfm.PresetClass;
import wfm.components.orgstructure.MathParameters;
import wfm.components.orgstructure.*;
import wfm.components.schedule.*;
import wfm.components.utils.*;
import wfm.models.*;
import wfm.repository.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static common.ErrorMessagesForRegExp.*;
import static common.Groups.*;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.CustomTools.*;
import static utils.tools.Format.UI;
import static utils.tools.Format.UI_DOTS;
import static utils.tools.RequestFormers.*;
import static wfm.PresetClass.TEST_ROLE;
import static wfm.models.MathParameter.MathValue;
import static wfm.models.User.getById;
import static wfm.repository.JobTitleRepository.getJob;
import static wfm.repository.JobTitleRepository.randomJobTitle;
import static wfm.repository.OrgUnitRepository.*;
import static wfm.repository.UserDeputyRepository.getUserDeputies;

@SuppressWarnings("groupsTestNG")
@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class OrgStructure extends BaseTest {

    private static final String RELEASE = Links.getTestProperty("release");
    private static final String CHANGE_OM_NAME = "B2 Проверка KDRS";
    private static final Logger LOG = LoggerFactory.getLogger(OrgStructure.class);
    private static final Section SECTION = Section.ORG_STRUCTURE;
    private static final String URL_ORG_STRUCTURE = RELEASE + SECTION.getUrlEnding();

    @Inject
    private OrgStructurePage os;

    @DataProvider(name = "positionWithoutPermissionAdding")
    private static Object[][] positionWithoutPermissionAdding() {
        Object[][] array = new Object[3][];
        array[0] = new Object[]{Role.SIXTH, LocalDate.now()};
        array[1] = new Object[]{Role.SIXTH, LocalDate.now().plusDays(new Random().nextInt(100) + 1)};
        array[2] = new Object[]{Role.FIFTH, LocalDate.now().minusDays(new Random().nextInt(100) + 1)};
        return array;
    }

    @DataProvider(name = "positionAdding")
    private static Object[][] positionAdding() {
        Object[][] array = new Object[6][];
        Random random = new Random();
        array[0] = new Object[]{Role.FIRST, LocalDate.now()};
        array[1] = new Object[]{Role.FIRST, LocalDate.now().plusDays(random.nextInt(100) + 1)};
        array[2] = new Object[]{Role.FIFTH, LocalDate.now()};
        array[3] = new Object[]{Role.FIFTH, LocalDate.now().plusDays(random.nextInt(100) + 1)};
        return array;
    }

    @DataProvider(name = "addEndDateNowAndInFuture")
    private static Object[][] positionDateEndAdding() {
        Object[][] array = new Object[6][];
        array[0] = new Object[]{Role.FIRST, ShiftTimePosition.DEFAULT};
        array[1] = new Object[]{Role.FIRST, ShiftTimePosition.FUTURE};
        array[2] = new Object[]{Role.SEVENTH, ShiftTimePosition.DEFAULT};
        array[3] = new Object[]{Role.SEVENTH, ShiftTimePosition.FUTURE};
        array[4] = new Object[]{Role.EIGHTH, ShiftTimePosition.DEFAULT};
        array[5] = new Object[]{Role.EIGHTH, ShiftTimePosition.FUTURE};
        return array;
    }

    @DataProvider(name = "positionDateEndAddingWithoutPermission")
    private static Object[][] positionDateEndAddingWithoutPermission() {
        Object[][] array = new Object[3][];
        Random random = new Random();
        array[0] = new Object[]{Role.EIGHTH, LocalDate.now()};
        array[1] = new Object[]{Role.EIGHTH, LocalDate.now().plusDays(random.nextInt(100) + 1)};
        array[2] = new Object[]{Role.SEVENTH, LocalDate.now().minusDays(random.nextInt(100) + 1)};
        return array;
    }

    @DataProvider(name = "positionStartDateChanging")
    private static Object[][] positionStartDateChanging() {
        Object[][] array = new Object[6][];
        Random random = new Random();
        array[0] = new Object[]{Role.FIRST, LocalDate.now()};
        array[1] = new Object[]{Role.FIRST, LocalDate.now().plusDays(random.nextInt(100) + 1)};
        array[2] = new Object[]{Role.NINTH, LocalDate.now()};
        array[3] = new Object[]{Role.NINTH, LocalDate.now().plusDays(random.nextInt(100) + 1)};
        array[4] = new Object[]{Role.FIRST, LocalDate.now().minusDays(random.nextInt(100) + 1)};
        array[5] = new Object[]{Role.TENTH, LocalDate.now().minusDays(random.nextInt(100) + 1)};
        return array;
    }

    @DataProvider(name = "positionStartDateChangingWithoutPermissions")
    private static Object[][] positionStartDateChangingWithoutPermissions() {
        Object[][] array = new Object[3][];
        Random random = new Random();
        array[0] = new Object[]{Role.TENTH, LocalDate.now()};
        array[1] = new Object[]{Role.TENTH, LocalDate.now().plusDays(random.nextInt(100) + 1)};
        array[2] = new Object[]{Role.NINTH, LocalDate.now().minusDays(random.nextInt(100) + 1)};
        return array;
    }

    @DataProvider(name = "snilsData")
    private Object[][] snilsData() {
        return new Object[][]{
                {"Можно ввести в поле СНИЛС количество цифр до 11 включительно", "ABCHR5330-1", true,
                        String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000_000L, 100_000_000_000L - 1))},
                {"Нельзя ввести в поле СНИЛС более 11 цифр", "ABCHR5330-2", false,
                        String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L - 1))},
                {"Ввести ноль цифр в поле СНИЛС (Очистить поле СНИЛС)", "ABCHR5330-3", false, ""}
        };
    }

    @DataProvider(name = "hideDataFirst")
    private String[] hideDataFirst() {
        return new String[]{
                "[1,2,3,4,5,6]", "[2,3,4,5,6]", "[3,4,5,6]", "[4,5,6]", "[5,6]", "[6]"};
    }

    @DataProvider(name = "hideDataSecond")
    private String[] hideDataSecond() {
        return new String[]{
                "NONE", "[1,2,3,4,5,6,7]", "NONE"};
    }

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void tearDown() {
        closeDriver(os.getWrappedDriver());
    }

    @AfterMethod(alwaysRun = true, description = "Очистить куки и подготовить новую вкладку")
    private void cleanCookies() {
        cleanCookiesAndOpenNewTab(os.getWrappedDriver());
    }

    @BeforeTest(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void setUp() {
        setBrowserTimeout(os.getWrappedDriver(), 30);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"CheckInDifferentModes"})
    public void setUpGroups() {
        goToOrgStructure();
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before manipulation with contacts in employee card"},
            description = "Сделать видимой группу \"Контакты\" в карточке сотрудника")
    public void setEmployeeExcludeGroups() {
        SystemProperty prop = SystemPropertyRepository.getSystemProperty(SystemProperties.EMPLOYEE_EXCLUDE_GROUPS);
        if (prop.getValue() == null) {
            return;
        }
        String excludeGroups = (String) prop.getValue();
        if (excludeGroups.contains("1,")) {
            changeProperty(SystemProperties.EMPLOYEE_EXCLUDE_GROUPS, excludeGroups.replace("1,", ""));
        }
    }

    @AfterGroups(value = "TEST-948", alwaysRun = true)
    @Step("Вернуть значения параметра в дефолтное состояние \"true\"")
    private void setDefaultProperties() {
        PresetClass.setSystemPropertyValue(SystemProperties.MANAGER_ALL_EMPLOYEES_ALLOW, true);
    }

    @AfterGroups(alwaysRun = true, value = {"OM-2", "TK2784-4"})
    @Step("Вернуть параметры оргюнита назад, после изменений в тесте")
    private void setOrgUnitParentAndType() {
        PresetClass.makeTypeAndParentUnitSettingBack();
    }

    /**
     * Метод для перехода в оргструктуру
     */
    private void goToOrgStructure() {
        new GoToPageSection(os).getPage(SECTION, 60);
        os.osSearchForm().waitUntil("Форма поиска не отображается", DisplayedMatcher.displayed(), 60);
        os.spinnerLoader().loadingForm().waitUntil("Спиннер все еще крутится",
                                                   Matchers.not(DisplayedMatcher.displayed()), 60);
    }

    @Step("Перейти в раздел \"Оргструктура\" с ролью \"{role.name}\"")
    private void goToOrgStructureAsUser(Role role) {
        new RoleWithCookies(os.getWrappedDriver(), role).getPage(SECTION, 60, true);
        os.spinnerLoader().loadingForm().waitUntil("Спиннер все еще крутится",
                                                   Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Перейти в раздел \"Оргструктура\" с ролью \"{role.name}\"")
    private void goToOrgStructureAsUser(Role role, User user) {
        new RoleWithCookies(os.getWrappedDriver(), role, user).getPage(SECTION, 60, true);
        os.spinnerLoader().loadingForm().waitUntil("Спиннер все еще крутится",
                                                   Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Перейти в раздел \"Оргструктура\" с ролью \"{role.name}\"")
    private void goToOrgStructureAsUserWithoutWait(Role role, OrgUnit orgUnit) {
        new RoleWithCookies(os.getWrappedDriver(), role, orgUnit).getPage(SECTION);
    }

    @Step("Перейти в раздел \"Оргструктура\" с ролью \"{role.name}\"")
    private void goToOrgStructureAsUser(Role role, OrgUnit orgUnit) {
        new RoleWithCookies(os.getWrappedDriver(), role, orgUnit).getPage(SECTION, 60, true);
    }

    @Step("Перейти в раздел \"Оргструктура\" с ролью \"{role.name}\"")
    private void goToOrgStructureAsUser(Role role, OrgUnit orgUnit, User user) {
        new RoleWithCookies(os.getWrappedDriver(), role, orgUnit, user).getPage(SECTION, 60, true);
    }

    @Step("Перейти в раздел \"Оргструктура\" с ролью \"{role.name}\"")
    private void goToOrgStructureAsUserWithoutWait(Role role, OrgUnit orgUnit, User user) {
        new RoleWithCookies(os.getWrappedDriver(), role, orgUnit, user).getPage(SECTION);
    }

    @Step("Нажать на фильтр \"Тип подразделения\"")
    private void clickOmTypeMenu() {
        os.osFilterForm().omType().click();
    }

    @Step("Нажать на фильтр \"Тэги\"")
    private void clickOmTags() {
        os.filterTypeOmForm()
                .waitUntil("Форма для выбора тегов не была отображена", Matchers.notNullValue(), 5);
        os.osFilterForm().omTags().click();
    }

    @Step("Нажать на чекбокс с типом {omType}")
    private void selectOmType(String omType) {
        os.filterTypeOmForm().selectedOmType(omType).click();
    }

    @Step("Нажать на кнопку \"Выбрать\" в меню \"Тип подразделения\"")
    private void pressOkOmType() {
        os.filterTypeOmForm().typeOmOkButton().click();
    }

    @Step("Нажать на кнопку \"Сбросить\" в меню \"Тип подразделения\"")
    private void pressResetOmType() {
        os.filterTypeOmForm().typeOmResetButton().click();
    }

    @Step("Нажать на кнопку \"Подразделения\"")
    private void clickFilterOM() {
        os.osFilterForm().omButton().click();
    }

    @Step("Нажать на кнопку \"Выбрать\" в форме \"Подразделения\"")
    private void filterClickOk() {
        os.filterOmForm().omOkButton().isEnabled();
        os.filterOmForm().omOkButton().click();
        os.spinnerLoader().loadingForm().waitUntil("Спиннер все еще крутится",
                                                   Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Нажать на кнопку \"Сбросить\"")
    private void filterClickReset() {
        os.filterOmForm().omResetButton().isEnabled();
        os.filterOmForm().omResetButton().click();
    }

    private void makeClickEmployeeTab() {
        systemSleep(5); //элемент считается кликабельным, но при клике на него без этого ожидания UI показывает подпись к иконке, но перехода на вкладку не происходит
        waitForClickable(os.osSwitchToTabs().empTab(), os, 10);
        os.osSwitchToTabs().empTab().click();
        os.osSearchForm().waitUntil("Страница не загрузилась", Matchers.notNullValue(), 5);
    }

    @Step("Нажать на кнопку \"Сотрудники\"")
    private void clickEmployeeTab() {
        makeClickEmployeeTab();
        os.osSearchForm().allSearchResult().waitUntil(Matchers.hasSize(Matchers.greaterThan(0)));
    }

    @Step("Нажать на кнопку \"Сотрудники\"")
    private void clickEmployeeTabWithRole() {
        makeClickEmployeeTab();
    }

    @Step("Нажать на кнопку \"Роли\"")
    private void clickRoleTab() {
        waitForClickable(os.osSwitchToTabs().roleTab(), os, 10);
        os.osSwitchToTabs().roleTab().click();
        LOG.info("Нажали на кнопку \"Роли\"");
    }

    @Step("Нажать на кнопку \"Просмотр прав\"")
    private void pressGiveRolesButton() {
        waitForClickable(os.empActionForm().addRoleButton(), os, 10);
        os.empActionForm().addRoleButton().click();
        os.spinnerLoader().loadingForm()
                .waitUntil("Список сотрудников всё ещё загружается", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Нажать на кнопку \"Создать роль\"")
    private void pressAddRoleButton() {
        waitForClickable(os.empActionForm().addRoleButton(), os, 10);
        os.empActionForm().addRoleButton().click();
    }

    @Step("Нажать на кнопку \"Добавить подразделение\"")
    private void pressAddUnitButton() {
        systemSleep(1);//без этого ожидания кнопка иногда нажимается до того, как может на это нажатие отреагировать
        os.addUnitButton()
                .should(OM_CREATION_BUTTON_NOT_DISPLAYED, DisplayedMatcher.displayed(), 10);
        new Actions(os.getWrappedDriver()).moveToElement(os.addUnitButton());
        os.addUnitButton().click();
    }

    @Step("Нажать на кнопку \"Добавить сотрудника\"")
    private void pressAddEmployeeButton() {
        os.empActionForm().addEmployeeButton()
                .waitUntil(EMPLOYEE_CREATION_BUTTON_NOT_DISPLAYED, DisplayedMatcher.displayed(), 5);
        os.empActionForm().addEmployeeButton().click();
    }

    @Step("Нажать на кнопку \"Подразделения\"")
    private void clickOmFilterTab() {
        os.osFilterForm().employeeOMButton().click();
        os.osSearchForm().waitUntil("Page not loaded", Matchers.notNullValue(), 5);
    }

    @Step("В поле \"{timeType.name}\" ввести время {timeSend}")
    private void chooseTime(TimeTypeField timeType, String timeSend) {
        int size = os.omEditingForm().chooseTime(timeType.getName()).size() - 1;
        os.omEditingForm().chooseTime(timeType.getName()).get(size).waitUntil("Поле для ввода времени не подгрузилось", DisplayedMatcher.displayed());
        os.omEditingForm().chooseTime(timeType.getName()).get(size).click();
        os.omEditingForm().chooseTime(timeType.getName()).get(size).clear();
        os.omEditingForm().chooseTime(timeType.getName()).get(size).sendKeys(timeSend);
    }

    @Step("Раскрыть список в поле \"Тип дня\" напротив нового исключения")
    private void openType() {
        os.omEditingForm().chooseType().click();
    }

    @Step("В раскрывшемся списке выбрать \"{type.name}\"")
    private void chooseExceptionType(DateTypeField type) {
        systemSleep(2); //чтобы панелька с выборами успела развернуться иначе мисскликает
        os.omEditingForm().typeOfException(type.getName()).click();
        LOG.info("В поле \"Тип дня\" выбрали \"%s\"", type.getName());
    }

    private LocalDate pickDateToCreateSpecialDay(OrgUnit unit) {
        Random random = new Random();
        List<BusinessHours> businessHours = BusinessHoursRepository.scheduleType(unit.getId());
        String link = businessHours.get(0).getSelfLink();
        String num = link.substring(link.lastIndexOf("/") + 1);
        JSONArray array = CommonRepository.getSpecialDaysArray(num);
        List<LocalDate> workingDays = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            LocalDate date = LocalDate.parse(array.getJSONObject(i).getString(DATE));
            workingDays.add(date);
        }
        List<LocalDate> daysOff = CommonRepository.getBusinessHoursDaysOff(num).stream()
                .map(LocalDate::parse)
                .collect(Collectors.toList());
        DateInterval dateInterval = new DateInterval(LocalDate.now().with(TemporalAdjusters.firstDayOfYear()),
                                                     LocalDate.now().with(TemporalAdjusters.lastDayOfYear()));
        List<LocalDate> includedDates = dateInterval.getBetweenDatesList();
        includedDates = ListUtils.subtract(includedDates, workingDays);
        includedDates = ListUtils.subtract(includedDates, daysOff);
        return includedDates.get(random.nextInt(includedDates.size()));
    }

    @Step("Проверить, что оргюниту \"{unit.name}\" был присвоен специальный день " +
            "c временем начала {interval.startDateTime} и временем окончания {interval.endDateTime}")
    private void assertSpecialDay(OrgUnit unit, DateTimeInterval interval) {
        LocalTime startTime = interval.getStartDateTime().toLocalTime();
        LocalTime endTime = interval.getEndDateTime().toLocalTime();
        List<BusinessHours> businessHours = BusinessHoursRepository.scheduleType(unit.getId());
        String link = businessHours.get(0).getSelfLink();
        String num = link.substring(link.lastIndexOf("/") + 1);
        JSONArray array = CommonRepository.getSpecialDaysArray(num);
        for (int i = 0; i < array.length(); i++) {
            LocalTime startAPITime = LocalTime.parse(array.getJSONObject(i).getJSONObject(TIME_INTERVAL).getString(START_TIME));
            LocalTime endAPITime = LocalTime.parse(array.getJSONObject(i).getJSONObject(TIME_INTERVAL).getString(END_TIME));
            String apiDate = array.getJSONObject(i).getString(DATE);
            if (apiDate.equals(interval.getStartDate().toString())) {
                SoftAssert softAssert = new SoftAssert();
                softAssert.assertEquals(startAPITime, startTime,
                                        String.format("Время начала в API - %s не совпало с ожидаемым %s", startAPITime, startTime));
                softAssert.assertEquals(endAPITime, endTime,
                                        String.format("Время окончания в API -  %s не совпало с ожидаемым %s", endAPITime, endTime));
                softAssert.assertAll();
                break;
            }
        }
    }

    private String getRandomParentUnitFromUi(List<String> unitsOfCertainType) {
        List<String> omUI = os.omInfoForm().listOfParentOM().stream().map(WebElement::getText).collect(Collectors.toList());
        omUI.retainAll(unitsOfCertainType);
        return getRandomFromList(omUI);
    }

    @Step("Проверить, что оргюниту \"{name}\" был присвоен выходной день на дату {date}")
    private void assertDayOff(String name, int omNumber, LocalDate date) {
        List<BusinessHours> businessHours = BusinessHoursRepository.scheduleType(omNumber);
        String activeSchedule = getActiveScheduleId().getLeft().toString();
        int num = businessHours.stream()
                .filter(bh -> bh.getType().equals(activeSchedule))
                .findAny().orElse(null)
                .getSelfId();
        List<String> datesDayOff = CommonRepository.getBusinessHoursDaysOff(String.valueOf(num));
        Assert.assertTrue(datesDayOff.contains(date.toString()),
                          String.format("Среди выходных оргюнита \"%s\" %s нет даты %s", name, datesDayOff, date));
        LOG.info("Оргюниту \"%s\" был присвоен выходной день на дату %s", name, date);
    }

    @Step("Нажать на кнопку раскрытия раздела \"{employeeInfoName.nameOfInformation}\"")
    private void clickOnShowButton(EmployeeInfoName employeeInfoName) {
        String name = employeeInfoName.getNameOfInformation();
        LOG.info("Нажимаем на кнопку раскрытия раздела \"{}\"", name);
        new Actions(os.getWrappedDriver()).moveToElement(os.osCardForm().showButton(name)).perform();
        os.osCardForm().showButton(name).click();
    }

    @Step("Нажать на значок карандаша раздела \"{employeeInfoName.nameOfInformation}\"")
    private void clickOnPencilButton(EmployeeInfoName employeeInfoName) {
        String name = employeeInfoName.getNameOfInformation();
        LOG.info("Нажимаем на значок карандаша раздела \"{}\"", name);
        new Actions(os.getWrappedDriver()).moveToElement(os.osCardForm().pencilButton(name)).perform();
        os.osCardForm().pencilButton(name).click();
    }

    @Step("Деактивировать отмеченные чекбоксы")
    private void clickOnAllActiveCheckBoxes() {
        List<AtlasWebElement> temp = new ArrayList<>(os.employeeData().activeCheckBoxes()
                                                             .waitUntil(Matchers.hasSize(Matchers.greaterThan(0))));
        temp.forEach(WebElement::click);
        Allure.addAttachment("Переключение",
                             String.format("Были деактивированы %d чекбокса", temp.size()));
    }

    @Step("Кликнуть на чекбокс \"требуется наставник\"")
    private void internCheckBoxClick() {
        new Actions(os.getWrappedDriver()).moveToElement(os.employeeData().internCheckBox()).perform();
        os.employeeData().internCheckBox().click();
        os.employeeData().mentorsListOpenButton()
                .waitUntil("Ожидание появления кнопки календаря", DisplayedMatcher.displayed(), 5);
    }

    @Step("Из раскрывающегося списка в поле \"Заместитель\" выбрать сотрудника с именем {name}")
    private void selectDeputyFromList(String name, int position) {
        os.employeeData().deputyListOpenButton(position).click();
        slowSendKeys(os.employeeData().deputySearchInput(), name);
        os.employeeData().firstDeputyInSearch().waitUntil(
                String.format("Не отобразилось имя %s в результатах поиска", name),
                DisplayedMatcher.displayed(), 30);
        os.employeeData().firstDeputyInSearch().click();
    }

    @Step("Раскрыть список в поле \"Наставник\", из раскрывшегося списка выбрать наставника")
    private void mentorSelect() {
        os.employeeData().mentorsListOpenButton().click();
        Random random = new Random();
        if (os.employeeData().mentorsList().size() != 0) {
            int size = os.employeeData().mentorsList().size();
            int empRnd = random.nextInt(size);
            LOG.info("Выбран наставник с именем {}", empRnd);
            os.employeeData().mentorsList().get(empRnd).click();
        }
    }

    @Step("Выбрать дату {date} окончания действия программы")
    private void selectInternProgramDateEnd(LocalDate date) {
        os.employeeData().internEndDateInput().sendKeys(date.format(UI.getFormat()));
    }

    @Step("Нажать на значок карандаша")
    private void changeEmp() {
        new Actions(os.getWrappedDriver()).moveToElement(os.osCardForm().empPencilButton()).perform();
        os.osCardForm().empPencilButton().click();
    }

    @Step("Нажать на стрелочку вниз в поле \"Сотрудник\"")
    private void clickOnSelectEmployeeChevron() {
        os.addNewEmployeeForm().selectEmployeeButton().click();
    }

    @Step("Нажать на кнопку \"Должности\" в фильтрах сотрудников")
    private void pressEmpPositionFilter() {
        os.osFilterForm().employeePositionButton().click();
        LOG.info("Нажали на кнопку \"Должности\" в фильтрах сотрудников");
    }

    @Step("Активировать чекбокс \"{position}\"")
    private void selectEmpPosition(String position) {
        LOG.info("Выбрана должность {}", position);
        os.filterEmpPositionForm().selectedEmployeePosition(position).click();
    }

    @Step("Нажать на копку \"Выбрать\" в форме фильтра сотрудников")
    private void pressOkEmpPositionForm() {
        os.filterEmpPositionForm().employeePositionOk().click();
        os.spinnerLoader().loadingForm().waitUntil("Спиннер все еще крутится",
                                                   Matchers.not(DisplayedMatcher.displayed()), 20);
        LOG.info("Нажали на копку \"Выбрать\" в форме фильтра сотрудников");
    }

    @Step("Нажать кнопку \"Подразделения\" в фильтрах Сотрудников")
    private void clickEmpOmFilter() {
        os.osFilterForm().employeeOMButton().click();
    }

    @Step("Нажать на кнопку {tab}")
    private void switchTo(String tab) {
        if (tab.equals("Сотрудники")) {
            os.osSwitchToTabs().empTab().click();
        } else {
            os.osSwitchToTabs().omTab().click();
        }
        os.osSearchForm().waitUntil("Page not loaded", Matchers.notNullValue(), 5);
    }

    @Step("Предварительно отключить фильтры по умолчанию")
    private void makeClearFilters() {
        while (os.osFilterForm().activeFilters().size() != 0) {
            os.osFilterForm().activeFilters().forEach(AtlasWebElement::click);
        }
        os.spinnerLoader().loadingForm().waitUntil("Спиннер все еще крутится",
                                                   Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Выбрать подразделение из результатов поиска")
    private void selectFromList() {
        os.osSearchForm()
                .waitUntil("Selected type of OM not found", DisplayedMatcher.displayed(), 5);
        os.osSearchForm().osPickFromList().click();
    }

    @Step("Ввести значение \"{nameToEnter}\" в строку поиска")
    private void searchTheModule(String nameToEnter, String displayedName) {
        os.osSearchForm().firstSearchResult()
                .waitUntil("Список оргюнитов не загрузился", DisplayedMatcher.displayed(), 10);
        if (!os.osSearchForm().firstSearchResult().getText().equals(displayedName)) {
            slowSendKeys(os.osSearchForm().orgUnitSearchInput(), nameToEnter);
            if (os.osSearchForm().allSearchResult().size() > 1) {
                os.osSearchForm().orgUnitSearchInput().sendKeys(Keys.ENTER);
            }
            os.omInfoForm().omName().waitUntil("", text(containsString(displayedName)), 15);
        }
        LOG.info("Перешли в оргюнит \"{}\"", os.omInfoForm().omName().getText());
    }

    /**
     * Ввести значение в строку поиска
     */
    private void searchTheModule(String name) {
        searchTheModule(name, name);
    }

    @Step("Нажать на значок \"Карандаша\" для редактирования основной информации ОргЮнита")
    private void pressMainDataPencil() {
        /*добавлено потому что панель оргюнита справа не успевает прогружаться, количество элементов у оргюнитов разное,
         как и количество полей, зацепится за них не получится, нужно ждать пока прогрузится последнее
         */
        systemSleep(1); //панель оргюнита справа не успевает прогружаться
        os.omInfoForm().editingButton()
                .waitUntil("Кнопка редактирования не отобразилась", DisplayedMatcher.displayed(), 5);
        os.omInfoForm().editingButton().click();
    }

    @Step("Нажать на значок карандаша раздела \"{omInfoName.nameOfInformation}\"")
    private void pressPencilAtOm(OmInfoName omInfoName) {
        String name = omInfoName.getNamesOfInformation();
        LOG.info("Нажали на значок карандаша раздела \"{}\"", name);
        os.omInfoForm()
                .waitUntil("Карточка оргюнита не загрузилась",
                           DisplayedMatcher.displayed(), 5);
        new Actions(os.getWrappedDriver()).moveToElement(os.omInfoForm().editButton(name)).perform();
        os.omInfoForm().editButton(name).click();
    }

    @Step("Ввести значение \"{omName}\" в поле \"Название\"")
    private void changeOmName(String omName) {
        os.omEditingForm().omFieldInput(OrgUnitInputs.OM_NAME.getFieldType()).clear();
        os.omEditingForm().omFieldInput(OrgUnitInputs.OM_NAME.getFieldType()).sendKeys(omName);
    }

    @Step("Ввести значение \"{omName}\" в поле \"OuterId\"")
    private void changeOmsOuterId(String omName) {
        os.omEditingForm().omFieldInput(OrgUnitInputs.OUTER_ID.getFieldType())
                .should("Поле редактирование outerId не было отображено", DisplayedMatcher.displayed());
        os.omEditingForm().omFieldInput(OrgUnitInputs.OUTER_ID.getFieldType()).clear();
        os.omEditingForm().omFieldInput(OrgUnitInputs.OUTER_ID.getFieldType()).sendKeys(omName);
    }

    @Step("Выбрать тип оргЮнита \"{omType}\"")
    private void changeOmType(String omType, String typeBefore) {
        os.omEditingForm().omFieldInput(OrgUnitInputs.TYPE.getFieldType()).click();
        os.omEditingForm().omTypeButton(omType).click();
        Allure.addAttachment("Выбор типа оргюнита",
                             String.format("Выбран тип оргюнита \"%s\" до этого был \"%s\"", omType, typeBefore));
    }

    @Step("Нажать на кнопку \"{inputs}\"")
    private void clickOnChangingOmButton(OrgUnitInputs inputs) {
        os.omEditingForm().omFieldInput(inputs.getFieldType()).click();
    }

    @Step("Выбрать родительское подразделение с именем {omParent}")
    private void selectParentOM(String omParent, OrgUnit lastParent) {
        os.omInfoForm().parentFromDropDownMenu(omParent)
                .waitUntil("Can't press the parent module", DisplayedMatcher.displayed(), 30);
        os.omInfoForm().parentFromDropDownMenu(omParent).click();
        Allure.addAttachment("Выбор родительского подразделения",
                             String.format("Выбрано родительское подразделение \"%s\" до этого был \"%s\"", omParent, lastParent.getName()));
    }

    @Step("Выбрать случайного заместителя из списка")
    private String changeDeputyEmployee() {
        os.omEditingForm().omFieldInput(OrgUnitInputs.DEPUTY.getFieldType()).click();
        ElementsCollection<AtlasWebElement> listDeputy = os.omEditingForm().omDeputyEmployeeList();
        Assert.assertTrue(listDeputy.size() > 0, "Список заместителей пустой");
        AtlasWebElement randomDeputy = getRandomFromList(listDeputy);
        String deputyName = randomDeputy.getText();
        randomDeputy.click();
        LOG.info("Выбран заместитель по имени {}", deputyName);
        Allure.addAttachment("Выбор заместителя", "Выбран заместитель по имени " + deputyName);
        return deputyName;
    }

    @Step("Ввести дату {date} в поле {dateType.name}")
    private void changeOmDate(DateTypeField dateType, LocalDate date) {
        os.omEditingForm().omDateInput(dateType.getName()).clear();
        os.omEditingForm().omDateInput(dateType.getName()).sendKeys(date.format(UI_DOTS.getFormat()));
    }

    @Step("Ввести число {wrongDate} в поле \"{dateType.name}\" и нажать \"Enter\"")
    private void changeOmDate(DateTypeField dateType, String wrongDate) {
        os.omEditingForm().omDateInput(dateType.getName()).clear();
        os.omEditingForm().omDateInput(dateType.getName()).sendKeys(wrongDate);
        os.omEditingForm().omDateInput(dateType.getName()).sendKeys(Keys.ENTER);
    }

    @Step("Проверить, что форма не закрылась и появилось сообщение об ошибке")
    private void assertThatForIncorrectEmployeeAddition(LocalDate input) {
        os.addNewEmployeeForm().saveButton().click();
        os.addNewEmployeeForm().should("Форма заполнения нового сотрудника пропала", DisplayedMatcher.displayed());
        Allure.addAttachment("Сообщение", "Отобразилось сообщение об ошибке: "
                + os.addNewEmployeeForm().alertDate(DateTypeField.END_JOB.getName()).getText());
        os.addNewEmployeeForm().alertDate(DateTypeField.END_JOB.getName())
                .should(text(containsString(String.format("Должна быть не ранее, чем %s 24:00:00",
                                                          input.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))));
    }

    @Step("Проверка присвоения сотруднику {empName} должности {variantsOfJobs} даты начала работе " +
            "{startDateJob} и даты окончания работы {endDateJob}, а так же дата начала должности {startDatePosition}")
    private void assertForEmployeePosition(JobTitle jobTitle, LocalDate startDateJob, LocalDate startDatePosition,
                                           String empName, int omNumber) {
        String positionName = jobTitle.getFullName();
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(omNumber, LocalDateTools.getLastDate(), false);
        EmployeePosition employeePosition = employeePositions.stream()
                .filter(ep -> ep.getEmployee().getFullName().equals(empName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        String.format("schedule message. Сотрудник с именем %s не найден в апи", empName)));
        String apiPositionName = employeePosition.getPosition().getName();
        LocalDate apiStartPositionDate = employeePosition.getPosition().getDateInterval().startDate;
        LocalDate apiStartJobDate = employeePosition.getDateInterval().startDate;
        Allure.addAttachment("Сотрудник", "text/plain",
                             String.format("Сотруднику %s была присвоена должность %s ,а в API - %s\n," +
                                                   "была присвоена дата начала работы %s, а в API - %s,\n " +
                                                   "была присвоена дата начала должности %s, а в API - %s",
                                           empName, jobTitle.getFullName(), apiPositionName,
                                           startDateJob, apiStartPositionDate,
                                           startDatePosition, apiStartJobDate));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(positionName, apiPositionName, "Название должностей не совпадает");
        softAssert.assertEquals(startDatePosition, apiStartPositionDate, "Даты начала позиций не совпадают");
        softAssert.assertEquals(startDateJob, apiStartJobDate, "Даты начала работы не совпадают");
        softAssert.assertAll();
    }

    private LocalDate getValueInputDate() {
        String dateValue = os.omEditingForm().omDateInput(DateTypeField.OPEN_DATE.getName()).getAttribute("value");
        return !dateValue.isEmpty() ? LocalDate.parse(dateValue, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                : null;
    }

    @Step("Нажать на чекбокс \"Участвует в расчете\"")
    private void clickCalculationFlag() {
        os.omInfoForm().availableForCalculation().should(CHECKBOX_NOT_DISPLAYED, DisplayedMatcher.displayed());
        os.omInfoForm().availableForCalculation().click();
    }

    @Step("Нажать на кнопку \"+1\"в правой части текстового поля \"Теги\"")
    private void clickAddTags() {
        os.omEditingForm().tagAddOne().click();
    }

    @Step("Ввести тег \"{tagName}\"")
    private void enterTags(String tagName) {
        os.omEditingForm().tagSpace().click();
        os.omEditingForm().tagSpace().sendKeys(tagName);
    }

    @Step("Кликнуть на поле \"Теги\"")
    private void clickTags() {
        os.omEditingForm().tagSpace().click();
    }

    @Step("Кликнуть на тег \"{tagName}\" в выпадающем списке")
    private void clickEnteredTag(String tagName) {
        os.omEditingForm().tagInDropdownList(tagName)
                .waitUntil(String.format("Тег %s не отобразился в выпадающем списке", tagName), DisplayedMatcher.displayed(), 5);
        os.omEditingForm().tagInDropdownList(tagName).click();
    }

    @Step("Нажать на кнопку \"Отменить\" в режиме редактировании информации оргЮнита")
    private void pressDismissChangeRedMain() {
        os.omEditingForm().cancelButton().click();
    }

    private String editContactField(ContactFields contactField, boolean isEmployee) {
        switch (contactField) {
            case PHONE_NUMBER:
            case PHONE_TYPE:
                String phoneNumber = "7" + RandomStringUtils.randomNumeric(10);
                String phoneType = PhoneTypes.ANOTHER.getPhoneName();
                enterPhoneNumber(phoneNumber);
                selectPhoneType(phoneType);
                return phoneType + " " + phoneNumber;
            case EMAIL:
                String email = RandomStringUtils.randomAlphanumeric(7) + "@gmail.com";
                enterEmail(email);
                return email;
            case FAX:
                if (!isEmployee) {
                    String faxNumber = "7" + RandomStringUtils.randomNumeric(10);
                    enterFaxNumber(faxNumber);
                    return faxNumber;
                }
            case POSTAL_CODE:
            case REGION:
            case CITY:
            case NOTE:
            case ADDRESS_TYPE:
            case COUNTRY:
            case ADDRESS_STREET:
            case ADDRESS_BUILDING:
                List<String> address = new ArrayList<>();
                address.add(enterAddressType(isEmployee));
                address.add(RandomStringUtils.randomNumeric(6));
                address.add("Россия");
                address.add("Москва");
                address.add("Москва");
                address.add("Арбатская");
                address.add(RandomStringUtils.randomNumeric(2));
                enterOmIndex(address.get(1));
                enterOmCountry(address.get(2));
                enterOmRegion(address.get(3));
                enterOmCity(address.get(4));
                enterOmStreet(address.get(5));
                enterOmBuilding(address.get(6));
                return address.stream().collect(Collectors.joining(", "));
        }
        throw new AssertionError("Выбранного поля не существует");
    }

    private void assertContactEdited(int id, ContactFields contactField, String value, boolean isEmployee) {
        switch (contactField) {
            case EMAIL:
                assertEmailEdited(id, value, isEmployee);
                break;
            case PHONE_TYPE:
            case PHONE_NUMBER:
                assertPhoneEdited(id, value, isEmployee);
                break;
            case FAX:
                if (!isEmployee) {
                    assertFaxEdited(id, value);
                    break;
                }
            case POSTAL_CODE:
            case REGION:
            case CITY:
            case NOTE:
            case ADDRESS_TYPE:
            case COUNTRY:
            case ADDRESS_STREET:
            case ADDRESS_BUILDING:
                assertAddressEdited(id, value, isEmployee);
                break;
        }
    }

    @Step("Ввести номер телефона {phoneNumber}")
    private void enterPhoneNumber(String phoneNumber) {
        if (os.omInfoForm().phoneDelete().isDisplayed()) {
            os.omInfoForm().phoneDelete().click();
        }
        //todo цифры могут ввестись в случайном порядке (только через код)
        String number = String.format("+%s(%s) %s-%s", phoneNumber.charAt(0), phoneNumber.substring(1, 4), phoneNumber.substring(4, 7),
                                      phoneNumber.substring(7, 11));
        slowSendKeys(os.osCardForm().contactsFieldByType(ContactFields.PHONE_NUMBER.getFieldName()), number);
    }

    @Step("Ввести номер факса {faxNumber}")
    private void enterFaxNumber(String faxNumber) {
        //todo цифры могут ввестись в случайном порядке (только через код)
        String number = String.format("+%s(%s) %s-%s", faxNumber.charAt(0), faxNumber.substring(1, 4), faxNumber.substring(4, 7),
                                      faxNumber.substring(7, 11));
        os.osCardForm().contactsFieldByType(ContactFields.FAX.getFieldName()).sendKeys(number);
    }

    @Step("Выбрать случайный тип телефона")
    private void selectPhoneType() {
        os.osCardForm().contactsFieldByType(ContactFields.PHONE_TYPE.getFieldName()).click();
        os.omInfoForm().phoneTypeButton((PhoneTypes.values()[new Random().nextInt(PhoneTypes.values().length)]).getPhoneName()).click();
    }

    @Step("Выбрать определённый тип телефона")
    private void selectPhoneType(String phoneTypes) {
        os.osCardForm().contactsFieldByType(ContactFields.PHONE_TYPE.getFieldName()).click();
        //без этого ожидания выбирается не тот тип
        systemSleep(1);
        os.omInfoForm().phoneTypeButton(phoneTypes).click();
    }

    @Step("Ввести адрес электронной почты \"{email}\"")
    private void enterEmail(String email) {
        os.osCardForm().contactsFieldByType(ContactFields.EMAIL.getFieldName()).clear();
        os.osCardForm().contactsFieldByType(ContactFields.EMAIL.getFieldName()).click();
        os.osCardForm().contactsFieldByType(ContactFields.EMAIL.getFieldName()).sendKeys(email);
        LOG.info("Изменили адрес электронной почты на {}", email);
    }

    @Step("Очистить поле адреса электронной почты")
    private void clearEmailField() {
        os.osCardForm().contactsFieldByType(ContactFields.EMAIL.getFieldName()).clear();
    }

    @Step("Проверить изменение e-mail на {mail}")
    private void checkEmailMatches(String mail, Employee employees) {
        SoftAssert softAssert = new SoftAssert();
        String uiAddress = os.employeeData().emailField().getText();
        String apiAddress = employees.refreshEmployee().getEmail();
        softAssert.assertEquals(apiAddress, mail, "Адрес почты не совпал в api");
        softAssert.assertEquals(mail, uiAddress, "Адрес почты не совпал на ui");
        Allure.addAttachment("Проверка",
                             String.format("Адрес, который отобразился в карточке: %s, адрес в api: %s",
                                           uiAddress, apiAddress));
        softAssert.assertAll();
    }

    @Step("Выбрать случайный тип адреса")
    private String enterAddressType(boolean isEmployee) {
        os.osCardForm().contactsFieldByType(ContactFields.ADDRESS_TYPE.getFieldName()).click();
        String addressType = isEmployee ? (EmpAddressType.values()[new Random().nextInt(EmpAddressType.values().length)]).getAddressName()
                : (AddressType.values()[new Random().nextInt(AddressType.values().length)]).getAddressName();
        waitForClickable(os.osCardForm().addressTypeButton(addressType), os, 5);
        os.osCardForm().addressTypeButton(addressType).click();
        return addressType;
    }

    @Step("Ввести почтовый индекс {index} ")
    private void enterOmIndex(String index) {
        os.osCardForm().contactsFieldByType(ContactFields.POSTAL_CODE.getFieldName()).sendKeys(index);
    }

    @Step("Ввести значение \"{country}\" в поле \"Страна\"")
    private void enterOmCountry(String country) {
        os.osCardForm().contactsFieldByType(ContactFields.COUNTRY.getFieldName()).sendKeys(country);
    }

    @Step("Ввести значение \"{region}\" в поле \"Область или край\"")
    private void enterOmRegion(String region) {
        os.osCardForm().contactsFieldByType(ContactFields.REGION.getFieldName()).sendKeys(region);
    }

    @Step("Ввести значение \"{city}\" в поле \"Город\"")
    private void enterOmCity(String city) {
        os.osCardForm().contactsFieldByType(ContactFields.CITY.getFieldName()).sendKeys(city);
    }

    @Step("Ввести значение \"{street}\" в поле \"Улица\"")
    private void enterOmStreet(String street) {
        os.osCardForm().contactsFieldByType(ContactFields.ADDRESS_STREET.getFieldName()).sendKeys(street);
    }

    @Step("Ввести значение \"{building}\" в поле \"Строение\"")
    private void enterOmBuilding(String building) {
        os.osCardForm().contactsFieldByType(ContactFields.ADDRESS_BUILDING.getFieldName()).sendKeys(building);
    }

    @Step("Ввести значение \"{note} в поле \"Примечание\"")
    private void enterNote(String note) {
        os.osCardForm().contactsFieldByType(ContactFields.NOTE.getFieldName()).sendKeys(note);
    }

    private List<String> getCurrentEmployeesNames() {
        if (os.omInfoForm().employeesNames().size() == 0) {
            return new ArrayList<>();
        }
        return os.omInfoForm().employeesNames().stream().map(WebElement::getText).collect(Collectors.toList());
    }

    @Step("В раскрывшемся меню выбрать \"Редактировать\"")
    private void editButtonClick() {
        LOG.info("В раскрывшемся меню выбираем \"Редактировать\"");
        os.omInfoForm().employeeEditButton()
                .should(EDIT_BUTTON_NOT_DISPLAYED, DisplayedMatcher.displayed());
        new Actions(os.getWrappedDriver()).moveToElement(os.omInfoForm().employeeEditButton()).perform();
        os.omInfoForm().employeeEditButton().click();
        os.addNewEmployeeForm()
                .waitUntil("Ожидание формы редактирования сотрудника", DisplayedMatcher.displayed(), 30);
    }

    @Step("В раскрывшемся меню выбрать \"{employeeVariant.name}\"")
    private void chooseEmployeeFunction(String name, EmployeeVariants employeeVariant) {
        os.omInfoForm().employeeButton(name, employeeVariant.getVariant())
                .should(String.format("Кнопка %s  отсутсвует", employeeVariant.getVariant()), DisplayedMatcher.displayed());
        new Actions(os.getWrappedDriver()).moveToElement(os.omInfoForm().employeeButton(name, employeeVariant.getVariant())).perform();
        os.omInfoForm().employeeButton(name, employeeVariant.getVariant()).click();
        LOG.info("В раскрывшемся меню сотрудника {} выбираем \"{}\"", name, employeeVariant.getName());
    }

    @Step("Нажать на кнопку \"Сохранить\" в форме добавления должности")
    private void saveButtonClick(boolean withWait) {
        LOG.info("Нажимаем на кнопку \"Сохранить\" в форме добавления должности");
        os.addNewEmployeeForm().saveButton()
                .waitUntil("Кнопка \"Сохранить не загрузилась\"", DisplayedMatcher.displayed(), 10);
        os.addNewEmployeeForm().saveButton().sendKeys(Keys.TAB);
        os.addNewEmployeeForm().saveButton().sendKeys(Keys.ENTER);
        inputAdditionalPositionInfo();
        if (withWait) {
            os.addNewEmployeeForm().waitUntil("Форма редактирования сотрудника не закрылась",
                                              Matchers.not(DisplayedMatcher.displayed()), 25);
            os.spinnerLoader().grayLoadingBackground().waitUntil("Спиннер всё ещё отображается", Matchers.not(DisplayedMatcher.displayed()), 30);
        }
    }

    @Step("Нажать на кнопку \"Отменить\" в форме добавления должности")
    private void cancelButtonClick() {
        LOG.info("Нажимаем на кнопку \"Отменить\" в форме добавления должности");
        os.addNewEmployeeForm().cancelButton()
                .waitUntil("Кнопка \"Отменить не загрузилась\"", DisplayedMatcher.displayed(), 10);
        os.addNewEmployeeForm().cancelButton().click();
    }

    private void saveButtonClick() {
        saveButtonClick(true);
    }

    private void saveButtonWithoutWait() {
        saveButtonClick(false);
    }

    @Step("Проверить, что сотруднику была добавлена позиция")
    private void assertPositionAdding(PositionGroup role, int orgUnitId, List<EmployeePosition> before, Employee employee) {
        List<EmployeePosition> after = EmployeePositionRepository.getEmployeePositions(PositionRepository.getPositionsArray(orgUnitId));
        SoftAssert softAssert = new SoftAssert();
        List<String> omEmployeesNames = getCurrentEmployeesNames();
        softAssert.assertTrue(omEmployeesNames.contains(employee.getFullName()),
                              "Сотрудник к котрому была добавлена позиции не отображается в списке");
        softAssert.assertEquals(after.size(), before.size() + 1, "Позиция не добавилась");
        after.stream().filter(employeePosition -> employeePosition.getEmployee().equals(employee))
                .filter(employeePosition -> employeePosition.getPosition().getPositionGroupId() == role.getId()
                        || employeePosition.getPosition().getName().equals(role.getName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "В списке сотрудников не нашли добавленную позицию"));
        softAssert.assertAll();
    }

    @Step("Проверить, что сотруднику была добавлена позиция")
    private void assertPositionAdding(PositionGroup role, int orgUnitId, DateInterval dateInterval,
                                      Employee employeeBefore, JobTitle jobTitle) {
        SoftAssert softAssert = new SoftAssert();
        os.omInfoForm().posJob(employeeBefore.getFullName()).should(text(containsString(jobTitle.getFullName())));
        EmployeePosition employeePosition = EmployeePositionRepository.getEmployeePosition(employeeBefore.getFullName(), dateInterval, orgUnitId);
        Position position = employeePosition.getPosition();
        Employee employee = employeePosition.getEmployee();
        if (role != null) {
            softAssert.assertEquals(employeePosition.getPosition().getPositionGroupId(),
                                    role.getId(), "Функциональные роли не совпали");
        }
        softAssert.assertEquals(orgUnitId,
                                employeePosition.getOrgUnit().getId(), "Оргюнит сотрудника не совпал с текущим");
        softAssert.assertEquals(employeePosition.getEmployee().getId(),
                                employee.getId(), "Добавленный на должность сотрудник имеет другой айди");
        softAssert.assertTrue(employeePosition.getDateInterval().equals(dateInterval),
                              String.format("У добавленного сотрудника были даты: %s\nВ тесте выбирали: %s",
                                            employeePosition.getDateInterval(), dateInterval));
        softAssert.assertEquals(position.getName(),
                                jobTitle.getFullName(), "Названия должностей не совпали");
        Allure.addAttachment("Проверка",
                             String.format("Был создан сотрудник по имени %s\n с датами начала и окончания работы %s %s\n и должностью %s",
                                           employee.getFullName(), employee.getStartWorkDate(), employee.getEndWorkDate(), position.getName()));
        softAssert.assertAll();
    }

    @Step("Проверить, что сотрудник стал руководителем")
    private void assertChiefAdding(int orgUnitId, EmployeePosition employeePosition) {
        Position chief = PositionRepository.getChief(orgUnitId);
        os.omInfoForm().omManagerName().should("Должность руководителя не была назначена сотруднику",
                                               text(containsString(employeePosition.getEmployee().getFullName())), 15);
        Assert.assertEquals(chief.getId(), employeePosition.getPosition().getId(),
                            "Сотрудник не был назначен на должность руководителя\n" +
                                    "Его айди не совпал с айди текущего руководителя");
        Allure.addAttachment("Проверка", String.format("Сотрудник с именем %s стал руководителем", employeePosition));
    }

    @Step("Проверить, что сотруднику была назначена функциональная роль")
    private void assertRoleSelect(EmployeePosition employeePosition, PositionGroup role) {
        Assert.assertEquals(employeePosition.getPosition().refreshPositions().getPositionGroupId(),
                            role.getId(), "Функциональная роль не совпала");
        Allure.addAttachment("Проверка", String.format("Сотрудник с именем %s стал руководителем", employeePosition));
    }

    @Step("Проверить, что был добавлен заместитель")
    private void assertDeputyAdding(User toWhom, Employee added, DateInterval dateInterval, int size, boolean isAdded) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"));
        os.employeeData().deputyDateIntervalByName(added.getFullName())
                .should(text(containsString(dateInterval.startDate.format(formatter) + " — " + dateInterval.endDate.format(formatter))));
        SoftAssert softAssert = new SoftAssert();
        User update = new User(getJsonFromUri(Projects.WFM, URI.create(toWhom.getLink(SELF))));
        User.RoleInUser updateRole = update.getRoles().get(0);
        List<UserDeputy> userDeputies = getUserDeputies(update);
        softAssert.assertTrue(isAdded ^ userDeputies.size() - size > 0, "Заместитель не добавлися/при изменении добавился");
        //Проверка что совпадает дата и сотрудник
        softAssert.assertTrue(userDeputies.stream().anyMatch(userDeputy ->
                                                                     userDeputy.getDateInterval().equals(dateInterval) &&
                                                                             new Employee(getJsonFromUri(Projects.WFM, URI.create(userDeputy.getEmployee().getSelfLink()))).equals(added)
        ), String.format("Добавленный сотрудник %s не был отображен в api в качестве заместителя", added));
        int roleId = updateRole.getUserRoleId();
        //Проверка что наследует оргюниты
        softAssert.assertTrue(getById(added.getUser().getRoles(), roleId).equals(updateRole),
                              "Роли у изначального сотрудника не были унаследованы " + added);
        softAssert.assertAll();
    }

    @Step("Проверить, что были добавлены два заместителя")
    private void assertTwoDeputiesAdding(User toWhom, Employee addedFirst, Employee addedSecond, DateInterval dateIntervalFirst,
                                         DateInterval dateIntervalSecond) {
        Locale localeRu = new Locale("ru", "RU");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy").withLocale(localeRu);
        os.employeeData().deputyDateIntervalByName(addedFirst.getFullName())
                .should(text(containsString(dateIntervalFirst.startDate.format(formatter) + " — " + dateIntervalFirst.endDate.format(formatter))));
        os.employeeData().deputyDateIntervalByName(addedSecond.getFullName())
                .should(text(containsString(dateIntervalSecond.startDate.format(formatter) + " — " + dateIntervalSecond.endDate.format(formatter))));
        SoftAssert softAssert = new SoftAssert();
        User update = new User(getJsonFromUri(Projects.WFM, URI.create(toWhom.getLink(SELF))));
        User.RoleInUser updateRole = update.getRoles().get(0);
        List<UserDeputy> userDeputies = getUserDeputies(update);
        //Проверка что совпадает дата и сотрудник
        softAssert.assertTrue(userDeputies.stream().anyMatch(userDeputy ->
                                                                     userDeputy.getDateInterval().equals(dateIntervalFirst) &&
                                                                             new Employee(getJsonFromUri(Projects.WFM, URI.create(userDeputy.getEmployee().getSelfLink()))).equals(addedFirst)),
                              String.format("Добавленный сотрудник %s не был отображен в api в качестве заместителя", addedFirst.getFullName()));
        softAssert.assertTrue(userDeputies.stream().anyMatch(userDeputy ->
                                                                     userDeputy.getDateInterval().equals(dateIntervalSecond) &&
                                                                             new Employee(getJsonFromUri(Projects.WFM, URI.create(userDeputy.getEmployee().getSelfLink()))).equals(addedSecond)
        ), String.format("Добавленный сотрудник %s не был отображен в api в качестве заместителя", addedFirst));
        int roleId = updateRole.getUserRoleId();
        //Проверка что наследует оргюниты
        softAssert.assertTrue(getById(addedFirst.getUser().getRoles(), roleId).equals(updateRole),
                              "Роли у изначального сотрудника не были унаследованы " + addedFirst);
        softAssert.assertTrue(getById(addedSecond.getUser().getRoles(), roleId).equals(updateRole),
                              "Роли у изначального сотрудника не были унаследованы " + addedSecond);
        softAssert.assertAll();
    }

    @Step("Сравнили две мапы, до {before} сохранения и после {after}")
    private void assertionCompareMaps(List<Position> before, JobTitle jobTitle, PositionGroup role, LocalDate startDate, int id) {
        systemSleep(1);//todo времени, пока крутится спинер, должно быть достаточно для загрузки данных в апи (в методе saveButtonClick() имеется явное ожидание для спинера)
        List<Position> after = PositionRepository.emptyPositionReturner(jobTitle, startDate, id);
        after.removeAll(before);
        if (after.size() == 0) {
            Assert.fail("Роль не была добавлена");
        }
        Position position = after.iterator().next();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(position.getPositionGroupId(), role.getId(), "Функциональные роли не совпали");
        softAssert.assertEquals(after.size(), 1, "В апи нет изменений");
        softAssert.assertAll();
    }

    @Step("Нажать на кнопку \"Изменить\"")
    private void clickOnChangeButton(EmployeeInfoName infoName) {
        String name = infoName.getNameOfInformation();
        os.osCardForm().changeButton(name).click();
        os.spinnerLoader().grayLoadingBackground().waitUntil("Загрузка всё еще идет", Matchers.not(DisplayedMatcher.displayed()), 30);
        LOG.info("Нажимаем на кнопку \"Изменить\"");
    }

    @Step("Нажать на кнопку \"Изменить\"")
    private void clickOnChangeButtonOutWait(EmployeeInfoName infoName) {
        String name = infoName.getNameOfInformation();
        new Actions(os.getWrappedDriver()).moveToElement(os.osCardForm().changeButton(name)).perform();
        os.osCardForm().changeButton(name).sendKeys(Keys.TAB);
        os.osCardForm().changeButton(name).sendKeys(Keys.ENTER);
        LOG.info("Нажимаем на кнопку \"Изменить\"");
    }

    @Step("Нажать на кнопку \"Отменить\"")
    private void clickOnCancelButton(EmployeeInfoName infoName) {
        String name = infoName.getNameOfInformation();
        new Actions(os.getWrappedDriver()).moveToElement(os.osCardForm().cancelButton(name)).perform();
        os.osCardForm().cancelButton(name).click();
        os.osCardForm().pencilButton(name).waitUntil("Загрузка всё еще идет",
                                                     DisplayedMatcher.displayed(), 20);
        LOG.info("Нажимаем на кнопку \"Отменить\"");

    }

    @Step("Нажать на значок корзины рядом с полем \"Заместитель\"")
    private void clickOnDeleteDeputyButton() {
        os.employeeData().deleteDeputyButton().click();
    }

    @Step("Проверить, что заместитель был удален")
    private void assertThatDeputyWasRemoved(List<UserDeputy> before, User user) {
        os.employeeData().noDataField().should("Список заместителей оказался не пустым", DisplayedMatcher.displayed());
        List<UserDeputy> after = getUserDeputies(user);
        before.removeAll(after);
        User update = new User(getJsonFromUri(Projects.WFM, user.getLink(SELF)));
        User.RoleInUser updateRole = update.getRoles().get(0);
        int roleId = updateRole.getUserRoleId();
        try {
            getById(before.get(0).getEmployee().getUser().getRoles(), roleId);
            Assert.fail("Сотрудник унаследовал роли удаленного заместителя");
        } catch (AssertionError ignored) {
        }
        Assert.assertEquals(before.size(), 1);
    }

    @Step("Проверить, что заместитель не был удален")
    private void assertThatDeputyWasNotRemoved(List<UserDeputy> before, User user) {
        os.employeeData().noDataField().should("Список заместителей оказался пустым",
                                               Matchers.not(DisplayedMatcher.displayed()), 2);
        List<UserDeputy> after = getUserDeputies(user);
        before.removeAll(after);
        Assert.assertEquals(before.size(), 0);
    }

    @Step("Проверить, что заместитель не был добавлен")
    private void assertThatDeputyWasNotAdded(List<UserDeputy> before, User user) {
        os.employeeData().noDataField().should("Список заместителей оказался не пустым",
                                               DisplayedMatcher.displayed());
        List<UserDeputy> after = getUserDeputies(user);
        before.removeAll(after);
        Assert.assertEquals(before.size(), 0);
    }

    @Step("Проверить, что Форма редактирования не закрывается. Поля подсвечиваются красны, отображется сообщение об ошибке")
    private void checkBothFieldsErrorException() {
        os.employeeData().errorField(DateTypeField.DEPUTY_START_DATE.getName())
                .should("Сообщение об ошибке не было отображено, или оно не свопало с ожидаемым",
                        text(containsString("Поле не может быть пустым")));
        os.employeeData().errorField(DateTypeField.DEPUTY_END_DATE.getName())
                .should("Сообщение об ошибке не было отображено, или оно не свопало с ожидаемым",
                        text(containsString("Поле не может быть пустым")));
    }

    @Step("Проверить, что форма редактирование не закрылась, поле \"Дата окончания замещения\" подсвечивается красным," +
            " появилось сообщение об ошибке")
    private void checkDateEndErrorException() {
        os.employeeData().errorField(DateTypeField.DEPUTY_END_DATE.getName())
                .should("Сообщение об ошибке не было отображено, или оно не свопало с ожидаемым",
                        text(containsString(String.format("Должна быть не ранее, чем %s %s",
                                                          LocalDate.now(), LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))))));
    }

    @Step("Проверить удаление навыков")
    private void checkSkillsDeletion(int previouslySize, int id) {
        os.employeeData().changeButton(EmployeeInfoName.SKILLS.getNameOfInformation())
                .waitUntil("Кнопка все еще отображена", Matchers.not(DisplayedMatcher.displayed()), 5);
        os.employeeData().skillsNamesField()
                .should("Поле с названием навыков все еще отображено", Matchers.not(DisplayedMatcher.displayed()));
        int newSizeApi = CommonRepository.checkValueOfSkill(id);
        String attachment;
        if (previouslySize == 1) {
            attachment = "Был успешно снят один навык с сотрудника.";
        } else {
            attachment = String.format("Были успешно сняты %d навыка.", previouslySize);
        }
        Allure.addAttachment("Проверка", attachment);
        Assert.assertEquals(newSizeApi, 0, "Навыки не удалены");
    }

    @Step("Проверить добавление всех трёх навыков. До добавления было {previouslySize} навыков")
    private void checkAllSkillsAdding(int previouslySize, int employeeId) {
        os.employeeData().changeButton(EmployeeInfoName.SKILLS.getNameOfInformation())
                .waitUntil("Кнопка все еще отображена", Matchers.not(DisplayedMatcher.displayed()), 5);
        List<String> uiSkills = os.employeeData().skillsNamesField().stream()
                .map(AtlasWebElement::getText).collect(Collectors.toList());
        List<String> neededSkills = new ArrayList<>();
        for (EmployeeSkills skills : EmployeeSkills.values()) {
            neededSkills.add(skills.getName());
        }
        int newSize = CommonRepository.checkValueOfSkill(employeeId);
        Allure.addAttachment("Проверка в API",
                             String.format("До добавления у сотрудника было %d навыков, после стало %d навыков", previouslySize, newSize));
        Allure.addAttachment("Проверка на UI",
                             String.format("На UI отображены следующие навыки у сотрудника: %s\nДолжны быть: %s", uiSkills, neededSkills));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(newSize - previouslySize, 3, "Количество навыков не совпадает");
        softAssert.assertEquals(neededSkills, uiSkills, "Навыки не совпадают");
        softAssert.assertAll();
    }

    @Step("Проверить добавление одного навыка")
    private void checkOneSkillAdding(int previouslySize, int id) {
        os.employeeData().changeButton(EmployeeInfoName.SKILLS.getNameOfInformation())
                .waitUntil("Кнопка все еще отображена", Matchers.not(DisplayedMatcher.displayed()), 5);
        int newSizeUi = os.employeeData().skillsNamesField().size();
        int newSizeApi = CommonRepository.checkValueOfSkill(id);
        Allure.addAttachment("Проверка",
                             String.format("Был добавлен один навык выбранному сотруднику; до добавления навыков было %d, после добавления стало %d",
                                           previouslySize, newSizeApi));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(newSizeApi - previouslySize, 1, "Навык не добавлен");
        softAssert.assertEquals(newSizeUi - previouslySize, 1, "Навык не добавлен");
        softAssert.assertAll();
    }

    private void chooseJob(JobTitle jobTitle) {
        openJobsList();
        systemSleep(1); //иногда кликает не туда
        selectJob(jobTitle);
    }

    @Step("Нажать на стрелку вниз в поле \"Название должности\"")
    private void openJobsList() {
        os.addNewEmployeeForm().inputJobCategory().click();
    }

    @Step("В раскрывшемся списке выбрать \"{jobTitle.fullName}\"")
    private void selectJob(JobTitle jobTitle) {
        os.addNewEmployeeForm().jobTitle(jobTitle.getFullName()).click();
        LOG.info("Была присвоена должность {}", jobTitle.getFullName());
    }

    @Step("Выбрать сотрудника, не работающего в этом подразделении. Выставить у него параметры (если не выставлены).")
    private void doActionsWithEmployee(Employee employee, LocalDate date, PositionGroup role) {
        selectAnyEmployee(employee.getFullName());
        chooseDatePositionForm(date, DateTypeField.START_JOB);
        selectFunctionalRole(role);
        Allure.addAttachment("Действия с сотрудником",
                             String.format("Был выбран сотрудник с именем: %s\n" +
                                                   "У сотрудника была выставлена дата и функциональная роль, которая соответствует категории должности",
                                           employee));
    }

    @Step("Выбрать сотрудника, не работающего в этом подразделении")
    private void chooseEmployeeDoesNotWorkHere(Employee employee) {
        selectAnyEmployee(employee.getFullName());
        Allure.addAttachment("Действия с сотрудником", "Был выбран сотрудник с именем: " +
                employee.getFullName());
    }

    @Step("Выбрать сотрудника c именем {name} из списка")
    private void selectAnyEmployee(String name) {
        LOG.info("Имя выбранного сотрудника: {}", name);
        slowSendKeys(os.addNewEmployeeForm().nameField(), name);
        os.addNewEmployeeForm().loadingField().waitUntil("Надпись загрузки все еще не пропала",
                                                         Matchers.not(DisplayedMatcher.displayed()), 20);
        os.addNewEmployeeForm().employeeSearchInput().sendKeys(Keys.BACK_SPACE);
        os.addNewEmployeeForm().loadingField().waitUntil("Надпись загрузки все еще не пропала",
                                                         Matchers.not(DisplayedMatcher.displayed()), 20);
        os.addNewEmployeeForm().employeeButton(name).should(EMPLOYEE_NOT_DISPLAYED,
                                                            DisplayedMatcher.displayed(), 5);
        os.addNewEmployeeForm().employeeButton(name).click();
    }

    private void selectFunctionalRole(PositionGroup role) {
        openFunctionalRoleList();
        selectRole(role.getName());
    }

    @Step("Проверить, что результат поиска пустой")
    private void checkSearchEmpty(String name) {
        LOG.info("Имя выбранного сотрудника: {}", name);
        slowSendKeys(os.addNewEmployeeForm().nameField(), name);
        os.addNewEmployeeForm().employeeButton(name).should("Не был отображен в списке: " + name,
                                                            Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Нажать на стрелку вниз в поле \"Функциональная роль\"")
    private void openFunctionalRoleList() {
        os.addNewEmployeeForm().functionalRoleButton().click();
    }

    @Step("В раскрывшемся списке выбрать \"{role}\"")
    private void selectRole(String role) {
        os.addNewEmployeeForm().funcRoleButton(role).click();
    }

    /**
     * Инкапсуляция логики работы с выбором даты
     *
     * @param date - дата
     */
    private void datePickLogic(LocalDate date) {
        DatePicker datePicker = new DatePicker(os.datePickerFormInEmployee());
        datePicker.pickDate(date);
        datePicker.okButtonClick();
    }

    /**
     * Метод прокручивает календарь до нужной нам даты и проверяет, можно ли кликнуть по дате или нет
     */
    private boolean dateIsClickable(LocalDate date) {
        DatePicker datePicker = new DatePicker(os.datePickerFormInEmployee());
        datePicker.pickDate(date);
        return datePicker.checkDateElementClickable(date);
    }

    @Step("Проверить, что форма не закрывается, появляется поп-ап с сообщением об ошибке доступа")
    private void checkCalendarButtonNotDisplayed(String errorText) {
        os.notificationMessage(errorText).should(String.format("Сообщение об ошибке доступа с текстом \"%s\" не отображается", errorText),
                                                 DisplayedMatcher.displayed(), 5);
        os.addNewEmployeeForm().should("Форма была закрыта", DisplayedMatcher.displayed());
    }

    @Step("В поле \"{inputDate.name}\" выбрать из календаря дату {date}")
    private void selectInCalendarPositionDate(LocalDate date, DateTypeField inputDate) {
        //используется в тесте с провайдером как средство для матчинга
        LOG.info("В поле \"{}\" выбрать из календаря дату {}", inputDate.getName(), date);
        os.addNewEmployeeForm().calendarButton(inputDate.getName())
                .should(CALENDAR_BUTTON_NOT_DISPLAYED, DisplayedMatcher.displayed());
        os.addNewEmployeeForm().calendarButton(inputDate.getName()).click();
        datePickLogic(date);
        Assert.assertEquals(os.addNewEmployeeForm().inputVariantDate(inputDate.getName()).getAttribute("value"),
                            date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))),
                            DATE_DOES_NOT_MATCH);
    }

    @Step("В поле \"{inputDate.name}\" выбрать из календаря дату {date} и проверить, что она недоступна для выборки")
    private void selectInCalendarPositionDateAndCheckDateClickable(LocalDate date, DateTypeField inputDate) {
        LOG.info("В поле \"{}\" выбрать из календаря дату {}", inputDate.getName(), date);
        os.addNewEmployeeForm().calendarButton(inputDate.getName())
                .should(CALENDAR_BUTTON_NOT_DISPLAYED, DisplayedMatcher.displayed());
        os.addNewEmployeeForm().calendarButton(inputDate.getName()).click();
        Assert.assertFalse(dateIsClickable(date), "В поле \"Окончание работы\" была выбрана текущая или будущая дата");
    }

    @Step("Проверить, что даты начала и окончания недоступны для изменений")
    private void assertDatesNotActive() {
        os.addNewEmployeeForm().calendarButton(DateTypeField.START_JOB.getName())
                .should(Matchers.not(DisplayedMatcher.displayed()));
        os.addNewEmployeeForm().calendarButton(DateTypeField.END_JOB.getName())
                .should(Matchers.not(DisplayedMatcher.displayed()));
    }

    @Step("Выбираем дату {date} в поле \"{inputVariants.name}\"")
    private void chooseDatePositionForm(LocalDate date, DateTypeField inputVariants) {
        String inputVar = inputVariants.getName();
        os.addNewEmployeeForm().inputVariantDate(inputVar).click();
        os.addNewEmployeeForm().inputVariantDate(inputVar).clear();
        os.addNewEmployeeForm().inputVariantDate(inputVar).sendKeys(date.format(Format.UI.getFormat()));
        LOG.info("В поле {} ввели дату {} ", inputVar, date);
    }

    @Step("Нажать на корзину рядом с полем \"Дата окончания должности\"")
    private void clearDateInFieldEndPosition() {
        AtlasWebElement el = os.addNewEmployeeForm().clearDateEndPosition();
        if (el.isDisplayed()) {
            el.click();
        }
    }

    @Step("Нажать на кисточку рядом с полем \"Дата окончания работы\"")
    private void clearDateInFieldEndWork() {
        AtlasWebElement el = os.addNewEmployeeForm().clearDateEndWork();
        if (el.isDisplayed()) {
            el.click();
        }
    }

    @Step("Выбираем дату {date} в поле \"{inputVariants.name}\"")
    private void chooseDeputyDate(LocalDate date, DateTypeField inputVariants, int position) {
        String inputVar = inputVariants.getName();
        os.employeeData().dateInput(position, inputVar).click();
        os.employeeData().dateInput(position, inputVar).clear();
        os.employeeData().dateInput(position, inputVar).sendKeys(date.format(Format.UI.getFormat()));
        LOG.info("В поле {} ввели дату {} ", inputVar, date);
    }

    @Step("Выбрать дату {date} в поле даты исключения")
    private void chooseDateExceptionsSchedule(LocalDate date) {
        os.omEditingForm().exceptionDate().waitUntil("Поле даты в разделе \"Исключения\" не подгрузилось", DisplayedMatcher.displayed());
        os.omEditingForm().exceptionDate().click();
        os.omEditingForm().exceptionDate().clear();
        os.omEditingForm().exceptionDate().sendKeys(date.format(UI.getFormat()));
        os.omEditingForm().exceptionDate().sendKeys(Keys.ENTER);
        LOG.info("В поле исключения ввели дату {} ", date);
    }

    @Step("Активировать чекбокс возле поля \"{skills.name}\"")
    private void clickOnSkillsCheckBox(EmployeeSkills skills) {
        os.osCardForm().skillCheckBox(skills.getName()).click();
        LOG.info("Активировали чекбокс \"{}\"", skills.getName());
    }

    @Step("Получить дату в поле \"{inputVariants.name}\"")
    private LocalDate getDateInForm(DateTypeField inputVariants) {
        String dateValue = os.addNewEmployeeForm().inputVariantDate(inputVariants.getName()).getAttribute("value");
        if (dateValue.isEmpty()) {
            return null;
        } else {
            return LocalDate.parse(dateValue,
                                   DateTimeFormatter.ofPattern("d MMMM yyyy").withLocale(new Locale("ru", "RU")));
        }
    }

    @Step("Нажать на кнопку сброс над фильтрами")
    private void resetFilters() {
        os.osFilterForm().resetButton()
                .waitUntil("Кнопка сброс не отображается", DisplayedMatcher.displayed(), 5);
        os.osFilterForm().resetButton().click();
        os.spinnerLoader().loadingForm().waitUntil("Спиннер все еще крутится",
                                                   Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Ввести значение \"{value}\" в поле \"{empFields.name}\"")
    private void sendValueInInput(EmpFields empFields, String value) {
        os.osCardForm().employeeFieldByType(empFields.getFieldName()).click();
        os.osCardForm().employeeFieldByType(empFields.getFieldName()).clear();
        os.osCardForm().employeeFieldByType(empFields.getFieldName()).sendKeys(value);
    }

    @Step("Выбрать пол {gender}")
    private void changeGender(EmpFields gender) {
        os.osCardForm().employeeFieldByType(EmpFields.GENDER.getFieldName()).click();
        os.osCardForm().genderButton(gender.getFieldName()).waitUntil(DisplayedMatcher.displayed());
        os.osCardForm().genderButton(gender.getFieldName()).click();
    }

    private EmpFields getAnotherGender() {
        String gender = os.osCardForm().employeeFieldByType(EmpFields.GENDER.getFieldName()).getAttribute("value");
        if (gender.equals(EmpFields.FEMALE.getFieldName())) {
            return EmpFields.MALE;
        } else if (gender.equals(EmpFields.MALE.getFieldName())) {
            return EmpFields.FEMALE;
        } else {
            //на случай пустого поля
            return EmpFields.MALE;
        }
    }

    @Step("Выбрать дату рождения {date}")
    private void enterEmployeeBirthday(LocalDate date) {
        os.osCardForm().calendarButtons(EmpFields.DATE_OF_BIRTH.getFieldName()).click();
        datePickLogic(date);
    }

    @Step("Выбрать дату окончания работы {date}")
    private void selectEmployeeEndDate(LocalDate date) {
        os.osCardForm().employeeDataFieldByType(EmpFields.END_WORK_DATE.getFieldName()).click();
        os.osCardForm().employeeDataFieldByType(EmpFields.END_WORK_DATE.getFieldName()).clear();
        os.osCardForm().employeeDataFieldByType(EmpFields.END_WORK_DATE.getFieldName())
                .sendKeys(date.format(UI_DOTS.getFormat()));
    }

    @Step("Нажать на шеврон раскрытия раздела \"{infoNames.nameOfInformation}\"")
    private void clickOnChevronButton(OmInfoName infoNames) {
        String section = infoNames.getNamesOfInformation();
        new Actions(os.getWrappedDriver()).moveToElement(os.omInfoForm().showButton(section)).perform();
        os.omInfoForm().showButton(section)
                .should(String.format("Шеврон раскрытия раздела \"%s\" не отображается", section),
                        DisplayedMatcher.displayed(), 10);
        os.omInfoForm().showButton(section).click();
        systemSleep(3); //иначе при раскрытии не всегда прогружается и может зависнуть
        LOG.info("Раскрыли раздел \"{}\"", section);
    }

    private String determineActiveScheduleId(int orderNumber) {
        ActWithSchedule actWithSchedule = new ActWithSchedule(os.selectScheduleForm());
        return actWithSchedule.getActiveScheduleId(orderNumber);
    }

    /**
     * Определяет порядковый номер активного графика при открытом меню троеточия
     */
    private int determineActiveScheduleNumber() {
        String activeSchedule = os.omInfoForm().activeSchedule().getText();
        List<String> schedules = os.omInfoForm().allAvailableSchedules().stream().map(AtlasWebElement::getText).collect(Collectors.toList());
        List<String> dates = os.omInfoForm().scheduleDates().stream().map(AtlasWebElement::getText).collect(Collectors.toList());
        for (int i = 0; i < schedules.size(); i++) {
            String[] openCloseDates = dates.get(i).trim().split(" ");
            LocalDate openDate = LocalDate.parse(openCloseDates[0]);
            LocalDate closeDate = LocalDate.parse(openCloseDates[2]);
            if (LocalDate.now().isBefore(closeDate) && LocalDate.now().isAfter(openDate) && schedules.get(i).equals(activeSchedule)) {
                return i;
            }
        }
        throw new AssertionError("Активный график не найден");
    }

    /**
     * Определяет порядковый номер необходимой роли
     */
    private int determineRoleOrderNumber(String roleName) {
        List<AtlasWebElement> roleTitles = os.editRoleForm().roleTitles();
        int i = 0;
        for (AtlasWebElement title : roleTitles) {
            if (title.getText().equals(roleName)) {
                return i;
            } else {
                i++;
            }
        }
        return i;
    }

    /**
     * Заходит в меню выбора графика при открытом меню троеточия
     */
    private void openScheduleSelectionMenu() {
        os.omInfoForm().selectScheduleButton().click();
        os.selectScheduleForm().waitUntil("Форма выбора графика работы не отобразилась", DisplayedMatcher.displayed(), 15);
        LOG.info("Зашли в меню выбора графика");
    }

    /**
     * Выходит из меню выбора графика
     */
    private void exitScheduleSelectionMenu() {
        os.selectScheduleForm().cancelButton().click();
        LOG.info("Вышли в меню выбора графика");
    }

    /**
     * Обновляет UI, чтобы на нем отобразились изменения, которые могли быть внесены пресетом
     */
    private void refreshScheduleUI() {
        clickOnThreeDotsButton();
        os.omInfoForm().activeSchedule().click();
        LOG.info("Обновили UI");
    }

    /**
     * Обновляет текущее окно. При SystemProperties.ROSTER_QUIT_TAB_NOTICE=true
     * если появляется всплывающее alert окно, то подтверждает изменения, и обновляет страницу.
     * Если не появляется, просто обновляет страницу.
     */
    private void refreshPageAndAcceptAlertWindow() {
        LOG.info("Обновляем текущее окно");
        os.getWrappedDriver().navigate().refresh();
        try {
            os.getWrappedDriver().switchTo().alert().accept();
        } catch (NoAlertPresentException e) {
            LOG.info("Всплывающее окно не появилось");
        }
        os.spinnerLoader().grayLoadingBackground().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 30);
        os.spinnerLoader().loadingForm().waitUntil("Спиннер все еще крутится",
                                                   Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Нажать на значок \"Троеточие\"")
    private void clickOnThreeDotsButton() {
        os.omInfoForm().threeDotsButton()
                .should("Кнопка \"Троеточие\" не отбразилось", DisplayedMatcher.displayed(), 10);
        os.omInfoForm().threeDotsButton().click();
        LOG.info("Нажали на значок \"Троеточие\"");
    }

    /**
     * Заходит в меню графиков и проверяет наличие изменений
     */
    @Step("Проверить, что отображаемый тип графика подразделения поменялся")
    private void typeChangeCheck(BusinessHours scheduleId) {
        String displayedType = os.omInfoForm().chosenScheduleType().getText();
        String expectedType = scheduleId.getEnumType().getNameOfType();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(displayedType, expectedType,
                                String.format("Отображен неверный тип графика. Выбран тип: \"%s\" а отобразился: \"%s\"",
                                              displayedType, expectedType));
        softAssert.assertEquals(os.omInfoForm().chosenScheduleTimePeriod().getText(), scheduleId.getDisplayedTimePeriod(),
                                "Отображен неверный временной интервал.");
        Allure.addAttachment("Тип текущего графика",
                             String.format("В данный момент активирован график с типом \"%s\"", displayedType));
        softAssert.assertAll();
    }

    @Step("Нажать на кнопку \"Управление списком графиков работы\"")
    private void clickOnSelectScheduleButton() {
        os.omInfoForm().selectScheduleButton()
                .waitUntil("Кнопка выбора графика не отображается", DisplayedMatcher.displayed(), 10);
        os.omInfoForm().selectScheduleButton().click();
    }

    @Step("Проверка графика добавления список графиков до: {before}, список после: {after}")
    private void scheduleCheckAdding(ScheduleType service, LocalDate dateOpen, LocalDate dateEnd,
                                     List<BusinessHours> before, List<BusinessHours> after) {
        after.removeAll(before);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(1, after.size(), "Проблемы с добавлением графика");
        Allure.addAttachment("График", "Было добавлено графиков:" + after.size());
        BusinessHours businessHours = after.iterator().next();
        softAssert.assertEquals(businessHours.getDateInterval().getStartDate(), dateOpen);
        softAssert.assertEquals(businessHours.getDateInterval().getEndDate(), dateEnd);
        softAssert.assertEquals(businessHours.getEnumType(), service);
        softAssert.assertAll();
    }

    @Step("Нажать на кнопку \"Редактировать график работы\"")
    private void clickOnEditScheduleButton() {
        os.omInfoForm().changeScheduleButton()
                .should("Кнопка редактирования не была отображена", DisplayedMatcher.displayed());
        os.omInfoForm().changeScheduleButton().click();
        LOG.info("Нажали на кнопку \"Редактировать график работы\"");
        os.omInfoForm().dayType(1)
                .waitUntil("Режим работы подразделения отображается в режиме просмотра, несмотря на нажатие кнопки", DisplayedMatcher.displayed(), 10);
    }

    @Step("У дня c номером {dayNumber} изменить \"Время начала\" на {time}")
    private void changeDayStartTime(LocalTime time, int dayNumber) {
        os.omInfoForm().dayStartTimeField(dayNumber)
                .waitUntil("Поле с временем не отобразилось", DisplayedMatcher.displayed(), 5);
        os.omInfoForm().dayStartTimeField(dayNumber).click();
        os.omInfoForm().dayStartTimeField(dayNumber).clear();
        os.omInfoForm().dayStartTimeField(dayNumber).sendKeys(time.format(DateTimeFormatter.ofPattern("HH:mm")));
        LOG.info("У дня c номером {} изменить \"Время начала\" на {}", dayNumber, time);
    }

    @Step("Проверить, что время начала и окончания у дня с номером {dayNumber}")
    private void switchDayTimeCheck(int dayNumber, String scheduleId, LocalTime startTime, LocalTime endTime) {
        waitForClickable(os.omInfoForm().threeDotsButton(), os, 15);
        Map<String, String> temp = CommonRepository.getWorkingDaysTime(scheduleId, dayNumber);
        String startTimeApi = temp.get(START_TIME);
        String endTimeApi = temp.get(END_TIME);
        Allure.addAttachment("Проверка",
                             String.format("Время начала и окончания введенные: %s %s " +
                                                   "Время начала и окончания, отобразившиеся в api после сохранения изменений: %s %s",
                                           startTime, endTime, startTimeApi, endTimeApi));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(startTime.toString(), startTimeApi, "Время начала в api и введенное не совпали");
        softAssert.assertEquals(endTime.toString(), endTimeApi, "Время окончания в api и введенное не совпали");
        softAssert.assertAll();
    }

    @Step("Нажать на кнопку \"Изменить\"")
    private void clickOnEditionScheduleChangeButton() {
        new Actions(os.getWrappedDriver()).moveToElement(os.omInfoForm().cancelScheduleCancelButton()).perform();
        os.omInfoForm().editionScheduleSaveButton().click();
        os.spinnerLoader().loadingForm().waitUntil("Спиннер всё ещё отображается", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("У дня c номером {dayNumber} изменить \"Время окончания\" на {time}")
    private void changeDayEndTime(LocalTime time, int dayNumber) {
        os.omInfoForm().dayEndTimeField(dayNumber).click();
        os.omInfoForm().dayEndTimeField(dayNumber).clear();
        os.omInfoForm().dayEndTimeField(dayNumber).sendKeys(time.format(DateTimeFormatter.ofPattern("HH:mm")));
        LOG.info("У дня c номером {} изменить \"Время окончания\" на {}", dayNumber, time);
    }

    @Step("Кликнуть на кнопку меню пустой должности (три точки)")
    private void threeDotsEmptyPositions() {
        Random random = new Random();
        os.omInfoForm().threeDotEmptyPositionButtons()
                .waitUntil("Не было пустых должностей отображено", Matchers.not(Matchers.empty()), 3);
        int size = random.nextInt(os.omInfoForm().threeDotEmptyPositionButtons().size());
        //На гриде мешает кнопка +
        new Actions(os.getWrappedDriver())
                .moveToElement(os.osCardForm().pencilButton(EmployeeInfoName.OPTIONS.getNameOfInformation())).perform();
        os.omInfoForm().threeDotEmptyPositionButtons().get(size).click();
        String positionName;
        try {
            positionName = os.omInfoForm().emptyPositionsNamesRelease().get(size).getText().replaceAll(" ", "");
        } catch (IndexOutOfBoundsException e) {
            positionName = os.omInfoForm().emptyPositionsNamesMaster().get(size).getText().substring("Ставка: / ".length() + 1);
        }
        LOG.info("Кликнули три точки напротив пустой должности \"{}\"", positionName);
        Allure.addAttachment("Название должности", "text/plain",
                             String.format("Выбрана должность: %s", positionName));
    }

    @Step("Кликнуть на кнопку меню пустой должности (три точки)")
    private void threeDotsEmptyPositions(Position position, int index) {
        os.omInfoForm().threeDotEmptyPositionButtons()
                .waitUntil("Не было пустых должностей отображено", Matchers.not(Matchers.empty()), 15);
        new Actions(os.getWrappedDriver())
                .moveToElement(os.osCardForm().pencilButton(EmployeeInfoName.OPTIONS.getNameOfInformation())).perform();
        os.omInfoForm().emptyPositionsByName(position.getName()).get(index).click();
    }

    @Step("В поле \"Сотрудники\" выбрать сотрудника с именем {name}, нажать на кнопку \"Троеточие\"")
    private void clickOnThreeDotsButtonByName(String name) {
        os.omInfoForm().allThreeDots().waitUntil(Matchers.hasSize(Matchers.greaterThan(0)));
        os.omInfoForm().threeDotsByNameOfEmployee(name).click();
    }

    @Step("Нажать на чекбокс \"Руководитель\"")
    private void leaderCheckBoxClick() {
        os.addNewEmployeeForm().leaderCheckBox().click();
    }

    @Step("Нажать на кнопку удаления должности")
    private void deletePosition() {
        os.omInfoForm().deletePositionButton()
                .waitUntil("Кнопка удаления должности не отобразилась", DisplayedMatcher.displayed(), 5);
        os.omInfoForm().deletePositionButton().click();
        LOG.info("Нажали на кнопку \"Удалить\"");
    }

    @Step("Проверить, что одна из пустых должностей была удалена")
    private void assertForDeleteEmptyPosition(List<Position> before, int omId, int previouslySizeUi) {
        systemSleep(2); //метод используется в неактуальных тестах
        os.omInfoForm().threeDotEmptyPositionButtons().should("Должность не пропала на UI",
                                                              Matchers.iterableWithSize(previouslySizeUi - 1));
        List<Position> after = PositionRepository.getFreePositions(PositionRepository.getPositionsArray(omId));
        before.removeAll(after);
        Assert.assertEquals(before.size(), 1, "Количество не назначенных должностей не изменилось");
    }

    @Step("Нажать на кнопку \"Плюс\"")
    private void clickOnPlusButtonEmployee() {
        os.omInfoForm().plusButtonEmployee().
                waitUntil("plus button was not displayed", DisplayedMatcher.displayed(), 10);
        os.omInfoForm().plusButtonEmployee().click();
    }

    @Step("Проверить добавление даты окончания {date}")
    private void assertDateEndAvailability(LocalDate date, EmployeePosition employeePosition) {
        EmployeePosition update = new EmployeePosition(getJsonFromUri(Projects.WFM, employeePosition.getSelfLink()));
        Assert.assertEquals(update.getDateInterval().endDate, date,
                            String.format("Дата не была выставлена. Дата окончания %s сотруднику с именем %s в api была найдена дата %s",
                                          date, employeePosition.getEmployee().getFullName(), employeePosition.getDateInterval().endDate));
    }

    @Step("Проверить, что дата начала должности сменилась на {date}")
    private void assertPositionStartDateChanged(LocalDate date, OrgUnit orgUnit, EmployeePosition employeePosition) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(PositionRepository.getPositionsArray(orgUnit.getId()).stream()
                                      .anyMatch(position -> position.getDateInterval().getEndDate().isEqual(date)),
                              "Не была найдена позиция с датой окончания: " + date);
        EmployeePosition update = new EmployeePosition(getJsonFromUri(Projects.WFM, employeePosition.getLink(SELF)));
        softAssert.assertTrue(update.getPosition().getDateInterval().getEndDate().isEqual(date),
                              "Дата начала работы не изменилась");
        softAssert.assertAll();
    }

    @Step("Ввести {newLogin} в поле \"Имя пользователя\"")
    private void enterNewLogin(String newLogin) {
        os.osCardForm().employeeLoginField().click();
        os.osCardForm().employeeLoginField().clear();
        os.osCardForm().employeeLoginField().sendKeys(newLogin);
    }

    @Step("Ввести {newPassword} в поле \"Пароль\"")
    private void enterNewPassword(String newPassword) {
        os.osCardForm().employeePassField().sendKeys(newPassword);
    }

    @Step("Ввести {newPassword} в поле \"Подтверждение пароля\"")
    private void confirmNewPassword(String newPassword) {
        os.osCardForm().employeeConformPassField().sendKeys(newPassword);
    }

    @Step("Click to reset button")
    private void resetButtonEmpPositionFilter() {
        os.filterEmpPositionForm().employeePositionReset().click();
    }

    @Step("Нажать на значок \"Троеточие\" возле сотрудника с именем {name}")
    private void clickOnEmployeeThreeDots(String name) {
        LOG.info("Нажимаем на значок \"Троеточие\" возле сотрудника с именем {}", name);
        new Actions(os.getWrappedDriver()).moveToElement(os.omInfoForm().threeDotsByNameOfEmployee(name)).perform();
        os.omInfoForm().allThreeDots().
                forEach(extendedWebElement -> extendedWebElement
                        .waitUntil("Не все троеточия загрузились", DisplayedMatcher.displayed(), 5));
        os.omInfoForm().threeDotsByNameOfEmployee(name).click();
    }

    @Step("Проверить совпадения ввода названия и результатом поиска")
    private void compareSelectedOm() {
        os.omInfoForm().parentOmFields().isEnabled();
        os.omInfoForm().omName().isEnabled();
        String selected = os.omInfoForm().omName().getText();
        String omName = os.osSearchForm().osPickSelectedFromList().getText();
        Assert.assertTrue(selected.equalsIgnoreCase(omName));
    }

    @Step("Проверить, что был активирован фильтр по позициям")
    private void assertFilterIndicator() {
        os.osFilterForm().employeePositionFilterIsActive()
                .should("Фильтр по позициям не был активирован", DisplayedMatcher.displayed(), 5);
    }

    @Step("Проверить добавление тега \"{changingTag}\" у оргюнита \"{unit.name}\"")
    private void assertChangingOmTags(String changingTag, OrgUnit unit, List<String> tagsBefore) {
        os.omInfoForm().tagsFieldOrgUnitCard().waitUntil("Поля тегов в карточке ОМ не отобразились", DisplayedMatcher.displayed(), 5);
        List<String> tagsAfter = unit.getTags();
        List<String> tagList = new ArrayList<>(Arrays.asList(os.omInfoForm().tagsFieldOrgUnitCard().getText().split(", ")));
        Assert.assertEquals(tagList, tagsAfter, "Списки тегов на UI и в API различаются");
        tagsAfter.removeAll(tagsBefore);
        Assert.assertFalse(tagsAfter.isEmpty(), "Список тегов в API не изменился после добавления тега на UI");
        String newTag = tagsAfter.iterator().next();
        Assert.assertEquals(newTag, changingTag, "Появившийся в API тег не соответствует введенному");
        Allure.addAttachment("Добавление тега " + changingTag, "На UI отобразились теги: " + tagList);
    }

    @Step("Проверить отмену добавления тега \"{changingTag}\" у оргюнита \"{unit.name}\" ")
    private void assertCancelOmTag(String changingTag, OrgUnit unit, NumberOfTags number) {
        os.omInfoForm().editingButton().waitUntil("Поля тегов в карточке ОМ не отобразились", DisplayedMatcher.displayed(), 5);
        OrgUnit orgUnit = unit.refreshOrgUnit();
        String tagString = orgUnit.getTags().toString();
        Allure.addAttachment("Теги", String.format("Был отменен тег \"%s\" в итоге в API отобразилось: %s",
                                                   changingTag, tagString));
        Assert.assertFalse(tagString.contains(changingTag),
                           String.format("Отмененный тег на: %s в процессе отмены установился у оргюнита \"%s\". В API: %s",
                                         changingTag, unit.getName(), tagString));
        Assert.assertEquals(tagString, tagString, "В строке появись лишние теги: " + tagString);
    }

    @Step("Проверить удаление тэга \"{changingTag}\" у оргюнита \"{unit.name}\"")
    private void assertDeletingOmTags(String changingTag, OrgUnit unit, String tagsBefore, NumberOfTags number) {
        os.omInfoForm().editingButton().waitUntil("Поля тегов в карточке ОМ не отобразились", DisplayedMatcher.displayed(), 5);
        OrgUnit orgUnit = unit.refreshOrgUnit();
        List<String> tagString = orgUnit.getTags();
        Allure.addAttachment("Теги",
                             String.format("Был удален тег \"%s\" в итоге в API отобразилось: %s",
                                           changingTag, tagString));
        Assert.assertFalse(tagString.contains(changingTag),
                           String.format("Удаленный тег на UI: %s не удалился у оргюнита \"%s\". В API: %s",
                                         changingTag, unit.getName(), tagString));
        if (number.equals(NumberOfTags.ONE_TAG)) {
            Assert.assertTrue(tagString.isEmpty(), "В строке появись лишние теги: " + tagString);
        } else {
            int before = tagsBefore.replaceAll(",,", "").split(",").length;
            int after = tagString.size();
            Assert.assertEquals(before - after, 1, "Теги были изменены");
        }
    }

    @Step("Проверить, что не отображается поле \"Участвует в расчете\"")
    private void assertChangingOmCalc(boolean saveChanges) {
        try {
            os.omInfoForm().omName().waitUntil("Elements not displayed", DisplayedMatcher.displayed(), 5);
        } catch (Exception ex) {
            Assert.fail("Elements not displayed");
        }
        if (saveChanges) {
            Assert.assertEquals(os.omInfoForm().omCalcFieldName().getText(), "Участвует в расчете");
        } else {
            os.omInfoForm().omCalcFieldName()
                    .should("Отображается поле \"Участвует в расчете\"", Matchers.not(DisplayedMatcher.displayed()), 5);
        }
    }

    @Step("Проверить, что текущее имя оргюнита - {expected}")
    private void assertChangingOmName(String expected, OrgUnit orgUnit) {
        os.omInfoForm().omName().should("Название оргюнита не изменилось", text(containsString(expected)), 10);
        String orgName = orgUnit.refreshOrgUnit().getName();
        Assert.assertEquals(expected, orgName);
        Allure.addAttachment("Проверка", String.format("Название подразделения после действий теста:\nUI: %s\nAPI: %s",
                                                       expected, orgName));
    }

    @Step("Проверить, что текущий outerId оргюнита изменился на \"{expected}\"")
    private void assertChangingOuterId(String expected, OrgUnit orgUnit) {
        String orgName = orgUnit.refreshOrgUnit().getOuterId();
        Assert.assertEquals(expected, orgName);
    }

    private LocalDate findStartDate() {
        List<AtlasWebElement> endDates = os.selectScheduleForm().allEndDates();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        List<String> value = endDates.stream().map(e -> e.getAttribute("value")).filter(s -> !s.equals(""))
                .collect(Collectors.toList());
        List<LocalDate> dates = value.stream()
                .map(ds -> LocalDate.parse(ds, formatter))
                .sorted(LocalDate::compareTo)
                .collect(Collectors.toCollection(ArrayList::new));
        return dates.get(dates.size() - 1).plusDays(2);
    }

    @Step("Проверить добавление наставника сотруднику с именем {name}")
    private void checkInternProgramMatches(Employee employees) {
        Assert.assertTrue(employees.refreshEmployee().isNeedMentor(),
                          "Сотрудника не была добалена стажерская программа");
        os.employeeData().needMentorField().should("Поле с текстом \"Требуется наставник\" не отобразилось",
                                                   text(containsString("Требуется наставник")), 5);
        Allure.addAttachment("Проверка",
                             String.format("Сотруднику с именем %s была успешно добавлена стажерская программа", employees));
    }

    @Step("Проверка на то, что у сотрудника {name} изменилась дата окончания на {date}")
    private void assertEndDate(LocalDate date, int empId, String name) {
        os.osCardForm().empPencilButton().waitUntil("Закрытие редактирования не произошло",
                                                    DisplayedMatcher.displayed(), 10);
        String urlEndingAllEmps = makePath(EMPLOYEES, empId);
        JSONObject empObject = getJsonFromUri(Projects.WFM, URL_ORG_STRUCTURE, urlEndingAllEmps);
        String endDateApi = empObject.getString("endWorkDate");
        Allure.addAttachment("Дата окончания", "text/plain",
                             String.format("У сотрудника по имени %s выбрана дата окончания %s а в API отобразилась %s",
                                           name, date.toString(), endDateApi));
        Assert.assertEquals(date.toString(), endDateApi,
                            String.format("Даты окончания у сотрудника не совпали. Выбрана дата окончания: %s а в API отобразилась %s",
                                          date, endDateApi));
    }

    @Step("Проверить отмену изменения типа подразделения")
    private void assertChangingOmType(ImmutablePair<String, Integer> typeIdAndTypeName, boolean acceptChanges, OrgUnit unit) {
        final int organizationUnitTypeId = unit.refreshOrgUnit().getOrganizationUnitTypeId();
        os.omInfoForm().omType().waitUntil("Поле с типом Ом не прогрузилось", DisplayedMatcher.displayed(), 20);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(typeIdAndTypeName.left.equals(os.omInfoForm().omType().getText()), acceptChanges);
        softAssert.assertEquals(organizationUnitTypeId == typeIdAndTypeName.right, acceptChanges);
        softAssert.assertAll();
    }

    @Step("Проверка на то, что назначение заместителя {employee} в оргюнит \"{orgUnit.name}\" было отменено")
    private void assertCancelDeputy(OrgUnit orgUnit, String deputyBefore) {
        String url = makePath(ORGANIZATION_UNITS, orgUnit.getId(), DEPUTY_EMPLOYEE);
        URI uri = setUri(Projects.WFM, URL_ORG_STRUCTURE, url);
        if (deputyBefore.equals("null")) {
            String jsonObj = setUrlAndInitiateForApi(uri, Projects.WFM);
            Allure.addAttachment("Заместитель", "text/plain",
                                 String.format("У оргюнита \"%s\", у которого не было сотрудника, было отменено назначение заместителя, и теперь в API: %s",
                                               orgUnit.getName(), jsonObj));
            Assert.assertEquals(jsonObj, "", String.format("У оргюнита без заместителя в API появился заместитель %s", jsonObj));
            os.omInfoForm().omDeputyName().should("У оргюнита, у которого не было заместителя, назначился заместитель",
                                                  Matchers.not(DisplayedMatcher.displayed()), 5);
        } else {
            JSONObject jsonObject = new JSONObject(setUrlAndInitiateForApi(uri, Projects.WFM));
            URI empHref = URI.create(jsonObject.getJSONObject(LINKS).getJSONObject(EMPLOYEE_JSON).getString(HREF));
            Employee emp = new Employee(new JSONObject(setUrlAndInitiateForApi(empHref, Projects.WFM)));
            Allure.addAttachment("Заместитель", "text/plain",
                                 String.format("У оргюнита \"%s\", у которого был сотрудник %s, было отменено назначение заместителя , и теперь в API %s",
                                               orgUnit.getName(), deputyBefore, emp.getFullName()));
            Assert.assertEquals(emp.getFullName(), deputyBefore,
                                String.format("В API поменялся заместитель. Должен был быть %s, а текущий - %s",
                                              deputyBefore, emp.getFullName()));
            os.omInfoForm().omDeputyName().should("Отображаемый заместитель изменился", text(containsString(emp.getFullName())));
        }
    }

    @Step("Проверить изменение родительского оргЮнита")
    private void assertChangingParentName(String currentParent, String lastParen) {
        try {
            os.omInfoForm().omParentName().waitUntil("Elements not displayed", DisplayedMatcher.displayed(), 5);
        } catch (Exception ex) {
            Assert.fail("Elements not displayed");
        }
        Allure.addAttachment("Родительский оргюнит", "text/plain",
                             String.format("У оргюнита был родительский оргюнит \"%s\", после изменения \"%s\"",
                                           lastParen, os.omInfoForm().omParentName().getText()));
        Assert.assertNotEquals(os.omInfoForm().omParentName().getText(), lastParen);
        Assert.assertEquals(os.omInfoForm().omParentName().getText(), currentParent);
    }

    @Step("Проверить отмену изменеия родительского оргЮнита")
    private void assertDismissChangeParentName(String currentParent, String lastParen) {
        try {
            os.omInfoForm().omParentName().waitUntil("Elements not displayed", DisplayedMatcher.displayed(), 5);
        } catch (Exception ex) {
            Assert.fail("Elements not displayed");
        }
        Allure.addAttachment("Родительский оргюнит", "text/plain",
                             String.format("У оргюнита был родительский оргюнит \"%s\", после отмены изменения \"%s\"",
                                           lastParen, os.omInfoForm().omParentName().getText()));
        Assert.assertEquals(os.omInfoForm().omParentName().getText(), lastParen);
        Assert.assertNotEquals(os.omInfoForm().omParentName().getText(), currentParent);
    }

    @Step("Проверить изменение контактов")
    private void assertChangingContacts() {
        String test = os.omInfoForm().testPhoneType().getText();
        Assert.assertEquals(test, "Домашний");
    }

    @Step("Проверить изменение имени сотрудника")
    private void assertChangingEmpName(String firstName, String lastName, String patronymicName, EmpFields empFields,
                                       LocalDate date) {
        String expectedName = String.format("%s %s %s", lastName, firstName, patronymicName);
        os.osCardForm().employeeNameField().should("Введенное имя не отобразилось", text(containsString(expectedName)));
        String dateBirthDay = os.osCardForm().employeeFieldsData(EmpFields.DATE_OF_BIRTH.getFieldName()).getText();
        String gender = os.osCardForm().employeeFieldsData("Пол").getText();
        Locale locale = new Locale("ru");
        String expectedDateBirthDay = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(dateBirthDay, expectedDateBirthDay, "Введенная дата рождения не совпадает с отображаемой");
        softAssert.assertEquals(empFields.getFieldName(), gender, "Введенный пол не совпадает с отображаемым");
        softAssert.assertAll();
    }

    @Step("Проверить изменение логина у пользователя")
    private void assertNewLogin() {
        os.osCardForm()
                .waitUntil("Can't load the page", DisplayedMatcher.displayed());
        String test = os.osCardForm().testNewLogin().getText();
        Assert.assertEquals(test, "LogTest");
    }

    /**
     * Метод получает список отобразившихся ОМ или сотрудников
     */
    private List<String> getListOnUI() {
        List<String> listFromUi = os.osSearchForm().allSearchResult()
                .stream()
                .map(WebElement::getText)
                .map(String::trim)
                .sorted()
                .collect(Collectors.toList());
        if (listFromUi.size() > 20) {
            listFromUi = listFromUi.subList(0, 20);
        }
        return listFromUi;
    }

    /**
     * Метод выполняет проверку корректности отображения отфильтроавнных ОМ
     * Смотрим в апи какие ОМ соотвевуют фильтру и сравниваем что отображается на UI
     */
    @Step("Проверка отображения результатов на UI в сравнении с API")
    private void checkThatListMatches(List<String> listFromApi) {
        List<String> list = getListOnUI();
        if (listFromApi.size() > 20) {
            listFromApi = listFromApi.subList(0, 20);
        }
        List<String> difference = new ArrayList<>(listFromApi);
        difference.removeAll(list);
        //в листе есть символ неразрывного пробела \u00A0
        listFromApi = listFromApi.stream().map(s -> s.replaceAll(" {2}", " "))
                .map(s -> s.replaceAll(" ", " "))
                .sorted()
                .collect(Collectors.toList());
        Allure.addAttachment("Сравнение списков на UI и в API", String.format("UI: \n%s\nAPI: \n%s", list, listFromApi));
        Assert.assertEquals(listFromApi, list, String.format("Списки на UI и API не совпали. Разница: %s", difference));
    }

    /**
     * Метод выполняет проверку корректности отображения отфильтроавнных сотрудников
     * Смотрим в апи какие сотрудники соотвевуют фильтру и сравниваем что отображается на UI
     */
    @Step("Проверка отображения результатов фильтрации сотрудников по должности \"{jobTitle.fullName}\" на UI в сравнении с API")
    private void compareEmployeeLists(List<Employee> listFromApi, JobTitle jobTitle) {
        LOG.info("Проверяем отображение результатов на UI в сравнении с API");
        List<String> list = getListOnUI();
        List<String> listFromApiAtName = listFromApi
                .stream()
                .map(Employee::getFullName)
                .collect(Collectors.toList());
        Allure.addAttachment("Сравнение списков на UI и в API", String.format("UI: \n%s\nAPI: \n%s", list, listFromApiAtName));
        List<String> difference = new ArrayList<>(CollectionUtils.disjunction(listFromApiAtName, list));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(difference.isEmpty(), String.format("Списки на UI и API не совпали. Разница: %s", difference));
        List<Employee> listFromWithoutPosition = listFromApi.stream()
                .filter(e -> !e.getPositions().stream().map(Position::getName).collect(Collectors.toList()).contains(jobTitle.getFullName()))
                .collect(Collectors.toList());
        softAssert.assertTrue(listFromWithoutPosition.isEmpty(),
                              String.format("Не все сотрудники из получившегося списка имеют должность \"%s\". Сотрудники без указанной должности: %s",
                                            jobTitle, listFromWithoutPosition));
        softAssert.assertAll();
    }

    @Step("Check filter did not work, compare list before filtr and after")
    private void checkFilterResetOnly(List<String> allResult) {
        List<String> currentList = new ArrayList<>(os.osSearchForm().allSearchResult().extract(WebElement::getText));
        Assert.assertEquals(currentList, allResult);
    }

    @Step("Выбрать тэг {tag}")
    private void choseCertainTags(String tagName) {
        os.tagsForm().tagByName(tagName).click();
    }

    @Step("Проверить изменение параметра \"{parameterName}\"")
    private void changeTheValuesOfTheDesiredParameters(String parameterName) {
        List<String> totalTags = os.tagsForm().listOfParametersText().extract(WebElement::getText);
        List<Integer> index = new ArrayList<>();
        for (int i = 0; totalTags.size() > i; i++) {
            if (totalTags.get(i).contains(parameterName)) {
                index.add(i);
            }
        }
        Assert.assertTrue(index.size() != 0, "Теги не были введены");
        for (int integer : index) {
            os.tagsForm().listOfTagsSearch().get(integer).click();
            if (parameterName.contains("Конверсия для расчета РЗ")) {
                double value = Math.random();
                os.tagsForm().listOfTagsSearch().get(integer).sendKeys(String.valueOf(value));
                Allure.addAttachment("Value", "text/plain", "Value: " + value);
            } else if (parameterName.contains("Производительность")) {
                Random random = new Random();
                int value = 10 + random.nextInt(41);
                os.tagsForm().listOfTagsSearch().get(integer).sendKeys(String.valueOf(value));
                Allure.addAttachment("Value", "text/plain", "Value: " + value);
            }
        }
    }

    @Step("Проверить удаление значения параметра \"{parameterName}\"")
    private void clearingTheValueFieldForTheDesiredParameters(String parameterName) {
        List<String> totalTags = os.tagsForm().listOfParametersText().extract(WebElement::getText);
        List<Integer> index = new ArrayList<>();
        for (int i = 0; totalTags.size() > i; i++) {
            if (totalTags.get(i).contains(parameterName)) {
                index.add(i);
            }
        }
        Assert.assertTrue(index.size() != 0, "Теги не были введены");
        for (int integer : index) {
            os.tagsForm().listOfTagsSearch().get(integer).click();
            if (os.tagsForm().listOfTagsSearch().get(integer).getText() != null) {
                os.tagsForm().listOfTagsSearch().get(integer).clear();
            }
        }
    }

    @Step("Проверить изменение телефона")
    private void assertPhoneEdited(int id, String value, boolean isEmployee) {
        SoftAssert softAssert = new SoftAssert();
        String[] phoneData = value.split(" ");
        softAssert.assertEquals(os.omInfoForm().phoneOrFaxNumberString("phone").getAttribute("value").replaceAll("[^0-9]+", ""),
                                phoneData[1].replaceAll("[^0-9]+", ""),
                                "Номер телефона на UI не соответствует ожидаемому");
        softAssert.assertEquals(os.omInfoForm().phoneTypeString().getText(), phoneData[0],
                                "Тип телефона на UI не соответствует ожидаемому");
        String entity = isEmployee ? EMPLOYEES : ORG_UNITS;
        JSONObject phone = getJsonFromUri(Projects.WFM, URL_ORG_STRUCTURE, makePath(entity, id, PHONES))
                .optJSONObject(EMBEDDED).optJSONArray(PHONES).optJSONObject(0);
        softAssert.assertNotNull(phone, "Номер и тип телефона отсутствуют в API");
        softAssert.assertTrue(phoneData[1].contains(phone.opt("number").toString()), "Номер телефона в API не соответствует ожидаемому");
        softAssert.assertEquals(phone.optJSONObject(TYPE).opt(NAME).toString(), phoneData[0], "Тип телефона в API не соответствует ожидаемому");
        softAssert.assertAll();
    }

    @Step("Проверить изменение факса")
    private void assertFaxEdited(int unitId, String value) {
        SoftAssert softAssert = new SoftAssert();
        String uiFax = os.omInfoForm().phoneOrFaxNumberString("fax").getAttribute("value");
        Allure.addAttachment("Fax", String.format("Fax на UI: %s;\n Введенный fax: %s", uiFax, value));
        softAssert.assertEquals(uiFax.replaceAll("[^0-9]+", ""), value.replaceAll("[^0-9]+", ""),
                                "Номер факса на UI не соответствует ожидаемому");
        String fax = getJsonFromUri(Projects.WFM, URL_ORG_STRUCTURE, makePath(ORG_UNITS, unitId))
                .optString("fax");
        softAssert.assertNotNull(fax, "Номер факса отсутствует в API");
        softAssert.assertEquals(fax, value.replaceAll("[^0-9]+", ""), "Номер телефона в API не соответствует ожидаемому");
        softAssert.assertAll();
    }

    @Step("Проверить изменение E-mail")
    private void assertEmailEdited(int id, String email, boolean isEmployee) {
        LOG.info("Проверяем изменение E-mail");
        String uiEmail = os.omInfoForm().emailString().getText();
        Allure.addAttachment("Email", String.format("Email на UI: %s;\n Введенный email: %s", uiEmail, email));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(email, uiEmail, "E-mail на UI не соответствует ожидаемому");
        String emailApi = isEmployee ? EmployeeRepository.getEmployee(id).getEmail()
                : getOrgUnit(id).getEmail();
        softAssert.assertEquals(email, emailApi, "E-mail в API не соответствует ожидаемому");
        softAssert.assertAll();
    }

    @Step("Проверить изменение адреса")
    private void assertAddressEdited(int id, String value, boolean isEmployee) {
        LOG.info("Проверяем изменение адреса");
        List<String> address = Arrays.stream(value.split(", ")).collect(Collectors.toList());
        SoftAssert softAssert = new SoftAssert();
        List<String> addressList = os.omInfoForm().addressStringList().stream().map(WebElement::getText).collect(Collectors.toList());
        softAssert.assertTrue(value.contains(addressList.get(addressList.size() - 1).replaceAll("ул. ", "")),
                              String.format("Адрес на UI не соответствует ожидаемому. Expected: %s, Actual: %s", value, addressList.get(addressList.size() - 1).replaceAll("ул.", "")));
        List<String> addressTypeList = os.omInfoForm().addressTypeStringList().stream().map(WebElement::getText).collect(Collectors.toList());
        softAssert.assertEquals(address.get(0), addressTypeList.get(addressTypeList.size() - 1),
                                "Тип адреса на UI не соответствует ожидаемому");
        String entity = isEmployee ? EMPLOYEES : ORG_UNITS;
        JSONObject obj = getJsonFromUri(Projects.WFM, URL_ORG_STRUCTURE, makePath(entity, id, ADDRESSES));
        Assert.assertTrue(!obj.isNull(EMBEDDED), "У объекта нет адресов в API");
        JSONArray addresses = obj.optJSONObject(EMBEDDED).optJSONArray(ADDRESSES);
        Assert.assertTrue(!addresses.isEmpty(), "Адрес отсутствует в API");
        //костыль, так как на zozo не удаляется адрес
        JSONObject addressApi = addresses.optJSONObject(addresses.length() - 1);
        softAssert.assertTrue(!addresses.isEmpty(), "Адрес отсутствует в API");
        softAssert.assertEquals(address.get(0), addressApi.optJSONObject(TYPE).opt(NAME).toString(), "Тип адреса в API не соответствует ожидаемому");
        softAssert.assertEquals(address.get(1), addressApi.opt("postalCode").toString(), "Индекс адреса в API не соответствует ожидаемому");
        softAssert.assertEquals(address.get(2), addressApi.opt("country").toString(), "Страна адреса в API не соответствует ожидаемому");
        softAssert.assertEquals(address.get(3), addressApi.opt("region").toString(), "Регион адреса в API не соответствует ожидаемому");
        softAssert.assertEquals(address.get(4), addressApi.opt("city").toString(), "Город адреса в API не соответствует ожидаемому");
        softAssert.assertEquals(address.get(5), addressApi.opt("addressStreet").toString(), "Улица адреса в API не соответствует ожидаемому");
        softAssert.assertEquals(address.get(6), addressApi.opt("building").toString(), "Номер строения в API не соответствует ожидаемому");
        softAssert.assertAll();
    }

    @Step("Нажать на кнопку \"Выбрать\" в форме тегов")
    private void clickSubmitTags() {
        os.tagsForm().choseButton().click();
        os.spinnerLoader().loadingSpinnerInForm()
                .waitUntil("Spinner endless loading", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Нажать на кнопку \"Выбрать\" на вкладке тэги")
    private void clickSaveParameters() {
        os.tagsForm().saveButton().click();
        os.spinnerLoader().loadingSpinnerInForm()
                .waitUntil("Spinner endless loading", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Проверяется , произошло ли переключение после нажатия кнопки Сотрудники")
    private void assertForChangeOnEmp() {
        Assert.assertEquals(os.osSearchForm().structureField().getText(), "Сотрудники",
                            "Переключение на вкладку \"Сотрудники\" не состоялось, мы на вкладке " +
                                    os.osSearchForm().structureField().getText());
    }

    /**
     * В поле поиска во вкладке Подразделения вводится строка чтобы ограничить список.
     */
    @Step("Вводится в строку рандомная часть OM ")
    private void sendRandomVariantIntoOMField(String randomValue) {
        os.osSearchForm().orgUnitSearchInput().sendKeys(randomValue);
        Allure.addAttachment("Value", "text/plain", "Список ОМ был ограничен: " + randomValue);
    }

    @Step("В поле для ввода после добавления текста нажать на клавишу \"Enter\"")
    private void sendEnterIntoSearchFieldForEmp() {
        os.osSearchForm().employeeSearchInput().sendKeys(Keys.ENTER);
    }

    @Step("В поле для ввода после добавления текста нажать на клавишу \"Enter\"")
    private void sendEnterIntoSearchFieldForOM() {
        os.osSearchForm().orgUnitSearchInput().sendKeys(Keys.ENTER);
    }

    @Step("Проверка на то что результаты поиска на UI и значения из API отсортированы в обратном порядке и совпадают")
    private void assertForOmSortedByRevertAlphabet(List<String> listOm) {
        int numberOfOM = os.osSearchForm().allSearchResult().size();
        List<String> revertListOM = new ArrayList<>();
        for (int i = 0; i <= listOm.size() - 1; i++) {
            String chr = String.valueOf(listOm.get(i).charAt(0));
            if (!chr.matches("^[A-Z]|[А-Я]")) {
                listOm.remove(listOm.get(i));
            }
        }
        listOm.subList(0, listOm.size() - numberOfOM).clear();
        for (int j = listOm.size() - 1; j >= 0; j--) {
            revertListOM.add(listOm.get(j));
        }
        List<String> uiList = new ArrayList<>();
        for (int i = 0; i <= numberOfOM - 1; i++) {
            uiList.add(os.osSearchForm().allSearchResult().get(i).getAttribute("innerText"));
        }
        Allure.addAttachment("Value", "text/plain",
                             String.format("ОМ на UI: %s\nОМ на API перевернутые %s", uiList, revertListOM));
        List<String> difference = new ArrayList<>(uiList);
        difference.removeAll(revertListOM);
        Assert.assertEquals(uiList, revertListOM,
                            "Отсортированные коллекции по алфавиту в обратном порядке не сходятся " + difference);
    }

    /**
     * Данный метод осуществляет нажатие на чекбоксы у определнных ОМ
     *
     * @param desiredOmIds - строный массв id нужных ОМ
     */
    @Step("Выбрать следующие ОМ из дерева: {omNames}")
    private void workWithTree(List<Integer> desiredOmIds, List<String> omNames) {
        os.filterOmForm().waitUntil("Форма \"Подразделения\" с деревом ОМ вкладка ОМ и сотрудники не отображена",
                                    Matchers.not(Matchers.emptyArray()), 5);
        TreeNavigate treeNavigate = new TreeNavigate(CommonRepository.getTreePath(desiredOmIds, URL_ORG_STRUCTURE));
        treeNavigate.workWithTree(os.filterOmForm(), Direction.DOWN);
    }

    private void setOrgUnitFilterInEmployeesTab(String orgUnitName) {
        os.osFilterForm().orgUnitFilter().click();
        os.filterOmForm().omSearchBar().click();
        slowSendKeys(os.filterOmForm().omSearchBar(), orgUnitName);
        os.filterOmForm().checkBoxButton(orgUnitName).click();
        os.filterOmForm().omOkButton().click();
    }

    @Step("Проверка того что у сотрудника логин был изменен на {login}")
    private void assertEmployeeLogin(int empId, String login) {
        JSONArray arrayUsers = CommonRepository.getUsers();
        String apiUserName = "";
        for (int i = 0; i < arrayUsers.length(); i++) {
            JSONObject tempObj = arrayUsers.getJSONObject(i);
            JSONObject links = tempObj.getJSONObject(LINKS);
            String href = links.getJSONObject(EMPLOYEE_JSON).getString(HREF);
            href = href.substring(href.lastIndexOf("/") + 1);
            if (href.equals(String.valueOf(empId))) {
                apiUserName = tempObj.get("username").toString();
                break;
            }
        }
        String logFromUi = os.osCardForm().testNewLogin().getText();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(apiUserName, login);
        softAssert.assertEquals(login, logFromUi);
        softAssert.assertAll();
    }

    @Step("Проверить выбор варианта \"{variant}\" в матпараметре \"{matchParameter}\" у сотрудника {name}")
    private void assertParamChanging(VariantsInMathParameters variant, String employeeId, MathParameters matchParameter, String name) {
        String path = makePath(EMPLOYEES, employeeId, MATH_PARAMETER_VALUES, matchParameter.getMathParamId());
        JSONObject someObject;
        boolean matcher;
        String e = null;
        switch (variant) {
            case OFF:
                someObject = getJsonFromUri(Projects.WFM, URL_ORG_STRUCTURE, path);
                matcher = someObject.getBoolean("value");
                Allure.addAttachment("Описание действий", "text/plain",
                                     String.format("В матпарметре \"%s\" у сотрудника %s был выбран вариант \"%s\"",
                                                   matchParameter, name, variant));
                Assert.assertFalse(matcher, String.format("Поле %s не изменилось на %s", matchParameter, variant));
                break;
            case ON:
                someObject = getJsonFromUri(Projects.WFM, URL_ORG_STRUCTURE, path);
                matcher = someObject.getBoolean("value");
                Allure.addAttachment("Описание действий", "text/plain",
                                     String.format("В матпарметре \"%s\" у сотрудника %s был выбран вариант \"%s\"",
                                                   matchParameter, name, variant));
                Assert.assertTrue(matcher, String.format("Поле \"%s\" не изменилось на \"%s\"",
                                                         matchParameter, variant));
                break;
            case INHERITED_VALUE:
                try {
                    someObject = getJsonFromUri(Projects.WFM, URL_ORG_STRUCTURE, path);
                } catch (AssertionError error) {
                    e = error.getMessage();
                }
                Allure.addAttachment("Описание действий", "text/plain",
                                     String.format("В матпараметре \"%s\" у сотрудника \"%s\" был выбран вариант \"%s\"",
                                                   matchParameter, name, variant));
                Assert.assertEquals(e, "expected [200] but found [404]",
                                    String.format("Поле \"%s\" не изменилось на %s\"", matchParameter, variant));
                break;
        }
    }

    @Step("Из выпадающего списка выбрать вариант \"{variant}\"")
    private void choseVariant(VariantsInMathParameters variant) {
        os.osCardForm().variantsInMatchParam(variant.getName()).waitUntil(DisplayedMatcher.displayed());
        LOG.info("Выбран статус \"{}\"", variant.getName());
        os.osCardForm().variantsInMatchParam(variant.getName()).click();
    }

    @Step("Нажать на стрелку в параметре из списка \"{matchParameter}\"")
    private void chooseMatchParam(MathParameters matchParameter) {
        os.osCardForm().matchParameters(matchParameter.getNameParam()).waitUntil(DisplayedMatcher.displayed());
        os.osCardForm().matchParameters(matchParameter.getNameParam()).click();
    }

    /**
     * Возвращает вариант выбора в матпараметре, отличный от текущего
     */
    private VariantsInMathParameters returnVariant(MathParameters matchParameter) {
        String currentStatus = os.osCardForm().matchParameters(matchParameter.getNameParam()).getAttribute("value");
        VariantsInMathParameters variant;
        do {
            variant = VariantsInMathParameters.getRandomVariant();
        } while (variant.getName().equals(currentStatus));
        return variant;
    }

    @Step("Проверить добавление значения \"{valueUI}\" для параметра \"{params.name}\"")
    private void assertParamAdding(String valueAPI, String valueUI, MathParameter params, Employee employee) {
        String path = makePath(EMPLOYEES, employee.getId(), MATH_PARAMETER_VALUES, params.getMathParameterId());
        os.osCardForm().employeeFieldsData(params.getName())
                .should("Введенное значение не отображается в параметрах", text(containsString(valueUI)), 10);
        String apiValue = "";
        try {
            JSONObject someObject = getJsonFromUri(Projects.WFM, URL_ORG_STRUCTURE, path);
            apiValue = someObject.getString("value");
            Allure.addAttachment("Проверка",
                                 String.format("В ходе проверки были сравнены значения параметра в апи: %s и введенного значения: %s",
                                               apiValue, valueUI));
        } catch (AssertionError e) {
            Assert.fail(String.format("Параметр \"%s\" не был добавлен сотруднику %s",
                                      params.getName(), employee.getFullName()));
        }
        Assert.assertEquals(apiValue, valueAPI, "Значение параметра не совпадает");
    }

    @Step("Ввести значение \"{value}\" в  пустое поле параметра \"{params}\"")
    private void enterParamValue(String paramName, String value) {
        Actions actions = new Actions(os.getWrappedDriver());
        actions.moveToElement(os.omInfoForm().paramInputField(paramName), 0, -100)
                .perform();
        actions.moveToElement(os.omInfoForm().paramInputField(paramName)).perform();
        os.omInfoForm().paramInputField(paramName).sendKeys(value);
    }

    @Step("Проверить, что на UI отображаются мат. параметры, на которые у пользователя разрешения")
    private void checkParamsVisibility(List<MathParameter> params) {
        for (MathParameter param : params) {
            os.omInfoForm().paramInputField(param.getShortName()).should(DisplayedMatcher.displayed());
        }
        Allure.addAttachment("Проверка отображения мат. параметров, на которые были выданы права",
                             String.format("Отображаются мат. параметры: %s",
                                           params
                                                   .stream()
                                                   .map(MathParameter::getShortName)
                                                   .collect(Collectors.toList())));
    }

    @Step("Выбрать значение \"{valueName}\" в поле параметра \"{paramName}\"")
    private void selectParamValue(String paramName, String valueName) {
        LOG.info("Выбираем значение \"{}\" в поле параметра \"{}\"", valueName, paramName);
        Actions actions = new Actions(os.getWrappedDriver());
        actions.moveToElement(os.omInfoForm().paramInputField(paramName), 0, -100)
                .perform();
        os.omInfoForm().paramInputField(paramName).click();
        systemSleep(1); //тесты падают до вызова этого метода
        os.buttonTableType(valueName).click();
    }

    @Step("Ввести значение \"{valueName}\" в поле параметра \"{paramName}\"")
    private void inputParamValue(String paramName, String valueName) {
        LOG.info("Вводим значение \"{}\" в поле параметра \"{}\"", valueName, paramName);
        new Actions(os.getWrappedDriver()).moveToElement(os.omInfoForm().paramInputField(paramName)).perform();
        os.omInfoForm().paramInputField(paramName).click();
        os.omInfoForm().paramInputField(paramName).sendKeys(valueName);
    }

    @Step("Проверка перехода в карточку сотрудника с именем {name}")
    private void goToEmployeeCardCheck(String name) {
        os.osCardForm().employeeNameField()
                .should("Имя в карточке и имя выбранного сотрудника не совпадает", text(containsString(name)), 5);
        os.osCardForm().userSelected()
                .should("Имя подсвеченное синим и имя выбранного сотрудника не совпадает", text(containsString(name)), 5);
    }

    /**
     * Собирает все оргюниты и выбирает случайный
     *
     * @return название оргюнита
     */
    private ImmutablePair<String, String> getRandomOrgName() {
        List<OrgUnit> orgUnitList = getOrgUnitsNotClosedAndAllType();
        Random random = new Random();
        int randomIndex = random.nextInt(orgUnitList.size());
        boolean isFullName = random.nextBoolean();
        String fullName = orgUnitList.get(randomIndex).getName();
        ImmutablePair<String, String> pair = new ImmutablePair<>(fullName, isFullName ? fullName
                : fullName.substring(fullName.length() - 5));
        Allure.addAttachment("Выбор вводимого названия оргюнита",
                             String.format("Полное название: %s, будет введено название: %s", fullName, pair.right));
        return pair;
    }

    @Step("В текстовом поле \"Теги\" нажать на крестик кнопки тега c названием \"{tag}\"")
    private void deleteTag(String tag) {
        os.omEditingForm().deleteButtonTag(tag.trim()).click();
    }

    @Step("Удалить все теги")
    private void deleteAllTags() {
        os.omEditingForm().tagDeleteButtons().forEach(AtlasWebElement::click);
    }

    private String getParamName(boolean withValueParam, OrgUnit orgUnit) {
        Map<String, String> tempMap = CommonRepository.getMathParameterValues(orgUnit.getId());
        List<String> paramNameEqualsEnum = new ArrayList<>();
        for (int i = 0; i < (ParamName.values().length); i++) {
            String name = (ParamName.values()[i]).getName();
            if (tempMap.containsKey(name)) {
                paramNameEqualsEnum.add(name);
            }
        }
        List<String> paramNameWithout = new ArrayList<>();
        String targetParamName;
        if (!withValueParam) {
            if (paramNameEqualsEnum.size() == 0) {
                targetParamName = (ParamName.values()[new Random()
                        .nextInt(ParamName.values().length)]).getName();
            } else if (paramNameEqualsEnum.size() < ParamName.values().length) {
                for (int i = 0; i < ParamName.values().length; i++) {
                    if (!paramNameEqualsEnum.contains((ParamName.values()[i]).getName())) {
                        paramNameWithout.add((ParamName.values()[i]).getName());
                    }
                }
                targetParamName = String.valueOf(getRandomFromList(paramNameWithout));
            } else {
                targetParamName = PresetClass.makeClearParam(orgUnit);
            }
        } else {
            if (paramNameEqualsEnum.size() > 0) {
                targetParamName = String.valueOf(getRandomFromList(paramNameEqualsEnum));
            } else {
                String paramName = getParamName(false, orgUnit);
                String rndNumber = String.valueOf(new Random().nextInt(1000));
                sendInTargetParamInput(paramName, rndNumber);
                saveParameterChanges();
                targetParamName = paramName;
            }
        }
        LOG.info("Выбран параметр \"{}\"", targetParamName);
        return targetParamName;
    }

    @Step("Ввести в окно ввода параметра \"{paramName}\" текст \"{textToSend}\"")
    private void sendInTargetParamInput(String paramName, String textToSend) {
        LOG.info("Вводим в окно ввода параметра \"{}\" текст \"{}\"", paramName, textToSend);
        os.tagsForm().paramNameInput(paramName).clear();
        os.tagsForm().paramNameInput(paramName).sendKeys(textToSend);
    }

    @Step("Нажать на произвольный неактивированный чекбокс")
    private void clickOnAnySkillCheckBox() {
        List<AtlasWebElement> temp = new ArrayList<>(os.employeeData().freeCheckBoxList()
                                                             .waitUntil(Matchers.hasSize(Matchers.greaterThan(0))));
        getRandomFromList(temp).click();
    }

    @Step("Сохранить изменение параметров")
    private void saveParameterChanges() {
        LOG.info("Сохраняем изменение параметров");
        os.tagsForm().saveParamButton().click();
    }

    @Step("Сохранить изменение параметров сотрудника")
    private void saveEmployeeParameters() {
        os.employeeData().employeeParametersMenu().saveParamButton()
                .waitUntil("Кнопка \"Сохранить не загрузилась\"", DisplayedMatcher.displayed(), 10);
        os.employeeData().employeeParametersMenu().saveParamButton().click();
        os.employeeData().employeeParametersMenu().waitUntil("Форма редактирования мат параметров сотрудника не закрылась",
                                                             Matchers.not(DisplayedMatcher.displayed()), 10);

    }

    @Step("Проверить, что значение параметра \"{param.shortName}\" было изменено на \"{newValue}\"")
    private <T> void assertParameterChange(MathParameter param, MathParameterEntities entity, int entityId, T newValue, T oldValue) {
        os.tagsForm().waitUntil(Matchers.not(DisplayedMatcher.displayed()));
        MathParameterValue<T> value = MathParameterValueRepository.getMathParameterValueForEntity(entity, entityId, param);
        SoftAssert softAssert = new SoftAssert();
        T actualValue = value.getValue();
        if (actualValue instanceof Double) {
            softAssert.assertEquals(((Double) actualValue).intValue(), Integer.parseInt(newValue.toString()), "Параметр не был изменен");
        } else if (actualValue instanceof BigDecimal) {
            softAssert.assertEquals(((BigDecimal) actualValue).intValue(), Integer.parseInt(newValue.toString()), "Параметр не был изменен");

        } else {
            softAssert.assertEquals(actualValue, newValue, "Параметр не был изменен");
        }
        softAssert.assertNotEquals(actualValue, oldValue, "Параметр такой же, какой был до изменения значения");
        softAssert.assertAll();
        Allure.addAttachment("Изменение параметра",
                             String.format("Значение параметра %s было успешно изменено с значения %s на значение %s",
                                           param.getShortName(), oldValue, newValue));
    }

    @Step("Нажать на день с выбранным типом, который по счету {dayNumber}")
    private void clickOnDayTypeChangeButton(int dayNumber) {
        os.omInfoForm().daysTypes().waitUntil(Matchers.hasSize(Matchers.greaterThan(1)));
        os.omInfoForm().dayType(dayNumber).click();
        LOG.info("Нажали на {} по счёту день с выбранным типом", dayNumber);
    }

    private Days getAnotherDayType(DayOfWeek dayOfWeek) {
        Days current = Days.getByName(os.omInfoForm().dayType(dayOfWeek.getValue()).getAttribute("value"));
        return current == Days.DAY ? Days.DAY_OFF : Days.DAY;
    }

    @Step("Нажать на кнопку дня с типом \"{days}\"")
    private void switchDayTypeTo(Days days) {
        //todo добавить try-catch на случай бага с выпадающими списками?
        os.omInfoForm().dayTypeButton(days.getNameOfDay()).waitUntil(DisplayedMatcher.displayed());
        systemSleep(2); //без ожидания клик не срабатывает
        os.omInfoForm().dayTypeButton(days.getNameOfDay()).click();
        LOG.info("Нажали на кнопку дня с типом {}", days);
    }

    @Step("Проверка изменения типа дня. Номер дня: {dayNumber}, id расписания: {scheduleId}, тип дня: {days}")
    private void switchDayCheck(int dayNumber, String scheduleId, Days days) {
        Map<Integer, String> temp = CommonRepository.getWorkingDays(scheduleId);
        switch (days) {
            case DAY:
                Assert.assertNotNull(temp.get(dayNumber), "День все еще числится как выходной");
                break;
            case DAY_OFF:
                Assert.assertNull(temp.get(dayNumber), "День все ещё числится как рабочий");
        }
        Allure.addAttachment("Проверка", "Тип дня был успешно изменен на: " + days);
    }

    @Step("Проверить, что тип дня изменился на {days.nameOfDay}")
    private void changingDayTypeCheck(DayOfWeek dayOfWeek, Days days) {
        String name = dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru"));
        String capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
        if (days == Days.DAY_OFF) {
            os.omInfoForm().dayTypeField(capitalized).should(text(containsString(days.getNameOfDay())));
        } else {
            os.omInfoForm().dayTypeField(capitalized).should(Matchers.not(text(containsString(days.getNameOfDay()))));
        }
    }

    @Step("Проверить изменение даты закрытия ОргЮнита на {sendingDate}")
    private void assertChangeCloseDate(LocalDate sendingDate) {
        Locale locale = new Locale("ru");
        String expectedDate = sendingDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale));
        os.omInfoForm().omClosedDate().should("Дата не совпадает с ожидаемой", text(containsString(expectedDate)));
    }

    @Step("Проверить, что дата закрытия не изменилась")
    private void assertDateEndDidNotChange() {
        os.omInfoForm().omClosedDate().should("Дата отображается", Matchers.not(DisplayedMatcher.displayed()));
    }

    @Step("Проверить изменение даты открытия ОргЮнита")
    private void assertChangingOmStartedDate(LocalDate date, OrgUnit orgUnit) {
        Locale locale = new Locale("ru");
        String expectedDate = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale));
        os.omInfoForm().omStartDate().should("Дата не совпадает с ожидаемой",
                                             text(Matchers.equalToIgnoringWhiteSpace(expectedDate)));
        LocalDate startDate = orgUnit.refreshOrgUnit().getDateInterval().startDate;
        Assert.assertEquals(date, startDate);
    }

    @Step("Проверить изменение заместителя на {deputyName}")
    private void assertChangingDeputy(String deputyName, LocalDate startDate, LocalDate endDate) {
        Locale locale = new Locale("ru");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", locale);
        os.omInfoForm().omDeputyStartDate()
                .should("Отображаемая дата начала работы заместителя не соответствует ожидаемой",
                        text(containsString(startDate.format(formatter))));
        os.omInfoForm().omDeputyEndDate()
                .should("Отображаемая дата конца работы заместителя не соответствует ожидаемой",
                        text(containsString(endDate.format(formatter))));
        os.omInfoForm().omDeputyName().should("Отображаемый заместитель не изменился", text(containsString(deputyName)));
    }

    @Step("Проверка того что поле, \"{dateType.name}\" подсвечивается красным и появляется предупреждение \"Некорректная дата\"")
    private void assertHighlightInRedAndWrongDateMessage(DateTypeField dateType) {
        String statusField = os.omEditingForm().omDateArea(dateType.getName()).getAttribute("class");
        Assert.assertTrue(statusField.contains("invalid"), "Поле ввода не подсветилось красным.");
        os.omEditingForm().omDateError(dateType.getName())
                .should("Под полем ввода не отобразилось сообщение о некорректной дате.",
                        text(containsString("Некорректная дата")), 5);
    }

    @Step("Проверка того что форма редактирования графика не закрывается, поля \"Дата открытия\" и \"Тип\" " +
            "подсвечиваются красным цветом, у обоих отображается предупреждение \"Поле не может быть пустым\"")
    private void assertErrorField() {
        os.selectScheduleForm().should("Форма редактирования графика закрылась", DisplayedMatcher.displayed());
        os.selectScheduleForm().errorFieldByType(FieldType.DATE_OPEN.getNameOfType())
                .should("Поле \"Дата открытия\" не подсветилось красным цветом", DisplayedMatcher.displayed(), 5);
        os.selectScheduleForm().errorFieldByType(FieldType.TYPE.getNameOfType())
                .should("Поле \"Тип\" не подсветилось красным цветом", DisplayedMatcher.displayed(), 5);
        String errorText = "Поле не может быть пустым";
        SoftAssert softAssert = new SoftAssert();
        String classNameDateOpen = os.selectScheduleForm().errorFieldByType(FieldType.DATE_OPEN.getNameOfType()).getAttribute("class");
        String textDateOpenErrorElement = os.selectScheduleForm().textFieldError(classNameDateOpen).getText();
        softAssert.assertEquals(textDateOpenErrorElement, errorText,
                                "Текст ошибки поля \"Дата открытия\" не соответствует ожидаемому");
        String classNameType = os.selectScheduleForm().errorFieldByType(FieldType.TYPE.getNameOfType()).getAttribute("class");
        String textTypeErrorElement = os.selectScheduleForm().textFieldError(classNameType).getText();
        softAssert.assertEquals(textTypeErrorElement, errorText,
                                "Текст ошибки поля \"Тип\" не соответствует ожидаемому");
        softAssert.assertAll();
    }

    /**
     * Выбирает дату окончания работы в соответствии с параметром и с учетом даты начала работы сотрудника
     *
     * @param employee     - сотрудник
     * @param timePosition - где должна быть дата увольнения
     *                     PAST - в прошлом
     *                     FUTURE - в будущем
     *                     DEFAULT - сегодня
     */
    private LocalDate getEndDateForEmployeeFromTimePosition(Employee employee, ShiftTimePosition timePosition) {
        LocalDate localDate = LocalDate.now();
        Random random = new Random();
        switch (timePosition) {
            case DEFAULT:
                break;
            case PAST:
                if (employee.getStartWorkDate().isEqual(localDate)) {
                    break;
                }
                LocalDate rndBefore = localDate.minusDays(random.nextInt(100) + 1);
                LocalDate startWorkDate = employee.getStartWorkDate();
                while (rndBefore.isBefore(startWorkDate)) {
                    rndBefore = localDate.minusDays(random.nextInt(100) + 1);
                }
                localDate = rndBefore;
                break;
            case FUTURE:
                localDate = localDate.plusDays(random.nextInt(100) + 1);

        }
        return localDate;
    }

    @Step("Проверка удаления тега \"{deleted}\" у сотрудника")
    private void assertDeleteTag(Employee employee, List<String> tagsBefore, String deleted) {
        try {
            List<String> tagsOnUI = Arrays.asList(os.osCardForm().employeeTags().getText().split(", "));
            Assert.assertEquals(tagsOnUI.size(), tagsBefore.size() - 1,
                                "Количество отображаемых тегов не совпадает с ожидаемым");
            Assert.assertFalse(tagsOnUI.contains(deleted), "Удаленный тег отображается у сотрудника");
        } catch (org.openqa.selenium.NoSuchElementException e) {
            LOG.info("На UI теги не отображаются значит тегов у сотрудника нет.");
        }
        List<String> tagsAfter = employee.getActualTags();
        Assert.assertEquals(tagsAfter.size(), tagsBefore.size() - 1,
                            "Количество тегов в апи не совпадает с ожидаемым");
        Assert.assertFalse(tagsAfter.contains(deleted), "Выбранный тег не удалился из апи");
    }

    @Step("В поле \"Теги\" добавить тег \"{tag}\"")
    private void sendTagForEmployee(String tag) {
        os.osCardForm().employeeFieldByType(EmpFields.TAGS.getFieldName()).sendKeys(tag);
        if (!os.osCardForm().tagsVariantWhenSending().isEmpty()) {
            os.osCardForm().tagsVariantWhenSending().stream().filter(e -> e.getText().equals(tag)).findFirst()
                    .orElseThrow(() -> new AssertionError(String.format("Тега \"%s\" нет в списке при вводе", tag)))
                    .click();
        }
    }

    @Step("В поле \"Теги\" удалить тег \"{tag}\"")
    private void deleteTagForEmployee(String tag) {
        os.osCardForm().removeTagButton(tag).click();
    }

    @Step("Проверка добавления сотруднику тега \"{addedTag}\"")
    private void assertAddTag(Employee employee, List<String> tagsBefore, String addedTag) {
        List<String> tagsOnUI = Arrays.asList(os.osCardForm().employeeTags().getText().split(", "));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(tagsOnUI.size(), tagsBefore.size() + 1,
                                "Количество отображаемых тегов не совпадает с ожидаемым");
        softAssert.assertTrue(tagsOnUI.contains(addedTag), "Выбранный тег не отображается у сотрудника на UI");
        List<String> tagsAfter = employee.getActualTags();
        softAssert.assertEquals(tagsAfter.size(), tagsBefore.size() + 1,
                                "Количество тегов в апи не совпадает с ожидаемым");
        softAssert.assertTrue(tagsAfter.contains(addedTag), "Выбранный тег не добавился в апи");
        softAssert.assertAll();
        Allure.addAttachment("Список тегов до теста", "text/plain", String.valueOf(tagsBefore));
        Allure.addAttachment("Список тегов после теста", "text/plain", String.valueOf(tagsAfter));
    }

    /**
     * В поле поиска во вкладке "Сотрудники" вводится строка, чтобы ограничить список.
     */
    @Step("Выбрать сотрудника {name} ")
    private void chooseEmployee(String name) {
        os.osSearchForm().employeeSearchInput().click();
        os.osSearchForm().employeeSearchInput().sendKeys(name + Keys.ENTER);
        os.osSearchForm().allSearchResult().get(0).click();
        //элемент может перестать отображаься, но он не изменит свой атрибут до тех пор, пока карточка сотрудника не загрузится полностью
        os.spinnerLoader().loadingSpinnerInFormEmployee().waitUntil("Карточка сотрудника не загрузилась", Matchers.hasSize(Matchers.greaterThan(0)), 10);
        LOG.info("Выбираем сотрудника {}", name);
    }

    private void clearEmployeeSearchField() {
        os.osSearchForm().employeeSearchInput().click();
        os.osSearchForm().employeeSearchInput().clear();
    }

    @Step("Выбрать роль \"{role.name}\" в поле выбора №{roleOrder}.")
    private void pickRole(UserRole role, int roleOrder) {
        os.editRoleForm().roleEditBlock(roleOrder).roleInput().click();
        systemSleep(1); //ожидание раскрытие списка
        os.editRoleForm().roleEditBlock(roleOrder).roleNameButton(role.getName()).click();
        LOG.info("Выбрали роль \"{}\"", role.getName());
    }

    @Step("Выбрать оргюнит \"{unitName.name}\" для роли \"{roleName}\"")
    private void pickOrgUnit(int order, String roleName, OrgUnit... unitName) {
        AtlasWebElement topChevron = os.filterOmForm().topChevron();
        os.editRoleForm().chooseOrgUnitButton(order).click();
        String firstChevronStatus = topChevron.getAttribute("class");
        if (firstChevronStatus.contains("chevron-up")) {
            topChevron.click();
        }
        workWithTree(Stream.of(unitName).map(OrgUnit::getId).collect(Collectors.toList()),
                     Stream.of(unitName).map(OrgUnit::getName).collect(Collectors.toList()));
    }

    @Step("Выбрать оргюнит \"{unitName.name}\"")
    private void pickOrgUnitString(int order, OrgUnit... unitName) {
        os.editRoleForm().chooseOrgUnitButton(order).click();
        List<String> unitNames = Stream.of(unitName).map(OrgUnit::getName).collect(Collectors.toList());
        for (String omName : unitNames) {
            os.filterOmForm().omSearchBar().sendKeys(omName);
            os.filterOmForm().checkBoxButton(omName).waitUntil("Чекбокс подразделения не отображается",
                                                               DisplayedMatcher.displayed(), 5);
            if (!os.filterOmForm().checkBoxButton(omName).isSelected()) {
                os.filterOmForm().checkBoxButton(omName).click();
            }
            os.filterOmForm().omClear().click();
        }
        LOG.info(String.format("Быди выбраны следующие оргюниты: %s", unitNames));
        Allure.addAttachment("Выбор оргюнитов", String.format("Быди выбраны следующие оргюниты: %s", unitNames));
    }

    @Step("Выбрать {date} в поле \"{dateType.name}\" для роли №{order}")
    private void setRoleDate(LocalDate date, DateTypeField dateType, int order) {
        os.editRoleForm().roleEditBlock(order).calendarButton(dateType.getName()).click();
        datePickRoleLogic(date);
        LOG.info("Установили дату {} начала действия роли", date);
    }

    @Step("Ввести {date} в поле {dateType.name} для роли №{roleName}")
    private void sendRoleDate(String date, DateTypeField dateType, int order) {
        os.editRoleForm().roleEditBlock(order).calendarInput(dateType.getName()).sendKeys(date);
        os.editRoleForm().roleEditBlock(order).calendarInput(dateType.getName()).sendKeys(Keys.ENTER);
    }

    /**
     * Инкапсуляция логики работы с выбором даты
     *
     * @param date - дата
     */
    private void datePickRoleLogic(LocalDate date) {
        DatePicker datePicker = new DatePicker(os.datePickerFormInEmployee());
        datePicker.pickDate(date);
        datePicker.okButtonClick();
    }

    @Step("Нажать на кнопку \"Изменить\"")
    private void pressSaveButton() {
        os.editRoleForm().cancelButton().sendKeys(Keys.TAB);
        os.editRoleForm().saveButton().sendKeys(Keys.TAB);
        os.editRoleForm().saveButton().sendKeys(Keys.ENTER);
    }

    @Step("Нажать на кнопку \"Отменить\"")
    private void pressCancelButton() {
        os.editRoleForm().cancelButton().click();
    }

    @Step("Нажать на значок \"Карандаша\" для редактирования информации Роли")
    private void pressRoleDataPencil() {
        new Actions(os.getWrappedDriver()).moveToElement(os.editRoleForm().pencilButton()).perform();
        os.editRoleForm().pencilButton().click();
    }

    @Step("Проверка добавления роли \"{userRole.name}\" сотруднику {employee.lastName}")
    private void assertAddRole(Employee employee, User user, UserRole userRole, List<OrgUnit> orgUnits, LocalDate start, LocalDate end) {
        os.editRoleForm().spinner()
                .waitUntil("Спиннер все еще не исчез", Matchers.not(DisplayedMatcher.displayed()), 10);
        SoftAssert softAssert = new SoftAssert();
        String patternFormat = "d MMMM yyyy";
        Locale localeRu = new Locale("ru", "RU");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(patternFormat).withLocale(localeRu);
        int order = determineRoleOrderNumber(userRole.getName()) + 1;
        try {
            String dateStartText = os.editRoleForm().roleViewBlock(order).dateStartValue().getText();
            LocalDate dateStart = LocalDate.parse(dateStartText, formatter);
            softAssert.assertEquals(dateStart, start, "Дата начала роли на UI и введенной даты не совпадает");
        } catch (org.openqa.selenium.NoSuchElementException e) {
            softAssert.assertNull(start);
        }
        if (end != null) {
            LocalDate dateEnd;
            String dateEndText = os.editRoleForm().roleViewBlock(order).dateEndValue().getText();
            if (dateEndText.equals("")) {
                dateEnd = null;
            } else {
                dateEnd = LocalDate.parse(dateEndText, formatter);
            }
            softAssert.assertEquals(dateEnd, end, "Дата конца роли на UI и введенной даты не совпадает");
        }

        String role = os.editRoleForm().roleViewBlock(order).roleTitle().getText();
        //Сортировка добавлена на случай, если оргюниты в списках будут одинаковые, но расположенные в разном порядке, см. https://builder.goodt.me/view/TestJob/job/WFM_blank3/5/allure
        List<String> orgNamesActual = os.editRoleForm().roleViewBlock(order).valuesOrgUnits().stream()
                .map(WebElement::getText).sorted().collect(Collectors.toList());
        List<String> orgNamesExpected = orgUnits.stream().map(OrgUnit::getName).sorted().collect(Collectors.toList());
        softAssert.assertEquals(role, userRole.getName(), "Название роли на UI и введенной роли не совпадают");
        softAssert.assertEquals(orgNamesActual, orgNamesExpected, "Оргюнит на UI и введенный оргюнит не совпадают");

        List<User.RoleInUser> rolesBefore = user != null ? user.getRoles() : new ArrayList<>();
        List<User.RoleInUser> rolesAfter = employee.getUser().getRoles();
        rolesAfter.removeAll(rolesBefore);
        softAssert.assertEquals(rolesAfter.size(), 1);
        User.RoleInUser newRole = rolesAfter.iterator().next();
        Set<Integer> newRoleOrgUnitId = new HashSet<>(newRole.getOrgUnitList());
        Set<Integer> expectedRoleOrgUnitId = orgUnits.stream().map(OrgUnit::getId).collect(Collectors.toSet());

        softAssert.assertEquals((Integer) newRole.getUserRoleId(), userRole.getId(), "Роль в API не совпадает с введенной ролью");
        softAssert.assertEquals(newRole.getStartRoleDate(), start, "Дата начала роли в API и введенной даты не совпадает");
        softAssert.assertEquals(newRole.getEndRoleDate(), end, "Дата конца роли в API и введенной даты не совпадает");
        softAssert.assertEquals(newRoleOrgUnitId, expectedRoleOrgUnitId, "Оргюнит в API и введеный оргюнит не совпадают");
        softAssert.assertAll();
    }

    @Step("Проверка отмены добавления роли")
    private void assertCancelAddRole(Employee employee, User user) {
        os.editRoleForm().spinner()
                .waitUntil("Спиннер все еще не исчез", Matchers.not(DisplayedMatcher.displayed()), 10);
        SoftAssert softAssert = new SoftAssert();
        String patternFormat = "d MMMM yyyy";
        Locale localeRu = new Locale("ru", "RU");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(patternFormat).withLocale(localeRu);
        List<User.RoleInUser> before = new ArrayList<>();
        if (user != null) {
            before = user.getRoles();
        }
        List<User.RoleInUser> after = new ArrayList<>();
        User employeeUser = employee.getUser();
        if (employee.getUser() != null) {
            after = employeeUser.getRoles();
        }
        Assert.assertEquals(before.size(), after.size(), "Количество ролей у пользователя в апи изменилось");
        for (int i = 0; i < before.size(); i++) {
            User.RoleInUser roleInUserBefore = before.get(i);
            String role = os.editRoleForm().roleViewBlock(i + 1).roleTitle().getText();
            String dateStartText = os.editRoleForm().roleViewBlock(i + 1).dateStartValue().getText();
            LocalDate dateStart = LocalDate.parse(dateStartText, formatter);
            LocalDate dateEnd;
            try {
                String dateEndText = os.editRoleForm().roleViewBlock(i + 1).dateEndValue().getText();
                if (dateEndText.equals("") || dateEndText.equals("Настоящее время")) {
                    dateEnd = null;
                } else {
                    dateEnd = LocalDate.parse(dateEndText, formatter);
                }
            } catch (org.openqa.selenium.NoSuchElementException e) {
                dateEnd = null;
            }
            UserRole userRole = UserRoleRepository.getUserRoleById(roleInUserBefore.getUserRoleId());

            softAssert.assertEquals(role, userRole.getName(), "Название роли на UI и введенной роли не совпадают");
            softAssert.assertEquals(dateStart, roleInUserBefore.getStartRoleDate(),
                                    "Дата начала роли на UI и введенной даты не совпадает");
            softAssert.assertEquals(dateEnd, roleInUserBefore.getEndRoleDate(),
                                    "Дата конца роли на UI и введенной даты не совпадает");
            softAssert.assertEquals(roleInUserBefore, after.get(i));
        }
    }

    private int findRoleOrderByName(String roleName) {
        ElementsCollection<AtlasWebElement> allRoles = os.editRoleForm().allRoleInputs();
        int order = -1;
        for (int i = 0; i < allRoles.size(); i++) {
            String name = allRoles.get(i).getAttribute("value").trim();
            if (name.equals(roleName)) {
                order = i + 1;
                break;
            }
        }
        return order;
    }

    @Step("Нажать на кнопку удаления роли \"{roleName}\"")
    private void pressDeleteRoleButton(int order, String roleName) {
        waitForClickable(os.editRoleForm().roleEditBlock(order).roleDeleteButton(), os, 10);
        os.editRoleForm().roleEditBlock(order).roleDeleteButton().click();
    }

    @Step("Удалить все оргюниты для роли \"{roleName}\"")
    private void deleteAllOrgUnitsInRole(int order, String roleName) {
        os.editRoleForm().roleEditBlock(order).orgUnitDeleteButton().forEach(WebElement::click);
        try {
            while (os.editRoleForm().roleEditBlock(order).seeMoreButton().isDisplayed()) {
                os.editRoleForm().roleEditBlock(order).seeMoreButton().click();
                os.editRoleForm().roleEditBlock(order).orgUnitDeleteButton().forEach(WebElement::click);
            }
        } catch (NoSuchElementException e) {
            LOG.info("Кнопка \"Больше\" из списка оргюнитов для роли не отображается.");
        }
    }

    private List<String> getFreeEmployeeFromUi() {
        return os.addNewEmployeeForm().listOfFreeEmployees().stream()
                .map(AtlasWebElement::getText).collect(Collectors.toList());
    }

    @Step("Проверка удаления роли \"{role.name}\" у сотрудника {employee.lastName}")
    private void assertDeleteRole(Employee employee, User userBefore, UserRole role) {
        os.editRoleForm().spinner()
                .waitUntil("Спиннер все еще не исчез", Matchers.not(DisplayedMatcher.displayed()), 10);
        SoftAssert softAssert = new SoftAssert();
        User userAfter = employee.getUser();
        List<User.RoleInUser> rolesBefore = userBefore.getRoles();
        List<User.RoleInUser> rolesAfter = userAfter.getRoles();
        rolesAfter.removeAll(rolesBefore);
        softAssert.assertTrue(rolesAfter.isEmpty(), "В апи остались лишние роли");

        List<AtlasWebElement> rolesTitles = os.editRoleForm().roleTitles();
        softAssert.assertEquals(rolesTitles.size(), userBefore.getRoles().size() - 1, "На UI осталась лишняя роль");
        List<String> rolesOnUI = rolesTitles.stream().map(WebElement::getText).collect(Collectors.toList());
        softAssert.assertFalse(rolesOnUI.contains(role.getName()), "Название удаленной роли отображается на UI");
        softAssert.assertAll();
    }

    @Step("Проверка что поле \"Подразделение\" подсвечивается красным, форма редактирования роли не закрывается.")
    private void assertCantSaveWithOutRole(int order) {
        os.editRoleForm().roleEditBlock(order).redLineInRoleInput().should("Красная подсветка строки ввода оргюнита не отображается",
                                                                           DisplayedMatcher.displayed(), 5);
        os.editRoleForm().saveButton().should("Форма редактирования роли закрылась",
                                              DisplayedMatcher.displayed(), 10);
    }

    @Step("Проверка что поле \"Даты\" подсвечивается красным, форма редактирования роли не закрывается.")
    private void assertCantSaveWithWrongDate(LocalDate startDate, int order) {
        String date = startDate.toString();
        os.editRoleForm().roleEditBlock(order).redTextUnderDate().should("Красная подсветка строки ввода оргюнита не отображается",
                                                                         text(containsString(String.format("Должна быть не ранее, чем %s 24:00:00", date))), 5);
        os.editRoleForm().saveButton().should("Форма редактирования роли закрылась",
                                              DisplayedMatcher.displayed(), 10);
    }

    @Step("Проверка что поле \"Даты\" подсвечивается красным, форма редактирования роли не закрывается.")
    private void assertWrongDate(int order) {
        os.editRoleForm().roleEditBlock(order).redTextUnderDate().should("Красная подсветка строки ввода оргюнита не отображается",
                                                                         text(containsString("Некорректная дата")), 5);
    }

    @Step("Проверка того что форма закрывается и никаких изменений не происходит")
    private void assertNothingHappened() {
        os.editRoleForm().saveButton()
                .should("Форма редактирования не закрылась", Matchers.not(DisplayedMatcher.displayed()), 10);
        os.editRoleForm().titleInRoleBlock()
                .should("Текст в форме не совпадает с ожидаемым", text(containsString("Данные отсутствуют")), 5);
    }

    @Step("Проверка что поле \"Даты\" подсвечивается красным, форма редактирования роли не закрывается.")
    private void assertCantSaveWithoutDate(int order) {
        os.editRoleForm().roleEditBlock(order).redTextUnderDate().should("Красная подсветка строки \"Даты начала\" не отображается",
                                                                         text(containsString("Поле не может быть пустым")), 5);
        os.editRoleForm().saveButton().should("Форма редактирования роли закрылась",
                                              DisplayedMatcher.displayed(), 10);
    }

    @Step("Проверка того что форма выбора подразделений закрылась, подразделения не выбраны")
    private void assertResetSelection(Employee employee, User user, int order) {
        os.filterOmForm().should("Форма выбора подразделений открыта",
                                 Matchers.not(DisplayedMatcher.displayed()), 5);
        os.editRoleForm().chosenOrgUnitText(order)
                .should("Отобразился статус выбора оргюнита",
                        Matchers.not(DisplayedMatcher.displayed()), 5);
        User employeeUser = employee.getUser();
        Assert.assertTrue((employeeUser == null && user == null) || (employeeUser != null && employeeUser.equals(user)),
                          "Пользователь поменял свои значения. Хотя изменения не сохранялись.");
    }

    @Step("Проверка изменения роли с названием {roleNameAfter}")
    private void assertChangeRole(List<UserRole> rolesBefore, String roleNameAfter, String roleDescription) {
        SoftAssert softAssert = new SoftAssert();
        os.roleForm().roleName().waitUntil("Название роли не отобразилось", DisplayedMatcher.displayed(), 10);
        String roleNameUI = os.roleForm().roleName().getText();
        String roleNameDescription = os.roleForm().roleDescription().getText();
        softAssert.assertEquals(roleNameUI, roleNameAfter, "Название роли на UI и введенное не совпадают");
        softAssert.assertEquals(roleNameDescription, roleDescription, "Описание роли на UI и введенное не совпадают");
        List<UserRole> userRolesAfter = UserRoleRepository.getUserRoles();
        softAssert.assertEquals(userRolesAfter.size(), rolesBefore.size(), "Количество ролей изменилось");
        userRolesAfter.removeAll(rolesBefore);
        UserRole addedRole = userRolesAfter.iterator().next();
        softAssert.assertEquals(userRolesAfter.size(), 1, "Количество измененных ролей не совпадает с ожидаемым.");
        softAssert.assertEquals(addedRole.getName(), roleNameAfter, "Название роли в АPI и введенное не совпадают");
        softAssert.assertEquals(addedRole.getDescription(), roleDescription, "Описание роли в АPI и введенное не совпадают");
        softAssert.assertAll();
    }

    @Step("Проверить удаление роли с названием \"{roleName}\"")
    private void assertDeleteRole(List<UserRole> rolesBefore, String roleName) {
        os.spinnerLoader().popUp().waitUntil("", Matchers.not(DisplayedMatcher.displayed()), 10);
        SoftAssert softAssert = new SoftAssert();
        boolean anyMatch = os.osSearchForm().allSearchResult().stream()
                .anyMatch(extendedWebElement -> extendedWebElement.getText().equals(roleName));
        softAssert.assertFalse(anyMatch, "Роль не была удалена из списка на UI");
        List<UserRole> userRolesAfter = UserRoleRepository.getUserRoles();
        rolesBefore.removeAll(userRolesAfter);
        UserRole deletedRole = rolesBefore.iterator().next();
        softAssert.assertEquals(rolesBefore.size(), 1, "Количество ролей не совпадает с ожидаемым.");
        softAssert.assertEquals(deletedRole.getName(), roleName,
                                String.format("Была удалена роль с названием \"%s\", a должна быть удалена роль: \"%s\"",
                                              deletedRole.getName(), roleName));
        Allure.addAttachment("Проверка", String.format("Была удалена роль с названием \"%s\"", deletedRole.getName()));
        softAssert.assertAll();
    }

    @Step("Ввести в поле название роли \"{roleName}\"")
    private void sendRoleName(String roleName) {
        LOG.info("Вводим в поле название роли \"{}\"", roleName);
        waitForClickable(os.roleForm().roleNameInput(), os, 10);
        os.roleForm().roleNameInput().clear();
        os.roleForm().roleNameInput().sendKeys(roleName);
    }

    @Step("Ввести в поле описания роли \"{roleDescription}\"")
    private void sendRoleDescription(String roleDescription) {
        LOG.info("Вводим в поле описания роли \"{}\"", roleDescription);
        waitForClickable(os.roleForm().roleDescriptionInput(), os, 10);
        os.roleForm().roleDescriptionInput().clear();
        os.roleForm().roleDescriptionInput().sendKeys(roleDescription);
    }

    @Step("Выбрать произвольный тип роли из выпадающего списка")
    private String selectRandomRoleType() {
        waitForClickable(os.roleForm().roleTypeListDropdownButton(), os, 10);
        os.roleForm().roleTypeListDropdownButton().click();
        AtlasWebElement roleType = os.roleForm().roleTypes().stream().collect(randomItem());
        String roleTypeName = roleType.getText();
        waitForClickable(roleType, os, 10);
        roleType.click();
        return roleTypeName;
    }

    @Step("Нажать на кнопку \"Создать\" в форме создания роли")
    private void pressCreateRoleButton() {
        waitForClickable(os.roleForm().createButton(), os, 10);
        os.roleForm().createButton().click();
        systemSleep(5); //без этого ожидания ассерт не срабатывает
    }

    @Step("Нажать на кнопку \"Изменить\" в форме редактирования роли")
    private void pressChangeRoleButton() {
        LOG.info("Нажимаем на кнопку \"Изменить\" в форме редактирования роли");
        waitForClickable(os.roleForm().changeButton(), os, 10);
        os.roleForm().changeButton().click();
    }

    @Step("Нажать на кнопку \"Изменить\" в форме редактирования роли изменения разрешений")
    private void pressChangePermissionButton() {
        waitForClickable(os.roleForm().changePermissionButton(), os, 10);
        os.roleForm().changePermissionButton().click();
    }

    @Step("Нажать на кнопку удаления роли")
    private void pressDeleteRoleButton() {
        waitForClickable(os.roleForm().deleteRoleButton(), os, 10);
        os.roleForm().deleteRoleButton().click();
        LOG.info("Нажали на кнопку удаления роли");
    }

    @Step("Нажать на кнопку редактирования роли")
    private void pressEditRoleButton() {
        LOG.info("Нажимаем на кнопку редактирования роли");
        new Actions(os.getWrappedDriver()).moveToElement(os.roleForm().editRolePencilButton()).perform();
        waitForClickable(os.roleForm().editRolePencilButton(), os, 10);
        os.roleForm().editRolePencilButton().click();
    }

    @Step("Проверка добавления роли с названием {roleName}")
    private void assertAddRole(List<UserRole> rolesBefore, String roleName, String roleDescription, String roleType) {
        SoftAssert softAssert = new SoftAssert();
        os.roleForm().roleName().waitUntil("Название роли не отобразилось", DisplayedMatcher.displayed(), 10);
        String roleNameUI = os.roleForm().roleName().getText();
        String roleNameDescription = os.roleForm().roleDescription().getText();
        softAssert.assertEquals(roleNameUI, roleName, "Название роли на UI и введенное не совпадают");
        softAssert.assertEquals(roleNameDescription, String.format("%s – %s", roleDescription, roleType),
                                "Описание роли на UI и введенное не совпадают");
        List<UserRole> userRolesAfter = UserRoleRepository.getUserRoles();
        userRolesAfter.removeAll(rolesBefore);
        UserRole addedRole = userRolesAfter.iterator().next();
        softAssert.assertEquals(userRolesAfter.size(), 1, "Количество ролей не совпадает с ожидаемым.");
        softAssert.assertEquals(addedRole.getName(), roleName, "Название роли в АPI и введенное не совпадают");
        softAssert.assertEquals(addedRole.getDescription(), roleDescription, "Описание роли в АPI и введенное не совпадают");
        softAssert.assertAll();
        Reporter.getCurrentTestResult().getTestContext().setAttribute(TEST_ROLE, addedRole.getSelfLink());
    }

    @Step("Выбрать из списка роль с названием \"{roleName}\"")
    private void pressOnRoleName(String roleName) {
        LOG.info("Выбираем из списка роль с названием \"{}\"", roleName);
        int allRolesSize = UserRoleRepository.getUserRoles().size();
        ElementsCollection<AtlasWebElement> allUiRoles = os.osSearchForm().allSearchResult();
        AtlasWebElement element;
        if (allUiRoles.size() != allRolesSize) {
            scrollDownToRole(allUiRoles, allRolesSize, roleName);
        }
        element = os.osSearchForm().searchResult(roleName);
        Assert.assertNotNull(element, String.format("В списке нет роли с названием: \"%s\"", roleName));
        new Actions(os.getWrappedDriver()).moveToElement(element).perform();
        element.click();
    }

    private void scrollDownToRole(ElementsCollection<AtlasWebElement> allUiRoles, int allRolesSize, String roleName) {
        while (allUiRoles.size() < allRolesSize) {
            allUiRoles = os.osSearchForm().allSearchResult();
            List<String> roleStrings = allUiRoles.stream()
                    .map(AtlasWebElement::getText)
                    .map(String::trim).collect(Collectors.toList());
            if (roleStrings.contains(roleName)) {
                return;
            }
            new Actions(os.getWrappedDriver()).moveToElement(allUiRoles.get(allUiRoles.size() - 1)).perform();
        }
    }

    /**
     * Узнает активный график
     */

    public ImmutablePair<ScheduleType, String> getActiveScheduleId() {
        clickOnThreeDotsButton();
        String dataName = os.omInfoForm().activeSchedule().getText();
        ScheduleType uiType = Arrays.asList(ScheduleType.values())
                .stream()
                .filter(s -> s.getNameOfType().equals(dataName))
                .findFirst().orElse(null);
        return new ImmutablePair<>(uiType, dataName);
    }

    @Step("Проверка добавления разрешения \"{permission.name}\" к роли \"{userRole.name}\"")
    private void assertAddPermission(UserRole userRole, Permission permission) {
        UserRole refreshUserRole = userRole.refreshUserRole();
        SecuredOperationDescriptor securedOperationDescriptor = refreshUserRole.getSecuredOperationDescriptor();
        Set<Integer> permissionIds = securedOperationDescriptor.getPermissionIds();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(permissionIds.contains(permission.getId()), "Разрешение не добавилось");
        softAssert.assertEquals(permissionIds.size(), 1, "Количество разрешений у юзера не соответствует ожидаемому");
        softAssert.assertAll();
        ImmutablePair<String, String> groupAndName = permission.getPermissionGroupAndName();
        os.roleForm().permissionCheck(groupAndName.getLeft(), groupAndName.getRight())
                .should("Разрешение не было активировано", DisplayedMatcher.displayed(), 5);
    }

    @Step("Нажать на чекбокс разрешения \"{permission}\"")
    private void clickOnPermissionCheckBox(String permission) {
        new Actions(os.getWrappedDriver()).moveToElement(os.roleForm().permissionCheckBox(permission)).perform();
        os.roleForm().permissionCheckBox(permission).click();
    }

    @Step("Нажать на кнопку редактирования группы разрешений \"{permissionGroup}\"")
    private void clickOnPermissionGroupPencilButton(String permissionGroup) {
        new Actions(os.getWrappedDriver()).moveToElement(os.roleForm().permissionPencilButton(permissionGroup)).perform();
        os.roleForm().permissionPencilButton(permissionGroup).click();
    }

    @Step("Нажать на шеврон раскрытия группы разрешений \"{permissionGroup}\"")
    private void clickOnPermissionGroupChevron(String permissionGroup) {
        new Actions(os.getWrappedDriver()).moveToElement(os.roleForm().permissionChevron(permissionGroup)).perform();
        os.roleForm().permissionChevron(permissionGroup).click();
    }

    @Step("Указать тип и категорию позиции, если стенд требует их для создания позиции")
    public void inputAdditionalPositionInfo() {
        try {
            if (os.addNewEmployeeForm().emptyFieldErrorMessage().isDisplayed()) {
                PositionType positionType = PositionTypeRepository.randomPositionType();
                selectPositionType(positionType);
                PositionCategory positionCategory = PositionCategoryRepository.randomPositionCategory();
                selectPositionCategory(positionCategory);
                os.addNewEmployeeForm().saveButton().sendKeys(Keys.TAB);
                os.addNewEmployeeForm().saveButton().sendKeys(Keys.ENTER);
            }
        } catch (NoSuchElementException e) {
            LOG.info("Стенд не потребовал заполнения полей типа и категории позиции.");
        }
    }

    @Step("Указать категорию {positionCategory} для позиции")
    private void selectPositionCategory(PositionCategory positionCategory) {
        os.addNewEmployeeForm().categoryOfJobInput().click();
        os.addNewEmployeeForm().categoryOfJob(positionCategory.getName()).click();
    }

    @Step("Указать тип {positionType} для позиции")
    private void selectPositionType(PositionType positionType) {
        os.addNewEmployeeForm().positionTypeField().click();
        os.addNewEmployeeForm().positionTypeListItem(positionType.getName()).click();
    }

    @Step("Нажать на кнопку \"Больше\", если она есть, чтобы показать все оргюниты для роли №{order}.")
    private void pressMoreButton(int order) {
        try {
            while (os.editRoleForm().roleViewBlock(order).seeMoreButton().isDisplayed()) {
                os.editRoleForm().roleViewBlock(order).seeMoreButton().click();
                systemSleep(1); //цикл
            }
        } catch (org.openqa.selenium.NoSuchElementException e) {
            LOG.info("Кнопка \"Больше\" из списка оргюнитов для роли не отображается.");
        }
    }

    /**
     * Кнопка "Изменить" в карточке подразделения\сотрудника
     */
    @Step("Кликнуть кнопку \"Изменить\"")
    private void changeInfoCardButtonClick() {
        new Actions(os.getWrappedDriver()).moveToElement(os.osCardForm().osCardChangeButton()).perform();
        os.osCardForm().osCardChangeButton().click();
        os.spinnerLoader().grayLoadingBackground().waitUntil("Спиннер всё ещё отображается",
                                                             Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Проверить, что ФИО сотрудника было изменено")
    private void assertEmployeeNameChanged(Employee randomEmployee, String newName) {
        os.osCardForm().employeeName().waitUntil("Форма редактирования не закрылась", DisplayedMatcher.displayed(), 10);
        randomEmployee = randomEmployee.refreshEmployee();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(randomEmployee.getFullName(), newName, "Имя в API не изменилось");
        String newUIName = os.osCardForm().employeeName().getText().trim();
        softAssert.assertEquals(newUIName, newName, "Имя на UI не изменилось");
        softAssert.assertAll();
    }

    @Step("Выбрать статус \"{status}\" из выпадающего списка")
    private void selectStatus(String status, int orderNumber) {
        AtlasWebElement newInput = os.osCardForm().employeeStatusInputs().get(orderNumber);
        waitForClickable(newInput, os, 10);
        newInput.click();
        AtlasWebElement statusOption = os.osCardForm().statusOptions(status).get(orderNumber);
        waitForClickable(statusOption, os, 10);
        statusOption.click();
        LOG.info("Выбираем статус \"{}\" из выпадающего списка", status);
    }

    @Step("Ввести дату {date} в поле \"{field.name}\"")
    private void inputStatusDate(DateTypeField field, LocalDate date, int orderNumber) {
        AtlasWebElement dateField = os.osCardForm().dateInputs(field.getName()).get(orderNumber);
        waitForClickable(dateField, os, 10);
        dateField.sendKeys(date.format(UI_DOTS.getFormat()));
        LOG.info("Вводим дату {} в поле \"{}\"", date, field.getName());
    }

    /**
     * Возвращает список пунктов из заданной секции. Точно подходит для статусов и заместителей
     *
     * @param employeeInfoName секция, из которой нужно достать список
     */
    private List<EmployeeInfoBlock> getCurrentUiInfoBlocks(EmployeeInfoName employeeInfoName) {
        return os.osCardForm().employeeInfoListItem(employeeInfoName.getNameOfInformation());
    }

    @Step("Проверить, что введенный статус отражается в API и на UI")
    private void assertAddedStatus(Employee employee, List<EmployeeStatus> statusesBefore, LocalDate startDate, LocalDate endDate,
                                   List<String> statusesBeforeUi, EmployeeStatusType statusType) {
        List<EmployeeStatus> statusesAfter = employee.getStatuses();
        statusesAfter.removeAll(statusesBefore);
        Assert.assertFalse(statusesAfter.isEmpty(), "Новый статус не появился в API");
        EmployeeStatus newStatus = statusesAfter.iterator().next();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(newStatus.getStatusType().getOuterId(), statusType.getOuterId(), "Тип статуса в API не соответствует введенному");
        softAssert.assertEquals(newStatus.getFrom(), startDate, "Дата начала декрета в API не соответствует введенной");
        softAssert.assertEquals(newStatus.getTo(), endDate, "Дата конца декрета в API не соответствует введенной");
        Allure.addAttachment("Проверка статуса в API", "text/plain", String.format("Сотруднику %s был присвоен статус \"%s\" с %s по %s",
                                                                                   employee.getFullName(), newStatus.getStatusType().getTitle(), newStatus.getFrom(), newStatus.getTo()));
        EmployeeInfoBlock newStatusUi = getCurrentUiInfoBlocks(EmployeeInfoName.STATUS)
                .stream()
                .filter(e -> !statusesBeforeUi.contains(e.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertException("Новый статус не появился на UI"));
        String statusTitle = newStatusUi.title().getText();
        softAssert.assertEquals(statusTitle, statusType.getTitle(), "Тип статуса на UI не соответствует введенному");
        ImmutablePair<LocalDate, LocalDate> durationPair = parseUiDates(newStatusUi.duration().getText());
        softAssert.assertEquals(durationPair.left, startDate, "Дата начала действия статуса на UI  не соответствует введенной");
        softAssert.assertEquals(durationPair.right, endDate, "Дата окончания действия статуса на UI  не соответствует введенной");
        Allure.addAttachment("Проверка статуса на UI", "text/plain", String.format("Сотруднику %s был присвоен статус \"%s\" с %s по %s",
                                                                                   employee.getFullName(), statusTitle, durationPair.left, durationPair.right));
        softAssert.assertAll();
    }

    private ImmutablePair<LocalDate, LocalDate> parseUiDates(String durationUi) {
        String patternFormat = "d MMMM yyyy";
        Locale localeRu = new Locale("ru", "RU");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(patternFormat).withLocale(localeRu);
        LocalDate dateStartUi = LocalDate.parse(durationUi.substring(0, durationUi.indexOf("—") - 1), formatter);
        LocalDate dateEndUi = LocalDate.parse(durationUi.substring(durationUi.indexOf("—") + 2), formatter);
        return new ImmutablePair<>(dateStartUi, dateEndUi);
    }

    @Step("Нажать на значок карандаша параметра \"{infoName.nameOfInformation}\"")
    private void clickOnParametersPencilButton(EmployeeInfoName infoName) {
        os.employeeData().
                waitUntil("Форма данных о сотруднике не отобразилась.", DisplayedMatcher.displayed(), 10);
        new Actions(os.getWrappedDriver()).moveToElement(os.employeeData().parametersPencilButton(infoName.getNameOfInformation())).perform();
        os.employeeData().parametersPencilButton(infoName.getNameOfInformation()).click();
    }

    /**
     * Выбирает список мат параметров случайным образом
     */
    private List<MathParameter> getRandomListFromMathParameters(MathParameterEntities entity) {
        List<MathParameter> mathParamList = MathParameterRepository.getMathParametersWithEntity(entity);
        Collections.shuffle(mathParamList);
        Random r = new Random();
        int mathParamsAmount = r.nextInt(mathParamList.size() - 1);
        List<MathParameter> mathParams = mathParamList.subList(0, mathParamsAmount + 1);
        List<String> params = mathParams.stream().map(MathParameter::getCommonName).collect(Collectors.toList());
        LOG.info("Были выбраны следующие мат параметры: {}", params);
        Allure.addAttachment("Выбор мат параметров",
                             String.format("Случайным образом были выбраны следующие мат параметры: %s", params));
        return mathParams;
    }

    @Step("Проверить отображение мат параметров, на которые есть права")
    private void checkMathParameters(List<MathParameter> mathParams, MathParameterEntities entity) {
        List<String> tempParamNames = os.employeeData().employeeParametersMenu().mathParamsLabels()
                .stream().map(AtlasWebElement::getText)
                .sorted()
                .map(e -> e.contains("\n") ? e.substring(0, e.indexOf("\n")) : e)
                .collect(Collectors.toList());
        if (tempParamNames.isEmpty()) {
            Assert.fail("Ни одного мат. параметра не найдено.");
        }
        Allure.addAttachment("Проверка",
                             String.format("Отображены мат параметры %s", tempParamNames));
        List<String> allMathParamNames = MathParameterRepository.getMathParametersWithEntity(entity).stream().map(MathParameter::getCommonName).collect(Collectors.toList());
        List<String> mathParamNames = mathParams.stream().map(MathParameter::getCommonName).collect(Collectors.toList());
        allMathParamNames.removeAll(tempParamNames);
        tempParamNames.removeAll(mathParamNames);
        mathParamNames.retainAll(allMathParamNames);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(mathParamNames.isEmpty(),
                              String.format("Не отображены следующие мат параметры: %s", mathParamNames));
        softAssert.assertTrue(tempParamNames.isEmpty(),
                              String.format("Отображены следующие мат параметры, на которые нет прав: %s", tempParamNames));
        softAssert.assertAll();
    }

    /**
     * Выбирает случайный строковый мат параметр
     *
     * @param mathParams список мат параметров
     * @return мат параметр, выбранный случайным образом
     */
    private MathParameter chooseMathParam(List<MathParameter> mathParams) {
        Set<String> textMathParams = os.employeeData().employeeParametersMenu().textMathParamList()
                .stream().map(WebElement::getText).collect(Collectors.toSet());
        List<MathParameter> resultParams = mathParams.stream()
                .filter(p -> textMathParams.contains(p.getCommonName())).collect(Collectors.toList());
        if (resultParams.isEmpty()) {
            Assert.fail("Ни одного мат. параметра не найдено.");
        }
        MathParameter randMathParam = CustomTools.getRandomFromList(resultParams);
        Allure.addAttachment("Выбор мат параметра позиции сотрудника",
                             String.format("Был выбран мат параметр \"%s\"", randMathParam.getCommonName()));
        return randMathParam;
    }

    /**
     * Возвращает случайное число
     *
     * @param limit верхняя граница для выбора элемента
     * @return случайное число
     */
    private Integer chooseRandomNumber(int limit) {
        Random random = new Random();
        return random.nextInt(limit - 1) + 1;
    }

    /**
     * Если форма содержит список для выбора группы мат параметров, выбираем группу с позицией сотрудника
     *
     * @param position название позиции сотрудника
     */
    private void selectEmployeePositionGroup(String position, String orgUnit) {
        os.employeeData().employeeParametersMenu().mathParamGroups().waitUntil(Matchers.hasSize(Matchers.greaterThan(0)));
        ElementsCollection<AtlasWebElement> groups = os.employeeData().employeeParametersMenu().mathParamGroups();
        if (groups.size() > 1) {
            for (AtlasWebElement el : groups) {
                if (el.getAttribute("textContent").contains(position) && el.getAttribute("textContent").contains(orgUnit)) {
                    os.employeeData().employeeParametersMenu().mathParamGroupsChevron().click();
                    el.waitUntil(String.format("Позиция \"%s\" не подгрузилась в список", position), DisplayedMatcher.displayed(), 5);
                    el.click();
                }
            }
        }
    }

    @Step("Присвоить мат параметру \"{param.commonName}\" значение {randNumber}")
    private void changeMathParameter(MathParameter param, int randNumber) {
        EmployeeParametersMenu paramMenu = os.employeeData().employeeParametersMenu();
        String paramName = param.getCommonName();
        new Actions(os.getWrappedDriver()).moveToElement(paramMenu.textMathParam(paramName)).perform();
        String randString = Integer.toString(randNumber);
        paramMenu.textMathParam(paramName).sendKeys(randString);
        paramMenu.textMathParam(paramName).sendKeys(Keys.ENTER);
        LOG.info("Значение мат параметра {} установлено в: {}", paramName, randNumber);
    }

    @Step("Проверить, что значение введенного мат параметра {mathParam.commonName} у сотрудника сохранено")
    private <T> void checkMathParameter(EmployeePosition position, MathParameter mathParam, int randNumber) {
        systemSleep(3); //без этого ожидания мат параметр в апи не находится
        String randString = Integer.toString(randNumber);
        if (mathParam.getType().equals("DOUBLE")) {
            randString = randString + ".0";
        }
        List<MathParameterValue> actualValues = MathParameterValueRepository.getMathParameterValuesForEntity(MathParameterEntities.EMPLOYEE_POSITION, position.getId());
        LOG.info("У сотрудника {} есть следующие мат параметры: {}", position.getEmployee().getFullName(), actualValues);
        String expectedMathParam = mathParam.getCommonName();
        MathParameterValue actualValue = MathParameterValueRepository.getValueForParam(actualValues, mathParam);
        if (actualValue == null) {
            Assert.fail("У сотрудника нет мат параметра " + expectedMathParam);
        }
        Assert.assertEquals(actualValue.getValue().toString(), randString,
                            "Значения мат. параметров не совпали: должно быть " + randString + ", а было :" + actualValue.getValue());
        Allure.addAttachment("Проверка",
                             String.format("Мат параметр \"%s\" позиции сотрудника %s со значением \"%s\" сохранен",
                                           expectedMathParam, position.getEmployee().getFullName(), randString));
    }

    @Step("В поле \"Название\" ввести название подразделения {name}")
    private void enterOMName(String name) {
        os.omCreateCardForm().employeeNameField().click();
        os.omCreateCardForm().employeeNameField().sendKeys(name);
    }

    @Step("В поле \"OuterId\" ввести outerId подразделения {outerId}")
    private void enterOuterId(String outerId) {
        os.omCreateCardForm().outerIdField().click();
        os.omCreateCardForm().outerIdField().sendKeys(outerId);
    }

    @Step("В поле \"Тип\" подразделения выбрать {unitType}")
    private void chooseUnitType(String unitType) {
        os.omCreateCardForm().unitTypeField().click();
        os.omCreateCardForm().unitType(unitType)
                .waitUntil(String.format("Тип подразделения %s отсутствует в выпадающем списке", unitType),
                           DisplayedMatcher.displayed(), 5);
        os.omCreateCardForm().unitType(unitType).click();
    }

    @Step("В поле \"{dateType.name}\" выбрать дату {date}")
    private void enterUnitDateOpenOrClose(LocalDate date, DateTypeField dateType) {
        String dateTypeString = dateType.getName();
        os.omCreateCardForm().dateOpenOrCloseInput(dateTypeString).click();
        os.omCreateCardForm().dateOpenOrCloseInput(dateTypeString).clear();
        os.omCreateCardForm().dateOpenOrCloseInput(dateTypeString)
                .sendKeys(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        os.omCreateCardForm().dateOpenOrCloseInput(dateTypeString).sendKeys(Keys.ENTER);
        LOG.info("В поле \"{}\" введена дата {}", dateTypeString, date);
    }

    @Step("В поле \"{dateType.name}\" выбрать дату {date}")
    private void enterWorkDateBeginOrEnd(LocalDate date, DateTypeField dateType) {
        String dateTypeString = dateType.getName();
        os.empCreateCardForm().workDate(dateTypeString).click();
        os.empCreateCardForm().workDate(dateTypeString).clear();
        os.empCreateCardForm().workDate(dateTypeString)
                .sendKeys(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        os.empCreateCardForm().workDate(dateTypeString).sendKeys(Keys.ENTER);
        LOG.info("В поле \"{}\" введена дата {}", dateTypeString, date);
    }

    @Step("В поле \"Родительское подразделение\" выбрать название подразделения {parentName}")
    private void chooseParentUnit(String parentName) {
        os.omCreateCardForm().unitParentField().click();
        if (os.omCreateCardForm().searchField().isEnabled()) {
            os.omCreateCardForm().searchField().sendKeys(parentName);
            systemSleep(1); //тест падает до вызова этого метода
            os.omCreateCardForm().searchField().click();
            os.omCreateCardForm().allSearchResults()
                    .waitUntil("Список орг юнитов не подгрузился", Matchers.hasSize(Matchers.greaterThan(0)), 10);

            os.omCreateCardForm().allSearchResults().get(0).click();
            LOG.info("В поле \"Родительское подразделение\" введено подразделение {}", parentName);
        }
    }

    @Step("В поле \"Подразделение\" выбрать название подразделения {name}")
    private void enterUnit(String name) {
        os.empCreateCardForm().unitField().click();
        os.empCreateCardForm().unitNameInList(name)
                .waitUntil(String.format("Подразделение %s, на которое у пользователя есть права, не подгрузилось", name),
                           DisplayedMatcher.displayed(), 10);
        os.empCreateCardForm().unitNameInList(name).click();
        LOG.info("В поле \"Подразделение\" введено название подразделения {}", name);
    }

    @Step("Нажать \"Создать\"")
    private void pressCreateOMButton() {
        os.omCreateCardForm().createButton().click();
        LOG.info("Нажали кнопку \"Создать\"");
        os.spinnerLoader().loadingForm().waitUntil("Спиннер все еще крутится",
                                                   Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Нажать \"Создать\"")
    private void pressCreateEmployeeButton() {
        os.empCreateCardForm().createButton().click();
        LOG.info("Нажали кнопку \"Создать\"");
        os.spinnerLoader().loadingForm().waitUntil("Спиннер все еще крутится",
                                                   Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Проверка, что оргюнит \"{name}\" был добавлен к потомкам подразделения {parent.name}")
    private void assertUnitAdded(String name, OrgUnit parent) {
        OrgUnit unit = OrgUnitRepository.getAllChildrenOrgUnits(parent).stream()
                .filter(u -> u.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertException("Созданное подразделение не появилось в API"));
        Assert.assertEquals(unit.getDateInterval().startDate, LocalDate.now(), "Дата открытия не соответствует дате, введенной в форме создания подразделения");
        Allure.addAttachment("Проверка",
                             String.format("К потомкам подразделения %s было добавлено подразделение с названием %s",
                                           parent.getName(), name));
    }

    @Step("Проверка, что сотрудник с фамилией \"{lastName}\" и именем \"{firstName}\" был добавлен в подразделение {unit.name}")
    private void assertEmployeeAdded(String firstName, String lastName, OrgUnit unit) {
        SoftAssert softAssert = new SoftAssert();
        List<Employee> employees = EmployeeRepository.getEmployeesFromOM(unit.getId()).stream()
                .filter(emp -> (emp.getLastName().equals(lastName) && emp.getFirstName().equals(firstName)))
                .collect(Collectors.toList());
        softAssert.assertTrue(employees.size() > 0, String.format("Созданный сотрудник с фамилией %s не появился в API", lastName));
        List<AtlasWebElement> employeesOnUi = os.omInfoForm().employeesNames().stream()
                .filter(el -> el.getText().equals(lastName + " " + firstName))
                .collect(Collectors.toList());
        softAssert.assertTrue(employeesOnUi.size() > 0, String.format("Созданный сотрудник с фамилией %s и именем %s не появился на UI", lastName, firstName));
        softAssert.assertAll();
        Allure.addAttachment("Проверка",
                             String.format("В подразделение %s был добавлен сотрудник с именем %s",
                                           unit.getName(), lastName + " " + firstName));
    }

    @Step("В поле \"Фамилия\" ввести фамилию {lastName} сотрудника")
    private void enterLastName(String lastName) {
        os.empCreateCardForm()
                .waitUntil("Поле ввода фамилии не появилось", DisplayedMatcher.displayed(), 3);
        os.empCreateCardForm().lastNameField().click();
        os.empCreateCardForm().lastNameField().sendKeys(lastName);
    }

    @Step("В поле \"Имя\" ввести имя {firstName} сотрудника")
    private void enterFirstName(String firstName) {
        os.empCreateCardForm()
                .waitUntil("Поле ввода имени не появилось", DisplayedMatcher.displayed(), 3);
        os.empCreateCardForm().firstNameField().click();
        os.empCreateCardForm().firstNameField().sendKeys(firstName);
    }

    @Step("В поле \"Должность\" ввести должность {jobTitle} сотрудника")
    private void enterJobTitle(String jobTitle) {
        os.empCreateCardForm().jobTitleField().click();
        os.empCreateCardForm().jobTitle(jobTitle)
                .waitUntil(String.format("Должность %s не подгрузилась", jobTitle),
                           DisplayedMatcher.displayed(), 10);
        os.empCreateCardForm().jobTitle(jobTitle).click();
    }

    @Step("Проверить, что имена сотрудников в списке скрыты")
    private void checkVisibilityOfEmployeeNames(boolean hasPermissions) {
        changeStepNameIfTrue(hasPermissions, "Проверить, что имена сотрудников в списке отображаются");
        os.omInfoForm().employeesListWithPosition().forEach(e -> e.should(getMatcherDependingOnPermissions(hasPermissions)));
    }

    @Step("Проверить, что контактные данные сотрудника скрыты")
    private void checkVisibilityOfContacts(boolean hasPermissions, PhoneTypes phoneType) {
        changeStepNameIfTrue(hasPermissions, "Проверить, что контактные данные сотрудника отображаются");
        os.employeeData().emailField().should(getMatcherDependingOnPermissions(hasPermissions));
        if (hasPermissions) {
            os.employeeData().phoneField(phoneType.getPhoneName()).should(getMatcherDependingOnPermissions(true));
        } else {
            Assert.assertEquals(os.employeeData().phoneField(phoneType.getPhoneName()).getAttribute("value"), "+7");
        }
    }

    @Step("Проверить даты окончания должности")
    public void assertPositionEndDates(Boolean closePosition, EmployeePosition emp, LocalDate positionEndDateBeforeUI,
                                       LocalDate positionEndDateAfterUI, LocalDate expectedPositionEndDate) {
        LocalDate endDateApi = emp.refreshEmployeePosition().getPosition().getDateInterval().getEndDate();
        SoftAssert softAssert = new SoftAssert();
        if (Objects.isNull(closePosition)) {
            softAssert.assertNull(endDateApi, "\"Дата окончания должности\" не стала пустой");
        } else if (closePosition) {
            softAssert.assertEquals(expectedPositionEndDate, positionEndDateAfterUI,
                                    "в поле \"Дата окончания должности\" автоматически не проставляется дата = дата окончания работы +1");
            softAssert.assertEquals(expectedPositionEndDate, endDateApi, "Дата окончания должности не совпадает в api");
        } else {
            softAssert.assertEquals(positionEndDateBeforeUI, positionEndDateAfterUI,
                                    "значение в поле \"Дата окончания должности\" изменилось с " + positionEndDateBeforeUI + " на " + positionEndDateAfterUI);
            softAssert.assertEquals(positionEndDateBeforeUI, endDateApi, "Дата окончания должности не совпадает в api");
        }
        softAssert.assertAll();
    }

    @Step("Ввести в поле СНИЛС {numberSnils}")
    private void snilsInput(String numberSnils) {
        os.osCardForm().inputSnils().click();
        os.osCardForm().inputSnils().clear();
        slowSendKeys(os.osCardForm().inputSnils(), numberSnils);
        LOG.info("Ввести в поле СНИЛС {numberSnils}");
    }

    @Step("Очистить поле СНИЛС")
    private void clearFieldSnils() {
        os.osCardForm().inputSnils().clear();
        LOG.info("Очистить поле СНИЛС");
    }

    @Step("Проверить СНИЛС")
    private void assertSnils(String snilsValue, Employee employee) {
        String snilsOnUi;
        try {
            snilsOnUi = os.osCardForm().outputSnils().getText().replaceAll("-", "");
        } catch (NoSuchElementException e) {
            snilsOnUi = "";
        }
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(snilsOnUi.length() < 12, "Значение превышает 12 символов");
        softAssert.assertTrue(snilsValue.startsWith(snilsOnUi),
                              String.format("Введено неверное значение в поле СНИЛС. Ожидаемое значение: - %s не совпало с фактическим %s",
                                            snilsValue, snilsOnUi));
        softAssert.assertEquals(employee.getSnils(), snilsOnUi,
                                String.format("Значение СНИЛС в API не совпало со значением на UI. Ожидаемое значение: - %s , фактическое: %s",
                                              snilsOnUi, employee.getSnils()));
        softAssert.assertAll();
    }

    @Step("Ввести значение \"{value}\" для атрибута \"{title}\"")
    private void enterAttributeValue(String title, String value) {
        os.omInfoForm().attributeForm()
                .waitUntil("Форма для ввода значений атрибутов не открылась", DisplayedMatcher.displayed(), 5);
        os.omInfoForm().attributeForm().attributeValueInput(title).click();
        os.omInfoForm().attributeForm().attributeValueInput(title).sendKeys(value);
        LOG.info("Вводим значение \"{}\" для атрибута \"{}\"", value, title);
    }

    @Step("Сохранить значение атрибута")
    private void saveAttributeValueButtonClick() {
        os.omInfoForm().attributeForm().saveButton().click();
        LOG.info("Нажимаем кнопку \"Сохранить\"");
    }

    @Step("Проверить, что значение \"{expectedValue}\" атрибута позиции {propertiesKey} было добавлено")
    private void assertPositionAttributeAdded(int posId, String propertiesKey, String expectedValue) {
        EntityProperty property = EntityPropertyRepository.getEntityPropertyByKey(MathParameterEntities.POSITION, posId, propertiesKey);
        Assert.assertNotNull(property);
        LOG.info("После проведения теста на UI значение атрибута {} у позиции с id {} равно {}", property.getTitle()
                , posId, property.getValue().toString());
        Assert.assertEquals(property.getValue(), expectedValue);
    }

    @Step("Проверить поле \"{fieldName}\"")
    private void assertDisplayedField(String fieldName, boolean visible) {
        String attach = visible ? "отображается" : "не отображается";
        String until = visible ? "ожидается отображение" : "ожидается скрытие";
        Matcher matcher = visible ? Matchers.is(DisplayedMatcher.displayed()) : Matchers.not(DisplayedMatcher.displayed());

        switch (fieldName) {
            case "Тип должности":
                os.addNewEmployeeForm().positionTypeField()
                        .waitUntil("Поле \"" + fieldName + "\" " + until, matcher, 3);
                Allure.attachment("Проверка", "Поле \"" + fieldName + "\" " + attach);
                break;
            case "OuterId должности":
                os.addNewEmployeeForm().outerIdJobTitleField()
                        .waitUntil("Поле \"" + fieldName + "\" " + until, matcher, 3);
                Allure.attachment("Проверка", "Поле \"" + fieldName + "\" " + attach);
                break;
            case "Функциональная роль":
                os.addNewEmployeeForm().functionalRoleInput()
                        .waitUntil("Поле \"" + fieldName + "\" " + until, matcher, 3);
                Allure.attachment("Проверка", "Поле \"" + fieldName + "\" " + attach);
                break;
            case "Категория должности":
                os.addNewEmployeeForm().categoryOfJobInput()
                        .waitUntil("Поле \"" + fieldName + "\" " + until, matcher, 3);
                Allure.attachment("Проверка", "Поле \"" + fieldName + "\" " + attach);
                break;
            case "Табельный номер":
                os.addNewEmployeeForm().cardNumberField()
                        .waitUntil("Поле \"" + fieldName + "\" " + until, matcher, 3);
                Allure.attachment("Проверка", "Поле \"" + fieldName + "\" " + attach);
                break;
            case "Ставка":
                os.addNewEmployeeForm().rateField()
                        .waitUntil("Поле \"" + fieldName + "\" " + until, matcher, 3);
                Allure.attachment("Проверка", "Поле \"" + fieldName + "\" " + attach);
                break;
            //            case "Закрепление за залом":
            //                os.addNewEmployeeForm().assignmentToHallField()
            //                        .waitUntil("Поле \"" + fieldName + "\" " + until, matcher, 3);
            //                Allure.attachment("Проверка", "Поле \"" + fieldName + "\" " + attach);
            //                break;

            default:
                throw new IllegalArgumentException("Некорректное название поля: " + fieldName);
        }
    }

    @Step("Проверить, что логин был изменен")
    private void assertIsEditLogin(String login, Employee employee) {
        Assert.assertEquals(login, employee.getUser().getUsername(), "Логин не был изменен");
    }

    @Step("Проверить блок \"{employeeInfoName}\", что \"карандаша\" в нем нет.")
    private void assertPencilNotChange(String employeeInfoName) {
        os.osCardForm().pencilButton(employeeInfoName).should("Карандаш отображается", Matchers.not(DisplayedMatcher.displayed()));
    }

    @Step("Проверить, что блок \"{employeeInfoName}\" не отображается.")
    private void assertEmployeeInfoNotChange(String employeeInfoName) {
        os.osCardForm().showButton(employeeInfoName).should(employeeInfoName + " отображается", Matchers.not(DisplayedMatcher.displayed()));
    }

    @Step("Проверить, что поле неактивно: {fieldName}")
    private void assertFieldDisabled(String fieldName) {
        String errorMessage = "Поле \"" + fieldName + "\" доступно для ввода";
        AtlasWebElement field = null;
        if (fieldName.equals("Логин")) {
            field = os.osCardForm().employeeLoginField();
        } else if (fieldName.equals("Пароль")) {
            field = os.osCardForm().employeePassField();
        } else if (fieldName.equals("Подтверждение пароля")) {
            field = os.osCardForm().employeeConformPassField();
        } else {
            throw new IllegalArgumentException("Неподдерживаемое поле: " + fieldName);
        }

        String value = field.getAttribute("disabled");
        Assert.assertEquals(value, "true", errorMessage);
        try {
            field.sendKeys(RandomStringUtils.randomAlphanumeric(10));
            throw new AssertionError(errorMessage);
        } catch (ElementNotInteractableException ignored) {
        }
    }

    private void assertAllHideValue(String hideValue, boolean visible) {
        if (hideValue.equals("NONE")) {
            assertDisplayedField("Тип должности", visible);
            assertDisplayedField("OuterId должности", visible);
            assertDisplayedField("Функциональная роль", visible);
            assertDisplayedField("Категория должности", visible);
            assertDisplayedField("Табельный номер", visible);
            assertDisplayedField("Ставка", visible);
            //assertDisplayedField("Закрепление за залом", visible);
        } else {
            int[] fieldsToCheck = Arrays.stream(hideValue.substring(1, hideValue.length() - 1).split(","))
                    .mapToInt(Integer::parseInt)
                    .toArray();

            for (int field : fieldsToCheck) {
                switch (field) {
                    case 1:
                        assertDisplayedField("Тип должности", visible);
                        break;
                    case 2:
                        assertDisplayedField("OuterId должности", visible);
                        break;
                    case 3:
                        assertDisplayedField("Функциональная роль", visible);
                        break;
                    case 4:
                        assertDisplayedField("Категория должности", visible);
                        break;
                    case 5:
                        assertDisplayedField("Табельный номер", visible);
                        break;
                    case 6:
                        assertDisplayedField("Ставка", visible);
                        break;
                    default:
                        throw new AssertionError("Некорректное значение поля для проверки: " + field);
                }
            }
        }
    }

    @Step("Ввести табельный номер")
    public void enterCardNumber() {
        os.addNewEmployeeForm().cardNumberField().sendKeys(RandomStringUtils.randomNumeric(6));
    }

    @Test(groups = {"FM-19", "release"}, description = "Фильтр подразделений")
    public void unitFilter() {
        goToOrgStructure();
        makeClearFilters();
        List<String> omName = Arrays.asList("kdrs", "care", "huaw", "lego");
        String randomValue = omName.get((new Random()).nextInt(omName.size()));
        sendRandomVariantIntoOMField(randomValue);
        sendEnterIntoSearchFieldForOM();
        checkThatListMatches(new Sorter().getOrgUnitsByName(SorterOptions.OM_BY_NAME, false, randomValue));
    }

    @Test(groups = {"FM-1"}, description = "Использование фильтра Тип подразделений в Подразделениях")
    public void moduleMixFilterTypeOM() {
        goToOrgStructure();
        makeClearFilters();
        clickOmTypeMenu();
        ImmutablePair<String, Integer> omType = CommonRepository.getRandomUnitType();
        selectOmType(omType.left);
        pressOkOmType();
        checkThatListMatches(new Sorter().getOrgUnitsByTypeIds(SorterOptions.OM_BY_TYPE_ID, false, omType.right));
    }

    @Test(groups = {"FM-18"})
    public void moduleMixFilterTypeOMOnlyOk() {
        goToOrgStructure();
        clickOmTypeMenu();
        pressOkOmType();
    }

    @Test(groups = {"FM-2"})
    public void moduleMixFilterTypeOmReset() {
        goToOrgStructure();
        clickOmTypeMenu();
        ImmutablePair<String, Integer> omType = CommonRepository.getRandomUnitType();
        selectOmType(omType.left);
        pressResetOmType();
        Sorter sorter = new Sorter();
        checkThatListMatches(sorter.getOrgUnits(false, SorterOptions.OM_DISCHARGE));
    }

    @Test(groups = {"FM-3"})
    public void moduleMixFilterTypeOmOnlyReset() {
        goToOrgStructure();
        clickOmTypeMenu();
        pressResetOmType();
        Sorter sorter = new Sorter();
        checkThatListMatches(sorter.getOrgUnits(false, SorterOptions.OM_DISCHARGE));
    }

    @Test(groups = {"FM-6"})
    public void moduleMixFilterOMResetOnly() {
        goToOrgStructure();
        List<String> allResults = os.osSearchForm().allSearchResult().extract(WebElement::getText);
        clickFilterOM();
        filterClickReset();
        checkFilterResetOnly(allResults);
    }

    @Test(groups = {"FM-7", "TEST-221", "release"}, description = "Использование фильтра тегами  вкладки Подразделения")
    public void moduleMixFilterOmTags() {
        goToOrgStructure();
        makeClearFilters();
        clickOmTags();
        Map<String, String> tags = CommonRepository.getTags();
        String randomKey = getRandomFromList(new ArrayList<>(tags.keySet()));
        choseCertainTags(randomKey);
        clickSubmitTags();
        checkThatListMatches(new Sorter().getOrgUnitsByTag(SorterOptions.OM_BY_TAG, false, tags.get(randomKey)));
    }

    @Test(groups = {"FM-11"})
    public void moduleMixFilterOmReset() {
        goToOrgStructure();
        resetFilters();
    }

    @Test(groups = {"FM-12"})
    public void moduleMixFilterTypeOMResetGlobal() {
        goToOrgStructure();
        clickOmTypeMenu();
        ImmutablePair<String, Integer> omType = CommonRepository.getRandomUnitType();
        selectOmType(omType.left);
        pressOkOmType();
        resetFilters();
    }

    @Test(groups = {"FE-1", G2, MIX2},
            description = "Использование фильтра \"Должность\" в \"Сотрудниках\"")
    @Link(name = "Статья: \"Тесты для списка модуля \"Оргструктура\"\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%98%D1%81%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5%D1%84%D0%B8%D0%BB%D1%8C%D1%82%D1%80%D0%B0%22%D0%94%D0%BE%D0%BB%D0%B6%D0%BD%D0%BE%D1%81%D1%82%D1%8C%22%D0%B2%22%D0%A1%D0%BE%D1%82%D1%80%D1%83%D0%B4%D0%BD%D0%B8%D0%BA%D0%B0%D1%85%22")
    @TmsLink("60192")
    @Owner(BUTINSKAYA)
    @Tag("FE-1")
    @Tag(MIX2)
    public void moduleMixFilterEmpPosition() {
        ImmutablePair<List<Employee>, JobTitle> employeeListWithJobTitle = EmployeeRepository.getRandomEmployeeByPositionNameWithLimit(20);
        List<Employee> employeesListApi = employeeListWithJobTitle.left
                .stream()
                .sorted(Comparator.comparing(Employee::getFullName))
                .collect(Collectors.toList());
        JobTitle randomJob = employeeListWithJobTitle.right;
        goToOrgStructure();
        clickEmployeeTab();
        pressEmpPositionFilter();
        selectEmpPosition(randomJob.getFullName());
        pressOkEmpPositionForm();
        compareEmployeeLists(employeesListApi, randomJob);
    }

    @Test(groups = {"FE-2"})
    public void moduleMixFilterEmpPositionReset() {
        goToOrgStructure();
        clickEmployeeTab();
        pressEmpPositionFilter();
        selectEmpPosition(getRandomFromList(CommonRepository.getAllPositionTypes()));
        resetButtonEmpPositionFilter();
        Sorter sorter = new Sorter();
        checkThatListMatches(sorter.getEmployees(false, SorterOptions.EMP_DISCHARGE));
    }

    @Test(groups = {"FE-3"})
    public void moduleMixFilterEmpPositionResetOnly() {
        goToOrgStructure();
        clickEmployeeTab();
        pressEmpPositionFilter();
        resetButtonEmpPositionFilter();
        Sorter sorter = new Sorter();
        checkThatListMatches(sorter.getEmployees(false, SorterOptions.EMP_DISCHARGE));
    }

    @Test(groups = {"FE-18", "release", "TEST-225"}, description = "Фильтр сотрудников")
    public void filteringEmployees() {
        goToOrgStructure();
        clickEmployeeTab();
        makeClearFilters();
        String randomValue = getRandomFromList(Arrays.asList("Юлия", "Александр", "Иван", "Анастасия", "Николай"));
        chooseEmployee(randomValue);
        sendEnterIntoSearchFieldForEmp();
        checkThatListMatches(new Sorter().getEmployeesByName(SorterOptions.EMP_BY_NAME, false, randomValue));
    }

    @Test(groups = {"FE-20"}, description = "Проверка переключения между вкладками Подразделения и Сотрудники")
    public void switchingBetweenDepartmentsAndEmployees() {
        goToOrgStructure();
        clickEmployeeTab();
        assertForChangeOnEmp();
    }

    @Test(groups = {"SM-1"})
    public void moduleMixPickOmFromList() {
        goToOrgStructure();
        selectFromList();
        compareSelectedOm();
    }

    @Test(groups = {"SM-2"})
    public void moduleMixPickOmSearch() {
        goToOrgStructure();
        ImmutablePair<String, String> namesPair = getRandomOrgName();
        searchTheModule(namesPair.left, namesPair.right);
    }

    @Test(groups = {"OM-1", "Rmix-9"}, description = "Изменение названия Ом")
    @Severity(value = SeverityLevel.NORMAL)
    public void moduleMixCardRedMainName() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitByMatchName(CHANGE_OM_NAME);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String randomName = CHANGE_OM_NAME + CustomTools.stringGenerator();
        changeOmName(randomName);
        changeInfoCardButtonClick();
        assertChangingOmName(randomName, unit);
    }

    @Test(groups = {"OM-2"}, description = "Изменение типа ОМ")
    public void moduleMixCardRedMainType() {
        int orgUnitType = getLowestOrgTypes(false);
        List<OrgUnit> unitList = OrgUnitRepository.getOrgUnitsByTypeId(orgUnitType);
        OrgUnit unit = getRandomFromList(unitList);
        ImmutablePair<String, Integer> omTypeIdAndName = CommonRepository.getRandomOrgUnitTypeExceptMostHigherAndAnother(unit);
        String typeBefore = CommonRepository.getAllOrgUnitTypes().get(unit.getOrganizationUnitTypeId());
        PresetClass.changeOrgUnitParentToHighLevelOrgUnit(unit);
        goToOrgStructure();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        changeOmType(omTypeIdAndName.left, typeBefore);
        changeInfoCardButtonClick();
        assertChangingOmType(omTypeIdAndName, true, unit);
    }

    @Test(groups = {"OM-3"}, description = "Изменение даты закрытия ОМ")
    public void moduleMixCardRedMainEndDate() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        LocalDate endDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM).plusYears(new Random().nextInt(2) + 1);
        changeOmDate(DateTypeField.CLOSE_DATE, endDate);
        changeInfoCardButtonClick();
        assertChangeCloseDate(endDate);
    }

    @Test(groups = {"OM-4"}, description = "Изменение даты открытия ОМ")
    public void moduleMixCardRedMainStartDate() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        LocalDate dateOpen = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM).minusYears(new Random().nextInt(3) + 1);
        changeOmDate(DateTypeField.OPEN_DATE, dateOpen);
        changeInfoCardButtonClick();
        assertChangingOmStartedDate(dateOpen, unit);
    }

    @Test(groups = {"OM-5", "broken", "not actual"}, description = "Выбор заместителя подразделения")
    public void moduleMixCardRedDeputy() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String deputyName = changeDeputyEmployee();
        LocalDate startDeputy = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM).minusYears(new Random().nextInt(3) + 1);
        LocalDate endDeputy = startDeputy.plusMonths(6);
        changeOmDate(DateTypeField.START_DEPUTY_DATE, startDeputy);
        changeOmDate(DateTypeField.END_DEPUTY_DATE, endDeputy);
        changeInfoCardButtonClick();
        assertChangingDeputy(deputyName, startDeputy, endDeputy);
    }

    @Test(groups = {"OM-6"}, description = "Изменение родительского подразделения ОМ")
    public void moduleMixCardRedMainParentOm() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        String parentId = unit.getLinks().get("parent").split("/")[6];
        OrgUnit currentParenOrgUnit = getOrgUnit(Integer.parseInt(parentId));
        searchTheModule(unit.getName());
        pressMainDataPencil();
        List<String> units = OrgUnitRepository.getOrgUnitsByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3).getId())
                .stream().map(OrgUnit::getName).collect(Collectors.toList());
        clickOnChangingOmButton(OrgUnitInputs.PARENT_OM);
        String nameOrgUnit = getRandomParentUnitFromUi(units);
        selectParentOM(nameOrgUnit, currentParenOrgUnit);
        changeInfoCardButtonClick();
        assertChangingParentName(nameOrgUnit, currentParenOrgUnit.getName());
    }

    @Test(groups = {"OM-8"}, description = "Добавление подразделения к участвующим в расчете")
    @Severity(value = SeverityLevel.NORMAL)
    public void moduleMixCardRedMainCalculations() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRndOrgUnitCalculatedFalse();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        clickCalculationFlag();
        changeInfoCardButtonClick();
        assertChangingOmCalc(true);
    }

    @Test(groups = {"OM-9"}, description = "Отмена изменения названия Ом")
    public void moduleMixCardRedMainNameDismiss() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        changeOmName(CHANGE_OM_NAME + CustomTools.stringGenerator());
        pressDismissChangeRedMain();
        assertChangingOmName(unit.getName(), unit);
    }

    @Test(groups = {"OM-10"}, description = "Отмена изменения типа ОМ")
    public void moduleMixCardRedMainTypeDismiss() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        String typeBefore = CommonRepository.getAllOrgUnitTypes().get(unit.getOrganizationUnitTypeId());
        searchTheModule(unit.getName());
        pressMainDataPencil();
        ImmutablePair<String, Integer> typeIdName = CommonRepository.getTypeIdExceptOne(unit);
        changeOmType(typeIdName.left, typeBefore);
        pressDismissChangeRedMain();
        assertChangingOmType(typeIdName, false, unit);
    }

    @Test(groups = {"OM-11"}, description = "Отмена изменения даты закрытия ОМ")
    public void moduleMixCardRedMainEndDateDismiss() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        LocalDate endDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM).minusYears(new Random().nextInt(5));
        changeOmDate(DateTypeField.CLOSE_DATE, endDate);
        pressDismissChangeRedMain();
        assertDateEndDidNotChange();
    }

    @Test(groups = {"OM-12"}, description = "Отмена изменения даты открытия ОМ")
    public void moduleMixCardRedMainStartDateDismiss() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        LocalDate before = getValueInputDate();
        LocalDate dateOpen = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM).minusYears(new Random().nextInt(5));
        changeOmDate(DateTypeField.OPEN_DATE, dateOpen);
        pressDismissChangeRedMain();
        assertChangingOmStartedDate(before, unit);
    }

    @Test(groups = {"OM-13", "Ref", "not actual"}, description = "Отмена назначения заместителя ОМ")
    public void moduleMixCardRedDeputyDismiss() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        searchTheModule(unit.getName());
        String deputyBefore = EmployeeRepository.getCurrentDeputy(unit).getFullName();
        pressMainDataPencil();
        changeDeputyEmployee();
        LocalDate startDate = LocalDateTools.randomSeedDate(-1, 2, ChronoUnit.MONTHS, TimeType.RANDOM);
        LocalDate endDate = startDate.plusMonths(1);
        changeOmDate(DateTypeField.START_DEPUTY_DATE, startDate);
        changeOmDate(DateTypeField.START_DEPUTY_DATE, endDate);
        pressDismissChangeRedMain();
        assertCancelDeputy(unit, deputyBefore);
    }

    @Test(groups = {"OM-14", "Ref"}, description = "Отмена назначения родительского подразделения ОМ")
    public void moduleMixCardRedMainParentOmDismiss() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        String parentId = unit.getLinks().get("parent").split("/")[6];
        OrgUnit currentParenOrgUnit = getOrgUnit(Integer.parseInt(parentId));
        searchTheModule(unit.getName());
        pressMainDataPencil();
        List<String> units = OrgUnitRepository.getOrgUnitsByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3).getId())
                .stream().map(OrgUnit::getName).collect(Collectors.toList());
        clickOnChangingOmButton(OrgUnitInputs.PARENT_OM);
        String nameOrgUnit = getRandomParentUnitFromUi(units);
        selectParentOM(nameOrgUnit, currentParenOrgUnit);
        pressDismissChangeRedMain();
        assertDismissChangeParentName(nameOrgUnit, currentParenOrgUnit.getName());
    }

    @Test(groups = {"OM-15", "Ref"}, description = "Отмена добавления тега к ОМ")
    public void moduleMixCardRedMainTagsDismiss() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        PresetClass.tagPreset(unit, TagValue.ONE);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String rndTag = RandomStringUtils.randomAlphabetic(10);
        enterTags(rndTag);
        clickAddTags();
        pressDismissChangeRedMain();
        assertCancelOmTag(rndTag, unit, NumberOfTags.ONE_TAG);
    }

    @Test(groups = {"OM-16", "Ref"}, description = "Отмена добавления подразделения к участвующим в расчете")
    public void moduleMixCardRedMainCalculationsDismiss() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRndOrgUnitCalculatedFalse();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        clickCalculationFlag();
        pressDismissChangeRedMain();
        assertChangingOmCalc(false);
    }

    @Test(groups = {"OMC-30"})
    public void moduleMixCardContacts() {
        goToOrgStructure();
        clickOnShowButton(EmployeeInfoName.CONTACTS);
        clickOnPencilButton(EmployeeInfoName.CONTACTS);
        enterPhoneNumber("88001234567");
        selectPhoneType();
        enterEmail("example@gmail.com");
        enterAddressType(false);
        enterOmIndex("123456");
        enterOmCountry("Россия");
        enterOmRegion("Московский");
        enterOmCity("Москва");
        enterOmStreet("Арбатская");
        enterNote("Заметка с цифрой9");
        changeInfoCardButtonClick();
        assertChangingContacts();
    }

    @Test(groups = {"EMP-1"}, description = "Изменение основных данных сотрудника")
    @Severity(value = SeverityLevel.CRITICAL)
    public void changeEmployeeCardData() {
        goToOrgStructure();
        clickEmployeeTab();
        pressMainDataPencil();
        String firstName = RandomStringUtils.randomAlphabetic(10);
        String lastName = RandomStringUtils.randomAlphabetic(10);
        String patronymicName = RandomStringUtils.randomAlphabetic(10);
        sendValueInInput(EmpFields.FIRST_NAME, firstName);
        sendValueInInput(EmpFields.LAST_NAME, lastName);
        sendValueInInput(EmpFields.PATRONYMIC_NAME, patronymicName);
        EmpFields actualGender = getAnotherGender();
        changeGender(actualGender);
        //поставлено на 25 лет назад потому что баг, нельзя в датапикере выбрать раньше 1990
        LocalDate birthdayDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM).minusYears(new Random().nextInt(25));
        enterEmployeeBirthday(birthdayDate);
        changeInfoCardButtonClick();
        assertChangingEmpName(firstName, lastName, patronymicName, actualGender, birthdayDate);
    }

    @Test(groups = {"EMP-2", "Assert", "not actual"})
    public void moduleMixPersCardContacts() {
        goToOrgStructure();
        clickEmployeeTab();
        clickOnShowButton(EmployeeInfoName.CONTACTS);
        clickOnPencilButton(EmployeeInfoName.CONTACTS);
        enterPhoneNumber("88001234567");
        selectPhoneType();
        enterEmail("example@gmail.com");
        enterAddressType(true);
        enterOmIndex("123456");
        enterOmCountry("Россия");
        enterOmRegion("Московский");
        enterOmCity("Москва");
        enterOmStreet("Арбатская");
        enterNote("Заметка с цифрой9");
        changeInfoCardButtonClick();
        assertChangingContacts();
    }

    @Test(groups = {"EMP-8", "broken", "not actual"})
    public void moduleMixPersLoginParam() {
        goToOrgStructure();
        clickEmployeeTab();
        clickOnShowButton(EmployeeInfoName.LOGIN_OPTIONS);
        clickOnPencilButton(EmployeeInfoName.LOGIN_OPTIONS);
        enterNewLogin("LogTest");
        enterNewPassword("PassTest");
        confirmNewPassword("PassTest");
        changeInfoCardButtonClick();
        assertNewLogin();
    }

    @Test(groups = {"EMP-10", "broken", "not actual"})
    public void moduleMixPersParameters() {
        goToOrgStructure();
        clickEmployeeTab();
        clickOnShowButton(EmployeeInfoName.OPTIONS);
        clickOnPencilButton(EmployeeInfoName.OPTIONS);
        changeInfoCardButtonClick();
    }

    @Test(groups = "PNA-6.3.4.1")
    public void enteringParametersForTheCalculationOfResourceRequirements() {
        goToOrgStructure();
        searchTheModule("LEGO_КДР_МЕГА");
        clickOnChevronButton(OmInfoName.PARAMETERS);
        changeTheValuesOfTheDesiredParameters("Конверсия для расчета РЗ");
        changeTheValuesOfTheDesiredParameters("Производительность (операции)");
        changeTheValuesOfTheDesiredParameters("Производительность (трафик)");
        clickSaveParameters();
    }

    @Test(groups = {"Rmix-1.1", "TEST-519"}, description = "Добавить дату увольнения на должности")
    @Severity(value = SeverityLevel.NORMAL)
    public void addDateOfDismissalOM() {
        goToOrgStructure();
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository
                .getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        OrgUnit orgUnit = pair.left;
        searchTheModule(orgUnit.getName());
        Employee employee = pair.right.getEmployeePosition().getEmployee();
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        String employeeName = employee.getFullName();
        clickOnEmployeeThreeDots(employeeName);
        editButtonClick();
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.THAT).plusMonths(new Random().nextInt(5) + 1);
        chooseDatePositionForm(date, DateTypeField.END_JOB);
        saveButtonClick();
        assertDateEndAvailability(date, pair.right.getEmployeePosition());
    }

    @Test(groups = {"Rmix-1.2", "TEST-519"}, description = "Добавить дату увольнения")
    public void addDateOfDismissalEmp() {
        goToOrgStructure();
        clickEmployeeTab();
        Employee employee = EmployeeRepository.getEmployeeWithOutEndDate();
        String employeeName = employee.getFullName();
        int idEmp = employee.getId();
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM).plusMonths(new Random().nextInt(10) + 1);
        chooseEmployee(employeeName);
        changeEmp();
        selectEmployeeEndDate(date);
        changeInfoCardButtonClick();
        assertEndDate(date, idEmp, employeeName);
    }

    @Test(groups = {"Rmix-3", "TEST-521"}, description = "Создание виртуального сотрудника")
    public void creatingVirtualEmployee() {
        goToOrgStructure();
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit();
        String orgName = orgUnit.getName();
        int omId = orgUnit.getId();
        searchTheModule(orgName);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        Employee employee = EmployeeRepository.getRandomEmployeeWithoutWork(omId, getCurrentEmployeesNames());
        clickOnPlusButtonEmployee();
        clickOnSelectEmployeeChevron();
        selectAnyEmployee(employee.getFullName());
        LocalDate jobStart = LocalDateTools.randomSeedDate(-1, 3, ChronoUnit.MONTHS, TimeType.RANDOM);
        LocalDate dateStartPosition = LocalDateTools.randomSeedDate(-4, 8, ChronoUnit.MONTHS, TimeType.RANDOM);
        chooseJob(getJob("Виртуальный сотрудник"));
        chooseDatePositionForm(jobStart, DateTypeField.START_JOB);
        PositionGroup positionGroup = PositionGroupRepository.randomPositionGroup();
        selectFunctionalRole(positionGroup);
        chooseDatePositionForm(dateStartPosition, DateTypeField.POSITION_START_DATE);
        saveButtonClick();
        assertForEmployeePosition(getJob("Виртуальный сотрудник"), jobStart, dateStartPosition, employee.getFullName(), omId);
    }

    @Test(groups = {"Rmix-5", "TEST-523"}, description = "Изменение параметров входа")
    public void changeLoginSettings() {
        goToOrgStructure();
        clickEmployeeTab();
        Employee employee = EmployeeRepository.getEmployeeWithOutEndDate();
        String employeeName = employee.getFullName();
        int idEmp = employee.getId();
        chooseEmployee(employeeName);
        clickOnShowButton(EmployeeInfoName.LOGIN_OPTIONS);
        clickOnPencilButton(EmployeeInfoName.LOGIN_OPTIONS);
        String login = RandomStringUtils.randomAlphanumeric(10);
        String pass = "123456Qq";
        enterNewLogin(login);
        enterNewPassword(pass);
        confirmNewPassword(pass);
        clickOnChangeButton(EmployeeInfoName.LOGIN_OPTIONS);
        assertEmployeeLogin(idEmp, login);
    }

    @Test(groups = {"Rmix-7.1", "TEST-525"}, description = "Сохранение параметра сотруднику")
    @Severity(value = SeverityLevel.CRITICAL)
    public void addEmployeeParameter() {
        goToOrgStructure();
        clickEmployeeTab();
        Employee employee = EmployeeRepository.getRandomEmployee();
        EmployeeParams random = EmployeeParams.getRandomSimpleParam();
        MathParameter parameter = MathParameterRepository.getMathParameter(random.getId());
        PresetClass.checkEmployeeParams(employee.getId(), parameter.getMathParameterId(), parameter.getName());
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.OPTIONS);
        clickOnPencilButton(EmployeeInfoName.OPTIONS);
        String value = RandomStringUtils.randomAlphabetic(10);
        enterParamValue(random.getName(), value);
        clickOnChangeButton(EmployeeInfoName.OPTIONS);
        assertParamAdding(value, value, parameter, employee);
    }

    @Test(groups = {"Rmix-7.2", "TEST-525"}, description = "Изменение внесенного параметра сотруднику")
    public void adjustEmployeeParameter() {
        goToOrgStructure();
        clickEmployeeTab();
        Employee employee = EmployeeRepository.getRandomEmployee();
        String employeeName = employee.getFullName();
        int idEmp = employee.getId();
        chooseEmployee(employeeName);
        clickOnShowButton(EmployeeInfoName.OPTIONS);
        clickOnPencilButton(EmployeeInfoName.OPTIONS);
        MathParameters matchParameter = MathParameters.getRandomMathParameter();
        VariantsInMathParameters variant = returnVariant(matchParameter);
        chooseMatchParam(matchParameter);
        choseVariant(variant);
        clickOnChangeButton(EmployeeInfoName.OPTIONS);
        assertParamChanging(variant, String.valueOf(idEmp), matchParameter, employeeName);
    }

    @Test(groups = {"Rmix-8.1", "TEST-526"}, description = "Переход в карточку ОМ")
    public void gotToOrgUnitCard() {
        goToOrgStructure();
        ImmutablePair<String, String> namesPair = getRandomOrgName();
        searchTheModule(namesPair.left, namesPair.right);
    }

    @Test(groups = {"Rmix-8.2", "TEST-526"}, description = "Переход в карточку сотрудника")
    public void goToEmployeeCard() {
        goToOrgStructure();
        clickEmployeeTab();
        Employee employee = EmployeeRepository.getRandomEmployee();
        String employeeName = employee.getFullName();
        chooseEmployee(employeeName);
        goToEmployeeCardCheck(employeeName);
    }

    @Test(groups = {"Rmix-17.1", G0, MIX1}, description = "Внесение параметра по ОМ")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%94%D0%BE%D0%B1%D0%B0%D0%B2%D0%BB%D0%B5%D0%BD%D0%B8%D0%B5%D1%82%D0%B5%D0%B3%D0%B0%D1%81%D0%BE%D1%82%D1%80%D1%83%D0%B4%D0%BD%D0%B8%D0%BA%D1%83")
    @Severity(value = SeverityLevel.CRITICAL)
    @TmsLink("60486")
    @Tag(MIX1)
    @Tag("Rmix-17.1")
    public void enterParameterOnOmAndSave() {
        goToOrgStructure();
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotClosedOrgName();
        String randomOrgName = orgUnit.getName();
        int omId = orgUnit.getId();
        searchTheModule(randomOrgName);
        String paramName = getParamName(false, orgUnit);
        MathParameter param = MathParameterRepository.getMathParameters().stream().filter(p -> p.getShortName().equals(paramName)).findFirst().orElse(null);
        int rndNumber = new Random().nextInt(1000);
        pressPencilAtOm(OmInfoName.PARAMETERS);
        sendInTargetParamInput(paramName, String.valueOf(rndNumber));
        saveParameterChanges();
        assertParameterChange(param, MathParameterEntities.ORGANIZATION_UNIT, omId, rndNumber, null);
    }

    @Test(groups = {"Rmix-17.2", "TEST-535"}, description = "Скорректировать параметр по ОМ")
    public void correctParameterOnOm() {
        goToOrgStructure();
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotClosedOrgName();
        String randomOrgName = orgUnit.getName();
        int omId = orgUnit.getId();
        searchTheModule(randomOrgName);
        String paramName = getParamName(true, orgUnit);
        MathParameter param = MathParameterRepository.getMathParameters().stream().filter(p -> p.getShortName().equals(paramName)).findFirst().orElse(null);
        int rndNumber = new Random().nextInt(1000);
        pressPencilAtOm(OmInfoName.PARAMETERS);
        String valueBefore = os.tagsForm().paramNameInput(paramName).getAttribute("value");
        sendInTargetParamInput(paramName, String.valueOf(rndNumber));
        saveParameterChanges();
        assertParameterChange(param, MathParameterEntities.ORGANIZATION_UNIT, omId, rndNumber, valueBefore);
    }

    @Test(groups = {"Rmix-2.1", "TEST-520", "@Before manipulation with contacts in employee card"},
            description = "Добавить e-mail сотруднику")
    public void addEmail() {
        goToOrgStructure();
        clickEmployeeTab();
        Employee employee = EmployeeRepository.getEmployeeEmailOption(false);
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.CONTACTS);
        clickOnPencilButton(EmployeeInfoName.CONTACTS);
        String newMail = generateRandomEmail();
        enterEmail(newMail);
        clickOnChangeButton(EmployeeInfoName.CONTACTS);
        checkEmailMatches(newMail, employee);
    }

    @Test(groups = {"Rmix-2.2", "TEST-520", "@Before manipulation with contacts in employee card"}, description = "Изменить e-mail сотрудника")
    public void changeEmail() {
        goToOrgStructure();
        clickEmployeeTab();
        Employee employee = EmployeeRepository.getEmployeeEmailOption(true);
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.CONTACTS);
        clickOnPencilButton(EmployeeInfoName.CONTACTS);
        String newMail = generateRandomEmail();
        clearEmailField();
        enterEmail(newMail);
        clickOnChangeButton(EmployeeInfoName.CONTACTS);
        checkEmailMatches(newMail, employee);
    }

    @Test(groups = {"Rmix-2.3", G2, MIX2,
            "@Before allow change OuterId"},
            description = "Добавление тега сотруднику")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%94%D0%BE%D0%B1%D0%B0%D0%B2%D0%BB%D0%B5%D0%BD%D0%B8%D0%B5%D1%82%D0%B5%D0%B3%D0%B0%D1%81%D0%BE%D1%82%D1%80%D1%83%D0%B4%D0%BD%D0%B8%D0%BA%D1%83")
    @TmsLink("60233")
    @Tag("Rmix-2.3")
    @Tag(MIX2)
    public void addTagToEmployee() {
        goToOrgStructure();
        clickEmployeeTab();
        Employee employee = EmployeePositionRepository.getRandomEmployeeWorkingWithUser().getEmployee();
        chooseEmployee(employee.getFullName());
        changeEmp();
        List<String> allTags = new ArrayList<>(CommonRepository.getTags().keySet());
        String randomTag = getRandomFromList(allTags);
        sendTagForEmployee(randomTag);
        List<String> tagsBefore = employee.getActualTags();
        changeInfoCardButtonClick();
        assertAddTag(employee, tagsBefore, randomTag);
    }

    @Test(groups = {"Rmix-2.4", "TEST-1161"}, description = "Удаление тега сотрудника")
    public void deleteTagToEmployee() {
        Employee employee = EmployeeRepository.getRandomEmployee();
        List<String> tagsBefore = PresetClass.checkEmployeeTags(employee);
        goToOrgStructure();
        clickEmployeeTab();
        chooseEmployee(employee.getFullName());
        changeEmp();
        String randomTag = getRandomFromList(tagsBefore);
        deleteTagForEmployee(randomTag);
        changeInfoCardButtonClick();
        assertDeleteTag(employee, tagsBefore, randomTag);
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"Rmix-4"},
            description = "Добавление стажерской программы")
    @Link(name = "Статья: \"Тесты для списка модуля \"Оргструктура\"\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%94%D0%BE%D0%B1%D0%B0%D0%B2%D0%BB%D0%B5%D0%BD%D0%B8%D0%B5%D1%81%D1%82%D0%B0%D0%B6%D0%B5%D1%80%D1%81%D0%BA%D0%BE%D0%B9%D0%BF%D1%80%D0%BE%D0%B3%D1%80%D0%B0%D0%BC%D0%BC%D1%8B")
    @TmsLink("60192")
    @Owner(BUTINSKAYA)
    @Tag("Rmix-4")
    @Tag(MIX2)
    public void addInternProgram() {
        Employee employee = EmployeeRepository.getEmployeeWithoutIntern();
        goToOrgStructure();
        clickEmployeeTab();
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.INTERNSHIP_PROGRAM);
        clickOnPencilButton(EmployeeInfoName.INTERNSHIP_PROGRAM);
        internCheckBoxClick();
        mentorSelect();
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM).plusMonths(new Random().nextInt(6) + 1);
        selectInternProgramDateEnd(date);
        clickOnChangeButton(EmployeeInfoName.INTERNSHIP_PROGRAM);
        checkInternProgramMatches(employee);
    }

    @Test(groups = {"Rmix-6.1", "TEST-524"},
            description = "Добавление всех навыков сотруднику")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%94%D0%BE%D0%B1%D0%B0%D0%B2%D0%BB%D0%B5%D0%BD%D0%B8%D0%B5%D0%B2%D1%81%D0%B5%D1%85%D0%BD%D0%B0%D0%B2%D1%8B%D0%BA%D0%BE%D0%B2%D1%81%D0%BE%D1%82%D1%80%D1%83%D0%B4%D0%BD%D0%B8%D0%BA%D1%83")
    @TmsLink("60233")
    @Tag("Rmix-6.1")
    public void addAllSkills() {
        goToOrgStructure();
        clickEmployeeTab();
        Employee employee = EmployeeRepository.getRandomEmployee();
        int size = PresetClass.addOrDeleteSkills(employee.getId(), false);
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.SKILLS);
        clickOnPencilButton(EmployeeInfoName.SKILLS);
        clickOnSkillsCheckBox(EmployeeSkills.TUTOR);
        clickOnSkillsCheckBox(EmployeeSkills.RESPONSIBLE);
        clickOnSkillsCheckBox(EmployeeSkills.MASTER);
        clickOnChangeButton(EmployeeInfoName.SKILLS);
        checkAllSkillsAdding(size, employee.getId());
    }

    @Test(groups = {"Rmix-6.2", "TEST-524"}, description = "Добавление одного навыка, при отсутсвии остальных")
    public void addSkillWithNoSkill() {
        goToOrgStructure();
        clickEmployeeTab();
        Employee employee = EmployeeRepository.getRandomEmployee();
        int size = PresetClass.addOrDeleteSkills(employee.getId(), false);
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.SKILLS);
        clickOnPencilButton(EmployeeInfoName.SKILLS);
        clickOnSkillsCheckBox(EmployeeSkills.values()[new Random().nextInt(EmployeeSkills.values().length)]);
        clickOnChangeButton(EmployeeInfoName.SKILLS);
        checkOneSkillAdding(size, employee.getId());
    }

    @Test(groups = {"Rmix-6.3", "TEST-524"}, description = "Добавление одного навыка, при наличии других(-ого)")
    public void addOneSkillWithAnySkill() {
        goToOrgStructure();
        clickEmployeeTab();
        Employee employee = EmployeeRepository.getRandomEmployee();
        int size = PresetClass.addOrDeleteSkills(employee.getId(), true);
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.SKILLS);
        clickOnPencilButton(EmployeeInfoName.SKILLS);
        clickOnAnySkillCheckBox();
        clickOnChangeButton(EmployeeInfoName.SKILLS);
        checkOneSkillAdding(size, employee.getId());
    }

    @Test(groups = {"Rmix-6.4", "TEST-524"}, description = "Снять активные навыки у сотрудника")
    public void deleteAllActiveSkills() {
        Employee employee = EmployeeRepository.getRandomEmployee();
        int size = PresetClass.addOrDeleteSkills(employee.getId(), true);
        goToOrgStructure();
        clickEmployeeTab();
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.SKILLS);
        clickOnPencilButton(EmployeeInfoName.SKILLS);
        clickOnAllActiveCheckBoxes();
        clickOnChangeButton(EmployeeInfoName.SKILLS);
        checkSkillsDeletion(size, employee.getId());
    }

    @Test(groups = {"Rmix-11.2", "TEST-529"},
            description = "Удалить пустую должность")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%A3%D0%B4%D0%B0%D0%BB%D0%B8%D1%82%D1%8C%D0%BF%D1%83%D1%81%D1%82%D1%83%D1%8E%D0%B4%D0%BE%D0%BB%D0%B6%D0%BD%D0%BE%D1%81%D1%82%D1%8C")
    @TmsLink("60233")
    @Tag("Rmix-11.2")
    public void deleteEmptyPosition() {
        goToOrgStructure();
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS);
        searchTheModule(pair.left.getName());
        List<Position> beforeDelete = PositionRepository.getFreePositions(PositionRepository.getPositionsArray(pair.left.getId()));
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        threeDotsEmptyPositions();
        int sizeOfEmptyPositions = os.omInfoForm().emptyPositionsThreeDotsList().size();
        deletePosition();
        assertForDeleteEmptyPosition(beforeDelete, pair.left.getId(), sizeOfEmptyPositions);
    }

    @Test(groups = {"Rmix-11.1", G1, MIX1}, description = "Создать пустую должность")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%A3%D0%B4%D0%B0%D0%BB%D0%B8%D1%82%D1%8C%D0%BF%D1%83%D1%81%D1%82%D1%83%D1%8E%D0%B4%D0%BE%D0%BB%D0%B6%D0%BD%D0%BE%D1%81%D1%82%D1%8C")
    @Severity(value = SeverityLevel.NORMAL)
    @TmsLink("60233")
    @Tag("Rmix-11.1")
    @Tag(MIX1)
    public void createEmptyPost() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomStore();
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnPlusButtonEmployee();
        JobTitle jobTitle = randomJobTitle();
        chooseJob(jobTitle);
        PositionType posType = PositionTypeRepository.randomPositionType();
        selectPositionType(posType);
        PositionCategory posCat = PositionCategoryRepository.randomDynamicPositionCategory();
        selectPositionCategory(posCat);
        PositionGroup role = PositionGroupRepository.randomPositionGroup();
        selectFunctionalRole(role);
        LocalDate startDate = LocalDate.now().plusMonths(new Random().nextInt(5) + 1);
        chooseDatePositionForm(startDate, DateTypeField.POSITION_START_DATE);
        List<Position> arrayBefore = PositionRepository.emptyPositionReturner(jobTitle, startDate, unit.getId());
        saveButtonClick();
        assertionCompareMaps(arrayBefore, jobTitle, role, startDate, unit.getId());
    }

    @Test(groups = {"Rmix-12.1", G0, MIX1},
            description = "Назначить сотрудника на свободную должность")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%A3%D0%B4%D0%B0%D0%BB%D0%B8%D1%82%D1%8C%D0%BF%D1%83%D1%81%D1%82%D1%83%D1%8E%D0%B4%D0%BE%D0%BB%D0%B6%D0%BD%D0%BE%D1%81%D1%82%D1%8C")
    @Severity(value = SeverityLevel.CRITICAL)
    @TmsLink("61694")
    @Tag("Rmix-12.1")
    @Tag(MIX1)
    public void addEmpOnPosition() {
        goToOrgStructure();
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM).minusMonths(1);
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS);
        OrgUnit unit = pair.left;
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        threeDotsEmptyPositions();
        Employee employee = EmployeeRepository.getRandomEmployeeWithoutWork(unit.getId(), getCurrentEmployeesNames());
        editButtonClick();
        PositionGroup role = PositionGroup.getGroup(os.addNewEmployeeForm().functionalRoleInput().getAttribute("value"));
        clickOnSelectEmployeeChevron();
        doActionsWithEmployee(employee, startDate, role);
        List<EmployeePosition> before = EmployeePositionRepository.getEmployeePositions(PositionRepository.getPositionsArray(unit.getId()));
        enterCardNumber();
        saveButtonClick();
        assertPositionAdding(role, unit.getId(), before, employee);
    }

    @Test(groups = {"Rmix-12.2", "TEST-529"}, description = "Назначить сотрудника на создаваемую должность")
    public void addPositionToEmp() {
        goToOrgStructure();
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM);
        LocalDate endDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM).plusMonths(new Random().nextInt(2) + 1);
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITHOUT_POSITION);
        Employee employee = pair.right.getEmployee();
        OrgUnit unit = pair.left;
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnEmployeeThreeDots(employee.getFullName());
        editButtonClick();
        JobTitle jobTitle = randomJobTitle();
        chooseJob(jobTitle);
        PositionGroup role = PositionGroup.getGroup(jobTitle.getFullName());
        selectFunctionalRole(role);
        chooseDatePositionForm(startDate, DateTypeField.START_JOB);
        chooseDatePositionForm(endDate, DateTypeField.END_JOB);
        saveButtonClick();
        assertPositionAdding(role, unit.getId(), new DateInterval(startDate, endDate), employee, jobTitle);
    }

    @Test(groups = {"Rmix-12.3", "TEST-529"}, description = "Сделать должность руководителем подразделения")
    public void makeLeader() {
        goToOrgStructure();
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        EmployeePosition employeePosition = pair.right.getEmployeePosition();
        searchTheModule(pair.left.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnThreeDotsButtonByName(employeePosition.getEmployee().getFullName());
        editButtonClick();
        leaderCheckBoxClick();
        saveButtonClick();
        assertChiefAdding(pair.left.getId(), employeePosition);
    }

    @Test(groups = {"Rmix-12.4", "TEST-529"}, description = "Назначить должности функциональную роль")
    public void selectFuncRole() {
        goToOrgStructure();
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        EmployeePosition employeePosition = pair.right.getEmployeePosition();
        searchTheModule(pair.left.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnThreeDotsButtonByName(employeePosition.getEmployee().getFullName());
        editButtonClick();
        PositionGroup role = PositionGroupRepository.randomPositionGroup();
        selectFunctionalRole(role);
        saveButtonClick();
        assertRoleSelect(employeePosition, role);
    }

    @Test(groups = {"Rmix-13.1", G1, MIX1}, description = "Добавить график работы Service")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%A3%D0%B4%D0%B0%D0%BB%D0%B8%D1%82%D1%8C%D0%BF%D1%83%D1%81%D1%82%D1%83%D1%8E%D0%B4%D0%BE%D0%BB%D0%B6%D0%BD%D0%BE%D1%81%D1%82%D1%8C")
    @Tag("Rmix-13.1")
    @Tag(MIX1)
    @TmsLink("61690")
    public void addTimeTable() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomStoreWithBusinessHours(ScheduleType.ANY_TYPE);
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        clickOnSelectScheduleButton();
        ActWithSchedule actWithSchedule = new ActWithSchedule(os.selectScheduleForm());
        actWithSchedule.clickOnPlusButton();
        LocalDate dateOpen = findStartDate();
        actWithSchedule.dateOpenSelect(dateOpen);
        LocalDate dateEnd = dateOpen.plusYears(1);
        actWithSchedule.dateCloseSelect(dateEnd);
        ScheduleType service = ScheduleType.SERVICE;
        actWithSchedule.selectScheduleType(service);
        List<BusinessHours> checkListBefore = BusinessHoursRepository.checkForAvailability(unit.getId());
        actWithSchedule.clickOnSaveButton();
        List<BusinessHours> checkListAfter = BusinessHoursRepository.checkForAvailability(unit.getId());
        scheduleCheckAdding(service, dateOpen, dateEnd, checkListBefore, checkListAfter);
    }

    @Test(groups = {"Rmix-13.2", "Rmix-13.3"},
            description = "Переключение графика работы на SALE",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%9F%D0%B5%D1%80%D0%B5%D0%BA%D0%BB%D1%8E%D1%87%D0%B5%D0%BD%D0%B8%D0%B5%D0%B3%D1%80%D0%B0%D1%84%D0%B8%D0%BA%D0%B0%D1%80%D0%B0%D0%B1%D0%BE%D1%82%D1%8B%D0%BD%D0%B0SALE")
    @TmsLink("60233")
    public void changeSchedulesTypeToSale(boolean sale) {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomStoreWithBusinessHours(ScheduleType.SALE_AND_SERVICE);
        searchTheModule(unit.getName());
        int omId = unit.getId();
        ActWithSchedule actWithSchedule = new ActWithSchedule(os.selectScheduleForm());
        BusinessHours scheduleName;
        if (sale) {
            scheduleName = BusinessHoursRepository.getAnyScheduleWithTypeWithPreset(ScheduleType.SALE, omId);
            addTag("Rmix-13.2");
        } else {
            scheduleName = BusinessHoursRepository.getAnyScheduleWithTypeWithPreset(ScheduleType.SERVICE, omId);
            Allure.getLifecycle().updateTestCase(tr -> tr.setName("Переключение графика работы на SERVICE"));
            addTag("Rmix-13.3");
        }
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        actWithSchedule.selectActiveSchedule(scheduleName);
        typeChangeCheck(scheduleName);
    }

    @Test(groups = {"Rmix-13.3"}, description = "Переключение графика работы на SERVICE")
    public void changeSchedulesTypeToService() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomStoreWithBusinessHours(ScheduleType.SALE_AND_SERVICE);
        searchTheModule(unit.getName());
        int omId = unit.getId();
        ActWithSchedule actWithSchedule = new ActWithSchedule(os.selectScheduleForm());
        BusinessHours scheduleNameService = BusinessHoursRepository.getAnyScheduleWithTypeWithPreset(ScheduleType.SERVICE, omId);
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        actWithSchedule.selectActiveSchedule(scheduleNameService);
        typeChangeCheck(scheduleNameService);
    }

    @Test(groups = {"Rmix-14", G1, MIX1}, description = "Корректировка времени начала и окончания работ")
    @Link(name = "Статья: \"Тесты для списка модуля \"Оргструктура\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525")
    @Severity(value = SeverityLevel.NORMAL)
    @TmsLink("61251")
    @Tag("Rmix-14")
    @Tag(MIX1)
    public void adjustStartAndEndWorkTimes() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkOrWeekend(true);
        goToOrgStructure();
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        int orderNumber = determineActiveScheduleNumber();
        openScheduleSelectionMenu();
        String scheduleId = determineActiveScheduleId(orderNumber);
        int dayId = PresetClass.getAnyDayWithType(scheduleId, Days.DAY);
        exitScheduleSelectionMenu();
        refreshScheduleUI();
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        LocalTime startTime = LocalTime.of(new Random().nextInt(14), 20);
        LocalTime endTime = startTime.plusHours(2);
        changeDayStartTime(startTime, dayId);
        changeDayEndTime(endTime, dayId);
        clickOnEditionScheduleChangeButton();
        switchDayTimeCheck(dayId, scheduleId, startTime, endTime);
    }

    @Test(groups = {"Rmix-15.1", G1, MIX1}, description = "Скорректировать тип дня на выходной")
    @Link(name = "Статья: \"Тесты для списка модуля \"Оргструктура\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525")
    @TmsLink("61250")
    @Tag("Rmix-15.1")
    @Tag(MIX1)
    public void adjustDayToDayOff() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkOrWeekend(true);
        goToOrgStructure();
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        int orderNumber = determineActiveScheduleNumber();
        openScheduleSelectionMenu();
        String scheduleId = determineActiveScheduleId(orderNumber);
        int dayId = PresetClass.getAnyDayWithType(scheduleId, Days.DAY);
        exitScheduleSelectionMenu();
        refreshScheduleUI();
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        clickOnDayTypeChangeButton(dayId);
        switchDayTypeTo(Days.DAY_OFF);
        clickOnEditionScheduleChangeButton();
        switchDayCheck(dayId, scheduleId, Days.DAY_OFF);
    }

    @Test(groups = {"Rmix-15.2", G1, MIX1},
            description = "Скорректировать тип дня на рабочий")
    @Link(name = "Статья: \"Тесты для списка модуля \"Оргструктура\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525")
    @TmsLink("61250")
    @Tag("Rmix-15.2")
    @Tag(MIX1)
    public void adjustDayOffTypeToDay() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkOrWeekend(false);
        goToOrgStructure();
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        int orderNumber = determineActiveScheduleNumber();
        openScheduleSelectionMenu();
        String scheduleId = determineActiveScheduleId(orderNumber);
        int dayId = PresetClass.getAnyDayWithType(scheduleId, Days.DAY_OFF);
        exitScheduleSelectionMenu();
        refreshScheduleUI();
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        clickOnDayTypeChangeButton(dayId);
        switchDayTypeTo(Days.DAY);
        clickOnEditionScheduleChangeButton();
        switchDayCheck(dayId, scheduleId, Days.DAY);
    }

    @Test(groups = {"Rmix-16.1", G1, MIX1}, description = "Добавление исключения без выбора поведения KPI")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%94%D0%BE%D0%B1%D0%B0%D0%B2%D0%BB%D0%B5%D0%BD%D0%B8%D0%B5%D0%B8%D1%81%D0%BA%D0%BB%D1%8E%D1%87%D0%B5%D0%BD%D0%B8%D1%8F%D0%B1%D0%B5%D0%B7%D0%B2%D1%8B%D0%B1%D0%BE%D1%80%D0%B0%D0%BF%D0%BE%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D1%8FKPI")
    @TmsLink("60257")
    @TmsLink("60233")
    @Tag("Rmix-16.1")
    @Tag(MIX1)
    private void addingAnExceptionWithoutChoosingKPIBehavior() {
        OrgUnit unit = OrgUnitRepository.getRandomStoreWithBusinessHours(ScheduleType.ANY_TYPE);
        LocalDate date = pickDateToCreateSpecialDay(unit);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = startTime.plusHours(new Random().nextInt(5) + 1);
        goToOrgStructure();
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        Allure.step("Нажать на кнопку выбора графика работы подразделения", () -> os.omInfoForm().allAvailableSchedules().get(0).click());
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        chooseDateExceptionsSchedule(date);
        chooseTime(TimeTypeField.START_TIME, startTime.format(Format.TIME.getFormat()));
        chooseTime(TimeTypeField.END_TIME, endTime.format(Format.TIME.getFormat()));
        openType();
        chooseExceptionType(DateTypeField.WORKING);
        clickOnEditionScheduleChangeButton();
        assertSpecialDay(unit, new DateTimeInterval(LocalDateTime.of(date, startTime), LocalDateTime.of(date, endTime)));
    }

    @Test(groups = {"Rmix-16.2", G1, MIX1},
            description = "Добавление выходного в Оргструктуре")
    @Link(name = "Статья: \"Тесты для списка модуля \"Оргструктура\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525")
    @Owner(value = MATSKEVICH)
    @TmsLink("60257")
    @TmsLink("60233")
    @Tag("Rmix-16.2")
    @Tag(MIX1)
    private void addingDayOff() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomStoreWithBusinessHours(ScheduleType.ANY_TYPE);
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        chooseDateExceptionsSchedule(date);
        openType();
        chooseExceptionType(DateTypeField.DAY_OFF);
        clickOnEditionScheduleChangeButton();
        assertDayOff(unit.getName(), unit.getId(), date);
    }

    @Test(groups = {"ABCHR4600-3", G2, MIX1},
            description = "Невозможно добавить тег подразделению без разрешения")
    @Link(name = "Статья: \"4600_Добавить права на управление тегами\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=223511977&moved=true")
    @Owner(value = MATSKEVICH)
    @TmsLink("60222")
    @Tag("ABCHR4600-3")
    @Tag(MIX1)
    private void cannotEditTagsInOrgUnit() {
        OrgUnit unit = OrgUnitRepository.getRandomStoreWithBusinessHours(ScheduleType.ANY_TYPE);
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(PermissionType.ORGANIZATION_UNIT_VIEW,
                                                                             PermissionType.ORGANIZATION_UNIT_EDIT,
                                                                             PermissionType.SYS_LIST_ORG_TYPES_READ));
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        goToOrgStructureAsUser(role, unit);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        Assert.assertThrows(NoSuchElementException.class, this::clickTags);
    }

    @Test(groups = {"ABCHR4600-5", G2, MIX2},
            description = "Пользователь с разрешением на свои теги не может добавить тег в карточку другого пользователя")
    @Link(name = "Статья: \"4600_Добавить права на управление тегами\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=223511977&moved=true")
    @Owner(value = MATSKEVICH)
    @TmsLink("60222")
    @Tag("ABCHR4600-5")
    @Tag(MIX2)
    private void cannotEditTagsForAnotherEmployee() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(PermissionType.ORGANIZATION_UNIT_VIEW,
                                                                             PermissionType.ORGANIZATION_UNIT_EDIT,
                                                                             PermissionType.ORG_EMPLOYEE_EDIT,
                                                                             PermissionType.EDIT_SELF_TAGS,
                                                                             PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        User user = PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(10), null, unit);
        goToOrgStructureAsUser(role, user);
        Employee employee = EmployeeRepository.getRandomWorkingEmployee(unit.getId(), user.getEmployee());
        clickEmployeeTab();
        chooseEmployee(employee.getFullName());
        changeEmp();
        Assert.assertThrows(NoSuchElementException.class, this::clickTags);
    }

    @Test(groups = {"ABCHR4600-6", G2, MIX2},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Невозможно добавить тег в свою карточку сотрудника без разрешения")
    @Link(name = "Статья: \"4600_Добавить права на управление тегами\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=223511977&moved=true")
    @Owner(value = MATSKEVICH)
    @TmsLink("60222")
    @Tag(MIX2)
    @Tag("ABCHR4600-6")
    private void cannotEditTagsForEmployee(boolean selectMyself) {
        changeStepNameIfTrue(!selectMyself, "Невозможно добавить тег в карточку другого сотрудника без разрешения");
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(PermissionType.ORGANIZATION_UNIT_VIEW,
                                                                             PermissionType.ORGANIZATION_UNIT_EDIT,
                                                                             PermissionType.ORG_EMPLOYEE_EDIT,
                                                                             PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        User user = PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(10), null);
        Employee employee = user.getEmployee();
        OrgUnit unit = EmployeePositionRepository.getFirstActiveEmployeePositionFromEmployee(employee).getOrgUnit();
        goToOrgStructureAsUser(role, user);
        if (!selectMyself) {
            employee = EmployeeRepository.getRandomWorkingEmployee(unit.getId(), employee);
        }
        clickEmployeeTab();
        chooseEmployee(employee.getFullName());
        changeEmp();
        Assert.assertThrows(NoSuchElementException.class, this::clickTags);
    }

    @Test(groups = {"ABCHR4629", G2, MIX1,
            "@Before show all employee groups"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Изменение контактных данных подразделения (пользователь)")
    @Link(name = "Статья: \"4629_Добавить права на контактную информацию в карточке сотрудника\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=223512788")
    @Owner(value = MATSKEVICH)
    @TmsLink("60017")
    @Tag(MIX1)
    @Tag("ABCHR4629-1")
    private void addContactInformation(boolean editContacts) {
        changeTestIDDependingOnParameter(editContacts, "ABCHR4629-1", "ABCHR4629-2",
                                         "Изменение контактных данных подразделения (пользователь) без прав");
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(PermissionType.ORGANIZATION_UNIT_VIEW,
                                                                             PermissionType.ORGANIZATION_UNIT_EDIT,
                                                                             PermissionType.SYS_LIST_ORG_TYPES_READ));
        if (editContacts) {
            permissionTypes.add(PermissionType.ORGANIZATION_UNIT_CONTACTS_EDIT);
        }
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        OmInfoName omInfoName = OmInfoName.CONTACTS;
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        int unitId = unit.getId();
        goToOrgStructureAsUser(role, unit);
        searchTheModule(unit.getName());
        clickOnChevronButton(omInfoName);
        if (editContacts) {
            ContactFields contactField = ContactFields.PHONE_TYPE.randomContact();
            pressPencilAtOm(omInfoName);
            if (!os.omInfoForm().addressDeleteButtons().isEmpty()) {
                os.omInfoForm().addressDeleteButtons().stream().forEach(b -> b.click());
            }
            String value = editContactField(contactField, false);
            changeInfoCardButtonClick();
            assertContactEdited(unitId, contactField, value, false);
        } else {
            Assert.assertThrows(ElementNotInteractableException.class, () -> pressPencilAtOm(omInfoName));
        }
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR4629", G2, MIX1,
            "@Before show all employee groups"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Изменение контактных данных сотрудника (пользователь)")
    @Link(name = "Статья: \"4629_Добавить права на контактную информацию в карточке сотрудника\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=223512788")
    @Owner(BUTINSKAYA)
    @TmsLink("60017")
    @Tag(MIX1)
    @Tag("ABCHR4629-3")
    private void addContactInformationToEmployee(boolean editContacts) {
        changeTestIDDependingOnParameter(editContacts, "ABCHR4629-3", "ABCHR4629-4",
                                         "Нельзя изменить контактные данные сотрудника без разрешения (пользователь)");
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(PermissionType.ORGANIZATION_UNIT_VIEW,
                                                                             PermissionType.ORGANIZATION_UNIT_EDIT,
                                                                             PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        if (editContacts) {
            permissionTypes.add(PermissionType.ORG_EMPLOYEE_CONTACT_EDIT);
        }
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        EmployeeInfoName empInfoName = EmployeeInfoName.CONTACTS;
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeePosition(unit);
        goToOrgStructureAsUserWithoutWait(role, unit, ep.getEmployee().getUser());
        clickEmployeeTab();
        chooseEmployee(ep.getEmployee().getFullName());
        clickOnShowButton(empInfoName);
        if (editContacts) {
            ContactFields contactField = ContactFields.randomContact();
            clickOnPencilButton(empInfoName);
            if (!os.omInfoForm().addressDeleteButtons().isEmpty()) {
                os.omInfoForm().addressDeleteButtons().stream().forEach(b -> b.click());
            }
            String value = editContactField(contactField, true);
            changeInfoCardButtonClick();
            assertContactEdited(ep.getEmployee().getId(), contactField, value, true);
        } else {
            Assert.assertThrows(ElementNotInteractableException.class, () -> clickOnPencilButton(empInfoName));
        }
    }

    @Test(groups = {"ABCHR7494", G2, MIX1, IN_PROGRESS,
            "@Before show all employee groups"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Права на редактирование контактов всех сотрудников")
    @Link(name = "Статья: \"7494_Права на редактирование контактных данных сотрудников\"",
            url = "https://wiki.goodt.me/x/hP_6Dw")
    @TmsLink("118714")
    @Tag(MIX1)
    private void editEmployeeContactInformationAsUser(boolean editContacts) {
        changeTestIDDependingOnParameter(editContacts, "ABCHR7494-1", "ABCHR7494-3",
                                         "Отсутствие прав на редактирование контактов всех сотрудников");
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(PermissionType.ORGANIZATION_UNIT_VIEW,
                                                                             PermissionType.ORGANIZATION_UNIT_EDIT,
                                                                             PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        if (editContacts) {
            permissionTypes.add(PermissionType.ORG_EMPLOYEE_CONTACT_EDIT_ALL);
        }
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        EmployeeInfoName empInfoName = EmployeeInfoName.CONTACTS;
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        List<Employee> twoEmployees = getRandomFromList(EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(unit.getId(), LocalDate.now(), true), 2)
                .stream()
                .map(EmployeePosition::getEmployee)
                .collect(Collectors.toList());
        User user = twoEmployees.get(0).getUser();
        goToOrgStructureAsUserWithoutWait(role, unit, user);
        clickEmployeeTab();
        //выставление фильтра нужно, чтобы на ui находились нужные сотрудники, если у пользователя доступ к нескольким оргюнитам
        setOrgUnitFilterInEmployeesTab(unit.getName());
        for (Employee employee : twoEmployees) {
            chooseEmployee(employee.getFullName());
            clickOnShowButton(empInfoName);
            if (editContacts) {
                ContactFields contactField = ContactFields.randomContact();
                clickOnPencilButton(empInfoName);
                if (!os.omInfoForm().addressDeleteButtons().isEmpty()) {
                    os.omInfoForm().addressDeleteButtons().stream().forEach(b -> b.click());
                }
                String value = editContactField(contactField, true);
                changeInfoCardButtonClick();
                assertContactEdited(employee.getId(), contactField, value, true);
            } else {
                Assert.assertThrows(ElementNotInteractableException.class, () -> clickOnPencilButton(empInfoName));
            }
            clearEmployeeSearchField();
        }
    }

    @Test(groups = {"ABCHR7494", G2, MIX1, IN_PROGRESS,
            "@Before show all employee groups"},
            description = "Права на редактирование личных контактов")
    @Link(name = "Статья: \"7494_Права на редактирование контактных данных сотрудников\"",
            url = "https://wiki.goodt.me/x/hP_6Dw")
    @TmsLink("118714")
    @Tag(MIX1)
    @Tag("ABCHR7494-2")
    private void editOwnContactInformationAsUser() {
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(PermissionType.ORGANIZATION_UNIT_VIEW,
                                                                             PermissionType.ORGANIZATION_UNIT_EDIT,
                                                                             PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                             PermissionType.ORG_EMPLOYEE_CONTACT_EDIT));
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        EmployeeInfoName empInfoName = EmployeeInfoName.CONTACTS;
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        List<Employee> twoEmployees = getRandomFromList(EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(unit.getId(), LocalDate.now(), true), 2)
                .stream()
                .map(EmployeePosition::getEmployee)
                .collect(Collectors.toList());
        Employee employeeSelf = twoEmployees.get(0);
        User user = employeeSelf.getUser();
        goToOrgStructureAsUserWithoutWait(role, unit, user);
        clickEmployeeTab();
        //выставление фильтра нужно, чтобы на ui находились нужные сотрудники, если у пользователя доступ к нескольким оргюнитам
        setOrgUnitFilterInEmployeesTab(unit.getName());
        chooseEmployee(employeeSelf.getFullName());
        clickOnShowButton(empInfoName);
        ContactFields contactField = ContactFields.randomContact();
        clickOnPencilButton(empInfoName);
        if (!os.omInfoForm().addressDeleteButtons().isEmpty()) {
            os.omInfoForm().addressDeleteButtons().stream().forEach(b -> b.click());
        }
        String value = editContactField(contactField, true);
        changeInfoCardButtonClick();
        assertContactEdited(employeeSelf.getId(), contactField, value, true);
        clearEmployeeSearchField();
        chooseEmployee(twoEmployees.get(1).getFullName());
        clickOnShowButton(empInfoName);
        Assert.assertThrows(ElementNotInteractableException.class, () -> clickOnPencilButton(empInfoName));
    }

    @Test(groups = {"Rmix-10.1", G2, MIX1},
            description = "Добавление тега к подразделению с уже имеющимся тегом")
    @Link(name = "Статья: \"Тесты для списка модуля \"Оргструктура\"\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%98%D1%81%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5%D1%84%D0%B8%D0%BB%D1%8C%D1%82%D1%80%D0%B0%22%D0%94%D0%BE%D0%BB%D0%B6%D0%BD%D0%BE%D1%81%D1%82%D1%8C%22%D0%B2%22%D0%A1%D0%BE%D1%82%D1%80%D1%83%D0%B4%D0%BD%D0%B8%D0%BA%D0%B0%D1%85%22")
    @TmsLink("117464")
    @Tag("Rmix-10.1")
    @Tag(MIX1)
    private void addingTagToOrganizationalUnitWithExistingTag() {
        boolean isNumeric = ThreadLocalRandom.current().nextBoolean();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        List<String> tagsBefore = PresetClass.tagPreset(unit, TagValue.ONE);
        String tag = isNumeric ? RandomStringUtils.randomNumeric(10) : RandomStringUtils.randomAlphanumeric(10);
        goToOrgStructure();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        enterTags(tag);
        changeInfoCardButtonClick();
        assertChangingOmTags(tag, unit, tagsBefore);
    }

    @Test(groups = {"TEST-528", "Rmix-10.2"}, description = "Добавление тега к подразделению с несколькими тегами")
    private void addingTagToOrganizationalUnitWithExistingManyTags() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        List<String> tagsBefore = PresetClass.tagPreset(unit, TagValue.SEVERAl);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String tag = RandomStringUtils.randomAlphabetic(10);
        enterTags(tag);
        clickAddTags();
        changeInfoCardButtonClick();
        assertChangingOmTags(tag, unit, tagsBefore);
    }

    @Test(groups = {"Rmix-10.3", G2, MIX1},
            description = "Добавление тега к подразделению без тегов")
    @Link(name = "Статья: \"Тесты для списка модуля \"Оргструктура\"\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%98%D1%81%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5%D1%84%D0%B8%D0%BB%D1%8C%D1%82%D1%80%D0%B0%22%D0%94%D0%BE%D0%BB%D0%B6%D0%BD%D0%BE%D1%81%D1%82%D1%8C%22%D0%B2%22%D0%A1%D0%BE%D1%82%D1%80%D1%83%D0%B4%D0%BD%D0%B8%D0%BA%D0%B0%D1%85%22")
    @TmsLink("117464")
    @Tag("Rmix-10.3")
    @Tag(MIX1)
    private void addingTagToOrganizationalUnitWithNoTags() {
        boolean isNumeric = ThreadLocalRandom.current().nextBoolean();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        List<String> tagsBefore = PresetClass.tagPreset(unit, TagValue.NO_ONE);
        String tag = isNumeric ? RandomStringUtils.randomNumeric(10) : RandomStringUtils.randomAlphanumeric(10);
        goToOrgStructure();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        enterTags(tag);
        changeInfoCardButtonClick();
        assertChangingOmTags(tag, unit, tagsBefore);
    }

    @Test(groups = {"Rmix-10.4", G1, MIX1},
            description = "Удаление тега с имеющимся одним тегом")
    @Link(name = "Статья: \"Тесты для списка модуля \"Оргструктура\"\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%98%D1%81%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5%D1%84%D0%B8%D0%BB%D1%8C%D1%82%D1%80%D0%B0%22%D0%94%D0%BE%D0%BB%D0%B6%D0%BD%D0%BE%D1%81%D1%82%D1%8C%22%D0%B2%22%D0%A1%D0%BE%D1%82%D1%80%D1%83%D0%B4%D0%BD%D0%B8%D0%BA%D0%B0%D1%85%22")
    @TmsLink("60628")
    @Tag("Rmix-10.4")
    @Tag(MIX1)
    private void deletingTagToOrganizationalUnitWithExistingTag() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        List<String> tags = PresetClass.tagPreset(unit, TagValue.ONE);
        String tagsBefore = String.join(",", tags);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String tag = CommonRepository.getOrgUnitTag(unit);
        deleteTag(tag);
        changeInfoCardButtonClick();
        assertDeletingOmTags(tag, unit, tagsBefore, NumberOfTags.ONE_TAG);
    }

    @Test(groups = {"TEST-528", "Rmix-10.5"}, description = "Удаление тега с имеющимся несколькими тегами")
    private void deletingTagToOrganizationalUnitWithExistingManyTag() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        List<String> tags = PresetClass.tagPreset(unit, TagValue.SEVERAl);
        String tagsBefore = String.join(",", tags);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String tag = CommonRepository.getOrgUnitTag(unit);
        deleteTag(tag);
        changeInfoCardButtonClick();
        assertDeletingOmTags(tag, unit, tagsBefore, NumberOfTags.MANY_TAG);
    }

    @Test(groups = {"TEST-717", "Rmix-18.1"}, description = "Ввод некорректных значений в поле \"Дата открытия\"")
    private void enteringInvalidValuesInOpenDateField() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String sendInData = RandomStringUtils.random(6, 51, 57, false, true);
        changeOmDate(DateTypeField.OPEN_DATE, sendInData);
        assertHighlightInRedAndWrongDateMessage(DateTypeField.OPEN_DATE);
    }

    @Test(groups = {"TEST-717", "Rmix-18.2"}, description = "Ввод некорректных значений в поле \"Дата закрытия\"")
    private void enteringInvalidValuesInCloseDateField() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String sendInData = RandomStringUtils.random(6, 51, 57, false, true);
        changeOmDate(DateTypeField.CLOSE_DATE, sendInData);
        assertHighlightInRedAndWrongDateMessage(DateTypeField.CLOSE_DATE);
    }

    @Test(groups = {"TEST-717", "Rmix-18.3", "not actual"}, description = "Ввод некорректных значений в поле \"Дата начала замещения\"")
    private void enteringInvalidValuesInStartDeputyDateField() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String sendInData = RandomStringUtils.random(6, 51, 57, false, true);
        changeOmDate(DateTypeField.START_DEPUTY_DATE, sendInData);
        assertHighlightInRedAndWrongDateMessage(DateTypeField.START_DEPUTY_DATE);
    }

    @Test(groups = {"TEST-717", "Rmix-18.4", "not actual"}, description = "Ввод некорректных значений в поле \"Дата окончания замещения\"")
    private void enteringInvalidValuesInEndDeputyDateField() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String sendInData = RandomStringUtils.random(6, 51, 57, false, true);
        changeOmDate(DateTypeField.END_DEPUTY_DATE, sendInData);
        assertHighlightInRedAndWrongDateMessage(DateTypeField.END_DEPUTY_DATE);
    }

    @Test(groups = {"TEST-718", "Rmix-19"}, description = "Некорректное добавление сотрудника")
    private void incorrectEmployeeAddition() {
        goToOrgStructure();
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS);
        searchTheModule(pair.left.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        threeDotsEmptyPositions();
        Employee employee = EmployeeRepository.getRandomEmployeeWithoutWork(pair.left.getId(), getCurrentEmployeesNames());
        editButtonClick();
        clickOnSelectEmployeeChevron();
        chooseEmployeeDoesNotWorkHere(employee);
        LocalDate start = LocalDate.parse(os.addNewEmployeeForm()
                                                  .inputVariantDate(DateTypeField.START_JOB.getName()).getAttribute("value"),
                                          DateTimeFormatter.ofPattern("d MMMM yyyy").withLocale(new Locale("ru", "RU")));
        chooseDatePositionForm(start.minusDays(new Random().nextInt(100) + 1), DateTypeField.END_JOB);
        assertThatForIncorrectEmployeeAddition(start);
    }

    @Test(groups = {"TEST-719", "Rmix-20"}, description = "Некорректное добавление графика работы")
    private void incorrectWorkingGraphAddition() {
        goToOrgStructure();
        OrgUnit unit = OrgUnitRepository.getRandomStoreWithBusinessHours(ScheduleType.ANY_TYPE);
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        clickOnSelectScheduleButton();
        ActWithSchedule actWithSchedule = new ActWithSchedule(os.selectScheduleForm());
        actWithSchedule.clickOnPlusButton();
        actWithSchedule.clickOnSaveButtonWithError();
        assertErrorField();
    }

    @Test(groups = {"TK2415-1-1", "TEST-948"}, description = "Выбор сотрудника при назначении на свободную должность" +
            " с учетом значения параметра \"Разрешить менеджеру назначать на должность из всех сотрудников\"")
    public void addEmpOnPositionWithAllowed() {
        PresetClass.setSystemPropertyValue(SystemProperties.MANAGER_ALL_EMPLOYEES_ALLOW, true);
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS);
        OrgUnit unit = pair.left;
        goToOrgStructureAsUserWithoutWait(Role.FIRST, unit);
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.THAT);
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        threeDotsEmptyPositions();
        ImmutablePair<OrgUnit, EmployeeEssence> pair2 = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        editButtonClick();
        clickOnSelectEmployeeChevron();
        selectAnyEmployee(pair2.right.getEmployee().getFullName());
        chooseDatePositionForm(startDate, DateTypeField.START_JOB);
        PositionGroup role = PositionGroup.getGroup(os.addNewEmployeeForm().functionalRoleInput().getAttribute("value"));
        List<EmployeePosition> before = EmployeePositionRepository.getEmployeePositions(PositionRepository.getPositionsArray(unit.getId()));
        saveButtonClick();
        assertPositionAdding(role, unit.getId(), before, pair2.right.getEmployee());
    }

    @Test(groups = {"TK2415-1-2", "TEST-948"}, description = "Выбор сотрудника при назначении на свободную должность" +
            " с учетом значения параметра \"Разрешить менеджеру назначать на должность из всех сотрудников\" с отключенным параметром",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + EMPLOYEE_NOT_DISPLAYED + ANY)
    public void addEmpOnPositionWithoutAllowed() {
        PresetClass.setSystemPropertyValue(SystemProperties.MANAGER_ALL_EMPLOYEES_ALLOW, false);
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS);
        goToOrgStructureAsUserWithoutWait(Role.FIRST, pair.left);
        searchTheModule(pair.left.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        threeDotsEmptyPositions();
        Employee employee = EmployeeRepository.getRandomEmployeeWithoutWork(pair.left.getId(), getCurrentEmployeesNames());
        editButtonClick();
        clickOnSelectEmployeeChevron();
        selectAnyEmployee(employee.getFullName());
    }

    @Test(groups = {"TK2415-2-1", "TEST-948"}, description = "Выбор сотрудника при назначении на создаваемую должность" +
            " с учетом значения параметра \"Разрешить менеджеру назначать на должность из всех сотрудников\"")
    public void addEmpOnNewPositionWithAllowed() {
        PresetClass.setSystemPropertyValue(SystemProperties.MANAGER_ALL_EMPLOYEES_ALLOW, true);
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS);
        goToOrgStructureAsUserWithoutWait(Role.FIRST, pair.left);
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.THAT);
        searchTheModule(pair.left.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        Employee employee = EmployeeRepository.getRandomEmployeeWithoutWork(pair.left.getId(), getCurrentEmployeesNames());
        clickOnPlusButtonEmployee();
        clickOnSelectEmployeeChevron();
        selectAnyEmployee(employee.getFullName());
        chooseDatePositionForm(startDate, DateTypeField.START_JOB);
        JobTitle jobTitle = randomJobTitle();
        chooseJob(jobTitle);
        saveButtonClick();
        assertPositionAdding(null, pair.left.getId(), new DateInterval(startDate, null), employee, jobTitle);
    }

    @Test(groups = {"TK2415-2-2", "TEST-948"}, description = "Выбор сотрудника при назначении на создаваемую должность " +
            "с учетом значения параметра \"Разрешить менеджеру назначать на должность из всех сотрудников\" без доступа")
    public void addEmpOnNewPositionWithoutAllowed() {
        PresetClass.setSystemPropertyValue(SystemProperties.MANAGER_ALL_EMPLOYEES_ALLOW, false);
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS);
        goToOrgStructureAsUserWithoutWait(Role.FIRST, pair.left);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        Employee employee = EmployeeRepository.getRandomEmployeeWithoutWork(pair.left.getId(), getCurrentEmployeesNames());
        clickOnPlusButtonEmployee();
        clickOnSelectEmployeeChevron();
        checkSearchEmpty(employee.getFullName());
    }

    @Test(groups = {"TK2041-2", G1, MIX1},
            description = "Добавление подразделения к участвующим в расчете",
            dataProvider = "roles 1, 4", dataProviderClass = DataProviders.class)
    @Tag("TK2041-2")
    @Tag(MIX1)
    @Severity(value = SeverityLevel.NORMAL)
    public void involveInCalculation(Role role) {
        PresetClass.setCaptureLogsAttribute();
        OrgUnit unit = OrgUnitRepository.getRndOrgUnitCalculatedFalse();
        goToOrgStructureAsUserWithoutWait(role, unit);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        clickCalculationFlag();
        changeInfoCardButtonClick();
        assertChangingOmCalc(true);
    }

    @Test(groups = {"TK2041-2", G1, MIX1},
            description = "Добавление подразделения к участвующим в расчете без доступа",
            dataProvider = "roles 5-10", dataProviderClass = DataProviders.class,
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + CHECKBOX_NOT_DISPLAYED + ANY)
    @Severity(value = SeverityLevel.NORMAL)
    @Tag("TK2041-2")
    @Tag(MIX1)
    public void involveInCalculationWithoutPermissions(Role role) {
        PresetClass.setCaptureLogsAttribute();
        OrgUnit unit = OrgUnitRepository.getRndOrgUnitCalculatedFalse();
        goToOrgStructureAsUserWithoutWait(role, unit);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        clickCalculationFlag();
    }

    @Test(groups = {"TK2041-3.1"}, description = "Назначить на должность", dataProvider = "positionAdding")
    public void positionAdding(Role role, LocalDate date) {
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS);
        goToOrgStructureAsUserWithoutWait(role, pair.left);
        searchTheModule(pair.left.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        threeDotsEmptyPositions();
        editButtonClick();
        clickOnSelectEmployeeChevron();
        Employee employee = EmployeeRepository.getRandomEmployeeWithoutWorkWithUiCheck(getFreeEmployeeFromUi());
        selectAnyEmployee(employee.getFullName());
        selectInCalendarPositionDate(date, DateTypeField.START_JOB);
        JobTitle jobTitle = getJob(os.addNewEmployeeForm().inputJobCategory().getAttribute("value"));
        saveButtonClick();
        assertPositionAdding(null, pair.left.getId(), new DateInterval(date, null), employee, jobTitle);
    }

    @Test(groups = {"TK2041-3.2"}, description = "Назначить на должность (без полномочий)",
            dataProvider = "positionWithoutPermissionAdding",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + DATE_DOES_NOT_MATCH + ANY)
    public void addEmpOnPositionWithoutPermission(Role role, LocalDate date) {
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS);
        goToOrgStructureAsUserWithoutWait(role, pair.left);
        searchTheModule(pair.left.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        threeDotsEmptyPositions();
        Employee employee = EmployeeRepository.getRandomEmployeeWithoutWork(pair.left.getId(), getCurrentEmployeesNames());
        editButtonClick();
        clickOnSelectEmployeeChevron();
        selectAnyEmployee(employee.getFullName());
        selectInCalendarPositionDate(date, DateTypeField.START_JOB);
    }

    @Test(groups = {"TK2041-4.1", G0, MIX1},
            description = "Увольнение с позиции", dataProvider = "addEndDateNowAndInFuture")
    @Link(url = "https://wiki.goodt.me/x/IAPFCw", name = "Статья: \"2041_Применение роли в системе блок \"Оргструктура\"\"")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("TK2041-4.1")
    @Tag(MIX1)
    public void addEndDateNowAndInFuture(Role role, ShiftTimePosition timePosition) {
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToOrgStructureAsUserWithoutWait(role, pair.left);
        OrgUnit orgUnit = pair.left;
        searchTheModule(orgUnit.getName());
        Employee employee = pair.right.getEmployeePosition().getEmployee();
        LocalDate date = getEndDateForEmployeeFromTimePosition(employee, timePosition);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        String employeeName = employee.getFullName();
        clickOnEmployeeThreeDots(employeeName);
        editButtonClick();
        if (role == Role.EIGHTH) {
            selectInCalendarPositionDateAndCheckDateClickable(date, DateTypeField.END_JOB);
        } else {
            selectInCalendarPositionDate(date, DateTypeField.END_JOB);
            saveButtonClick();
            assertDateEndAvailability(date, pair.right.getEmployeePosition());
        }
    }

    @Test(groups = {"TK2041-5.1"}, description = "Изменить дату окончания должности", dataProvider = "positionStartDateChanging")
    public void changePositionStartDate(Role role, LocalDate date) {
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToOrgStructureAsUserWithoutWait(role, pair.left);
        OrgUnit orgUnit = pair.left;
        searchTheModule(orgUnit.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        String employeeName = pair.right.getEmployee().getFullName();
        clickOnEmployeeThreeDots(employeeName);
        editButtonClick();
        selectInCalendarPositionDate(date, DateTypeField.POSITION_END_DATE);
        saveButtonClick();
        assertPositionStartDateChanged(date, orgUnit, pair.getRight().getEmployeePosition());
    }

    @Test(groups = {"TK2041-5.2.1"}, description = "Изменить дату начала должности, с недоступными временными интервалами",
            dataProvider = "positionStartDateChangingWithoutPermissions")
    public void changePositionStartDateWithoutPermissions(Role role, LocalDate date) {
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToOrgStructureAsUserWithoutWait(role, pair.left);
        OrgUnit orgUnit = pair.left;
        searchTheModule(orgUnit.getName());
        Employee employee = pair.right.getEmployeePosition().getEmployee();
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnEmployeeThreeDots(employee.getFullName());
        editButtonClick();
        chooseDatePositionForm(date, DateTypeField.POSITION_END_DATE);
        saveButtonWithoutWait();
        checkCalendarButtonNotDisplayed("placeholder");
    }

    @Test(groups = {"TK2041-5.2.2"}, description = "Изменить дату начала должности, с неактивным полем",
            dataProvider = "roles 4-8", dataProviderClass = DataProviders.class, expectedExceptions = AssertionError.class,
            expectedExceptionsMessageRegExp = ANY + CALENDAR_BUTTON_NOT_DISPLAYED + ANY)
    public void changePositionStartDateWithoutField(Role role) {
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToOrgStructureAsUserWithoutWait(role, pair.left);
        OrgUnit orgUnit = pair.left;
        searchTheModule(orgUnit.getName());
        Employee employee = pair.right.getEmployeePosition().getEmployee();
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnEmployeeThreeDots(employee.getFullName());
        editButtonClick();
        selectInCalendarPositionDate(LocalDate.now(), DateTypeField.POSITION_END_DATE);
    }

    @Test(groups = {"TK2041-6.1"}, description = "Проверить, что дата начала работы недоступна для изменений",
            dataProvider = "roles 7-8", dataProviderClass = DataProviders.class, expectedExceptions = AssertionError.class,
            expectedExceptionsMessageRegExp = ANY + CALENDAR_BUTTON_NOT_DISPLAYED + ANY)
    public void checkCalendarStartNotDisplayed(Role role) {
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToOrgStructureAsUserWithoutWait(role, pair.left);
        OrgUnit orgUnit = pair.left;
        searchTheModule(orgUnit.getName());
        Employee employee = pair.right.getEmployeePosition().getEmployee();
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnEmployeeThreeDots(employee.getFullName());
        editButtonClick();
        selectInCalendarPositionDate(LocalDate.now(), DateTypeField.START_JOB);
    }

    @Test(groups = {"TK2041-6.2"}, description = "Проверить, что дата окончания работы недоступна для изменений",
            dataProvider = "roles 5-6", dataProviderClass = DataProviders.class, expectedExceptions = AssertionError.class,
            expectedExceptionsMessageRegExp = ANY + CALENDAR_BUTTON_NOT_DISPLAYED + ANY)
    public void checkCalendarEndNotDisplayed(Role role) {
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToOrgStructureAsUserWithoutWait(role, pair.left);
        OrgUnit orgUnit = pair.left;
        searchTheModule(orgUnit.getName());
        Employee employee = pair.right.getEmployeePosition().getEmployee();
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnEmployeeThreeDots(employee.getFullName());
        editButtonClick();
        selectInCalendarPositionDate(LocalDate.now(), DateTypeField.END_JOB);
    }

    @Test(groups = {"TK2041-6.3"}, description = "Проверить, что даты окончания и начала работы недоступны для изменений",
            dataProvider = "roles 4, 9-10", dataProviderClass = DataProviders.class)
    public void checkCalendarBothDisplayed(Role role) {
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToOrgStructureAsUserWithoutWait(role, pair.left);
        OrgUnit orgUnit = pair.left;
        searchTheModule(orgUnit.getName());
        Employee employee = pair.right.getEmployeePosition().getEmployee();
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnEmployeeThreeDots(employee.getFullName());
        editButtonClick();
        assertDatesNotActive();
    }

    @Test(groups = {"TK2041-3-3"}, description = "Назначение на должность. Роль 3 (нет полномочий)",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + EDIT_BUTTON_NOT_DISPLAYED + ANY)
    public void checkEmployeeEditButtonNotDisplayed() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS).left;
        goToOrgStructureAsUserWithoutWait(Role.THIRD, unit);
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        threeDotsEmptyPositions();
        editButtonClick();
    }

    @Test(groups = {"TK2041-2-3"}, description = "Редактирование информации об ОМ. Роль 3 (без полномочий)",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".* кнопка для редактирования.*")
    public void checkThirdRoleChangeOmInfo() {
        OrgUnit unit = OrgUnitRepository.getRndOrgUnitCalculatedFalse();
        goToOrgStructureAsUserWithoutWait(Role.THIRD, unit);
        searchTheModule(unit.getName());
        pressMainDataPencil();
    }

    @Test(groups = {"TK2041-3.1-1"}, description = "Назначить на позицию в прошлом, 1 роль")
    public void positionInPastFirstRole() {
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS);
        goToOrgStructureAsUserWithoutWait(Role.FIRST, pair.left);
        DateInterval dateInterval = new DateInterval(LocalDate.now().minusDays(new Random().nextInt(30) + 1), null);
        ImmutablePair<Position, Integer> positionIndex = PositionRepository.getRandomPosition(
                PositionRepository.getFreePositions(PositionRepository.getPositionsArray(pair.left.getId()), dateInterval),
                PositionRepository.getFreePositions(PositionRepository.getPositionsArray(pair.left.getId())));
        searchTheModule(pair.left.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        threeDotsEmptyPositions(positionIndex.left, positionIndex.right);
        Employee employee = EmployeeRepository.getRandomEmployeeWithoutWork(pair.left.getId(), getCurrentEmployeesNames());
        editButtonClick();
        clickOnSelectEmployeeChevron();
        selectAnyEmployee(employee.getFullName());
        selectInCalendarPositionDate(dateInterval.startDate, DateTypeField.START_JOB);
        JobTitle jobTitle = getJob(os.addNewEmployeeForm().inputJobCategory().getAttribute("value"));
        saveButtonClick();
        assertPositionAdding(null, pair.left.getId(), dateInterval, employee, jobTitle);
    }

    @Test(groups = {"TK2041-3.1-5"}, description = "Назначить на позицию в прошлом, 5 роль")
    public void positionInPastSixthRole() {
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.WITH_FREE_POSITIONS);
        goToOrgStructureAsUserWithoutWait(Role.SIXTH, pair.left);
        DateInterval dateInterval = new DateInterval(LocalDate.now().minusDays(new Random().nextInt(30) + 1), null);
        ImmutablePair<Position, Integer> positionIndex = PositionRepository.getRandomPosition(
                PositionRepository.getFreePositions(PositionRepository.getPositionsArray(pair.left.getId()), dateInterval),
                PositionRepository.getFreePositions(PositionRepository.getPositionsArray(pair.left.getId())));
        searchTheModule(pair.left.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        threeDotsEmptyPositions(positionIndex.left, positionIndex.right);
        Employee employee = EmployeeRepository.getRandomEmployeeWithoutWork(pair.left.getId(), getCurrentEmployeesNames());
        editButtonClick();
        clickOnSelectEmployeeChevron();
        selectAnyEmployee(employee.getFullName());
        selectInCalendarPositionDate(dateInterval.startDate, DateTypeField.START_JOB);
        JobTitle jobTitle = getJob(os.addNewEmployeeForm().inputJobCategory().getAttribute("value"));
        saveButtonClick();
        assertPositionAdding(null, pair.left.getId(), dateInterval, employee, jobTitle);
    }

    @Test(groups = {"TK2784-1.1", "TEST-1106"}, description = "Изменение outerId оргюнита, 1 роль")
    public void changeOuterId1() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        goToOrgStructureAsUserWithoutWait(Role.FIRST, unit);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String randomName = CustomTools.stringGenerator();
        changeOmsOuterId(randomName);
        changeInfoCardButtonClick();
        assertChangingOuterId(randomName, unit);
    }

    @Test(groups = {"TK2784-1.4", "TEST-1106"}, description = "Изменение outerId оргюнита, 4 роль (без полномочий)",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".* редактирование outerId не было.*")
    public void changeOuterId3() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        goToOrgStructureAsUserWithoutWait(Role.FOURTH, unit);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String randomName = CustomTools.stringGenerator();
        changeOmsOuterId(randomName);
        changeInfoCardButtonClick();
        assertChangingOuterId(randomName, unit);
    }

    @Test(groups = {"TK2784-2.1", "TK2784-2", G1, MIX1},
            description = "Редактирование графика работы. 1 роль")
    @Link(name = "Статья: \"2784_Применение роли в системе блок \"Оргструктура\" Расширение", url = "https://wiki.goodt.me/x/uQEUD")
    @TmsLink("60683")
    @Tag("TK2784-2")
    @Tag(MIX1)
    public void editScheduleDateFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkOrWeekend(false);
        goToOrgStructureAsUserWithoutWait(Role.FIRST, unit);
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        DayOfWeek dayOfWeek = DayOfWeek.values()[new Random().nextInt(7)];
        Days day = getAnotherDayType(dayOfWeek);
        clickOnDayTypeChangeButton(dayOfWeek.getValue());
        switchDayTypeTo(day);
        clickOnEditionScheduleChangeButton();
        changingDayTypeCheck(dayOfWeek, day);
    }

    @Test(groups = {"TK2784-2.3", "TK2784-2", G1, MIX1},
            description = "Редактирование графика работы. Роли без полномочий",
            dataProvider = "roles 3-4", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"2784_Применение роли в системе блок \"Оргструктура\" Расширение", url = "https://wiki.goodt.me/x/uQEUD")
    @TmsLink("60683")
    @Tag("TK2784-2")
    @Tag(MIX1)
    public void editScheduleDateNoPermissions(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkOrWeekend(true);
        goToOrgStructureAsUserWithoutWait(role, unit);
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        DayOfWeek dayOfWeek = DayOfWeek.values()[new Random().nextInt(7)];
        Days day = getAnotherDayType(dayOfWeek);
        clickOnDayTypeChangeButton(dayOfWeek.getValue());
        switchDayTypeTo(day);
        clickOnEditionScheduleChangeButton();
    }

    @Test(groups = {"TK2784-3", G1, MIX1},
            description = "Изменение названия Ом, используя роль",
            dataProvider = "roles 1, 4", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"2784_Применение роли в системе блок \"Оргструктура\" Расширение", url = "https://wiki.goodt.me/x/uQEUD")
    @TmsLink("60683")
    @Tag("TK2784-3")
    @Tag(MIX1)
    public void moduleOrgStructureCardRedMainNameRole(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitByMatchName(CHANGE_OM_NAME);
        goToOrgStructureAsUserWithoutWait(role, unit);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        String randomName = CHANGE_OM_NAME + CustomTools.stringGenerator();
        changeOmName(randomName);
        changeInfoCardButtonClick();
        assertChangingOmName(randomName, unit);
    }

    @Test(groups = {"TK2784-4", "TEST-1106"}, description = "Изменение типа ОМ, используя роль",
            dataProvider = "roles 1, 4", dataProviderClass = DataProviders.class)
    public void moduleMixCardRedMainTypeRole(Role role) {
        int orgUnitType = getLowestOrgTypes(false);
        List<OrgUnit> unitList = OrgUnitRepository.getOrgUnitsByTypeId(orgUnitType);
        OrgUnit unit = getRandomFromList(unitList);
        PresetClass.changeOrgUnitParentToHighLevelOrgUnit(unit);
        String typeBefore = CommonRepository.getAllOrgUnitTypes().get(unit.getOrganizationUnitTypeId());
        goToOrgStructureAsUserWithoutWait(role, unit);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        ImmutablePair<String, Integer> organizationUnitTypeId = CommonRepository.getRandomOrgUnitTypeExceptMostHigherAndAnother(unit);
        changeOmType(organizationUnitTypeId.left, typeBefore);
        changeInfoCardButtonClick();
        assertChangingOmType(organizationUnitTypeId, true, unit);
    }

    @Test(groups = {"TK2784-5", "TEST-1106"},
            description = "Изменение даты открытия ОМ, используя роль",
            dataProvider = "roles 1, 4", dataProviderClass = DataProviders.class)
    public void moduleMixCardRedMainStartDateRole(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        goToOrgStructureAsUserWithoutWait(role, unit);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        LocalDate dateOpen = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM).minusYears(new Random().nextInt(3) + 1);
        changeOmDate(DateTypeField.OPEN_DATE, dateOpen);
        changeInfoCardButtonClick();
        assertChangingOmStartedDate(dateOpen, unit);
    }

    @Test(groups = {"TK2784-6", "TEST-1106"},
            description = "Изменение даты закрытия ОМ, используя роль",
            dataProvider = "roles 1, 4", dataProviderClass = DataProviders.class)
    public void moduleMixCardRedMainEndDateRole(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit();
        goToOrgStructureAsUserWithoutWait(role, unit);
        searchTheModule(unit.getName());
        pressMainDataPencil();
        LocalDate endDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM).plusYears(new Random().nextInt(2) + 1);
        changeOmDate(DateTypeField.CLOSE_DATE, endDate);
        changeInfoCardButtonClick();
        assertChangeCloseDate(endDate);
    }

    @Test(groups = {"ABCHR2659-1", "TEST-1107"}, description = "Назначение заместителем")
    public void makeDeputy() {
        User user = PresetClass.addRoleToEmployee(Role.EIGHTH, false);
        goToOrgStructure();
        clickEmployeeTab();
        Employee first = user.getEmployee();
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false, first);
        int deputiesSize = user.getUserDeputies().size();
        chooseEmployee(first.getFullName());
        clickOnShowButton(EmployeeInfoName.DEPUTY);
        clickOnPencilButton(EmployeeInfoName.DEPUTY);
        selectDeputyFromList(employee.getFullName(), 1);
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100));
        LocalDate end = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        chooseDeputyDate(start, DateTypeField.DEPUTY_START_DATE, 1);
        chooseDeputyDate(end, DateTypeField.DEPUTY_END_DATE, 1);
        clickOnChangeButton(EmployeeInfoName.DEPUTY);
        assertDeputyAdding(user, employee, new DateInterval(start, end), deputiesSize, false);
    }

    @Test(groups = {"ABCHR2659-2", "TEST-1107"}, description = "Назначение нескольких заместителей")
    public void makeTwoDeputies() {
        User user = PresetClass.addRoleToEmployee(Role.TWELFTH, false);
        goToOrgStructure();
        clickEmployeeTab();
        Employee first = user.getEmployee();
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false, first);
        Employee another = EmployeeRepository.getRandomEmployeeWithAccount(false, first, employee);
        chooseEmployee(first.getFullName());
        clickOnShowButton(EmployeeInfoName.DEPUTY);
        clickOnPencilButton(EmployeeInfoName.DEPUTY);
        selectDeputyFromList(employee.getFullName(), 1);
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100));
        LocalDate end = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        LocalDate startSecond = LocalDate.now().minusDays(new Random().nextInt(100));
        LocalDate endSecond = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        chooseDeputyDate(start, DateTypeField.DEPUTY_START_DATE, 1);
        chooseDeputyDate(end, DateTypeField.DEPUTY_END_DATE, 1);
        selectDeputyFromList(another.getFullName(), 2);
        chooseDeputyDate(startSecond, DateTypeField.DEPUTY_START_DATE, 2);
        chooseDeputyDate(endSecond, DateTypeField.DEPUTY_END_DATE, 2);
        clickOnChangeButton(EmployeeInfoName.DEPUTY);
        assertTwoDeputiesAdding(user, employee, another, new DateInterval(start, end), new DateInterval(startSecond, endSecond));
    }

    @Test(groups = {"ABCHR2659-3", "TEST-1107"}, description = "Назначение заместителем без указания периода замещения")
    public void makeDeputyWithoutEnteringDate() {
        User user = PresetClass.addRoleToEmployee(Role.TWELFTH, false);
        goToOrgStructure();
        clickEmployeeTab();
        Employee first = user.getEmployee();
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false, first);
        chooseEmployee(first.getFullName());
        clickOnShowButton(EmployeeInfoName.DEPUTY);
        clickOnPencilButton(EmployeeInfoName.DEPUTY);
        selectDeputyFromList(employee.getFullName(), 1);
        clickOnChangeButtonOutWait(EmployeeInfoName.DEPUTY);
        checkBothFieldsErrorException();
    }

    @Test(groups = {"ABCHR2659-4", "TEST-1107"}, description = "Назначение заместителем с указанием даты окончания замещения раньше даты начала")
    public void makeDeputyWithEnterWrongDateInterval() {
        User user = PresetClass.addRoleToEmployee(Role.TWELFTH, false);
        goToOrgStructure();
        clickEmployeeTab();
        Employee first = user.getEmployee();
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false, first);
        chooseEmployee(first.getFullName());
        clickOnShowButton(EmployeeInfoName.DEPUTY);
        clickOnPencilButton(EmployeeInfoName.DEPUTY);
        selectDeputyFromList(employee.getFullName(), 1);
        LocalDate end = LocalDate.now().minusDays(new Random().nextInt(100));
        LocalDate start = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        chooseDeputyDate(start, DateTypeField.DEPUTY_START_DATE, 1);
        chooseDeputyDate(end, DateTypeField.DEPUTY_END_DATE, 1);
        clickOnChangeButtonOutWait(EmployeeInfoName.DEPUTY);
        checkDateEndErrorException();
    }

    @Test(groups = {"ABCHR2659-5", "TEST-1107"}, description = "Удаление заместителя")
    public void deleteDeputy() {
        User user = PresetClass.addRoleToEmployee(Role.EIGHTH, true);
        goToOrgStructure();
        clickEmployeeTab();
        Employee first = user.getEmployee();
        chooseEmployee(first.getFullName());
        clickOnShowButton(EmployeeInfoName.DEPUTY);
        clickOnPencilButton(EmployeeInfoName.DEPUTY);
        List<UserDeputy> previous = getUserDeputies(user);
        clickOnDeleteDeputyButton();
        clickOnChangeButton(EmployeeInfoName.DEPUTY);
        assertThatDeputyWasRemoved(previous, user);
    }

    @Test(groups = {"ABCHR2659-7", "TEST-1107"}, description = "Отмена удаление заместителя")
    public void undoDeleteDeputy() {
        User user = PresetClass.addRoleToEmployee(Role.TWELFTH, true);
        goToOrgStructure();
        clickEmployeeTab();
        Employee first = user.getEmployee();
        chooseEmployee(first.getFullName());
        clickOnShowButton(EmployeeInfoName.DEPUTY);
        clickOnPencilButton(EmployeeInfoName.DEPUTY);
        List<UserDeputy> previous = getUserDeputies(user);
        clickOnDeleteDeputyButton();
        clickOnCancelButton(EmployeeInfoName.DEPUTY);
        assertThatDeputyWasNotRemoved(previous, user);
    }

    @Test(groups = {"ABCHR2659-6", "TEST-1107"}, description = "Отмена назначения заместителя")
    public void undoAddDeputy() {
        User user = PresetClass.addRoleToEmployee(Role.TWELFTH, false);
        goToOrgStructure();
        clickEmployeeTab();
        Employee first = user.getEmployee();
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false, first);
        chooseEmployee(first.getFullName());
        clickOnShowButton(EmployeeInfoName.DEPUTY);
        clickOnPencilButton(EmployeeInfoName.DEPUTY);
        selectDeputyFromList(employee.getFullName(), 1);
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100));
        LocalDate end = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        chooseDeputyDate(start, DateTypeField.DEPUTY_START_DATE, 1);
        chooseDeputyDate(end, DateTypeField.DEPUTY_END_DATE, 1);
        List<UserDeputy> previous = getUserDeputies(user);
        clickOnCancelButton(EmployeeInfoName.DEPUTY);
        assertThatDeputyWasNotAdded(previous, user);
    }

    @Test(groups = {"ABCHR2659-8", "TEST-1107"}, description = "Изменение заместителя")
    public void changeDeputyPerson() {
        User user = PresetClass.addRoleToEmployee(Role.EIGHTH, true);
        goToOrgStructure();
        clickEmployeeTab();
        List<UserDeputy> userDeputies = user.getUserDeputies();
        int deputiesSize = userDeputies.size();
        Employee first = user.getEmployee();
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false, first,
                                                                            userDeputies.get(0).getEmployee());
        chooseEmployee(first.getFullName());
        clickOnShowButton(EmployeeInfoName.DEPUTY);
        clickOnPencilButton(EmployeeInfoName.DEPUTY);
        selectDeputyFromList(employee.getFullName(), 1);
        clickOnChangeButton(EmployeeInfoName.DEPUTY);
        assertDeputyAdding(user, employee, userDeputies.get(0).getDateInterval(), deputiesSize, true);
    }

    @Test(groups = {"ABCHR2659-9", "TEST-1107"}, description = "Изменение временного интервала замещения")
    public void changeDeputyDateInterval() {
        User user = PresetClass.addRoleToEmployee(Role.TWELFTH, true);
        goToOrgStructure();
        clickEmployeeTab();
        int deputiesSize = user.getUserDeputies().size();
        Employee first = user.getEmployee();
        chooseEmployee(first.getFullName());
        clickOnShowButton(EmployeeInfoName.DEPUTY);
        clickOnPencilButton(EmployeeInfoName.DEPUTY);
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100));
        LocalDate end = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        chooseDeputyDate(start, DateTypeField.DEPUTY_START_DATE, 1);
        chooseDeputyDate(end, DateTypeField.DEPUTY_END_DATE, 1);
        clickOnChangeButton(EmployeeInfoName.DEPUTY);
        assertDeputyAdding(user, new Employee(getJsonFromUri(Projects.WFM,
                                                             user.getUserDeputies().get(0).getEmployee().getLink(SELF))), new DateInterval(start, end), deputiesSize, true);
    }

    @Test(groups = {"ABCHR2847-1", "TEST-1080"}, description = "Изменение типа формирования табеля для сотрудника")
    @Severity(value = SeverityLevel.CRITICAL)
    public void changeTypeTimeSheetForEmployee() {
        goToOrgStructure();
        clickEmployeeTab();
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        Employee employee = pair.right.getEmployeePosition().getEmployee();
        MathParameter parameter = MathParameterRepository.getMathParameter(EmployeeParams.TABLE_MODE_CREATE.getId());
        PresetClass.checkEmployeeParams(employee.getId(), parameter.getMathParameterId(), parameter.getName());
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.OPTIONS);
        clickOnPencilButton(EmployeeInfoName.OPTIONS);
        MathValue value = MathParameterRepository.getAnotherMathValue(employee, parameter);
        selectParamValue(parameter.getName(), value.getName());
        clickOnChangeButton(EmployeeInfoName.OPTIONS);
        assertParamAdding(value.getValue(), value.getName(), parameter, employee);
    }

    @Test(groups = {"TK2644-1", G1, MIX2},
            description = "Добавление роли с указанием даты начала и даты окончания срока действия")
    @Link(name = "Статья: \"2644_Пользователь может указать для роли срок действия назначения\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460418")
    @Tag("TK2644-1")
    @Tag(MIX2)
    public void addRoleWithStartAndEndDate() {
        Employee employee = EmployeeRepository.getRandomEmployee();
        PresetClass.addUser(employee);
        employee = employee.refreshEmployee();
        User user = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        pickRole(randomRole, order);
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100) + 1);
        LocalDate end = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        setRoleDate(start, DateTypeField.START_DATE, order);
        setRoleDate(end, DateTypeField.END_DATE, order);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit();
        pickOrgUnitString(order, orgUnit);
        filterClickOk();
        pressSaveButton();
        assertAddRole(employee, user, randomRole, Collections.singletonList(orgUnit), start, end);
    }

    @Test(groups = {"TK2644-1.1", "TEST-951"},
            description = "Отмена добавления роли с указанием даты начала и даты окончания срока действия")
    public void cancelAddRoleWithStartAndEndDate() {
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        pickRole(randomRole, order);
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100) + 1);
        LocalDate end = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        setRoleDate(start, DateTypeField.START_DATE, order);
        setRoleDate(end, DateTypeField.END_DATE, order);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit();
        pickOrgUnitString(order, orgUnit);
        filterClickOk();
        pressCancelButton();
        assertCancelAddRole(employee, user);
    }

    @Test(groups = {"TK2644-1.2", G1, MIX2},
            description = "Добавление роли для нескольких подразделений")
    @Link(name = "Статья: \"2644_Пользователь может указать для роли срок действия назначения\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460418")
    @Tag("TK2644-1.2")
    @Tag(MIX2)
    public void addRoleForSeveralOrgUnits() {
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        pickRole(randomRole, order);
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100) + 1);
        LocalDate end = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        setRoleDate(start, DateTypeField.START_DATE, order);
        setRoleDate(end, DateTypeField.END_DATE, order);
        List<OrgUnit> orgUnits = OrgUnitRepository.getTwoRandomOrgUnit();
        pickOrgUnit(order, randomRole.getName(), orgUnits.get(0), orgUnits.get(1));
        filterClickOk();
        pressSaveButton();
        pressMoreButton(order);
        assertAddRole(employee, user, randomRole, orgUnits, start, end);
    }

    @Test(groups = {"TK2644-2", G1, MIX2},
            description = "Удаление существующей роли")
    @Link(name = "Статья: \"2644_Пользователь может указать для роли срок действия назначения\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460418")
    @Tag("TK2644-2")
    @Tag(MIX2)
    public void deleteRoleInUser() {
        Role role = PresetClass.createCustomPermissionRole(Collections.emptyList());
        User user = PresetClass.addRoleToRandomUser(role, LocalDate.now().plusMonths(1));
        Employee employee = user.getEmployee();
        UserRole userRole = UserRoleRepository.getUserRoles()
                .stream()
                .filter(ur -> ur.getName().equals(role.getName()))
                .findFirst()
                .orElse(null);
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        int order = findRoleOrderByName(role.getName());
        deleteAllOrgUnitsInRole(order, role.getName());
        pressSaveButton();
        pressRoleDataPencil();
        pressDeleteRoleButton(order, role.getName());
        pressSaveButton();
        assertDeleteRole(employee, user, userRole);
    }

    @Ignore("Заменен на api-тест")
    @Test(groups = {"ABCHR4965-2", G2, MIX2},
            description = "Добавление роли сотруднику при наличии разрешения (пользователь)")
    @Link(name = "Статья: \"4965_Добавить права на блок \"Раздать права\"", url = "https://wiki.goodt.me/x/CwCqDQ")
    @TmsLink("60240")
    @Tag("ABCHR4965-2")
    @Tag(MIX2)
    public void addRoleInUserWithPermission() {
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(PermissionType.ORGANIZATION_UNIT_VIEW,
                                                                             PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                             PermissionType.ROLES_DISTRIBUTION_VIEW,
                                                                             PermissionType.ROLES_DISTRIBUTION_EDIT));
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWorkingWithUser();
        List<EmployeePosition> eps = EmployeePositionRepository.getActualEmployeePositionsWithChief(ep.getOrgUnit().getId());
        EmployeePosition anotherEp = eps.stream().filter(e -> !e.equals(ep)).collect(randomItem());
        Employee employee = anotherEp.getEmployee();
        User user = employee.getUser();
        goToOrgStructureAsUser(role, ep.getOrgUnit(), ep.getEmployee().getUser());
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        pickRole(randomRole, order);
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100) + 1);
        setRoleDate(start, DateTypeField.START_DATE, order);
        pickOrgUnitString(order, anotherEp.getOrgUnit());
        filterClickOk();
        pressSaveButton();
        assertAddRole(employee, user, randomRole, Collections.singletonList(anotherEp.getOrgUnit()), start, null);
    }

    @Test(groups = {"TK2644-3", G3, MIX2},
            description = "Добавление роли без указания даты начала и даты окончания срока действия")
    @Link(name = "Статья: \"2644_Пользователь может указать для роли срок действия назначения\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460418")
    @Tag("TK2644-3")
    @Tag(MIX2)
    public void addRoleWithOutStartAndEndDate() {
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        pickRole(randomRole, order);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit();
        pickOrgUnitString(order, orgUnit);
        filterClickOk();
        pressSaveButton();
        assertCantSaveWithoutDate(order);
    }

    @Test(groups = {"TK2644-3.1", "TEST-951"},
            description = "Отмена добавления роли без указания даты начала и даты окончания срока действия")
    public void cancelAddRoleWithOutStartAndEndDate() {
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        pickRole(randomRole, order);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit();
        pickOrgUnitString(order, orgUnit);
        filterClickOk();
        pressCancelButton();
        assertCancelAddRole(employee, user);
    }

    @Test(groups = {"TK2644-4", "TEST-951"},
            description = "Добавление роли без указания подразделения")
    public void addRoleWithOutOrgUnit() {
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        pickRole(randomRole, order);
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100) + 1);
        LocalDate end = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        setRoleDate(start, DateTypeField.START_DATE, order);
        setRoleDate(end, DateTypeField.END_DATE, order);
        pressSaveButton();
        assertAddRole(employee, user, randomRole, Collections.emptyList(), start, end);
    }

    @Test(groups = {"TK2644-5", "TEST-951"},
            description = "Добавление роли без указания роли")
    public void addRoleWithOutRole() {
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = employee.getUser();
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100) + 1);
        LocalDate end = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        setRoleDate(start, DateTypeField.START_DATE, order);
        setRoleDate(end, DateTypeField.END_DATE, order);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit();
        pickOrgUnitString(order, orgUnit);
        filterClickOk();
        pressSaveButton();
        assertCantSaveWithOutRole(order);
    }

    @Test(groups = {"TK2644-6", "TEST-951"},
            description = "Добавление роли с указанием даты начала срока действия")
    public void addRoleWithStartDate() {
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        pickRole(randomRole, order);
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100) + 1);
        setRoleDate(start, DateTypeField.START_DATE, order);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit();
        pickOrgUnitString(order, orgUnit);
        filterClickOk();
        pressSaveButton();
        assertAddRole(employee, user, randomRole, Collections.singletonList(orgUnit), start, null);
    }

    @Test(groups = {"TK2644-7", "TEST-951"},
            description = "Добавление роли с указанием даты окончания срока действия")
    public void addRoleWithEndDate() {
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        pickRole(randomRole, order);
        LocalDate end = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        setRoleDate(end, DateTypeField.END_DATE, order);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit();
        pickOrgUnitString(order, orgUnit);
        filterClickOk();
        pressSaveButton();
        assertCantSaveWithoutDate(order);
    }

    @Test(groups = {"TK2644-8", "TEST-951"}, description = "Изменение роли без внесения данных")
    public void changeRoleWithOutDataEntry() {
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(EmployeeRepository.getRandomEmployee(false).getFullName());
        pressRoleDataPencil();
        pressSaveButton();
        assertNothingHappened();
    }

    @Test(groups = {"TK2644-9", "TEST-951"}, description = "Отмена изменения роли без внесения данных")
    public void cancelChangeRoleWithOutDataEntry() {
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(EmployeeRepository.getRandomEmployee(false).getFullName());
        pressRoleDataPencil();
        pressCancelButton();
        assertNothingHappened();
    }

    @Test(groups = {"TK2644-12", G3, MIX2},
            description = "Добавление роли с указанием даты окончания срока действия роли раньше даты начала")
    @Link(name = "Статья: \"2644_Пользователь может указать для роли срок действия назначения\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460418")
    @TmsLink("60836")
    @Tag(MIX2)
    @Tag("TK2644-12")
    public void addRoleWithStartAfterEndDate() {
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        pickRole(randomRole, order);
        LocalDate end = LocalDate.now().minusDays(new Random().nextInt(100) + 1);
        LocalDate start = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        setRoleDate(start, DateTypeField.START_DATE, order);
        setRoleDate(end, DateTypeField.END_DATE, order);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit();
        pickOrgUnitString(order, orgUnit);
        filterClickOk();
        pressSaveButton();
        assertCantSaveWithWrongDate(start, order);
    }

    @Test(groups = {"TK2644-13", "TEST-951"},
            description = "Ввод некорректных значений в поля \"Дата начала\" и \"Дата окончания\"")
    public void sendIncorrectDate() {
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        pickRole(randomRole, order);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit();
        pickOrgUnitString(order, orgUnit);
        filterClickOk();
        String rndNumbers = 9 + RandomStringUtils.randomNumeric(10);
        sendRoleDate(rndNumbers, DateTypeField.START_DATE, order);
        assertWrongDate(order);
    }

    @Test(groups = {"TK2644-15", "TEST-1293"}, description = "Сброс выбранных подразделений при назначении роли сотруднику")
    public void resetSelectedUnitsAssigningRoleToEmployee() {
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(user);
        int order = user != null ? user.getRoles().size() + 1 : 1;
        goToOrgStructure();
        clickEmployeeTab();
        pressGiveRolesButton();
        chooseEmployee(employee.getFullName());
        pressRoleDataPencil();
        pickRole(randomRole, order);
        LocalDate start = LocalDate.now().minusDays(new Random().nextInt(100) + 1);
        LocalDate end = LocalDate.now().plusDays(new Random().nextInt(100) + 1);
        setRoleDate(start, DateTypeField.START_DATE, order);
        setRoleDate(end, DateTypeField.END_DATE, order);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit();
        pickOrgUnitString(order, orgUnit);
        filterClickReset();
        assertResetSelection(employee, user, order);
    }

    @Test(groups = {"Rmix-21", G1, MIX3}, description = "Создание роли")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%A3%D0%B4%D0%B0%D0%BB%D0%B5%D0%BD%D0%B8%D0%B5%D1%80%D0%BE%D0%BB%D0%B8")
    @Tag("Rmix-21")
    @Tag(MIX3)
    public void createRole() {
        goToOrgStructure();
        List<UserRole> rolesBefore = UserRoleRepository.getUserRoles();
        clickRoleTab();
        pressAddRoleButton();
        String roleName = "test_role_" + RandomStringUtils.randomAlphabetic(5);
        sendRoleName(roleName);
        String roleDescription = RandomStringUtils.randomAlphabetic(50);
        sendRoleDescription(roleDescription);
        String roleTypeName = selectRandomRoleType();
        pressCreateRoleButton();
        pressOnRoleName(roleName);
        assertAddRole(rolesBefore, roleName, roleDescription, roleTypeName);
    }

    @Test(groups = {"Rmix-22"}, description = "Удаление роли")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%A3%D0%B4%D0%B0%D0%BB%D0%B5%D0%BD%D0%B8%D0%B5%D1%80%D0%BE%D0%BB%D0%B8")
    @TmsLink("60233")
    @Tag("Rmix-22")
    public void deleteRole() {
        String roleName = "test_role_" + RandomStringUtils.randomAlphabetic(5);
        PresetClass.addUserRole(roleName);
        goToOrgStructure();
        List<UserRole> rolesBefore = UserRoleRepository.getUserRoles();
        clickRoleTab();
        pressOnRoleName(roleName);
        pressDeleteRoleButton();
        assertDeleteRole(rolesBefore, roleName);
    }

    @Test(groups = {"Rmix-23", G1, MIX3}, description = "Назначение полномочий роли")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%A3%D0%B4%D0%B0%D0%BB%D0%B5%D0%BD%D0%B8%D0%B5%D1%80%D0%BE%D0%BB%D0%B8")
    @Tag("Rmix-23")
    @Tag(MIX3)
    public void addPermissionRole() {
        String roleName = "test_role_" + RandomStringUtils.randomAlphabetic(5);
        UserRole roleBefore = PresetClass.addUserRole(roleName);
        Permission permission = getRandomFromList(PermissionRepository.getPermissions());
        goToOrgStructure();
        clickRoleTab();
        pressOnRoleName(roleName);
        ImmutablePair<String, String> groupAndName = permission.getPermissionGroupAndName();
        clickOnPermissionGroupChevron(groupAndName.getLeft());
        clickOnPermissionGroupPencilButton(groupAndName.getLeft());
        clickOnPermissionCheckBox(groupAndName.getRight());
        pressChangePermissionButton();
        assertAddPermission(roleBefore, permission);
    }

    @Test(groups = {"Rmix-24", G1, MIX3}, description = "Редактирование роли")
    @Link(name = "Тесты для списка модуля \"Оргструктура\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=189006525#id-%D0%A2%D0%B5%D1%81%D1%82%D1%8B%D0%B4%D0%BB%D1%8F%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F%22%D0%9E%D1%80%D0%B3%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%82%D1%83%D1%80%D0%B0%22-%D0%A3%D0%B4%D0%B0%D0%BB%D0%B5%D0%BD%D0%B8%D0%B5%D1%80%D0%BE%D0%BB%D0%B8")
    @Tag("Rmix-24")
    @Tag(MIX3)
    public void editRole() {
        String roleName = "test_role_" + RandomStringUtils.randomAlphabetic(5);
        PresetClass.addUserRole(roleName);
        goToOrgStructure();
        List<UserRole> rolesBefore = UserRoleRepository.getUserRoles();
        clickRoleTab();
        pressOnRoleName(roleName);
        pressEditRoleButton();
        String roleName1 = "test_role_" + RandomStringUtils.randomAlphabetic(5);
        sendRoleName(roleName1);
        String roleDescription = RandomStringUtils.randomAlphabetic(50);
        sendRoleDescription(roleDescription);
        pressChangeRoleButton();
        assertChangeRole(rolesBefore, roleName1, roleDescription);
    }

    @Test(groups = {"ABCHR3185", G1, MIX2},
            description = "Изменение ФИО сотрудника ")
    @Link(name = "Статья: \"3185_Добавить права на редактирование ФИО сотруднику\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204281317")
    @TmsLink("60322")
    @Tag("ABCHR3185-3")
    @Tag(MIX2)
    public void changeEmployeeName() {
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(PermissionType.ORGANIZATION_UNIT_VIEW,
                                                                             PermissionType.ORGANIZATION_UNIT_NAME_EDIT,
                                                                             PermissionType.ORGANIZATION_UNIT_EDIT,
                                                                             PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                             PermissionType.ORG_EMPLOYEE_EDIT));
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWorkingWithUser();

        goToOrgStructureAsUser(role, ep.getOrgUnit(), ep.getEmployee().getUser());
        clickEmployeeTab();
        Employee randomEmployee = getRandomFromList(EmployeeRepository.getEmployeesFromOM(ep.getOrgUnit().getId()));
        chooseEmployee(randomEmployee.getFullName());
        changeEmp();
        String newLastName = RandomStringUtils.randomAlphabetic(10);
        sendValueInInput(EmpFields.LAST_NAME, newLastName);
        String newFirstName = RandomStringUtils.randomAlphabetic(10);
        sendValueInInput(EmpFields.FIRST_NAME, newFirstName);
        String newPatronymicName = RandomStringUtils.randomAlphabetic(10);
        sendValueInInput(EmpFields.PATRONYMIC_NAME, newPatronymicName);
        changeInfoCardButtonClick();
        assertEmployeeNameChanged(randomEmployee, String.join(" ", newLastName, newFirstName, newPatronymicName).trim());
    }

    @Test(groups = {"ABCHR4600-1", G1, MIX1},
            description = "Создание нового тега подразделению (пользователь)")
    @Link(name = "Статья: \"4600_Добавить права на управление тегами\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=223511977")
    @TmsLink("60321")
    @Tag("ABCHR4600-1")
    @Tag(MIX1)
    public void addTagToOrgUnitAsUser() {
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_VIEW,
                PermissionType.ORGANIZATION_UNIT_EDIT,
                PermissionType.EDIT_ORG_UNIT_TAGS,
                PermissionType.SYS_LIST_ORG_TYPES_READ
        ));
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWorkingWithUser();

        goToOrgStructureAsUser(role, ep.getOrgUnit(), ep.getEmployee().getUser());
        OrgUnit unit = ep.getOrgUnit();
        searchTheModule(unit.getName());
        List<String> tagsBefore = unit.getTags();
        pressMainDataPencil();
        String tag = RandomStringUtils.randomAlphabetic(10);
        enterTags(tag);
        changeInfoCardButtonClick();
        assertChangingOmTags(tag, unit, tagsBefore);
    }

    @Test(groups = {"ABCHR4600-2", G1, MIX1},
            description = "Добавление существующего тега подразделению (пользователь)")
    @Link(name = "Статья: \"4600_Добавить права на управление тегами\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=223511977")
    @TmsLink("118707")
    @Tag("ABCHR4600-2")
    @Tag(MIX1)
    public void addExistingTagToOrgUnitAsUser() {
        ImmutablePair<String, String> tagPair = CommonRepository.getRandomTagFromApi();
        String tag = tagPair.left;
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_VIEW,
                PermissionType.ORGANIZATION_UNIT_EDIT,
                PermissionType.EDIT_ORG_UNIT_TAGS,
                PermissionType.SYS_LIST_ORG_TYPES_READ
        ));
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWorkingWithUser();
        OrgUnit unit = ep.getOrgUnit();
        List<String> tagsBefore = unit.getTags();

        goToOrgStructureAsUser(role, ep.getOrgUnit(), ep.getEmployee().getUser());
        searchTheModule(unit.getName());
        pressMainDataPencil();
        enterTags(tag);
        clickEnteredTag(tag);
        changeInfoCardButtonClick();
        assertChangingOmTags(tag, unit, tagsBefore);
    }

    @Test(groups = {"ABCHR4600-4", G2, MIX2},
            description = "Добавление тега в свою карточку сотрудника")
    @Link(name = "Статья: \"4600_Добавить права на управление тегами\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=223511977")
    @Tag("ABCHR4600-4")
    @Tag(MIX2)
    @Owner(SCHASTLIVAYA)
    public void addTagToOwnCard() {
        List<PermissionType> permissionTypes = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_VIEW,
                PermissionType.ORGANIZATION_UNIT_EDIT,
                PermissionType.ORG_EMPLOYEE_EDIT,
                PermissionType.EDIT_SELF_TAGS,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissionTypes);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWorkingWithUser();
        Employee employee = ep.getEmployee();

        goToOrgStructureAsUser(role, ep.getOrgUnit(), ep.getEmployee().getUser());
        clickEmployeeTab();
        chooseEmployee(employee.getFullName());
        changeEmp();
        List<String> allTags = new ArrayList<>(CommonRepository.getTags().keySet());
        String randomTag = getRandomFromList(allTags);
        sendTagForEmployee(randomTag);
        List<String> tagsBefore = employee.getActualTags();
        changeInfoCardButtonClick();
        assertAddTag(employee, tagsBefore, randomTag);
    }

    @Test(groups = {"ABCHR2777-1", G1, MIX2, POCHTA,
            "@Before show all employee groups"},
            description = "Указание статуса сотрудника \"декрет\"")
    @Link(name = "Статья: \"2777_Ведение сотрудника в Декрете в WFM\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204276828")
    @TmsLink("60340")
    @Tag(MIX2)
    @Tag("ABCHR2777-1")
    public void setMaternityLeaveStatus() {
        Employee employee = EmployeePositionRepository.getRandomEmployeeWorkingWithUser().getEmployee();
        goToOrgStructure();
        clickEmployeeTab();
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.STATUS);
        EmployeeStatusType statusType = CommonRepository.getEmployeeStatusTypeByName("Декрет");
        List<String> statusesBeforeUi = getCurrentUiInfoBlocks(EmployeeInfoName.STATUS)
                .stream().map(AtlasWebElement::getText).collect(Collectors.toList());

        int orderNumber = statusesBeforeUi.size();
        clickOnPencilButton(EmployeeInfoName.STATUS);
        selectStatus(statusType.getTitle(), orderNumber);
        LocalDate startDate = getRandomFromList(new DateInterval().getBetweenDatesList());
        inputStatusDate(DateTypeField.START_DATE, startDate, orderNumber);
        LocalDate endDate = startDate.plusMonths(new Random().nextInt(12) + 1);
        inputStatusDate(DateTypeField.END_DATE, endDate, orderNumber);

        List<EmployeeStatus> statusesBefore = employee.getStatuses();
        clickOnChangeButton(EmployeeInfoName.STATUS);
        assertAddedStatus(employee, statusesBefore, startDate, endDate, statusesBeforeUi, statusType);
    }

    @Test(groups = {"ABCHR3224-1", G1, MIX2},
            description = "Доступ к параметрам должности в карточке сотрудника")
    @Link(name = "3224_Добавить права на параметры позиции сотрудника", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204279000")
    @TmsLink("60332")
    @Owner(BUTINSKAYA)
    @Tag("ABCHR3224-1")
    @Tag(MIX2)
    public void addPermissionsToEmployeeParameters() {
        List<PermissionType> permissionCustomGeneratedTypes = new ArrayList<>(Arrays.asList(
                PermissionType.MATH_PARAMETERS_VIEW,
                PermissionType.MATH_PARAMETERS_EDIT,
                PermissionType.ORGANIZATION_UNIT_VIEW
        ));
        List<MathParameter> mathParameters = getRandomListFromMathParameters(MathParameterEntities.EMPLOYEE_POSITION);
        Role role = PresetClass.addMathParamsPermissionsToRole(
                PresetClass.createCustomPermissionRole(permissionCustomGeneratedTypes), mathParameters);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToOrgStructureAsUserWithoutWait(role, unit);
        clickEmployeeTab();
        EmployeePosition position = EmployeePositionRepository.getRandomEmployeePosition(unit);
        chooseEmployee(position.getEmployee().getFullName());
        clickOnParametersPencilButton(EmployeeInfoName.OPTIONS);
        selectEmployeePositionGroup(position.getPosition().getName(), unit.getName());
        checkMathParameters(mathParameters, MathParameterEntities.EMPLOYEE_POSITION);
    }

    @Test(groups = {"ABCHR3224-2", G1, MIX2},
            description = "Изменение параметра должности сотрудника")
    @Link(name = "3224_Добавить права на параметры позиции сотрудника", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204279000")
    @TmsLink("60332")
    @Owner(BUTINSKAYA)
    @Tag("ABCHR3224-2")
    @Tag(MIX2)
    public void changeMathParameterEmployeePosition() {
        List<PermissionType> permissionCustomGeneratedTypes = new ArrayList<>(Arrays.asList(
                PermissionType.MATH_PARAMETERS_VIEW,
                PermissionType.MATH_PARAMETERS_EDIT,
                PermissionType.ORGANIZATION_UNIT_VIEW,
                PermissionType.ORGANIZATION_UNIT_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        List<MathParameter> mathParameters = MathParameterRepository.getMathParametersWithEntity(MathParameterEntities.EMPLOYEE_POSITION);
        Role role = PresetClass.addMathParamsPermissionsToRole(
                PresetClass.createCustomPermissionRole(permissionCustomGeneratedTypes), mathParameters);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToOrgStructureAsUserWithoutWait(role, unit);
        clickEmployeeTab();
        EmployeePosition position = EmployeePositionRepository.getRandomEmployeePosition(unit);
        chooseEmployee(position.getEmployee().getFullName());
        clickOnParametersPencilButton(EmployeeInfoName.OPTIONS);
        selectEmployeePositionGroup(position.getPosition().getName(), unit.getName());
        int randNumber = chooseRandomNumber(9);
        MathParameter mathParam = chooseMathParam(mathParameters);
        changeMathParameter(mathParam, randNumber);
        saveEmployeeParameters();
        checkMathParameter(position, mathParam, randNumber);
    }

    @Test(groups = {"ABCHR4281-1", G1, MIX1},
            description = "Доступ к созданию подразделения при наличии прав")
    @Link(name = "4281_Добавить права для блоков Расписание, Оргструктура и Разное", url = "https://wiki.goodt.me/x/yAr6D")
    @TmsLink("60331")
    @Tag("ABCHR4281-1")
    @Tag(MIX1)
    @Owner(BUTINSKAYA)
    public void accessToCreateOrgUnit() {
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_VIEW,
                PermissionType.ORGANIZATION_UNIT_EDIT,
                PermissionType.SYS_LIST_ORG_TYPES_READ,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                PermissionType.ORGANIZATION_UNIT_CREATE
        ));
        List<OrganizationUnitTypeId> orgUnitTypes = OrganizationUnitTypeId.getAllOrgUnitTypes();
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit randomUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(orgUnitTypes.get(orgUnitTypes.size() - 2));
        goToOrgStructureAsUser(role, randomUnit);
        pressAddUnitButton();
        String randomName = RandomStringUtils.randomAlphanumeric(10);
        String randomOuterId = RandomStringUtils.randomAlphanumeric(10);
        enterOMName(randomName);
        enterOuterId(randomOuterId);
        chooseUnitType(orgUnitTypes.get(orgUnitTypes.size() - 1).getName());
        enterUnitDateOpenOrClose(LocalDate.now(), DateTypeField.OPEN_DATE);
        chooseParentUnit(randomUnit.getName());
        pressCreateOMButton();
        assertUnitAdded(randomName, randomUnit);
    }

    @Test(groups = {"ABCHR4281", G1, MIX1},
            description = "Доступ к созданию подразделения при отсутствии прав",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + OM_CREATION_BUTTON_NOT_DISPLAYED + ANY)
    @Link(name = "4281_Добавить права для блоков Расписание, Оргструктура и Разное", url = "https://wiki.goodt.me/x/yAr6D")
    @TmsLink("60331")
    @Tag("ABCHR4281-1-1")
    @Tag(MIX1)
    @Owner(BUTINSKAYA)
    public void accessToCreateOrgUnitWithoutPermission() {
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_VIEW,
                PermissionType.SYS_LIST_ORG_TYPES_READ
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit randomUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getRandomType());
        goToOrgStructureAsUserWithoutWait(role, randomUnit);
        pressAddUnitButton();
    }

    @Test(groups = {"ABCHR4281-2", G1, MIX2},
            description = "Доступ к созданию сотрудника при наличии прав")
    @Link(name = "4281_Добавить права для блоков Расписание, Оргструктура и Разное", url = "https://wiki.goodt.me/x/yAr6D")
    @TmsLink("60331")
    @Tag("ABCHR4281-2")
    @Tag(MIX2)
    @Owner(BUTINSKAYA)
    public void accessToCreateEmployee() {
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_VIEW,
                PermissionType.ORGANIZATION_UNIT_EDIT,
                PermissionType.ORG_EMPLOYEE_CREATE,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                PermissionType.ORGANIZATION_UNIT_POSITION_APPOINTMENT_IN_PRESENT_AND_FUTURE,
                PermissionType.ORGANIZATION_UNIT_CREATE
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit unit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        goToOrgStructureAsUserWithoutWait(role, unit);
        clickEmployeeTabWithRole();
        pressAddEmployeeButton();
        String firstName = RandomStringUtils.randomAlphabetic(10);
        enterFirstName(firstName);
        String lastName = RandomStringUtils.randomAlphabetic(10);
        enterLastName(lastName);
        enterUnit(unit.getName());
        enterWorkDateBeginOrEnd(LocalDate.now(), DateTypeField.START_JOB);
        pressCreateEmployeeButton();
        refreshPageAndAcceptAlertWindow();
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        assertEmployeeAdded(firstName, lastName, unit);
    }

    @Test(groups = {"ABCHR4281-2", G1, MIX2},
            description = "Доступ к созданию сотрудника при отсутствии прав",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = ANY + EMPLOYEE_CREATION_BUTTON_NOT_DISPLAYED + ANY)
    @Link(name = "4281_Добавить права для блоков Расписание, Оргструктура и Разное", url = "https://wiki.goodt.me/x/yAr6D")
    @TmsLink("60331")
    @Tag("ABCHR4281-2-1")
    @Tag(MIX2)
    @Owner(BUTINSKAYA)
    public void accessToCreateEmployeeWithoutPermission() {
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_VIEW,
                PermissionType.ORGANIZATION_UNIT_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                PermissionType.ORGANIZATION_UNIT_POSITION_APPOINTMENT_IN_PRESENT_AND_FUTURE
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit unit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        goToOrgStructureAsUserWithoutWait(role, unit);
        clickEmployeeTabWithRole();
        pressAddEmployeeButton();
    }

    @Test(groups = {"ABCHR4067", G2, MIX2,
            "@Before manipulation with contacts in employee card"},
            description = "Отображение персональных данных пользователя в модуле \"Оргструктура\" при наличии прав",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"4067_Обезличивание персональных данных\"", url = "https://wiki.goodt.me/x/NgH6D")
    @TmsLink("60242")
    @Owner(SCHASTLIVAYA)
    @Severity(SeverityLevel.MINOR)
    @Tag(MIX2)
    public void showPersonalDataDependingOnPermission(boolean hasPermissions) {
        changeTestIDDependingOnParameter(hasPermissions, "ABCHR4067-2", "ABCHR4067-4",
                                         "Отображение персональных данных пользователя в модуле \"Оргструктура\" при отсутствии прав");
        List<PermissionType> permissions = new ArrayList<>(Collections.singletonList(PermissionType.ORGANIZATION_UNIT_VIEW));
        if (hasPermissions) {
            permissions.add(PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        List<EmployeePosition> allEmployees = EmployeePositionRepository.getActualEmployeePositionsWithChief(unit.getId());
        String name = allEmployees.stream()
                .map(e -> e.getEmployee().getFullName())
                .sorted()
                .findFirst()
                .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "В подразделении нет сотрудников"));
        EmployeePosition ep = EmployeePositionRepository.getEmployeePosition(name, unit.getId());
        EmployeePosition loginEp = allEmployees.stream()
                .filter(e -> !e.equals(ep))
                .findAny()
                .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "Нужно хотя бы два сотрудника в подразделении"));
        PresetClass.makeEmailInApi(ep.getEmployee(), CustomTools.generateRandomEmail());
        PresetClass.addPhone(ep.getEmployee(), PhoneTypes.MOBILE);
        goToOrgStructureAsUserWithoutWait(role, unit, loginEp.getEmployee().getUser());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        checkVisibilityOfEmployeeNames(hasPermissions);
        clickEmployeeTab();
        chooseEmployee(ep.getEmployee().getFullName());
        clickOnShowButton(EmployeeInfoName.CONTACTS);
        checkVisibilityOfContacts(hasPermissions, PhoneTypes.MOBILE);
    }

    @Test(groups = {"ABCHR4337-1", G0, MIX2},
            description = "Доступ к параметрам сотрудника в карточке сотрудника с разрешением")
    @Link(name = "Статья: \"4337_Доработка карточки матпараметров сотрудник\"", url = "https://wiki.goodt.me/x/aQ76D")
    @TmsLink("60207")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR4337-1")
    @Severity(SeverityLevel.CRITICAL)
    @Tag(MIX2)
    public void accessEmployeeMathParametersWithPermissions() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        Employee employee = EmployeePositionRepository.getActualEmployeePositionsWithChief(unit.getId())
                .stream()
                .findAny()
                .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "В подразделении нет сотрудников"))
                .getEmployee();

        List<PermissionType> permissions = Arrays.asList(PermissionType.ORGANIZATION_UNIT_VIEW,
                                                         PermissionType.ORGANIZATION_UNIT_EDIT,
                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                         PermissionType.MATH_PARAMETERS_VIEW,
                                                         PermissionType.MATH_PARAMETERS_EDIT);
        Role role = PresetClass.createCustomPermissionRole(permissions);
        List<MathParameter> params = PresetClass.addSeveralMathParametersToRole(role, MathParameterEntities.EMPLOYEE, "LIST", new Random().nextInt(5) + 1);
        goToOrgStructureAsUserWithoutWait(role, unit);
        clickEmployeeTab();
        chooseEmployee(employee.getFullName());
        clickOnPencilButton(EmployeeInfoName.OPTIONS);
        MathParameter param = getRandomFromList(params);
        MathValue value = MathParameterRepository.getAnotherMathValue(employee, param);
        checkParamsVisibility(params);
        selectParamValue(param.getShortName(), value.getName());
        saveParameterChanges();
        assertParameterChange(param, MathParameterEntities.EMPLOYEE, employee.getId(), value.getValue(), null);
    }

    @Test(groups = {"ABCHR5330", MIX2, G2, "@Before show all employee groups"}, dataProvider = "snilsData")
    @Link(name = "Статья: \"5330_Снять проверку с поля СНИЛС в карточке сотрудника на кол-во цифр\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=254640419")
    @Owner(KHOROSHKOV)
    @TmsLink("60018")
    @Tag(MIX2)
    public void validateSnils(String testName, String tag, boolean digitRemoved, String snilsValue) {
        excludeParametersAndRecalculateHistoryId(Collections.singletonList("arg3"));
        Employee employee = EmployeeRepository.getRandomEmployee();
        changeTestName(testName);
        addTag(tag);
        goToOrgStructure();
        clickEmployeeTab();
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.ACCOUNTING);
        clickOnPencilButton(EmployeeInfoName.ACCOUNTING);
        if (digitRemoved) {
            snilsInput(snilsValue.substring(0, snilsValue.length() - 1));
            clickOnChangeButton(EmployeeInfoName.ACCOUNTING);
            clickOnPencilButton(EmployeeInfoName.ACCOUNTING);
        }
        if (snilsValue.isEmpty()) {
            clearFieldSnils();
        } else {
            snilsInput(snilsValue);
        }
        clickOnChangeButton(EmployeeInfoName.ACCOUNTING);
        assertSnils(snilsValue, employee.refreshEmployee());
    }

    @Test(groups = {"ABCHR-3104-1", LIST6, G2, "@After revert position attribute value"}, description = "Добавление атрибутов позиции")
    @Link(name = "Статья: \"3104_Возможность управления дополнительными атрибутами Позиции\"",
            url = "https://wiki.goodt.me/x/GQwtD")
    @TmsLink("95360")
    @Tag("ABCHR-3104-1")
    @Tag(LIST6)
    public void addAttributeToPosition() {
        EntityPropertiesKey key = PresetClass.addAttributeToSystemLists(MathParameterEntities.POSITION);
        String keyId = key.getKey();
        ImmutablePair<OrgUnit, EmployeeEssence> pair =
                OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        OrgUnit unit = pair.left;
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApiNotFiredOnDate(pair.left.getId(), true, LocalDate.now());
        int positionId = ep.getPosition().getId();
        PresetClass.checkIfPositionHasAttribute(positionId, keyId);
        String employeeName = ep.getEmployee().getFullName();
        String randomValue = RandomStringUtils.randomAlphabetic(5);
        goToOrgStructure();
        searchTheModule(unit.getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnEmployeeThreeDots(employeeName);
        chooseEmployeeFunction(employeeName, EmployeeVariants.POSITION_ATTRIBUTES);
        enterAttributeValue(key.getTitle(), randomValue);
        saveAttributeValueButtonClick();
        refreshPageAndAcceptAlertWindow();
        assertPositionAttributeAdded(positionId, keyId, randomValue);
    }

    @Test(groups = {"ABCHR87511", "@After reset Hidden Fields In Card UI"}, dataProvider = "hideDataFirst",
            description = "Отображение полей в карточке назначения с помощью системной настройки")
    @Link(name = "87511_ [Карточка назначения] Убрать отображение полей в карточке назначения",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=281445803")
    @Owner(KHOROSHKOV)
    @TmsLink("115541")
    @Tag("ABCHR87511-1")
    public void displayFieldsWithSystemSetting(String hideValue) {
        changeProperty(SystemProperties.HIDE_UI_FIELDS_IN_DESTINATION_CARDS, hideValue);
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        String empFullName = pair.getRight().getEmployeePosition().getEmployee().getFullName();
        goToOrgStructureAsUserWithoutWait(Role.FIFTEEN, pair.left);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnPlusButtonEmployee();
        cancelButtonClick();
        if (!getCurrentEmployeesNames().contains(empFullName)) {
            throw new AssertionError("Не найден сотрудник:" + empFullName + " в выпадающем списке");
        }
        clickOnEmployeeThreeDots(empFullName);
        editButtonClick();
        assertAllHideValue(hideValue, false);
    }

    @Test(groups = {"ABCHR87511", "@After reset Hidden Fields In Card UI"}, dataProvider = "hideDataSecond",
            description = "Ранее внесённые значения в скрытых полях остаются прежними")
    @Link(name = "87511_ [Карточка назначения] Убрать отображение полей в карточке назначения",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=281445803")
    @Owner(KHOROSHKOV)
    @TmsLink("115541")
    @Tag("ABCHR87511-2")
    public void restoreHiddenFieldsValues(String hideValue) {
        changeProperty(SystemProperties.HIDE_UI_FIELDS_IN_DESTINATION_CARDS, hideValue);
        boolean visible = hideValue.equals("NONE");
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        String empFullName = pair.getRight().getEmployeePosition().getEmployee().getFullName();
        goToOrgStructureAsUserWithoutWait(Role.FIFTEEN, pair.left);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        if (!getCurrentEmployeesNames().contains(empFullName)) {
            throw new AssertionError("Не найден сотрудник:" + empFullName + " в выпадающем списке");
        }
        clickOnEmployeeThreeDots(empFullName);
        editButtonClick();
        assertAllHideValue(hideValue, visible);
    }

    @Test(groups = {"ABCHR87511", "@After reset Hidden Fields In Card UI"},
            description = "При создании нового назначения скрытые поля заполняются автоматически")
    @Link(name = "87511_ [Карточка назначения] Убрать отображение полей в карточке назначения",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=281445803")
    @Owner(KHOROSHKOV)
    @TmsLink("115541")
    @Tag("ABCHR87511-3")
    public void autoFillHiddenFields() {
        changeProperty(SystemProperties.HIDE_UI_FIELDS_IN_DESTINATION_CARDS, "[1,2,3,4,5,6,7]");
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        EmployeePosition emp = pair.getRight().getEmployeePosition();
        String empFullName = emp.getEmployee().getFullName();
        goToOrgStructureAsUserWithoutWait(Role.FIFTEEN, pair.left);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        if (!getCurrentEmployeesNames().contains(empFullName)) {
            throw new AssertionError("Не найден сотрудник:" + empFullName + " в выпадающем списке");
        }
        clickOnPlusButtonEmployee();
        clickOnSelectEmployeeChevron();
        selectAnyEmployee(empFullName);
        JobTitle jobTitle = JobTitleRepository.randomJobTitle();
        chooseJob(jobTitle);
        LocalDate jobStart = LocalDateTools.randomSeedDate(-1, 3, ChronoUnit.MONTHS, TimeType.RANDOM);
        LocalDate dateStartPosition = LocalDateTools.randomSeedDate(-4, 8, ChronoUnit.MONTHS, TimeType.RANDOM);
        chooseDatePositionForm(jobStart, DateTypeField.START_JOB);
        chooseDatePositionForm(dateStartPosition, DateTypeField.POSITION_START_DATE);
        changeProperty(SystemProperties.HIDE_UI_FIELDS_IN_DESTINATION_CARDS, "NONE");
        saveButtonClick(); //не срабатывает, потому что поле категории должности иногда не вставляется
        assertForEmployeePosition(jobTitle, jobStart, dateStartPosition, empFullName, pair.getLeft().getId());
        clickOnEmployeeThreeDots(empFullName);
        editButtonClick();
    }

    @Test(groups = {"ABCHR4840-1", MIX1, G2, IN_PROGRESS}, description = "Проставление даты окончания должности при закрытии назначения",
            dataProvider = "true/false/null", dataProviderClass = DataProviders.class)
    @Link(name = "4840_добавить системную настройку по автоматическому заполнению даты окончания должности",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=223511239")
    @Owner(KHOROSHKOV)
    @TmsLink("118980")
    @Tag("ABCHR4840-1")
    public void positionClosureUpdatesEndDate(Boolean closePosition) {
        boolean autoClosePosition = Objects.isNull(closePosition) || closePosition;
        changeProperty(SystemProperties.EMPLOYEE_POSITION_AUTO_CLOSE_POSITION, autoClosePosition);
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        LocalDate dateNow = LocalDate.now();
        EmployeePosition emp = pair.getRight().getEmployeePosition();
        String empFullName = emp.getEmployee().getFullName();
        Position position = emp.getPosition();
        LocalDate positionStartDate = position.getDateInterval().getStartDate();
        if (positionStartDate.isAfter(dateNow)) {
            PresetClass.updateEmployeeStartDateInSchedule(position, emp.getEmployee(), pair.getLeft(), positionStartDate,
                                                          dateNow.minusYears(2), dateNow.plusYears(1));
        }

        goToOrgStructure();
        searchTheModule(pair.getLeft().getName());
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnThreeDotsButtonByName(empFullName);
        editButtonClick();
        LocalDate positionEndDateBeforeUI = getDateInForm(DateTypeField.POSITION_END_DATE);
        LocalDate randomJobEnd = LocalDateTools.randomSeedDate(8, 0, ChronoUnit.MONTHS, TimeType.RANDOM);
        if (Objects.isNull(closePosition)) {
            addTag("ABCHR4840-2");
            changeTestName("Удаление даты окончания должности");
            clearDateInFieldEndPosition();
        } else {
            chooseDatePositionForm(randomJobEnd, DateTypeField.END_JOB);
        }
        LocalDate positionEndDateAfterUI = getDateInForm(DateTypeField.POSITION_END_DATE);
        saveButtonClick();
        assertPositionEndDates(closePosition, emp, positionEndDateBeforeUI, positionEndDateAfterUI, randomJobEnd.plusDays(1));
    }

    @Test(groups = {"ABCHR8550-1", MIX1, G2, IN_PROGRESS}, description = "Права на изменение логина / пароля для входа в систему другому сотруднику",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "8550_Ограничение редактирования логина пользователя.", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=270096776")
    @Owner(KHOROSHKOV)
    @TmsLink("118977")
    @Tag("ABCHR4840-1")
    public void transferLoginPasswordModificationRights(boolean permissionsType) {
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_VIEW, PermissionType.ORGANIZATION_UNIT_EDIT));
        if (permissionsType) {
            permissions.add(PermissionType.ORG_EDIT_USER_NAME_PARAMETERS);
            permissions.add(PermissionType.ORG_EDIT_PASS_PARAMETERS);
        } else {
            addTag("ABCHR8550-2");
            changeTestName("Права на изменение своего логина / пароля для входа в систему");
            permissions.add(PermissionType.ORG_EDIT_PERSONAL_USER_NAME_PARAMETERS);
            permissions.add(PermissionType.ORG_EDIT_PERSONAL_PASS_PARAMETERS);
        }
        ImmutableTriple<OrgUnit, EmployeePosition, EmployeePosition> triple = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftTriple();
        OrgUnit orgUnit = triple.getLeft();
        Employee firstEmployee = triple.getMiddle().getEmployeePosition().getEmployee();
        Employee secondEmployee = triple.getRight().getEmployeePosition().getEmployee();
        Role role = PresetClass.createCustomPermissionRole(permissions);
        EmployeeInfoName employeeInfoName = EmployeeInfoName.LOGIN_OPTIONS;
        String login = RandomStringUtils.randomAlphanumeric(10);
        String password = RandomStringUtils.randomAlphanumeric(10);
        goToOrgStructureAsUserWithoutWait(role, orgUnit, firstEmployee.getUser());
        clickEmployeeTab();
        Employee currentEmployee = permissionsType ? secondEmployee : firstEmployee;
        chooseEmployee(currentEmployee.getFullName());
        clickOnShowButton(employeeInfoName);
        clickOnPencilButton(employeeInfoName);
        enterNewLogin(login);
        enterNewPassword(password);
        confirmNewPassword(password);
        clickOnChangeButton(employeeInfoName);
        assertIsEditLogin(login, currentEmployee.refreshEmployee());
        currentEmployee = permissionsType ? firstEmployee : secondEmployee;
        clearEmployeeSearchField();
        chooseEmployee(currentEmployee.getFullName());
        clickOnShowButton(employeeInfoName);
        assertPencilNotChange(employeeInfoName.getNameOfInformation());
    }

    @Test(groups = {"ABCHR8550-3", MIX1, G2, IN_PROGRESS}, description = "Права на изменение логина для входа в систему себе и другому сотруднику без возможности изменения пароля",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "8550_Ограничение редактирования логина пользователя.", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=270096776")
    @Owner(KHOROSHKOV)
    @TmsLink("118977")
    @Tag("ABCHR4840-3")
    public void editLoginWithoutPasswordModificationForSelfAndOtherUser(boolean permissionsType) {
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_VIEW, PermissionType.ORGANIZATION_UNIT_EDIT));
        if (permissionsType) {
            permissions.add(PermissionType.ORG_EDIT_USER_NAME_PARAMETERS);
            permissions.add(PermissionType.ORG_EDIT_PERSONAL_USER_NAME_PARAMETERS);
        } else {
            addTag("ABCHR8550-4");
            changeTestName("Права на изменение пароля для входа в систему себе и другому сотруднику без возможности изменения логина");
            permissions.add(PermissionType.ORG_EDIT_PASS_PARAMETERS);
            permissions.add(PermissionType.ORG_EDIT_PERSONAL_PASS_PARAMETERS);
        }
        ImmutableTriple<OrgUnit, EmployeePosition, EmployeePosition> triple = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftTriple();
        OrgUnit orgUnit = triple.getLeft();
        Employee firstEmployee = triple.getMiddle().getEmployeePosition().getEmployee();
        Employee secondEmployee = triple.getRight().getEmployeePosition().getEmployee();
        Role role = PresetClass.createCustomPermissionRole(permissions);
        EmployeeInfoName employeeInfoName = EmployeeInfoName.LOGIN_OPTIONS;
        List<Employee> employees = Arrays.asList(firstEmployee, secondEmployee);
        goToOrgStructureAsUserWithoutWait(role, orgUnit, firstEmployee.getUser());
        for (Employee currentEmployee : employees) {
            clickEmployeeTab();
            clearEmployeeSearchField();
            chooseEmployee(currentEmployee.getFullName());
            clickOnShowButton(employeeInfoName);
            clickOnPencilButton(employeeInfoName);
            if (permissionsType) {
                String login = RandomStringUtils.randomAlphanumeric(10);
                enterNewLogin(login);
                assertFieldDisabled("Пароль");
                assertFieldDisabled("Подтверждение пароля");
                clickOnChangeButton(employeeInfoName);
                assertIsEditLogin(login, currentEmployee.refreshEmployee());
            } else {
                assertFieldDisabled("Логин");
            }
        }
    }

    private String composeEmployeePosition(EmployeePosition ep) {
        String dateIntervalOnUI = String.format("%s – %s", composeDateForEmployeePositionUI(ep.getDateInterval().getStartDate()), composeDateForEmployeePositionUI(ep.getDateInterval().getEndDate()));
        String positionName = ep.getPosition().getName();
        String orgUnitName = (boolean) SystemPropertyRepository.getSystemProperty(SystemProperties.DISPLAY_THE_NEAREST_PARENT_OF_ORGUNIT).getValue() ?
                String.format("%s / %s", ep.getOrgUnit().getParentOrgUnit().getName(), ep.getOrgUnit().getName()) :
                ep.getOrgUnit().getName();
        return String.format("%s %s  %s", positionName, orgUnitName, dateIntervalOnUI);
    }

    private List<String> getEmployeePositionsFromUI() {
        systemSleep(2);
        return os.osCardForm().allEmployeePositions().stream()
                .map(emp -> emp.getAttribute("outerText").replace("\n", " "))
                .sorted()
                .collect(Collectors.toList());
    }

    private String composeDateForEmployeePositionUI(LocalDate date) {
        if (Objects.isNull(date)) {
            return "Настоящее время";
        } else {
            return String.format("%d %s %d", date.getDayOfMonth(), date.getMonth().getDisplayName(TextStyle.FULL, new Locale("ru")), date.getYear());
        }
    }

    private void assertEmployeePositionIsVisible(List<String> employeePositionsOnUI, EmployeePosition ep) {
        String expectedEmployeePosition = composeEmployeePosition(ep);
        LOG.info(employeePositionsOnUI.toString());
        Assert.assertTrue(employeePositionsOnUI.contains(expectedEmployeePosition), "Назначение " + expectedEmployeePosition + " не найдено в списке");
    }

    @Test(groups = {"ABCHR-85672-1", MIX2, G2, IN_PROGRESS},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Логика скрытия назначений в блоке \"Место работы\" в карточке сотрудника под пользователем " +
                    "в зависимости от системной настройки")
    @Link(name = "Статья: \"85672_[Магнит. Карточка сотрудника] Отображение блока \"Место работы\"\"",
            url = "https://wiki.goodt.me/x/HHhwE")
    @TmsLink("118721")
    @Tag("ABCHR-85672-1")
    @Tag(MIX2)
    public void displayEmployeePositionsAccordingToSystemSetting(boolean systemSettingValue) {
        changeProperty(SystemProperties.NEW_DISPLAY_OF_WORK_PLACE, systemSettingValue);
        ImmutablePair<OrgUnit, EmployeeEssence> pair = getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        OrgUnit unit = pair.left;
        Employee employee = EmployeeRepository.getEmployeeWithMoreThanOneMainPosition(unit);
        PresetClass.presetForOpenedAndClosedEmployeePositions(employee);
        List<EmployeePosition> employeePositionsList = systemSettingValue ? EmployeePositionRepository.getEmployeePositionsFromEmployee(employee) : EmployeePositionRepository.getAllEmployeePositionsFromEmployee(employee);
        List<String> expectedEmployeePositionsOnUI = employeePositionsList.stream().map(this::composeEmployeePosition).sorted().collect(Collectors.toList());
        goToOrgStructureAsUserWithoutWait(Role.ADMIN_TT, unit);
        clickEmployeeTab();
        setOrgUnitFilterInEmployeesTab(unit.getName());
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.PLACE_OF_WORK);
        List<String> actualEmployeePositionsUI = getEmployeePositionsFromUI();
        Allure.step("Проверить, что найденные через API назначения отображаются на UI", () -> {
            Allure.addAttachment("Назначения в API", expectedEmployeePositionsOnUI.toString());
            Allure.addAttachment("Назначения на UI", actualEmployeePositionsUI.toString());
            Assert.assertEquals(actualEmployeePositionsUI, expectedEmployeePositionsOnUI, "Списки не совпадают");
        });
    }

    @Test(groups = {"ABCHR-85672-2", MIX2, G2, IN_PROGRESS},
            description = "Cкрытие всех временных назначений в блоке \"Место работы\" в карточке сотрудника под пользователем")
    @Link(name = "Статья: \"85672_[Магнит. Карточка сотрудника] Отображение блока \"Место работы\"\"",
            url = "https://wiki.goodt.me/x/HHhwE")
    @TmsLink("118721")
    @Tag("ABCHR-85672-2")
    @Tag(MIX2)
    public void hideTemporaryEmployeePositions() {
        changeProperty(SystemProperties.NEW_DISPLAY_OF_WORK_PLACE, true);
        OrgUnit unit = getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION).left;
        EmployeePosition ep = PresetClass.temporaryPositionPreset(unit);
        goToOrgStructureAsUserWithoutWait(Role.ADMIN_TT, unit);
        clickEmployeeTab();
        setOrgUnitFilterInEmployeesTab(unit.getName());
        chooseEmployee(ep.getEmployee().getFullName());
        clickOnShowButton(EmployeeInfoName.PLACE_OF_WORK);
        Allure.step("Проверить, что временное назначение не отображается в списке",
                    () -> Assert.assertThrows(WaitUntilException.class, () -> assertEmployeePositionIsVisible(getEmployeePositionsFromUI(), ep)));
    }

    @Test(groups = {"ABCHR-85672-4", MIX2, G2, IN_PROGRESS},
            description = "При отсутствии активных назначений, выводить только последнее закрытое назначение в хронологическом порядке, в блоке \"Место работы\" в карточке сотрудника под пользователем")
    @Link(name = "Статья: \"85672_[Магнит. Карточка сотрудника] Отображение блока \"Место работы\"\"",
            url = "https://wiki.goodt.me/x/HHhwE")
    @TmsLink("118721")
    @Tag("ABCHR-85672-4")
    @Tag(MIX2)
    public void showLastActiveEmployeePosition() {
        changeProperty(SystemProperties.NEW_DISPLAY_OF_WORK_PLACE, true);
        OrgUnit unit = getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION).left;
        Employee employee = EmployeeRepository.getEmployeeWithOnlyClosedPositions(unit);
        PresetClass.presetForClosedEmployeePositions(employee);
        EmployeePosition ep = EmployeePositionRepository.getAllEmployeePositionsFromEmployee(employee).stream().max(Comparator.comparing(employeePosition -> employeePosition.getDateInterval().getEndDate())).get();
        goToOrgStructureAsUserWithoutWait(Role.ADMIN_TT, unit);
        clickEmployeeTab();
        setOrgUnitFilterInEmployeesTab(unit.getName());
        chooseEmployee(ep.getEmployee().getFullName());
        clickOnShowButton(EmployeeInfoName.PLACE_OF_WORK);
        Allure.step("Проверить, что временное назначение не отображается в списке", () -> assertEmployeePositionIsVisible(getEmployeePositionsFromUI(), ep));
    }

    @Test(groups = {"ABCHR-85672-5", MIX2, G2, IN_PROGRESS},
            description = "При переводе сотрудника в рамках текущего месяца, в течение этого месяца в карточке сотрудника будут отображаться все его кадровые перемещения текущего месяца")
    @Link(name = "Статья: \"85672_[Магнит. Карточка сотрудника] Отображение блока \"Место работы\"\"",
            url = "https://wiki.goodt.me/x/HHhwE")
    @TmsLink("118721")
    @Tag("ABCHR-85672-5")
    @Tag(MIX2)
    public void showChangesInEmployeePositionsThisMonth() {
        changeProperty(SystemProperties.NEW_DISPLAY_OF_WORK_PLACE, true);
        OrgUnit unit = getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION).left;
        Employee employee = EmployeeRepository.getEmployeeWithMoreThanOneMainPosition(unit);
        PresetClass.presetForOpenedAndClosedEmployeePositionsThisMonth(employee);
        List<EmployeePosition> employeePositionsList = EmployeePositionRepository.getEmployeePositionsFromEmployee(employee, ShiftTimePosition.ALLMONTH.getShiftsDateInterval());
        List<String> expectedEmployeePositionsOnUI = employeePositionsList.stream().map(this::composeEmployeePosition).sorted().collect(Collectors.toList());
        goToOrgStructureAsUserWithoutWait(Role.ADMIN_TT, unit);
        clickEmployeeTab();
        setOrgUnitFilterInEmployeesTab(unit.getName());
        chooseEmployee(employee.getFullName());
        clickOnShowButton(EmployeeInfoName.PLACE_OF_WORK);
        List<String> actualEmployeePositionsUI = getEmployeePositionsFromUI();
        Allure.step("Проверить, что найденные через API назначения отображаются на UI", () -> {
            Allure.addAttachment("Назначения в API", expectedEmployeePositionsOnUI.toString());
            Allure.addAttachment("Назначения на UI", actualEmployeePositionsUI.toString());
            Assert.assertEquals(actualEmployeePositionsUI, expectedEmployeePositionsOnUI, "Списки не совпадают");
        });
    }
}
