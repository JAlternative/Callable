package testutils;

import elements.reports.MainHeader;
import elements.scheduleBoard.Layout;
import elements.scheduleBoard.TopBar;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.exception.WaitUntilException;
import org.hamcrest.Matchers;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import pages.ScheduleBoardPage;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import utils.Links;
import wfm.components.schedule.AppDefaultLocale;
import wfm.components.schedule.SystemProperties;
import wfm.components.utils.ScopeReportTab;
import wfm.models.EmployeePosition;
import wfm.repository.CommonRepository;
import wfm.repository.SystemPropertyRepository;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static common.ErrorMessagesForRegExp.SHIFT_ACTIONS_NOT_DISPLAYED;
import static utils.Links.LOG;
import static utils.Params.*;
import static utils.tools.CustomTools.systemSleep;

public class ScheduleWorker {

    private final TopBar fromTopBar;
    private final Layout layout;
    private final MainHeader mainHeader;
    private final WebDriver driver;
    private final String NOT_FOUND_ERROR_MESSAGE = "Не удалось найти элемент %s для сотрудника %s за %s";
    private final String locale;

    private enum elementType {
        SCHEDULE_REQUEST,
        OUTSIDE_PLAN
    }

    public ScheduleWorker(ScheduleBoardPage page) {
        fromTopBar = page.formTopBar();
        layout = page.formLayout();
        mainHeader = page.mainHeader();
        driver = page.getWrappedDriver();
        locale = SystemPropertyRepository.getSystemProperty(SystemProperties.APP_DEFAULT_LOCALE).getValue().toString();
    }

    /**
     * Определяем какой масштаб активен
     *
     * @return текущий масштаб расписания
     */
    private ScopeReportTab scopeChecker() {
        boolean isList = (boolean) SystemPropertyRepository.getSystemProperty(SystemProperties.SHOW_BUTTON_TO_PUBLISH_ROSTER).getValue();
        ArrayList<AtlasWebElement> scopeElementsList = new ArrayList<>();
        ScopeReportTab[] listOfScope = ScopeReportTab.values();
        if (isList) {
            return Arrays.stream(listOfScope)
                    .filter(scope -> scope.getScopeName().toLowerCase().contains(mainHeader.magnitPeriod().getText().toLowerCase()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Такого элемента нет"));
        }
        scopeElementsList.add(mainHeader.dayScope());
        scopeElementsList.add(mainHeader.weekScope());
        scopeElementsList.add(mainHeader.monthScope());

        AtlasWebElement currentActiveElement = scopeElementsList.stream()
                .filter(element -> element.getAttribute("class").contains(ACTIVE))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Такого индекса нет"));
        return Arrays.stream(listOfScope)
                .filter(scope -> String.valueOf(scope.ordinal()).contains(currentActiveElement.getAttribute("data-index")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Такого элемента нет"));
    }

    /**
     * Находит элемент смены по дате и имени сотрудника
     *
     * @param ep   позиция сотрудника, у которого ищем смену
     * @param date дата смены
     * @return элемент смены сотрудника в заданный день
     */
    public AtlasWebElement getScheduleShiftElement(EmployeePosition ep, LocalDate date) {
        int orderNumber = getOrderName(ep);
        ElementsCollection<AtlasWebElement> elements;
        List<String> indicators = layout.timesheetIndicator()
                .stream()
                .map(AtlasWebElement::getText)
                .collect(Collectors.toList());
        boolean twoIndicatorsAndPastShift = date.isBefore(LocalDate.now()) && indicators.size() > 1;
        String firstIndicator = indicators.get(0).replaceAll("( не .*| опуб.* | утв.*|\\.)", "");
        String timesheetIndicatorByLocale = AppDefaultLocale.findTextByLocale(locale);
        boolean startsWith = firstIndicator.startsWith(timesheetIndicatorByLocale);
        boolean oneIndicatorForTimesheet = indicators.size() == 1 && startsWith;
        if (twoIndicatorsAndPastShift || oneIndicatorForTimesheet) {
            elements = layout.shiftElementWorked(orderNumber, date, true);
        } else {
            elements = layout.shiftElementWorked(orderNumber, date, false);
        }
        if (elements.isEmpty()) {
            String errorMessage = String.format(NOT_FOUND_ERROR_MESSAGE, "смены", ep, date);
            LOG.info(errorMessage);
            return null;
        } else {
            return elements.stream().reduce((e1, e2) -> e2).orElse(null);
        }
    }

    /**
     * Находит элементы плановой или фактической смены для сотрудника в конкретный день
     * Актуально для объединенного отображения плановых/фактических смен
     *
     * @param ep          сотрудник, у которого ищем
     * @param date        координаты искомой смены
     * @param workedShift true, если нужен элемент фактической смены,
     *                    false, если нужен элемент плановой смены
     */
    public AtlasWebElement getPlanOrFactShiftElement(EmployeePosition ep, LocalDate date, boolean workedShift) {
        int orderNumber = getOrderName(ep);
        ElementsCollection<AtlasWebElement> elements = layout.shiftElementWorked(orderNumber, date, workedShift);
        if (elements.isEmpty()) {
            String errorMessage = String.format(NOT_FOUND_ERROR_MESSAGE, "смены", ep, date);
            LOG.info(errorMessage);
            return null;
        } else {
            return elements.get(0);
        }
    }

    /**
     * Находит элемент смены, переходящей между месяцами
     *
     * @param ep   сотрудник, у которого ищем
     * @param date координаты искомой смены
     */
    public AtlasWebElement getOutsideShiftElement(EmployeePosition ep, LocalDate date) {
        int orderNumber = getOrderName(ep);
        ElementsCollection<AtlasWebElement> elements;
        if (CommonRepository.URL_BASE.contains("pochta")) {
            elements = layout.shiftElementDefault(orderNumber, date, false);
        } else {
            elements = layout.shiftElementOutside(orderNumber, date, false);
        }
        if (elements.isEmpty()) {
            String errorMessage = String.format(NOT_FOUND_ERROR_MESSAGE, "смены", ep, date);
            LOG.info(errorMessage);
            return null;
        } else {
            return elements.get(0);
        }
    }

    public ElementsCollection<AtlasWebElement> getShiftPlanMismatch(EmployeePosition ep, LocalDate date) {
        int orderNumber = getOrderName(ep);
        return layout.shiftPlanMismatch(orderNumber, date);
    }

    /**
     * Находит элемент запроса (выходной, сверхурочная работа и пр.) по дате и имени сотрудника
     *
     * @param ep   позиция сотрудника, у которого ищем запрос
     * @param date дата запроса
     * @return элемент запроса сотрудника в заданный день
     */
    public AtlasWebElement getScheduleRequestElement(EmployeePosition ep, LocalDate date) {
        AtlasWebElement element = layout.shiftOrRequestElement(getOrderName(ep), date, elementType.SCHEDULE_REQUEST.name());
        if (element == null) {
            String errorMessage = String.format(NOT_FOUND_ERROR_MESSAGE, "запроса", ep, date);
            LOG.info(errorMessage);
        }
        return element;
    }

    public ElementsCollection<AtlasWebElement> getScheduleRequestElements(EmployeePosition ep, LocalDate date) {
        return layout.shiftOrRequestElements(getOrderName(ep), date, elementType.SCHEDULE_REQUEST.name());
    }

    /**
     * Находит элемент сверхурочной работы или дежурства по дате и имени сотрудника
     *
     * @param ep   позиция сотрудника, у которого ищем запрос
     * @param date дата запроса
     * @return элемент запроса сотрудника в заданный день
     */
    public AtlasWebElement getOutsidePlanElement(EmployeePosition ep, LocalDate date) {
        int orderNumber = getOrderName(ep);
        AtlasWebElement element = layout.shiftOrRequestElement(orderNumber, date, elementType.OUTSIDE_PLAN.name());
        if (element == null) {
            String errorMessage = String.format(NOT_FOUND_ERROR_MESSAGE, "запроса", ep, date);
            LOG.info(errorMessage);
        }
        return element;
    }

    public ElementsCollection<AtlasWebElement> getOutsidePlanElements(EmployeePosition ep, LocalDate date) {
        int orderNumber = getOrderName(ep);
        return layout.shiftOrRequestElements(orderNumber, date, elementType.OUTSIDE_PLAN.name());
    }

    /**
     * Находит элемент доп. работ по дате и имени сотрудника
     *
     * @param ep   позиция сотрудника, у которого ищем доп. работу
     * @param date дата доп. работы
     */
    public AtlasWebElement getAdditionalWorkElement(EmployeePosition ep, LocalDate date) {
        int orderNumber = getOrderName(ep);
        List<AtlasWebElement> elements = layout.additionalWorkElement(orderNumber, date);
        if (elements.isEmpty()) {
            String errorMessage = String.format(NOT_FOUND_ERROR_MESSAGE, "доп. работы", ep, date);
            LOG.info(errorMessage);
            return null;
        } else {
            return elements.get(0);
        }
    }

    /**
     * Кликает на элемент плановой или фактической смены для сотрудника в конкретный день
     * Актуально для объединенного отображения плановых/фактических смен
     *
     * @param ep          сотрудник, у которого ищем
     * @param date        координаты искомой смены
     * @param workedShift true, если нужен элемент фактической смены,
     *                    false, если нужен элемент плановой смены
     */
    public void clickOnTargetShiftPlanOrFact(EmployeePosition ep, LocalDate date, boolean workedShift) {
        AtlasWebElement targetShift = getPlanOrFactShiftElement(ep, date, workedShift);
        checkForNull(targetShift, ep, date);
        targetShift.waitUntil("Смена не отобразилась", DisplayedMatcher.displayed(), 5);
        try {
            new Actions(driver).moveToElement(targetShift).click().build().perform();
        } catch (WebDriverException e) {
            makeShiftClickable(ep);
            new Actions(driver).moveToElement(targetShift).click().build().perform();
        }
    }

    /**
     * Тыкаем в ячейку смены на UI по заданным координатам, основная задача тыкать в пустые ячейки.
     *
     * @param employeePosition связка сотрудника и его должности
     * @param day              значение даты или часа
     */
    public void onEmptyCellClicker(EmployeePosition employeePosition, int day, boolean forMassActions) {
        scroll(true);
        int verticalLinesEnd = layout.verticalLine().size() - 1;
        int orderName = getOrderName(employeePosition);
        int location1 = layout.horizontalLine().get(0).getLocation().y;
        int location2 = layout.horizontalLine().get(1).getLocation().y;
        int y = ((location2 - location1) / 2);
        int x;
        if (scopeChecker() == ScopeReportTab.DAY) {
            location1 = fromTopBar.hourAboveGraph().get(0).getLocation().x;
            location2 = fromTopBar.hourAboveGraph().get(1).getLocation().x;
            x = (location2 - location1) * (day) + ((location2 - location1) / 2);
        } else {
            location1 = fromTopBar.dateAboveGraph().get(0).getLocation().x;
            location2 = fromTopBar.dateAboveGraph().get(1).getLocation().x;
            x = (location2 - location1) * (day - 1) + ((location2 - location1) / 2);
        }
        new Actions(driver)
                .moveToElement(layout.horizontalLine().get(orderName))
                .perform();
        layout.allEmployeeNameButtons().get(orderName).waitUntil(DisplayedMatcher.displayed(), 5);
        //делаем ховер из-за того что расписание не сжимается теперь.
        if (day > 25) {
            new Actions(driver).moveToElement(layout.verticalLine().get(verticalLinesEnd));
            //layout.verticalLine().get(layout.verticalLine().size() - 1).hover();
        }
        int middleOfHorisontalLineX = (layout.verticalLine().get(verticalLinesEnd).getCoordinates().inViewPort().x - layout.verticalLine().get(0).getCoordinates().inViewPort().x) / 2;
        if (forMassActions) {
            new Actions(driver).moveToElement(layout.horizontalLine().get(orderName), -middleOfHorisontalLineX, y).moveByOffset(x, 0).build().perform();
            new Actions(driver).moveToElement(layout.horizontalLine().get(orderName), -middleOfHorisontalLineX, y).moveByOffset(x, 0).build().perform();
            layout.blueFrame().waitUntil("Синяя рамка не отобразилась", DisplayedMatcher.displayed(), 5);
            layout.blueFrame().click();
        } else {
            new Actions(driver).moveToElement(layout.horizontalLine().get(orderName), -middleOfHorisontalLineX, y).moveByOffset(x, 0).click().build().perform();
        }
    }

    /**
     * Кликает на красный кружочек в строке "Конфликты" над расписанием за указанный день месяца
     *
     * @param day порядковый номер дня в месяце
     */
    public void onConflictCircleClicker(int day) {
        int location1 = layout.conflictCircles().get(0).getLocation().x;
        int location2 = layout.conflictCircles().get(1).getLocation().x;
        int x = (location2 - location1) * (day - 1);
        new Actions(driver).moveToElement(layout.conflictCircles().get(0), x, 0).click().build().perform();
    }

    /**
     * Определяет порядковый номер сотрудника на UI.
     *
     * @param employeePosition искомый сотрудник
     */
    private int getOrderName(EmployeePosition employeePosition) {
        ElementsCollection<AtlasWebElement> employees = layout.allEmployeeNameButtons();
        for (int i = 0; i < employees.size(); i++) {
            if (employees.get(i).getAttribute(ID).contains(Integer.toString(employeePosition.getId()))) {
                return i;
            }
        }
        throw new AssertionError("Сотрудник не найден в списке");
    }

    /**
     * Производит перенос смены
     *
     * @param empPosFrom от кого переносим
     * @param empPosTo   кому
     * @param dayFrom    за какую дату смену берем
     * @param dayTo      на какую дату переносим
     */
    public void exchangeShift(EmployeePosition empPosFrom, EmployeePosition empPosTo, LocalDate dayFrom, int dayTo) {
        WebElement toTake = getScheduleShiftElement(empPosFrom, dayFrom);
        systemSleep(1);
        checkForNull(toTake, empPosFrom, dayFrom);
        int orderName = getOrderName(empPosFrom);
        Actions actions = new Actions(driver);
        int locationX1 = fromTopBar.dateAboveGraph().get(0).getLocation().x;
        int locationX2 = fromTopBar.dateAboveGraph().get(1).getLocation().x;
        int diff = locationX2 - locationX1;
        int targetLocation = toTake.getLocation().x;
        int x = ((diff) * (dayTo)) + locationX1 - targetLocation - diff;
        int nameLocation1 = layout.horizontalLine().get(0).getLocation().y;
        int nameLocation2 = layout.horizontalLine().get(1).getLocation().y;
        diff = nameLocation2 - nameLocation1;
        int targetOrder = getOrderName(empPosTo);
        int y;
        if (empPosFrom.equals(empPosTo)) {
            y = 0;
        } else {
            y = diff * (targetOrder - orderName);
        }
        int maxDay = LocalDate.now().lengthOfMonth();
        //Иф добавлен для того чтобы исключить случаи с ненажимаемыми сменами под стрелочками влево вправо на расписании
        if (dayTo >= maxDay - 1 && orderName > 7) {
            try {
                actions.moveToElement(layout.horizontalLine().get(orderName + 2)).perform();
            } catch (Exception e) {
                actions.moveToElement(layout.underTable()).perform();
            }
        } else {
            actions.moveToElement(layout.horizontalLine().get(orderName)).perform();
        }
        actions.clickAndHold(toTake).pause(1000).moveByOffset(x, y).pause(1000).release().perform();
        layout.shiftActionPanel().waitUntil(SHIFT_ACTIONS_NOT_DISPLAYED, DisplayedMatcher.displayed(), 30);
    }

    /**
     * Прокручивает список сотрудников на две строки ниже той, в которой находится заданный сотрудник.
     * Нужно в случаях, когда смена заданного сотрудника оказывается под стрелками переключения месяца.
     */
    public void makeShiftClickable(EmployeePosition ep) {
        int orderName = getOrderName(ep);
        if (layout.horizontalLine().size() >= orderName + 3) {
            new Actions(driver).moveToElement(layout.horizontalLine().get(orderName + 2)).perform();
        } else {
            scroll(false);
        }
    }

    /**
     * Прокручивает страницу на самый верх.
     */
    public void scroll(boolean up) {
        Keys scroll = Keys.END;
        if (up) {
            scroll = Keys.HOME;
        }
        new Actions(driver).sendKeys(fromTopBar.monthSelected(), scroll).perform();
    }

    /**
     * Выбираем через SHIFT две смены одного сотрудника.
     *
     * @param employeePosition имя сотрудника
     * @param dates            даты, которые будем выделять
     */
    public void selectTwoShifts(EmployeePosition employeePosition, LocalDate[] dates) {
        LocalDate td = dates[0];
        LocalDate tmd = dates[1];
        Actions actions = new Actions(driver);
        LocalDate last = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        //Иф добавлен для того чтобы исключить случаи с ненажимаемыми сменами под стрелочками влево вправо на расписании
        if (td.isEqual(last) || tmd.isEqual(last)) {
            int orderName = getOrderName(employeePosition);
            if (orderName > 7) {
                try {
                    actions.moveToElement(layout.horizontalLine().get(orderName + 2)).perform();
                } catch (Exception e) {
                    actions.moveToElement(layout.underTable()).perform();
                }
            } else {
                actions.moveToElement(layout.horizontalLine().get(orderName + 1)).perform();
            }
        }
        AtlasWebElement targetShift = getScheduleShiftElement(employeePosition, td);
        checkForNull(targetShift, employeePosition, td);
        AtlasWebElement toShift = getScheduleShiftElement(employeePosition, tmd);
        checkForNull(targetShift, employeePosition, tmd);
        actions.keyDown(Keys.SHIFT).click(targetShift).click(toShift).keyUp(Keys.SHIFT).build().perform();
    }

    /**
     * Вспомогательный метод. Выбрасывает WaitUntilException, если заданный элемент смены == null
     */
    private void checkForNull(WebElement element, EmployeePosition ep, LocalDate date) {
        if (element == null) {
            throw new WaitUntilException(String.format("Элемент смены сотрудника %s за %s не найден", ep, date));
        }
    }

    /**
     * Дублирует смены на указанные даты.
     *
     * @param employeePosition сотрудник, на которого будут продублированы смены
     * @param date             первая из дат, на которые будут перемещаться смены
     */
    public void manipulationShiftDuplicate(EmployeePosition employeePosition, LocalDate date) {
        layout.duplicateMassShift().click();
        int day = date.getDayOfMonth();
        scroll(true);
        int verticalLinesEnd = layout.verticalLine().size() - 1;
        int targetVerticalLineLocation = layout.verticalLine().get(day).getLocation().x;
        int middleOfHorisontalLineX = (layout.verticalLine().get(verticalLinesEnd).getCoordinates().inViewPort().x - layout.verticalLine().get(0).getCoordinates().inViewPort().x) / 2;
        int orderName = getOrderName(employeePosition);
        int location1 = layout.horizontalLine().get(0).getLocation().y;
        int location2 = layout.horizontalLine().get(1).getLocation().y;
        int y = ((location2 - location1) / 2);
        int x;
        int xDiff;
        if (scopeChecker() == ScopeReportTab.DAY) {
            location1 = fromTopBar.hourAboveGraph().get(0).getLocation().x;
            location2 = fromTopBar.hourAboveGraph().get(1).getLocation().x;
            xDiff = (location2 - location1);
            x = xDiff * (day) + (xDiff / 2);
        } else {
            location1 = layout.verticalLine().get(0).getLocation().x;
            location2 = layout.verticalLine().get(1).getLocation().x;
            xDiff = (location2 - location1);
            x = xDiff * (day - 1) + xDiff / 2;
        }
        new Actions(driver)
                .moveToElement(layout.horizontalLine().get(orderName))
                .perform();
        layout.allEmployeeNameButtons().get(orderName).waitUntil(DisplayedMatcher.displayed());
        //делаем ховер из-за того что расписание не сжимается теперь.
        if (day > 25) {
            new Actions(driver).moveToElement(layout.verticalLine().get(verticalLinesEnd));
        }
        new Actions(driver).moveToElement(layout.horizontalLine().get(orderName), -middleOfHorisontalLineX, y).moveByOffset(x, 0).build().perform();
        new Actions(driver).moveToElement(layout.horizontalLine().get(orderName), -middleOfHorisontalLineX, y).moveByOffset(x, 0).build().perform();
        layout.blueFrame().waitUntil("Синяя рамка не отобразилась", DisplayedMatcher.displayed(), 5);
        if (layout.blueFrame().getLocation().x < targetVerticalLineLocation) {
            new Actions(driver).moveByOffset(xDiff, 0).build().perform();
            layout.blueFrame().waitUntil("Синяя рамка не отобразилась", DisplayedMatcher.displayed(), 5);
        }
        layout.blueFrame().click();
    }

    /**
     * Перемещает смены на указанные даты
     *
     * @param empPosToMove сотрудник, на которого будут перемещены смены
     * @param date         первая из дат, которые будут перемещаться
     */
    public void manipulationShiftTransfer(EmployeePosition empPosToMove, LocalDate date) {
        layout.replaceMassShift().click();
        onEmptyCellClicker(empPosToMove, date.getDayOfMonth(), true);
    }

    /**
     * Находит класс элемента, вложенного в элемент расписания
     */
    public String getInternalClass(AtlasWebElement element) {
        return layout.getInfo(element.getAttribute(ID)).getAttribute("class");
    }

    /**
     * Находит элемент отметки о присутствии
     *
     * @param ep - позиция сотрудника, у которого нужно найти отметку
     * @return найденный элемент
     */
    public AtlasWebElement getPresenceMarkElement(EmployeePosition ep) {
        int orderNumber = getOrderName(ep);
        return layout.presenceMark(orderNumber);
    }

    public String getShiftHours(EmployeePosition ep, LocalDate lastDate, boolean b) {
        int employeeOrder = getOrderName(ep);
        AtlasWebElement shiftHours = layout.shiftHours(employeeOrder, lastDate, b, 1);
        String shiftStart = shiftHours.getText();
        if (shiftStart.contains("-")) {
            return shiftStart;
        } else {
            String shiftEnd = layout.shiftHours(employeeOrder, lastDate, b, 2).getText();
            return String.format("%s-%s", shiftStart, shiftEnd);
        }
    }
}