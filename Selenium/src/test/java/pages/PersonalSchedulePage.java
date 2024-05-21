package pages;

import elements.general.DatePickerForm;
import elements.personalSchedule.*;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import elements.scheduleBoard.SpinnerLoader;

public interface PersonalSchedulePage extends WebPage {

    @Name("Форма редактирования или создания запроса")
    @FindBy("//div[@show.bind='dialogs.request && !dialogs.summary']")
    EditDayForm editDayForm();

    @Name("Таблица с клетками расписания")
    @FindBy("//div[@class='timetable__grid']")
    TimetableGridForm timetableGridForm();

    @Name("Форма датапикера")
    @FindBy("//div[contains(@class, 'datetimepicker--open' )]")
    DatePickerForm datePickDialog();

    @Name("Панель слева от таблицы")
    @FindBy("//div[@class='timetable__left mdl-shadow--4dp']")
    LeftPanel leftPanel();

    @Name("Форма с информацией \"Сводка дня\"")
    @FindBy("//div[@class='mdl-layout__right mdl-shadow--4dp au-target']")
    InformationForm informForm();

    @Name("Всплывающий поп-ап с сообщением в нижней части экрана")
    @FindBy("//div[@ref='snack']")
    BottomDialog bottomDialog();

    @Name("Всплывающий поп-ап с сообщением по центру экрана")
    @FindBy("//div[@class='mdl-dialog__content']/div[@style]")
    BottomDialog middleDialog();

    @Name("Хедер страницы")
    @FindBy("//header")
    Header header();

    @Name("Панель переключения влево/вправо")
    @FindBy("//div[@class ='timetable__ctrl']")
    SwitchDirectionPanel switchDirectionPanel();

    @Name("Общие для страницы элементы")
    @FindBy("//body")
    SpinnerLoader bodyElements();

    @Name("Окно со списком свободных смен на выбранный день")
    @FindBy("//div[contains(@class, 'mdl-dialog')]//span[contains(text(), 'Свободные смены')]/../../..")
    FreeShiftDialog freeShiftDialog();

}
