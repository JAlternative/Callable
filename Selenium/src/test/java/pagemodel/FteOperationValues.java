package pagemodel;

import com.google.inject.Inject;
import guice.TestModule;
import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hamcrest.Matchers;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.FteOperationValuesPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.DatePicker;
import testutils.GoToPageSection;
import utils.Links;
import utils.tools.Format;
import wfm.components.calculation.FilterType;
import wfm.components.utils.DateTypeField;
import wfm.components.utils.Section;
import wfm.models.DateInterval;
import wfm.models.FteOperationValuesModel;
import wfm.models.KpiList;
import wfm.models.OrgUnit;
import wfm.repository.CommonRepository;
import wfm.repository.FteOperationValuesRepository;
import wfm.repository.KpiListRepository;
import wfm.repository.OrgUnitRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.CustomTools.slowSendKeys;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class FteOperationValues {
    private static final String RELEASE_URL = Links.getTestProperty("release");
    private static final Section SECTION = Section.FTE_OPERATION_VALUES;
    private static final String URL_FOV = RELEASE_URL + SECTION.getUrlEnding();

    @Inject
    private FteOperationValuesPage fvp;

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriver() {
        fvp.getWrappedDriver().close();
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void driverConfig() {
        fvp.getWrappedDriver().manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    @Step("Перейти в раздел \"Результат расчета рабочей нагрузки\"")
    private void goToFteOperationValuesPage() {
        new GoToPageSection(fvp).getPage(SECTION, 60);
    }

    @Step("Нажать на стрелку, раскрывающую список ОМ")
    private void pressOnExpandOmButtons() {
        fvp.filterPanel().openOrgUnitListButton().click();
    }

    @Step("Выбрать оргюнит: {orgUnit.name}")
    private void chooseOrgUnit(OrgUnit orgUnit) {
        String orgUnitName = orgUnit.getName();
        fvp.filterPanel().searchOrgUnitInput().clear();
        slowSendKeys(fvp.filterPanel().searchOrgUnitInput(), orgUnit.getName());
        fvp.filterPanel().orgNameButton(orgUnitName)
                .waitUntil("Введенный оргюнит не отображается в поиске", DisplayedMatcher.displayed(), 100);
        fvp.filterPanel().orgNameButton(orgUnitName).click();
    }

    @Step("Выбрать {calendarButtons.dateType} {localDate}")
    private void pickDateFilter(DateTypeField dateTypeField, LocalDate localDate) {
        fvp.filterPanel().filterCalendarButton(dateTypeField.getName()).click();
        DatePicker datePicker = new DatePicker(fvp.datePickerForm());
        datePicker.pickDate(localDate);
        datePicker.okButtonClick();
    }

    @Step("Нажать на кнопку фильтрации {filterType.typeName}")
    private void pressOnFilterButton(FilterType filterType) {
        fvp.filterPanel().filterChevronButton(filterType.getTypeNumber()).click();
    }

    @Step("В фильтре \"Типы событий\" выбрать чекбокс {type}")
    private void pickEventType(String type) {
        fvp.eventTypeForm().checkboxByType(type).click();
        fvp.eventTypeForm().applyFilterButton().click();
    }

    @Step("В фильтре \"KPI\" выбрать чекбокс {kpiName}")
    private void pickKpiType(String kpiName) {
        fvp.kpiTypeForm().checkboxByKpiName(kpiName).click();
        fvp.kpiTypeForm().applyFilterButton().click();
    }

    @Step("В фильтре \"Функциональные роли\" выбрать чекбокс {roleName}")
    private void pickRoleType(String roleName) {
        fvp.functionalRoleForm().checkboxByRoleName(roleName).click();
        fvp.functionalRoleForm().applyFilterButton().click();
    }

    private List<String> getKpiAndEventOnTable() {
        return fvp.tableForm().allKpiAndEvents().stream().map(AtlasWebElement::getText).collect(Collectors.toList());
    }

    @Step("Проверка фильтрации KPI : {filterKpiName} ")
    private SoftAssert assertKpiAfterFilter(FteOperationValuesModel model, KpiList filterKpiName, SoftAssert softAssert) {
        List<String> kpiOnTableAfter = getKpiAndEventOnTable();
        kpiOnTableAfter.removeAll(new ArrayList<>(CommonRepository.getEventTypes().values()));
        if (kpiOnTableAfter.contains(filterKpiName.getName())) {
            softAssert.assertEquals(kpiOnTableAfter.size(), 1, "После фильтрации осталось больше одного KPI");
            softAssert.assertTrue(kpiOnTableAfter.contains(filterKpiName.getName()), "KPI в таблице: " + kpiOnTableAfter
                    + ", а должен быть: " + filterKpiName);
        } else {
            softAssert.assertTrue(kpiOnTableAfter.isEmpty(),
                                  "После фильтрации должна была остаться пустая таблица, но в таблице отображаются: " + kpiOnTableAfter);
        }
        softAssert.assertEquals(kpiOnTableAfter, model.getKpiList().stream().map(KpiList::getName).collect(Collectors.toList()),
                                "Списки KPI на UI и в API не совпали");
        return softAssert;
    }

    @Step("Проверка фильтрации по событию : {filterEventName} ")
    private SoftAssert assertEventAfterFilter(FteOperationValuesModel model, String filterEventName, SoftAssert softAssert) {
        List<String> eventOnTable = getKpiAndEventOnTable();
        eventOnTable.removeAll(KpiListRepository.getKpiTypes().stream().map(KpiList::getName).collect(Collectors.toList()));
        if (eventOnTable.contains(filterEventName)) {
            softAssert.assertEquals(eventOnTable.size(), 1, "После фильтрации осталось больше одного типа эвента");
            softAssert.assertTrue(eventOnTable.contains(filterEventName), "Тип эвента в таблице: " + eventOnTable
                    + ", а должен быть: " + filterEventName);
        } else {
            softAssert.assertTrue(eventOnTable.isEmpty(),
                                  "После фильтрации должна была остаться пустая таблица, но в таблице отображаются: " + eventOnTable);
        }
        softAssert.assertEquals(eventOnTable, model.getEventNames(), "Списки событий на UI и в API не совпали");
        return softAssert;
    }

    @Step("Проверка фильтрации по Функциональной роли : {roleName}")
    private SoftAssert assertRoleAfterFilter(FteOperationValuesModel model, String roleName, SoftAssert softAssert) {
        Set<String> allRolesOnTable = fvp.tableForm().allRoles().stream()
                .map(AtlasWebElement::getText).filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        softAssert.assertEquals(allRolesOnTable.size(), 1);
        softAssert.assertTrue(allRolesOnTable.contains(roleName));
        softAssert.assertEquals(allRolesOnTable, model.getFunctionalRoles(), "Списки ролей на UI и в API не совпали");
        return softAssert;
    }

    @Step("Проверка выгрузки таблицы при наличии всех фильтров.")
    private void assertAllFilters(OrgUnit orgUnit, DateInterval dateInterval, String eventType, KpiList randomKpi, String roleName) {
        FteOperationValuesModel model = FteOperationValuesRepository.getAndSetFteOperationValues(orgUnit, FteOperationValuesRepository.getPairsFormParamNames(dateInterval, eventType, randomKpi, roleName));
        SoftAssert softAssert = new SoftAssert();
        assertDate(dateInterval, softAssert);
        assertEventAfterFilter(model, eventType, softAssert);
        assertKpiAfterFilter(model, randomKpi, softAssert);
        assertRoleAfterFilter(model, roleName, softAssert);
        softAssert.assertAll();
    }

    @Step("Проверка того что в даты в таблице находятся в указанном диапазоне")
    private SoftAssert assertDate(DateInterval dateInterval, SoftAssert softAssert) {
        List<LocalDate> uiDateList = fvp.tableForm().allDates().stream().map(AtlasWebElement::getText)
                .map(s -> LocalDate.parse(s, Format.API_KPI_VALUE.getFormat())).collect(Collectors.toList());
        List<LocalDate> expectedDateList = dateInterval.getBetweenDatesList();
        softAssert.assertTrue(expectedDateList.containsAll(uiDateList),
                              "На UI отображаются даты которых нет в указанном диапазоне");
        return softAssert;
    }

    @Step("Нажать на кнопку сброса фильтров")
    private void pressResetFilterButton() {
        fvp.filterPanel().resetButton().click();
    }

    @Step("Проверка что поле \"Даты окончания\" подсвечивается красным.")
    private void assertCantPickWrongDate(LocalDate startDate) {
        String date = startDate.toString();
        fvp.filterPanel().endDateError().should("Красная подсветка строки ввода даты",
                                                text(containsString("Должна быть не ранее, чем " + date + " 24:00:00")), 5);
    }

    @Step("Проверка что фильтры сбросились и таблица не отображается.")
    private void assertResetFilters() {
        fvp.tableForm().should("Таблица отображется после сброса всех фильтров.",
                               Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Test(groups = {"ABCHR2656-1", "TEST-1065"}, description = "Расчет рабочей нагрузки с применением всех фильтров")
    public void calculationWorkloadWithAllFilters() {
        goToFteOperationValuesPage();
        ImmutablePair<OrgUnit, FteOperationValuesModel> orgUnitValues = OrgUnitRepository.getRandomOrgUnitWithFteGroup(FilterType.EVENT_AND_KPI);
        pressOnExpandOmButtons();
        chooseOrgUnit(orgUnitValues.left);
        DateInterval dateInterval = orgUnitValues.right.getRandomDatesWithFte();
        pickDateFilter(DateTypeField.START_DATE, dateInterval.startDate);
        pickDateFilter(DateTypeField.END_DATE, dateInterval.endDate);
        String eventType = getRandomFromList(orgUnitValues.right.getEventNames());
        pressOnFilterButton(FilterType.EVENT_TYPE);
        pickEventType(eventType);
        List<KpiList> kpiInUnit = orgUnitValues.right.getKpiList();
        KpiList randomKpi = getRandomFromList(kpiInUnit);
        pressOnFilterButton(FilterType.KPI);
        pickKpiType(randomKpi.getName());
        String randomRoleName = getRandomFromList(orgUnitValues.right.getFunctionalRoles());
        pressOnFilterButton(FilterType.FUNCTIONAL_ROLE);
        pickRoleType(randomRoleName);
        assertAllFilters(orgUnitValues.left, dateInterval, eventType, randomKpi, randomRoleName);
    }

    @Test(groups = {"ABCHR2656-2", "TEST-1065"}, description = "Расчет рабочей нагрузки с применением фильтра \"Типы событий\"")
    public void calculationWorkloadWithTypeFilter() {
        goToFteOperationValuesPage();
        ImmutablePair<OrgUnit, FteOperationValuesModel> orgUnitValues = OrgUnitRepository.getRandomOrgUnitWithFteGroup(FilterType.EVENT_TYPE);
        pressOnExpandOmButtons();
        chooseOrgUnit(orgUnitValues.left);
        DateInterval dateInterval = orgUnitValues.right.getRandomDatesWithFte();
        pickDateFilter(DateTypeField.START_DATE, dateInterval.startDate);
        pickDateFilter(DateTypeField.END_DATE, dateInterval.endDate);
        String eventType = getRandomFromList(orgUnitValues.right.getEventNames());
        pressOnFilterButton(FilterType.EVENT_TYPE);
        pickEventType(eventType);
        FteOperationValuesModel model = FteOperationValuesRepository.getAndSetFteOperationValues(orgUnitValues.left,
                                                                                                 FteOperationValuesRepository.getPairsFormParamNames(dateInterval, eventType, null, null));
        assertEventAfterFilter(model, eventType, new SoftAssert()).assertAll();
    }

    @Test(groups = {"ABCHR2656-3", "TEST-1065"}, description = "Расчет рабочей нагрузки с применением фильтра \"KPI\"")
    public void calculationWorkloadWithKpiFilter() {
        goToFteOperationValuesPage();
        ImmutablePair<OrgUnit, FteOperationValuesModel> orgUnitValues = OrgUnitRepository.getRandomOrgUnitWithFteGroup(FilterType.KPI);
        pressOnExpandOmButtons();
        chooseOrgUnit(orgUnitValues.left);
        DateInterval dateInterval = orgUnitValues.right.getRandomDatesWithFte();
        pickDateFilter(DateTypeField.START_DATE, dateInterval.startDate);
        pickDateFilter(DateTypeField.END_DATE, dateInterval.endDate);
        pressOnFilterButton(FilterType.KPI);
        List<KpiList> kpiInUnit = orgUnitValues.right.getKpiList();
        KpiList randomKpi = getRandomFromList(kpiInUnit);
        pickKpiType(randomKpi.getName());
        FteOperationValuesModel model = FteOperationValuesRepository.getAndSetFteOperationValues(orgUnitValues.left,
                                                                                                 FteOperationValuesRepository.getPairsFormParamNames(dateInterval, null, randomKpi, null));
        assertKpiAfterFilter(model, randomKpi, new SoftAssert()).assertAll();
    }

    @Test(groups = {"ABCHR2656-4", "TEST-1065"}, description = "Расчет рабочей нагрузки с применением фильтра \"Функциональные роли\"")
    public void calculationWorkloadWithRoleFilter() {
        goToFteOperationValuesPage();
        ImmutablePair<OrgUnit, FteOperationValuesModel> orgUnitValues = OrgUnitRepository.getRandomOrgUnitWithFteGroup(FilterType.FUNCTIONAL_ROLE);
        pressOnExpandOmButtons();
        chooseOrgUnit(orgUnitValues.left);
        DateInterval dateInterval = orgUnitValues.right.getRandomDatesWithFte();
        pickDateFilter(DateTypeField.START_DATE, dateInterval.startDate);
        pickDateFilter(DateTypeField.END_DATE, dateInterval.endDate);
        pressOnFilterButton(FilterType.FUNCTIONAL_ROLE);
        String randomRoleName = getRandomFromList(orgUnitValues.right.getFunctionalRoles());
        pickRoleType(randomRoleName);
        FteOperationValuesModel model = FteOperationValuesRepository.getAndSetFteOperationValues(orgUnitValues.left,
                                                                                                 FteOperationValuesRepository.getPairsFormParamNames(dateInterval, null, null, randomRoleName));
        assertRoleAfterFilter(model, randomRoleName, new SoftAssert()).assertAll();
    }

    @Test(groups = {"ABCHR2656-5", "TEST-1065"},
            description = "Расчет рабочей нагрузки с выбором даты окончания периода расчета раньше даты начала")
    public void calculationWorkloadWithEndDateBeforeStartDate() {
        goToFteOperationValuesPage();
        ImmutablePair<OrgUnit, FteOperationValuesModel> orgUnitValues = OrgUnitRepository.getRandomOrgUnitWithFteGroup(FilterType.FUNCTIONAL_ROLE);
        pressOnExpandOmButtons();
        chooseOrgUnit(orgUnitValues.left);
        DateInterval dateInterval = orgUnitValues.right.getRandomDatesWithFte();
        pickDateFilter(DateTypeField.START_DATE, dateInterval.endDate);
        pickDateFilter(DateTypeField.END_DATE, dateInterval.startDate);
        assertCantPickWrongDate(dateInterval.getEndDate());
    }

    @Test(groups = {"ABCHR2656-6", "TEST-1065"}, description = "Сброс фильтров при расчете рабочей нагрузки")
    public void calculationWorkloadReset() {
        goToFteOperationValuesPage();
        ImmutablePair<OrgUnit, FteOperationValuesModel> orgUnitValues = OrgUnitRepository.getRandomOrgUnitWithFteGroup(FilterType.EVENT_AND_KPI);
        pressOnExpandOmButtons();
        chooseOrgUnit(orgUnitValues.left);
        DateInterval dateInterval = orgUnitValues.right.getRandomDatesWithFte();
        pickDateFilter(DateTypeField.START_DATE, dateInterval.startDate);
        pickDateFilter(DateTypeField.END_DATE, dateInterval.endDate);
        String eventType = getRandomFromList(orgUnitValues.right.getEventNames());
        pressOnFilterButton(FilterType.EVENT_TYPE);
        pickEventType(eventType);
        List<KpiList> kpiInUnit = orgUnitValues.right.getKpiList();
        KpiList randomKpi = getRandomFromList(kpiInUnit);
        pressOnFilterButton(FilterType.KPI);
        pickKpiType(randomKpi.getName());
        String randomRoleName = getRandomFromList(orgUnitValues.right.getFunctionalRoles());
        pressOnFilterButton(FilterType.FUNCTIONAL_ROLE);
        pickRoleType(randomRoleName);
        pressResetFilterButton();
        assertResetFilters();
    }
}
