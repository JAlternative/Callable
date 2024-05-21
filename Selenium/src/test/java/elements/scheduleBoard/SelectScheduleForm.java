package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.openqa.selenium.WebElement;

import java.util.List;

public interface SelectScheduleForm extends AtlasWebElement {

    @Name("Кнопка плюс добавляющая график работы")
    @FindBy("//button[@title = 'Добавить график работы' ]")
    AtlasWebElement addSchedulePlusButton();

    @Name("Кнопка дискеты \"Cохранить\"")
    @FindBy("//button[@click.trigger = 'save(businessHoursList)'][not(@disabled)]")
    AtlasWebElement saveButton();

    @Name("Кнопка с типом графика : {nameOfType}")
    @FindBy("//div[@class='menu au-target is-visible']//div[@class='menu__item au-target'][contains(text(),'{{ nameOfType }}')]")
    AtlasWebElement selectTypeButton(@Param("nameOfType") String nameOfType);

    @Name("Кнопка \"Отменить\"")
    @FindBy("//div[contains(@class, 'api-render au-target')and not(contains(@class, 'hide'))]//button[@class='mdl-button mdl-button--primary au-target mdl-js-button'][contains(text(),'Закрыть')]")
    AtlasWebElement cancelButton();

    @Name("Список всех расписаний")
    @FindBy("//input[contains(@id, 'business-hours-') and @value.bind='businessHoursList.name']")
    ElementsCollection<WebElement> schedulesName();

    @Name("Кнопка выбора типа, выбранного расписания")
    @FindBy("//input[@value.bind='businessHoursList.name']/../../div//input[@id='business-hours-list-{{ index }}']")
    AtlasWebElement selectTypeField(@Param("index") int index);

    @Name("Кнопка выбора графика")
    @FindBy("//span[normalize-space(text()) = '{{ type }}']/..//text()[normalize-space(.)='{{ dateName }}']/../..")
    AtlasWebElement scheduleButton(@Param("type") String type, @Param("dateName") String dateName);

    @Name("Поле ввода даты выбранного графика")
    @FindBy("//input[@id='business-hours-list-{{ index }}']/../..//label[contains(text(), 'открытия')]/../input")
    AtlasWebElement dateOpenField(@Param("index") int index);

    @Name("Все поля дат начала всех графиков работы")
    @FindBy(".//label[text()='Дата закрытия']/../input")
    List<AtlasWebElement> allEndDates();

    @Name("Все поля дат начала всех графиков работы")
    @FindBy(".//label[text()='Тип']/../input")
    List<AtlasWebElement> allChartTypes();

    @Name("Поле ввода даты закрытия выбранного графика")
    @FindBy("//input[@id='business-hours-list-{{ index }}']/../..//label[contains(text(), 'закрытия')]/../input")
    AtlasWebElement dateCloseField(@Param("index") int index);

    @Name("Кнопка удалить выбранного графика")
    @FindBy("//input[@id='business-hours-list-{{ index }}']/../../../..//button[@click.trigger='delete(businessHoursList)']")
    AtlasWebElement deleteButton(@Param("index") int index);

    @Name("Поле названия выбранного графика")
    @FindBy("(//span[@class = 'menu__icon mdi au-target mdi-check']/../..)[2]")
    AtlasWebElement activeSchedule();

    @Name("Поле типа графика с активироанным радиобатоном")
    @FindBy("//label[contains(@class, 'is-checked')]/../..//input[contains(@value.bind, 'type')]")
    AtlasWebElement typeFieldActiveSchedule();

    @Name("Сообщение об ошибке")
    @FindBy("//span[@class='mdl-textfield__error' and (contains(text(), '24:00:00'))]")
    AtlasWebElement errorPopUp();

    @Name("Всплывающее сообщение")
    @FindBy("//div[@class= 'mdl-snackbar au-target mdl-snackbar--active']")
    AtlasWebElement popupMessage();

    @Name("Поле {fieldType} подсвеченное красным")
    @FindBy("//div[contains(@class, 'invalid')]//*[text() = '{{ fieldType }}']/..")
    AtlasWebElement errorFieldByType(@Param("fieldType") String fieldType);

    @Name("Сообщение под полем")
    @FindBy("//div[@class = '{{ className }}']//span[@class = 'mdl-textfield__error']")
    AtlasWebElement textFieldError(@Param("className") String className);

}
