package elements.batchCalculation;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface PlannedStrengthForecastForm extends AtlasWebElement {
    @Name("Лист стратегии")
    @FindBy("//div[@class='menu au-target is-visible']//div[@class='menu__item au-target'][{{ strategy }}]")
    AtlasWebElement strategyList(@Param("strategy") int strategy);

    @Name("Поле стратегии")
    @FindBy("//input[@id='staff-number-strategy']")
    AtlasWebElement strategyInput();

    @Name("Лист метода")
    @FindBy("//div[@class='menu au-target is-visible']//div[@class='menu__item au-target'][{{ method }}]")
    AtlasWebElement methodList(@Param("method") int method);

    @Name("Поле метода")
    @FindBy(".//input[@id='staff-number-type']")
    AtlasWebElement methodInput();

    @Name("Необходимое количество месяцев подряд без изменений в численности штата")
    @FindBy("//input[@t='[title]dialogs.staffNumber.tooltip.constantNoeRequirement']")
    AtlasWebElement staffSize();

    @Name("Максимальное положительное изменение численности")
    @FindBy("//input[@t='[title]dialogs.staffNumber.tooltip.maxPositiveChange']")
    AtlasWebElement changeNumbersPlus();

    @Name("Максимальное отрицательное изменение численности")
    @FindBy("//input[@t='[title]dialogs.staffNumber.tooltip.maxNegativeChange']")
    AtlasWebElement changeNumbersMinus();

    @Name("Рассчитать")
    @FindBy(".//button[@t='common.actions.calculate']")
    AtlasWebElement calculatePlannedStrength();

    @Name("Кнопка отменить или крестик(закрыть форму)")
    @FindBy(".//button[@t='common.actions.calculate']")
    AtlasWebElement buttonCloseForm();

    @Name("Строка ввода {type}")
    @FindBy(".//input[@id='{{ type }}']")
    AtlasWebElement percentInput(@Param("type") String type);
}
