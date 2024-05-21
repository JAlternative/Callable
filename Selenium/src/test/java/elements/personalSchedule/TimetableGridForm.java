package elements.personalSchedule;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;


public interface TimetableGridForm extends AtlasWebElement {

    @Name("Кнопки дат в формате месяц")
    @FindBy(".//div[contains(@class,'timetable__day au-target')]//div[@class=\"timetable__day-date au-target link\"]//*")
    ElementsCollection<AtlasWebElement> listOfSpans();

    @FindBy(".//div[contains(@class,'timetable__day au-target')]//div[@class='timetable__day-events']//strong[contains(text(),'Отгул')]/ancestor::*[4]/preceding-sibling::*//span")
    ElementsCollection<AtlasWebElement> listOfDayOffs();

    @FindBy(".//div[contains(@class,'timetable__day au-target')]//div[@class='timetable__day-events']//strong[contains(text(),'Больничный')]/ancestor::*[4]/preceding-sibling::*//span")
    ElementsCollection<AtlasWebElement> listOfDaySick();

    @FindBy(".//div[contains(@class,'timetable__day au-target')]//div[@class='timetable__day-events']//span[contains(text(),'Частичное отсутствие')]/ancestor::*[4]/preceding-sibling::*//span")
    ElementsCollection<AtlasWebElement> listOfPartialAbsence();

    @FindBy(".//div[contains(@class,'timetable__day au-target')]//div[@class='timetable__day-events']//strong[contains(text(),'Смена')]/ancestor::*[4]/preceding-sibling::*//span")
    ElementsCollection<AtlasWebElement> listOfDayShift();

    @FindBy(".//*[contains(@class,\"timetable__day au-target\")]//span[contains(text(),'01')]/ancestor::*[3]")
    AtlasWebElement verificationElement();

    @FindBy(".//*[@class=\"timetable__day au-target\"]//span[@t.bind='name']/ancestor::*[5] | //strong[@t.bind= 'name']/ancestor::*[5]")
    ElementsCollection<AtlasWebElement> tileList();

    @Name("Список из дней на которые можно кликнуть")
    @FindBy("//div[@class='timetable__day au-target timetable__interactive']")
    ElementsCollection<AtlasWebElement> listOfDays();

    @Name("Список из часов")
    @FindBy("//div[@show.bind='period === periods.WEEK']/div")
    ElementsCollection<AtlasWebElement> listOfHours();

    @Name("Ячейки смен в будущих днях")
    @FindBy(".//*[@class=\"timetable__day au-target timetable__interactive\"]")
    ElementsCollection<AtlasWebElement> certainDays();

    @Name("Ячейка таблицы в будущем за {day} день месяца")
    @FindBy(".//span[contains(@class.bind, 'day.date')][contains(text(),'{{ day }}')]/../../../self::*[contains(@class,'timetable__interactive')]")
    AtlasWebElement requestCellByDay(@Param("day") String day);

    @Name("Кнопка предпочтения за {day} день месяца кроме смен")
    @FindBy(".//span[contains(@class.bind, 'day.date')][text() = '{{ day }}']/../..//strong[@au-target-id]")
    ElementsCollection<AtlasWebElement> preferenceButtonByDay(@Param("day") String day);


    @Name("Кнопка предпочтения за {day} день месяца кроме смен")
    @FindBy(".//span[contains(@class.bind, 'day.date')][text() = '{{ day }}']/../..//span[@t.bind]/../..")
    ElementsCollection<AtlasWebElement> preferenceShiftButtonByDay(@Param("day") String day);
}
