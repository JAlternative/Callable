package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface PublishCalendar extends AtlasWebElement {

    @Name("Кнопка \"ОК\"")
    @FindBy(".//button[@click.trigger = 'ok(selectedMonthStart)']")
    AtlasWebElement buttonOKM();

    @Name("Выбор месяца : \"{month}\" ")
    @FindBy("//div[contains(@class,' datetimepicker--date au-target datetimepicker--open')]//" +
            "div[@class='datetimepicker__main']//div//div[@class='datepicker__days au-target']/div[{{ month }}]/span")
    AtlasWebElement variantsOfMonth(@Param("month") String month);

}
