package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface CorrectionSlider extends AtlasWebElement {

    @Name("Слайдер выбора значения коррекции")
    @FindBy(".//input[@type='range']")
    AtlasWebElement slider();

    @Name("Поле выбора даты начала")
    @FindBy(".//input[@id='kpi-start-date']")
    AtlasWebElement kpiStartDate();

    @Name("Поле выбора даты окончания")
    @FindBy(".//input[@id='kpi-end-date']")
    AtlasWebElement kpiEndDate();

    @Name("Кнопка \"Отменить\"")
    @FindBy(".//button[1]/span")
    AtlasWebElement cancelButton();

    @Name("Кнопка \"Сохранить\" в слайдере коррекции")
    @FindBy(".//button[2]/span")
    AtlasWebElement okButton();

    @Name("Неактивная кнопка \"Сохранить\" в слайдере коррекции")
    @FindBy(".//button[2][@disabled]/span")
    AtlasWebElement offOkButton();

    @Name("Поле ввода значения коррекции")
    @FindBy(".//input[@value.bind='correction.delta']")
    AtlasWebElement fieldForCorrection();

    @Name("Поле ввода коэффициента коррекции")
    @FindBy(".//input[@value.bind='correction.coefficient'][@type = 'number']")
    AtlasWebElement fieldCoefficientCorrection();

    @Name("Поле выбора типа распределения")
    @FindBy(".//input[@value.bind = 'correction.distribution.name']")
    AtlasWebElement fieldDistribution();

    @Name("Варианты типов распределения")
    @FindBy(".//div[@class='menu au-target is-visible']/div")
    List<AtlasWebElement> variantsForDistribution();

    @Name("Индикатор ошибки ввода значения коррекции")
    @FindBy(".//div[contains(@class.bind, 'correction.delta')]")
    AtlasWebElement fieldForCorrectionIndicateRed();

}