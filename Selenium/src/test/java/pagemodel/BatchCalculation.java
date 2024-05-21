package pagemodel;

import com.google.inject.Inject;
import guice.TestModule;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.BatchCalculationPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.*;
import utils.Links;
import utils.Projects;
import utils.downloading.FileDownloadCheckerForBatchCalculation;
import utils.downloading.TypeOfAcceptContent;
import utils.downloading.TypeOfBatch;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import wfm.PresetClass;
import wfm.components.analytics.KpiAlgorithm;
import wfm.components.analytics.KpiType;
import wfm.components.calculation.*;
import wfm.components.orgstructure.OrganizationUnitTypeId;
import wfm.components.schedule.SystemProperties;
import wfm.components.utils.DateTypeField;
import wfm.components.utils.Direction;
import wfm.components.utils.Role;
import wfm.components.utils.Section;
import wfm.models.*;
import wfm.repository.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static common.Groups.*;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.*;
import static wfm.repository.CommonRepository.URL_BASE;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class BatchCalculation extends BaseTest {

    private static final Section SECTION = Section.BATCH_CALCULATION;
    private static final String RELEASE_URL = Links.getTestProperty("release");
    private static final String URL_B = RELEASE_URL + SECTION.getUrlEnding();

    @Inject
    private BatchCalculationPage bc;

    @DataProvider(name = "calculationTypes")
    private static Object[][] orgUnitToCheckData() {
        Object[][] array = new Object[4][];
        array[0] = new Object[]{Role.THIRD, Collections.singletonList(CalculationType.KPI)};
        array[1] = new Object[]{Role.FOURTH, Collections.singletonList(CalculationType.FTE)};
        array[2] = new Object[]{Role.FIFTH, Collections.singletonList(CalculationType.SHIFT)};
        array[3] = new Object[]{Role.SIXTH, Collections.singletonList(CalculationType.TARGET_NUMBER)};
        return array;
    }

    @DataProvider(name = "typesWithRolesG1")
    private static Object[][] calculationTypesWithRolesG1() {
        Object[][] array = new Object[4][];
        array[0] = new Object[]{Role.FIRST, CalculationType.SHIFT, TypeOfBatch.SHIFTS};
        array[1] = new Object[]{Role.FIFTH, CalculationType.SHIFT, TypeOfBatch.SHIFTS};
        array[2] = new Object[]{Role.FIRST, CalculationType.TARGET_NUMBER, TypeOfBatch.TARGET_NUMBER};
        array[3] = new Object[]{Role.SIXTH, CalculationType.TARGET_NUMBER, TypeOfBatch.TARGET_NUMBER};
        return array;
    }

    @DataProvider(name = "typesWithRolesG2")
    private static Object[][] calculationTypesWithRolesG2() {
        Object[][] array = new Object[4][];
        array[0] = new Object[]{Role.FIRST, CalculationType.KPI, TypeOfBatch.FORECAST};
        array[1] = new Object[]{Role.THIRD, CalculationType.KPI, TypeOfBatch.FORECAST};
        array[2] = new Object[]{Role.FIRST, CalculationType.FTE, TypeOfBatch.FTE};
        array[3] = new Object[]{Role.FOURTH, CalculationType.FTE, TypeOfBatch.FTE};
        return array;
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void driverSetUp() {
        setBrowserTimeout(bc.getWrappedDriver(), 15);
    }

    @AfterMethod(alwaysRun = true, description = "Очистить куки и подготовить новую вкладку")
    private void cleanCookies() {
        cleanCookiesAndOpenNewTab(bc.getWrappedDriver());
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"withOutRole"})
    private void goOnPage() {
        goToMathBatchCalculationPage();
    }

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void tearDown() {
        closeDriver(bc.getWrappedDriver());
    }

    @AfterMethod(alwaysRun = true, description = "Сбросить все расчеты", dependsOnGroups = "forecast")
    private void closeForecast() {
        for (TypeOfBatch value : TypeOfBatch.values()) {
            String urlEnding = makePath(BATCH, value.getName());
            //    PresetClass.deleteRequest(setUri(Projects.WFM, URL_B, urlEnding));
        }
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"ABCHR5650-1"})
    private void disableCalculationsApi() {
        changeProperty(SystemProperties.BATCH_CALCULATION_USE_CALCULATION_API_FOR_SHIFTS, false);
    }

    private void goToMathBatchCalculationPage() {
        new GoToPageSection(bc).getPage(Section.BATCH_CALCULATION, 60);
        AtlasWebElement mainPanel;
        mainPanel = bc.mainPanel().mainTreePanel();
        mainPanel.waitUntil("Не было отображено дерево с оргЮнитами", DisplayedMatcher.displayed(), 40);
    }

    @Step("Кликнуть по полю \"Использовать исторические данные\" и выбрать из списка позицию : {list}")
    private void typeUpServiceList(UpServiceList list) {
        bc.fteForecastForm().upServiceInput().click();
        bc.fteForecastForm().upServiceList(list.ordinal() + 1).click();
    }

    @Step("Кликнуть по полю \"Метод\" и выбрать из списка позицию : {method}")
    private void fteTypeList(FteMethod method) {
        bc.fteForecastForm().fteTypeInput().click();
        bc.fteForecastForm().fteTypeList(method.ordinal() + 1).click();
    }

    @Step("Кликнуть по полю \"Алгоритм\" и выбрать из списка позицию : {list}")
    private void fteAlgoritm(FteAlgorithmList list) {
        bc.fteForecastForm().fteAlgorithmInput().click();
        bc.fteForecastForm().fteAlgorithmList(list.ordinal() + 1).click();
    }

    @Step("Кликнуть по полю \"Стратегия\" и выбрать из списка позицию : {str}")
    private void typeStrategyList(StrategyList str) {
        systemSleep(2); //Метод используется в неактуальных тестах
        bc.plannedStrengthForecastForm().strategyInput().click();
        bc.plannedStrengthForecastForm().strategyList(str.ordinal() + 1).click();
    }

    @Step("Кликнуть по полю \"Метод\" и выбрать из списка позицию : {method}")
    private void typeMethodList() {
        systemSleep(2);//Метод используется в неактуальных тестах
        bc.plannedStrengthForecastForm().methodInput().click();
        bc.plannedStrengthForecastForm().methodList(1).click();
    }

    @Step("Кликнуть \"Рассчитать\" в открывшейся форме")
    private void confirmCalculation() {
        LOG.info("Кликаем \"Рассчитать\" в открывшейся форме");
        systemSleep(0.5);//без этого ожидания может быть нажата не та кнопка
        bc.plannedStrengthForecastForm().calculatePlannedStrength().click();
    }

    @Step("Ввести в поле \"Количество месяцев без изменения в численности\" : {num}")
    private void sendNumIntoStaffSize(int num) {
        bc.plannedStrengthForecastForm().staffSize().sendKeys(Integer.toString(num));
    }

    @Step("Ввести в поле \"Макс.отрицательное изменение численности\" : {max}")
    private void sendNumIntoStrengthMinus(int max) {
        bc.plannedStrengthForecastForm().changeNumbersMinus().sendKeys(Integer.toString(max));
    }

    @Step("Ввести в поле \"Макс.положительное изменение численности\" : {min}")
    private void sendNumIntoStrengthPlus(int min) {
        bc.plannedStrengthForecastForm().changeNumbersPlus().sendKeys(Integer.toString(min));
    }

    @Step("Кликнуть \"Рассчитать\"")
    private void calculateFTEButton() {
        bc.fteForecastForm().buttonCalculateFTE().click();
    }

    @Step("Кликнуть по полю \"Тип расчета\" в левом нижнем углу и выбрать тип \"{type.batchCalculation}\"")
    private void selectTypeOfCalculation(CalculationType type) {
        LOG.info("Кликаем по полю \"Тип расчета\" в левом нижнем углу и выбираем тип \"{}\"", type.getBatchCalculation());
        waitForClickable(bc.bottomPanel().buttonBatchCalculationType(), bc, 30);
        bc.bottomPanel().buttonBatchCalculationType().click();
        bc.bottomPanel().batchCalculationType(type.getBatchCalculation()).click();
    }

    @Step("Вернуться к началу страницы и развернуть \"Опции\" в столбце {type}")
    private void clickOptionsForecast(ListOfOption type) {
        JavascriptExecutor js = (JavascriptExecutor) bc.getWrappedDriver();
        js.executeScript("document.getElementById('options-menu-forecast').scrollIntoView(0)");
        bc.mainPanel().listOptions(type.ordinal() + 1).click();
    }

    @Step("Кликнуть : {chose}")
    private void chooseOptionForecast(ListOfNotification chose) {
        bc.mainPanel().optionsMenuForecast(chose.ordinal() + 1).click();
        bc.mainPanel().optionsMenuForecast(chose.ordinal() + 1)
                .waitUntil("Окно с выбором вариантов не закрылось после выбора",
                           Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Кликнуть на кнопку \"Фильтр по тегам\"")
    private void clickTagFilterButton() {
        bc.mainPanel().filterButton().click();
        LOG.info("Кликаем на кнопку \"Фильтр\"");
        bc.mainPanel().tagsButton().waitUntil("Кнопка \"Теги\" не отображается", DisplayedMatcher.displayed());
        bc.mainPanel().tagsButton().click();
        LOG.info("Выбираем \"Теги\"");
    }

    @Step("Активировать чекбокс рядом с тегом \"{param}\"")
    private void clickTag(String param) {
        bc.tagForm().waitUntil(DisplayedMatcher.displayed());
        bc.tagForm().tagCheckbox(param).click();
        if (bc.tagForm().tagCheckbox(param).getAttribute("class").contains("is-checked_ is-checked")) {
            Allure.addAttachment(DATE, "text/plain", String.format("Тег \"%s\" активирован", param));
            LOG.info("Активируем чекбокс тега {}", param);
        } else {
            Allure.addAttachment(DATE, "text/plain", String.format("Тег \"%s\" деактивирован", param));
            LOG.info("Деактивируем чекбокс тега {}", param);
            changeStepName(String.format("Деактивировать чекбокс рядом с тегом \"%s\"", param));
        }
    }

    @Step("Нажать кнопку \"Выбрать\"")
    private void clickChooseButtonInTagForm() {
        bc.tagForm().selectTagsButton().click();
        LOG.info("Нажимаем кнопку \"Выбрать\"");
        bc.tagForm().waitUntil("Форма фильтрации по тегам не закрылась", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Выбрать произвольный год и нажать на произвольный месяц")
    private void choseRandomMonth() {
        DatePicker monthPicker = new DatePicker(bc.datePickerForm());
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM).minusYears(new Random().nextInt(3));
        monthPicker.pickMonth(date);
        Allure.addAttachment(DATE, "text/plain", "Была выбрана дата: " + date);
    }

    @Step("Нажать на клавишу ОК в форме выбора даты")
    private void clickOkButtonInMonthPicker() {
        DatePicker monthPicker = new DatePicker(bc.datePickerForm());
        monthPicker.okButtonClick();
    }

    @Step("Нажать на клавишу \"Отмена\" в форме выбора даты")
    private void clickCancelButtonInMonthPicker() {
        DatePicker monthPicker = new DatePicker(bc.datePickerForm());
        monthPicker.cancelButtonClick();
    }

    /**
     * Данный метод осуществляет нажатие на чекбоксы у определнных ОМ
     *
     * @param desiredOmID список id нужных ОМ
     */
    @Step("Выбрать из дерева подразделения: {desiredOmNames}")
    private void workWithTree(List<Integer> desiredOmID, List<String> desiredOmNames) {
        LOG.info("Выбираем ОМ из дерева c именами {}", desiredOmNames);
        bc.mainPanel().waitUntil("Форма всей страницы без нижней панели не отображена",
                                 Matchers.not(Matchers.emptyArray()), 5);
        TreeNavigate treeNavigate = new TreeNavigate(CommonRepository.getTreePath(desiredOmID, URL_B));
        treeNavigate.workWithTree(bc.mainPanel(), Direction.DOWN);
    }

    /**
     * Данный метод осуществляет раскрытие дерева до нужного ОМ
     *
     * @param desiredOmID - список id нужных ОМ
     */
    @Step("Выбрать ОМ из дерева c именами : {desiredOmNames}")
    private void workWithTreeWithOutClick(List<Integer> desiredOmID) {
        bc.mainPanel().waitUntil("Форма всей страницы без нижней панели не отображена",
                                 Matchers.not(Matchers.emptyArray()), 5);
        new TreeNavigate(CommonRepository.getTreePath(desiredOmID, URL_B));
    }

    @Step("Вернуться до центрального офиса, путем закрытия шевронов, или деактивации чекбоксов, если они активны")
    private void workWithTreeToStart(List<Integer> desiredOmID) {
        bc.mainPanel().waitUntil("Форма всей страницы без нижней панели не отображена",
                                 Matchers.not(Matchers.emptyArray()), 5);
        TreeNavigate treeNavigate = new TreeNavigate(CommonRepository.getTreePath(desiredOmID, URL_B));
        treeNavigate.workWithTree(bc.mainPanel(), Direction.UP);
    }

    @Step("Активировать чекбокс \"С минимальным отклонением\"")
    private void clickCheckBoxMIN() {
        bc.shiftForecastForm().checkboxMIN().click();
    }

    @Step("Нажать \"Рассчитать\"")
    private void calculateShift() {
        LOG.info("Нажимаем \"Рассчитать\"");
        bc.shiftForecastForm().calculateShift().click();
    }

    @Step("Кликнуть кнопку \"Рассчитать\"")
    private void clickToCalculate() {
        LOG.info("Кликаем кнопку \"Рассчитать\"");
        systemSleep(1);//без этого ожидания может быть нажата не та кнопка
        bc.bottomPanel().buttonCalculate().click();
    }

    @Step("Кликнуть по полю KPI и выбрать : {kpiType}")
    private void selectKpiType(KpiType kpiType) {
        bc.kpiForecastForm().kpiValueButton().click();
        bc.kpiForecastForm().kpiValueType(kpiType.getType()).waitUntil(DisplayedMatcher.displayed());
        bc.kpiForecastForm().kpiValueType(kpiType.getType()).click();
    }

    @Step("Проверка на то , что окно расчета kpi закрылось")
    private void assertThatKPIForecastFormClosed() {
        bc.kpiForecastForm().should("расчет KPI не закрылся", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Проверка на то , что окно расчета fte закрылось")
    private void assertThatFTEForecastFormClosed() {
        bc.fteForecastForm().should("расчет KPI не закрылся", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Ввести значение в поле \"Мин.значение трафика\" : {minKpi}")
    private void pickMinKpi(int minKpi) {
        bc.kpiForecastForm().kpiMin().sendKeys(Integer.toString(minKpi));
    }

    @Step("Ввести в поле \"Макс.значение трафика\" значение \"{maxKpi}\"")
    private void pickMaxKpi(int maxKpi) {
        bc.kpiForecastForm().kpiMax().sendKeys(Integer.toString(maxKpi));
    }

    @Step("Проверить, что начался расчет типа \"{type.name}\"")
    private void checkCalculation(TypeOfBatch type) {
        bc.mainPanel().calculationStatus(type.getColumnNumber()).should("Расчет не начался", DisplayedMatcher.displayed(), 5);
        Allure.addAttachment("Process", "text/plain", "Статус:  "
                + bc.mainPanel().calculationStatus(type.getColumnNumber()).getText());
    }

    @Step("Проверить, что начался расчет типа \"{type.name}\"")
    private void checkCalculation(TypeOfBatch type, OrgUnit orgUnit) {
        AtlasWebElement statusElement = bc.mainPanel().calculationStatus(orgUnit.getName(), type.getColumnNumber());
        statusElement.should("Расчет не начался. Статус расчета: " + statusElement.getText(),
                             Matchers.anyOf(text(containsString("В очереди")),
                                            text(containsString("Идет расчет"))), 5);
        Allure.addAttachment("Расчет", "Статус:  " + statusElement.getText());
        List<OrgUnit> temp = OrgUnitRepository.getAllChildrenOrgUnits(orgUnit);
        List<Calculation> calculations = CalculationRepository.getAllCalculations(type);
        Allure.addAttachment("Проверка", "После активации расчета по " + type.getName()
                + " Отобразились следующие подразделения со статусами:\n" + calculations);
        Assert.assertTrue(calculations.stream().map(Calculation::getOrganizationUnitId).collect(Collectors.toList())
                                  .containsAll(temp.stream().map(OrgUnit::getId).collect(Collectors.toList())),
                          "Подразделения, добавленные в расчет, не отобразились в api");
    }

    @Step("Проверить что начался расчет типа \"{type.name}\" для подразделения \"{orgUnit.name}\"")
    private void checkUnitCalculation(TypeOfBatch type, OrgUnit orgUnit) {
        bc.plannedStrengthForecastForm().waitUntil("Форма запуска расчета не закрылась",
                                                   Matchers.not(DisplayedMatcher.displayed()), 5);
        AtlasWebElement statusElement = bc.mainPanel().calculationStatus(orgUnit.getName(), type.getColumnNumber());
        statusElement.should("Расчет не начался. Статус расчета: " + statusElement.getText(),
                             Matchers.anyOf(text(containsString("В очереди")),
                                            text(containsString("Идет расчет")),
                                            text(containsString("Расчет завершен"))), 5);
        Allure.addAttachment("Расчет", "Статус:  " + statusElement.getText());
        List<Calculation> calculations = CalculationRepository.getAllCalculations(type);
        LOG.info("Calculated units with statuses: {}", calculations.stream().map(Calculation::getOrganizationUnitId).collect(Collectors.toList()));
        Calculation calculation = CalculationRepository.getCalculationForOrgUnit(type, orgUnit.getId());
        String log = String.format("После запуска расчета для подразделения \"%s\" по \"%s\" В API отразился статус: \"%s\"",
                                   orgUnit.getName(), type.getName(), calculation.getStatus());
        // Опущена явная проверка существования calculation, т.к. метод его получения выбросит ошибку, если не найдет такой расчет в апи.
        LOG.info(log);
        Allure.addAttachment("Проверка", log);
        statusElement.waitUntil("Расчет не завершился", text(containsString("Расчет завершен")), 60);
    }

    /**
     * Проводит минимальную проверку того, что расчет начался. Нужно для последующей отмены расчета, чтобы он не успел завершиться,
     * что часто происходит при полном цикле проверок.
     */
    @Step("Проверить что начался расчет типа \"{type.name}\" для подразделения \"{orgUnit.name}\"")
    private void quickCheckUnitCalculation(TypeOfBatch type, OrgUnit orgUnit) {
        AtlasWebElement statusElement = bc.mainPanel().calculationStatus(orgUnit.getName(), type.getColumnNumber());
        statusElement.should(String.format("Расчет не начался. Статус расчета: %s", statusElement.getText()),
                             Matchers.anyOf(text(containsString("В очереди")),
                                            text(containsString("Идет расчет"))), 5);
        CalcJob calcJob = CalcJobRepository.search(orgUnit, type, LocalDate.now(), LocalDate.now())
                .stream()
                .findFirst()
                .orElse(null);
        Assert.assertNotNull(calcJob, "Расчет не появился в API");
        Allure.addAttachment("Расчет", String.format("Статус на UI:  %s", statusElement.getText()));
        Allure.addAttachment("Расчет", String.format("Статус в API:  %s", calcJob.getStatus()));
    }

    @Step("Проверить, что в списке отображается только {calculationType}")
    private void checkCalculationTypesMatches(List<CalculationType> calculationType) {
        bc.bottomPanel().calculationTypesList().waitUntil("Количество допустимых видов расчета не совпало с ожидаемым",
                                                          Matchers.iterableWithSize(calculationType.size()));
        List<String> uiList = bc.bottomPanel().calculationTypesList().stream()
                .map(WebElement::getText).collect(Collectors.toList());
        Assert.assertEquals(uiList, calculationType.stream().map(CalculationType::getBatchCalculation).collect(Collectors.toList()),
                            "На Ui отобразились типы: " + uiList + ", ожидалось: " + calculationType);
    }

    @Step("Указать тренд прогноза графика в процентах")
    private void sendInTrendForm() {
        Random rnd = new Random();
        int range = rnd.nextInt(100);
        boolean downOrUp = rnd.nextBoolean();
        if (downOrUp) {
            range = range * -1;
        }
        bc.kpiForecastForm().trendForm().clear();
        bc.kpiForecastForm().trendForm().sendKeys(String.valueOf(range));
        Allure.addAttachment("Тренд", "В поле тренда было отправлено значение : " + range);
    }

    @Step("Нажать на кнопку \"Создать\" в форме расчета KPI")
    private void createForecast() {
        bc.kpiForecastForm().kpiForecastCreate().click();
    }

    @Step("Кликнуть по полю \"Алгоритм\" и выбрать позицию : {kpiAlgorithm}")
    private void selectForecastAlgorithm(KpiAlgorithm kpiAlgorithm) {
        bc.kpiForecastForm().kpiForecastAlgorithm().click();
        bc.kpiForecastForm().kpiForecastAlgorithmList().get(kpiAlgorithm.ordinal()).click();
    }

    @Step("Проверить количество открытых шевронов")
    private void checkGoToOrgUnitPoint(OrganizationUnitTypeId id) {
        int currentSize = bc.mainPanel().arrayOfUnfoldingArrows().size();
        Assert.assertEquals(id.getId() - 1, currentSize,
                            "Количество открытых шевронов в логике теста и на сайте на совпало");
        Allure.addAttachment("Проверка", "Открытх шевронов оказалось: " + currentSize);
    }

    @Step("Кликнуть на значок \"крестик\" в правом верхнем углу для закрытия формы расчета прогноза")
    private void closeBatchCalculation() {
        bc.calculationTheForecast().closeCalculationTheForecast().click();
    }

    @Step("Проверка на то что форма расчета прогноза закрылась")
    private void assertCloseBatchCalculation() {
        bc.calculationTheForecast().should("Форма прогноза не закрылась",
                                           Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Кликнуть на значок \"крестик\" в правом верхнем углу для закрытия формы FTE")
    private void closeFTEForecastForm() {
        bc.fteForecastForm().buttonCloseFormFTE().click();
    }

    @Step("Кликнуть на значок \"крестик\" в правом верхнем углу для закрытия формы для расчета смены")
    private void closeShiftForm() {
        bc.shiftForecastForm().buttonCloseForm().click();
    }

    @Step("Проверка, что форма расчета FTE закрылась")
    private void assertForClosingFTEForm() {
        bc.fteForecastForm().should("this form was not closed", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Проверка на то, что форма расчета смены закрылась")
    private void assertForClosingShiftForm() {
        bc.shiftForecastForm().should("this form was not closed", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Проверка на то, что форма расчета плановой численности закрылась")
    private void assertForClosingPlannedStrengthForecastForm() {
        bc.plannedStrengthForecastForm()
                .should("Форма плановой численности не закрылась", Matchers.not(DisplayedMatcher.displayed()), 5);

    }

    @Step("Закрыть форму расчета плановой численности")
    private void closingPlannedStrengthForecastForm() {
        bc.plannedStrengthForecastForm().buttonCloseForm().click();
    }

    @Step("Нажать на кнопку \"Отменить\" в форме расчета плановой численности")
    private void cancelPlannedStrengthForecastForm() {
        bc.plannedStrengthForecastForm().buttonCloseForm().click();
    }

    @Step("Кликнуть на значок \"календаря\" рядом с названием месяца в правом нижнем углу")
    private void openDateForm() {
        bc.bottomPanel().buttonOpenCalendar().click();
    }

    @Step("Проверка на то, что выбор даты был отменен")
    private void assertCancelDateForm(String date) {
        bc.datePickerForm().waitUntil(Matchers.not(DisplayedMatcher.displayed()));
        bc.datePickerForm().should("this form was not closed", Matchers.not(DisplayedMatcher.displayed()));
        String currentDate = bc.bottomPanel().dateInput().getAttribute("value");
        Allure.addAttachment(DATE, "text/plain", "Дата до отмены : " + date +
                "\n" + "Дата поле отмены : " + currentDate);
        Assert.assertEquals(date, currentDate, "Дата в процессе теста поменялась");
    }

    @Step("Проверка на то, что количество ОМ участвующих в расчете соответствует колличеству на UI")
    private void assertForChildren(OrgUnit orgUnit) {
        String name = orgUnit.getName();
        List<OrgUnit> allChildrenOrgUnits = OrgUnitRepository.getAllChildrenOrgUnits(orgUnit);
        int numChildrenInAPI = allChildrenOrgUnits.size() + 1;
        //здесь мы получаем текст состояния расчета и убираем все лишние символы
        String numChildrenOnUI = bc.mainPanel().kpiForecastCalculationStatus().
                getText().replaceAll("[()]|(\\s+)|[а-я]|[A-Я]", "").split("/")[1];
        //проверяем что количество чилдренов этого оргюнита равно количеству расчетов на UI
        Allure.addAttachment("Forecast", "text/plain", "Количество участвующих в расчете " +
                "чилдренов вместе с ОМ c именем " + name + "на UI : " + numChildrenOnUI + "\n" +
                "Количеству чилдренов этого ОМ в API " + numChildrenInAPI);
        Assert.assertEquals(String.valueOf(numChildrenInAPI), numChildrenOnUI, "Количество " +
                "чилдренов ОМ с именем " + name + " на UI : " + numChildrenOnUI + " , участвующих в расчете " +
                ", не равно количеству чилдренов в API " + numChildrenInAPI);

    }

    @Step("Нажать на кнопку \"Очистить\"")
    private void clickClearButtonInTagsForm() {
        bc.tagForm().clearTagsButton().click();
        LOG.info("Нажимаем на кнопку \"Очистить\"");
        bc.tagForm().waitUntil("Форма фильтрации по тегам не закрылась", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    /**
     * Нажимаем на все видимые кнопки "Загрузить еще"
     */
    private void clickButtonsLoadMore() {
        ElementsCollection<AtlasWebElement> loadMoreButtons = bc.mainPanel().buttonsLoadMore();
        while (!loadMoreButtons.isEmpty()) {
            new Actions(bc.getWrappedDriver()).moveToElement(loadMoreButtons.get(0)).click().perform();
            //waitUntil тут некорректно работает
            systemSleep(1);
            loadMoreButtons = bc.mainPanel().buttonsLoadMore();
        }
    }

    @Step("Проверить, что на UI отображаются только нужные подразделения")
    private void assertOnlySpecifiedUnitsAreDisplayed(List<OrgUnit> unitsFromApi) {
        List<String> unitNamesFromApi = unitsFromApi
                .stream()
                .filter(unit -> OrgUnitRepository.getAllLowLevelChildrenOrgUnits(unit.getId()).isEmpty())
                .map(OrgUnit::getName)
                .sorted()
                .collect(Collectors.toList());
        ElementsCollection<AtlasWebElement> closedChevrons = bc.mainPanel().closedChevrons();
        boolean allDisabled = false;
        ElementsCollection<AtlasWebElement> loadMoreButtons = bc.mainPanel().buttonsLoadMore();
        while (!closedChevrons.isEmpty() && !allDisabled) {
            allDisabled = true;
            for (int i = 0; i < closedChevrons.size(); i++) {
                if (closedChevrons.get(i).isDisplayed()) {
                    try {
                        closedChevrons.get(i).click();
                    } catch (ElementNotVisibleException ex) {
                        LOG.info("Не удалось нажать на шеврон {}", closedChevrons.get(i).toString());
                        clickButtonsLoadMore();
                    }
                    allDisabled = false;
                }
            }
            closedChevrons = bc.mainPanel().closedChevrons();
        }
        clickButtonsLoadMore();
        List<String> unitNamesOnUi = new ArrayList<>();
        for (String name : unitNamesFromApi) {
            if (!bc.mainPanel().chevronButton(name).isDisplayed()) {
                unitNamesOnUi.add(name);
            }
        }
        Assert.assertEquals(unitNamesOnUi, unitNamesFromApi, "Список подразделений на UI не соответствует списку из API");
        Allure.addAttachment("Список подразделений", unitNamesFromApi.toString());
    }

    private void assertChildrenOfMainUnitAreDisplayed() {
        OrgUnit mainUnitFromApi = OrgUnitRepository.getOrgUnitsByTypeId(OrganizationUnitTypeId.getHighest().getId()).get(0);
        List<String> unitNamesFromApi = OrgUnitRepository.getOrgUnitsInTree(mainUnitFromApi.getId())
                .stream()
                .map(OrgUnit::getName)
                .sorted()
                .collect(Collectors.toList());
        ElementsCollection<AtlasWebElement> closedChevrons = bc.mainPanel().closedChevrons();
        Assert.assertTrue(closedChevrons.size() == 1, "Отображено больше одного главного подразделения");
        closedChevrons.get(0).click();
        if (bc.mainPanel().displayedOrgUnitNames().size() == 1) {
            closedChevrons.get(0).click();
            bc.mainPanel().displayedOrgUnitNames().waitUntil(Matchers.hasSize(Matchers.greaterThan(1)));
        }
        List<String> names = bc.mainPanel().displayedOrgUnitNames()
                .stream()
                .map(WebElement::getText)
                .sorted()
                .collect(Collectors.toList());
        names.remove(OrgUnitRepository.getMainOrgUnit().getName());
        Assert.assertEquals(names, unitNamesFromApi, "Список подразделений на UI не соответствует списку из API");
        Allure.addAttachment("Список подразделений", unitNamesFromApi.toString());
    }

    @Step("Проверка, что месяц в расчете соответствует текущему")
    private void checkMonthCalculation() {
        LocalDate now = LocalDate.now();
        String monthName = now.getMonth()
                .getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru"));
        Allure.addAttachment("Проверка",
                             String.format("Текущий месяц: %s\n\nМесяц в расчете: %s", monthName,
                                           bc.bottomPanel().dateInput().getAttribute("value")));
        Assert.assertEquals(bc.bottomPanel().dateInput().getAttribute("value"),
                            monthName + " " + now.getYear());
    }

    @Step("Произвольным образом выбрать тип расчета : {type}")
    private void clickOnRandomTypeCalculation(CalculationType type) {
        JavascriptExecutor js = (JavascriptExecutor) bc.getWrappedDriver();
        js.executeScript("document.getElementsByClassName(\"mdl-button mdl-button--raised " +
                                 "au-target mdl-js-button\")[0].scrollIntoView()");
        bc.bottomPanel().buttonBatchCalculationType().click();
        bc.optionsForm().chooseOption(type.getListBatchCalculationType()).click();
        Allure.addAttachment("Calculation type", "text/plain", "Был выбран тип : " + type);
    }

    @Step("Кликнуть кнопку \"Рассчитать\"/\"Создать\"")
    private void clickButtonCalculateOrCreate(CalculationType type) {
        switch (type) {
            case KPI:
                bc.kpiForecastForm().createButton().click();
                break;
            case FTE:
                bc.fteForecastForm().buttonCalculateFTE().click();
                break;
            case SHIFT:
                bc.shiftForecastForm().calculateShift().click();
                break;
            case TARGET_NUMBER:
                bc.plannedStrengthForecastForm().calculatePlannedStrength().click();
                break;
            default:
                Assert.fail("Ни одна из форм расчета не была загружена");
        }
    }

    @Step("Проверка на открытие страницы скачивания расчета")
    private void assertForDownload(TypeOfBatch typeOfBatch) {
        FileDownloadCheckerForBatchCalculation checker = new FileDownloadCheckerForBatchCalculation(Role.ADMIN, typeOfBatch);
        HttpResponse httpResponse = checker.downloadResponse(Role.ADMIN, TypeOfAcceptContent.PDF_XLSX);
        assertStatusCode(httpResponse, 200, checker.getDownloadLink().toString());
    }

    @Step("Проверка, на участие ОМ по id : {chosenOmId} и ом , которые ниже его по иерархии в расчете типа : {type}")
    private void checkRandomCalculationStatus(List<OrgUnit> chosenOmId, CalculationType type) {
        List<String> ids = chosenOmId.stream().map(orgUnit -> String.valueOf(orgUnit.getId())).collect(Collectors.toList());
        List<NameValuePair> pairs = Pairs.newBuilder()
                .withChildren(true)
                .orgUnitIds(StringUtils.join(ids, ','))
                .size(1000)
                .build();
        JSONObject someObject = getJsonFromUri(Projects.WFM, URL_B, ORGANIZATION_UNITS, pairs);
        JSONArray jsonArray = someObject.getJSONObject(EMBEDDED).getJSONArray(REL_ORGANIZATION_UNITS);
        List<String> arrayForApi = new ArrayList<>();
        List<String> arrayForCalculation = new ArrayList<>();
        int id;
        String urlForApi;
        URI uriForApi;
        JSONObject someObjectForApi;
        JSONArray jsonArrayForApi;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            if (json.getBoolean("availableForCalculation")) {
                switch (json.getInt("organizationUnitTypeId")) {
                    case 5:
                        arrayForApi.add(json.getString(NAME));
                        break;
                    case 4:
                    case 3:
                    case 2:
                    case 1:
                        id = json.getInt("id");
                        urlForApi = makePath(ORGANIZATION_UNITS, id, CHILDREN);
                        try {
                            someObjectForApi = getJsonFromUri(Projects.WFM, URL_B, urlForApi);
                        } catch (JSONException e) {
                            arrayForApi.add(json.getString(NAME));
                            break;
                        }
                        break;
                }
            }
        }
        //Выбор запроса в зависмоти от выбраного типа расчета
        String urlForCalculation;
        switch (type) {
            case KPI:
                urlForCalculation = makePath(BATCH, KPI_FORECAST);
                break;
            case FTE:
                urlForCalculation = makePath(BATCH, FTE);
                break;
            case SHIFT:
                urlForCalculation = makePath(BATCH, ROSTERING);
                break;
            case TARGET_NUMBER:
                urlForCalculation = makePath(BATCH, CALCULATE_NUMBER_OF_EMPLOYEES);
            default:
                urlForCalculation = null;
                Assert.fail("Url для расчета не выбрано");
        }
        //здесь мы вытаскиваем из расчета имена , а те , которые null
        JSONObject forCalculation = getJsonFromUri(Projects.WFM, URL_B, urlForCalculation);
        JSONArray jsonArrayForCalculation = forCalculation.getJSONObject(EMBEDDED).getJSONArray("calculations");
        for (int i = 0; i < jsonArrayForCalculation.length(); i++) {
            try {
                String om = jsonArrayForCalculation.getJSONObject(i).getString("organizationUnitName");
                arrayForCalculation.add(om);
            } catch (JSONException ignored) {
                int idFormCalc = jsonArrayForCalculation.getJSONObject(i).getInt("organizationUnitId");
                String urlForNullOMInCalculation = makePath(ORGANIZATION_UNITS, idFormCalc);
                JSONObject object = getJsonFromUri(Projects.WFM, URL_B, urlForNullOMInCalculation);
                String nameOfNullOM = object.getString(NAME);
                arrayForCalculation.add(nameOfNullOM);
            }
        }
        //Посмотреть разницу
        List<String> differenceUIAndAPI = new ArrayList<>(arrayForCalculation);
        for (String d : arrayForApi) {
            differenceUIAndAPI.remove(d);
        }
        Allure.addAttachment("Calculation", "text/plain",
                             "Был выбран тип расчета : " + type + "\n" + "\n"
                                     + "ОМ в API, которые должны были участвовать в расчете данного типа: " + arrayForApi + "\n" + "\n"
                                     + "ОМ в расчете данного типа на UI : " + arrayForCalculation + "\n" + "\n"
                                     + "Разница между листами в API и на UI : " + differenceUIAndAPI);
        Assert.assertEquals(arrayForCalculation, arrayForApi, "В расчет попали ОМ, " +
                "которые не должны были участвовать в нем , а именно : " + differenceUIAndAPI);
    }

    @Step("Нажать на кнопку просмотра данных в новом окне")
    private void clickOnDisplayErrorInNewWindow() {
        bc.errorForm().newWindowErrorButton().click();
    }

    @Step("Проверка отображения информации об ошибке в новом окне")
    private void assertNewWindowOpen(OrgUnit orgUnit) {
        ArrayList<String> tabs2 = new ArrayList<>(bc.getWrappedDriver().getWindowHandles());
        Assert.assertEquals(tabs2.size(), 2, "Вкладка с информацией об ошибке не была открыта");
        bc.getWrappedDriver().switchTo().window(tabs2.get(1));
        String text = bc.getWrappedDriver().getPageSource();
        String errorText = text.substring(text.indexOf("{"), text.lastIndexOf("}") + 1);
        JSONObject jsonObject = new JSONObject(errorText);
        int orgId = jsonObject.getInt("organizationUnitId");
        Assert.assertEquals(orgId, orgUnit.getId());
        String orgName = jsonObject.getString("organizationUnitName");
        Assert.assertEquals(orgName, orgUnit.getName());
        String errorType = jsonObject.getJSONObject("error").getString("code");

        Allure.addAttachment("Сообщение об ошибке",
                             "Открылось новое окное браузера с следующим сообщение об ошибке типа: " + errorType);
        bc.getWrappedDriver().close();
        bc.getWrappedDriver().switchTo().window(tabs2.get(0));
    }

    @Step("Нажать на кнопку закрыть в окне информации об ошибке")
    private void clickCloseButtonErrorForm() {
        bc.errorForm().closeErrorFormButton().click();
        bc.errorForm().should("Форма с сообщением об ошибке все еще отображается",
                              Matchers.not(DisplayedMatcher.displayed()), 20);
    }

    @Step("Нажать на кнопку \"Ошибка\" в столбце прогноз")
    private void klickOnKpiErrorButton() {
        bc.mainPanel().errorKpiButton()
                .should("Кнопка Ошибка в столбце прогноз не появилась", DisplayedMatcher.displayed(), 90);
        bc.mainPanel().errorKpiButton().click();
    }

    @Step("Нажать на кнопку \"Ошибка\" в столбце FTE")
    private void klickOnFteErrorButton() {
        bc.mainPanel().errorFteButton()
                .should("Кнопка Ошибка в столбце FTE не появилась", DisplayedMatcher.displayed(), 90);
        bc.mainPanel().errorFteButton().click();
    }

    @Step("Нажать на кнопку \"Ошибка\" в столбце смен")
    private void klickOnShiftErrorButton() {
        bc.mainPanel().errorShiftsButton()
                .should("Кнопка Ошибка в столбце смен не появилась", DisplayedMatcher.displayed(), 90);
        bc.mainPanel().errorShiftsButton().click();
    }

    @Step("Ожидаем окончания рассчетов")
    private void waitCalculate(ListOfOption option) {
        bc.mainPanel().orgCalculateStatus(option.ordinal() + 1).waitUntil("Прошло 10 минут но рассчет все еще идет",
                                                                          Matchers.not(DisplayedMatcher.displayed()), 600);
        boolean needWaiting = true;
        long start = System.currentTimeMillis();
        long end;
        long diff = 0;
        while (needWaiting && diff <= 600) {
            needWaiting = false;
            ElementsCollection<AtlasWebElement> allStatus = bc.mainPanel().orgStatus(option.ordinal() + 1);
            for (AtlasWebElement element : allStatus) {
                String tempText = element.getText();
                if (tempText.contains("Идет расчет") || tempText.contains("В очереди")) {
                    needWaiting = true;
                }
            }
            end = System.currentTimeMillis();
            diff = (end - start) / 1000;
        }
        Assert.assertTrue(diff < 600, "Ждали 10 минут но рассчеты продолжаются");
    }

    @Step("Проверка того что весь столбец очистился")
    private void checkStatusColumn(ListOfOption option) {
        ElementsCollection<AtlasWebElement> allStatus = bc.mainPanel().orgStatus(option.ordinal() + 1);
        for (AtlasWebElement element : allStatus) {
            String tempText = element.getText();
            Assert.assertEquals(tempText, "", "Столбец не очистился");
        }
    }

    /**
     * Находит порядковый номер искомого подразделения в списке для быстрого нажатия.
     * На zozo расчеты проходят быстро, поэтому для того, чтобы успеть проверить отмену расчета, это действие надо выполнить до запуска расчета.
     */
    private int findOrderNumber(String omName) {
        return bc.mainPanel().listOfOrgUnitNames().stream().map(AtlasWebElement::getText).collect(Collectors.toList()).indexOf(omName);
    }

    @Step("Нажать на кнопку отмены расчета подразделения \"{orgName}\"")
    private void clickCancelButton(String orgName, TypeOfBatch type, int index) {
        Actions actions = new Actions(bc.getWrappedDriver());
        // Если сделать просто hover на нужный элемент, то его все равно не будет видно, т.к. его будет закрывать шапка страницы.
        actions.moveToElement(bc.mainPanel().oneOmNameButton(orgName)).perform();
        AtlasWebElement cancelButton = bc.mainPanel().cancelCalculationButton(orgName, type.getColumnNumber());
        cancelButton.should("Кнопка отменить не отобразилась", DisplayedMatcher.displayed(), 10);
        new Actions(bc.getWrappedDriver()).moveToElement(cancelButton);
        if (RELEASE_URL.contains(POCHTA)) {
            //Пока что на стенде почты руками кнопка "Прервать" не срабатывает с первого раза, приходится нажимать несколько раз
            new Actions(bc.getWrappedDriver()).click(cancelButton).perform();
            systemSleep(2);
        }
        new Actions(bc.getWrappedDriver()).click(cancelButton).perform();
    }

    @Step("Проверка отмены расчета типа \"{type.name}\" для подразделения \"{unit.name}\"")
    private void assertCancelledCalculation(OrgUnit unit, TypeOfBatch type) {
        AtlasWebElement statusElement = bc.mainPanel().calculationStatus(unit.getName(), type.getColumnNumber());
        statusElement.should("Статус расчета не изменился на \"Техническая ошибка\"",
                             text(containsString("Техническая ошибка")), 40);
        String status = CalcJobRepository.getLatestCalculation(unit.getId()).getStatus();
        //На zozo - FINISHED, на pochta - STOP
        Assert.assertTrue(status.equals("STOP") || status.equals("FINISHED") || status.equals("CANCELED"),
                          "В API расчет не отмечен как cозданный, текущий статус: " + status);
    }

    @Step("Проверить, что расчет запустился. По его окончании проверить время создания новых ростеров")
    private void assertCalculatedShifts(OrgUnit orgUnit, List<Roster> initialRosters, DateInterval dates) {
        new Actions(bc.getWrappedDriver()).moveToElement(bc.mainPanel().checkBoxButton(orgUnit.getName())).perform();
        setBrowserTimeout(bc.getWrappedDriver(), 300);
        CalculationRepository.waitForCalculation(TypeOfBatch.SHIFTS, orgUnit.getId(), 60);
        setBrowserTimeout(bc.getWrappedDriver(), 15);
        ZonedDateTime localTime = ZonedDateTime.now();
        SoftAssert softAssert = new SoftAssert();
        List<Roster> newRosters = RosterRepository.getRosters(orgUnit.getId(), dates, false);
        newRosters.removeAll(initialRosters);
        softAssert.assertEquals(newRosters.size(), 2,
                                "Новые ростеры не найдены в api");
        for (Roster roster : newRosters) {
            ZonedDateTime rosterCreationTime;
            if (orgUnit.getTimeZone() == null) {
                rosterCreationTime = roster.getCreationTime().atZone(ZoneId.of("UTC"));
            } else {
                rosterCreationTime = roster.getCreationTime().atZone(orgUnit.getTimeZone());
            }
            softAssert.assertTrue(localTime.until(rosterCreationTime, ChronoUnit.MINUTES) < 1,
                                  "Время в api: " + rosterCreationTime + " текущее время: " + localTime);
        }
    }

    @Step("Кликнуть по полю \"Тип расчета\" в левом нижнем углу")
    private void typeOfCalculationFieldClick() {
        waitForClickable(bc.bottomPanel().buttonBatchCalculationType(), bc, 30);
        bc.bottomPanel().buttonBatchCalculationType().click();
    }

    private void assertTypeOfCalculationNotDisplayed(CalculationType type) {
        String bathType = type.getBatchCalculation();
        bc.bottomPanel().batchCalculationType(bathType)
                .should("Кнопка выбора типа рассчета \"" + bathType + "\" отображается",
                        Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Кликнуть на ОМ с названием: {omName}")
    private void clickOnDesiredOm(String omName) {
        bc.mainPanel().oneOmNameButton(omName).click();
    }

    @Step("Переключиться на новую вкладку")
    private void removeFirstWindowHandler() {
        String oldTab = bc.getWrappedDriver().getWindowHandle();
        ArrayList<String> newTab = new ArrayList<>(bc.getWrappedDriver().getWindowHandles());
        newTab.remove(oldTab);
        bc.getWrappedDriver().close();
        bc.getWrappedDriver().switchTo().window(newTab.get(0));
    }

    @Step("Проверка того что мы перешли в раздел \"Прогнозирование\"")
    private void assertSwitchToForecast(int orgUnitId) {
        String pageUrl = bc.getWrappedDriver().getCurrentUrl();
        Assert.assertTrue(pageUrl.contains(makePath(Section.ANALYTICS.getUrlEnding(), orgUnitId)),
                          "Переход в раздел \"Прогнозирование\" оргюнита № " + orgUnitId + " не осуществился");
    }

    @Step("Ввести значение {value} в поле ввода {utilizationOrCover.name}")
    private void sendValueInUtilizationOrCover(UtilizationOrCover utilizationOrCover, int value) {
        bc.plannedStrengthForecastForm().percentInput(utilizationOrCover.getId()).clear();
        bc.plannedStrengthForecastForm().percentInput(utilizationOrCover.getId()).sendKeys(String.valueOf(value));
        systemSleep(1); //без этого не всегда срабатывает если нажать потом калькулейт
    }

    @Step("Выбрать в поле \"{dateTypeField.name}\" месяц {month}")
    private void selectMonth(DateTypeField dateTypeField, LocalDate month) {
        LOG.info("Выбираем в поле \"{}\" месяц {}", dateTypeField.getName(), month);
        bc.bottomPanel().calendarButton(dateTypeField.getName()).click();
        DatePicker dp = new DatePicker(bc.datePickerForm());
        dp.pickMonth(month);
        dp.okButtonClickWithoutStep();
    }

    private void clickUnitCheckbox(OrgUnit orgUnit) {
        if (URL_BASE.contains(MAGNIT)) {
            workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        } else {
            bc.mainPanel().checkBoxButton(orgUnit.getName())
                    .waitUntil("Выбранный орг юнит не отображен", DisplayedMatcher.displayed(), 10);
            new Actions(bc.getWrappedDriver()).moveToElement(bc.mainPanel().checkBoxButton(orgUnit.getName())).perform();
            bc.mainPanel().checkBoxButton(orgUnit.getName()).click();
        }
    }

    @Test(groups = {"demo-7", "withOutRole"})
    private void batchCalculationVerification() {
        selectTypeOfCalculation(CalculationType.KPI);
        clickTagFilterButton();
        clickTag("demo7");
        clickChooseButtonInTagForm();
        openDateForm();
        choseRandomMonth();
        clickOkButtonInMonthPicker();
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        selectKpiType(KpiType.CLIENT_COUNT);
        sendInTrendForm();
        createForecast();
    }

    @Test(groups = {"MP-1.1", "TEST-27", "withOutRole"}, description = "Раскрытие дерева ОМ")
    private void treeOpeningOM() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        List<Integer> desiredOmId = Collections.singletonList(orgUnit.getId());
        workWithTree(desiredOmId, Collections.singletonList(orgUnit.getName()));
        checkGoToOrgUnitPoint(OrganizationUnitTypeId.getLowest());
        workWithTreeToStart(desiredOmId);
        checkGoToOrgUnitPoint(OrganizationUnitTypeId.getHighest());
    }

    @Test(groups = {"MP-1.2", "TEST-27", "withOutRole"}, description = "Активация чекбоксов")
    private void checkboxActivation() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getHighest());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        OrgUnit orgUnit1 = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getHighest());
        workWithTree(Collections.singletonList(orgUnit1.getId()), Collections.singletonList(orgUnit1.getName()));
        List<Integer> desiredOmId2 = Arrays.asList(41, 40, 39, 1);
        workWithTreeToStart(desiredOmId2);
    }

    @Test(groups = {"MP-2.1", "TEST-27", "withOutRole"}, description = "Запуск массового расчета по KPI")
    @Severity(value = SeverityLevel.NORMAL)
    private void runningMassKpiCalculation() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        createForecast();
        checkMonthCalculation();
        checkCalculation(TypeOfBatch.FORECAST, orgUnit);
    }

    @Test(groups = {"MP-2.2", "TEST-27", "withOutRole"}, description = "Запуск массового расчета по FTE")
    @Severity(value = SeverityLevel.NORMAL)
    private void runningMassFteCalculation() {
        selectTypeOfCalculation(CalculationType.FTE);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        checkMonthCalculation();
        checkCalculation(TypeOfBatch.FTE, orgUnit);
    }

    @Test(groups = {"MP-2.3", "TEST-27", "withOutRole"}, description = "Запуск массового расчета по сменам")
    @Severity(value = SeverityLevel.NORMAL)
    private void startingMassSettlementInShifts() {
        selectTypeOfCalculation(CalculationType.SHIFT);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        calculateShift();
        checkCalculation(TypeOfBatch.SHIFTS, orgUnit);
        checkMonthCalculation();
    }

    @Test(groups = {"MP-2.4", "TEST-27", "withOutRole"}, description = "Запуск массового расчета по плановой численности")
    private void runMassCalculationOfThePlannedNumber() {
        selectTypeOfCalculation(CalculationType.TARGET_NUMBER);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        confirmCalculation();
        checkCalculation(TypeOfBatch.TARGET_NUMBER, orgUnit);
        checkMonthCalculation();
    }

    @Issue("51519")
    @Test(groups = {"TK2050", G1, BC1},
            description = "Рассчитать смены (пользователь)",
            dataProvider = "typesWithRolesG1")
    @Link(name = "2050_Применение роли в системе блок \"Массовый расчет\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460855")
    @TmsLink("60302")
    @Tag(BC1)
    @Severity(SeverityLevel.NORMAL)
    @Owner(BUTINSKAYA)
    public void runMassCalculationsWithPermissionsGrade1(Role role, CalculationType calcType, TypeOfBatch batchType) {
        changeTestIDDependingOnParameter(batchType == TypeOfBatch.SHIFTS, "TK2050-4", "TK2050-5",
                                         "Рассчитать плановую численность (пользователь)");
        OrgUnit orgUnit = OrgUnitRepository.getRandomAvailableOrgUnit();
        new RoleWithCookies(bc.getWrappedDriver(), role, orgUnit).getPage(SECTION);
        clickUnitCheckbox(orgUnit);
        selectTypeOfCalculation(calcType);
        clickToCalculate();
        systemSleep(4); //на zozo без этого не работает. Методом научного тыка проверено, что нужно именно 4 секунды.
        clickButtonCalculateOrCreate(calcType);
        checkMonthCalculation();
        quickCheckUnitCalculation(batchType, orgUnit);
    }

    @Issue("51519")
    @Test(groups = {"TK2050", G2, BC1},
            description = "Рассчитать FTE (пользователь)",
            dataProvider = "typesWithRolesG2")
    @Link(name = "2050_Применение роли в системе блок \"Массовый расчет\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460855")
    @TmsLink("60302")
    @Owner(BUTINSKAYA)
    @Severity(SeverityLevel.MINOR)
    @Tag(BC1)
    public void runMassCalculationsWithPermissionsGrade2(Role role, CalculationType calcType, TypeOfBatch batchType) {
        changeTestIDDependingOnParameter(batchType == TypeOfBatch.FTE, "TK2050-3", "TK2050-2",
                                         "Запуск массового расчета по KPI (пользователь)");
        OrgUnit orgUnit = OrgUnitRepository.getRandomAvailableOrgUnit();
        new RoleWithCookies(bc.getWrappedDriver(), role, orgUnit).getPage(SECTION);
        clickUnitCheckbox(orgUnit);
        selectTypeOfCalculation(calcType);
        clickToCalculate();
        systemSleep(4); //на zozo без этого не работает. Методом научного тыка проверено, что нужно именно 4 секунды.
        clickButtonCalculateOrCreate(calcType);
        checkMonthCalculation();
        quickCheckUnitCalculation(batchType, orgUnit);
    }

    @Test(groups = {"MP-3.1", "TEST-35", "withOutRole"}, description = "Отмена запуска массового расчета по KPI")
    private void closeStartMassCalculationByKPI() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(1));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        closeBatchCalculation();
        assertCloseBatchCalculation();
    }

    @Test(groups = {"MP-3.2", "TEST-35", "withOutRole", "not actual"}, description = "Отмена запуска массового расчета по FTE")
    private void closeStartMassCalculationByFTE() {
        //выделяем все в SONY
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(1));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        selectTypeOfCalculation(CalculationType.FTE);
        clickToCalculate();
        closeFTEForecastForm();
        assertForClosingFTEForm();
    }

    @Test(groups = {"MP-3.3", "TEST-35", "withOutRole"}, description = "Отмена запуска массового расчета по сменам")
    private void cancelStartMassShift() {
        //выделяем все в SONY
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        selectTypeOfCalculation(CalculationType.TARGET_NUMBER);
        clickToCalculate();
        closeShiftForm();
        assertForClosingShiftForm();
    }

    @Test(groups = {"MP-3.4.1", "TEST-35", "withOutRole"}, description = "Отмена запуска массового расчета по плановой численности")
    private void closeStartMassCalculationAccordingToPlannedSize() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        selectTypeOfCalculation(CalculationType.SHIFT);
        clickToCalculate();
        closingPlannedStrengthForecastForm();
        assertForClosingPlannedStrengthForecastForm();
    }

    @Test(groups = {"MP-3.4.2", "TEST-35", "withOutRole"}, description = "Отмена запуска массового расчета по плановой численности через кнопку Отменить")
    private void cancelStartMassCalculationAccordingToPlannedSize() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        selectTypeOfCalculation(CalculationType.TARGET_NUMBER);
        clickToCalculate();
        cancelPlannedStrengthForecastForm();
        assertForClosingPlannedStrengthForecastForm();
    }

    @Test(groups = {"MP-4.1", "TEST-72", "withOutRole"}, description = "Выбор периода для расчета")
    private void selectionOfPeriodToCalculate() {
        OrgUnit orgUnit = getRandomFromList(OrgUnitRepository.getOrgUnitsNotClosedAndAllType());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        openDateForm();
        choseRandomMonth();
        clickOkButtonInMonthPicker();
        clickToCalculate();
        createForecast();
        assertForChildren(orgUnit);
    }

    @Test(groups = {"MP-4.2", "TEST-72", "withOutRole"}, description = "Отмена выбора периода для расчета")
    private void deselectPeriodToCalculate() {
        String currentDate = bc.bottomPanel().dateInput().getAttribute("value");
        openDateForm();
        choseRandomMonth();
        clickCancelButtonInMonthPicker();
        assertCancelDateForm(currentDate);
    }

    @Test(groups = {"MP-4.3", "TEST-35", "withOutRole"}, description = "Запуск расчета по умолчанию на текущий месяц")
    private void runTheDefaultCalculationForTheCurrentMonth() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        CalculationType type = CalculationType.randomCalculationType();
        clickOnRandomTypeCalculation(type);
        clickToCalculate();
        clickButtonCalculateOrCreate(type);
        checkRandomCalculationStatus(Collections.singletonList(orgUnit), type);
    }

    @Test(groups = {"MP-5", G1, BC1},
            description = "Выбор подразделений для расчета по тегам")
    @Link(name = "Статья: \"Массовый расчет\"", url = "https://wiki.goodt.me/x/chGrCg")
    @TmsLink("60283")
    @Tag("MP-5")
    @Tag(BC1)
    private void selectionOfUnitsForCalculatingByTags() {
        ImmutablePair<String, String> tagPair = CommonRepository.getRandomTagFromApi();
        String tag = tagPair.left;
        goToMathBatchCalculationPage();
        clickTagFilterButton();
        clickTag(tag);
        clickChooseButtonInTagForm();
        List<OrgUnit> unitsFromApi = OrgUnitRepository.getAllOrgUnitsByTag(tagPair.right);
        assertOnlySpecifiedUnitsAreDisplayed(unitsFromApi);
        clickTagFilterButton();
        clickTag(tag);
        clickClearButtonInTagsForm();
        assertChildrenOfMainUnitAreDisplayed();

    }

    @Test(groups = {"MP-6.1", "TEST-36", "withOutRole"}, description = "Просмотр информации об ошибках KPI")
    private void viewKPIErrorInfo() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        PresetClass.checkForErrorKPIorFteOrShiftsAndPreset(orgUnit.getId(), "KPI");
        List<Integer> desiredOmId = Collections.singletonList(orgUnit.getId());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        klickOnKpiErrorButton();
        clickOnDisplayErrorInNewWindow();
        assertNewWindowOpen(orgUnit);
        clickCloseButtonErrorForm();
    }

    @Test(groups = {"MP-6.2", "TEST-36", "withOutRole"}, description = "Просмотр информации об ошибках FTE")
    private void viewFteErrorInfo() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        PresetClass.checkForErrorKPIorFteOrShiftsAndPreset(orgUnit.getId(), "Fte");
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        klickOnFteErrorButton();
        clickOnDisplayErrorInNewWindow();
        assertNewWindowOpen(orgUnit);
        clickCloseButtonErrorForm();
    }

    @Test(groups = {"MP-6.3", "TEST-36", "withOutRole"}, description = "Просмотр информации об ошибках в расчете смен")
    private void viewShiftsErrorInfo() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        PresetClass.checkForErrorKPIorFteOrShiftsAndPreset(orgUnit.getId(), "shifts");
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        klickOnShiftErrorButton();
        clickOnDisplayErrorInNewWindow();
        assertNewWindowOpen(orgUnit);
        clickCloseButtonErrorForm();
    }

    @Step("Выбрать оргюнит {orgUnit.name}")
    private void selectOmInSearch(OrgUnit orgUnit) {
        String name = orgUnit.getName();
        bc.mainPanel().inputOmSearchFiled().clear();
        bc.mainPanel().inputOmSearchFiled().click();
        slowSendKeys(bc.mainPanel().inputOmSearchFiled(), name);
        bc.mainPanel().spinnerLoadingOm()
                .waitUntil("Загрузка оргюнитов идет слишком долго", Matchers.not(DisplayedMatcher.displayed()), 30);
        bc.mainPanel().checkBoxButton(name).waitUntil("Чекбокс оргюнита " + name
                                                              + " не был отображен", DisplayedMatcher.displayed(), 10);
        bc.mainPanel().checkBoxButton(name).click();
    }

    @Test(groups = {"МР-7.1", G1, BC1}, description = "Отмена запущенного расчета смен")
    @Link(name = "Статья: \"Массовый расчет\"", url = "https://wiki.goodt.me/x/chGrCg")
    @TmsLink("60283")
    @Tag("МР-7.1")
    @Tag(BC1)
    private void cancelRunningShiftCalculation() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        String omName = unit.getName();
        goToMathBatchCalculationPage();
        selectOmInSearch(unit);
        selectTypeOfCalculation(CalculationType.SHIFT);
        int index = findOrderNumber(omName);
        clickToCalculate();
        confirmCalculation();
        TypeOfBatch type = TypeOfBatch.SHIFTS;
        quickCheckUnitCalculation(type, unit);
        clickCancelButton(omName, type, index);
        assertCancelledCalculation(unit, type);
    }

    @Test(groups = {"MP-7.2.1", "TEST-36", "withOutRole"}, description = "Отмена запущенного расчета KPI через Сбросить результаты")
    private void cancelRunningResetKpiCalculation() {
        List<OrgUnit> desiredOmId = OrgUnitRepository.getTwoRandomOrgId(OrganizationUnitTypeId.getLowest());
        workWithTree(desiredOmId.stream().map(OrgUnit::getId).collect(Collectors.toList()),
                     desiredOmId.stream().map(OrgUnit::getName).collect(Collectors.toList()));
        selectTypeOfCalculation(CalculationType.KPI);
        clickToCalculate();
        confirmCalculation();
        waitCalculate(ListOfOption.FORECAST);
        clickOptionsForecast(ListOfOption.FORECAST);
        chooseOptionForecast(ListOfNotification.RESET);
        checkStatusColumn((ListOfOption.FORECAST));
    }

    @Test(groups = {"MP-7.2.2", "TEST-36", "withOutRole"}, description = "Отмена запущенного расчета FTE через Сбросить результаты")
    private void cancelRunningResetFteCalculation() {
        List<OrgUnit> desiredOmId = OrgUnitRepository.getTwoRandomOrgId(OrganizationUnitTypeId.getLowest());
        workWithTree(desiredOmId.stream().map(OrgUnit::getId).collect(Collectors.toList()),
                     desiredOmId.stream().map(OrgUnit::getName).collect(Collectors.toList()));
        selectTypeOfCalculation(CalculationType.FTE);
        clickToCalculate();
        waitCalculate(ListOfOption.FTE);
        clickOptionsForecast(ListOfOption.FTE);
        chooseOptionForecast(ListOfNotification.RESET);
        checkStatusColumn((ListOfOption.FTE));
    }

    @Test(groups = {"MP-7.2.3", "TEST-36", "withOutRole"}, description = "Отмена запущенного расчета смен через Сбросить результаты")
    private void cancelRunningResetShiftCalculation() {
        List<OrgUnit> desiredOmId = OrgUnitRepository.getTwoRandomOrgId(OrganizationUnitTypeId.getLowest());
        workWithTree(desiredOmId.stream().map(OrgUnit::getId).collect(Collectors.toList()),
                     desiredOmId.stream().map(OrgUnit::getName).collect(Collectors.toList()));
        selectTypeOfCalculation(CalculationType.SHIFT);
        clickToCalculate();
        confirmCalculation();
        waitCalculate(ListOfOption.SHIFTS);
        clickOptionsForecast(ListOfOption.SHIFTS);
        chooseOptionForecast(ListOfNotification.RESET);
        checkStatusColumn((ListOfOption.SHIFTS));
    }

    @Test(groups = {"MP-7.2.4", "TEST-36", "withOutRole"}, description = "Отмена запущенного расчета плановой численности через Сбросить результаты")
    private void cancelRunningResetPlannedPopulateCalculation() {
        List<OrgUnit> desiredOmId = OrgUnitRepository.getTwoRandomOrgId(OrganizationUnitTypeId.getLowest());
        workWithTree(desiredOmId.stream().map(OrgUnit::getId).collect(Collectors.toList()),
                     desiredOmId.stream().map(OrgUnit::getName).collect(Collectors.toList()));
        selectTypeOfCalculation(CalculationType.TARGET_NUMBER);
        clickToCalculate();
        confirmCalculation();
        waitCalculate(ListOfOption.TARGET_NUMBER);
        clickOptionsForecast(ListOfOption.TARGET_NUMBER);
        chooseOptionForecast(ListOfNotification.RESET);
        checkStatusColumn((ListOfOption.TARGET_NUMBER));
    }

    @Test(groups = {"MP-8.1", "forecast", "TEST-37", "withOutRole"}, description = "Скачивание отчета о состоянии расчета KPI через Скачать состояние")
    private void downloadingAReportOnTheStatusOfTheCalculationOfKPIThroughDownloadStatus() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getRandomType());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        CalculationType calcType = CalculationType.KPI;
        selectTypeOfCalculation(calcType);
        clickToCalculate();
        createForecast();
        clickOptionsForecast(ListOfOption.FORECAST);
        chooseOptionForecast(ListOfNotification.DOWNLOAD);
        assertForDownload(TypeOfBatch.FORECAST);
        checkRandomCalculationStatus(Collections.singletonList(orgUnit), calcType);
    }

    @Test(groups = {"MP-8.2", "forecast", "TEST-37", "withOutRole"}, description = "Скачивание отчета о состоянии расчета FTE через Скачать состояние")
    private void downloadingAReportOnTheStatusOfTheCalculationOfFTEThroughDownloadStatus() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getRandomType());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        CalculationType calcType = CalculationType.FTE;
        selectTypeOfCalculation(calcType);
        clickToCalculate();
        clickOptionsForecast(ListOfOption.FTE);
        chooseOptionForecast(ListOfNotification.DOWNLOAD);
        assertForDownload(TypeOfBatch.FTE);
        checkRandomCalculationStatus(Collections.singletonList(orgUnit), calcType);
    }

    @Test(groups = {"MP-8.3", "forecast", "TEST-37", "withOutRole"}, description = "Скачивание отчета о состоянии расчета смен через Скачать состояние")
    private void downloadingAReportOnTheStatusOfTheCalculationOfchoseOptionRostering() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getRandomType());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        CalculationType calcType = CalculationType.SHIFT;
        selectTypeOfCalculation(calcType);
        clickToCalculate();
        calculateShift();
        clickOptionsForecast(ListOfOption.SHIFTS);
        chooseOptionForecast(ListOfNotification.DOWNLOAD);
        assertForDownload(TypeOfBatch.SHIFTS);
        checkRandomCalculationStatus(Collections.singletonList(orgUnit), calcType);

    }

    @Test(groups = {"MP-8.4", "forecast", "TEST-37", "withOutRole"}, description = "Скачивание отчета о состоянии расчета плановой численности через Скачать состояние")
    private void downloadingAReportOnTheStatusOfTheCalculationOfchoseOptionStaffNumber() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getRandomType());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        CalculationType calcType = CalculationType.TARGET_NUMBER;
        selectTypeOfCalculation(calcType);
        clickToCalculate();
        confirmCalculation();
        clickOptionsForecast(ListOfOption.TARGET_NUMBER);
        chooseOptionForecast(ListOfNotification.DOWNLOAD);
        assertForDownload(TypeOfBatch.TARGET_NUMBER);
        checkRandomCalculationStatus(Collections.singletonList(orgUnit), calcType);
    }

    @Test(groups = {"MP-9.1", "forecast", "TEST-37", "withOutRole", "not actual"}, description = "Окно Расчет прогноза")
    private void windowCalculationOfTheForecast() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getRandomType());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        selectKpiType(KpiType.CHECK_COUNT);
        selectForecastAlgorithm(KpiAlgorithm.FOR_MONTH);
        int minKpi = new Random().nextInt(100);
        int maxKpi = minKpi + new Random().nextInt(100 - minKpi + 1);
        pickMinKpi(minKpi);
        pickMaxKpi(maxKpi);
        //        setKpiTrend(KpiTrendSliderOrField.getRandomVariantKpiTrendSetting());
        createForecast();
        checkRandomCalculationStatus(Collections.singletonList(orgUnit), CalculationType.KPI);
        assertThatKPIForecastFormClosed();
    }

    @Test(groups = {"MP-9.2", "forecast", "TEST-37", "withOutRole", "not actual"}, description = "Окно Расчет ресурсного запроса")
    private void windowCalculationOfTheFTE() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getRandomType());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        selectTypeOfCalculation(CalculationType.FTE);
        clickToCalculate();
        typeUpServiceList(UpServiceList.FOT_MINIMIZATION);
        typeUpServiceList(UpServiceList.MAXIMIZING_FOT);
        fteTypeList(FteMethod.USE_OPTIONS);
        fteAlgoritm(FteAlgorithmList.getRandomType());
        calculateFTEButton();
        checkRandomCalculationStatus(Collections.singletonList(orgUnit), CalculationType.FTE);
        assertThatFTEForecastFormClosed();
    }

    @Test(groups = {"MP-9.3", "forecast", "TEST-37", "withOutRole"}, description = "Окно Расчет смен")
    private void windowCalculationOfTheShiftCalculation() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getRandomType());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        selectTypeOfCalculation(CalculationType.SHIFT);
        clickToCalculate();
        clickCheckBoxMIN();
        calculateShift();
        checkRandomCalculationStatus(Collections.singletonList(orgUnit), CalculationType.SHIFT);
    }

    @Test(groups = {"MP-9.4.1", "forecast", "TEST-37", "withOutRole", "not actual"}, description = "Расчет по параметрам плановой численности и KPI при минимизации ФОТ и сохранении уровня сервиса")
    private void windowCalculationOfThePlannedStrength() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getRandomType());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        selectTypeOfCalculation(CalculationType.TARGET_NUMBER);
        clickToCalculate();
        typeStrategyList(StrategyList.FOT_MINIMIZATION_PRESERVATION_SERVICE_LEVEL);
        typeMethodList();
        confirmCalculation();
        checkCalculation(TypeOfBatch.TARGET_NUMBER);
        checkRandomCalculationStatus(Collections.singletonList(orgUnit), CalculationType.TARGET_NUMBER);
    }

    @Test(groups = {"MP-9.4.2", "forecast", "TEST-37", "withOutRole", "not actual"}, description = "Расчет плановой численности и KPI при максимизации конверсии с увеличением ФОТ")
    private void windowCalculationOfThePlannedStrengthWithPlus() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getRandomType());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        selectTypeOfCalculation(CalculationType.TARGET_NUMBER);
        clickToCalculate();
        typeStrategyList(StrategyList.MAXIMIZING_CONVERSION_WITH_INCREASED_FOT);
        sendNumIntoStaffSize(new Random().nextInt(13));
        sendNumIntoStrengthMinus(new Random().nextInt(100));
        sendNumIntoStrengthPlus(new Random().nextInt(100));
        confirmCalculation();
        checkCalculation(TypeOfBatch.TARGET_NUMBER);
        checkRandomCalculationStatus(Collections.singletonList(orgUnit), CalculationType.TARGET_NUMBER);
    }

    @Test(groups = {"TK2050-2.1", "TEST-942"}, description = "Запуск массового расчета по KPI Роль 1")
    private void runningMassKpiCalculationRole1() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FIRST).getPage(SECTION);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        createForecast();
        checkMonthCalculation();
        checkCalculation(TypeOfBatch.FORECAST, orgUnit);
    }

    @Test(groups = {"TK2050-2.3", "TEST-942"}, description = "Рассчитать прогноз Роль 3")
    private void runningMassKpiCalculationRole3() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.THIRD).getPage(SECTION);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        createForecast();
        checkMonthCalculation();
        checkCalculation(TypeOfBatch.FORECAST, orgUnit);
    }

    @Test(groups = {"TK2050-2.4", "TEST-942"}, description = "Рассчитать прогноз Роль 4")
    private void runningMassKpiCalculationRole4() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FOURTH).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.KPI);
    }

    @Test(groups = {"TK2050-2.5", "TEST-942"}, description = "Рассчитать прогноз Роль 5")
    private void runningMassKpiCalculationRole5() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FIFTH).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.KPI);
    }

    @Test(groups = {"TK2050-2.6", "TEST-942"}, description = "Рассчитать прогноз Роль 6")
    private void runningMassKpiCalculationRole6() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.SIXTH).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.KPI);
    }

    @Test(groups = {"TK2050-3.1", "TEST-942"}, description = "Рассчитать FTE Роль 1")
    private void runningMassFteCalculationRole1() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FIRST).getPage(SECTION);
        selectTypeOfCalculation(CalculationType.FTE);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        calculateFTEButton();
        checkMonthCalculation();
        checkCalculation(TypeOfBatch.FTE, orgUnit);
    }

    @Test(groups = {"TK2050-3.4", "TEST-942"}, description = "Рассчитать FTE Роль 4")
    private void runningMassFteCalculationRole4() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FOURTH).getPage(SECTION);
        selectTypeOfCalculation(CalculationType.FTE);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        calculateFTEButton();
        checkMonthCalculation();
        checkCalculation(TypeOfBatch.FTE, orgUnit);
    }

    @Test(groups = {"TK2050-3.3", "TEST-942"}, description = "Рассчитать FTE Роль 3")
    private void runningMassFteCalculationRole3() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.THIRD).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.FTE);
    }

    @Test(groups = {"TK2050-3.5", "TEST-942"}, description = "Рассчитать FTE Роль 5")
    private void runningMassFteCalculationRole5() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FIFTH).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.FTE);
    }

    @Test(groups = {"TK2050-3.6", "TEST-942"}, description = "Рассчитать FTE Роль 6")
    private void runningMassFteCalculationRole6() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.SIXTH).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.FTE);
    }

    @Test(groups = {"TK2050-4.1", "TEST-942"}, description = "Рассчитать смены Роль 1")
    private void startingMassSettlementInShiftsRole1() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FIRST).getPage(SECTION);
        selectTypeOfCalculation(CalculationType.SHIFT);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        calculateShift();
        checkCalculation(TypeOfBatch.SHIFTS, orgUnit);
        checkMonthCalculation();
    }

    @Test(groups = {"TK2050-4.5", "TEST-942"}, description = "Рассчитать смены Роль 5")
    private void startingMassSettlementInShiftsRole5() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FIFTH).getPage(SECTION);
        selectTypeOfCalculation(CalculationType.SHIFT);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getOrgUnitTypeByLevel(3));
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        calculateShift();
        checkCalculation(TypeOfBatch.SHIFTS, orgUnit);
        checkMonthCalculation();
    }

    @Test(groups = {"TK2050-4.3", "TEST-942"}, description = "Рассчитать смены Роль 3")
    private void startingMassSettlementInShiftsRole3() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.THIRD).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.SHIFT);
    }

    @Test(groups = {"TK2050-4.4", "TEST-942"}, description = "Рассчитать смены Роль 4")
    private void startingMassSettlementInShiftsRole4() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FOURTH).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.SHIFT);
    }

    @Test(groups = {"TK2050-4.6", "TEST-942"}, description = "Рассчитать смены Роль 6")
    private void startingMassSettlementInShiftsRole6() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.SIXTH).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.SHIFT);
    }

    @Test(groups = {"TK2050-5.1", "TEST-942"}, description = "Рассчитать плановую численность Роль 1")
    private void runMassCalculationOfThePlannedNumberRole1() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FIRST).getPage(SECTION);
        selectTypeOfCalculation(CalculationType.TARGET_NUMBER);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        confirmCalculation();
        checkCalculation(TypeOfBatch.TARGET_NUMBER, orgUnit);
        checkMonthCalculation();
    }

    @Test(groups = {"TK2050-5.6", "TEST-942"}, description = "Рассчитать плановую численность Роль 6")
    private void runMassCalculationOfThePlannedNumberRole6() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.SIXTH).getPage(SECTION);
        selectTypeOfCalculation(CalculationType.TARGET_NUMBER);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        confirmCalculation();
        checkCalculation(TypeOfBatch.TARGET_NUMBER, orgUnit);
        checkMonthCalculation();
    }

    @Test(groups = {"TK2050-5.3", "TEST-942"}, description = "Рассчитать плановую численность Роль 3")
    private void runMassCalculationOfThePlannedNumberRole3() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.THIRD).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.TARGET_NUMBER);
    }

    @Test(groups = {"TK2050-5.4", "TEST-942"}, description = "Рассчитать плановую численность Роль 4")
    private void runMassCalculationOfThePlannedNumberRole4() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FOURTH).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.TARGET_NUMBER);
    }

    @Test(groups = {"TK2050-5.5", "TEST-942"}, description = "Рассчитать плановую численность Роль 5")
    private void runMassCalculationOfThePlannedNumberRole5() {
        new RoleWithCookies(bc.getWrappedDriver(), Role.FIFTH).getPage(SECTION);
        typeOfCalculationFieldClick();
        assertTypeOfCalculationNotDisplayed(CalculationType.TARGET_NUMBER);
    }

    @Test(groups = {"TK2050-6", "TEST-1024"}, description = "Проверить количество доступных расчетов для каждой роли",
            dataProvider = "calculationTypes")
    private void checkCalculationTypes(Role role, List<CalculationType> calculationTypes) {
        new RoleWithCookies(bc.getWrappedDriver(), role).getPage(SECTION);
        typeOfCalculationFieldClick();
        checkCalculationTypesMatches(calculationTypes);
    }

    @Test(groups = {"TK-2838", "TEST-1062", "withOutRole"},
            description = "Переход из раздела \"Стратегическое планирование\" в раздел \"Прогнозирование\" ")
    private void switchingFromMassCalculationToForecast() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickOnDesiredOm(orgUnit.getName());
        removeFirstWindowHandler();
        assertSwitchToForecast(orgUnit.getId());
    }

    @Test(groups = {"TK2836-1", "TEST-1064", "withOutRole"}, description = "Расчет плановой численности с указанием утилизации и покрытия")
    private void runMassCalculationOfThePlannedNumberWithUtilizationAndCover() {
        selectTypeOfCalculation(CalculationType.TARGET_NUMBER);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitByTypeId(OrganizationUnitTypeId.getLowest());
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        int utilizationValue = new Random().nextInt(100) + 1;
        sendValueInUtilizationOrCover(UtilizationOrCover.UTILIZATION, utilizationValue);
        int coverValue = new Random().nextInt(100) + 1;
        sendValueInUtilizationOrCover(UtilizationOrCover.COVER, coverValue);
        confirmCalculation();
        checkCalculation(TypeOfBatch.TARGET_NUMBER, orgUnit);
    }

    @Test(groups = {"ABCHR5650-1", X5, "no actual"},
            description = "Расчет смен на два месяца")
    @Link(name = "Статья: \"5650_Возможность рассчитывать график на 2 месяца вперед\"", url = "https://wiki.goodt.me/x/OTD0DQ")
    @TmsLink("60325")
    @Tag("ABCHR5650-1")
    @Tag(BC1)
    private void calculateTwoMonths() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitWithFteGroup(FilterType.KPI).left;
        LocalDate start = LocalDateTools.getFirstDate();
        LocalDate nowPlusMonth = LocalDate.now().plusMonths(1);
        LocalDate endDate = nowPlusMonth.withDayOfMonth(nowPlusMonth.getMonth().length(nowPlusMonth.isLeapYear()));
        DateInterval dates = new DateInterval(start, endDate);
        List<Roster> rosters = RosterRepository.getRosters(orgUnit.getId(), dates, false);
        goToMathBatchCalculationPage();
        selectTypeOfCalculation(CalculationType.SHIFT);
        LocalDate month = LocalDate.now().plusMonths(1).withDayOfMonth(LocalDate.now().plusMonths(1).lengthOfMonth());
        selectMonth(DateTypeField.END_DATE, month);
        workWithTree(Collections.singletonList(orgUnit.getId()), Collections.singletonList(orgUnit.getName()));
        clickToCalculate();
        calculateShift();
        assertCalculatedShifts(orgUnit, rosters, dates);
    }
}
