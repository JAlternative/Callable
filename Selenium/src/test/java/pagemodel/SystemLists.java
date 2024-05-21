package pagemodel;

import com.google.inject.Inject;
import guice.TestModule;
import io.qameta.allure.*;
import io.qameta.allure.junit4.Tag;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.SystemListsPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.BaseTest;
import testutils.GoToPageSection;
import utils.Links;
import wfm.ApiRequest;
import wfm.PresetClass;
import wfm.components.orgstructure.OrganizationUnitTypeId;
import wfm.components.utils.Section;
import wfm.models.PositionGroup;
import wfm.models.ShiftEditReason;
import wfm.repository.PositionGroupRepository;
import wfm.repository.ShiftEditReasonRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static common.Groups.*;
import static utils.Links.*;
import static utils.tools.CustomTools.systemSleep;
import static utils.tools.RequestFormers.makePath;
import static wfm.repository.CommonRepository.URL_BASE;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class SystemLists extends BaseTest {

    private static final String RELEASE_URL = Links.getTestProperty("release");
    private static final Section SECTION = Section.SYSTEM_LISTS;
    private static final String POS_GROUP_PREFIX = "PosGroup";

    @Inject
    private SystemListsPage sl;

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriver() {
        closeDriver(sl.getWrappedDriver());
    }

    @AfterGroups(value = {"TK2686-1", "TK2686-3"}, alwaysRun = true)
    public void clean() {
        afterTest();
    }

    @AfterMethod(alwaysRun = true, description = "Очистить куки и подготовить новую вкладку")
    private void cleanCookies() {
        cleanCookiesAndOpenNewTab(sl.getWrappedDriver());
    }

    @AfterMethod(alwaysRun = true, onlyForGroups = {"@After remove added hidden functionality"})
    public void removeAddedHiddenFunctionality(ITestContext c) {
        List<String> attributes = c.getAttributeNames().stream()
                .filter(a -> a.startsWith(POS_GROUP_PREFIX))
                .collect(Collectors.toList());
        if (!attributes.isEmpty()) {
            for (String attribute : attributes) {
                int id = (int) c.getAttribute(attribute);
                PresetClass.hideFunctionality(PositionGroupRepository.getPositionGroupById(id), false);
            }
        }
    }

    @AfterMethod(alwaysRun = true, onlyForGroups = {"@After delete added orgunittype"})
    public void removeAddedOrgUnitType(ITestContext c) {
        List<String> attributes = c.getAttributeNames().stream()
                .filter(a -> a.startsWith("self_orgunittype_link"))
                .collect(Collectors.toList());
        if (!attributes.isEmpty()) {
            for (String attribute : attributes) {
                String link = (String) c.getAttribute(attribute);
                PresetClass.deleteRequest(link);
            }
        }
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void driverConfig() {
        setBrowserTimeout(sl.getWrappedDriver(), 30);
    }

    private void goToSystemListsPage() {
        new GoToPageSection(sl).getPage(SECTION, 60);
    }

    @Step("Удалить тестовый комментарий пресетом")
    private void afterTest() {
        PresetClass.deleteAllShiftEditReasonsMatchesText("тестовый");
    }

    @Step("Раскрыть список в поле \"Список\"")
    private void openListsList() {
        sl.layout().inputSystemLists().click();
        sl.layout().allElementsInList().waitUntil("Названия системных списков не прогрузились", Matchers.hasSize(Matchers.greaterThan(0)), 5); //метод используется в неактуальных тестах
    }

    @Step("Пресет. Добавить тестовый комментарий")
    private void createTestShiftComment(String name) {
        PresetClass.createShiftEditReason(name);
    }

    @Step("Выбрать \"Комментарии к сменам\"")
    private void selectShiftComment() {
        sl.layout().shiftCommentButton().click();
    }

    @Step("Нажать кнопку создания нового комментария (Красный круг с плюсом)")
    private void clickPlusButton() {
        sl.layout().plusButton().click();
    }

    @Step("Ввести значение \"{value}\" в поле \"Код\"")
    private void enterCodeValue(String value) {
        sl.addingPanel().codeInputField().click();
        sl.addingPanel().codeInputField().clear();
        sl.addingPanel().codeInputField().sendKeys(value);
    }

    @Step("Нажать \"Создать\"")
    private void clickSaveButton() {
        sl.addingPanel().saveButton().click();
    }

    @Step("Нажать \"Изменить\"")
    private void clickChangeButton() {
        sl.addingPanel().saveButton().click();
    }

    @Step("Нажать \"Удалить\"")
    private void clickDeleteButton() {
        sl.addingPanel().removeButton().click();
    }

    @Step("Ввести значение \"{value}\" в поле \"Название\"")
    private void enterNameValue(String value) {
        sl.addingPanel().nameInputField().click();
        sl.addingPanel().nameInputField().clear();
        sl.addingPanel().nameInputField().sendKeys(value);
    }

    @Step("Кликнуть комментарию из таблицы с названием \"{name}\"")
    private void clickOnComment(String name) {
        sl.shiftCommentTable().commentByName(name).click();
    }

    @Step("Пресет. Изменить чекбокс \"Скрывать в системных списках\" случайному элементу из списка \"{posGroup.name}\" на \"{value}\"")
    private void hiddenFunctionality(PositionGroup posGroup, boolean value) {
        PresetClass.hideFunctionality(posGroup, value);
        Reporter.getCurrentTestResult().getTestContext().setAttribute(POS_GROUP_PREFIX + posGroup.getId(), posGroup.getId());
    }

    @Step("Выбрать список \"{value}\"")
    private void selectElementList(String value) {
        sl.layout().elementList(value).click();
    }

    @Step("Раскрыть выпадающий список \"Функциональная роль\"")
    private void clickOnFieldFunctionalRoles() {
        sl.addingPanel().functionalRoles().click();
    }

    @Step("Проверить наличие комментария к сменам с именем {name}, кодом {code}")
    private void assertShiftEditReason(String name, String code) {
        systemSleep(2); //метод используется в неактуальных тестах
        Assert.assertTrue(sl.shiftCommentTable().namesInTable().stream()
                                  .anyMatch(extendedWebElement -> extendedWebElement.getText().trim().equals(name)),
                          "Не был добавлен комментарий с именем: " + name);
        Assert.assertTrue(sl.shiftCommentTable().codesInTable().stream()
                                  .anyMatch(extendedWebElement -> extendedWebElement.getText().trim().equals(code)),
                          "Не был добавлен комментарий с кодом: " + code);
    }

    @Step("Проверить остутствие комментария к сменам с именем {name}")
    private void assertDeletingShiftReason(String name) {
        sl.shiftCommentTable().commentByName(name).waitUntil("Комментарий не был удален.",
                                                             Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Проверить, что группа должностей {value} не отображается")
    private void assertSelectedPositionGroupNotDisplayed(String value) {
        boolean valueOfDisplayed = sl.addingPanel().getFunctionalRolesDropdownOptions()
                .stream().map(v -> v.getText())
                .anyMatch(p -> p.equals(value));
        Assert.assertFalse(valueOfDisplayed, "Выбранная группа должностей '"
                + value + "' отображается в выпадающем списке");
    }

    private OrganizationUnitTypeId sendRequestToCreateOrgUnitType(JSONObject orgUnitTypeToImport) {
        new ApiRequest.PostBuilder(makePath(URL_BASE, API_V1, INTEGRATION_JSON, ORGANIZATION_UNIT_TYPES))
                .withBody("[" + orgUnitTypeToImport + "]")
                .withStatus(200)
                .send();
        return OrganizationUnitTypeId.getLowest();
    }

    @Step("Проверить, что тип подразделения {organizationUnitTypeId.name} был успешно импортирован в WFM")
    private void assertOrgUnitTypeAdded(OrganizationUnitTypeId organizationUnitTypeId, JSONObject importedOrgUnitType, List<OrganizationUnitTypeId> before) {
        List<OrganizationUnitTypeId> after = OrganizationUnitTypeId.getAllOrgUnitTypes();
        Allure.addAttachment("Список типов подразделения до импорта", before.stream().map(OrganizationUnitTypeId::getName).collect(Collectors.toList()).toString());
        Allure.addAttachment("Список типов подразделения после импорта", after.stream().map(OrganizationUnitTypeId::getName).collect(Collectors.toList()).toString());
        String orgUnitTypeName = organizationUnitTypeId.getName();
        int orgUnitTypeLevel = organizationUnitTypeId.getLevel();
        String orgUnitTypeOuterId = organizationUnitTypeId.getOuterId();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(after.size(), before.size() + 1, "Размер списка типов подразделений не увеличился");
        softAssert.assertEquals(orgUnitTypeName, importedOrgUnitType.getString("name"), "Название типа подразделения в API не совпадает с импортированным");
        softAssert.assertEquals(orgUnitTypeLevel, importedOrgUnitType.getInt("level"), "Уровень типа подразделения в API не совпадает с импортированным");
        softAssert.assertEquals(orgUnitTypeOuterId, importedOrgUnitType.getString("outerId"), "OuterId типа подразделения в API не совпадает с импортированным");
        sl.shiftCommentTable().allRowsInTable().waitUntil("В системном списке не отобразились типы подразделений", Matchers.hasSize(Matchers.greaterThan(0)), 5);
        ElementsCollection<AtlasWebElement> rowWithOrgUnitType = sl.shiftCommentTable().allRowFieldsByOneRow(organizationUnitTypeId.getName())
                .waitUntil("В системном списке на UI не отобразился тип подразделения с названием " + organizationUnitTypeId.getName(), Matchers.hasSize(Matchers.greaterThan(0)), 5);
        softAssert.assertEquals(rowWithOrgUnitType.get(0).getText(), orgUnitTypeOuterId,
                                  "Отображаемый на UI outerId типа подразделения в системном списке не совпадает с импортированным");
        softAssert.assertEquals(rowWithOrgUnitType.get(2).getText(), String.valueOf(orgUnitTypeLevel),
                                  "Отображаемый на UI уровень типа подразделения в системном списке не совпадает с импортированным");
        Allure.addAttachment("Результат проверки", String.format("Тип подразделения %s успешно импортирован в WFM", orgUnitTypeName));
        softAssert.assertAll();
    }

    @Test(groups = {"TK2686-1", "TEST-950"}, description = "Создание нового комментария")
    private void addingShiftEditReason() {
        goToSystemListsPage();
        openListsList();
        selectShiftComment();
        clickPlusButton();
        String randomName = "тестовый_" + RandomStringUtils.randomAlphabetic(10);
        String code = "CODE_" + randomName.toUpperCase();
        enterNameValue(randomName);
        enterCodeValue(code);
        clickSaveButton();
        assertShiftEditReason(randomName, code);
    }

    @Test(groups = {"TK2686-2", "TEST-950"}, description = "Удаление комментария")
    private void deletingShiftEditReason() {
        String name = "тестовый комментарий";
        createTestShiftComment(name);
        ShiftEditReason reason = ShiftEditReasonRepository.getShiftEditReasonByName(name);
        goToSystemListsPage();
        openListsList();
        selectShiftComment();
        clickOnComment(reason.getTitle());
        clickDeleteButton();
        assertDeletingShiftReason(reason.getTitle());
    }

    @Test(groups = {"TK2686-3", "TEST-950"}, description = "Редактирование комментария")
    private void editingShiftEditReason() {
        String name = "тестовый комментарий";
        createTestShiftComment(name);
        ShiftEditReason reason = ShiftEditReasonRepository.getShiftEditReasonByName(name);
        goToSystemListsPage();
        openListsList();
        selectShiftComment();
        clickOnComment(reason.getTitle());
        String randomName = "тестовый_" + RandomStringUtils.randomAlphabetic(10);
        String code = "CODE_" + randomName.toUpperCase();
        enterNameValue(randomName);
        enterCodeValue(code);
        clickChangeButton();
        assertShiftEditReason(randomName, code);
    }

    @Test(groups = {"ABCHR-6494", OTHER4, MAGNIT, "@After remove added hidden functionality"},
            description = "Отображение групп должностей в системных списках")
    @Link(name = "Статья: \"Свободные смены. Биржа смен\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=256465598")
    @TmsLink("96634")
    @Owner(KHOROSHKOV)
    @Tag("ABCHR-6494")
    @Tag(OTHER4)
    private void displayPositionGroupsInSystemLists() {
        PositionGroup posGroup = PositionGroupRepository.randomPositionGroup();
        if (!posGroup.isHidden()) {
            hiddenFunctionality(posGroup, true);
        }
        goToSystemListsPage();
        openListsList();
        selectElementList("Перерывы");
        clickPlusButton();
        clickOnFieldFunctionalRoles();
        assertSelectedPositionGroupNotDisplayed(posGroup.getName());
    }

    @Test(groups = {"ABCHR5857-1", G2, LIST15, MAGNIT, "@After delete added orgunittype"},
            description = "[Интеграция Магнит] механизм импорта справочника типов организаций")
    @Link(name = "Статья: \"5857_[Интеграция Магнит] механизм импорта справочника типов организаций\"",
            url = "https://wiki.goodt.me/x/nhtZDg")
    @TmsLink("118296")
    private void importOrgUnitTypes() {
        addTag("ABCHR5857-1"); //теги через аннотацию не добавлялись в отчёт
        addTag(LIST15);
        List<OrganizationUnitTypeId> orgUnitTypesBefore = OrganizationUnitTypeId.getAllOrgUnitTypes();
        JSONObject orgUnitType = new JSONObject();
        int orgUnitTypeLevel = OrganizationUnitTypeId.getLowest().getLevel() + 1000;
        String orgUnitTypeName = "test_OrgUnitType_" + orgUnitTypeLevel;
        orgUnitType.put("level", orgUnitTypeLevel);
        orgUnitType.put("name", orgUnitTypeName);
        orgUnitType.put("outerId", String.valueOf(orgUnitTypeLevel));
        OrganizationUnitTypeId organizationUnitTypeId = sendRequestToCreateOrgUnitType(orgUnitType);
        Reporter.getCurrentTestResult()
                .getTestContext().setAttribute("self_orgunittype_link", makePath(URL_BASE, API_V1, ORGANIZATION_UNIT_TYPES, organizationUnitTypeId.getId()));
        goToSystemListsPage();
        openListsList();
        selectElementList("Типы подразделений");
        assertOrgUnitTypeAdded(organizationUnitTypeId, orgUnitType, orgUnitTypesBefore);
    }
}
