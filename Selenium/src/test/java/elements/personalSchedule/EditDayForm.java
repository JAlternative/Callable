package elements.personalSchedule;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EditDayForm extends AtlasWebElement {

    @Name("Меню троеточия")
    @FindBy(".//button[@id='schedule-request-menu']")
    AtlasWebElement buttonDotsMenu();

    @Name("Кнопка закрытия формы")
    @FindBy(".//button[@click.trigger='close()']")
    AtlasWebElement closeFormButton();

    @Name("Варианты действий с запросом")
    @FindBy("//div[@class='menu menu--right au-target is-visible']/div[contains(text(),'{{ type }}')]")
    AtlasWebElement typeButtons(@Param("type") String type);

    @Name("Кнопка \"Удалить\" в меню троеточия")
    @FindBy("//div[@class='menu__item au-target'][contains(text(),'Удалить')]")
    AtlasWebElement buttonDelete();

    @Name("Кнопка \"Изменить\" в меню троеточия")
    @FindBy("//div[@class='menu__item au-target'][contains(text(),'Изменить')]")
    AtlasWebElement buttonChange();

    @Name("Кнопка \"Изменить\" в меню запроса")
    @FindBy("//button[@class = 'mdl-button mdl-button--primary au-target mdl-js-button']//span[contains(text(),'Изменить')]")
    AtlasWebElement buttonChangeInShiftEdit();

    @Name("Кнопка \"Подтвердить\" в меню троеточия")
    @FindBy("//div[@class='menu__item au-target'][contains(text(),'Подтвердить')]")
    AtlasWebElement buttonConfirm();

    @Name("Кнопка \"Создать\"")
    @FindBy("//*[@click.trigger = 'createScheduleRequest()']")
    AtlasWebElement buttonCreateShift();

    @Name("Кнопка \"Изменить\"")
    @FindBy("//*[@click.trigger = 'editScheduleRequest()']")
    AtlasWebElement buttonEditShift();

    @Name("Кнопка выбора времени конца или начала запроса")
    @FindBy(".//label[text()='{{ endOrStart }}']/../../div[contains(@class,'mdl-list__text-field--33')]/button")
    AtlasWebElement buttonStartOrEndTimeRequest(@Param("endOrStart") String endOrStart);

    @Name("Кнопка выбора даты конца или начала запроса")
    @FindBy(".//label[text()='{{ endOrStart }}']/..//i")
    AtlasWebElement buttonStartOrEndDateRequest(@Param("endOrStart") String endOrStart);

    @Name("Поле выбора времени конца или начала смены")
    @FindBy(".//label[text()='{{ endOrStart }}']/../../div[contains(@class,'mdl-list__text-field--33')]/input")
    AtlasWebElement inputStartOrEndTimeRequest(@Param("endOrStart") String endOrStart);

    @Name("Поле ввода даты начала или конца")
    @FindBy(".//label[text()='{{ endOrStart }}']/../input")
    AtlasWebElement dateStartOrEndInput(@Param("endOrStart") String endOrStart);

    @Name("Шеврон выбора типа запроса")
    @FindBy("//label[@for='schedule-request-type']/../button")
    AtlasWebElement selectTypeButton();

    @Name("Кнопки с вариантами типов смен")
    @FindBy(".//div[@class='menu au-target is-visible']//div[@click.delegate='selectScheduleRequestType(type)' and (contains(text(), '{{ type }}'))]")
    AtlasWebElement typeButton(@Param("type") String type);

    @Name("Кнопки с вариантами типов смен")
    @FindBy(".//div[@class='menu au-target is-visible']//div[@click.delegate='selectScheduleRequestType(type)'][not(contains(@class, 'aurelia-hide'))]")
    ElementsCollection<AtlasWebElement> typeButtons();

    @Name("Кнопка раскрытия меню периодичности")
    @FindBy("//label[@t='common.essentials.periodicity']/../button")
    AtlasWebElement periodicitySelectButton();

    @Name("Кнопка с выбранной периодичностью")
    @FindBy("//div[@class='menu menu--short menu--bottom au-target is-visible']//div")
    ElementsCollection<AtlasWebElement> periodicityTypeButtons();

    @Name("Панель выбора периодичности")
    @FindBy("//div[@menu ='schedule-request-periodicity']")
    AtlasWebElement periodicityPanel();

    @Name("Кнопка выбора типа переодичности")
    @FindBy("//label[@for='event-periodicity']/..//button")
    AtlasWebElement selectEventRepeatButton();

    @Name("Кнопка периодичности события")
    @FindBy("//div[contains(text(), '{{ repeatType }}')]")
    AtlasWebElement eventRepeatButton(@Param("repeatType") String repeatType);

    @Name("Поле ввода даты окончания повтора")
    @FindBy("//div[contains (@show.bind, 'scheduleRequest.periodicity')]//input")
    AtlasWebElement dateRepeatEndField();

}
