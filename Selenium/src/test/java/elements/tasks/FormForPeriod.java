package elements.tasks;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

//TODO удалить
public interface FormForPeriod extends AtlasWebElement {
    @FindBy("//input[@id='custom-mode']")
    AtlasWebElement PeriodButtonInForm();

    @FindBy("//div[@class='menu au-target is-visible']/div")
    List<AtlasWebElement> listOfPeriodInForm();

    @FindBy("//input[@type='number']")
    AtlasWebElement fildForPeriodInForm();

    @FindBy("//input[@id='duration-mode']")
    AtlasWebElement duration();

    @FindBy("//div[@class='menu au-target is-visible']/div")
    AtlasWebElement listOfDuration();

    @FindBy("//div[@class='mdl-list mdl-list--no-margin']//button[@type='button'][contains(text(),'Отменить')]")
    AtlasWebElement cancelPeriodFormButton();

    @FindBy("//button[contains(text(),'Применить')]")
    AtlasWebElement okBauttonForPeriodForm();

}
