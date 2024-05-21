package pagemodel;

import com.google.inject.Inject;
import guice.TestModule;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.hamcrest.Matchers;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.MathParametersPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.GoToPageSection;
import testutils.RoleWithCookies;
import utils.Links;
import utils.tools.CustomTools;
import wfm.PresetClass;
import wfm.components.utils.Role;
import wfm.components.utils.Section;
import wfm.models.MathParameter;
import wfm.repository.MathParameterRepository;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.tools.CustomTools.getRandomFromList;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class MathParameters {

    private static final String RELEASE = Links.getTestProperty("release");
    private static final String URL_A = RELEASE + "/math-parameters";
    private static final Section MATH_PARAMETERS = Section.MATH_PARAMETERS;

    @Inject
    private MathParametersPage pp;

    @BeforeMethod(alwaysRun = true)
    private void goToScheduleBoard() {
        driverConfig();
    }

    @AfterTest(alwaysRun = true, description = "Закрываем драйвер")
    private void tearDown() {
        pp.getWrappedDriver().close();
    }

    private void goToMathParametersPage() {
        new GoToPageSection(pp).getPage(MATH_PARAMETERS, 60);
    }

    private void driverConfig() {
        pp.getWrappedDriver().manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    @Step("Нажать на кнопку сделать выбранный математический параметр невидимым")
    private void pointOutHiddenClick() {
        pp.rightBar().makeItemHidden().click();
    }

    @Step("Нажать кнопку отмены действия с математическим параметром")
    private void cancelButtonClick() {
        pp.rightBar().buttonCancel().click();
    }

    @Step("Нажать на кнопку сохранения изменения математического параметра")
    private void saveButtonClick() {
        pp.rightBar().buttonSave().click();
        pp.spinnerLoader().loadingSpinnerInForm().waitUntil("Значок загрузки не пропал", Matchers.not(DisplayedMatcher.displayed()), 5);
        ;
    }

    @Step("Кликнуть на математический параметр под названием: \"{mathParameter.name}\"")
    private void clickParameterSelection(MathParameter mathParameter) {
        pp.mathParametersBar().paramByOuterId(mathParameter.getEntity(), mathParameter.getOuterId()).click();
    }

    private String getRandomNameOfParameter() {
        return "New parameter" + CustomTools.stringGenerator();
    }

    @Step("Ввод имени параметра")
    private void editParameterDescription(String name) {
        pp.rightBar().nameParameters().clear();
        pp.rightBar().nameParameters().sendKeys(name);
        Allure.addAttachment("Изменение имени", "text/plain",
                             "Имя математического параметра " + name + ", было изменено на: " + name);
    }

    @Step("Проверка того чтоы математический параметр который мы пытались скрыть, отображается")
    private void checkTheMathematicalParameterDisplayed(String name) {
        pp.mathParametersBar().parametersDescriptions().filter(o -> o.getText().equals(name)).get(0)
                .should("Матаметический параметр скрыт", DisplayedMatcher.displayed(), 5);
    }

    @Step("Проверить изменение названия параметра на \"{newName}\"")
    private void assertNameChange(MathParameter previous, String newName) {
        String paramName = pp.mathParametersBar().paramByOuterId(previous.getEntity(), previous.getOuterId()).getText();
        MathParameter mathParameterNew = MathParameterRepository.getMathParameter(previous.getMathParameterId());
        String newNameApi = mathParameterNew.getName();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(newName, newNameApi, "Введенное имя в ходе теста и найденное в апи не совпали");
        softAssert.assertEquals(paramName, newNameApi, "Введенное имя в ходе теста не совпало с отображаемым на UI");
        Allure.addAttachment("Проверка", "Было введено имя: "
                + newName + ", имя отобразившиеся в api: " + newNameApi);
        PresetClass.changeParameterField(previous, "name", previous.getName());
        softAssert.assertAll();
    }

    @Step("Проверить, что форма редактирования не закрывается. Появляется поп-ап \"Ошибка доступа")
    private void assertNotEnoughPermission() {
        pp.rightBar().should("Форма редактирования закрылась", DisplayedMatcher.displayed(), 3);
        pp.mathParametersBar().snackBar()
                .waitUntil("Снэкбар не был отображен", DisplayedMatcher.displayed(), 6);
        pp.mathParametersBar().snackBar()
                .should("Текст снэкбара не совпал с ожидаемым", text(containsString("Ошибка доступа")), 2);
    }

    @Step("Проверить изменение статуса параметра с именем \"{mathParameter.name}\" на \"Hidden\"")
    private void assertHiddenStatusChange(MathParameter mathParameter) {
        String after = pp.mathParametersBar().paramHiddenStatus(mathParameter.getEntity(), mathParameter.getOuterId()).getText();
        boolean afterApi = MathParameterRepository.getMathParameter(mathParameter.getMathParameterId()).isHidden();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(afterApi, "Статус в api не был \"Hidden\"");
        softAssert.assertEquals(String.valueOf(true), after, "Статус на UI не был \"Hidden\"");
        Allure.addAttachment("Проверка", "Текущий статус параметра \"Hidden\"");
        PresetClass.changeParameterField(mathParameter, "hidden", false);
        softAssert.assertAll();
    }

    @Step("Проверяем отображение списка математических параметров")
    private void testCheckPresenceMainForm() {
        pp.mathParametersBar().should(DisplayedMatcher.displayed());
    }

    @Test(groups = {"MP-1.1", "TEST-296", "nameChanging"}, description = "Изменение названия параметра")
    private void changingTheNameOfTheParameter() {
        goToMathParametersPage();
        MathParameter math = getRandomFromList(MathParameterRepository.getMathParameters());
        clickParameterSelection(math);
        String parameter = getRandomNameOfParameter();
        editParameterDescription(parameter);
        saveButtonClick();
        assertNameChange(math, parameter);
    }

    @Test(groups = "MP-1.2", description = "Отмена изменения названия параметра")
    private void cancelParameterChange() {
        goToMathParametersPage();
        MathParameter math = getRandomFromList(MathParameterRepository.getMathParameters());
        clickParameterSelection(math);
        String parameter = getRandomNameOfParameter();
        editParameterDescription(parameter);
        cancelButtonClick();
        checkTheMathematicalParameterDisplayed(math.getName());
    }

    @Test(groups = {"MP-2.1", "TEST-296"}, description = "Скрытие параметра")
    private void hideParameter() {
        goToMathParametersPage();
        MathParameter math = getRandomFromList(MathParameterRepository.getMathParameters());
        PresetClass.changeParameterField(math, "hidden", false);
        clickParameterSelection(math);
        pointOutHiddenClick();
        saveButtonClick();
        assertHiddenStatusChange(math);
        PresetClass.changeParameterField(math, "hidden", false);
    }

    @Test(groups = {"MP-2.2"}, description = "Отмена скрытия параметра")
    private void cancelParameterHiding() {
        goToMathParametersPage();
        MathParameter math = getRandomFromList(MathParameterRepository.getMathParameters());
        clickParameterSelection(math);
        pointOutHiddenClick();
        cancelButtonClick();
        checkTheMathematicalParameterDisplayed(math.getName());
    }

    @Test(groups = {"MP-3", "TEST-296"}, description = "Переключение на другой параметр")
    private void switchToAnotherParameter() {
        goToMathParametersPage();
        MathParameter math = getRandomFromList(MathParameterRepository.getMathParameters());
        MathParameter anotherMath = getRandomFromList(MathParameterRepository.getMathParameters());
        PresetClass.changeParameterField(anotherMath, "hidden", false);
        clickParameterSelection(math);
        clickParameterSelection(anotherMath);
        String newName = getRandomNameOfParameter();
        editParameterDescription(newName);
        pointOutHiddenClick();
        saveButtonClick();
        assertNameChange(anotherMath, newName);
        assertHiddenStatusChange(anotherMath);
    }

    @Test(groups = "MP-4", description = "Закрытие окна изменения параметра")
    private void closingTheParameterChangeWindow() {
        goToMathParametersPage();
        MathParameter math = getRandomFromList(MathParameterRepository.getMathParameters());
        clickParameterSelection(math);
        cancelButtonClick();
        testCheckPresenceMainForm();
    }

    @Test(groups = {"TK2051-2-1", "TEST-901"}, description = "Изменение описания параметра c разрешением на просмотр и редактирование")
    private void changeParameterDescription() {
        new RoleWithCookies(pp.getWrappedDriver(), Role.FIRST).getPage(MATH_PARAMETERS);
        MathParameter mathParameter = getRandomFromList(MathParameterRepository.getMathParameters());
        clickParameterSelection(mathParameter);
        String newName = getRandomNameOfParameter();
        editParameterDescription(newName);
        saveButtonClick();
        assertNameChange(mathParameter, newName);
        PresetClass.changeParameterField(mathParameter, "name", mathParameter.getName());
    }

    @Test(groups = {"TK2051-3-1", "TEST-901"}, description = "Изменение отображения параметра c разрешением на просмотр и редактирование")
    private void hideParameterDescription() {
        new RoleWithCookies(pp.getWrappedDriver(), Role.FIRST).getPage(MATH_PARAMETERS);
        MathParameter math = getRandomFromList(MathParameterRepository.getMathParameters());
        PresetClass.changeParameterField(math, "hidden", false);
        clickParameterSelection(math);
        pointOutHiddenClick();
        saveButtonClick();
        assertHiddenStatusChange(math);
        PresetClass.changeParameterField(math, "hidden", false);
    }
}


