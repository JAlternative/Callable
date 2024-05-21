package elements.reports;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;
import pages.TreeBehavior;

public interface TargetNumberPage extends TreeBehavior {

    @Name("Кнопка Фильтра по тегам")
    @FindBy("//button/span[contains(text(), 'Фильтр по тегам')]")
    AtlasWebElement tagFilterButton();

    @Override
    @Name("Чекбокс выбранного оргЮнита")
    @FindBy("//label[contains(text(), '{{ name }}')]/..//span[3]")
    AtlasWebElement checkBoxButton(@Param("name") String name);

    @Name("Кнопка Фильтра по тегам")
    @FindBy("//label[contains(text(), 'Месяц начала')]/../input")
    AtlasWebElement startMonthInput();

    @Name("Кнопка календаря выбранной даты")
    @FindBy("//label[contains(text(), '{{ variantsButtonDate }}')]/../input")
    AtlasWebElement buttonDate(@Param("variantsButtonDate") String variantsButtonDate);

    @Name("Красный текст ошибки под датой")
    @FindBy(".//div[contains(@class, 'invalid')]/span[@class = 'mdl-textfield__error']")
    AtlasWebElement redTextUnderDate();

    @Name("Кнопка Фильтра по тегам")
    @FindBy("//label[contains(text(), 'Месяц окончания')]/../input")
    AtlasWebElement endMonthInput();

    @Override
    @Name("Шеврон выбранного оргЮнита")
    @FindBy("//label[contains(text(), '{{ name }}')]/../..//button")
    AtlasWebElement chevronButton(@Param("name") String name);


    @Name("Кнопка скачать отчет")
    @FindBy("//div[@class='au-target']//a[1]")
    AtlasWebElement downloadXSLXButton();

}
