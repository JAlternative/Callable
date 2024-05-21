package pages;

import elements.addOrgUnit.AddUnitForm;
import elements.addOrgUnit.Header;
import elements.general.NewDatePikerForm;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;


public interface AddOrgUnitPage extends WebPage {

    @Name("Заголовок")
    @FindBy("//header[contains(@class, 'header')]")
    Header header();

    @Name("Добавление подразеления")
    @FindBy("//div[contains(@class,'cell')]/div[contains(@class,'margin')]")
    AddUnitForm addUnitForm();

    @Name("Форма для выбора даты")
    @FindBy("//div[@class='datetimepicker datetimepicker--date au-target datetimepicker--open']")
    NewDatePikerForm datePicker();

}
