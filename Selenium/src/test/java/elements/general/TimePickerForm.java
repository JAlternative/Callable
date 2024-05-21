package elements.general;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface TimePickerForm extends AtlasWebElement<TimePickerForm> {

    @Name("Часы в форме выбора времени")
    @FindBy("//div[contains(@class, 'datetimepicker--open')]//span[@class='timepicker__time-text']")
    ElementsCollection<TimePickerForm> elementsHours();

    @Name("Минуты в форме выбора времени")
    @FindBy("//div[contains(@class, 'datetimepicker--open')]//span[@class='timepicker__time-text']")
    ElementsCollection<TimePickerForm> elementsMinutes();

    @FindBy("//div[contains(@class, 'datetimepicker--open')]//button[contains(@click.trigger, 'ok')]")
    AtlasWebElement buttonOK();

    @FindBy("//div[contains(@class, 'datetimepicker--open')]//button[contains(@click.trigger, 'cancel')]")
    AtlasWebElement buttonCancel();

}
