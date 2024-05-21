package testutils;

import elements.scheduleBoard.SelectScheduleForm;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import wfm.components.schedule.ScheduleType;
import wfm.models.BusinessHours;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static utils.tools.CustomTools.systemSleep;
import static utils.tools.Format.UI;

public class ActWithSchedule {

    //Стандартное имя графика при добавлении нового
    private static final String SCHEDULE_NAME = "Новый график";
    private static final Logger LOG = LoggerFactory.getLogger(ActWithSchedule.class);
    private final SelectScheduleForm sf;
    //Порядковый номер графика из списка
    private int index;

    public ActWithSchedule(SelectScheduleForm sf) {
        this.sf = sf;
    }

    @Step("Нажать на значок \"+\" Добавить новый график")
    public void clickOnPlusButton() {
        sf.addSchedulePlusButton()
                .waitUntil("Ожидание появления формы выбора графика", DisplayedMatcher.displayed(), 25);
        index = getListIndex("Новый график");
        sf.addSchedulePlusButton().click();
    }

    @Step("В добавленной строке выбрать дату открытия {dateStart}")
    public void dateOpenSelect(LocalDate dateStart) {
        sf.dateOpenField(getListIndex(SCHEDULE_NAME))
                .waitUntil("Ожидание появления поля даты ", DisplayedMatcher.displayed(), 5);
        sf.dateOpenField(getListIndex(SCHEDULE_NAME)).clear();
        sf.dateOpenField(getListIndex(SCHEDULE_NAME)).sendKeys(dateStart.format(UI.getFormat()));
    }

    @Step("В добавленной строке выбрать дату закрытия {dateEnd}")
    public void dateCloseSelect(LocalDate dateEnd) {
        sf.dateCloseField(getListIndex(SCHEDULE_NAME))
                .waitUntil("Ожидание появления поля даты ", DisplayedMatcher.displayed(), 5);
        sf.dateCloseField(getListIndex(SCHEDULE_NAME)).clear();
        sf.dateCloseField(getListIndex(SCHEDULE_NAME)).sendKeys(dateEnd.format(UI.getFormat()));
    }

    @Step("Выбрать тип расписания: \"{type}\"")
    public void selectScheduleType(ScheduleType type) {
        sf.selectTypeField(getListIndex(SCHEDULE_NAME)).click();
        sf.selectTypeButton(type.getNameOfType())
                .waitUntil("Ожидания меню выбора типа", DisplayedMatcher.displayed(), 5);
        LOG.info("Выбран тип расписания: {}", sf.selectTypeButton(type.getNameOfType()).getText());
        sf.selectTypeButton(type.getNameOfType()).click();
    }

    @Step("Активировать радио-кнопку напротив графика работы с типом \"{scheduleNameService.type}\"")
    public void selectActiveSchedule(BusinessHours scheduleNameService) {
        systemSleep(1); //метод используется в неактуальных тестах
        WebElement webElement = sf.scheduleButton(scheduleNameService.getEnumType().getNameOfType(), scheduleNameService.getDisplayedTimePeriod());
        if (webElement.isDisplayed()) {
            webElement.click();
        } else {
            Assert.fail("Данный график уже выбран");
        }
        LOG.info("Выбран тип графика работы подразделения: {}", scheduleNameService.getType());
    }

    @Step("Нажать на кнопку \"Сохранить\"")
    public void clickOnSaveButton() {
        sf.saveButton().click();
        try {
            if (sf.popupMessage().isDisplayed()) {
                String message = "Было отображено всплывающее сообщение с текстом: " + sf.popupMessage().getText();
                LOG.info(message);
                Allure.addAttachment("Всплывающий popUp", message);
                sf.waitUntil(
                        "Форма с графиками все еще отображена, перед этим был отображен поп-ап с текстом об ошибке",
                        Matchers.not(DisplayedMatcher.displayed()),
                        10
                );
            }
        } catch (NoSuchElementException ignored) {
        }
        sf.saveButton().waitUntil("Изменения не были сохранены", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    //используется когда форма не должна закрываться или когда мы должны заматчить ошибку
    @Step("Нажать на кнопку \"Сохранить\"")
    public void clickOnSaveButtonWithError() {
        sf.saveButton().click();
    }

    @Step("Нажать кнопку отменить")
    private void clickOnCancelButton() {
        sf.cancelButton().click();
    }

    @Step("Нажать на кнопку удалить")
    public void clickDeleteButton(String scheduleName) {
        sf.deleteButton(getListIndex(scheduleName)).click();
    }

    /**
     * Определение порядкового номера графика по имени
     *
     * @param scheduleName имя графика работы
     * @return порядковый номер в списке графиков
     */
    private int getListIndex(String scheduleName) {
        index = 0;
        List<String> scheduleNameList = sf.schedulesName()
                .stream()
                .map(webElement -> webElement.getAttribute("value"))
                .collect(Collectors.toList());
        for (String name : scheduleNameList) {
            if (name.contains(scheduleName)) {
                index = scheduleNameList.indexOf(name);
            }
        }
        return index;
    }

    @Step("*особые действия: узнать номер активного графика")
    public String getActiveScheduleId(int orderNumber) {
        String id = "business-hours-" + orderNumber;
        return sf.findElement(By.id(id)).getAttribute("value").substring(14);
    }

}
