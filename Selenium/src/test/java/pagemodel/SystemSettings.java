package pagemodel;

import guice.TestModule;
import io.qameta.allure.Allure;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.SystemSettingsPage;
import reporting.TestListener;
import com.google.inject.Inject;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.GoToPageSection;
import wfm.components.schedule.SystemProperties;
import wfm.components.utils.Section;
import wfm.models.SystemProperty;
import wfm.repository.SystemPropertyRepository;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class SystemSettings {
    private final Section SECTION = Section.SYSTEM_SETTINGS;
    private static final Logger LOG = LoggerFactory.getLogger(ScheduleBoard.class);

    @Inject
    private SystemSettingsPage ssp;

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriver() {
        ssp.getWrappedDriver().quit();
    }

    @Step("Перейти в раздел \"Системные настройки\"")
    private void goToSystemSettings() {
        new GoToPageSection(ssp).getPage(SECTION, 60);
    }

    @Step("Кликнуть по строке {itemTitle} в таблице системных настроек")
    private void clickTableItem(String itemTitle) {
        ssp.systemSettingTable()
                .waitUntil("Страница системных настроек не отображается", DisplayedMatcher.displayed(), 30);
        new Actions(ssp.getWrappedDriver()).moveToElement(ssp.systemSettingTable()).perform();
        ssp.systemSettingTable().itemSystemSettingByTitle(itemTitle)
                .waitUntil("Пункт настроек " + itemTitle + " не найден", DisplayedMatcher.displayed(), 5);
        ssp.systemSettingTable().itemSystemSettingByTitle(itemTitle).click();
    }

    @Step("Нажать кнопку всплывающего окна подтверждения {buttonTitle}")
    public void pressButton(String buttonTitle) {
        ssp.dialogWindow().popUpWindowButton(buttonTitle).click();
        Allure.addAttachment("Нажата кнопка ", buttonTitle);
        LOG.info("Нажата кнопка " + buttonTitle);
    }

    @Step("В поле \"Значение\" изменить значение параметра (true/false)")
    private void setParameterValue(Boolean parameterValue) {
        new Actions(ssp.getWrappedDriver()).moveToElement(ssp.systemSettingTable()).perform();
        ssp.dialogWindow()
                .waitUntil("Нет окна ввода параметров", DisplayedMatcher.displayed(), 3);
        new Actions(ssp.getWrappedDriver()).moveToElement(ssp.dialogWindow()).perform();
        Allure.addAttachment("Меняем значение параметра с " + !parameterValue, " на " + parameterValue);
        LOG.info("Меняем значение параметра с {} на {}", !parameterValue, parameterValue);
        ssp.dialogWindow().inputBox().clear();
        ssp.dialogWindow().inputBox().sendKeys(String.valueOf(parameterValue));
    }

    /**
     * @param expectedValue - значение, отличающееся от того, что было в начале теста
     * @param itemTitle     название параметра
     * @param isMatch       true, если expectedValue и actualValue должны совпадать; иначе false
     */
    @Step("Проверка изменения/отмены параметра { itemTitle }")
    private void checkingParameterChanges(Boolean expectedValue, String itemTitle, boolean isMatch) {
        Boolean actualValue = (Boolean) SystemPropertyRepository.getSystemProperty(SystemProperties.SCHEDULE_BOARD_HELP_INDICATOR).getValue();
        if (isMatch) {
            Assert.assertEquals(actualValue, expectedValue, "Значение \"" + itemTitle + "\" не изменилось");
            checkingValueDisplayed(itemTitle, String.valueOf(expectedValue), isMatch);
        } else {
            Assert.assertNotEquals(actualValue, expectedValue, "Значение \"" + itemTitle + "\" изменилось");
            checkingValueDisplayed(itemTitle, String.valueOf(!expectedValue), isMatch);
        }

    }

    @Step("Проверка отображения значения для параметра {itemTitle} ")
    private void checkingValueDisplayed(String itemTitle, String expectedItemValue, boolean isMatch) {
        ssp.systemSettingTable()
                .waitUntil("Страница системных настроек не отображается", DisplayedMatcher.displayed(), 30);
        new Actions(ssp.getWrappedDriver()).moveToElement(ssp.systemSettingTable()).perform();
        ssp.systemSettingTable().itemSystemSettingByTitle(itemTitle)
                .waitUntil("Пункт настроек " + itemTitle + " не найден", DisplayedMatcher.displayed(), 3);
        new Actions(ssp.getWrappedDriver()).moveToElement(ssp.systemSettingTable().itemSystemSettingByTitle(itemTitle)).perform();
        ssp.systemSettingTable().itemValue(expectedItemValue)
                .waitUntil("Требуемое значение \"" + expectedItemValue + "\" для \"" + itemTitle + "\" не отобразилось на странице.", DisplayedMatcher.displayed(), 1);

        Allure.addAttachment("Проверяемое значение " + expectedItemValue + ((isMatch) ? " после подтверждения изменения, корректно изменилось" : " после отмены изменения, осталось прежним"), "");
        LOG.info("Проверяемое значение {} {} ", expectedItemValue, (isMatch) ? " после подтверждения изменения, корректно изменилось" : "после отмены изменения, осталось прежним");
    }

    @Test(groups = {"TK2688-1"}, description = "Изменение параметра \"Индикатор дополнительной информации\"")
    @Link(name = "Ссылка на тест-кейс", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460864")
    @Owner(value = "a.bugorov")
    private void changeAdditionalInformationIndicator() {
        SystemProperty sysProp = SystemPropertyRepository.getSystemProperty(SystemProperties.SCHEDULE_BOARD_HELP_INDICATOR);
        String itemTitle = sysProp.getTitle();
        Boolean actualValue = (Boolean) sysProp.getValue();
        Boolean oppositeValue = !actualValue;
        goToSystemSettings();
        clickTableItem(itemTitle);
        setParameterValue(oppositeValue);
        pressButton("Изменить");
        checkingParameterChanges(oppositeValue, itemTitle, true);
    }

    @Test(groups = {"TK2688-2"}, description = "Отмена изменения параметра \"Индикатор дополнительной информации\"")
    @Link(name = "Ссылка на тест-кейс", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460864")
    @Owner(value = "a.bugorov")
    public void notChangeAdditionalInformationIndicator() {
        SystemProperty sysProp = SystemPropertyRepository.getSystemProperty(SystemProperties.SCHEDULE_BOARD_HELP_INDICATOR);
        String itemTitle = sysProp.getTitle();
        Boolean actualValue = (Boolean) sysProp.getValue();
        Boolean oppositeValue = !actualValue;
        goToSystemSettings();
        clickTableItem(itemTitle);
        setParameterValue(oppositeValue);
        pressButton("Отменить");
        checkingParameterChanges(oppositeValue, itemTitle, false);
    }

}

