package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EventForm extends AtlasWebElement {

    @Name("Поле ввода количества человек")
    @FindBy("//input[@id='event-value']")
    AtlasWebElement peopleValueField();

    @Name("Поля выбора даты начла и конца")
    @FindBy(".//label[text() = '{{ endOrStart }}']/..//input")
    AtlasWebElement dateStartOrEndField(@Param("endOrStart") String endOrStart);

    @Name("Поле ввода даты окончания повтора")
    @FindBy("(//div[contains (@show.bind, 'repeatRule')]//input)[2]")
    AtlasWebElement dateRepeatEndField();

    @Name("Поле ввода времени напротив даты окончания или начала")
    @FindBy(".//label[text()='{{ endOrStart }}']/../..//div[contains(@class,'mdl-list__text-field--33 mdl-js-textfield')]/input")
    AtlasWebElement timeEndOrStartField(@Param("endOrStart") String endOrStar);

    @Name("Кнопка \"Cоздать\"")
    @FindBy("//button[contains(@t, 'create')]")
    AtlasWebElement createButton();

    @Name("Кнопка \"Изменить\"")
    @FindBy(".//button[contains(@t, 'common.actions.change') and contains(@click.trigger, 'CHANGE')]")
    AtlasWebElement changeButton();

    @Name("Кнопка выбора типа события")
    @FindBy("//label[@for='event-type']/..//button")
    AtlasWebElement selectEventTypeButton();

    @Name("Кнопка выбранного типа")
    @FindBy("//div[contains(text(), '{{ eventType }}')]")
    AtlasWebElement eventTypeButton(@Param("eventType") String eventType);

    @Name("Кнопка выбора типа переодичности")
    @FindBy("//label[@for='event-periodicity']/..//button")
    AtlasWebElement selectEventRepeatButton();

    @Name("Кнопка периодичности события")
    @FindBy("//div[contains(text(), '{{ repeatType }}')]")
    AtlasWebElement eventRepeatButton(@Param("repeatType") String repeatType);

    @Name("Кнопка крестик закрытия формы эвента")
    @FindBy("//div[@show.bind = 'showEventDialog']//i[@class = 'mdi mdi-close']")
    AtlasWebElement closeEventForm();

    @Name("Кнопка изменить в форме \"Изменить\" ")
    @FindBy("//button[@show.bind = \"actionVariant === 'CHANGE'\"]/span[@class = 'mdl-button__ripple-container']")
    AtlasWebElement changeButtonInChange();

    @Name("Радобаттон выбора изменения события")
    @FindBy("//div[@show.bind = 'showEventDialog']//span[contains(text(), '{{ whatToChange }}')]/..//span[contains(@class, 'mdl-radio__ripple-container')]")
    AtlasWebElement radioButtonChange(@Param("whatToChange") String whatToChange);
}
