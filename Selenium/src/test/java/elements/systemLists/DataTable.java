package elements.systemLists;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface DataTable extends AtlasWebElement {

    @Name("Все столбцы Имени")
    @FindBy("//tbody//tr/td[1]")
    ElementsCollection<AtlasWebElement> namesInTable();

    @Name("Коды всех комментариев в таблице")
    @FindBy("//tbody//tr/td[2]")
    ElementsCollection<AtlasWebElement> codesInTable();

    @Name("Комментарий по его имени")
    @FindBy("//td[contains(text(), '{{ name }}')][1]")
    AtlasWebElement commentByName(@Param("name") String name);

    @Name("Все поля в ряду по содержанию одной ячейки этого ряда")
    @FindBy("//td[contains(text(), '{{ name }}')]/ancestor::tr/td")
    ElementsCollection<AtlasWebElement> allRowFieldsByOneRow(@Param("name") String name);

    @Name("Все ряды в теле таблицы")
    @FindBy("//tbody//tr")
    ElementsCollection<AtlasWebElement> allRowsInTable();
}
