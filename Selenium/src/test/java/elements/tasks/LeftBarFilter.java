package elements.tasks;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface LeftBarFilter extends AtlasWebElement {

    @FindBy("//div[@class='mdl-cell mdl-cell--relative mdl-cell--3-col']//div[@class='mdl-list']//div[4]//button[1]//i[1]")
    AtlasWebElement statusButton();

    @FindBy("//div[@show.bind='showFilters[0]']/span[2]")
    List<AtlasWebElement> chooseStatus();

    @FindBy("//div[@class='mdl-list']//div[12]//button[1]")
    AtlasWebElement dateButton();

    @FindBy("//span[@class='mdl-radio__ripple-container mdl-js-ripple-effect mdl-ripple--center']")
    List<AtlasWebElement> choseDate();

    @FindBy("//div[@class='mdl-list']//div[19]//button[1]")
    AtlasWebElement employeeButton();

    @FindBy("//div[@show.bind='showFilters[2]']/span[2]")
    List<AtlasWebElement> choseEmployee();

    @FindBy("//div[@class='mdl-list']//div[26]//button[1]")
    AtlasWebElement tagButton();

    @FindBy("//div[@show.bind='showFilters[3]']/span[2]")
    List<AtlasWebElement> choseTag();

    @FindBy("//div[@class='mdl-cell mdl-cell--relative mdl-cell--3-col']//div[@class='mdl-list']//div[16]//button[1]")
    AtlasWebElement subunitButton();

    @FindBy("//div[@show.bind='showFilters[1]']/span[2]")
    List<AtlasWebElement> chooseSection();

    ////////////сравнения для инсертов
    @FindBy("//div[@show.bind='showFilters[0]']")
    List<AtlasWebElement> insertStatusComp();

    @FindBy("//div[@show.bind='showFilters[5]']")
    List<AtlasWebElement> insertDateComp();

    @FindBy("//div[@show.bind='showFilters[1]']")
    List<AtlasWebElement> insertSubunitComp();

    @FindBy("//div[@show.bind='showFilters[2]']")
    List<AtlasWebElement> insertEmployeeComp();

    @FindBy("//div[@show.bind='showFilters[3]']")
    List<AtlasWebElement> insertTagComp();


//////////////// поиски для сравнений

    @FindBy("//div[@show.bind='showFilters[0]']/span[1]/span")
    List<AtlasWebElement> buttonsToFindStatus();

    @FindBy("//div[@show.bind='showFilters[2]']/span[1]")
    List<AtlasWebElement> buttonsToFindEmployee();

    @FindBy("//div[@show.bind='showFilters[3]']/span[1]")
    List<AtlasWebElement> buttonsToFindTag();

    @FindBy("//div[@show.bind='showFilters[1]']/span[1]")
    List<AtlasWebElement> buttonsToFindSubunit();

    @FindBy("//div[@show.bind='showFilters[5]']/span[1]/span")
    List<AtlasWebElement> buttonsToFindDate();

////////////////это на включены или не включены чекбоксы и радиобатоны

    @FindBy("//div[@show.bind='showFilters[0]']/span[2]/label")
    List<AtlasWebElement> onOrOffStatusCheckbox();

    @FindBy("//div[@show.bind='showFilters[1]']/span[2]/label")
    List<AtlasWebElement> onOrOffSubunitCheckbox();

    @FindBy("//div[@show.bind='showFilters[2]']/span[2]/label")
    List<AtlasWebElement> onOrOffEmployeeCheckbox();

    @FindBy("//div[@show.bind='showFilters[3]']/span[2]/label")
    List<AtlasWebElement> onOrOffTagCheckbox();

    @FindBy("//div[@show.bind='showFilters[5]']/span[2]/label")
    List<AtlasWebElement> onOrOffDateCheckbox();

}
