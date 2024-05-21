package pagemodel;

import com.google.inject.Inject;
import guice.TestModule;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.exception.WaitUntilException;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.PersonalSchedulePage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.*;
import utils.Links;
import utils.Params;
import utils.db.DBUtils;
import utils.tools.LocalDateTools;
import wfm.PresetClass;
import wfm.components.orgstructure.MathParameterValues;
import wfm.components.schedule.*;
import wfm.components.schedule.ScheduleRequestType;
import wfm.components.utils.*;
import wfm.models.*;
import wfm.repository.EmployeePositionRepository;
import wfm.repository.OrgUnitRepository;
import wfm.repository.ScheduleRequestRepository;
import wfm.repository.ShiftRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static common.Groups.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.tools.CustomTools.*;
import static utils.tools.Format.UI_DOTS;
import static wfm.repository.CommonRepository.URL_BASE;

@Listeners({TestListener.class})
@Guice(modules = {TestModule.class})
public class PersonalSchedule extends BaseTest {

    //FIXME тут все хардкод, нужно удалять
    private static final int SHIFT_SINGLE_DAY = 2;
    private static final int SHIFT_DAILY_START = 4;
    private static final int SHIFT_EDIT_DAILY_START = 5;
    private static final int SHIFT_WEEKLY_START = 11;
    private static final int SHIFT_MONTHLY_DAY = 23;
    private static final int SHIFT_EDIT_START_HOUR = 4;
    private static final int SHIFT_EDIT_END_HOUR = 12;
    private static final int START_DAY = 18;
    private static final int END_DAY = 22;
    private static final int EDIT_DAY = 24;
    private static final int HOUR_START = 8;
    private static final int HOUR_END = 10;
    private static final int MIN_START = 0;
    private static final int MIN_END = 0;
    private static final int HOUR_EDIT = 12;

    private static final String RELEASE_URL = Links.getTestProperty("release");
    private static final Section SECTION = Section.PERSONAL_SCHEDULE_REQUESTS;
    private static final String URL_PS = RELEASE_URL + SECTION.getUrlEnding();

    private static final Logger LOG = LoggerFactory.getLogger(PersonalSchedule.class);

    @Inject
    private PersonalSchedulePage ps;

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriver() {
        closeDriver(ps.getWrappedDriver());
    }

    @AfterMethod(alwaysRun = true, description = "Очистить куки и подготовить новую вкладку")
    private void cleanCookies() {
        cleanCookiesAndOpenNewTab(ps.getWrappedDriver());
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void setUp() {
        setBrowserTimeout(ps.getWrappedDriver(), 15);
    }

    @Step("Перейти в раздел «Личное расписание».")
    private void goToPersonalSchedule() {
        new GoToPageSection(ps).getPage(SECTION, 60);
        ps.bodyElements().grayLoadingBackground()
                .waitUntil("Страница не загружается", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Перейти в раздел \"Личное расписание\" подразделения \"{orgUnit.name}\" под пользователем \"{user.username}\" с ролью \"{role.name}\"")
    private void goToScheduleAsUser(Role role, OrgUnit orgUnit, User user) {
        new RoleWithCookies(ps.getWrappedDriver(), role, orgUnit, user).getPage(SECTION);
        ps.bodyElements().grayLoadingBackground()
                .waitUntil("Страница не загружается", Matchers.not(DisplayedMatcher.displayed()), 120);
    }

    @Step("Нажать на случайную ячейку(день).")
    private void clickRandomDay() {
        ps.timetableGridForm()
                .waitUntil("Форма с выбором типа дня не отображена", DisplayedMatcher.displayed(), 5);
        int freeDaySize = ps.timetableGridForm().listOfDays().size();
        int x = ps.timetableGridForm().listOfDays().get(new Random().nextInt(freeDaySize - 1)).getLocation().x;
        int y = ps.timetableGridForm().listOfHours().get(2).getLocation().y;
        //TODO был какой то странный акшон который двигался к смене у которой было 00.00 во времени
        //        Actions actions = new Actions(ps.getWrappedDriver());
        //        actions.moveToElement(ps.timetableGridForm().zeroTime(), x, y).click().build().perform();
    }

    @Step("Кликнуть на кнопку \"Закрыть\"")
    public void clickCloseButton() {
        LOG.info("Кликаем на кнопку \"Закрыть\"");
        ps.editDayForm().closeFormButton().click();
        ps.editDayForm().waitUntil("Форма не закрылась", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Кликнуть на раскрывающийся список \"Тип\"")
    private void pressDayType() {
        ps.editDayForm().selectTypeButton().waitUntil("Кнопка не появилась", DisplayedMatcher.displayed(), 10);
        ps.editDayForm().selectTypeButton().click();
    }

    @Step("В раскрывшемся списке выбрать \"{requestType.name}\"")
    private void selectRequestType(ScheduleRequestType requestType) {
        LOG.info("Выбираем в списке: \"{}\"", requestType.getName());
        ps.editDayForm().typeButtons()
                .stream().filter(extendedWebElement -> extendedWebElement.getText().trim().equals(requestType.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("schedule message. Кнопки с выбранном типом \""
                                                              + requestType.getName() + "\" не было в списке"))
                .click();
    }

    @Step("В календаре \"{dateType}\" запроса выбрать дату {time}")
    private void pickRequestDateStartOrEnd(LocalDate date, DateTypeField dateType) {
        ps.editDayForm().buttonStartOrEndDateRequest(dateType.getName()).click();
        DatePicker datePicker = new DatePicker(ps.datePickDialog());
        datePicker.pickDate(date);
        datePicker.okButtonClick();
    }

    @Step("В поле \"{dateType}\" запроса выбрать время {time}")
    private void enterRequestTimeStartOrEnd(LocalTime time, DateTypeField dateType) {
        ps.editDayForm().inputStartOrEndTimeRequest(dateType.getName()).click();
        ps.editDayForm().inputStartOrEndTimeRequest(dateType.getName()).clear();
        ps.editDayForm().inputStartOrEndTimeRequest(dateType.getName())
                .sendKeys(time.format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    @Step("Выбрать {repeat} в типе периодичности")
    private void selectPeriodicityType(Periodicity repeat) {
        ps.editDayForm().selectEventRepeatButton()
                .waitUntil("Кнопка выбора периодичности не отобразилась", DisplayedMatcher.displayed(), 3);
        ps.editDayForm().selectEventRepeatButton().click();
        ps.editDayForm().eventRepeatButton(repeat.getRepeatType())
                .waitUntil("Кнопка с выбранным типом не отобразилась", DisplayedMatcher.displayed(), 3);
        ps.editDayForm().eventRepeatButton(repeat.getRepeatType()).click();
    }

    @Step("В поле \"Дата окончания повтора\" выбрать дату: {date}")
    private void sendDateEndRepeat(LocalDate date) {
        ps.editDayForm().dateRepeatEndField().waitUntil("Поле ввода даты окончания повтора не отобразилось",
                                                        DisplayedMatcher.displayed(), 5);
        ps.editDayForm().dateRepeatEndField().clear();
        ps.editDayForm().dateRepeatEndField().sendKeys(date.format(UI_DOTS.getFormat()));
    }

    @Step("Изменить тип периодичности повтора")
    private void changeRepeat() {
        try {
            selectPeriodicityType(Periodicity.NON_REPEAT);
        } catch (Exception e) {
            selectPeriodicityType(Periodicity.DAILY);
            sendDateEndRepeat(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        }
    }

    @Step("Проверить что даты в поле, до и после действий с календарем и нажатием на кнопку \"Отменить\" совпадают")
    private void checkDateMatch(String dateBefore, String dateAfter) {
        Allure.addAttachment("Проверка",
                             "Дата до выбора даты в календаре и нажатия на \"Отменить\": "
                                     + dateBefore + " , дата после: " + dateAfter);
        Assert.assertEquals(dateBefore, dateAfter, "Даты не совпали");
    }

    @Step("Проверить что время в поле, до и после действий с календарем и нажатием на кнопку \"Отменить\" совпадают")
    private void checkTimeMatch(String timeBefore, String timeAfter) {
        Allure.addAttachment("Проверка",
                             "Значение времени до выбора времени в календаре и нажатия на \"Отменить\": "
                                     + timeBefore + " , Значение времени после: " + timeAfter);
        Assert.assertEquals(timeBefore, timeAfter, "значения времён не совпали");
    }

    @Step("Нажать на кнопку \"Создать\"")
    private void pressCreateButton() {
        AtlasWebElement createButton = ps.editDayForm().buttonCreateShift();
        createButton.click();
        ps.bodyElements().grayLoadingBackground()
                .waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Кликнуть на раскрывающийся список \"Тип\"")
    private void pressOnRequestTypeChevron() {
        ps.editDayForm().selectTypeButton().waitUntil("Кнопка не появилась", DisplayedMatcher.displayed(), 10);
        ps.editDayForm().selectTypeButton().click();
    }

    @Step("Кликнуть на кнопку \"Троеточие\"")
    private void requestThreeDotsClick() {
        LOG.info("Кликаем на кнопку \"Троеточие\"");
        ps.editDayForm().waitUntil("Форма не открылась", DisplayedMatcher.displayed(), 5);
        ps.editDayForm().buttonDotsMenu().click();
    }

    @Step("В раскрывшемся меню троеточия запроса выбрать: {action.action}")
    private void selectActionInTripleDotRequest(RequestAction action) {
        LOG.info("В раскрывшемся меню троеточия запроса выбрать: {}", action);
        ps.editDayForm().typeButtons(action.getAction()).waitUntil
                ("Варианты действий со сменами не прогрузились", DisplayedMatcher.displayed(), 10);
        ps.editDayForm().typeButtons(action.getAction()).click();
        ps.editDayForm().waitUntil("Форма редактирования не закрылась", Matchers.not(DisplayedMatcher.displayed()), 5);
        ps.bodyElements().grayLoadingBackground()
                .waitUntil("Изменения не отразились на UI", Matchers.not(DisplayedMatcher.displayed()), 120);
    }

    @Step("Нажать кнопку \"Отмена\" в нижнем диалоговом окне.")
    private void cancelButtonClick() {
        ps.bottomDialog()
                .should("Диалоговое окно не отобразилсь", DisplayedMatcher.displayed(), 20);
        ps.bottomDialog().buttonAction().click();
    }

    @Step("Нажать на существующее предпочтение за {day} число месяца")
    private void existingPreferenceClick(int day) {
        ps.leftPanel().selectDate().waitUntil("Страница не загрузилась", DisplayedMatcher.displayed(), 30);
        ps.timetableGridForm().tileList()
                .filter(tileList -> tileList.getText().contains(addLeadZero(day))).get(0).click();
    }

    @Step("Проверка отмены создания {requestType.name}")
    private void cancelCreationRequestTypeAssert(int day, ScheduleRequestType requestType) {
        ps.bottomDialog().buttonAction()
                .should("Отобразилось сообщние об ошибке", not(DisplayedMatcher.displayed()), 40);
        ps.timetableGridForm().listOfSpans()
                .filter(form -> form.getText().contains(addLeadZero(day)))
                .get(0)
                .should(DisplayedMatcher.displayed());
        ps.timetableGridForm().listOfSpans()
                .filter(form -> form.getText().contains(addLeadZero(day)))
                .get(0)
                .click();
        ps.informForm().eventInformation(requestType.getName())
                .should("Создание " + requestType.getName() + " отменилось", not(DisplayedMatcher.displayed()), 10);
        ps.informForm().closeButton().click();
    }

    @Step("Проверка создания {requestType.name} за {day} число месяца")
    private void creationRequestTypeAssert(int day, ScheduleRequestType requestType) {
        ps.bottomDialog().buttonAction()
                .should("Отобразилось сообщние об ошибке", not(DisplayedMatcher.displayed()), 40);
        ElementsCollection<AtlasWebElement> element;
        switch (requestType) {
            case SHIFT:
                element = ps.timetableGridForm().listOfDayShift();
                break;
            case SICK_LEAVE:
                element = ps.timetableGridForm().listOfDaySick();
                break;
            case DAY_OFF:
                element = ps.timetableGridForm().listOfDayOffs();
                break;
            case PARTIAL_ABSENCE:
                element = ps.timetableGridForm().listOfPartialAbsence();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestType);
        }
        try {
            element.filter(form -> form.getText().contains(addLeadZero(day)))
                    .get(0)
                    .should(requestType.getName() + " не найден", DisplayedMatcher.displayed(), 30);
        } catch (java.lang.IndexOutOfBoundsException ex) {
            ps.timetableGridForm().listOfSpans()
                    .filter(form -> form.getText().contains(addLeadZero(day)))
                    .get(0)
                    .click();
            ps.informForm().eventInformation(requestType.getName())
                    .should(requestType.getName() + " не найден", DisplayedMatcher.displayed(), 10);
            ps.informForm().closeButton().click();
        }
    }

    @Step("Проверка удаления {requestType.name} за {day} число месяца")
    private void deleteRequestTypeAssert(int day, ScheduleRequestType requestType) {
        ps.bottomDialog().buttonAction()
                .should("Отобразилось сообщние об ошибке", not(DisplayedMatcher.displayed()), 40);
        ElementsCollection<AtlasWebElement> element;
        switch (requestType) {
            case DAY_OFF:
                element = ps.timetableGridForm().listOfDayOffs();
                break;
            case PARTIAL_ABSENCE:
                element = ps.timetableGridForm().listOfPartialAbsence();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestType);
        }
        try {
            element
                    .filter(form -> form.getText().contains(addLeadZero(day)))
                    .get(0)
                    .should(requestType.getName() + " не был удален", not(DisplayedMatcher.displayed()), 30);
        } catch (java.lang.IndexOutOfBoundsException ex) {
            ps.timetableGridForm().listOfSpans()
                    .filter(form -> form.getText().contains(addLeadZero(day)))
                    .get(0)
                    .click();
            ps.informForm().eventInformation(requestType.getName())
                    .should(requestType.getName() + " не был удален", not(DisplayedMatcher.displayed()), 10);
            ps.informForm().closeButton().click();
        }
    }

    @Step("Проверка того что смена не существует за {day} день месяца")
    private void notExistingShiftAssert(int day) {
        ps.bottomDialog().buttonAction()
                .should("Отобразилось сообщние об ошибке", not(DisplayedMatcher.displayed()), 40);
        try {
            ps.timetableGridForm().listOfDayShift()
                    .filter(form -> form.getText().contains(addLeadZero(day)))
                    .get(0)
                    .should("Смена осталась на месте", not(DisplayedMatcher.displayed()), 30);
        } catch (java.lang.IndexOutOfBoundsException ignored) {
        }
    }

    @Step("Редактировение индивидуального предпочтения")
    private void indEditSelection() {
        //TODO не понятно что делает степ
    }

    @Step("Редактирование серии предпочтений")
    private void serEditSelection() {
        //TODO не понятно что делает степ
    }

    @Step("Проверка отмены редактирования частичного отсутствия")
    private void cancelEditPartialAbsenceAssert(int day, int startTime, int endTime) {
        ps.bottomDialog().buttonAction()
                .should("Отобразилось сообщние об ошибке", not(DisplayedMatcher.displayed()), 40);
        ps.timetableGridForm().tileList()
                .filter(tileList -> tileList.getText().contains(addLeadZero(day)))
                .get(0)
                .click();
        //TODO Не понятно что тут была за проверка и почему там был клик
    }

    @Step("Проврка редактирования частичного отсутствия")
    private void editPartialAbsenceAssert(int day, int startTime, int endTime) {
        ps.bottomDialog().buttonAction()
                .should("Отобразилось сообщние об ошибке", not(DisplayedMatcher.displayed()), 40);
        ps.timetableGridForm().tileList()
                .filter(tileList -> tileList.getText().contains(addLeadZero(day)))
                .get(0)
                .click();
        //TODO Не понятно что тут была за проверка и почему там был клик
    }

    @Step("Нажать ячейку смены в будущем")
    private void dayCreationClick() {
        ps.leftPanel().selectDate().waitUntil("Страница не загрузилась", DisplayedMatcher.displayed(), 30);
        ps.timetableGridForm().verificationElement().waitUntil(DisplayedMatcher.displayed());
        ps.timetableGridForm().certainDays().get(0).click();
    }

    @Step("Click edit preference")
    private void clickEditPreference() {
        try {
            ps.editDayForm().selectEventRepeatButton().isDisplayed();
            selectActionInTripleDotRequest(RequestAction.EDIT);
        } catch (Exception e) {
            selectActionInTripleDotRequest(RequestAction.EDIT);
            indEditSelection();
        }
    }

    @Step("Click delete preference")
    private void clickDeletePreference() {
        try {
            ps.editDayForm().dateRepeatEndField().isDisplayed();
            selectActionInTripleDotRequest(RequestAction.DELETE);
            indEditSelection();
        } catch (Exception e) {
            selectActionInTripleDotRequest(RequestAction.DELETE);
        }
    }

    @Step("Assert partial absence edition")
    private void editShiftDayAssert(int day, int startTime, int endTime) {
        ps.bottomDialog().buttonAction()
                .should("Отобразилось сообщние об ошибке", not(DisplayedMatcher.displayed()), 40);
        //TODO не понятно что тут происходило
    }

    private String addLeadZero(int day) {
        String strDay = Integer.toString(day);
        return strDay.length() == 1 ? "0".concat(strDay) : strDay;
    }

    @Step("Проверка того, что предпочтение {requestType.name} за {localDate} не было создано. Появляется сообщение об отсутствии разрешения")
    private void assertAccessError(Employee employee, LocalDate localDate, ScheduleRequestType requestType,
                                   List<ScheduleRequest> requestsBefore) {
        ps.middleDialog().should("Сообщение об отсутствии разрешения не отобразилось",
                                 text(containsString("Отсутствует разрешение")), 5);
        SoftAssert softAssert = new SoftAssert();
        ScheduleRequestType requestTypeUI = newTable().getPreferencesTypeByDay(localDate);
        softAssert.assertNotEquals(requestType, requestTypeUI, "На UI не нашли элемент предпочтения за: " + localDate);
        List<ScheduleRequest> requestsAfter = ScheduleRequestRepository
                .getEmployeeSelfScheduleRequests(employee.getId(), newTable().getFutureInterval());
        softAssert.assertEquals(requestsAfter, requestsBefore, "Предпочтения изменились");
        softAssert.assertAll();
    }

    @Step("Проверка появления предпочтения {requestType.name} за {localDate}")
    private void assertCreatePreference(Employee employee, LocalDate localDate, ScheduleRequestType requestType,
                                        List<ScheduleRequest> requestsBefore) {
        SoftAssert softAssert = new SoftAssert();
        ScheduleRequestType requestTypeUI = newTable().getPreferencesTypeByDay(localDate);
        softAssert.assertEquals(requestTypeUI, requestType, "На UI не нашли элемент предпочтения за: " + localDate);
        List<ScheduleRequest> requestsAfter = ScheduleRequestRepository
                .getEmployeeSelfScheduleRequests(employee.getId(), newTable().getFutureInterval());
        requestsAfter.removeAll(requestsBefore);
        ScheduleRequest request = requestsAfter.iterator().next();
        softAssert.assertEquals(requestsAfter.size(), 1, "Предпочтение не добавилось");
        softAssert.assertEquals(request.getTitle(), requestType.getName(), "Имя предпочтения не совпадает с добавленным");
        softAssert.assertEquals(request.getDateTimeInterval().toDateInterval(), new DateInterval(localDate, localDate),
                                "Временной интервал предпочтения не совпадает с введенным");
        softAssert.assertAll();
    }

    @Step("Проверить, что свободная смена за {localDate} была назначена сотруднику {employee}")
    private void assertCreateShift(Employee employee, LocalDate localDate, List<Shift> shiftsBefore) {
        SoftAssert softAssert = new SoftAssert();
        Map<String, String> text = newTable().getText(localDate);
        List<Shift> requestsAfter = ShiftRepository.getEmployeeSelfShifts(employee.getId(), new DateInterval());
        requestsAfter.removeAll(shiftsBefore);
        softAssert.assertEquals(requestsAfter.size(), 1, "Добавилось больше одной смены");
        Shift shift = requestsAfter.iterator().next();
        softAssert.assertEquals(shift.getDateTimeInterval().toTimeInterval().toString(), text.get(Params.TIME_INTERVAL), "Время смены не совпало");
        softAssert.assertEquals(EmployeePositionRepository.getEmployeePositionById(shift.getEmployeePositionId()).getOrgUnit().getName(), text.get(Params.ORG_NAME), "Подразделение не совпало");
        softAssert.assertAll();
    }

    @Step("Кликнуть на запрос типа \"{type.name}\" за {date}")
    private void clickRequest(LocalDate date, ScheduleRequestType type) {
        LOG.info("Кликаем на запрос типа \"{}\" за {}", type.getName(), date);
        newTable().clickRequest(date, type);
    }

    @Step("Нажать на \"Взять\" для свободной смены с {shift.dateTimeInterval.startDateTime.time} по {shift.dateTimeInterval.endDateTime.time} " +
            "в подразделении {unit.name}")
    private void takeFreeShift(OrgUnit unit, Shift shift) {
        LocalTime start = shift.getDateTimeInterval().getStartDateTime().toLocalTime();
        LocalTime end = shift.getDateTimeInterval().getEndDateTime().toLocalTime();
        LOG.info("Нажимаем \"Взять\" для смены с {} по {} в подразделении {}",
                 start, end, unit.getName());
        int order = getFreeShiftOrderNumber(unit, start, end);
        ps.freeShiftDialog().takeFreeShiftButton(order).click();
        ps.freeShiftDialog().should(Matchers.not(DisplayedMatcher.displayed()));
        Assert.assertThrows(org.openqa.selenium.WebDriverException.class, () -> ps.bottomDialog().click());
        ps.bodyElements().grayLoadingBackground()
                .waitUntil("Страница не загружается", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    /**
     * Возвращает порядковый номер свободной смены по указанным критериям
     *
     * @param unit  подразделение, где есть свободная смена
     * @param start время начала смены
     * @param end   время окончания смены
     */
    private int getFreeShiftOrderNumber(OrgUnit unit, LocalTime start, LocalTime end) {
        ElementsCollection<AtlasWebElement> freeShifts = ps.freeShiftDialog().freeShifts();
        int size = freeShifts.size();
        int order = 0;
        for (int i = 0; i < size; i++) {
            boolean unitMatches = ps.freeShiftDialog().freeShiftField(i, "Подразделение").getAttribute(Params.VALUE).equals(unit.getName());
            boolean startMatches = ps.freeShiftDialog().freeShiftField(i, TimeTypeField.START_TIME.getName()).getAttribute(Params.VALUE).equals(start.toString());
            boolean endMatches = ps.freeShiftDialog().freeShiftField(i, TimeTypeField.END_TIME.getName()).getAttribute(Params.VALUE).equals(end.toString());
            if (unitMatches && startMatches && endMatches) {
                order = i;
                break;
            }
        }
        Allure.addAttachment("Поиск порядкового номера свободной смены на UI", String.valueOf(order));
        return order;
    }

    @Step("Проверить, что смена была передана на биржу")
    private void assertShiftMovedToExchange(int omId, Shift changedShift, List<Shift> freeShiftsBefore) {
        SoftAssert softAssert = new SoftAssert();
        LocalDate shiftDate = changedShift.getStartDate();
        softAssert.assertEquals(newTable().getPreferencesTypeByDay(shiftDate), ScheduleRequestType.FREE_SHIFT,
                                String.format("Элемент в ячейке за %s не принадлежит типу %s", shiftDate, ScheduleRequestType.FREE_SHIFT));
        softAssert.assertTrue(ShiftRepository.getFreeShifts(omId, shiftDate).size() - 1 == freeShiftsBefore.size(),
                              "Свободные смены не были добавлены или было добавлено несколько свободных смен");
        changedShift = changedShift.refreshShift();
        softAssert.assertTrue(changedShift.getEmployeePositionId() == 0 || changedShift.getEmployeePositionId() == null,
                              "Смена осталась закреплена за работником");
        softAssert.assertAll();
        Allure.addAttachment("Проверка", "Смена была передана на биржу. На бирже появилась ровно одна новая смена.");
    }

    @Step("Нажать на ячейку без предпочтений от: {localDate}")
    private void clickOnFuturePreference(LocalDate localDate) {
        LOG.info("Выбрана дата: {}", localDate.toString());
        newTable().clickForPreferencesInFuture(localDate, ps.getWrappedDriver());
        ps.editDayForm().waitUntil("Форма редактирования не открылась", DisplayedMatcher.displayed(), 5);
    }

    @Step("Смена масштаба расписания на {scope.scopeName}")
    private void switchScope(ScopeType scope) {
        ps.header().scopeButton(scope.getScopeName()).click();
        ps.bodyElements().grayLoadingBackground()
                .waitUntil("Страница не загружается", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    public List<LocalDate> getFreeDatesInFuture(Employee employee) {
        DateInterval futureInterval = newTable().getFutureInterval();
        List<ScheduleRequest> requests = ScheduleRequestRepository.getEmployeeSelfScheduleRequests(employee.getId(), futureInterval);
        List<LocalDate> busyDates = new ArrayList<>();
        requests.stream().map(ScheduleRequest::getDateTimeInterval).map(dateTimeInterval -> dateTimeInterval.toDateInterval().getBetweenDatesList())
                .forEach(busyDates::addAll);
        List<LocalDate> futureList = futureInterval.getBetweenDatesList();
        futureList.removeAll(busyDates);
        return futureList;
    }

    /**
     * Инициализирует новый экземпляр таблицы
     */
    private PersonalTable newTable() {
        return new PersonalTable(ps.timetableGridForm(), ps.header());
    }

    @Step("Проверить, что переход не выполнился, отображается страница без заголовка или происходит переход на \"Расписание\"")
    private void assertModuleNotOpen() {
        systemSleep(5); //Прогружает, нечем заменить sleep
        String url = ps.getWrappedDriver().getCurrentUrl();
        Assert.assertFalse(url.contains(Section.PERSONAL_SCHEDULE_REQUESTS.getUrlEnding()), "Есть доступ к разделу");
    }

    @Test(groups = {"TO-1"})
    public void createPreference() {
        goToPersonalSchedule();
        clickRandomDay();
        pressDayType();
        selectRequestType(ScheduleRequestType.VACATION);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        pressCreateButton();
    }

    @Test(groups = {"TO-2"})
    public void notCreatePreference() {
        goToPersonalSchedule();
        clickRandomDay();
        pressDayType();
        selectRequestType(ScheduleRequestType.VACATION);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        pressCreateButton();
        cancelButtonClick();
    }

    @Test(groups = {"TO-3"})
    public void changePrefStartDate() {
        goToPersonalSchedule();
        clickRandomDay();
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(2018, LocalDateTools.RANDOM, 17), DateTypeField.START_DATE);
        pressOnRequestTypeChevron();
    }

    @Test(groups = {"TO-4"})
    public void notChangePrefStartDate() {
        goToPersonalSchedule();
        clickRandomDay();
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(2018, LocalDateTools.RANDOM, 3), DateTypeField.START_DATE);
        pressOnRequestTypeChevron();
        cancelButtonClick();
    }

    @Test(groups = {"TO-5"})
    public void changePrefEndDate() {
        goToPersonalSchedule();
        clickRandomDay();
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pressOnRequestTypeChevron();
        cancelButtonClick();
    }

    @Test(groups = {"TO-6"})
    public void notChangePrefEndDate() {
        goToPersonalSchedule();
        clickRandomDay();
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pressOnRequestTypeChevron();
        cancelButtonClick();
    }

    @Test(groups = {"TO-7"})
    public void deletePreference() {
        goToPersonalSchedule();
        clickRandomDay();
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
    }

    @Test(groups = {"TO-8"})
    public void notDeletePreference() {
        goToPersonalSchedule();
        clickRandomDay();
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
        cancelButtonClick();
    }

    @Test(groups = {"TOG-1"})
    public void aCreateDayOff() {
        goToPersonalSchedule();
        dayCreationClick();
        pressDayType();
        selectRequestType(ScheduleRequestType.DAY_OFF);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        pressCreateButton();
        cancelButtonClick();
        cancelCreationRequestTypeAssert(START_DAY, ScheduleRequestType.DAY_OFF);
        dayCreationClick();
        pressDayType();
        selectRequestType(ScheduleRequestType.DAY_OFF);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        pressCreateButton();
        creationRequestTypeAssert(START_DAY, ScheduleRequestType.DAY_OFF);
    }

    @Test(groups = {"TOG-2"}, dependsOnGroups = {"TOG-1"})
    public void bEditDayOff() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
        cancelButtonClick();
        cancelCreationRequestTypeAssert(EDIT_DAY, ScheduleRequestType.DAY_OFF);
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
        creationRequestTypeAssert(EDIT_DAY, ScheduleRequestType.DAY_OFF);
    }

    @Test(groups = {"TOG-3"}, dependsOnGroups = {"TOG-1"})
    public void cDeleteTest() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
        cancelButtonClick();
        creationRequestTypeAssert(START_DAY, ScheduleRequestType.DAY_OFF);
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
        deleteRequestTypeAssert(START_DAY, ScheduleRequestType.DAY_OFF);
    }

    @Test(groups = {"TS-1.1"}, dependsOnGroups = {"TS-1.6"})
    public void createSingleShift() {
        goToPersonalSchedule();
        dayCreationClick();
        selectRequestType(ScheduleRequestType.SHIFT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        selectPeriodicityType(Periodicity.NON_REPEAT);
        pressCreateButton();
        creationRequestTypeAssert(SHIFT_SINGLE_DAY, ScheduleRequestType.SHIFT);
    }

    @Test(groups = {"TS-1.2"})
    public void createDailyShift() {
        goToPersonalSchedule();
        dayCreationClick();
        selectRequestType(ScheduleRequestType.SHIFT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        selectPeriodicityType(Periodicity.DAILY);
        sendDateEndRepeat(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        pressCreateButton();
        creationRequestTypeAssert(SHIFT_DAILY_START, ScheduleRequestType.SHIFT);
    }

    @Test(groups = {"TS-1.3"})
    public void createWeeklyShift() {
        goToPersonalSchedule();
        dayCreationClick();
        selectRequestType(ScheduleRequestType.SHIFT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        selectPeriodicityType(Periodicity.WEEKLY);
        sendDateEndRepeat(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        pressCreateButton();
        creationRequestTypeAssert(SHIFT_WEEKLY_START, ScheduleRequestType.SHIFT);
    }

    @Test(groups = {"TS-1.4"})
    public void createMonthlyShift() {
        goToPersonalSchedule();
        dayCreationClick();
        selectRequestType(ScheduleRequestType.SHIFT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        selectPeriodicityType(Periodicity.MONTHLY);
        sendDateEndRepeat(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        pressCreateButton();
        creationRequestTypeAssert(SHIFT_MONTHLY_DAY, ScheduleRequestType.SHIFT);
    }

    @Test(groups = {"TS-1.6"})
    public void cancelCreateDayShift() {
        goToPersonalSchedule();
        dayCreationClick();
        selectRequestType(ScheduleRequestType.SHIFT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        selectPeriodicityType(Periodicity.NON_REPEAT);
        pressCreateButton();
        cancelButtonClick();
        notExistingShiftAssert(SHIFT_MONTHLY_DAY);
    }

    @Test(groups = {"TS-2.1"}, dependsOnGroups = {"TS-1.2"})
    public void editShiftDay() {
        goToPersonalSchedule();
        //TODO так понимаю здесь должно было быть переключение на день
        existingPreferenceClick(SHIFT_DAILY_START);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(SHIFT_EDIT_START_HOUR, 0), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(SHIFT_EDIT_END_HOUR, 0), DateTypeField.END_DATE);
        changeRepeat();
        clickEditPreference();
        editShiftDayAssert(SHIFT_EDIT_DAILY_START, SHIFT_EDIT_START_HOUR, SHIFT_EDIT_END_HOUR);
    }

    @Test(groups = {"TS-2.2"}, dependsOnGroups = {"TS-1.3"})
    public void editShiftDayCancel() {
        goToPersonalSchedule();
        existingPreferenceClick(SHIFT_WEEKLY_START);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(SHIFT_EDIT_START_HOUR, 0), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(SHIFT_EDIT_END_HOUR, 0), DateTypeField.END_DATE);
        changeRepeat();
        clickEditPreference();
        cancelButtonClick();
        editShiftDayAssert(SHIFT_WEEKLY_START, 0, 8);
    }

    @Test(groups = {"TS-3.1"}, dependsOnGroups = {"TS-1.1"})
    public void deleteShiftDay() {
        goToPersonalSchedule();
        existingPreferenceClick(SHIFT_SINGLE_DAY);
        requestThreeDotsClick();
        clickDeletePreference();
        notExistingShiftAssert(SHIFT_SINGLE_DAY);
    }

    @Test(groups = {"TS-3.2"}, dependsOnGroups = {"TS-1.4"})
    public void deleteShiftDayCancel() {
        goToPersonalSchedule();
        existingPreferenceClick(SHIFT_MONTHLY_DAY);
        requestThreeDotsClick();
        clickDeletePreference();
        cancelButtonClick();
        notExistingShiftAssert(SHIFT_MONTHLY_DAY);
    }

    @Test(groups = {"TChO-1"})
    public void indPartialAbsenceCreation() {
        goToPersonalSchedule();
        dayCreationClick();
        pressDayType();
        selectRequestType(ScheduleRequestType.PARTIAL_ABSENCE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_START, MIN_START), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_END, MIN_END), DateTypeField.END_DATE);
        pressCreateButton();
        cancelButtonClick();
        cancelCreationRequestTypeAssert(START_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
        dayCreationClick();
        pressDayType();
        selectRequestType(ScheduleRequestType.PARTIAL_ABSENCE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_START, MIN_START), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_END, MIN_END), DateTypeField.END_DATE);
        pressCreateButton();
        creationRequestTypeAssert(START_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
    }

    @Test(groups = {"TChO-2"}, dependsOnGroups = {"TChO-1"})
    public void indPartialAbsenceEdit() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_START, MIN_START), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_EDIT, MIN_END), DateTypeField.END_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
        cancelButtonClick();
        cancelEditPartialAbsenceAssert(START_DAY, HOUR_START, HOUR_END);
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_START, MIN_START), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_EDIT, MIN_END), DateTypeField.END_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
        editPartialAbsenceAssert(START_DAY, HOUR_START, HOUR_EDIT);
    }

    @Test(groups = {"TChO-3"}, dependsOnGroups = {"TChO-1"})
    public void indPartialAbsenceDelete() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
        cancelButtonClick();
        creationRequestTypeAssert(START_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
        deleteRequestTypeAssert(START_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
    }

    @Test(groups = {"TChO-4"})
    public void serPartialAbsenceCreation() {
        goToPersonalSchedule();
        dayCreationClick();
        pressDayType();
        selectRequestType(ScheduleRequestType.PARTIAL_ABSENCE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_START, MIN_START), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_END, MIN_END), DateTypeField.END_DATE);
        selectPeriodicityType(Periodicity.DAILY);
        sendDateEndRepeat(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        pressCreateButton();
        cancelButtonClick();
        cancelCreationRequestTypeAssert(START_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
        cancelCreationRequestTypeAssert(END_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
        dayCreationClick();
        pressDayType();
        selectRequestType(ScheduleRequestType.PARTIAL_ABSENCE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_START, MIN_START), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_END, MIN_END), DateTypeField.END_DATE);
        selectPeriodicityType(Periodicity.DAILY);
        sendDateEndRepeat(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        pressCreateButton();
        creationRequestTypeAssert(START_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
        creationRequestTypeAssert(END_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
    }

    @Test(groups = {"TChO-5"}, dependsOnGroups = {"TChO-4"})
    public void serPartialAbsenceEndRepeatDateEdit() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        sendDateEndRepeat(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        selectActionInTripleDotRequest(RequestAction.EDIT);
        serEditSelection();
        cancelButtonClick();
        cancelCreationRequestTypeAssert(EDIT_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        sendDateEndRepeat(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        selectActionInTripleDotRequest(RequestAction.EDIT);
        serEditSelection();
        creationRequestTypeAssert(EDIT_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
    }

    @Test(groups = {"TChO-6"}, dependsOnGroups = {"TChO-4"})
    public void indInSerPartialAbsenceEdition() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_START, MIN_START), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_EDIT, MIN_END), DateTypeField.END_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
        indEditSelection();
        cancelButtonClick();
        cancelEditPartialAbsenceAssert(START_DAY, HOUR_START, HOUR_END);
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_START, MIN_START), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_EDIT, MIN_END), DateTypeField.END_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
        indEditSelection();
        editPartialAbsenceAssert(START_DAY, HOUR_START, HOUR_EDIT);
    }

    @Test(groups = {"TChO-7"}, dependsOnGroups = {"TChO-4"})
    public void indInSerPartialAbsenceDelete() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
        indEditSelection();
        cancelButtonClick();
        creationRequestTypeAssert(START_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
        indEditSelection();
        deleteRequestTypeAssert(START_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
    }

    @Test(groups = {"TChO-8"}, dependsOnGroups = {"TChO-4", "TChO-5"})
    public void serPartialAbsenceEdition() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY + 1);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_START, MIN_START), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_EDIT, MIN_END), DateTypeField.END_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
        serEditSelection();
        cancelButtonClick();
        cancelEditPartialAbsenceAssert(START_DAY + 1, HOUR_START, HOUR_END);
        cancelEditPartialAbsenceAssert(EDIT_DAY, HOUR_START, HOUR_END);
        existingPreferenceClick(START_DAY + 1);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_START, MIN_START), DateTypeField.START_DATE);
        enterRequestTimeStartOrEnd(LocalTime.of(HOUR_EDIT, MIN_END), DateTypeField.END_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
        serEditSelection();
        editPartialAbsenceAssert(START_DAY + 1, HOUR_START, HOUR_EDIT);
        editPartialAbsenceAssert(EDIT_DAY, HOUR_START, HOUR_EDIT);
    }

    @Test(groups = {"TChO-9"}, dependsOnGroups = {"TChO-4", "TChO-5"})
    public void serPartialAbsenceDelete() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY + 1);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
        serEditSelection();
        cancelButtonClick();
        creationRequestTypeAssert(START_DAY + 1, ScheduleRequestType.PARTIAL_ABSENCE);
        creationRequestTypeAssert(EDIT_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
        existingPreferenceClick(START_DAY + 1);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
        serEditSelection();
        deleteRequestTypeAssert(START_DAY + 1, ScheduleRequestType.PARTIAL_ABSENCE);
        deleteRequestTypeAssert(EDIT_DAY, ScheduleRequestType.PARTIAL_ABSENCE);
    }

    @Test(groups = {"TB-1.1"})
    public void cancelStartDateCreateSickDay() {
        goToPersonalSchedule();
        dayCreationClick();
        pressDayType();
        selectRequestType(ScheduleRequestType.SICK_LEAVE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pressCreateButton();
        cancelButtonClick();
    }

    @Test(groups = {"TB-1.2"})
    public void cancelEndDateCreateSickDay() {
        goToPersonalSchedule();
        dayCreationClick();
        pressDayType();
        selectRequestType(ScheduleRequestType.SICK_LEAVE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        pressCreateButton();
        cancelButtonClick();
    }

    @Test(groups = {"TB-1.3"}, dependsOnGroups = {"TB-1.1", "TB-1.2"})
    public void createSickDay() {
        goToPersonalSchedule();
        dayCreationClick();
        pressDayType();
        selectRequestType(ScheduleRequestType.SICK_LEAVE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        pressCreateButton();
        creationRequestTypeAssert(START_DAY, ScheduleRequestType.SICK_LEAVE);
    }

    @Test(groups = {"TB-2.1"}, dependsOnGroups = {"TB-1.1", "TB-1.2", "TB-1.3"})
    public void cancelEditStartDaySickDay() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
        cancelButtonClick();
    }

    @Test(groups = {"TB-2.2"}, dependsOnGroups = {"TB-1.1", "TB-1.2", "TB-1.3"})
    public void cancelEditEndDaySickDay() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
        cancelButtonClick();
    }

    @Test(groups = {"TB-2.3"}, dependsOnGroups = {"TB-2.1", "TB-2.2"})
    public void editStartDaySickDay() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
    }

    @Test(groups = {"TB-2.4"}, dependsOnGroups = {"TB-2.1", "TB-2.2", "TB-2.3"})
    public void editEndDaySickDay() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.EDIT);
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        selectActionInTripleDotRequest(RequestAction.EDIT);
    }

    @Test(groups = {"TB-3.1"}, dependsOnGroups = {"TB-2.4"})
    public void cancelDeleteSickDay() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
        cancelButtonClick();
    }

    @Test(groups = {"TB-3.2"}, dependsOnGroups = {"TB-3.1"})
    public void deleteSickDay() {
        goToPersonalSchedule();
        existingPreferenceClick(START_DAY);
        requestThreeDotsClick();
        selectActionInTripleDotRequest(RequestAction.DELETE);
        deleteRequestTypeAssert(START_DAY, ScheduleRequestType.DAY_OFF);
    }

    @Test(groups = {"TC-1.1.4"})
    public void deselectPreferenceDateStart() {
        goToPersonalSchedule();
        clickRandomDay();
        pressDayType();
        selectRequestType(ScheduleRequestType.SHIFT);
        String dateBefore = ps.editDayForm().dateStartOrEndInput(DateTypeField.START_DATE.getName()).getAttribute("value");
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        checkDateMatch(dateBefore, ps.editDayForm().dateStartOrEndInput(DateTypeField.START_DATE.getName()).getAttribute("value"));
    }

    @Test(groups = {"TC-1.1.5"})
    public void deselectPreferenceTimeStart() {
        goToPersonalSchedule();
        clickRandomDay();
        pressDayType();
        selectRequestType(ScheduleRequestType.SHIFT);
        String timeBefore = ps.editDayForm().inputStartOrEndTimeRequest(DateTypeField.START_DATE.getName()).getAttribute("value");
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.START_DATE);
        checkTimeMatch(timeBefore, ps.editDayForm().inputStartOrEndTimeRequest(DateTypeField.START_DATE.getName()).getAttribute("value"));
    }

    @Test(groups = {"TC-1.1.6"})
    public void deselectPreferenceDateEnd() {
        goToPersonalSchedule();
        clickRandomDay();
        pressDayType();
        selectRequestType(ScheduleRequestType.SHIFT);
        String dateBefore = ps.editDayForm().dateStartOrEndInput(DateTypeField.END_DATE.getName()).getAttribute("value");
        pickRequestDateStartOrEnd(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM), DateTypeField.END_DATE);
        checkDateMatch(dateBefore, ps.editDayForm().dateStartOrEndInput(DateTypeField.END_DATE.getName()).getAttribute("value"));
    }

    @Test(groups = {"TC-1.1.7"})
    public void deselectPreferenceTimeEnd() {
        goToPersonalSchedule();
        clickRandomDay();
        pressDayType();
        selectRequestType(ScheduleRequestType.SHIFT);
        String timeBefore = ps.editDayForm().inputStartOrEndTimeRequest(DateTypeField.END_DATE.getName()).getAttribute("value");
        enterRequestTimeStartOrEnd(LocalTime.now().withMinute(15), DateTypeField.END_DATE);
        checkTimeMatch(timeBefore, ps.editDayForm().inputStartOrEndTimeRequest(DateTypeField.END_DATE.getName()).getAttribute("value"));
    }

    @Test(groups = {"TC-1.1.8"})
    public void deselectPreferencePeriodicityDateEnd() {
        goToPersonalSchedule();
        clickRandomDay();
        pressDayType();
        selectRequestType(ScheduleRequestType.SHIFT);
        selectPeriodicityType(Periodicity.DAILY);
        String dateBefore = ps.editDayForm().dateRepeatEndField().getAttribute("value");
        sendDateEndRepeat(LocalDateTools.getDate(LocalDateTools.RANDOM, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        checkDateMatch(dateBefore, ps.editDayForm().dateRepeatEndField().getAttribute("value"));
    }

    @Test(groups = {"ТК2829", G1, PS2,
            "@Before don't show button to publish roster", "@Before allow editing plan shifts in future"},
            description = "Ввод предпочтения сотрудником с доступом")
    @Link(name = "2829_Права на создание неподтвержденного запроса в Личном расписании", url = "https://wiki.goodt.me/x/KQEUD")
    @TmsLink("60790")
    @Tag("ТК2829")
    @Tag(PS2)
    public void enteringEmployeePreferencesWithAccess() {
        Role role = PresetClass.createRoleWithPersonalSchedule(true);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        Employee employee = employeePosition.getEmployee();
        ScheduleRequestType type = ScheduleRequestType.getRandomAbsenceRequest();
        PresetClass.addScheduleRequestTypeRights(role, type);
        goToScheduleAsUser(role, unit, employee.getUser());
        switchScope(ScopeType.MONTH);
        List<ScheduleRequest> requests = ScheduleRequestRepository.getEmployeeSelfScheduleRequests(employee.getId(), newTable().getFutureInterval());
        List<LocalDate> dateList = getFreeDatesInFuture(employee);
        LocalDate localDate = getRandomFromList(dateList);
        clickOnFuturePreference(localDate);
        pressDayType();
        selectRequestType(type);
        pressCreateButton();
        assertCreatePreference(employee, localDate, type, requests);
    }

    @Test(groups = {"ТК2829", G1, PS2, "@Before allow editing plan shifts in future"}, description = "Ввод предпочтения сотрудником без доступа на создание")
    @Link(name = "2829_Права на создание неподтвержденного запроса в Личном расписании", url = "https://wiki.goodt.me/x/KQEUD")
    @TmsLink("60790")
    @Tag("ТК2829")
    @Tag(PS2)
    public void enteringEmployeePreferencesWithoutCreatingAccess() {
        Role role = PresetClass.createRoleWithPersonalSchedule(false);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        Employee employee = employeePosition.getEmployee();
        ScheduleRequestType type = ScheduleRequestType.getRandomAbsenceRequest();
        PresetClass.addScheduleRequestTypeRights(role, type);
        goToScheduleAsUser(role, unit, employee.getUser());
        switchScope(ScopeType.MONTH);
        List<ScheduleRequest> requests = ScheduleRequestRepository.getEmployeeSelfScheduleRequests(employee.getId(), newTable().getFutureInterval());
        List<LocalDate> dateList = getFreeDatesInFuture(employee);
        LocalDate localDate = getRandomFromList(dateList);
        clickOnFuturePreference(localDate);
        pressDayType();
        selectRequestType(type);
        pressCreateButton();
        assertAccessError(employee, localDate, type, requests);
    }

    @Test(groups = {"ТК2829", G1, PS2}, description = "Ввод предпочтения сотрудником без доступа к разделу")
    @Link(name = "2829_Права на создание неподтвержденного запроса в Личном расписании", url = "https://wiki.goodt.me/x/KQEUD")
    @TmsLink("60790")
    @Tag("ТК2829")
    @Tag(PS2)
    public void enteringEmployeePreferencesWithoutAccess() {
        Role role = PresetClass.createCustomPermissionRole(Collections.emptyList());
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        Employee employee = employeePosition.getEmployee();
        new RoleWithCookies(ps.getWrappedDriver(), role, employeePosition.getOrgUnit(), employee.getUser()).getPageWithoutWait(SECTION);
        assertModuleNotOpen();
    }

    @Test(groups = {"ABCHR2939-3", "TEST-1172"}, description = "Создание запроса типа \"Смена в другом подразделении\" в Личном расписании")
    public void createRequestOtherShifts() {
        Role role = PresetClass.createRoleWithPersonalSchedule(true);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWorkingWithUser();
        Employee employee = employeePosition.getEmployee();
        List<ScheduleRequest> requests = ScheduleRequestRepository.getEmployeeSelfScheduleRequests(employee.getId(), newTable().getFutureInterval());
        List<LocalDate> dateList = getFreeDatesInFuture(employee);
        LocalDate localDate = getRandomFromList(dateList);
        goToScheduleAsUser(role, employeePosition.getOrgUnit(), employee.getUser());
        switchScope(ScopeType.MONTH);
        clickOnFuturePreference(localDate);
        pressDayType();
        ScheduleRequestType requestType = ScheduleRequestType.SHIFT_OTHER;
        selectRequestType(requestType);
        pressCreateButton();
        assertCreatePreference(employee, localDate, requestType, requests);
    }

    @Test(groups = {"FS", G1, PS2,
            "@Before allow free shifts for own employees",
            "@Before disable roster single edited version",
            "@Before disable pre-publication checks"},
            description = "Назначение свободной смены в Личном расписании")
    @Link(name = "Статья: \"Свободные смены\"", url = "https://wiki.goodt.me/x/_QUtD")
    @Tag("FS-1")
    @Tag(PS2)
    @Severity(SeverityLevel.NORMAL)
    @Owner(SCHASTLIVAYA)
    @TmsLink("60205")
    public void assignFreeShift() {
        List<PermissionType> permissions = Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                         PermissionType.SCHEDULE_PERSONAL,
                                                         PermissionType.SHIFT_EXCHANGE_FOR_SELF);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.DEPERSONALISATION_MODE, "NONE", true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.addRandomTagToEmployeeAndOrgUnit(unit, ep.getEmployee());
        Shift shift = PresetClass.presetForMakeShift(ep, false, ShiftTimePosition.FUTURE);
        PresetClass.moveShiftToExchange(shift);
        PresetClass.checkAndMakePublicationRoster(omId);
        Employee employee = ep.getEmployee();
        List<Shift> shiftsBefore = ShiftRepository.getEmployeeSelfShifts(employee.getId(), new DateInterval());
        goToScheduleAsUser(PresetClass.createCustomPermissionRole(permissions), unit, employee.getUser());
        clickRequest(shift.getStartDate(), ScheduleRequestType.FREE_SHIFT);
        takeFreeShift(unit, shift);
        assertCreateShift(employee, shift.getStartDate(), shiftsBefore);
    }

    @Test(groups = {"FS-3", G2, PS2,
            "@Before move to exchange only shifts from exchange",
            "@Before disable pre-publication checks",
            "@Before publish without checking for yearly overtime limit violation"},
            description = "Передача смены на биржу в Личном расписании, если можно передавать только назначенную смену")
    @Link(name = "Статья: \"Свободные смены\"", url = "https://wiki.goodt.me/x/_QUtD")
    @Tag(PS2)
    @Tag("FS-3")
    @Severity(SeverityLevel.MINOR)
    @Owner(SCHASTLIVAYA)
    @TmsLink("60205")
    public void moveShiftToExchangeIfItIsAvailableOnlyForAssignedShifts() {
        checkLastDayOfMonth();
        List<PermissionType> permissions = Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_PERSONAL,
                PermissionType.SHIFT_EXCHANGE_FOR_SELF);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.addUser(ep.getEmployee());
        PresetClass.addRandomTagToEmployeeAndOrgUnit(unit, ep.getEmployee());
        ep = ep.refreshEmployeePosition();
        LocalDate shiftFromExchangeDate = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        LocalDate plannedShiftDate = getRandomFromList(ShiftTimePosition.FUTURE.getShiftsDateInterval().subtract(Arrays.asList(shiftFromExchangeDate)));
        PresetClass.makeClearDate(ep, shiftFromExchangeDate, plannedShiftDate);
        Shift shiftFromExchange = PresetClass.presetForMakeShiftDate(ep, shiftFromExchangeDate, false, ShiftTimePosition.FUTURE);
        DBUtils.makeShiftFromExchange(shiftFromExchange);
        PresetClass.presetForMakeShiftDate(ep, plannedShiftDate, false, ShiftTimePosition.FUTURE);
        PresetClass.checkAndMakePublicationRoster(unit.getId());
        goToScheduleAsUser(PresetClass.createCustomPermissionRole(permissions), unit, ep.getEmployee().getUser());
        clickRequest(plannedShiftDate, ScheduleRequestType.SHIFT_REQUEST);
        try {
            requestThreeDotsClick();
            Assert.assertThrows(WaitUntilException.class, () -> selectActionInTripleDotRequest(RequestAction.MOVE_TO_EXCHANGE));
        } catch (org.openqa.selenium.ElementNotInteractableException e) {
            Assert.assertThrows(org.openqa.selenium.ElementNotInteractableException.class, this::requestThreeDotsClick);
        }
        clickCloseButton();
        clickRequest(shiftFromExchangeDate, ScheduleRequestType.SHIFT_REQUEST);
        requestThreeDotsClick();
        List<Shift> freeShiftsBefore = ShiftRepository.getFreeShifts(omId, shiftFromExchangeDate);
        selectActionInTripleDotRequest(RequestAction.MOVE_TO_EXCHANGE);
        assertShiftMovedToExchange(omId, shiftFromExchange, freeShiftsBefore);
    }
}
