package elements.mathParameters;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;


public interface MathParametersBar extends AtlasWebElement {

    @Name("Составные элементы формы (MathParametersBar) ")
    @FindBy(".//tr/td[1]")
    ElementsCollection<AtlasWebElement> parametersDescriptions();

    @Name("При нажатии на математический параметр открывает его свойства")
    @FindBy(".//tr[@click.trigger=\"open(mathParameter)\"]")
    AtlasWebElement buttonOpenDescription();

    @Name("Снэкбар")
    @FindBy("//div[@class='mdl-snackbar au-target mdl-snackbar--active']/div")
    AtlasWebElement snackBar();

    @Name("Математический параметр по его outerId")
    @FindBy("//td[contains(text(), '{{ entity }}')]/..//td[4][contains(text(), '{{ outerId }}')]/../td[1]")
    AtlasWebElement paramByOuterId(@Param("entity") String entity, @Param("outerId") String outerId);

    @Name("Математический параметр по его outerId")
    @FindBy("//td[contains(text(), '{{ entity }}')]/..//td[4][contains(text(), '{{ outerId }}')]/../td[last()]")
    AtlasWebElement paramHiddenStatus(@Param("entity") String entity, @Param("outerId") String outerId);
}
