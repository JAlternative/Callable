package pagemodel;

import bio.components.terminal.*;
import bio.repository.CommonBioRepository;
import com.google.inject.Inject;
import com.mchange.util.AssertException;
import guice.TestModule;
import io.qameta.allure.Allure;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matchers;
import org.openqa.selenium.interactions.Actions;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.TerminalPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import utils.Links;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.tools.CustomTools.randomItem;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class BioTerminal {

    private static final String TERMINAL_URL = Links.getTestProperty("terminal");
    private static final String URL_HOME = TERMINAL_URL;

    @Inject
    private TerminalPage bt;

    @DataProvider(name = "UnitList")
    private static Object[][] orgUnitToCheckData() {
        Object[][] array = new Object[7][];
        array[0] = new Object[]{AuthType.ONE_EVENT, CheckInType.RECORD};
        array[1] = new Object[]{AuthType.TWO_EVENT, CheckInType.OPEN_SHIFT};
        array[2] = new Object[]{AuthType.TWO_EVENT, CheckInType.CLOSE_SHIFT};
        array[3] = new Object[]{AuthType.FOUR_EVENT, CheckInType.OPEN_SHIFT};
        array[4] = new Object[]{AuthType.FOUR_EVENT, CheckInType.CLOSE_SHIFT};
        array[5] = new Object[]{AuthType.FOUR_EVENT, CheckInType.OPEN_BREAK};
        array[6] = new Object[]{AuthType.FOUR_EVENT, CheckInType.CLOSE_BREAK};
        return array;
    }

    @Step("Установка методики ожидания для драйвера")
    private void driverConfig() {
        bt.getWrappedDriver().manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    @Step("Закрытие драйвера")
    private void driverClose() {
        bt.getWrappedDriver().close();
    }

    @AfterTest(alwaysRun = true)
    private void tearDown() {
        driverClose();
    }

    @BeforeMethod(alwaysRun = true)
    private void setUp() {
        driverConfig();
        goToTerminalSection();
    }

    @Test(dataProvider = "UnitList", groups = {"CheckInDifferentModes", "TEST-423"}, description = "Проверка режимов авторизации")
    private void apiCheck(AuthType type, CheckInType checkInType) {
        preSetForAuth(type);
        clickCheckInButton(checkInType);
        checkGoToCheckInPage(checkInType);
        clickCancelButton();
    }

    @Step("Перейти по URL: {url}")
    private void goToTerminalSection() {
        bt.getWrappedDriver().get(BioTerminal.URL_HOME);
        bt.mainPage().waitUntil("Главная страница не загрузилась", DisplayedMatcher.displayed(), 30);
        bt.mainPage().authButtonsList().waitUntil(Matchers.hasSize(Matchers.greaterThan(0)));
    }

    @Step("Проверить отображение авторизации, даты и времени на главной странице")
    private void checkGoToHomePage() {
        LocalDate currentDate = LocalDate.now();
        LocalTime time = LocalTime.parse(bt.mainPage().currentTimeField().getText());
        String date = bt.mainPage().currentDateField().getText().trim();
        SoftAssert softAssert = new SoftAssert();
        LocalTime currentTime = LocalTime.now().withSecond(0).withNano(0);
        //        if (LocaleKeys.getAssertProperty("browser").equals("grid")) {
        //            currentTime = currentTime.minusHours(5);
        //        }
        softAssert.assertEquals(time.withSecond(0), currentTime,
                                "Текущее время и время в терминале не совпали");
        String monthName = currentDate.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd");
        String currentDay = currentDate.format(formatter);
        String expectedDay = currentDay + " " + monthName;
        softAssert.assertEquals(date, expectedDay, "Дата отображенная в поле не совпала с текущей");
        int authButtonsValue = bt.mainPage().authButtonsList().size();
        softAssert.assertTrue(authButtonsValue >= 1, "Не было отображено ни одного режима авторизации.");
        Allure.addAttachment("Проверка",
                             "Был выполнен переход на главную страницу терминала были отображены дата и время:\n"
                                     + date + " " + time +
                                     "\nКолличество режимов авторицаии было отображено: " + authButtonsValue);
        softAssert.assertAll();
    }

    @Step("Проверить функцию отображение e-mail поддержки в левом нижнем углу")
    private void checkSupportEmailIsDisplayed() {
        bt.mainPage().supportEmailField()
                .should("Email поддержки не был отображен.", DisplayedMatcher.displayed(), 5);
        Allure.addAttachment("Проверка отображения e-mail поддержки", "Был отображен e-mail с тектом: " +
                bt.mainPage().supportEmailField().getText());
    }

    @Step("Проверить, что e-mail поддержки в левом нижнем углу соответствует значению: {mail}")
    private void checkMailsMatches(String mail) {
        Allure.addAttachment("Проверка отображения e-mail поддержки", "Был отображен e-mail с тектом: " +
                bt.mainPage().supportEmailField().getText());
        bt.mainPage().supportEmailField()
                .should("Email поддержки не был отображен.", text(containsString(mail)), 5);
    }

    @Step("В блоке \"Режим распознавания\" выбрать \"{type.controlType}\"")
    private void selectCheckInButton(ControlMode type) {
        bt.settingsPage().controlButton(type.getControlType()).click();
    }

    @Step("Активировать переключатель для \"Автоматический переход на главную страницу после распознавания \"")
    private void mainPageRedirectSwitcherClick() {
        bt.settingsPage().mainPageRedirectSwitcher().click();
    }

    @Step("Произвольно передвинуть ползунок в блоке \"{type.sliderName}\"")
    private void selectRange(SliderType type) {
        Actions actions = new Actions(bt.getWrappedDriver());
        AtlasWebElement slider = bt.settingsPage().sliderInput(type.getSliderName());
        int size = slider.getSize().width;
        int randomStep = new Random().nextInt(9);
        actions.dragAndDropBy(slider,
                              randomStep * size / 8 - size / 2, 0).perform();
        int time = randomStep + 2;
        String timeText = bt.settingsPage().currentTimeValue(type.getPosition()).getText();
        Assert.assertEquals(timeText.split(" ")[0], String.valueOf(time));
        Allure.addAttachment("Проверка", "Было выставлено значение: " + time
                + "\nОтобразился текст: " + timeText);
    }

    @Step("Активировать метод работы \"{workingMethod.methodName}\"")
    private void workingMethodButtonClick(WorkingMethod workingMethod) {
        bt.settingsPage().workingMethodButton(workingMethod.getMethodName()).click();
    }

    @Step("Проверить активацию радио-кнопки метода работы \"{workingMethod.methodName}\"")
    private void assertActivateWorkingMethod(WorkingMethod workingMethod) {
        Assert.assertTrue(bt.settingsPage().workingMethodButton(workingMethod.getMethodName()).getAttribute("class").contains("selected"),
                          "Радио-кнопка не была активирована");
        Allure.addAttachment("Проверка", "Радио кнопка была успешно активирована!");
    }

    @Step("Выбрать тип Используемой маски \"{type}\"")
    private void changeMaskType(String type) {
        bt.settingsPage().maskTypeButton(type).click();
        Assert.assertTrue(bt.settingsPage().maskTypeButton(type).getAttribute("class").contains("selected"),
                          "Маска не была выбрана");
        Allure.addAttachment("Выбор маски", "Маска была успешно выбрана.");
    }

    @Step("Изменить \"E-mail технической поддержки\" в блоке \"Техническая поддержка\" на {mail}")
    private void changeSupportMail(String mail) {
        bt.settingsPage().pencilButton(SliderType.EMAIL_TEXT.getSliderName()).click();
        bt.settingsPage().textInputField().click();
        bt.settingsPage().textInputField().clear();
        bt.settingsPage().textInputField().sendKeys(mail);
        bt.settingsPage().acceptTextChangingButton().click();
    }

    @Step("Нажать на переключатель \"{type.sliderName}\" и проверить что он переключился")
    private void turnSlider(SliderType type, boolean enable) {
        bt.settingsPage().turnButton(type.getSliderName()).click();
        String allureStr = "Не активирован";
        boolean status = false;
        if (bt.settingsPage().turnButton(type.getSliderName()).getAttribute("class").contains("checked")) {
            status = true;
            allureStr = "Активирован";
        }
        Allure.addAttachment("Статус переключателя", allureStr);
        Assert.assertEquals(enable, status, "Статус слайдера не изменился. ");
    }

    @Step("Произвольно изменить данные для \"Ссылка на видеопоток для видеоконтейнера №1\"")
    private void changeStreamsRef() {
        bt.settingsPage().pencilButton(SliderType.FIRST_STREAM_HREF.getSliderName()).click();
        bt.settingsPage().textInputField().clear();
        String text = RandomStringUtils.randomAlphabetic(10);
        bt.settingsPage().textInputField().sendKeys(text);
        Assert.assertEquals(bt.settingsPage().textInputField().getAttribute("value"), text,
                            "Отобразившийся текст не совпал с введенным");
        Allure.addAttachment("Ввод текста", "Был введен текст: " + text);
    }

    @Step("Проверить, что поле ввода текста неактивно в данный момент")
    private void assertInputFieldNotActive() {
        Assert.assertEquals(bt.settingsPage().textInputFields().size(), 0,
                            "Еще остались активные поля ввода текста.");
    }

    @Step("Произвольно изменить данные для \"Подсказка внизу видеоконтейнера при использовании маски\"")
    private void changeMaskTipText() {
        bt.settingsPage().pencilButton(SliderType.MASK.getSliderName()).click();
        bt.settingsPage().textInputField().clear();
        String text = RandomStringUtils.randomAlphabetic(10);
        bt.settingsPage().textInputField().sendKeys(text);
        Assert.assertEquals(bt.settingsPage().textInputField().getAttribute("value"), text,
                            "Отобразившийся текст не совпал с введенным");
        Allure.addAttachment("Ввод текста", "Был введен текст: " + text);
    }

    @Step("Пресет. Проверить, что отображается нужная модель авторизации, если нет, то переключить в настройках")
    private void preSetForAuth(AuthType type) {
        if (type.getValue() != bt.mainPage().authButtonsList().size()) {
            clickSettingsButton();
            clickAuthButton(type);
            exitButtonClick();
        }
    }

    @Step("В блоке \"Варианты отметок\" активировать радио-кнопку \"{type.settingsName}\"")
    private void clickAuthButton(AuthType type) {
        bt.settingsPage().authRadioButtonWithType(type.getSettingsName()).click();
        Assert.assertTrue(bt.settingsPage().authRadioButtonIsChecked(type.getSettingsName()).isDisplayed(),
                          "Кнопка со именем " + type.getSettingsName() + " не была отображена");
        Allure.addAttachment("Активация радио-кнопки",
                             "Была успешно активирована радио-кнопка с названием: " + type.getSettingsName());
    }

    @Step("Проверить что открывается панель администрирования, отображается вкладка \"Основные настройки\"")
    private void assertSettingsPageIsDisplayed() {
        bt.settingsPage().basicSettingsPanel().should("Вкладка \"Основные настройки\"  не отобразилась",
                                                      DisplayedMatcher.displayed(), 5);
        Allure.addAttachment("Проверка ", "Был осуществлен переход в в административную" +
                " панель отобразилась вкладка \"Основные настройки\"");
    }

    @Step("Проверить совпадений версий")
    private void compareVersions() {
        bt.mainPage().versionField()
                .should("Поле с текущей версией не было отображено", DisplayedMatcher.displayed(), 5);
        String versionUi = bt.mainPage().versionField().getText().split(" ")[0];
        String apiVersion = CommonBioRepository.getCurrentVersionApi().replaceAll("\\n", "");
        Allure.addAttachment("Проверка", "Версия на UI: " + versionUi + ". Версия в api: " + apiVersion);
        Assert.assertEquals(apiVersion, versionUi, "Отображенная версия на ui не совпала с версией в api");
    }

    @Step("Нажать на кнопку \"Выход\"")
    private void exitButtonClick() {
        bt.settingsPage().exitButton().click();
        bt.mainPage()
                .waitUntil("Не был осуществлен переход на гланый экран", DisplayedMatcher.displayed(), 10);
    }

    @Step("Нажать на кнопку \"Отменить\"")
    private void clickCancelButton() {
        bt.checkInPage().cancelButton().click();
        bt.mainPage()
                .waitUntil("Не был осуществлен переход на гланый экран", DisplayedMatcher.displayed(), 6);
    }

    @Step("Нажать на значок шестеренки.")
    private void clickSettingsButton() {
        bt.mainPage().goToSettingsButton().click();
        bt.settingsPage()
                .waitUntil("Не была отображена административная панель", DisplayedMatcher.displayed(), 10);
    }

    @Step("Проверить отображение иконки о статусе подключения к центральному серверу")
    private void checkConnectStatusDisplayed(boolean isDisplayed) {
        String status = "не отображена";
        if (isDisplayed) {
            bt.settingsPage().connectionStatusIcon().should(DisplayedMatcher.displayed());
            status = "отображена";
        } else {
            bt.settingsPage().connectionStatusIcon().should(Matchers.not(DisplayedMatcher.displayed()));
        }
        Allure.addAttachment("Проверка отображения статуса", "Иконка со статусом подключения " + status);
    }

    @Step("Нажать на кнопку \"{type}\"")
    private void clickCheckInButton(CheckInType type) {
        bt.mainPage().checkInButtonByType(type.toString()).click();
        Allure.addAttachment("Выбор действия.", "Было выбрано действие" + type.getCheckInWayName());
    }

    @Step("Проверить переход на страницу для авторизации по событию {type}")
    private void checkGoToCheckInPage(CheckInType type) {
        bt.checkInPage().cameraContainer().should("Панель с камерой не была отображена", DisplayedMatcher.displayed(), 5);
        String checkInType = bt.checkInPage().checkInTypeField().getText();
        Allure.addAttachment("Проверка", "После перехода в режим авторизации был отображен текст " + checkInType);
        Assert.assertEquals(type.getTextOnPanel(), checkInType, "Названия режимов авторизации не совпали");
    }

    @Step("Проверка отображения статуса переключения слайдера \"Отображать только 1 камеру\"")
    private void assertDisplayOnlyOneCamera(boolean mute) {
        String statusText = "";
        if (mute) {
            statusText = "Отображение со 2+ камеры будет скрыто";
        }
        bt.settingsPage().hiddenSecondCamText()
                .should("Отобразился неверный статус переключения камеры отображения",
                        text(containsString(statusText)), 5);
    }

    @Step("Проверить статус поворота камеры {type}")
    private void assertTurnStatusStreamSlider(SliderType type, boolean enable, Side side) {
        int order = side.ordinal() + 1;
        String allureStr = "Не активирован";
        boolean status = false;
        if (bt.settingsPage().turnStreamSlider(type.getSliderName(), order).getAttribute("class").contains("checked")) {
            status = true;
            allureStr = "Активирован";
        }
        Allure.addAttachment("Статус переключателя", allureStr);
        Assert.assertEquals(enable, status, "Статус слайдера не изменился. ");
    }

    @Step("Нажать на переключатель стрима \"{type.sliderName}\" {side.name}")
    private void turnStreamSlider(SliderType type, Side side) {
        int order = side.ordinal() + 1;
        bt.settingsPage().turnStreamSlider(type.getSliderName(), order).click();
    }

    @Step("Нажать на {side.name} кнопку редактирования источника")
    private void pressStreamPencilButton(Side side) {
        int order = side.ordinal() + 1;
        bt.settingsPage().pencilStreamButton(order).click();
    }

    @Step("Нажать на {side.name} кнопку принятия источника")
    private void pressStreamAcceptButton(Side side) {
        int order = side.ordinal() + 1;
        bt.settingsPage().acceptStreamButton(order).click();
    }

    @Step("Ввести текст \"{text}\" в поле источника {side.name}")
    private void sendTextInInputStreamField(Side side, String text) {
        int order = side.ordinal() + 1;
        bt.settingsPage().streamInputField(order).clear();
        bt.settingsPage().streamInputField(order).sendKeys(text);
    }

    @Step("Проверить измение статуса и источников видеопотоков")
    private void assertUseExternalStreams(String leftStream, String rightStream) {
        assertTurnStatusStreamSlider(SliderType.STREAM, true, Side.LEFT);
        assertTurnStatusStreamSlider(SliderType.STREAM, true, Side.RIGHT);
        String leftText = bt.settingsPage().streamInputField(Side.LEFT.ordinal() + 1).getAttribute("value");
        String rightText = bt.settingsPage().streamInputField(Side.RIGHT.ordinal() + 1).getAttribute("value");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(leftText, leftStream, "Название левого источника не совпадает с введенным");
        softAssert.assertEquals(rightText, rightStream, "Название правого источника не совпадает с введенным");
        softAssert.assertAll();
    }

    @Step("Проверка применения масок")
    private void assertUsingMask(String expectedLeftMask, String expectedRightMask) {
        String leftMask = bt.settingsPage().maskSelectButton(Side.LEFT.ordinal() + 1).getAttribute("value");
        String rightMask = bt.settingsPage().maskSelectButton(Side.RIGHT.ordinal() + 1).getAttribute("value");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(leftMask, expectedLeftMask, "Левая маска не совпадает с выставленной");
        softAssert.assertEquals(rightMask, expectedRightMask, "Правая маска не совпадает с выставленной");
        softAssert.assertAll();
    }

    @Step("Проверка выбора расположения камер")
    private void assertChooseCameraLocation(String expectedLeftLocation, String expectedRightLocation) {
        String leftMask = bt.settingsPage().cameraLocatedSelectButton(Side.LEFT.ordinal() + 1).getAttribute("value");
        String rightMask = bt.settingsPage().cameraLocatedSelectButton(Side.RIGHT.ordinal() + 1).getAttribute("value");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(leftMask, expectedLeftLocation,
                                "Расположение левой камеры не совпадает с выставленным");
        softAssert.assertEquals(rightMask, expectedRightLocation,
                                "Расположение правой камеры не совпадает с выставленным");
        softAssert.assertAll();
    }

    @Step("Выбрать {side.name} маску {maskName}")
    private void selectMask(String maskName, Side side) {
        bt.settingsPage().maskSelectButton(side.ordinal() + 1).click();
        bt.settingsPage().allMasks(side.ordinal() + 1).stream()
                .filter(extendedWebElement -> extendedWebElement.getText().equals(maskName))
                .findAny()
                .orElseThrow(() -> new AssertException("Не нашли в списке маску: " + maskName)).click();
    }

    @Step("Выбрать {side.name} расположение камеры \"{location}\"")
    private void selectCameraLocation(String location, Side side) {
        bt.settingsPage().cameraLocatedSelectButton(side.ordinal() + 1).click();
        bt.settingsPage().allCameraLocated(side.ordinal() + 1).stream()
                .filter(extendedWebElement -> extendedWebElement.getText().equals(location))
                .findAny()
                .orElseThrow(() -> new AssertException("Не нашли в списке маску: " + location)).click();
    }

    /**
     * Берет случайное название маски из списка масок на UI
     *
     * @param side - с какой стороны будем брать
     */
    private String getRandomMaskName(Side side) {
        AtlasWebElement element = bt.settingsPage().allMasks(side.ordinal() + 1).stream().collect(randomItem());
        return element.getText();
    }

    /**
     * Берет случайное название маски из списка масок на UI
     *
     * @param side - с какой стороны будем брать
     */
    private String getRandomCameraLocation(Side side) {
        AtlasWebElement element = bt.settingsPage().allCameraLocated(side.ordinal() + 1).stream().collect(randomItem());
        return element.getText();
    }

    @Step("В блоке \"Вариант отображения ФИО\" выбрать \"{mode.mode}\"")
    private void selectNameVision(NameMode mode) {
        bt.settingsPage().controlButton(mode.getMode()).click();
    }

    @Step("Проверка выбора в блоке \"Вариант отображения ФИО\" варианта \"{mode.mode}\"")
    private void assertSelectNameVision(NameMode mode) {
        String aClass = bt.settingsPage().controlButton(mode.getMode()).getAttribute("class");
        Assert.assertTrue(aClass.contains("selected"), "");
    }

    @Step("Проверить выбор устройства")
    private void assertChooseVideoDevice(String leftStream, String rightStream) {
        String leftText = bt.settingsPage().streamInputField(Side.LEFT.ordinal() + 1).getAttribute("value");
        String rightText = bt.settingsPage().streamInputField(Side.RIGHT.ordinal() + 1).getAttribute("value");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(leftText, leftStream, "Название левого источника не совпадает с введенным");
        softAssert.assertEquals(rightText, rightStream, "Название правого источника не совпадает с введенным");
        softAssert.assertAll();
    }

    @Test(groups = {"BI-10.1", "TEST-422"}, description = "Переход на главный экран")
    public void goToHomePageWithCheck() {
        checkGoToHomePage();
    }

    @Test(groups = {"BI-10.6", "TEST-425"}, description = "Проверка отображения e-mail поддержки")
    public void checkSupportEmail() {
        checkSupportEmailIsDisplayed();
    }

    @Test(groups = {"BI-10.7", "TEST-426"}, description = "Проверка соответствия версии с api")
    public void compareVersionWithApi() {
        compareVersions();
    }

    @Test(groups = {"BI-10.2", "TEST-423"}, description = "Переход в административную панель")
    public void goToSettings() {
        clickSettingsButton();
        assertSettingsPageIsDisplayed();
        exitButtonClick();
        checkGoToHomePage();
    }

    @Test(groups = {"PNBI-10.1.1", "TEST-423"},
            description = "Смена мониторинга событий на авторизацию Одна кнопка (идентификация)")
    public void switchToOneEventAndClientControl() {
        clickSettingsButton();
        clickAuthButton(AuthType.ONE_EVENT);
    }

    @Test(groups = {"PNBI-10.1.1.2", "TEST-1206"},
            description = "Смена мониторинга событий на авторизацию Одна кнопка с использованием антифрода (идентификация)")
    public void switchToOneButtonWithAntifraud() {
        clickSettingsButton();
        workingMethodButtonClick(WorkingMethod.ONE_CAMERA_FAST);
        assertActivateWorkingMethod(WorkingMethod.ONE_CAMERA_FAST);
    }

    @Test(groups = {"PNBI-10.1.2", "TEST-423"},
            description = "Смена мониторинга событий на авторизацию Две кнопки (начало и окончание смены)")
    public void switchToTwoEventAndClientControl() {
        clickSettingsButton();
        clickAuthButton(AuthType.TWO_EVENT);
    }

    @Test(groups = {"PNBI-10.1.3", "TEST-423"},
            description = "Смена мониторинга событий на авторизацию Четыре кнопки (начало и окончание смены и перерыва)")
    public void switchToFourEventAndClientContr1ol() {
        clickSettingsButton();
        clickAuthButton(AuthType.FOUR_EVENT);
    }

    @Test(groups = {"PNBI-10.1.4", "TEST-423"}, description = "Изменение метода распознавания на \"Одна камера\"")
    public void switchToFourEventAndClientCont1rol() {
        clickSettingsButton();
        workingMethodButtonClick(WorkingMethod.SIMPLE);
        assertActivateWorkingMethod(WorkingMethod.SIMPLE);
    }

    @Test(groups = {"PNBI-10.1.5", "TEST-423"}, description = "Изменение метода распознавания на \"Две камеры\"")
    public void switchToFourEventAndClientControl() {
        clickSettingsButton();
        workingMethodButtonClick(WorkingMethod.TWO_CAMERA_FAST);
        assertActivateWorkingMethod(WorkingMethod.TWO_CAMERA_FAST);
    }

    @Test(groups = {"PNBI-10.1.12", "TEST-712"}, description = "Выбор режима управления \"Ручное управление\"")
    public void switchToManual() {
        clickSettingsButton();
        selectCheckInButton(ControlMode.MANUAL_CONTROL);
        mainPageRedirectSwitcherClick();
        selectRange(SliderType.MANUAL_DISPLAY_PERIOD);
    }

    @Test(groups = {"PNBI-10.1.13", "TEST-712"}, description = "Выбор режима управления \"Автоматическое управление\"")
    public void switchToAuto() {
        clickSettingsButton();
        selectCheckInButton(ControlMode.AUTO_CONTROL);
        selectRange(SliderType.AUTO_DISPLAY_PERIOD);
        selectRange(SliderType.AUTO_COUNTDOWN_DURATION);
    }

    @Test(groups = {"PNBI-10.1.6", "TEST-715"}, description = "Поворот первой камеры")
    public void turnFirstCamera() {
        clickSettingsButton();
        SliderType camOne = SliderType.CAM_ONE;
        Side side = Side.LEFT;
        turnStreamSlider(camOne, side);
        assertTurnStatusStreamSlider(camOne, true, side);
        turnStreamSlider(camOne, side);
        assertTurnStatusStreamSlider(camOne, false, side);
    }

    @Test(groups = {"PNBI-10.1.7", "TEST-715"}, description = "Поворот второй камеры")
    public void turnSecondCamera() {
        clickSettingsButton();
        workingMethodButtonClick(WorkingMethod.TWO_CAMERA_FAST);
        SliderType camTwo = SliderType.CAM_TWO;
        Side side = Side.RIGHT;
        turnStreamSlider(camTwo, side);
        assertTurnStatusStreamSlider(camTwo, true, side);
        turnStreamSlider(camTwo, side);
        assertTurnStatusStreamSlider(camTwo, false, side);
    }

    @Test(groups = {"PNBI-10.1.8", "TEST-715"}, description = "Использование внешних видеопотоков")
    public void usingExternalStreams() {
        clickSettingsButton();
        String left = RandomStringUtils.randomAlphabetic(10);
        String right = RandomStringUtils.randomAlphabetic(10);
        SliderType stream = SliderType.STREAM;
        turnStreamSlider(stream, Side.LEFT);
        pressStreamPencilButton(Side.LEFT);
        sendTextInInputStreamField(Side.LEFT, left);
        pressStreamAcceptButton(Side.LEFT);
        turnStreamSlider(stream, Side.RIGHT);
        pressStreamPencilButton(Side.RIGHT);
        sendTextInInputStreamField(Side.RIGHT, right);
        pressStreamAcceptButton(Side.RIGHT);
        assertUseExternalStreams(left, right);
    }

    @Test(groups = {"PNBI-10.1.9", "TEST-715"}, description = "Использование маски-подсказчика")
    public void usingMask() {
        clickSettingsButton();
        String leftRandomMaskName = getRandomMaskName(Side.LEFT);
        selectMask(leftRandomMaskName, Side.LEFT);
        String rightRandomMaskName = getRandomMaskName(Side.RIGHT);
        selectMask(rightRandomMaskName, Side.RIGHT);
        assertUsingMask(leftRandomMaskName, rightRandomMaskName);
    }

    @Test(groups = {"PNBI-10.1.10", "TEST-715", "not actual"}, description = "Изменение типа используемой маски")
    public void selectFrameMask() {
        clickSettingsButton();
        turnSlider(SliderType.MASK, true);
        String maskType = "Рамка";
        changeMaskType(maskType);
        turnSlider(SliderType.MASK, false);
    }

    @Test(groups = {"PNBI-10.1.11", "TEST-715", "not actual"}, description = "Изменение типа используемой маски")
    public void selectOvalMask() {
        clickSettingsButton();
        turnSlider(SliderType.MASK, true);
        String maskType = "Овал";
        changeMaskType(maskType);
        turnSlider(SliderType.MASK, false);
    }

    @Test(groups = {"PNBI-10.1.14", "TEST-716"}, description = "Отображение статуса подключения к центральному серверу")
    public void connectStatusDisplay() {
        clickSettingsButton();
        turnSlider(SliderType.CONNECT_STATUS, true);
        checkConnectStatusDisplayed(true);
        turnSlider(SliderType.CONNECT_STATUS, false);
        checkConnectStatusDisplayed(false);
    }

    @Test(groups = {"PNBI-10.1.16", "TEST-716"}, description = "Изменение адреса технической поддержки")
    public void changeSupportEmail() {
        clickSettingsButton();
        turnSlider(SliderType.EMAIL_DISPLAY, false);
        turnSlider(SliderType.EMAIL_DISPLAY, true);
        String mailText = RandomStringUtils.randomAlphabetic(10);
        changeSupportMail(mailText);
        exitButtonClick();
        checkMailsMatches(mailText);
    }

    @Test(groups = {"PNBI-10.1.17", "TEST-1077"}, description = "Отключение отображения второй камеры")
    public void muteSecondCamera() {
        clickSettingsButton();
        workingMethodButtonClick(WorkingMethod.TWO_CAMERA_FAST);
        turnSlider(SliderType.DISPLAY_ONE_CAM, true);
        assertDisplayOnlyOneCamera(true);
    }

    @Test(groups = {"PNBI-10.1.18", "TEST-1077"}, description = "Активация функции \"Ложное распознавание\"")
    public void activatingFalseRecognition() {
        clickSettingsButton();
        turnSlider(SliderType.ITS_NOT_ME, true);
    }

    @Test(groups = {"PNBI-10.1.19", "TEST-1077"}, dependsOnGroups = {"PNBI-10.1.18"},
            description = "Деактивация функции \"Ложное распознавание\"")
    public void deactivatingFalseRecognition() {
        clickSettingsButton();
        turnSlider(SliderType.ITS_NOT_ME, false);
    }

    @Test(groups = {"PNBI-10.1.20", "TEST-1077"}, dependsOnGroups = {"PNBI-10.1.17"},
            description = "Включение отображения второй камеры ")
    public void unmuteSecondCamera() {
        clickSettingsButton();
        workingMethodButtonClick(WorkingMethod.TWO_CAMERA_FAST);
        turnSlider(SliderType.DISPLAY_ONE_CAM, false);
        assertDisplayOnlyOneCamera(false);
    }

    @Test(groups = {"PNBI-10.1.21", "TEST-1169"},
            description = "Изменение метода отображения ФИО на полное")
    public void changingMethodDisplayingNameToFull() {
        clickSettingsButton();
        NameMode full = NameMode.FULL;
        selectNameVision(full);
        assertSelectNameVision(full);
    }

    @Test(groups = {"PNBI-10.1.22", "TEST-1169"},
            description = "Изменение метода отображения ФИО на полное")
    public void changingMethodDisplayingNameToShort() {
        clickSettingsButton();
        NameMode full = NameMode.SHORT_FAMILY;
        selectNameVision(full);
        assertSelectNameVision(full);
    }

    @Test(groups = {"PNBI-10.1.25", "TEST-1169"},
            description = "Изменение метода отображения ФИО на полное")
    public void changingMethodDisplayingNameOff() {
        clickSettingsButton();
        NameMode full = NameMode.NOT_DISPLAYED;
        selectNameVision(full);
        assertSelectNameVision(full);
    }

    @Test(groups = {"PNBI-10.1.23", "TEST-1169"}, description = "Выбор видео устройства")
    public void chooseVideoDevice() {
        clickSettingsButton();
        String left = RandomStringUtils.randomAlphabetic(10);
        String right = RandomStringUtils.randomAlphabetic(10);
        pressStreamPencilButton(Side.LEFT);
        sendTextInInputStreamField(Side.LEFT, left);
        pressStreamAcceptButton(Side.LEFT);
        pressStreamPencilButton(Side.RIGHT);
        sendTextInInputStreamField(Side.RIGHT, right);
        pressStreamAcceptButton(Side.RIGHT);
        assertChooseVideoDevice(left, right);
    }

    @Test(groups = {"PNBI-10.1.24", "TEST-1169"}, description = "Выбор расположения камеры")
    public void chooseCameraLocation() {
        clickSettingsButton();
        String leftRandomCameraLocation = getRandomCameraLocation(Side.LEFT);
        selectCameraLocation(leftRandomCameraLocation, Side.LEFT);
        String rightRandomCameraLocation = getRandomCameraLocation(Side.RIGHT);
        selectCameraLocation(rightRandomCameraLocation, Side.RIGHT);
        assertChooseCameraLocation(leftRandomCameraLocation, rightRandomCameraLocation);
    }

}
