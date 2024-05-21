package testutils;

import com.mchange.util.AssertException;
import elements.general.DatePickerForm;
import elements.general.TimePickerForm;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.hamcrest.Matchers;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import wfm.components.utils.MonthsEnum;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

import static utils.tools.CustomTools.systemSleep;

public class DatePicker {

    private DatePickerForm datePickerForm;
    private TimePickerForm timePickerForm;

    public DatePicker(DatePickerForm datePickerForm) {
        this.datePickerForm = datePickerForm;
    }

    public DatePicker(TimePickerForm timePickerForm) {
        this.timePickerForm = timePickerForm;
    }

    @Step("Выбрать дату {date} в календаре")
    public void pickDate(LocalDate date) {
        datePickerForm.waitUntil("Date picker form is not displayed ", DisplayedMatcher.displayed(), 5);
        WebElement currentMonth = datePickerForm.currentLabel();
        String currentMonthText = currentMonth.getText();
        String[] currentMonthSeparatedText = currentMonthText.split(" ");
        String yearString = String.valueOf(date.getYear());
        String dayString = String.valueOf(date.getDayOfMonth());
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru"));
        while (!currentMonthSeparatedText[0].equalsIgnoreCase(monthName)) {
            datePickerForm.rightSwitchButton().click();
            datePickerForm.currentLabel()
                    .waitUntil("Current month is not displayed", DisplayedMatcher.displayed(), 5);
            String tempMY = datePickerForm.currentLabel().getText();
            String[] tempMYSplit = tempMY.split(" ");
            currentMonthSeparatedText[0] = tempMYSplit[0];
            currentMonthSeparatedText[1] = tempMYSplit[1];
        }
        datePickerForm.switchToYearsButton()
                .waitUntil("Years label is not displayed", DisplayedMatcher.displayed(), 5);
        if (!Objects.equals(yearString, currentMonthSeparatedText[1])) {
            datePickerForm.switchToYearsButton().click();
            datePickerForm.listOfYears()
                    .filter(DatePickerForm -> DatePickerForm.getText().contains(yearString))
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertException("This year is not listed: " + yearString))
                    .click();
        }
        systemSleep(1); //добавлен, потому что список дней перемещается не сразу, а какое то время прокручивается, без этого может возникать ошибка
        datePickerForm.listOfDays()
                .filter(DatePickerForm -> DatePickerForm.getText().equals(dayString))
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertException("This day is not listed: " + dayString))
                .click();
    }

    /**
     * Метод проверяет, кликабельная ли дата в календаре
     */
    public boolean checkDateElementClickable(LocalDate date) {
        ElementsCollection<AtlasWebElement> dateElement = datePickerForm.dateClickableStatus(date.getDayOfMonth());
        return !dateElement.get(dateElement.size() - 1).getAttribute("class").contains("invalid");
    }

    public void pickMonth(LocalDate date) {
        datePickerForm.waitUntil("Форма календаря не была отображена", DisplayedMatcher.displayed(), 5);
        datePickerForm.currentLabel().waitUntil("Не был отображен масштаб календаря", DisplayedMatcher.displayed(), 5);
        int year = date.getYear();
        String monthName = date.getMonth().getDisplayName(TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("ru"));
        String certainYear = datePickerForm.currentLabel().getText();
        while (!certainYear.contains(String.valueOf(year))) {
            if (Integer.parseInt(certainYear) > year) {
                datePickerForm.leftSwitchButton().click();
                certainYear = datePickerForm.currentLabel().getText();
            }
            if (Integer.parseInt(certainYear) < year) {
                datePickerForm.rightSwitchButton().click();
                certainYear = datePickerForm.currentLabel().getText();
            }
        }
        datePickerForm.listOfMonth()
                .waitUntil(Matchers.hasSize(Matchers.equalTo(MonthsEnum.values().length)))
                .filter(extendedWebElement -> extendedWebElement.getText().contains(monthName))
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Не был найден месяц с названием " + monthName))
                .click();
    }

    public void pickTime(LocalTime time) {
        timePickerForm.waitUntil(DisplayedMatcher.displayed());
        String hourString = time.format(DateTimeFormatter.ofPattern("HH"));
        String minuteString = time.format(DateTimeFormatter.ofPattern("mm"));
        timePickerForm.elementsHours()
                .filter(TimePickerForm -> TimePickerForm.getText().contains(hourString)).get(0).click();
        timePickerForm.elementsMinutes()
                .waitUntil(Matchers.hasSize(Matchers.equalTo(12)));
        timePickerForm.elementsMinutes()
                .filter(TimePickerForm -> TimePickerForm.getText().contains(minuteString)).stream()
                .findFirst()
                .orElseThrow(() -> new AssertException("This minute is not listed: " + minuteString))
                .click();
        timePickerForm.buttonOK().click();
    }

    @Step("Кликнуть на стрелочку вправо в верхней части окна.")
    public void rightYearSwitch() {
        datePickerForm.waitUntil("Форма календаря не была отображена", DisplayedMatcher.displayed(), 5);
        int currentYear = Integer.parseInt(datePickerForm.currentLabel().getText());
        datePickerForm.rightSwitchButton().click();
        int yearAfter = Integer.parseInt(datePickerForm.currentLabel().getText());
        Assert.assertEquals(currentYear + 1, yearAfter, "Дата не поменялась");
        Allure.addAttachment("Проверка",
                             "До перехода влево был временной промежуток " + currentYear + " после " + yearAfter);
    }

    @Step("Кликнуть на стрелочку влево в верхней части окна.")
    public void leftYearSwitch() {
        datePickerForm.waitUntil("Форма календаря не была отображена", DisplayedMatcher.displayed(), 5);
        int currentYear = Integer.parseInt(datePickerForm.currentLabel().getText());
        datePickerForm.leftSwitchButton().click();
        int yearAfter = Integer.parseInt(datePickerForm.currentLabel().getText());
        Assert.assertEquals(currentYear - 1, yearAfter, "Дата не поменялась");
        Allure.addAttachment("Проверка",
                             "До перехода влево был временной промежуток " + currentYear + " после " + yearAfter);
    }

    @Step("Нажать \"Ок\".")
    public void okButtonClick() {
        if (datePickerForm != null) {
            datePickerForm.buttonOK().click();
        } else {
            timePickerForm.buttonOK().click();
        }
        systemSleep(1.5);
    }

    public void okButtonClickWithoutStep() {
        if (datePickerForm != null) {
            datePickerForm.buttonOK().click();
        } else {
            timePickerForm.buttonOK().click();
        }
    }

    @Step("Нажать \"Отмена\".")
    public void cancelButtonClick() {
        if (datePickerForm != null) {
            datePickerForm.buttonCancel().click();
        } else {
            timePickerForm.buttonCancel().click();
        }
        systemSleep(1.5); //метод используется в неактуальных тестах
    }
}
