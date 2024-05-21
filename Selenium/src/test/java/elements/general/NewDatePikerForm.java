package elements.general;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface NewDatePikerForm extends AtlasWebElement<NewDatePikerForm> {

    @FindBy(".")
    AtlasWebElement newElementYearLabel();

    @FindBy(".//div[@class='datetimepicker__extra']//div[@class='datetimepicker__year-caption au-target']")
    AtlasWebElement choseElementYearLabel();

    @FindBy("//button[@class='yearpicker__year au-target']")
    List<AtlasWebElement> yearSelection();

    @FindBy(".//div[@class='datetimepicker__month-next au-target']//*[@class='datetimepicker__month-switch-button']")
    AtlasWebElement newButtonNextMonth();

    @FindBy(".//div[@class='datetimepicker__month-name']")
    AtlasWebElement newElementCurrentMonth();

    @FindBy(".//div[not(contains(@class,'other'))]/span[contains(@class, 'datepicker__day-text')]")
    ElementsCollection<DatePickerForm> newListOfDays();

    @FindBy(".//button[@class='yearpicker__year au-target']")
    ElementsCollection<DatePickerForm> newListOfYears();

    @FindBy(".//div[@class='yearpicker au-target']")
    AtlasWebElement formListOfYears();

    @FindBy(".//button[@class='mdl-button mdl-button--primary au-target'][contains(text(),'Ок')]")
    AtlasWebElement newButtonOK();

    @FindBy(".//button[@class='mdl-button mdl-button--primary au-target'][contains(text(),'Отменить')]")
    AtlasWebElement newButtonCancel();

    @FindBy(".//div[contains(@class, 'actions')]/button[contains(@click.trigger, 'ok')]")
    AtlasWebElement okMonthPicker();

}