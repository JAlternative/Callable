package pagemodel;

import com.google.inject.Inject;
import guice.TestModule;
import io.qameta.allure.Step;
import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.PositionTypesPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.GoToPageSection;
import testutils.RoleWithCookies;
import utils.Links;
import utils.Projects;
import wfm.PresetClass;
import wfm.components.positiontypes.PosTypeTripleButton;
import wfm.components.positiontypes.PositionParameters;
import wfm.components.utils.Role;
import wfm.components.utils.Section;
import wfm.models.MathParameter;
import wfm.repository.CommonRepository;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.RequestFormers.*;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class PositionTypes {

    private static final String RELEASE_URL = Links.getTestProperty("release");
    private static final Section SECTION = Section.POSITION_TYPES;
    private static final String URL_PT = RELEASE_URL + SECTION.getUrlEnding();
    private static final String TEST_NAME = "test";
    @Inject
    private PositionTypesPage pt;

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriver() {
        pt.getWrappedDriver().close();
    }

    @AfterGroups(value = {"TK2052-2.1.1", "TK2052-2.2.1", "TK2052-4.1"})
    private void clean() {
        PresetClass.deleteTestPositionType(TEST_NAME);
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void driverConfig() {
        pt.getWrappedDriver().manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    @Step("Перейти в раздел \"Типы позиций\"")
    private void goToPositionTypesPage() {
        new GoToPageSection(pt).getPage(SECTION, 60);
    }

    @Step("Перейти в раздел \"Типы позиций\" с ролью \"{role.name}\"")
    private void goToPositionTypesPageAsUser(Role role) {
        new RoleWithCookies(pt.getWrappedDriver(), role).getPage(SECTION);
        pt.spinner().grayLoadingBackground().waitUntil("Спиннер не исчез", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    private String getRandomPositionNameFromUI() {
        List<String> allPositionNames = pt.table().allPositionNames().stream().
                map(webElement -> webElement.getText().trim()).collect(Collectors.toList());
        return getRandomFromList(allPositionNames).trim();
    }

    /**
     * Выбирает случайный тип позиции за исключением указанной
     *
     * @param firstPositionTypeName - имя позиции для исключения из выбора
     */
    private String getRandomPositionNameFromUI(String firstPositionTypeName) {
        List<String> allPositionNames = pt.table().allPositionNames().stream().
                map(webElement -> webElement.getText().trim()).collect(Collectors.toList());
        allPositionNames.remove(firstPositionTypeName);
        return getRandomFromList(allPositionNames);
    }

    @Step("Нажать на троеточие типа позиции с именем \"{positionName}\"")
    private void clickTripleDotByName(String positionName) {
        pt.table().tripleDotByName(positionName).click();
    }

    @Step("Перетащить позицию с именем \"{positionName}\" на место позиции \"{anotherPosition}\"")
    private void dragPositionOnAnother(String positionName, String anotherPosition) {
        Actions actions = new Actions(pt.getWrappedDriver());
        actions.moveToElement(pt.table().dragButtonByName(positionName))
                .clickAndHold(pt.table().dragButtonByName(positionName))
                .moveByOffset(-5, -5)
                .moveToElement(pt.table().dragButtonByName(anotherPosition))
                .release()
                .build()
                .perform();
    }

    @Step("Нажать на кнопку \"{variant.variant}\" в меню троеточия")
    private void clickVariantInTripleButton(PosTypeTripleButton variant) {
        pt.table().inTripleDotButton(variant.getVariant()).click();
        pt.spinner().grayLoadingBackground().waitUntil("Спиннер не исчез", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Проверка того что доступен просмотр данных типа позиции")
    private void assertAccessDataView(String positionName) {
        pt.dialogForm().should("Окно просмотра не открылось", DisplayedMatcher.displayed(), 5);
        pt.dialogForm().positionTypeName()
                .should("Перешли не в ту позицию или не в тот раздел", text(containsString(positionName)), 5);
    }

    @Step("Ввод в поле \"Название типа\" текста: {typeName}")
    private void sendPositionTypeName(String typeName) {
        pt.dialogForm().inputTypePositionName().clear();
        pt.dialogForm().inputTypePositionName().sendKeys(typeName);
    }

    @Step("Нажать на кнопку \"Сохранить\"")
    private void pressSaveButton() {
        pt.dialogForm().saveButton().click();
        pt.spinner().grayLoadingBackground().waitUntil("Спиннер не исчез", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Проверка того что в меню со списком доступных действий пункта \"{variant.variant}\" в списке нет")
    private void assertButtonNotExist(PosTypeTripleButton variant) {
        pt.table().inTripleDotButton(variant.getVariant())
                .should("Пункт \"" + variant.getVariant() + "\" есть в списке", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Проверка изменения названия или создания типа позиции на имя: {name}")
    private void assertEditOrCreatePositionType(String name, Map<String, Integer> positionTypesBefore, boolean shouldBeEdited) {
        SoftAssert softAssert = new SoftAssert();
        Map<String, Integer> positionTypesAfter = CommonRepository.getAllPositionTypesForPositionType();
        if (shouldBeEdited) {
            softAssert.assertEquals(positionTypesAfter.size(), positionTypesBefore.size(),
                                    "Добавилось новое значение при редактировании имени");
        } else {
            softAssert.assertEquals(positionTypesAfter.size(), positionTypesBefore.size() + 1,
                                    "В апи не добавился новый тип позиции");
        }
        softAssert.assertTrue(positionTypesAfter.containsKey(name), "В API имя типа позиции не отобразилось");
        List<String> positionTypeNameList = pt.table().allPositionNames().stream()
                .map(webElement -> webElement.getText().trim()).collect(Collectors.toList());
        softAssert.assertTrue(positionTypeNameList.contains(name), "В таблице имя типа позиции не отобразилось");
        PresetClass.deleteTestPositionType(name);
        softAssert.assertAll();
    }

    @Step("Ввести значение {value} в параметр \"{parameterName.parameterName}\"")
    private void sendValueInParameter(PositionParameters parameterName, int value) {
        pt.dialogForm().inputParameter(parameterName.getParameterName()).sendKeys(String.valueOf(value));
    }

    @Step("Проверка изменения параметра \"{parameter.parameterName}\" типа позиции \"{positionTypeName}\"")
    private void assertChangePositionTypeParameter(PositionParameters parameter, int value, String positionTypeName) {
        String parameterName = parameter.getParameterName();
        SoftAssert softAssert = new SoftAssert();
        List<String> allParameters = pt.dialogForm().allParametersInView().stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
        String targetParam = allParameters.stream().filter(s -> s.contains(parameterName)).findFirst()
                .orElseThrow(() -> new AssertionError("Не был найден измененый параметр"));
        int valueOnUi = Integer.parseInt(targetParam.substring(0, targetParam.indexOf("\n")));
        softAssert.assertEquals(valueOnUi, value, "Значение на UI не соответствует тому на которое мы изменяли");
        int positionTypeId = CommonRepository.getAllPositionTypesForPositionType().get(positionTypeName);
        String path = makePath(POSITION_TYPES, positionTypeId, MATH_PARAMETER_VALUES);
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, URL_PT, path);
        JSONArray mathArray = jsonObject.getJSONObject(EMBEDDED).getJSONArray(REL_MATH_PARAMETER_VALUES);

        boolean weFindThisParameterAndValue = false;
        for (int i = 0; i < mathArray.length(); i++) {
            JSONObject mathType = mathArray.getJSONObject(i);
            int valueParam = mathType.getInt("value");
            if (valueParam == value) {
                String mathLink = mathType.getJSONObject(LINKS).getJSONObject("mathParameter").getString(HREF);
                JSONObject mathParameter = new JSONObject(setUrlAndInitiateForApi(URI.create(mathLink), Projects.WFM));
                MathParameter tempParameter = new MathParameter(mathParameter);
                if (parameterName.equals(tempParameter.getName()) || parameterName.equals(tempParameter.getCommonName())
                        || parameterName.equals(tempParameter.getShortName())) {
                    weFindThisParameterAndValue = true;
                }
            }
        }
        softAssert.assertTrue(weFindThisParameterAndValue, "Не нашли у типа позиции \"" + positionTypeName
                + "\" мат параметра: " + parameterName + ", со значением " + value);
        //добавил чтобы удаление ушло в отчет.
        PresetClass.deleteTestPositionType(positionTypeName);
        softAssert.assertAll();
    }

    @Step("Проверка того что красная кнопка \"+\" не отображается")
    private void assertRedButtonIsNotDisplayed() {
        pt.table().redPlusButton()
                .should("Красная кнопка \"+\" отображается", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Нажать на красную кнопку \"+\"")
    private void pressOnRedPlusButton() {
        pt.table().redPlusButton().click();
    }

    @Step("Проверка удаления типа позиции с именем: {deletedPositions}")
    private void assertDeletePositions(String deletedPositions, Map<String, Integer> positionTypesBefore) {
        SoftAssert softAssert = new SoftAssert();
        pt.table().popUP().should("Поп-ап с сообщением \"Тип удален\" не отобразился ", text(containsString("Тип удален")), 5);
        Map<String, Integer> positionTypesAfter = CommonRepository.getAllPositionTypesForPositionType();
        softAssert.assertEquals(positionTypesAfter.size(), positionTypesBefore.size() - 1,
                                "Тип позиции не удалился");
        softAssert.assertFalse(positionTypesAfter.containsKey(deletedPositions),
                               "В API не исчезло удаленное имя типа позиции");
        List<String> positionTypeNameList = pt.table().allPositionNames().stream()
                .map(webElement -> webElement.getText().trim()).collect(Collectors.toList());
        softAssert.assertFalse(positionTypeNameList.contains(deletedPositions),
                               "В таблице отображается имя удаленной позиции");
        softAssert.assertAll();
    }

    @Step("Проверка того что кнопки перемещения рядом с названием типа позиции нет")
    private void assertTransferNotEnabled(String randomPositionNameFromUI) {
        pt.table().dragButtonByName(randomPositionNameFromUI)
                .should("Кнопка перемещения панелей отображается", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Test(groups = {"TK2052-1.2.1", "TEST-900"}, description = "Просмотр данных типа позиции")
    private void viewDataPositionTypeRole1() {
        goToPositionTypesPageAsUser(Role.FIRST);
        String positionName = getRandomPositionNameFromUI();
        clickTripleDotByName(positionName);
        clickVariantInTripleButton(PosTypeTripleButton.VIEW);
        assertAccessDataView(positionName);
    }

    @Test(groups = {"TK2052-1.2.2", "TEST-900"}, description = "Просмотр данных типа позиции c разрешением на просмотр")
    private void viewDataPositionTypeRole3() {
        goToPositionTypesPageAsUser(Role.THIRD);
        String positionName = getRandomPositionNameFromUI();
        clickTripleDotByName(positionName);
        clickVariantInTripleButton(PosTypeTripleButton.VIEW);
        assertAccessDataView(positionName);
    }

    @Test(groups = {"TK2052-2.1.1", "TEST-900"}, description = "Изменение названия типа позиции")
    private void editPositionTypeRole1() {
        PresetClass.makeNewPositionType(TEST_NAME);
        goToPositionTypesPageAsUser(Role.FIRST);
        Map<String, Integer> positionTypes = CommonRepository.getAllPositionTypesForPositionType();
        clickTripleDotByName(TEST_NAME);
        clickVariantInTripleButton(PosTypeTripleButton.EDIT);
        String randomName = RandomStringUtils.randomAlphabetic(10);
        sendPositionTypeName(randomName);
        pressSaveButton();
        assertEditOrCreatePositionType(randomName, positionTypes, true);
        PresetClass.deleteTestPositionType(randomName);
    }

    @Test(groups = {"TK2052-2.1.2", "TEST-900"}, description = "Изменение названия типа позиции c разрешением на просмотр")
    private void editPositionTypeRole3() {
        goToPositionTypesPageAsUser(Role.THIRD);
        String positionName = getRandomPositionNameFromUI();
        clickTripleDotByName(positionName);
        assertButtonNotExist(PosTypeTripleButton.EDIT);
    }

    @Test(groups = {"TK2052-2.2.1", "TEST-900"}, description = "Изменение параметра")
    private void editParameterRole1() {
        PresetClass.makeNewPositionType(TEST_NAME);
        goToPositionTypesPageAsUser(Role.FIRST);
        clickTripleDotByName(TEST_NAME);
        clickVariantInTripleButton(PosTypeTripleButton.EDIT);
        PositionParameters parameterName = PositionParameters.values()[new Random().nextInt(PositionParameters.values().length)];
        int value = new Random().nextInt(4) + 1;
        sendValueInParameter(parameterName, value);
        pressSaveButton();
        assertChangePositionTypeParameter(parameterName, value, TEST_NAME);
    }

    @Test(groups = {"TK2052-3.1", "TEST-900"}, description = "Создание типа позиции")
    private void createPositionTypeRole1() {
        Map<String, Integer> positionTypes = CommonRepository.getAllPositionTypesForPositionType();
        goToPositionTypesPageAsUser(Role.FIRST);
        pressOnRedPlusButton();
        String randomName = RandomStringUtils.randomAlphabetic(10);
        sendPositionTypeName(randomName);
        pressSaveButton();
        assertEditOrCreatePositionType(randomName, positionTypes, false);
    }

    @Test(groups = {"TK2052-3.2", "TEST-900"}, description = "Создание типа позиции c разрешением на просмотр")
    private void createPositionTypeRole3() {
        goToPositionTypesPageAsUser(Role.THIRD);
        assertRedButtonIsNotDisplayed();
    }

    @Test(groups = {"TK2052-4.1", "TEST-900"}, description = "Удаление типа позиции")
    private void deletePositionTypeRole1() {
        PresetClass.makeNewPositionType(TEST_NAME);
        goToPositionTypesPageAsUser(Role.FIRST);
        Map<String, Integer> positionTypes = CommonRepository.getAllPositionTypesForPositionType();
        clickTripleDotByName(TEST_NAME);
        clickVariantInTripleButton(PosTypeTripleButton.DELETE);
        assertDeletePositions(TEST_NAME, positionTypes);
    }

    @Test(groups = {"TK2052-4.2", "TEST-900"}, description = "Удаление типа позиции c разрешением на просмотр")
    private void deletePositionTypeRole3() {
        goToPositionTypesPageAsUser(Role.THIRD);
        String positionName = getRandomPositionNameFromUI();
        clickTripleDotByName(positionName);
        assertButtonNotExist(PosTypeTripleButton.DELETE);
    }

    @Test(groups = {"TK2052-5.1", "TEST-900", "broken"}, description = "Перемещение типа позиции в таблице")
    private void transferPositionTypeRole1() {
        goToPositionTypesPageAsUser(Role.FIRST);
        String firstPositionType = getRandomPositionNameFromUI();
        String secondPositionType = getRandomPositionNameFromUI(firstPositionType);
        //TODO не работает перетаскивание из за ghost элементов в HTML5 и нет ассерта, проверено 17.06 @e.gurkin
        dragPositionOnAnother(firstPositionType, secondPositionType);
    }

    @Test(groups = {"TK2052-5.2", "TEST-900"}, description = "Перемещение типа позиции в таблице c разрешением на просмотр")
    private void transferPositionTypeRole3() {
        goToPositionTypesPageAsUser(Role.THIRD);
        String randomPositionNameFromUI = getRandomPositionNameFromUI();
        assertTransferNotEnabled(randomPositionNameFromUI);
    }

}
