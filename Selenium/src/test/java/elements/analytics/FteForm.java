package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface FteForm extends AtlasWebElement {
    @Name("Кнопка Закрыть форму")
    @FindBy(".//h5[@t='dialogs.fteForecast.title']/ancestor::div[2]/button")
    AtlasWebElement fteCloseForm();

    @Name("Кнопка \"рассчитать\"")
    @FindBy(".//button[@t='common.actions.calculate']")
    AtlasWebElement fteEvaluation();

    /**
     * 0st - date from
     * 1nd - date to
     */
    @Name("Поля дат начала и конца расчета")
    @FindBy(".//h5[@t='dialogs.fteForecast.title']/ancestor::div[3]/div[3]//div[contains(@class,'mdl-list__text-fields-container')]/div[{{ startEndDate }}]/input")
    AtlasWebElement fteEvaluationRangeList(@Param("startEndDate") int startEndDate);

    @Name("Некорректная дата ввода")
    @FindBy(".//h5[@t='dialogs.fteForecast.title']/ancestor::div[3]/div[3]//div[contains(@class,'mdl-list__text-fields-container')]/div[{{ startEndDate }}]/span")
    AtlasWebElement fteFormDataIncorrect(@Param("startEndDate") int startEndDate);

    @Name("Некорректная дата ввода элемент")
    @FindBy("//span[contains(text(),'Некорректная дата')]")
    AtlasWebElement fteFormDataIncorrectElement();

    @Name("Кнопки календаря открытие формы даты")
    @FindBy(".//label[contains(text(), 'Дата')]/../button")
    ElementsCollection<AtlasWebElement> dateFormes();

    @Name("Поле выбора использования исторических данных")
    @FindBy(".//input[@id='fte-strategy']")
    AtlasWebElement fteStrategy();

    @Name("Форма листа исторических данных")
    @FindBy(".//div[contains(text(),'{{ strategy }}')]")
    AtlasWebElement fteStrategyList(@Param("strategy") String strategy);

    @Name("Поле выбора метода")
    @FindBy(".//input[@id='fte-type']")
    AtlasWebElement methodButton();

    @Name("Элементы из выпадающего меню \"Метод\" на вкладке \"Расчет ресурсного запроса\"")
    @FindBy(".//div[@menu='fte-type']/div[{{ algorithmType }}]")
    AtlasWebElement fteMethod(@Param("algorithmType") String algorithmType);

    @Name("Форма листа выбора алгоритма")
    @FindBy(".//div[contains(text(),'{{ algorithm }}')]")
    AtlasWebElement fteAlgorithmList(@Param("algorithm") String algorithm);

    @Name("Поле листа выбора алгоритма")
    @FindBy(".//div[contains(@class.bind,'selectedAltAlgorithm')]/input")
    AtlasWebElement fteAlgorithm();

    @Name("Поле выбора магазина для подрезделения для импорта")
    @FindBy(".//input[@id='org-unit-for-import-fte']")
    AtlasWebElement fteOrgUnitImport();

    @Name(("Поле для ввода оргЮнита"))
    @FindBy(".//div[contains(@class, 'au-target is-visible')]//input[contains(@id, 'search-input')]")
    AtlasWebElement orgUnitField();

    @Name("Лист выбора магазина для подразделения для импорта")
    @FindBy(".//div[contains(@class, 'au-target is-visible')]//div[contains(@class, 'menu__item') and not(contains(@class, 'aurelia-hide'))]")
    ElementsCollection<AtlasWebElement> fteOrgUnitImportList();

    @Name("Чекбокс \"Перерасчитать плановую численность\" ")
    @FindBy(".//span[@class='mdl-checkbox__tick-outline']")
    AtlasWebElement fteReEvalFlag();

    @Name("Ссылка \"Данные для расчета\" ")
    @FindBy(".//a[@class='link au-target']")
    AtlasWebElement fteDataHref();

}

