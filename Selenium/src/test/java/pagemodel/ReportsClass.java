package pagemodel;

import com.google.inject.Inject;
import common.DataProviders;
import guice.TestModule;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.exception.WaitUntilException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.HttpResponse;
import org.hamcrest.Matchers;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.ReportsPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.*;
import utils.Links;
import utils.downloading.FileDownloadCheckerForReport;
import utils.downloading.TypeOfAcceptContent;
import utils.downloading.TypeOfFiles;
import utils.downloading.TypeOfReports;
import utils.tools.CustomTools;
import utils.tools.LocalDateTools;
import wfm.PresetClass;
import wfm.components.orgstructure.OrganizationUnitTypeId;
import wfm.components.schedule.ScheduleType;
import wfm.components.utils.*;
import wfm.models.DateInterval;
import wfm.models.OrgUnit;
import wfm.repository.CommonRepository;
import wfm.repository.OrgUnitRepository;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static common.Groups.*;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.Params.ACTIVE;
import static utils.downloading.FileDownloadChecker.getFileNameExtensionFromResponse;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.assertStatusCode;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class ReportsClass extends BaseTest {

    private static final String RELEASE_URL = Links.getTestProperty("release");
    private static final String URL_A = RELEASE_URL + "/reports";
    //TODO определить на каком ОМ или на списке ОМов тестировать. Задача есть в джире (TEST-1200).
    private static final String OM_NAME = "IRG";
    private static final Logger LOG = LoggerFactory.getLogger(ReportsClass.class);
    private static final Section SECTION = Section.REPORTS;

    @Inject
    private ReportsPage rp;

    @DataProvider(name = "RolesWithoutPermissions")
    private static Object[][] rolesWithoutPermissions() {
        Object[][] array = new Object[13][2];
        array[0] = new Object[]{Role.FIRST, Role.FIRST.getNames()};
        array[1] = new Object[]{Role.THIRD, Role.THIRD.getNames()};
        array[2] = new Object[]{Role.FOURTH, Role.FOURTH.getNames()};
        array[3] = new Object[]{Role.FIFTH, Role.FIFTH.getNames()};
        array[4] = new Object[]{Role.SIXTH, Role.SIXTH.getNames()};
        array[5] = new Object[]{Role.SEVENTH, Role.SEVENTH.getNames()};
        array[6] = new Object[]{Role.EIGHTH, Role.EIGHTH.getNames()};
        array[7] = new Object[]{Role.NINTH, Role.NINTH.getNames()};
        array[8] = new Object[]{Role.TENTH, Role.TENTH.getNames()};
        array[9] = new Object[]{Role.ELEVENTH, Role.ELEVENTH.getNames()};
        array[10] = new Object[]{Role.TWELFTH, Role.TWELFTH.getNames()};
        array[11] = new Object[]{Role.THIRTEENTH, Role.THIRTEENTH.getNames()};
        array[12] = new Object[]{Role.FOURTEENTH, Role.FOURTEENTH.getNames()};
        return array;
    }

    @DataProvider(name = "download shift reports")
    private static Object[][] rolesWithPermissions() {
        Object[][] array = new Object[6][];
        array[0] = new Object[]{Role.FIRST, TypeOfFiles.XLSX, "TK2046-2"};
        array[1] = new Object[]{Role.FOURTH, TypeOfFiles.XLSX, "TK2046-2"};
        array[2] = new Object[]{Role.FIRST, TypeOfFiles.CSV, "TK2046-3"};
        array[3] = new Object[]{Role.FOURTH, TypeOfFiles.CSV, "TK2046-3"};
        array[4] = new Object[]{Role.FIRST, TypeOfFiles.ZIP, "TK2046-4"};
        array[5] = new Object[]{Role.FOURTH, TypeOfFiles.ZIP, "TK2046-4"};
        return array;
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void setUp() {
        setBrowserTimeout(rp.getWrappedDriver(), 15);
    }

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriverWindow() {
        closeDriver(rp.getWrappedDriver());
    }

    @AfterMethod(alwaysRun = true, description = "Очистить куки и подготовить новую вкладку")
    private void cleanCookies() {
        cleanCookiesAndOpenNewTab(rp.getWrappedDriver());
    }

    /**
     * Перейти в раздел Отчеты
     */
    private void goToReports() {
        new GoToPageSection(rp).getPage(SECTION, 60);
    }

    @Step("Нажать на кнопку \"Численность по графикам\"")
    private void numberOfGraphsClick() {
        rp.reportWorkField().numberOfGraphs().click();
    }

    @Step("Нажать на кнопку \"Статус публикации графиков\"")
    private void publicationStatusClick() {
        LOG.info("Нажимаем на кнопку \"Статус публикации графиков\"");
        rp.rightPanelInReport().publicationStatus().click();
    }

    @Step("Переключение на новую активную вкладку")
    private void switchToWindow(int windowId) {
        ArrayList<String> tab = new ArrayList<>(rp.getWrappedDriver().getWindowHandles());
        rp.getWrappedDriver().switchTo().window(tab.get(windowId));
    }

    @Step("Ввести название ОМ и выбрать \"{certainOM}\" из списка")
    private void sendOmNameAndCertainOmFromList(String certainOM) {
        rp.reportWorkField().inputOmSearchField().click();
        slowSendKeys(rp.reportWorkField().inputOmSearchField(), certainOM);
        LOG.info("Выбираем {} из списка", certainOM);
        if (rp.reportWorkField().checkBoxButton(certainOM).isDisplayed()) {
            rp.reportWorkField().checkBoxButton(certainOM).click();
            changeStepName(String.format("Выбрать: \"%s\" из списка", certainOM));
            return;
        }
        try {
            rp.reportWorkField().certainOmCheckBox(certainOM)
                    .waitUntil("Введенный оргюнит не отображается в списке", DisplayedMatcher.displayed(), 30);
            waitForClickable(rp.reportWorkField().certainOmCheckBox(certainOM), rp, 15);
            rp.reportWorkField().certainOmCheckBox(certainOM).click();
        } catch (WaitUntilException e) {
            rp.reportWorkField().inputOmSearchField().sendKeys(Keys.SPACE);
            systemSleep(2); //метод используется в неактуальных тестах
            rp.reportWorkField().inputOmSearchField().sendKeys(Keys.BACK_SPACE);
            systemSleep(2); //метод используется в неактуальных тестах
            rp.reportWorkField().certainOmCheckBox(certainOM)
                    .waitUntil("Введенный оргюнит не отображается в списке", DisplayedMatcher.displayed(), 30);
            rp.reportWorkField().certainOmCheckBox(certainOM).click();
        }
    }

    @Step("Выбрать ОМ: {certainOM} из списка")
    private void certainOmFromList(String certainOM) {
        rp.reportWorkField().certainOmCheckBox(certainOM)
                .waitUntil("Введенный оргюнит не отображается в списке", DisplayedMatcher.displayed(), 30);
        rp.reportWorkField().certainOmCheckBox(certainOM).click();
    }

    @Step("Выбрать {date} в появившемся окне календаря")
    private void pickMonthForReport(LocalDate date) {
        LOG.info("Выбираем {} в появившемся окне календаря", date);
        rp.rightPanelInReport().monthInputField().click();
        DatePicker monthPicker = new DatePicker(rp.datePickerForm());
        monthPicker.pickMonth(date);
        monthPicker.okButtonClick();
    }

    @Step("Выбрать {date} в появившемся окне календаря")
    private void pickStartMonthForTargetNumber(LocalDate date) {
        rp.targetNumberPage().startMonthInput().click();
        DatePicker monthPicker = new DatePicker(rp.datePickerForm());
        monthPicker.pickMonth(date);
        monthPicker.okButtonClick();
    }

    @Step("Выбрать {date} в появившемся окне календаря, для скачивания отчета")
    private void pickEndMonthForTargetNumber(LocalDate date) {
        rp.targetNumberPage().endMonthInput().click();
        DatePicker monthPicker = new DatePicker(rp.datePickerForm());
        monthPicker.pickMonth(date);
        monthPicker.okButtonClick();
    }

    @Step("В поле \"Дата начала\" выбрать {date}")
    private void pickMonthForAttendance(LocalDate date, DateTypeField buttonDate) {
        rp.targetNumberPage().buttonDate(buttonDate.getName()).clear();
        rp.targetNumberPage().buttonDate(buttonDate.getName()).sendKeys(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    }

    @Step("Выбрать тип файла {typeOfFiles} для скачивания из отчета {typeOfFiles.fileExtension}")
    private void chooseDownloadFormat(TypeOfFiles typeOfFiles) {
        rp.rightPanelInReport().typeOfDownloadFiles(typeOfFiles.getFileFormat()).click();
    }

    @Step("Нажать на \"{typeOfReports.nameOfReport}\" из списка отчетов")
    private void chooseReportType(TypeOfReports typeOfReports) {
        LOG.info("Выбираем тип отчета {}", typeOfReports.getNameOfReport());
        rp.reportTypePanel()
                .waitUntil("Панель с типами отчетов не загрузилась", DisplayedMatcher.displayed(), 30);
        systemSleep(5); //пришлось проставить ожидания, потому что на гриде в дженкинсе не прогружается
        rp.reportTypePanel().reportButtonByName(typeOfReports.getNameOfReport()).click();
        systemSleep(5); //пришлось проставить ожидания, потому что на гриде в дженкинсе не прогружается
    }

    @Step("Переключиться на новую вкладку")
    private void removeFirstWindowHandler() {
        CustomTools.removeFirstWindowHandler(rp);
    }

    @Step("Проверить, что список доступных отчетов, совпадает с отчетами в разрешениях")
    private void checkReportsMatches(List<String> expected) {
        List<String> uiList = rp.reportTypePanel().reportsButtons().stream().map(WebElement::getText).collect(Collectors.toList());
        Allure.addAttachment("Отобразились следующие роли", uiList.toString()
                .replaceAll("\\[, ]", "").replaceAll(", ", "\n"));
        uiList.remove("Накопленный баланс переработок");
        List<String> copy = new ArrayList<>(uiList);
        copy.removeAll(expected);
        Assert.assertTrue(expected.containsAll(uiList), "Имеются лишние разрешения: " + copy);
    }

    @Step("Проверить что был выполнен переход на страницу \"Целевая численность\"")
    private void assertGoToTargetNumberPage() {
        rp.targetNumberPage().should("Переход не был осуществлен", DisplayedMatcher.displayed(), 20);
        rp.targetNumberPage().endMonthInput().should("Переход не был осуществлен", DisplayedMatcher.displayed(), 20);
        rp.targetNumberPage().startMonthInput().should("Переход не был осуществлен", DisplayedMatcher.displayed(), 20);
        rp.targetNumberPage().tagFilterButton().should("Переход не был осуществлен", DisplayedMatcher.displayed(), 20);
        Allure.addAttachment("Проверка", "Был осуществлен переход на вкладку  \"Целевая численность\" Были " +
                "отображены элементы:" +
                " формы с деревом ОМ, кнопкой \"Фильтр по тегам\" и формы с выбором даты начала и окончания.");
    }

    /**
     * Моделируем скачивание выбранного типа отчета под конкретный ЮРЛ по текущей кукой
     * определенного пользователя
     *
     * @param checker   респонс для проверки скачивания
     * @param localDate дата отчета
     * @param role      роль, из-под которой происходит скачивание
     */
    @Step("Проверка скачивания отчета {checker.typeOfReports.nameOfReport} под текущей кукой")
    private void checkReportDownloading(FileDownloadCheckerForReport checker, LocalDate localDate, Role role) {
        LocalDate startMonth = localDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endMonth = localDate.with(TemporalAdjusters.lastDayOfMonth());
        assertResponse(TypeOfAcceptContent.BASIC, checker, startMonth, endMonth, role);
    }

    @Step("Проверить, что был скачан файл отчета типа {checker.typeOfReports.nameOfReport} и в нужный диапазон дат {dates}")
    private void checkReportDownloading(FileDownloadCheckerForReport checker, DateInterval dateInterval, TypeOfAcceptContent content) {
        assertResponse(content, checker, dateInterval.getStartDate(), dateInterval.getEndDate(), Role.ADMIN);
    }

    /**
     * Метод проверки скачивания отчетов для контента за выбранные даты/месяц
     *
     * @param content   тип контента
     * @param checker   инициализированный чекер
     * @param startDate дата начала отчета
     * @param endDate   дата конца отчета
     * @param role      роль, из-под которой происходит скачивание
     */
    private void assertResponse(TypeOfAcceptContent content, FileDownloadCheckerForReport checker,
                                LocalDate startDate, LocalDate endDate, Role role) {
        HttpResponse httpResponse = checker.downloadResponse(role, content);
        assertStatusCode(httpResponse, 200, checker.getDownloadLink().toString());
        //Определение имени файла
        ImmutablePair<String, String> fileNameExtensionFromResponse = getFileNameExtensionFromResponse(httpResponse);
        String expectedFormat = checker.getTypeOfFiles().getFileExtension();
        String expectedFilename = checker.getFileName();
        String responseFilename = fileNameExtensionFromResponse.left;
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(fileNameExtensionFromResponse.right, expectedFormat, "Расширение файла не совпадает с ожидаемым");
        softAssert.assertEquals(responseFilename, expectedFilename, "Имя файла не совпадает с ожидаемым");
        softAssert.assertTrue(responseFilename.contains(startDate.toString()), "Даты начала не было в названии файла");
        softAssert.assertTrue(responseFilename.contains(endDate.toString()), "Даты окончания не было в названии файла");
        softAssert.assertAll();
        Allure.addAttachment("Скачанный файл",
                             "text/plain",
                             "Скачан файл: " + responseFilename + "." + fileNameExtensionFromResponse.right);
    }

    @Step("Нажали на кнопку скачивания отчета качества исторических данных (CSV)")
    private void pressDownloadCsvReport() {
        rp.rightPanelInReport().csvReport().click();
    }

    @Step("Проверка скачивания и названия скачиваемого файла формата {checker.typeOfFiles.fileExtension}")
    private void assertDownloadSimpleFile(FileDownloadCheckerForReport checker) {
        HttpResponse httpResponse = checker.downloadResponse(Role.ADMIN, TypeOfAcceptContent.BASIC);
        assertStatusCode(httpResponse, 200, checker.getDownloadLink().toString());
        //Определение имени файла
        ImmutablePair<String, String> fileNameExtensionFromResponse = getFileNameExtensionFromResponse(httpResponse);
        String expectedFormat = checker.getTypeOfFiles().getFileExtension();
        String expectedFilename = checker.getFileName();
        String responseFilename = fileNameExtensionFromResponse.left;
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(fileNameExtensionFromResponse.right, expectedFormat);
        softAssert.assertEquals(responseFilename, expectedFilename);
        softAssert.assertAll();
        Allure.addAttachment("Скачанный файл",
                             "text/plain",
                             "Скачан файл: " + responseFilename + "." + fileNameExtensionFromResponse.right);
    }

    @Step("Выбор ОМ из дерева: {desiredOm.name}")
    private void chooseCertainOmFromTreeInTargetNumber(List<OrgUnit> desiredOm) {
        rp.targetNumberPage().waitUntil("Страница вкладки Целевая численность не отображена",
                                        DisplayedMatcher.displayed(), 25);
        List<Integer> omIds = desiredOm.stream().map(OrgUnit::getId).collect(Collectors.toList());
        TreeNavigate treeNavigate = new TreeNavigate(CommonRepository.getTreePath(omIds, URL_A));

        treeNavigate.workWithTree(rp.targetNumberPage(), Direction.DOWN);
    }

    @Step("Проверка осуществления перехода в выбранный отчет: {typeOfReports}")
    private void openCertainTabAssert(TypeOfReports typeOfReports) {
        Assert.assertEquals(rp.reportWorkField().nameOfReport().getText().trim(), typeOfReports.getNameOfReport(),
                            "Название отчета не соответствует переходу");
    }

    @Step("Нажать на кнопку \"Просмотреть отчет\"")
    private void checkReportClick() {
        rp.reportWorkField().checkReport().click();
        removeFirstWindowHandler();
        rp.loadingSpinner().grayLoadingBackground()
                .waitUntil("Спиннер загрузки все еще крутится", Matchers.not(DisplayedMatcher.displayed()), 60);
    }

    @Step("Нажать кнопку \"Скачать отчет (XLSX)\".")
    private void downloadXSLXLeftButtonClick() {
        rp.reportWorkField().checkReport().click();
    }

    @Step("Нажать кнопку \"Скачать отчет\".")
    private void downloadButtonClick() {
        rp.downloadReportButtonClick().click();
    }

    //    @Step("Нажать кнопку \"Скачать отчет\".")
    //    private void downloadButtonClick() {
    //        rp.targetNumberPage().downloadXSLXButton().click();
    //    }

    @Step("Проверяется отображение таблицы и ее название для ОМ {unitName}")
    private void checkTableForNewTab(String unitName) {
        Assert.assertEquals(rp.openReportTab().nameOfUnit().getText().split(":", 2).length, 2,
                            "Странное название у ОМ: " + unitName);
        String separatedName = rp.openReportTab().nameOfUnit().getText().split(":", 2)[1].trim();
        Assert.assertEquals(separatedName, unitName, "Неверное название таблицы для ОМ " + unitName);
        rp.openReportTab().mainTable().should("Таблица со значениями не отображена",
                                              DisplayedMatcher.displayed(), 5);
    }

    @Step("Переключение на масштаб {scopeReportTab}")
    private void scopeSwitchForTab(ScopeReportTab scopeReportTab) {
        //добавлено затем чтобы не перескакивало сразу на другие масштабы пока не прогрузится
        String actualScope = rp.mainHeader().actualScope().getText();
        ScopeReportTab actualScoreReport = Arrays.stream(ScopeReportTab.values())
                .filter(scope -> scope.getScopeName().contains(actualScope))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Такого масштаба в списке нет"));
        rp.openReportTab().allCellsFromFirstColumn()
                .waitUntil(Matchers.hasSize(Matchers.greaterThanOrEqualTo(actualScoreReport.getNumberOfElements())));
        rp.mainHeader().dayScope(scopeReportTab.ordinal()).click();
        rp.openReportTab().allCellsFromFirstColumn()
                .waitUntil(Matchers.hasSize(Matchers.greaterThanOrEqualTo(scopeReportTab.getNumberOfElements())));
    }

    @Step("Проверка перехода на страницу \"{typeOfReports.nameOfReport}\"")
    private void checkGoToPage(LocalDate date, TypeOfReports typeOfReports) {
        removeFirstWindowHandler();
        String dateString = getMonthYearString(date);
        rp.publicationGraphTable().currentDateMonth().should("Дата в разделе не совпала с ожидаемой",
                                                             text(containsString("Месяц: " + dateString)), 2);
        Allure.addAttachment("Проверка", "Была отображена дата: " + dateString);
    }

    @Step("Проверка перехода на страницу \"{typeOfReports.nameOfReport}\"")
    private void checkGoToPage(TypeOfReports typeOfReports, String orgUnitName) {
        removeFirstWindowHandler();
        rp.mainHeader().headerText().waitUntil("Не был осуществлен переход в раздел.",
                                               text(containsString(typeOfReports.getTitleName())), 10);
        rp.openReportTab().nameOfUnit().should(text(containsString("Подразделение: " + orgUnitName)));
    }

    private String getMonthYearString(LocalDate date) {
        return date.getMonth().getDisplayName(TextStyle.FULL_STANDALONE,
                                              Locale.forLanguageTag("ru")).toLowerCase() + " " + date.getYear();
    }

    @Step("Переключиться с даты {startDate} на {direction.changeValue} {unit}")
    private void tableSwitch(TableDirection direction, ChronoUnit unit, LocalDate startDate) {
        rp.openReportTab().allCellsFromFirstColumn().waitUntil(Matchers.hasSize(Matchers.greaterThan(1)));
        new Actions(rp.getWrappedDriver()).moveToElement(rp.openReportTab().mainTable()).perform();
        rp.openReportTab().tableSwitcher(direction.getNameOfDirection()).click();
        String dateAfter = getDateWithFormat(unit, startDate.plus(direction.getChangeValue(), unit).atTime(0, 0));
        rp.openReportTab().certainCellFromTable(1).waitUntil(text(containsString(dateAfter)));
        isDateMatches(unit, startDate.atTime(0, 0).plus(direction.getChangeValue(), unit));
    }

    @Step("Нажать ссылку <посмотреть> на странице \"публикация графиков\" для выбранного оргюнита")
    private void clickLookLink(String subdivision) {
        new Actions(rp.getWrappedDriver()).moveToElement(rp.publicationGraphTable().tableRow(subdivision)).perform();
        rp.publicationGraphTable().tableRow(subdivision).subdivisionLook()
                .waitUntil("Нет ссылки Просмотреть для подразделения, но можно сравнить " + subdivision, DisplayedMatcher.displayed(), 5);
        rp.publicationGraphTable().tableRow(subdivision).subdivisionLook().click();
    }

    @Step("Нажать ссылку <сравнить> на странице \"публикация графиков\" для выбранного оргюнита")
    private void clickCompareLink(String subdivision) {
        /* Наводим курсор на строку таблицы с  <subdivision>*/
        new Actions(rp.getWrappedDriver()).moveToElement(rp.publicationGraphTable().tableRow(subdivision)).perform();
        rp.publicationGraphTable().tableRow(subdivision).subdivisionCompare()
                .waitUntil("Нет кнопки сравнить для подразделения " + subdivision, DisplayedMatcher.displayed(), 5);
        rp.publicationGraphTable().tableRow(subdivision).subdivisionCompare().click();
    }

    private void isDateMatches(ChronoUnit unit, LocalDateTime firstDateTime) {
        ChronoUnit unitToMatch;
        switch (unit) {
            case DAYS:
                unitToMatch = ChronoUnit.HOURS;
                break;
            case MONTHS:
                unitToMatch = ChronoUnit.DAYS;
                break;
            case YEARS:
            default:
                unitToMatch = ChronoUnit.MONTHS;
        }
        List<String> uiDates = rp.openReportTab().allCellsFromFirstColumn()
                .stream().map(WebElement::getText).collect(Collectors.toList());
        for (String date : uiDates) {
            String format = getDateWithFormat(unit, firstDateTime);
            LOG.info("В ячейке {} отобразилась дата {}", uiDates.indexOf(date) + 1, date);
            Assert.assertTrue(date.equalsIgnoreCase(format), "Дата не совпала, на сайте: " + date + ", ожидалось: " + format);
            firstDateTime = firstDateTime.plus(1, unitToMatch);
        }
    }

    private String getDateWithFormat(ChronoUnit unit, LocalDateTime date) {
        switch (unit) {
            case DAYS:
                return date.format(DateTimeFormatter.ofPattern("HH:mm, d/MM/yy"));
            case MONTHS:
                String add = ".";
                if (date.getMonth() == Month.MAY || date.getMonth() == Month.JUNE || date.getMonth() == Month.JULY) {
                    add = "";
                }
                String month = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("ru")) + add;
                return date.getDayOfMonth() + " " + month + ", " + date.format(DateTimeFormatter.ofPattern("yy"));
            case YEARS:
                String monthDisplay = date.getMonth().getDisplayName(TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("ru"));
                return monthDisplay + ", " + date.format(DateTimeFormatter.ofPattern("yy"));
            default:
                return null;
        }
    }

    @Step("Выбор подразделения {subdivision} на странице публикации графиков")
    private void clickOnSubdivisionInPublishingTable(String subdivision) {
        /* На этом участке кода метод падает*/
        //        rp.loadingSpinner().grayBackground()
        //                .waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 30);
        //        systemSleep(1) ;

        /* Наводим курсор на строку таблицы с  <subdivision>*/
        new Actions(rp.getWrappedDriver()).moveToElement(rp.publicationGraphTable().tableRow(subdivision)).perform();
        rp.publicationGraphTable().tableRow(subdivision).subdivisionInTable(subdivision).click();
        removeFirstWindowHandler();
    }

    private String getRandomOrgUnitFromTable() {
        List<String> orgUnits = rp.publicationGraphTable().orgUnitsList()
                .waitUntil("В списке оргюнитов не было ни одного значения",
                           Matchers.not(Matchers.empty()), 10)
                .stream().map(WebElement::getText).collect(Collectors.toList());
        return getRandomFromList(orgUnits);
    }

    @Step("Проверка перехода на таблицу расписания")
    private void checkScheduleTable() {
        rp.scheduleTable().should("Расписание не отобразилось", DisplayedMatcher.displayed(), 30);
    }

    @Step("Проверить, что был осуществлен переход в расписание оргЮнита \"{orgUnit}\"")
    private void assertGoToOrgUnit(String orgUnit) {
        rp.mainHeader().headerText().waitUntil("Имя оргюнита не отобразилось",
                                               text(containsString("Расписание: " + orgUnit)), 15);
    }

    @Step("Проверка перехода на таблицу расписания в режиме сравнения")
    private void checkComparisonModeIndicator(String orgUnitName) {
        rp.chapter(orgUnitName).should("Страница расписания для " + orgUnitName + " не загрузилась", DisplayedMatcher.displayed(), 20);
        //Баг это окно не видео в полноэкранном режиме chrome хотя тест проходит. Пока отключил
        rp.comparisonWindow().waitUntil("Окно \"режим сравнения\"  с кнопкой \"Выйти\" не загрузилось", DisplayedMatcher.displayed(), 10);
        rp.comparisonWindow().click();
    }

    /**
     * Возвращаются имена ОМ по id из общего списка ОМов в API
     *
     * @param desiredOmIds        - List из id рандомно выбранного ОМ
     * @param mapFilledIdAndNames - Map из всех возможных ОМ
     * @return - List из имени ОМ с аналогичным id как в desiredOmIds
     */
    private List<String> omNamesReturner(List<Integer> desiredOmIds, Map<Integer, String> mapFilledIdAndNames) {
        return mapFilledIdAndNames.entrySet().stream().filter(entry -> desiredOmIds.contains(entry.getKey()))
                .map(Map.Entry::getValue).collect(Collectors.toList());
    }

    @Step("Проверка открытия новой вкладки для отчета {typeOfReports}")
    private void openNewTabForCertainAssert(TypeOfReports typeOfReports, LocalDate date, String unitName) {
        List<OrgUnit> orgUnits = OrgUnitRepository.getOrgUnitsNotClosedAndAllType();
        OrgUnit unit = orgUnits.stream().filter(orgUnit -> orgUnit.getName().equals(unitName)).findAny().orElse(null);
        int omId = unit == null ? 0 : unit.getId();
        String[] currentUrlByRegExp = rp.getWrappedDriver().getCurrentUrl().split("/");
        List<TypeOfReports> flagReports = new ArrayList<>();
        flagReports.add(TypeOfReports.PLAN_FACT_CONVERSION);
        flagReports.add(TypeOfReports.AVERAGE_CONVERSION);
        if (!flagReports.contains(typeOfReports)) {
            Allure.addAttachment("Сравнение URlов", "text/plain",
                                 "Текущий URL " + rp.getWrappedDriver().getCurrentUrl().split("/", 1)[0]
                                         + "\nURL который ожидаем: " + typeOfReports.getUrlName() + "/" + "org-units/"
                                         + omId + "/date/" + date);
        } else {
            Allure.addAttachment("Сравнение URlов", "text/plain",
                                 "Текущий URL " + rp.getWrappedDriver().getCurrentUrl().split("/", 1)[0]
                                         + "\nURL который ожидаем: " + typeOfReports.getUrlName() + "/" + "org-units/"
                                         + omId);
        }
        Assert.assertEquals(currentUrlByRegExp[3], typeOfReports.getUrlName());
        Assert.assertEquals(currentUrlByRegExp[5], String.valueOf(omId));
        if (!flagReports.contains(typeOfReports)) {
            Assert.assertEquals(currentUrlByRegExp[7], date.toString());
        }
        //при загрузке каких либо данных должно совпадать, если данных нет будет падать с ошибкой
        //        String reportTitle = rp.reportWorkField().newPageReportTitle().getText();
        //        reportTitle = reportTitle.substring(reportTitle.indexOf(" "), reportTitle.lastIndexOf("\""));
        //        Assert.assertEquals(reportTitle, unitName, "Название оргюнита в таблице не совпадает с выбранным");
    }

    @Step("Проверка перехода в отчеты репорта : \"Значения используемых параметров\" для оргюнита : {om}")
    private void assertForValuesOfParameters(String om) {
        rp.loadingSpinner().grayLoadingBackground()
                .waitUntil("Спиннер загрузки все еще крутится", Matchers.not(DisplayedMatcher.displayed()), 10);
        rp.mainHeader().headerText().waitUntil("Форма с таблицей не загрузилась", DisplayedMatcher.displayed(), 10);
        String nameOfReport = rp.openReportTab().nameOfUnit().getText();
        Assert.assertEquals(nameOfReport, "Подразделение: " + om, "Оргюниты в репорте не совпали");
    }

    @Step("Проверка на то, что в анализе средней конверсии, при переходе на вкладку \"День\", отображаются правильные даты")
    private void assertDateForTableScopeDay() {
        systemSleep(2); //метод используется в неактуальных тестах
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat scopeDay = new SimpleDateFormat("HH:mm, dd/MM/yy", Locale.getDefault());
        List<String> datesCompare = new ArrayList<>();
        List<String> datesUI = new ArrayList<>();
        int index = rp.openReportTab().allCellsFromFirstColumn().size();
        Calendar calendarForScopeDay = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                                             calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0);
        for (int i = 0; i < 24; i++) {
            String date = scopeDay.format(calendarForScopeDay.getTime());
            datesCompare.add(date);
            calendarForScopeDay.add(Calendar.HOUR, 1);
        }
        for (int i = 0; i < index; i++) {
            String dateFromUI = rp.openReportTab().allCellsFromFirstColumn().get(i).getText().replace(".", "");
            datesUI.add(dateFromUI);
        }
        Collections.sort(datesCompare);
        Collections.sort(datesUI);
        List<String> difference = new ArrayList<>(datesCompare);
        difference.removeAll(datesUI);
        Allure.addAttachment("Даты", "text/plain", "Даты на UI : " + datesUI + "\n\n" +
                "Ожидаемые даты : " + datesCompare + "\n\n" + "Разница между ними : " + difference);
        Assert.assertEquals(datesCompare, datesUI, "Даты на UI и ожидаемые даты не совпали");
        switchToWindow(0);
    }

    @Step("Проверка на то, что в анализе средней конверсии, при переходе на вкладку \"Год\", отображаются правильные даты")
    private void assertDateForTableScopeYear() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat scopeYear = new SimpleDateFormat("MMM., yy");
        List<String> datesCompare = new ArrayList<>();
        List<String> datesUI = new ArrayList<>();
        int index = rp.openReportTab().allCellsFromFirstColumn().size();
        Calendar calendarForScopeDay = new GregorianCalendar(calendar.get(Calendar.YEAR) + 1,
                                                             0, 0);
        // для года
        for (int i = 0; i <= 11; i++) {
            calendarForScopeDay.roll(Calendar.MONTH, 1);
            String date1 = scopeYear.format(calendarForScopeDay.getTime());
            String date = date1.substring(0, 1).toUpperCase() + date1.substring(1);
            String temp;
            switch (date.split(",")[0]) {
                case "Сен.":
                    temp = date.replace("Сен", "Сент");
                    datesCompare.add(temp);
                    break;
                case "Ноя.":
                    temp = date.replace("Ноя", "Нояб");
                    datesCompare.add(temp);
                    break;
                case "Июн.":
                    temp = date.replace("Июн.", "Июнь");
                    datesCompare.add(temp);
                    break;
                case "Июл.":
                    temp = date.replace("Июл.", "Июль");
                    datesCompare.add(temp);
                    break;
                case "Фев.":
                    temp = date.replace("Фев", "Февр");
                    datesCompare.add(temp);
                    break;
                case "Мар.":
                    temp = date.replace("Мар.", "Март");
                    datesCompare.add(temp);
                    break;
                case "Мая.":
                    temp = date.replace("Мая.", "Май");
                    datesCompare.add(temp);
                    break;
                default:
                    datesCompare.add(date);
                    break;
            }
        }
        for (int i = 0; i < index; i++) {
            String dateFromUI = rp.openReportTab().allCellsFromFirstColumn().get(i).getText();
            datesUI.add(dateFromUI);
        }
        Collections.sort(datesCompare);
        Collections.sort(datesUI);
        List<String> difference = new ArrayList<>(datesCompare);
        difference.removeAll(datesUI);
        Allure.addAttachment("Даты", "text/plain", "Даты на UI : " + datesUI + "\n\n" +
                "Ожидаемые даты : " + datesCompare + "\n\n" + "Разница между ними : " + difference);
        Assert.assertEquals(datesCompare, datesUI, "Даты на UI и ожидаемые даты не совпали");
        switchToWindow(0);
    }

    @Step("Проверка на то, что в анализе средней конверсии, при переходе на вкладку \"Месяц\", отображаются правильные даты")
    private void assertDateForTableScopeMonth() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat scopeMonth = new SimpleDateFormat("d MMM., yy");
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        List<String> datesCompare = new ArrayList<>();
        List<String> datesUI = new ArrayList<>();
        int index = rp.openReportTab().allCellsFromFirstColumn().size();
        calendar.set(Calendar.MONTH, currentMonth);
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        for (int i = 0; i < calendar.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
            calendar.roll(Calendar.DAY_OF_MONTH, 1);
            String date = scopeMonth.format(calendar.getTime());
            String temp;
            switch (currentMonth) {
                case 5:
                    temp = date.replace("мая.", "мая");
                    datesCompare.add(temp);
                    break;
                case 9:
                    temp = date.replace("сен", "сент");
                    datesCompare.add(temp);
                    break;
                case 11:
                    temp = date.replace("ноя", "нояб");
                    datesCompare.add(temp);
                    break;
                case 6:
                    temp = date.replace("июн.", "июня");
                    datesCompare.add(temp);
                    break;
                case 7:
                    temp = date.replace("июл.", "июля");
                    datesCompare.add(temp);
                    break;
                case 2:
                    temp = date.replace("фев", "февр");
                    datesCompare.add(temp);
                    break;
                default:
                    datesCompare.add(date);
            }
        }
        for (int i = 0; i < index; i++) {
            String dateFromUI = rp.openReportTab().allCellsFromFirstColumn().get(i).getText();
            datesUI.add(dateFromUI);
        }

        Collections.sort(datesCompare);
        Collections.sort(datesUI);
        List<String> difference = new ArrayList<>(datesCompare);
        difference.removeAll(datesUI);
        Allure.addAttachment("Даты", "text/plain", "Даты на UI : " + datesUI + "\n\n" +
                "Ожидаемые даты : " + datesCompare + "\n\n" + "Разница между ними : " + difference);
        Assert.assertEquals(datesCompare, datesUI, "Даты на UI и ожидаемые даты не совпали");
        switchToWindow(0);
    }

    @Step("Переключили таблицу в направлении : {tableDirection}")
    private void tableSwitch(TableDirection tableDirection) {
        List<AtlasWebElement> scopeElementsList = new ArrayList<>();
        scopeElementsList.add(rp.mainHeader().dayScope());
        scopeElementsList.add(rp.mainHeader().weekScope());
        scopeElementsList.add(rp.mainHeader().monthScope());
        ScopeReportTab[] listOfScope = ScopeReportTab.values();
        //Нашел текущий активный элемент
        AtlasWebElement currentActiveElement = scopeElementsList.stream()
                .filter(element -> element.getAttribute("class").contains(ACTIVE))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Такого индекса нет"));
        //Нашел какой это масштаб в енаме
        ScopeReportTab currentScope = Arrays.stream(listOfScope)
                .filter(scope -> String.valueOf(scope.ordinal()).contains(currentActiveElement.getAttribute("data-index")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Такого элемента нет"));
        new Actions(rp.getWrappedDriver()).moveToElement(rp.openReportTab().mainTable()).perform();
        //Для проверок даты
        String before = null;
        SimpleDateFormat dateFormat = null;
        Calendar calendar = Calendar.getInstance();
        Date currentDate = null;
        Date forAssertionDate = null;
        String beforeDateFormat = rp.openReportTab().certainCellFromTable(1).getText();
        switch (currentScope) {
            case YEAR:
                before = rp.openReportTab().certainCellFromTable(1).getText().split(",")[1].trim();
                dateFormat = new SimpleDateFormat("MMMM, yyyy");
                break;
            case MONTH:
                before = rp.openReportTab().certainCellFromTable(1).getText().split(" ")[1]
                        .replaceAll("[,.]", "");
                dateFormat = new SimpleDateFormat("dd MMMM, yyyy");
                break;
            case DAY:
                throw new AssertionError("Проблемы с перемещением на текущем масштабе");
        }
        int delta = 1;
        if (tableDirection != TableDirection.RIGHT) {
            delta = -1;
        }
        final String finalMonth = before;
        rp.openReportTab().tableSwitcher(tableDirection.getNameOfDirection()).click();
        systemSleep(3); //метод используется в неактуальных тестах
        Assert.assertEquals(rp.openReportTab().tableSwitcher(tableDirection.getNameOfDirection()).getText(),
                            beforeDateFormat, "Не успели переместиться по таблице перед проверкой");
        if (currentScope == ScopeReportTab.YEAR) {
            //TODO отладка для масштаба год
            int tempSum = Integer.parseInt(before) + delta;
            Allure.addAttachment("Перемещение в сторону " + tableDirection.getNameOfDirection()
                                         + " на масштабе " + currentScope,
                                 "text/plain",
                                 "В первой ячейке даты было " + before + "После перемещения ожидаем " + tempSum + "\n" + "");
        }
        if (currentScope == ScopeReportTab.MONTH) {
            MonthsEnum month = Arrays.stream(MonthsEnum.values())
                    .filter(monthsEnum -> monthsEnum.getDeclensionName().toLowerCase().contains(finalMonth))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Такого элемента нет"));
            if (month == MonthsEnum.MAR) {
                beforeDateFormat = beforeDateFormat.replaceAll("мар.", month.getDeclensionName());
            }
            try {
                currentDate = dateFormat.parse(beforeDateFormat);
            } catch (Exception ex) {
                LOG.info("Дата  {} не была распаршена", beforeDateFormat);
            }
            calendar.setTime(Objects.requireNonNull(currentDate));
            calendar.add(Calendar.MONTH, delta);
            Date waitDateAfterClick = calendar.getTime();
            Allure.addAttachment("Перемещение в сторону " + tableDirection.getNameOfDirection()
                                         + " на масштабе " + currentScope,
                                 "text/plain",
                                 "В первой ячейке даты было " + dateFormat.format(currentDate)
                                         + "\n" + "Ожидаем что будет " + dateFormat.format(waitDateAfterClick));
            String forAssertionDateString;
            if (rp.openReportTab().certainCellFromTable(1).getText().contains("мар.")) {
                forAssertionDateString = rp.openReportTab().certainCellFromTable(1).getText()
                        .replaceAll("мар.", month.getDeclensionName());
            } else {
                forAssertionDateString = rp.openReportTab().certainCellFromTable(1).getText();
            }
            try {
                forAssertionDate = dateFormat.parse(forAssertionDateString);
            } catch (Exception ex) {
                LOG.info("Дата {} не была распаршена", forAssertionDateString);
            }
            Assert.assertEquals(forAssertionDate, waitDateAfterClick);
        } else {
            throw new AssertionError("Проблемы с перемещением на текущем масштабе");
        }
    }

    /**
     * Данный метод осуществляет нажатие на чекбоксы у определенных ОМ
     *
     * @param desiredOmIds - строчный массив id нужных ОМ
     */
    @Step("Выбрать следующие ОМ из дерева : {omNames}")
    private void workWithTree(List<Integer> desiredOmIds, List<String> omNames) {
        rp.reportWorkField().waitUntil("Форма с выбором ОМ из дерева и поиска ОМ не отображена",
                                       Matchers.not(Matchers.emptyArray()), 5);
        TreeNavigate treeNavigate = new TreeNavigate(CommonRepository.getTreePath(desiredOmIds, URL_A));
        treeNavigate.workWithTree(rp.reportWorkField(), Direction.DOWN);
    }

    @Step("Выбрать оргюнит {orgUnit.name}")
    private void selectOmInSearch(OrgUnit orgUnit) {
        String name = orgUnit.getName();
        waitForClickable(rp.reportWorkField().largeOmInputSearchField(), rp, 10);
        rp.reportWorkField().largeOmInputSearchField().clear();
        rp.reportWorkField().largeOmInputSearchField().click();
        slowSendKeys(rp.reportWorkField().largeOmInputSearchField(), name);
        rp.reportWorkField().spinnerLoadingOm()
                .waitUntil("Загрузка оргюнитов идет слишком долго", Matchers.not(DisplayedMatcher.displayed()), 30);
        rp.reportWorkField().checkBoxButton(name).waitUntil("Чекбокс оргюнита " + name
                                                                    + " не был отображен", DisplayedMatcher.displayed(), 10);
        rp.reportWorkField().checkBoxButton(name).click();

    }

    @Step("Ввести в строку поиска и выбрать ОМ: {certainOM} из списка")
    private void certainOm(String certainOM) {
        rp.reportWorkField().inputOmFiled().click();
        slowSendKeys(rp.reportWorkField().inputIntoFiled(), certainOM);
        try {
            rp.reportWorkField().certainOmButton(certainOM)
                    .waitUntil("Введенный оргюнит не отображается в списке", DisplayedMatcher.displayed(), 30);
            rp.reportWorkField().certainOmButton(certainOM).click();
        } catch (WaitUntilException e) {
            rp.reportWorkField().inputIntoFiled().sendKeys(Keys.SPACE);
            systemSleep(2); //метод используется в неактуальных тестах
            rp.reportWorkField().inputIntoFiled().sendKeys(Keys.BACK_SPACE);
            systemSleep(2); //метод используется в неактуальных тестах
            rp.reportWorkField().certainOmButton(certainOM)
                    .waitUntil("Введенный оргюнит не отображается в списке", DisplayedMatcher.displayed(), 30);
            rp.reportWorkField().certainOmButton(certainOM).click();
        }
    }

    @Step("Выбрать оргюнит {orgUnit.name}")
    private void selectOmInSearch(List<OrgUnit> orgUnit) {
        rp.reportWorkField().largeOmInputSearchField().clear();
        rp.reportWorkField().largeOmInputSearchField().click();
        slowSendKeys(rp.reportWorkField().largeOmInputSearchField(), "_");
        for (OrgUnit unit : orgUnit) {
            String name = unit.getName();
            rp.reportWorkField().checkBoxButton(name).waitUntil("Чекбокс оргюнита " + name
                                                                        + " не был отображен", DisplayedMatcher.displayed(), 60);
            waitForClickable(rp.reportWorkField().checkBoxButton(name), rp, 60);
            rp.reportWorkField().checkBoxButton(name).click();
            systemSleep(1); //цикл
        }
    }

    @Step("Проверка на то, что отчет \"Посещаемость\" - отсутствует")
    private void checkForReportAbsent(TypeOfReports typeOfReports) {
        rp.reportTypePanel()
                .waitUntil("Панель с типами отчетов не загрузилась", DisplayedMatcher.displayed(), 10);
        rp.reportTypePanel().reportButtonByName(typeOfReports.getNameOfReport())
                .should("Отчет \"Посещаемость\" присутствует", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Проверка на то, что мы перешли на страницу типа отчета : {type}")
    private void assertForGoingToReportPage(TypeOfReports type) {
        rp.nameReportField().waitUntil("Поле с названием не отобразилось",
                                       DisplayedMatcher.displayed(), 10);
        String typeOnUI = rp.nameReportField().getText();
        String shouldType = type.getNameOfReport();
        Allure.addAttachment("Даты", "text/plain",
                             "Был переход на вкладку отчета под названием : " + typeOnUI);
        Assert.assertEquals(typeOnUI, shouldType, "Был переход во вкладку отчета : "
                + typeOnUI + ", а должен был быть : " + shouldType);
    }

    @Step("Проверить, что  поле \"Дата окончания\" подсвечивается красным, появилось сообщение об ошибке")
    private void checkDateEndErrorException(LocalDate startDate) {
        String date = startDate.toString();
        rp.targetNumberPage().redTextUnderDate()
                .should("Красная подсветка строки ввода даты не отображается",
                        text(containsString(("Должна быть не ранее, чем " + date))), 5);
    }

    @Step("Ввести часть названия ОМ: {certainOM} в строку поиска")
    private void sendPartNameInSearchField(String certainOM) {
        rp.reportWorkField().inputOmSearchField().click();
        String randomPartOfName = getRandomPartOfName(certainOM);
        slowSendKeys(rp.reportWorkField().inputOmSearchField(), randomPartOfName);
        rp.reportWorkField().spinnerLoadingOm()
                .waitUntil("Загрузка оргюнитов идет слишком долго", Matchers.not(DisplayedMatcher.displayed()), 30);
        Allure.addAttachment("Ввод", "Ввели в строку: " + randomPartOfName);
        List<String> unitList = rp.reportWorkField().displayedOrgUnits()
                .stream().map(AtlasWebElement::getText).collect(Collectors.toList());
        boolean b = unitList.stream().allMatch(s -> s.contains(randomPartOfName));
        Assert.assertTrue(b, "В списке оргюнитов есть названия без фрагмента названия введенного в поиск: " + randomPartOfName);
    }

    @Step("Нажать на кнопку очистки строки поиска")
    private void pressCleanButton() {
        rp.reportWorkField().cleanButton().click();
    }

    @Step("Проверка очистки строки поиска")
    private void assertCleanInputSearch() {
        systemSleep(5); //метод используется в неактуальных тестах
        List<String> unitList = rp.reportWorkField().displayedOrgUnits()
                .stream().map(AtlasWebElement::getText).collect(Collectors.toList());
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(unitList.size(), 1, "");
        softAssert.assertEquals(unitList.iterator().next(), "IRG");
        softAssert.assertAll();
    }

    @Step("Нажать на значок поиска по тегами")
    private void pressFindByTagButton() {
        rp.reportWorkField().tagButton().click();
    }

    @Step("Нажать на чекбоксы тегов: {tags}")
    private void pressTagCheckBoxes(List<String> tags) {
        for (String tag : tags) {
            rp.tagsForm().tagByName(tag).click();
        }
    }

    @Step("Нажать на кнопку \"Выбрать\" на вкладке тэги")
    private void clickSaveTags() {
        rp.tagsForm().choseButton().click();
    }

    @Step("Нажать на кнопку \"Очистить\" на вкладке тэги")
    private void clickResetTags() {
        rp.tagsForm().resetButton().click();
    }

    @Test(groups = "demo-7")
    private void firstTest() {
        goToReports();
        TypeOfReports shifts = TypeOfReports.SHIFTS;
        chooseReportType(shifts);
        pickMonthForReport(LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        chooseDownloadFormat(TypeOfFiles.CSV_GRID);
    }

    @Test(groups = "example")
    private void downloadScheduleXLSXShift() {
        goToReports();
        TypeOfReports shifts = TypeOfReports.SHIFTS;
        chooseReportType(shifts);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(Role.ADMIN, shifts, desiredOm, TypeOfFiles.CSV);
        pickMonthForReport(LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        chooseDownloadFormat(TypeOfFiles.XLSX);
        checkReportDownloading(checker, LocalDate.now(), Role.ADMIN);
    }

    @Test(groups = {"R-1.0.1", "release"}, description = "Открытие вкладки Смены")
    private void openingTheTabShifts() {
        goToReports();
        TypeOfReports shifts = TypeOfReports.SHIFTS;
        chooseReportType(shifts);
        openCertainTabAssert(shifts);
    }

    @Test(groups = {"R-1.1.1", "release"}, description = "Просмотр смен (xlsx)")
    private void viewShiftsInXlsx() {
        goToReports();
        TypeOfReports shifts = TypeOfReports.SHIFTS;
        chooseReportType(shifts);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        LocalDate dateForReport = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.ADMIN, shifts, desiredOm, dateForReport, TypeOfFiles.XLSX
        );
        pickMonthForReport(dateForReport);
        chooseDownloadFormat(TypeOfFiles.XLSX);
        checkReportDownloading(checker, dateForReport, Role.ADMIN);
    }

    @Test(groups = {"R-1.1.2", "release"}, description = "Просмотр смен (csv)")
    private void viewShiftInCsv() {
        goToReports();
        TypeOfReports shifts = TypeOfReports.SHIFTS;
        chooseReportType(shifts);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        LocalDate dateForReport = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.ADMIN, shifts, desiredOm, dateForReport, TypeOfFiles.CSV
        );
        pickMonthForReport(dateForReport);
        chooseDownloadFormat(TypeOfFiles.CSV);
        checkReportDownloading(checker, dateForReport, Role.ADMIN);
    }

    @Test(groups = {"R-1.1.3", "release"}, description = "Просмотр смен (zip)")
    private void viewShiftsInZip() {
        goToReports();
        TypeOfReports shifts = TypeOfReports.SHIFTS;
        chooseReportType(shifts);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        LocalDate dateForReport = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.ADMIN, shifts, desiredOm, dateForReport, TypeOfFiles.ZIP
        );
        pickMonthForReport(dateForReport);
        chooseDownloadFormat(TypeOfFiles.ZIP);
        checkReportDownloading(checker, dateForReport, Role.ADMIN);
    }

    @Test(groups = {"R-1.1.4", "release"}, description = "Просмотр смен (csv-grid)")
    private void viewShiftsInCsvGrid() {
        goToReports();
        TypeOfReports shifts = TypeOfReports.SHIFTS;
        chooseReportType(shifts);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        LocalDate dateForReport = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.ADMIN, shifts, desiredOm, dateForReport, TypeOfFiles.CSV_GRID
        );
        pickMonthForReport(dateForReport);
        chooseDownloadFormat(TypeOfFiles.CSV_GRID);
        checkReportDownloading(checker, dateForReport, Role.ADMIN);
    }

    @Test(groups = {"R-2.0.1", "release"}, description = "Открытие вкладки Численность по графикам")
    private void openingTheTabNumberOfGraph() {
        goToReports();
        TypeOfReports numberOfGraphs = TypeOfReports.NUMBER_OF_GRAPHS;
        chooseReportType(numberOfGraphs);
        openCertainTabAssert(numberOfGraphs);
    }

    @Test(groups = {"R-2.1", "release", "not actual"}, description = "Просмотр численности по графикам")
    private void viewTheTabNumberOfGraph() {
        goToReports();
        TypeOfReports type = TypeOfReports.NUMBER_OF_GRAPHS;
        chooseReportType(type);
        numberOfGraphsClick();
        checkGoToPage(LocalDate.now(), TypeOfReports.NUMBER_OF_GRAPHS);
    }

    @Test(groups = {"R-3.0.1", "release"}, description = "Открытие вкладки Статус публикации графиков")
    private void openingTheStatusPublicationGraph() {
        goToReports();
        TypeOfReports publicationStatus = TypeOfReports.PUBLICATION_STATUS;
        chooseReportType(publicationStatus);
        openCertainTabAssert(publicationStatus);
    }

    @Test(groups = {"R-3.1", "release"}, description = "Просмотр статуса публикации графиков")
    private void viewPublicationStatusOfGraphs() {
        goToReports();
        TypeOfReports type = TypeOfReports.PUBLICATION_STATUS;
        chooseReportType(type);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getHighest());
        selectOmInSearch(orgUnit);
        pickMonthForReport(date);
        publicationStatusClick();
        checkGoToPage(date, TypeOfReports.PUBLICATION_STATUS);
    }

    @Test(groups = {"R-3.3", "release"}, description = "Переход в режим Расписание из результата статуса публикации графиков")
    private void switchToScheduleModeFromStatusOfGraphs() {
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT).minusMonths(new Random().nextInt(12) + 1);
        goToReports();
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        chooseReportType(TypeOfReports.PUBLICATION_STATUS);
        sendOmNameAndCertainOmFromList(orgUnit.getName());
        pickMonthForReport(date);
        publicationStatusClick();
        removeFirstWindowHandler();
        clickLookLink(orgUnit.getName());
        removeFirstWindowHandler();
        checkComparisonModeIndicator(orgUnit.getName());
    }

    @Test(groups = {"R-3.4", "broken"}, description = "Переход в режим сравнения из результата статуса публикации графиков")
    private void switchToCompareModeFromStatusOfGraphs() {
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT).minusMonths(new Random().nextInt(12) + 1);
        goToReports();
        chooseReportType(TypeOfReports.PUBLICATION_STATUS);
        OrgUnit randomOrgUnit;
        randomOrgUnit = OrgUnitRepository.getRandomOrgUnitForComparisonGraph(date);
        sendOmNameAndCertainOmFromList(randomOrgUnit.getName());
        pickMonthForReport(date);
        publicationStatusClick();
        removeFirstWindowHandler();
        clickCompareLink(randomOrgUnit.getName());
        removeFirstWindowHandler();
        checkComparisonModeIndicator(randomOrgUnit.getName());
    }

    @Test(groups = {"R-4.0.1", "release"}, description = "Открытие вкладки Качество исторических данных")
    private void openingQualityOfHistoricalData() {
        goToReports();
        TypeOfReports qualityHistoricalData = TypeOfReports.QUALITY_HISTORICAL_DATA;
        chooseReportType(qualityHistoricalData);
        openCertainTabAssert(qualityHistoricalData);
    }

    @Test(groups = {"R-4.1", "release"}, description = "Просмотр Качества исторических данных")
    private void viewQualityOfHistoricalData() {
        goToReports();
        TypeOfReports qualityHistoricalData = TypeOfReports.QUALITY_HISTORICAL_DATA;
        chooseReportType(qualityHistoricalData);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.ADMIN, qualityHistoricalData, desiredOm, TypeOfFiles.CSV
        );
        pressDownloadCsvReport();
        assertDownloadSimpleFile(checker);
    }

    @Test(groups = {"R-5.0.1", "release", "TEST-76"}, description = "Открытие вкладки Квоты выходных дней")
    private void openHolidayQuotasTab() {
        goToReports();
        TypeOfReports holidayQuotas = TypeOfReports.HOLIDAY_QUOTAS;
        chooseReportType(holidayQuotas);
        openCertainTabAssert(holidayQuotas);
    }

    @Test(groups = {"R-5.1", "not actual", "TEST-76"}, description = "Просмотр Квоты выходных дней")
    private void checkHolidayQuotasTab() {
        goToReports();
        TypeOfReports holidayQuotas = TypeOfReports.HOLIDAY_QUOTAS;
        chooseReportType(holidayQuotas);
        String omName = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest()).getName();
        certainOm(omName);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        //TODO разобраться utils.tools.CustomTools.getObjectsOrgUnits
        openNewTabForCertainAssert(holidayQuotas, date, omName);
    }

    @Test(groups = {"R-6.0.1", "release"}, description = "Открытие вкладки Анализ средней конверсии")
    private void openAverageConversionTab() {
        goToReports();
        TypeOfReports averageConversion = TypeOfReports.AVERAGE_CONVERSION;
        chooseReportType(averageConversion);
        openCertainTabAssert(averageConversion);
    }

    @Test(groups = {"R-6.1", "broken", "not actual"}, description = "Просмотр Анализа средней конверсии")
    private void checkAverageConversionTab() {
        goToReports();
        TypeOfReports averageConversion = TypeOfReports.AVERAGE_CONVERSION;
        chooseReportType(averageConversion);
        String omName = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest()).getName();
        certainOm(omName);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        //TODO разобраться utils.tools.CustomTools.getObjectsOrgUnits
        openNewTabForCertainAssert(averageConversion, date, omName);
        checkTableForNewTab(omName);
    }

    @Test(groups = {"R-6.3.1", "Assert", "TEST-77"},
            description = "Изменение масштаба отображения результата анализа средней конверсии через вкладку День")
    private void checkAverageConversionTabScopeDay() {
        goToReports();
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest()).getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.DAY);
        assertDateForTableScopeDay();
    }

    @Test(groups = {"R-6.3.2", "Assert", "TEST-77"},
            description = "Изменение масштаба отображения результата анализа средней конверсии через вкладку Месяц")
    private void checkAverageConversionTabScopeMonth() {
        goToReports();
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest()).getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, 5, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.MONTH);
        assertDateForTableScopeMonth();
    }

    @Test(groups = {"R-6.3.3", "Assert", "TEST-77"},
            description = "Изменение масштаба отображения результата анализа средней конверсии через вкладку Год")
    private void checkAverageConversionTabScopeYear() {
        goToReports();
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest()).getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.YEAR);
        assertDateForTableScopeYear();
    }

    @Test(groups = {"R-6.4.1", "broken", "not actual"},
            description = "Переключение временного периода отображения результата анализа средней конверсии в режиме Год")
    private void checkAverageConversionTabDirectionSwitchYear() {
        goToReports();
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest()).getName());
        checkReportClick();
        LocalDate start = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.FIRST, LocalDateTools.RANDOM);
        tableSwitch(TableDirection.RIGHT, ChronoUnit.YEARS, start);
        tableSwitch(TableDirection.LEFT, ChronoUnit.YEARS, start.plusYears(1));
        tableSwitch(TableDirection.LEFT, ChronoUnit.YEARS, start);
    }

    @Test(groups = {"R-6.4.2", "broken", "not actual"},
            description = "Переключение временного периода отображения результата анализа средней конверсии в режиме Месяц")
    private void checkAverageConversionTabDirectionSwitchMonth() {
        goToReports();
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest()).getName());
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.MONTH);
        LocalDate start = LocalDateTools.getFirstDate();
        tableSwitch(TableDirection.RIGHT, ChronoUnit.MONTHS, start);
        tableSwitch(TableDirection.LEFT, ChronoUnit.MONTHS, start.plusMonths(1));
        tableSwitch(TableDirection.LEFT, ChronoUnit.MONTHS, start);
    }

    @Test(groups = {"R-6.4.3", "broken", "not actual"},
            description = "Переключение временного периода отображения результата анализа средней конверсии в режиме День")
    private void checkAverageConversionTabDirectionSwitchDay() {
        goToReports();
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest()).getName());
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.DAY);
        LocalDate start = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.THAT);
        tableSwitch(TableDirection.RIGHT, ChronoUnit.DAYS, start);
        tableSwitch(TableDirection.LEFT, ChronoUnit.DAYS, start.plusDays(1));
        tableSwitch(TableDirection.LEFT, ChronoUnit.DAYS, start);
    }

    @Test(groups = {"R-7.0.1", "release", "TEST-76"}, description = "Открытие вкладки Плановая и фактическая конверсия")
    private void openPlanFactConversionTab() {
        goToReports();
        TypeOfReports planFactConversion = TypeOfReports.PLAN_FACT_CONVERSION;
        chooseReportType(planFactConversion);
        openCertainTabAssert(planFactConversion);
    }

    @Test(groups = {"R-7.1", "TEST-76", "not actual"}, description = "Просмотр Плановой и фактической конверсии\n")
    private void checkPlanFactConversionTab() {
        goToReports();
        TypeOfReports planFactConversion = TypeOfReports.PLAN_FACT_CONVERSION;
        chooseReportType(planFactConversion);
        certainOm(OM_NAME);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        openNewTabForCertainAssert(planFactConversion, date, OM_NAME);
    }

    @Test(groups = {"R-8.0.1", "release", "TEST-76"}, description = "Открытие вкладки Численность персонала")
    private void openNumberOfStuffTab() {
        goToReports();
        TypeOfReports numberOfStaff = TypeOfReports.NUMBER_OF_STAFF;
        chooseReportType(numberOfStaff);
        openCertainTabAssert(numberOfStaff);
    }

    @Test(groups = {"R-8.1", "TEST-76", "not actual"}, description = "Просмотр Численности персонала")
    private void checkNumberOfStuffTab() {
        goToReports();
        TypeOfReports numberOfStaff = TypeOfReports.NUMBER_OF_STAFF;
        chooseReportType(numberOfStaff);
        certainOm(OM_NAME);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        openNewTabForCertainAssert(numberOfStaff, date, OM_NAME);
    }

    @Test(groups = {"R-9.0.1", "TEST-78"}, description = "Открытие вкладки \"Табель учёта рабочего времени\"")
    private void openingTheTimeSheetTab() {
        goToReports();
        TypeOfReports typeOfReports = TypeOfReports.TIME_SHEET;
        chooseReportType(typeOfReports);
        assertForGoingToReportPage(typeOfReports);
    }

    @Test(groups = {"R-9.1.1", "TEST-78"}, description = "Просмотр табеля учета рабочего времени (xlsx)")
    private void viewTimeSheetXLSX() {
        goToReports();
        TypeOfReports type = TypeOfReports.TIME_SHEET;
        TypeOfFiles typeDownload = TypeOfFiles.XLSX;
        chooseReportType(type);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getOrgUnitTypeByLevel(1));
        selectOmInSearch(orgUnit);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(Role.ADMIN, type,
                                                                                Collections.singletonList(orgUnit), date, typeDownload);
        pickMonthForReport(date);
        chooseDownloadFormat(typeDownload);
        checkReportDownloading(checker, date, Role.ADMIN);
    }

    @Test(groups = {"R-9.1.2", "TEST-78"}, description = "Просмотр табеля учета рабочего времени (csv)")
    private void viewTimeSheetCSV() {
        goToReports();
        TypeOfReports type = TypeOfReports.TIME_SHEET;
        TypeOfFiles typeDownload = TypeOfFiles.CSV;
        chooseReportType(type);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getHighest());
        selectOmInSearch(orgUnit);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(Role.ADMIN, type,
                                                                                Collections.singletonList(orgUnit), date, typeDownload);
        pickMonthForReport(date);
        chooseDownloadFormat(typeDownload);
        checkReportDownloading(checker, date, Role.ADMIN);
    }

    @Test(groups = {"R-9.1.3", "TEST-78"}, description = "Просмотр табеля учета рабочего времени (zip)")
    private void viewTimeSheetZIP() {
        goToReports();
        TypeOfReports type = TypeOfReports.TIME_SHEET;
        TypeOfFiles typeDownload = TypeOfFiles.ZIP;
        chooseReportType(type);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getOrgUnitTypeByLevel(1));
        selectOmInSearch(orgUnit);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(Role.ADMIN, type,
                                                                                Collections.singletonList(orgUnit), date, typeDownload);
        pickMonthForReport(date);
        chooseDownloadFormat(typeDownload);
        checkReportDownloading(checker, date, Role.ADMIN);
    }

    @Test(groups = {"R-9.1.4", "TEST-78"}, description = "Просмотр табеля учета рабочего времени (csv-grid)")
    private void viewTimeSheetCSVGrid() {
        goToReports();
        TypeOfReports type = TypeOfReports.TIME_SHEET;
        TypeOfFiles typeDownload = TypeOfFiles.CSV_GRID;
        chooseReportType(type);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getOrgUnitTypeByLevel(1));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(Role.ADMIN, type,
                                                                                Collections.singletonList(orgUnit), date, typeDownload);
        pickMonthForReport(date);
        chooseDownloadFormat(typeDownload);
        checkReportDownloading(checker, date, Role.ADMIN);
    }

    @Test(groups = {"R-10.0.1", "TEST-78"}, description = "Открытие вкладки \"Значения используемых параметров\"")
    private void openingTheTabValuesOfUsedParameters() {
        goToReports();
        TypeOfReports type = TypeOfReports.VALUES_OF_PARAMETERS;
        chooseReportType(type);
        assertForGoingToReportPage(type);
    }

    @Test(groups = {"R-10.1", "TEST-78", "not actual"}, description = "Просмотр значения используемых параметров")
    private void viewValuesOfParameters() {
        goToReports();
        TypeOfReports type = TypeOfReports.VALUES_OF_PARAMETERS;
        chooseReportType(type);
        certainOm(OM_NAME);
        checkReportClick();
        assertForValuesOfParameters(OM_NAME);
    }

    @Test(groups = {"R-11.1", "TEST-79"}, description = "Просмотр Целевой численности")
    private void viewTargetNumbers() {
        goToReports();
        TypeOfReports targetNumber = TypeOfReports.TARGET_NUMBER;
        chooseReportType(targetNumber);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        List<OrgUnit> orgUnitList = Collections.singletonList(orgUnit);
        chooseCertainOmFromTreeInTargetNumber(orgUnitList);
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM_PAST, LocalDateTools.FIRST);
        LocalDate endDate = LocalDateTools.getLastDate();
        DateInterval dates = new DateInterval(startDate, endDate);
        pickStartMonthForTargetNumber(startDate);
        pickEndMonthForTargetNumber(endDate);
        downloadButtonClick();
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.ADMIN, orgUnitList, dates, TypeOfFiles.XLSX, TypeOfReports.TARGET_NUMBER
        );
        checkReportDownloading(checker, dates, TypeOfAcceptContent.PDF_XLSX);
    }

    @Test(groups = {"R-11.0.1", "TEST-79"}, description = "Открытие вкладки \"Целевая численность\"")
    private void goToTargetNumberPage() {

        goToReports();
        TypeOfReports targetNumber = TypeOfReports.TARGET_NUMBER;
        chooseReportType(targetNumber);
        assertGoToTargetNumberPage();
    }

    @Test(groups = {"TK2046-1-1", "TEST-908"}, description = "Проверить доступ к просмотру модуля \"Отчеты\" с доступом")
    private void checkGoToReports() {
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST).getPage(SECTION);
    }

    @Test(groups = {"TK2046-1-2", "TEST-908"}, description = "Проверить доступ к просмотру модуля \"Отчеты\" без доступа",
            expectedExceptions = {WaitUntilException.class})
    private void checkGoToReportsWithOutPermission() {
        new RoleWithCookies(rp.getWrappedDriver(), Role.SECOND).getPage(SECTION);
        chooseReportType(TypeOfReports.NUMBER_OF_GRAPHS);
    }

    @Test(dataProvider = "RolesWithoutPermissions", groups = {"TK2046", "TEST-908"},
            description = "Проверка отображения отчетов")
    private void checkReportMatches(Role role, List<String> expectedReports) {
        new RoleWithCookies(rp.getWrappedDriver(), role).getPage(SECTION);
        checkReportsMatches(expectedReports);
    }

    @Test(groups = {"TK2046", G1, REP1},
            description = "Просмотр смен (пользователь)",
            dataProvider = "download shift reports")
    @Link(name = "Статья: \"Отчеты\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979898#id-%D0%9E%D1%82%D1%87%D0%B5%D1%82%D1%8B-R-1.1.1")
    @TmsLink("60879")
    @Tag(REP1)
    @Severity(SeverityLevel.NORMAL)
    private void downloadShiftWithPermissions(Role role, TypeOfFiles fileType, String tag) {
        addTag(tag);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), role, unit).getPage(SECTION);
        TypeOfReports shifts = TypeOfReports.SHIFTS;
        chooseReportType(shifts);
        selectOmInSearch(unit);
        LocalDate localDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                role, shifts, Collections.singletonList(unit), localDate, fileType);
        pickMonthForReport(localDate);
        chooseDownloadFormat(fileType);
        checkReportDownloading(checker, localDate, role);
    }

    @Test(groups = {"TK2046-7-1", "TEST-908", "not actual"},
            description = "Просмотр численности по графикам, 1 роль")
    private void numberOfGraphsFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getOrgUnitTypeByLevel(2));
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        TypeOfReports numberOfGraphs = TypeOfReports.NUMBER_OF_GRAPHS;
        chooseReportType(numberOfGraphs);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        pickMonthForReport(date);
        numberOfGraphsClick();
        checkGoToPage(date, TypeOfReports.NUMBER_OF_GRAPHS);
    }

    @Test(groups = {"TK2046-7-5", "TEST-908", "not actual"},
            description = "Просмотр численности по графикам, 5 роль")
    private void numberOfGraphsFifthRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getOrgUnitTypeByLevel(2));
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIFTH, unit).getPage(SECTION);
        TypeOfReports numberOfGraphs = TypeOfReports.NUMBER_OF_GRAPHS;
        chooseReportType(numberOfGraphs);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        pickMonthForReport(date);
        numberOfGraphsClick();
        checkGoToPage(date, TypeOfReports.NUMBER_OF_GRAPHS);
    }

    @Test(groups = {"TK2046-9", G1, REP1},
            dataProvider = "roles 1, 6", dataProviderClass = DataProviders.class,
            description = "Просмотр статуса публикации графиков")
    @Link(name = "Статья: \"2046_Применение роли в системе блок \"Отчеты\"\"", url = "http://wiki.goodt.me/pages/viewpage.action?pageId=197460832")
    @TmsLink("60879")
    @Tag(REP1)
    @Tag("TK2046-9")
    @Severity(SeverityLevel.NORMAL)
    private void viewSchedulePublication(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        new RoleWithCookies(rp.getWrappedDriver(), role, unit).getPage(SECTION);
        TypeOfReports numberOfGraphs = TypeOfReports.PUBLICATION_STATUS;
        chooseReportType(numberOfGraphs);
        sendOmNameAndCertainOmFromList(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        pickMonthForReport(date);
        publicationStatusClick();
        checkGoToPage(date, TypeOfReports.PUBLICATION_STATUS);
    }

    @Test(groups = {"TK2046-10-1", "TEST-908"},
            description = "Переход в режим \"Расписание\" из результата статуса публикации графиков, 1 роль")
    private void goToScheduleViaReportsFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getOrgUnitTypeByLevel(2));
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.PUBLICATION_STATUS);
        sendOmNameAndCertainOmFromList(unit.getName());
        pickMonthForReport(LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        publicationStatusClick();
        removeFirstWindowHandler();
        String orgUnitName = unit.getName();
        clickOnSubdivisionInPublishingTable(orgUnitName);
        checkScheduleTable();
        assertGoToOrgUnit(orgUnitName);
    }

    @Test(groups = {"TK2046-10-6", "TEST-908"},
            description = "Переход в режим \"Расписание\" из результата статуса публикации графиков, 6 роль")
    private void goToScheduleViaReportsSixthRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getOrgUnitTypeByLevel(2));
        new RoleWithCookies(rp.getWrappedDriver(), Role.SIXTH, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.PUBLICATION_STATUS);
        sendOmNameAndCertainOmFromList(unit.getName());
        pickMonthForReport(LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        publicationStatusClick();
        removeFirstWindowHandler();
        String orgUnitName = unit.getName();
        clickOnSubdivisionInPublishingTable(orgUnitName);
        checkScheduleTable();
        assertGoToOrgUnit(orgUnitName);
    }

    @Test(groups = {"TK2046-12-1", "TEST-908"},
            description = "Просмотр \"Качества исторических данных\", 1 роль")
    private void historicalDataQualityFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        TypeOfReports qualityHistoricalData = TypeOfReports.QUALITY_HISTORICAL_DATA;
        chooseReportType(qualityHistoricalData);
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.FIRST, qualityHistoricalData, desiredOm, TypeOfFiles.CSV
        );
        pressDownloadCsvReport();
        assertDownloadSimpleFile(checker);
    }

    @Test(groups = {"TK2046-12-7", "TEST-908"},
            description = "Просмотр \"Качества исторических данных\", 7 роль")
    private void historicalDataQualitySeventhRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.SEVENTH, unit).getPage(SECTION);
        TypeOfReports qualityHistoricalData = TypeOfReports.QUALITY_HISTORICAL_DATA;
        chooseReportType(qualityHistoricalData);
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.SEVENTH, qualityHistoricalData, desiredOm, TypeOfFiles.CSV
        );
        pressDownloadCsvReport();
        assertDownloadSimpleFile(checker);
    }

    @Test(groups = {"TK2046-14-1", "TEST-908", "not actual"},
            description = "Просмотр \"Квоты выходных дней\", 1 роль")
    private void holidayQuotasFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        TypeOfReports holidayQuotas = TypeOfReports.HOLIDAY_QUOTAS;
        chooseReportType(holidayQuotas);
        String orgUnitName = unit.getName();
        certainOm(orgUnitName);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        checkGoToPage(TypeOfReports.HOLIDAY_QUOTAS, orgUnitName);
    }

    @Test(groups = {"TK2046-14-8", "TEST-908", "not actual"},
            description = "Просмотр \"Квоты выходных дней\", 8 роль")
    private void holidayQuotasEighthRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.EIGHTH, unit).getPage(SECTION);
        TypeOfReports holidayQuotas = TypeOfReports.HOLIDAY_QUOTAS;
        chooseReportType(holidayQuotas);
        String orgUnitName = unit.getName();
        certainOm(orgUnitName);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        checkGoToPage(TypeOfReports.HOLIDAY_QUOTAS, orgUnitName);
    }

    @Test(groups = {"TK2046-16-1", "TEST-908", "not actual"},
            description = "Просмотр \"Анализа средней конверсии\", 1 роль")
    private void averageConversionFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        TypeOfReports holidayQuotas = TypeOfReports.AVERAGE_CONVERSION;
        chooseReportType(holidayQuotas);
        String orgUnitName = unit.getName();
        certainOm(orgUnitName);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        checkGoToPage(TypeOfReports.AVERAGE_CONVERSION, orgUnitName);
    }

    @Test(groups = {"TK2046-16-9", "TEST-908", "not actual"},
            description = "Просмотр \"Анализа средней конверсии\", 9 роль")
    private void averageConversionNinthRole() {
        new RoleWithCookies(rp.getWrappedDriver(), Role.NINTH).getPage(SECTION);
        TypeOfReports holidayQuotas = TypeOfReports.AVERAGE_CONVERSION;
        chooseReportType(holidayQuotas);
        String orgUnitName = OrgUnitRepository.getRandomOrgUnit().getName();
        certainOm(orgUnitName);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        checkGoToPage(TypeOfReports.AVERAGE_CONVERSION, orgUnitName);
    }

    @Test(groups = {"TK2046-17-1", "TEST-908"},
            description = "Изменение масштаба отображения результата анализа средней конверсии через вкладку \"День\", 1 роль")
    private void averageConversionFirstRoleChangeScopeDay() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.DAY);
        assertDateForTableScopeDay();
    }

    @Test(groups = {"TK2046-17-9", "TEST-908"},
            description = "Изменение масштаба отображения результата анализа средней конверсии через вкладку \"День\", 9 роль")
    private void averageConversionNinthRoleChangeScopeDay() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.NINTH, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.DAY);
        assertDateForTableScopeDay();
    }

    @Test(groups = {"TK2046-18-1", "TEST-908"},
            description = "Изменение масштаба отображения результата анализа средней конверсии через вкладку \"Месяц\", 1 роль")
    private void averageConversionFirstRoleChangeScopeMonth() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.MONTH);
        assertDateForTableScopeMonth();
    }

    @Test(groups = {"TK2046-18-9", "TEST-908"},
            description = "Изменение масштаба отображения результата анализа средней конверсии через вкладку \"Месяц\", 9 роль")
    private void averageConversionNinthRoleChangeScopeMonth() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.NINTH, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.MONTH);
        assertDateForTableScopeMonth();
    }

    @Test(groups = {"TK2046-19-1", "TEST-908"},
            description = "Изменение масштаба отображения результата анализа средней конверсии через вкладку \"Год\", 1 роль")
    private void averageConversionFirstRoleChangeScopeYear() {
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(OrgUnitRepository.getRandomOrgUnit().getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.DAY);
        assertDateForTableScopeYear();
    }

    @Test(groups = {"TK2046-19-9", "TEST-908"},
            description = "Изменение масштаба отображения результата анализа средней конверсии через вкладку \"Год\", 9 роль")
    private void averageConversionNinthRoleChangeScopeYear() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.NINTH, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.YEAR);
        assertDateForTableScopeYear();
    }

    @Test(groups = {"TK2046-20-1", "TEST-908"},
            description = "Переключение временного периода отображения результата анализа средней конверсии в режиме \"Год\", 1 роль")
    private void checkAverageConversionTabDirectionSwitchYearFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(unit.getName());
        checkReportClick();
        tableSwitch(TableDirection.RIGHT);
        tableSwitch(TableDirection.LEFT);
        tableSwitch(TableDirection.LEFT);
    }

    @Test(groups = {"TK2046-20-9", "TEST-908"},
            description = "Переключение временного периода отображения результата анализа средней конверсии в режиме \"Год\", 9 роль")
    private void checkAverageConversionTabDirectionSwitchYearNinthRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.NINTH, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(unit.getName());
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.MONTH);
        tableSwitch(TableDirection.RIGHT);
        tableSwitch(TableDirection.LEFT);
        tableSwitch(TableDirection.LEFT);
    }

    @Test(groups = {"TK2046-21-1", "TEST-908"},
            description = "Переключение временного периода отображения результата анализа средней конверсии в режиме \"Месяц\", 1 роль")
    private void checkAverageConversionTabDirectionSwitchMonthFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(unit.getName());
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.MONTH);
        tableSwitch(TableDirection.RIGHT);
        tableSwitch(TableDirection.LEFT);
        tableSwitch(TableDirection.LEFT);
    }

    @Test(groups = {"TK2046-21-9", "TEST-908"},
            description = "Переключение временного периода отображения результата анализа средней конверсии в режиме \"Месяц\", 9 роль")
    private void checkAverageConversionTabDirectionSwitchMonthNinthRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.NINTH, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(unit.getName());
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.MONTH);
        tableSwitch(TableDirection.RIGHT);
        tableSwitch(TableDirection.LEFT);
        tableSwitch(TableDirection.LEFT);
    }

    @Test(groups = {"TK2046-22-1", "TEST-908"},
            description = "Переключение временного периода отображения результата анализа средней конверсии в режиме \"День\", 1 роль")
    private void checkAverageConversionTabDirectionSwitchDayFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(unit.getName());
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.DAY);
        tableSwitch(TableDirection.RIGHT);
        tableSwitch(TableDirection.LEFT);
        tableSwitch(TableDirection.LEFT);
    }

    @Test(groups = {"TK2046-22-9", "TEST-908"},
            description = "Переключение временного периода отображения результата анализа средней конверсии в режиме \"День\", 9 роль")
    private void checkAverageConversionTabDirectionSwitchDayNinthRole() {
        new RoleWithCookies(rp.getWrappedDriver(), Role.NINTH).getPage(SECTION);
        chooseReportType(TypeOfReports.AVERAGE_CONVERSION);
        certainOm(OrgUnitRepository.getRandomOrgUnit().getName());
        checkReportClick();
        scopeSwitchForTab(ScopeReportTab.DAY);
        tableSwitch(TableDirection.RIGHT);
        tableSwitch(TableDirection.LEFT);
        tableSwitch(TableDirection.LEFT);
    }

    @Test(groups = {"TK2046-24-1", "TEST-908", "not actual"},
            description = "Просмотр \"Плановой и фактической конверсии\", 1 роль")
    private void planFactConversionFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        TypeOfReports planFactConversion = TypeOfReports.PLAN_FACT_CONVERSION;
        chooseReportType(planFactConversion);
        certainOm(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkGoToPage(TypeOfReports.AVERAGE_CONVERSION, unit.getName());
    }

    @Test(groups = {"TK2046-24-10", "TEST-908", "not actual"},
            description = "Просмотр \"Плановой и фактической конверсии\", 10 роль")
    private void planFactConversionTenthRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.TENTH, unit).getPage(SECTION);
        TypeOfReports planFactConversion = TypeOfReports.PLAN_FACT_CONVERSION;
        chooseReportType(planFactConversion);
        certainOm(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkGoToPage(TypeOfReports.AVERAGE_CONVERSION, unit.getName());
    }

    @Test(groups = {"TK2046-26-1", "TEST-908", "not actual"},
            description = "Просмотр \"Численность персонала\", 1 роль")
    private void staffNumberFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        TypeOfReports planFactConversion = TypeOfReports.NUMBER_OF_STAFF;
        chooseReportType(planFactConversion);
        certainOm(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkGoToPage(TypeOfReports.NUMBER_OF_STAFF, unit.getName());
    }

    @Test(groups = {"TK2046-26-11", "TEST-908", "not actual"},
            description = "Просмотр \"Численность персонала\", 11 роль")
    private void staffNumberEleventhRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.ELEVENTH, unit).getPage(SECTION);
        TypeOfReports planFactConversion = TypeOfReports.NUMBER_OF_STAFF;
        chooseReportType(planFactConversion);
        certainOm(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkGoToPage(TypeOfReports.NUMBER_OF_STAFF, unit.getName());
    }

    @Test(groups = {"TK2046-28-1", "TEST-908"},
            description = "Просмотр табеля учета рабочего времени (xlsx) 1 роль")
    private void downloadTimeSheetXlSXFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST).getPage(SECTION);
        TypeOfReports shifts = TypeOfReports.TIME_SHEET;
        chooseReportType(shifts);
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        LocalDate localDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.FIRST, shifts, desiredOm, localDate, TypeOfFiles.XLSX);
        pickMonthForReport(localDate);
        chooseDownloadFormat(TypeOfFiles.XLSX);
        checkReportDownloading(checker, localDate, Role.FIRST);
    }

    @Test(groups = {"TK2046-28-12", "TEST-908"},
            description = "Просмотр табеля учета рабочего времени (xlsx) 12 роль")
    private void downloadTimeSheetXlSXTwelfthRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.TWELFTH, unit).getPage(SECTION);
        TypeOfReports shifts = TypeOfReports.TIME_SHEET;
        chooseReportType(shifts);
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        LocalDate localDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.TWELFTH, shifts, desiredOm, localDate, TypeOfFiles.XLSX);
        pickMonthForReport(localDate);
        chooseDownloadFormat(TypeOfFiles.XLSX);
        checkReportDownloading(checker, localDate, Role.TWELFTH);
    }

    @Test(groups = {"TK2046-29-1", "TEST-908"},
            description = "Просмотр табеля учета рабочего времени (csv) 1 роль")
    private void downloadTimeSheetCSVFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        TypeOfReports shifts = TypeOfReports.TIME_SHEET;
        chooseReportType(shifts);
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        LocalDate localDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.FIRST, shifts, desiredOm, localDate, TypeOfFiles.CSV);
        pickMonthForReport(localDate);
        chooseDownloadFormat(TypeOfFiles.CSV);
        checkReportDownloading(checker, localDate, Role.FIRST);
    }

    @Test(groups = {"TK2046-29-12", "TEST-908"},
            description = "Просмотр табеля учета рабочего времени (csv) 12 роль")
    private void downloadTimeSheetCsvTwelfthRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.TWELFTH, unit).getPage(SECTION);
        TypeOfReports shifts = TypeOfReports.TIME_SHEET;
        chooseReportType(shifts);
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        LocalDate localDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.TWELFTH, shifts, desiredOm, localDate, TypeOfFiles.CSV);
        pickMonthForReport(localDate);
        chooseDownloadFormat(TypeOfFiles.CSV);
        checkReportDownloading(checker, localDate, Role.TWELFTH);
    }

    @Test(groups = {"TK2046-30-1", "TEST-908"},
            description = "Просмотр табеля учета рабочего времени (zip) 1 роль")
    private void downloadTimeSheetZIPFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        TypeOfReports shifts = TypeOfReports.TIME_SHEET;
        chooseReportType(shifts);
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        LocalDate localDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.FIRST, shifts, desiredOm, localDate, TypeOfFiles.ZIP);
        pickMonthForReport(localDate);
        chooseDownloadFormat(TypeOfFiles.ZIP);
        checkReportDownloading(checker, localDate, Role.FIRST);
    }

    @Test(groups = {"TK2046-30-12", "TEST-908"},
            description = "Просмотр табеля учета рабочего времени (zip) 12 роль")
    private void downloadTimeSheetZIPTwelfthRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.TWELFTH, unit).getPage(SECTION);
        TypeOfReports shifts = TypeOfReports.TIME_SHEET;
        chooseReportType(shifts);
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        selectOmInSearch(unit);
        LocalDate localDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.TWELFTH, shifts, desiredOm, localDate, TypeOfFiles.ZIP);
        pickMonthForReport(localDate);
        chooseDownloadFormat(TypeOfFiles.ZIP);
        checkReportDownloading(checker, localDate, Role.TWELFTH);
    }

    @Test(groups = {"TK2046-33-1", "TEST-908", "not actual"},
            description = "Просмотр \"Просмотр значения используемых параметров\", 1 роль")
    private void parametersValuesFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        TypeOfReports planFactConversion = TypeOfReports.PLAN_FACT_CONVERSION;
        chooseReportType(planFactConversion);
        certainOm(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkGoToPage(TypeOfReports.VALUES_OF_PARAMETERS, unit.getName());
    }

    @Test(groups = {"TK2046-33-14", "TEST-908", "not actual"},
            description = "Просмотр \"Просмотр значения используемых параметров\", 14 роль")
    private void parametersValuesFourteenthRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        new RoleWithCookies(rp.getWrappedDriver(), Role.FOURTEENTH, unit).getPage(SECTION);
        TypeOfReports planFactConversion = TypeOfReports.VALUES_OF_PARAMETERS;
        chooseReportType(planFactConversion);
        certainOm(unit.getName());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST);
        pickMonthForReport(date);
        checkGoToPage(TypeOfReports.VALUES_OF_PARAMETERS, unit.getName());
    }

    @Test(groups = {"TK2046-35-1", "TEST-908"},
            description = "Просмотр \"Просмотр Целевой численности\", 1 роль")
    private void dataForCalculationFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomStoreWithBusinessHours(ScheduleType.ANY_TYPE);
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        TypeOfReports targetNumber = TypeOfReports.TARGET_NUMBER;
        chooseReportType(targetNumber);
        List<OrgUnit> orgUnitList = Collections.singletonList(unit);
        sendOmNameAndCertainOmFromList(unit.getName());
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM_PAST, LocalDateTools.FIRST);
        LocalDate endDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM_PAST, LocalDateTools.LAST);
        DateInterval dates = new DateInterval(startDate, endDate);
        pickEndMonthForTargetNumber(startDate);
        pickStartMonthForTargetNumber(endDate);
        downloadButtonClick();
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.FIRST, orgUnitList, dates, TypeOfFiles.XLSX, TypeOfReports.TARGET_NUMBER
        );
        checkReportDownloading(checker, dates, TypeOfAcceptContent.PDF_XLSX);
    }

    @Test(groups = {"TK2046-35-13", "TEST-908"},
            description = "Просмотр \"Просмотр Целевой численности\", 13 роль")
    private void dataForCalculationThirteenthRole() {
        OrgUnit unit = OrgUnitRepository.getRandomStoreWithBusinessHours(ScheduleType.ANY_TYPE);
        new RoleWithCookies(rp.getWrappedDriver(), Role.THIRTEENTH, unit).getPage(SECTION);
        TypeOfReports targetNumber = TypeOfReports.TARGET_NUMBER;
        chooseReportType(targetNumber);
        List<OrgUnit> orgUnitList = Collections.singletonList(unit);
        sendOmNameAndCertainOmFromList(unit.getName());
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM_PAST, LocalDateTools.FIRST);
        LocalDate endDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM_PAST, LocalDateTools.LAST);
        DateInterval dates = new DateInterval(startDate, endDate);
        pickEndMonthForTargetNumber(startDate);
        pickStartMonthForTargetNumber(endDate);
        downloadButtonClick();
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.THIRTEENTH, orgUnitList, dates, TypeOfFiles.XLSX, TypeOfReports.TARGET_NUMBER
        );
        checkReportDownloading(checker, dates, TypeOfAcceptContent.PDF_XLSX);
    }

    @Test(groups = {"TK2831.1", "TEST-1061"},
            description = "Выгрузка отчета\"Посещаемость\" для одного подразделения, 1 роль")
    private void reportAttendanceSingleFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnit(OrganizationUnitTypeId.getLowest());
        List<OrgUnit> desiredOm = Collections.singletonList(unit);
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, unit).getPage(SECTION);
        TypeOfReports attendance = TypeOfReports.ATTENDANCE;
        chooseReportType(attendance);
        selectOmInSearch(unit);
        LocalDate startDate = LocalDate.now().minusDays(new Random().nextInt(28) + 2);
        LocalDate endDate = startDate.plusDays(1);
        DateInterval dates = new DateInterval(startDate, endDate);
        pickMonthForAttendance(startDate, DateTypeField.START_DATE);
        pickMonthForAttendance(endDate, DateTypeField.END_DATE);
        downloadButtonClick();
        FileDownloadCheckerForReport checker =
                new FileDownloadCheckerForReport(Role.FIRST, desiredOm, dates, TypeOfFiles.CSV, attendance);
        assertDownloadSimpleFile(checker);
    }

    @Test(groups = {"TK2831.2", "TEST-1061"},
            description = "Выгрузка отчета\"Посещаемость\" для нескольких подразделений, 1 роль")
    private void reportAttendanceSeveralFirstRole() {
        List<OrgUnit> orgUnits = OrgUnitRepository.getRandomOrgUnits(OrganizationUnitTypeId.getLowest(), 3);
        new RoleWithCookies(rp.getWrappedDriver(), Role.FIRST, orgUnits.get(0), orgUnits.get(1), orgUnits.get(2))
                .getPage(SECTION);
        TypeOfReports attendance = TypeOfReports.ATTENDANCE;
        chooseReportType(attendance);
        selectOmInSearch(orgUnits);
        LocalDate startDate = LocalDate.now().minusDays(new Random().nextInt(28) + 2);
        LocalDate endDate = startDate.plusDays(1);
        DateInterval dates = new DateInterval(startDate, endDate);
        pickMonthForAttendance(startDate, DateTypeField.START_DATE);
        pickMonthForAttendance(endDate, DateTypeField.END_DATE);
        downloadButtonClick();
        FileDownloadCheckerForReport checker =
                new FileDownloadCheckerForReport(Role.FIRST, orgUnits, dates, TypeOfFiles.CSV, attendance);
        assertDownloadSimpleFile(checker);
    }

    @Test(groups = {"TK2831.3", "TEST-1061"},
            description = "Ошибка при выгрузке отчета \"Посещаемость\" без полномочий")
    private void reportAttendanceSeveralErrorRole() {
        new RoleWithCookies(rp.getWrappedDriver(), Role.THIRD).getPage(SECTION);
        checkForReportAbsent(TypeOfReports.ATTENDANCE);
    }

    @Test(groups = {"TK-2811", "TEST-1173"}, description = "Выгрузка технического табеля")
    private void technicalTableUnloading() {
        goToReports();
        TypeOfReports technicalTableUnloading = TypeOfReports.TECHNICAL_TABLE_UNLOADING;
        chooseReportType(technicalTableUnloading);
        OrgUnit orgUnit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        certainOm(orgUnit.getName());
        downloadXSLXLeftButtonClick();
        LocalDate date = LocalDate.now();
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(Role.ADMIN,
                                                                                technicalTableUnloading, Collections.singletonList(orgUnit), date, TypeOfFiles.XLSX);
        checkReportDownloading(checker, date, Role.ADMIN);
    }

    @Test(groups = {"ABCHR2873-1", "TEST-1164"}, description = "Отчет факт работы сотрудника")
    private void employeeWorkFactReport() {
        goToReports();
        TypeOfReports workingFact = TypeOfReports.EMPLOYEE_WORKING_FACT;
        chooseReportType(workingFact);
        OrgUnit orgUnit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        sendOmNameAndCertainOmFromList(orgUnit.getName());
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM_PAST, LocalDateTools.FIRST);
        LocalDate endDate = startDate.plusDays(new Random().nextInt(30) + 5);
        DateInterval dates = new DateInterval(startDate, endDate);
        pickMonthForAttendance(startDate, DateTypeField.START_DATE);
        pickMonthForAttendance(endDate, DateTypeField.END_DATE);
        downloadButtonClick();
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(
                Role.ADMIN, Collections.singletonList(orgUnit), dates, TypeOfFiles.XLSX, workingFact
        );
        checkReportDownloading(checker, dates, TypeOfAcceptContent.PDF_XLSX);
    }

    @Test(groups = {"ABCHR2873-2", "TEST-1164"},
            description = "Отчет факт работы сотрудника с выбором даты окончания раньше даты начала")
    private void employeeWorkFactReportDateEndBeforeDateStart() {
        goToReports();
        TypeOfReports workingFact = TypeOfReports.EMPLOYEE_WORKING_FACT;
        chooseReportType(workingFact);
        OrgUnit orgUnit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        sendOmNameAndCertainOmFromList(orgUnit.getName());
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM_PAST, LocalDateTools.FIRST);
        LocalDate endDate = startDate.minusDays(new Random().nextInt(30) + 5);
        pickMonthForAttendance(startDate, DateTypeField.START_DATE);
        pickMonthForAttendance(endDate, DateTypeField.END_DATE);
        downloadButtonClick();
        checkDateEndErrorException(startDate);
    }

    @Test(groups = {"ABCHR3015-1", "TEST-1296"},
            description = "Отчет по сменам для внешних сотрудников")
    private void shiftReportForExternalEmployees() {
        goToReports();
        TypeOfReports shiftsExternalEmployee = TypeOfReports.SHIFTS_EXTERNAL_EMPLOYEE;
        chooseReportType(shiftsExternalEmployee);
        OrgUnit orgUnit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        sendOmNameAndCertainOmFromList(orgUnit.getName());
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.THAT);
        LocalDate endDate = startDate.plusDays(new Random().nextInt(30) + 5);
        pickMonthForAttendance(startDate, DateTypeField.START_DATE);
        pickMonthForAttendance(endDate, DateTypeField.END_DATE);
        downloadButtonClick();
        DateInterval dateInterval = new DateInterval(startDate, endDate);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(Role.ADMIN,
                                                                                Collections.singletonList(orgUnit), dateInterval, TypeOfFiles.XLSX, shiftsExternalEmployee
        );
        assertDownloadSimpleFile(checker);
    }

    @Test(groups = {"ABCHR3015-2", "TEST-1296"},
            description = "Выбор ОМ через строку поиска в отчете по сменам для внешних сотрудников")
    private void shiftReportForExternalEmployeesSearchInput() {
        goToReports();
        TypeOfReports shiftsExternalEmployee = TypeOfReports.SHIFTS_EXTERNAL_EMPLOYEE;
        chooseReportType(shiftsExternalEmployee);
        OrgUnit orgUnit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        chooseCertainOmFromTreeInTargetNumber(Collections.singletonList(orgUnit));
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.THAT);
        LocalDate endDate = startDate.plusDays(new Random().nextInt(30) + 5);
        pickMonthForAttendance(startDate, DateTypeField.START_DATE);
        pickMonthForAttendance(endDate, DateTypeField.END_DATE);
        downloadButtonClick();
        DateInterval dateInterval = new DateInterval(startDate, endDate);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(Role.ADMIN,
                                                                                Collections.singletonList(orgUnit), dateInterval, TypeOfFiles.XLSX, shiftsExternalEmployee
        );
        assertDownloadSimpleFile(checker);
    }

    @Test(groups = {"ABCHR3015-3", "TEST-1296"},
            description = "Очистка строки поиска в отчете по сменам для внешних сотрудников")
    private void shiftReportForExternalEmployeesClearInput() {
        goToReports();
        TypeOfReports workingFact = TypeOfReports.SHIFTS_EXTERNAL_EMPLOYEE;
        chooseReportType(workingFact);
        OrgUnit orgUnit = OrgUnitRepository.getRandomActualOrgUnit(OrganizationUnitTypeId.getLowest());
        sendPartNameInSearchField(orgUnit.getName());
        pressCleanButton();
        assertCleanInputSearch();
    }

    @Test(groups = {"ABCHR3015-4", "TEST-1296"},
            description = "Выбор ОМ по тегам в отчете по сменам для внешних сотрудников")
    private void shiftReportForExternalEmployeesByTag() {
        goToReports();
        TypeOfReports shiftsExternalEmployee = TypeOfReports.SHIFTS_EXTERNAL_EMPLOYEE;
        chooseReportType(shiftsExternalEmployee);
        ImmutablePair<String, String> randomTagFromApi = CommonRepository.getRandomTagFromApi();
        OrgUnit orgUnit = OrgUnitRepository.getRandomStoreUnitByTags(Collections.singletonList(randomTagFromApi.getRight()));
        pressFindByTagButton();
        pressTagCheckBoxes(Collections.singletonList(randomTagFromApi.getLeft()));
        clickSaveTags();
        certainOmFromList(orgUnit.getName());
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.THAT);
        LocalDate endDate = startDate.plusDays(new Random().nextInt(30) + 5);
        pickMonthForAttendance(startDate, DateTypeField.START_DATE);
        pickMonthForAttendance(endDate, DateTypeField.END_DATE);
        downloadButtonClick();
        DateInterval dateInterval = new DateInterval(startDate, endDate);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(Role.ADMIN,
                                                                                Collections.singletonList(orgUnit), dateInterval, TypeOfFiles.XLSX, shiftsExternalEmployee
        );
        assertDownloadSimpleFile(checker);
    }

    @Test(groups = {"ABCHR3015-5", "TEST-1296"},
            description = "Сброс поиска по тегам в отчете по сменам для внешних сотрудников")
    private void shiftReportForExternalEmployeesByCleanTag() {
        goToReports();
        TypeOfReports shiftsExternalEmployee = TypeOfReports.SHIFTS_EXTERNAL_EMPLOYEE;
        chooseReportType(shiftsExternalEmployee);
        ImmutablePair<String, String> randomTagFromApi = CommonRepository.getRandomTagFromApi();
        pressFindByTagButton();
        pressTagCheckBoxes(Collections.singletonList(randomTagFromApi.getLeft()));
        clickResetTags();
        assertCleanInputSearch();
    }

    @Step("Перейти в раздел \"{SECTION.name}\", с ролью \"{role.name}\"")
    private void loginWithSetOfRolesSchedulePageOrgUnit(Role role, OrgUnit orgUnit) {
        new RoleWithCookies(rp.getWrappedDriver(), role, orgUnit).getPage(SECTION);
    }

    private String checkDownloadPage() {
        WebDriver driver = rp.getWrappedDriver();
        String windowHandler = driver.getWindowHandle();
        driver.getWindowHandles().
                forEach(System.out::println);

        String newWindowHandler = driver.getWindowHandles().stream().
                filter(w -> !w.equals(windowHandler)).findFirst().
                orElseThrow(() -> new AssertionError("Ошибка загрузки отчета"));
        return driver.switchTo().window(newWindowHandler).getCurrentUrl();
    }

    @Test(groups = {"ABCHR3984-2"},
            description = "Сотрудник с правом на выгрузку отчета по компетенциям может выгрузить отчет по компетенциям")
    @Link(name = "Ссылка на тест-кейс", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=214762480")
    @Owner(value = "a.bugorov")
    public void uploadReportOnCompetencies() {
        List<PermissionType> permissionCustomGeneratedTypes = new ArrayList<>(Arrays.asList(
                PermissionType.REPORT_VIEW
                //PermissionType.REPORT_COMPETENCE такого пермишна больше нет
        ));
        Role role = PresetClass.createCustomPermissionRole(permissionCustomGeneratedTypes);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        loginWithSetOfRolesSchedulePageOrgUnit(role, orgUnit);
        chooseReportType(TypeOfReports.REPORT_COMPETENCE);

        certainOmFromList(orgUnit.getName());
        LocalDate startDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM_PAST, LocalDateTools.FIRST);
        LocalDate endDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM_PAST, LocalDateTools.LAST);
        DateInterval dates = new DateInterval(startDate, endDate);

        Thread checkDownloadPageThread = new Thread(() -> System.out.println(checkDownloadPage()));
        checkDownloadPageThread.start();
        systemSleep(3); //тест неактуален
        downloadButtonClick();
        try {
            checkDownloadPageThread.join(6000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    //    @Test(groups = {"ABCHR3984", IN_PROGRESS},
    //            description = "Сотрудник с правом на выгрузку отчета по компетенциям может выгрузить отчет по компетенциям",
    //            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @TmsLink("TEST-1456")
    @Link(name = "Статья: \"3984_Добавить права на отчет по компетенциям для СБС\"", url = "https://wiki.goodt.me/x/8APND")
    /**
     * Функционал, необходимый для работы этого теста, запрошен, но пока не разрабатывается.
     * Тест выключен, чтобы он не тратил время и ресурсы во время ночных прогонов
     */
    public void downloadCompetenceReport(boolean hasAccess) {
        List<PermissionType> permissions = new ArrayList<>(Collections.singletonList(PermissionType.REPORT_VIEW));
        if (hasAccess) {
            //permissions.add(PermissionType.REPORT_COMPETENCE); такого пермишна больше нет
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        loginWithSetOfRolesSchedulePageOrgUnit(role, orgUnit);
        TypeOfReports type = TypeOfReports.REPORT_COMPETENCE;
        if (hasAccess) {
            chooseReportType(type);
            certainOmFromList(orgUnit.getName());
            FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(role, type, Collections.singletonList(orgUnit), TypeOfFiles.XLSX);
            downloadButtonClick();
            checkReportDownloading(checker, LocalDate.now(), role);
        } else {
            changeTestName("Сотрудник без права на выгрузку отчета по компетенциям не может выгрузить отчет по компетенциям");
            Assert.assertThrows(org.openqa.selenium.NoSuchElementException.class, () -> chooseReportType(type));
        }
    }
}