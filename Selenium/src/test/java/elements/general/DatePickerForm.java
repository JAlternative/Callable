package elements.general;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface DatePickerForm extends AtlasWebElement<DatePickerForm> {

    @FindBy("//div[contains(@class, 'datetimepicker--open')]//div[@class='datetimepicker__month-next au-target']")
    AtlasWebElement rightSwitchButton();

    @Name("Кнопка переключения на список лет")
    @FindBy("//div[contains(@class, 'datetimepicker--open')]//div[contains(@click.trigger, 'year')]")
    AtlasWebElement switchToYearsButton();

    @FindBy("//div[contains(@class, 'datetimepicker--open')]//div[@class='datetimepicker__month-prev au-target']")
    AtlasWebElement leftSwitchButton();

    @FindBy("//div[contains(@class, 'datetimepicker--open')]//div[contains(@class.bind, 'datepicker__day') and not(contains(@class, 'other'))]")
    ElementsCollection<DatePickerForm> listOfDays();

    @FindBy("//div[contains(@class, 'datetimepicker--open')]//button[contains(@click.trigger, 'ok')]")
    AtlasWebElement buttonOK();

    @FindBy("//div[contains(@class, 'datetimepicker--open')]//button[contains(@click.trigger, 'cancel')]")
    AtlasWebElement buttonCancel();

    @FindBy("//div[@class='yearpicker au-target']//button[contains(@class, 'yearpicker__year au-target')]")
    ElementsCollection<DatePickerForm> listOfYears();

    //Локатор ниже указывает на поле с названием текущего месяца или поле с годом, в зависимости от того на каком сейчас масштабе открыта форма
    @FindBy(".//div[contains(@class, 'datetimepicker__month-name')]")
    AtlasWebElement currentLabel();

    @FindBy(".//div[contains(@ref, 'monthsArea')]/div/span")
    ElementsCollection<AtlasWebElement> listOfMonth();

    @Name("Название выбранного/текущего месяца отображающееся в форме календаря")
    @FindBy(".//div[@class='datetimepicker__month-caption']")
    AtlasWebElement valueMonth();

    @FindBy("//div[@class='datepicker au-target']//span[contains(@class, 'datepicker') and text()={{ day }}]/..")
    ElementsCollection<AtlasWebElement> dateClickableStatus(@Param("day") int day);

}
