package testutils;

import elements.personalSchedule.Header;
import elements.personalSchedule.TimetableGridForm;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import utils.Params;
import wfm.components.schedule.ScheduleRequestType;
import wfm.models.DateInterval;

import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

import static utils.tools.CustomTools.systemSleep;

/**
 * Таблица персонального расписания
 */
public class PersonalTable {

    private final TimetableGridForm gridForm;
    private final Header header;

    public PersonalTable(TimetableGridForm gridForm, Header header) {
        this.gridForm = gridForm;
        this.header = header;
    }

    /**
     * Возвращает временной интервал от самой первой ячейки до последний. Иначе временной интервал в котором могут быть
     * размещены даты смен и запросов.
     */
    public DateInterval getTableDateInterval() {
        String title = header.headerTitle().getText();
        title = title.substring(title.indexOf(":") + 2).trim();
        String firstDate = title.substring(0, title.indexOf("–") - 1).trim();
        String lastDate = title.substring(title.indexOf("–") + 1).trim();

        String patternFormat = "dd MMMM";
        Locale localeRu = new Locale("ru", "RU");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(patternFormat).withLocale(localeRu);
        int currentYear = LocalDate.now().getYear();
        LocalDate startDate = MonthDay.parse(firstDate, formatter).atYear(currentYear);
        LocalDate endDate = MonthDay.parse(lastDate, formatter).atYear(currentYear);
        return new DateInterval(startDate, endDate);
    }

    /**
     * Возвращает временной интервал от завтрашнего дня, до последнего дня в таблице
     */
    public DateInterval getFutureInterval() {
        DateInterval tableDateInterval = getTableDateInterval();
        LocalDate dateTomorrow = LocalDate.now().plusDays(1);
        LocalDate endDate = tableDateInterval.getEndDate();
        if (dateTomorrow.getMonth().equals(Month.DECEMBER) && endDate.getMonth().equals(Month.JANUARY)) {
            endDate = endDate.plusYears(1);
        }
        return new DateInterval(dateTomorrow, endDate);
    }

    /**
     * Нажимает на преодпочтения в будущем
     *
     * @param localDate - дата на которую тыкнуть
     * @param driver    - драйвер
     */
    public void clickForPreferencesInFuture(LocalDate localDate, WebDriver driver) {
        systemSleep(5);// для полной прогрузки
        String day = localDate.format(DateTimeFormatter.ofPattern("dd"));
        new Actions(driver).moveToElement(gridForm.requestCellByDay(day)).click().build().perform();
    }

    /**
     * Возвращает элемент личного расписания из текущего месяца.
     * Учитывает возможность существования двух ячеек за, например, 26 число (из прошлого месяца).
     * Проверяет заголовок ячейки (если он есть) на соответствие заданному типу элемента.
     *
     * @param date    дата, в которой ищем элемент
     * @param isShift является ли элемент сменой
     */
    private AtlasWebElement getElement(LocalDate date, boolean isShift) {
        ElementsCollection<AtlasWebElement> elements;
        String day = date.format(DateTimeFormatter.ofPattern("dd"));
        if (date.getDayOfMonth() == 1) {
            day = day + " " + date.getMonth().getDisplayName(TextStyle.FULL, new Locale("ru"));
        }
        if (isShift) {
            elements = gridForm.preferenceShiftButtonByDay(day);
        } else {
            elements = gridForm.preferenceButtonByDay(day);
        }
        AtlasWebElement element;
        if (elements.isEmpty()) {
            throw new org.openqa.selenium.NoSuchElementException(String.format("Не найден элемент в ячейке за %s число", day));
        }
        if (date.getDayOfMonth() > 22 && elements.size() == 2) {
            element = elements.get(1);
        } else {
            element = elements.get(0);
        }
        return element;
    }

    /**
     * Узнает что за предпочтение сейчас расположено на ячейке в дате.
     *
     * @param localDate - дата ячейки
     * @return тип запроса
     */
    public ScheduleRequestType getPreferencesTypeByDay(LocalDate localDate) {
        String name;
        try {
            name = getElement(localDate, false).getText();
        } catch (org.openqa.selenium.NoSuchElementException | IndexOutOfBoundsException e) {
            try {
                name = getElement(localDate, true).getText();
            } catch (org.openqa.selenium.NoSuchElementException | IndexOutOfBoundsException ex) {
                return null;
            }
        }
        ScheduleRequestType[] requestTypes = ScheduleRequestType.values();
        String finalName = name;
        return Arrays.stream(requestTypes)
                .filter(type -> type.getName().contains(finalName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Нажимает на ячейку указанного типа запроса на заданный день
     */
    public void clickRequest(LocalDate date, ScheduleRequestType requestType) {
        boolean isShift = Arrays.asList(ScheduleRequestType.SHIFT, ScheduleRequestType.SHIFT_REQUEST, ScheduleRequestType.SHIFT_OTHER).contains(requestType);
        AtlasWebElement element = getElement(date, isShift);
        element.click();
    }

    /**
     * Возвращает текст элемента смены
     *
     * @param localDate дата смены
     */
    public Map<String, String> getText(LocalDate localDate) {
        String name = getElement(localDate, true).getText();
        String[] split = name.split("\n");
        Map<String, String> map = new HashMap<>();
        map.put(Params.TIME_INTERVAL, split[0].replace("– ", "").trim());
        map.put(Params.ORG_NAME, split[1].trim());
        return map;
    }
}
