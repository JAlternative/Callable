package pagemodel;

import com.google.inject.Inject;
import com.mchange.util.AssertException;
import guice.TestModule;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.exception.WaitUntilException;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hamcrest.Matchers;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.AnalyticsPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.*;
import utils.Links;
import utils.downloading.FileDownloadCheckerForReport;
import utils.downloading.TypeOfFiles;
import utils.downloading.TypeOfReports;
import utils.tools.CustomTools;
import utils.tools.LocalDateTools;
import wfm.PresetClass;
import wfm.components.analytics.*;
import wfm.components.schedule.DateUnit;
import wfm.components.schedule.KPIOrFTE;
import wfm.components.schedule.ScheduleType;
import wfm.components.utils.*;
import wfm.models.DateInterval;
import wfm.models.Kpi;
import wfm.models.KpiList;
import wfm.models.OrgUnit;
import wfm.repository.CommonRepository;
import wfm.repository.KpiListRepository;
import wfm.repository.KpiRepository;
import wfm.repository.OrgUnitRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static common.Groups.*;
import static org.hamcrest.Matchers.*;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.tools.CustomTools.*;
import static utils.tools.Format.UI;
import static utils.tools.Format.UI_DOTS;
import static wfm.components.analytics.KpiType.getAnotherType;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class AnalyticsInt extends BaseTest {

    private static final String RELEASE_URL = Links.getTestProperty("release");
    private static final Section SECTION = Section.ANALYTICS;
    private static final String URL_A = RELEASE_URL + SECTION.getUrlEnding();
    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsInt.class);
    private static final TemporalAdjuster LAST_DAY = TemporalAdjusters.lastDayOfMonth();
    private static final int YEARS_MAXIMUM_COUNT = 5; // Переменная для определения того насколько далеко по годам мы хотим преключаться

    @Inject
    private AnalyticsPage ap;

    @DataProvider(name = "KpiList")
    private static Object[][] getKpiList() {
        List<String> tempList = CommonRepository.getVisibleKpiNames();
        Object[][] array = new Object[tempList.size()][];
        for (int i = 0; i < tempList.size(); i++) {
            array[i] = new Object[]{tempList.get(i)};
        }
        String kpiContent = Arrays.deepToString(array);
        LOG.info("Видимость включена у kpi с названиями: {}", kpiContent);
        Allure.addAttachment("Видимость", "Видимость включена у следующих параметров KPI: " + kpiContent);
        return array;
    }

    @DataProvider(name = "RolesWithoutPermissions")
    private static Object[][] rolesWithoutPermissions() {
        Object[][] array = new Object[5][2];
        array[0] = new Object[]{Role.THIRD,
                Arrays.asList(
                        TypeOfChartMenu.KPI_FORECAST,
                        TypeOfChartMenu.KPI_PUBLISHED,
                        TypeOfChartMenu.FTE_FORECAST,
                        TypeOfChartMenu.FTE_PUBLISHED)};
        array[1] = new Object[]{Role.FOURTH,
                Arrays.asList(
                        TypeOfChartMenu.KPI_PUBLISHED,
                        TypeOfChartMenu.FTE_FORECAST,
                        TypeOfChartMenu.FTE_PUBLISHED)};
        array[2] = new Object[]{Role.FIFTH,
                Arrays.asList(
                        TypeOfChartMenu.KPI_FORECAST,
                        TypeOfChartMenu.FTE_FORECAST,
                        TypeOfChartMenu.FTE_PUBLISHED)};
        array[3] = new Object[]{Role.SIXTH,
                Arrays.asList(
                        TypeOfChartMenu.KPI_FORECAST,
                        TypeOfChartMenu.KPI_PUBLISHED,
                        TypeOfChartMenu.FTE_PUBLISHED)};
        array[4] = new Object[]{Role.SEVENTH,
                Arrays.asList(
                        TypeOfChartMenu.KPI_FORECAST,
                        TypeOfChartMenu.KPI_PUBLISHED,
                        TypeOfChartMenu.FTE_FORECAST)};
        return array;
    }

    @DataProvider(name = "DataList")
    private static Object[][] getDataList() {
        Object[][] array = new Object[3][];
        KpiRepository forecastValues = new KpiRepository(DateUnit.MONTH);
        forecastValues.getRandomForecast(true, true);
        OrgUnit omNameForecast = forecastValues.getOrgUnit();
        KpiRepository historyValues = new KpiRepository(DateUnit.MONTH);
        historyValues.getRandomHistory(true, true);
        OrgUnit omNameHistory = historyValues.getOrgUnit();
        OrgUnit fteOm = OrgUnitRepository.getRandomFteUnit();
        array[0] = new Object[]{KPIOrFTE.KPI_FORECAST, omNameForecast};
        array[1] = new Object[]{KPIOrFTE.KPI_HISTORY, omNameHistory};
        array[2] = new Object[]{KPIOrFTE.FTE, fteOm};
        LOG.info("Видимость включена у kpi с названиями: {}", Arrays.deepToString(array));
        Allure.addAttachment("Пресет", "Для следующих данных были выбраны оргюниты: " +
                "\nПрогноз: " + omNameForecast.getName() +
                "\nИсторические данные: " + omNameHistory.getName() +
                "\nРесурсная потребность: " + fteOm.getName());
        return array;
    }

    private static String getFormattedDate(ChronoUnit unit, LocalDate date) {
        String expectedText;
        Locale locale = Locale.forLanguageTag("ru");
        switch (unit) {
            case DAYS:
                expectedText = date.getDayOfMonth() + " " + MonthsEnum.values()[date.getMonthValue() - 1].getDeclensionName().toLowerCase()
                        + ", " + date.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
                break;
            case MONTHS:
                expectedText = date.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, locale) + " " + date.getYear();
                break;
            case YEARS:
                expectedText = date.format(DateTimeFormatter.ofPattern("yyyy"));
                break;
            default:
                throw new AssertionError("Формат " + unit.toString() + " не поддерживается");
        }
        return expectedText;
    }

    private List<LocalDate> getDatesForForecast() {
        LocalDate now = LocalDate.now();
        LocalDate lastMonth = now.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastYear = now.minusYears(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastMonthYear = lastYear.minusMonths(1);
        return new ArrayList<>(Arrays.asList(lastMonthYear, lastYear, lastMonth));
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void setUp() {
        setBrowserTimeout(ap.getWrappedDriver(), 30);
    }

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriver() {
        closeDriver(ap.getWrappedDriver());
    }

    @AfterMethod(alwaysRun = true, description = "Очистить куки и подготовить новую вкладку")
    private void cleanCookies() {
        cleanCookiesAndOpenNewTab(ap.getWrappedDriver());
    }

    private void goToAnalytics() {
        new GoToPageSection(ap).getPage(SECTION, 60);
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 90);
    }

    @Step("Выбор из списка ОМ с названием: {unit.name}")
    private void certainOmFromList(OrgUnit unit) {
        String unitName = unit.getName();
        slowSendKeys(ap.searchOmList().inputSearch(), unitName);
        try {
            ap.searchOmList().certainOm(unitName).waitUntil("Введеный оргюнит не отображается в списке",
                                                            DisplayedMatcher.displayed(), 10);
            ap.searchOmList().certainOm(unitName).click();
        } catch (WaitUntilException e) {
            ap.searchOmList().inputSearch().sendKeys(Keys.SPACE);
            systemSleep(2); //Метод используется в неактуальных тестах
            ap.searchOmList().inputSearch().sendKeys(Keys.BACK_SPACE);
            systemSleep(2); //Метод используется в неактуальных тестах
            ap.searchOmList().certainOm(unitName).click();
        }
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 90);
    }

    /**
     * Перейти в раздел прогнозирования сразу на конкретный оргюнит
     *
     * @param unit - оргюнит на страницу которого осуществляется переход
     */
    private void goToAnalytics(OrgUnit unit) {
        new GoToPageSection(ap).goToOmWithoutUI(unit, SECTION);
    }

    /**
     * Перейти в раздел прогнозирования как пользователь сразу на конкретный оргюнит
     *
     * @param unit - оргюнит на страницу которого осуществляется переход
     */
    @Step("Перейти в раздел \"Прогнозирование\" подразделения \"{unit.name}\" с ролью \"{role.name}\"")
    private void goToAnalyticsAsUser(Role role, OrgUnit unit) {
        new RoleWithCookies(ap.getWrappedDriver(), role, unit).getSectionPageForSpecificOrgUnit(unit.getId(), SECTION);
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 90);
    }

    @Step("Кликнуть на значок \"дискеты\"")
    private void saveCorrectionButton() {
        CustomTools.waitForClickable(ap.correctionTable().saveCorrectionButton(), ap, 60);
        ap.correctionTable().saveCorrectionButton().click();
        ap.saveCommentForCorrectionForm().saveButton().waitUntil("Кнопка сохранить в форме не отобразилась",
                                                                 DisplayedMatcher.displayed(), 2);
    }

    @Step("Внести значение: {generatedString} в поле \"Комментарий\" ")
    private void sendCorrectionComment(String generatedString) {
        ap.saveCommentForCorrectionForm().inputComment().clear();
        ap.saveCommentForCorrectionForm().inputComment().sendKeys(generatedString);
        Allure.addAttachment("Ввод текста", "В поле ввода комментария было введено: " + generatedString);
    }

    @Step("Кликнуть \"Сохранить\"")
    private void saveCorrectionComment() {
        ap.saveCommentForCorrectionForm().saveButton().click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Проверка прогноза - поп-ап калькуляции отображается")
    private void checkForecast(ListOfNotification notification) {
        String notificationName = notification.getNotificationName();
        LOG.info("Ожидаемый текст: {}", notificationName);
        ap.downPanelResult().snackBar().
                should("Поп-ап с сообщением не отображается", DisplayedMatcher.displayed(), 200);
        String message = ap.downPanelResult().snackBar().getText();
        LOG.info("Текст всплывающего сообщения: {}", message);
        Allure.addAttachment("Проверка", "Было отображено сообщение с текстом: " + message);
        Assert.assertTrue(
                message.contains(notificationName),
                "Текст всплывающего сообщения не совпал с ожидаемым, ждали \"" + notificationName + "\", а получили: " + message
        );
    }

    @Step("В поле ввода тренда ввести: {trend}")
    private void sendInTrendForm(String trend) {
        ap.kpiForecastForm().trendForm().sendKeys(trend);
    }

    @Step("Нажать на кнопку импорт")
    private void importButtonClick() {
        ap.kpiForecastForm().kpiImport().click();
    }

    @Step("Нажать на значок магазина")
    private void omButtonClick() {
        ap.leftBarEdit().homeButton().click();
        ap.leftBarEdit().listOfDivisions().waitUntil("Список подразделений во вкладке «Расписание» не открылся",
                                                     DisplayedMatcher.displayed(), 3);
    }

    @Step("Кликнуть на произвольное подразделение в списке.")
    private void clickOnRandomOmFromList() {
        List<AtlasWebElement> list = ap.searchOmList().certainOm().waitUntil(Matchers.hasSize(Matchers.greaterThan(0)));
        AtlasWebElement randomOm = getRandomFromList(list);
        Allure.addAttachment("Оргюнит", "Был выбран оргюнит с именем: " + randomOm.getText());
        randomOm.click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 60);
    }

    @Step("Выбрать оргюнит для импорта : {certainOm}")
    private void getAnyCertainOmImportForecast(String certainOm) {
        ap.kpiForecastForm().orgUnitImport().clear();
        ap.kpiForecastForm().orgUnitImport().click();
        slowSendKeys(ap.kpiForecastForm().orgUnitImportInput(), certainOm);
        ap.kpiForecastForm().orgUnitImportInput().sendKeys(Keys.BACK_SPACE);
        ap.kpiForecastForm().orgUnitImportList().filter(o -> o.getText().trim().equals(certainOm)).iterator().next().click();
    }

    @Step("Выбрать оргюнит для импорта : {certainOm}")
    private void getAnyCertainOmImportFte(String certainOm) {
        slowSendKeys(ap.fteForm().orgUnitField(), certainOm);
        ElementsCollection<AtlasWebElement> omList = ap.fteForm().fteOrgUnitImportList();
        try {
            omList.get(0).click();
        } catch (ElementNotInteractableException e) {
            omList.get(0).click();
        }
    }

    @Step("Кликнуть на значок \"i\"")
    private void indicatorButtonClick() {
        waitForClickable(ap.leftBarEdit().indicatorsButton(), ap, 25);
        ap.leftBarEdit().indicatorsButton().click();
    }

    @Step("Выбрать показатель \"{name}\"")
    private void chooseIndicator(String name) {
        systemSleep(2); //нужно время, что бы ровно кликнул
        ap.informationForm().variantsInInfo(name).click();
    }

    @Step("Выбрать данные для отображения \"{data.name}\"")
    private void chooseIndicator(KPIOrFTE data) {
        systemSleep(2); //нужно время, что бы ровно кликнул
        ap.informationForm().variantsInInfo(data.getName()).click();
    }

    @Step("Проверить, что данные {data.name} отображены")
    private void assertDataIsDisplayed(KPIOrFTE data) {
        systemSleep(5); //Метод используется в неактуальных тестах
        switch (data) {
            case FTE:
                ap.rightGraphicDiagramForm().fteLine()
                        .should("Линия ресурсной потребности не была отображена", DisplayedMatcher.displayed(), 20);
                break;
            case KPI_HISTORY:
                ap.rightGraphicDiagramForm().kpiHistory().waitUntil(Matchers.hasSize(Matchers.greaterThan(0)));
                Allure.addAttachment("Проверка", "Было отображено "
                        + ap.rightGraphicDiagramForm().kpiHistory().size() + " столбцов исторических данных на графике");
                break;
            case KPI_FORECAST:
                ap.rightGraphicDiagramForm().kpiForecast().waitUntil(Matchers.hasSize(Matchers.greaterThan(0)));
                Allure.addAttachment("Проверка", "Было отображено "
                        + ap.rightGraphicDiagramForm().kpiForecast().size() + " столбцов прогноза на графике");
                break;
        }
    }

    @Step("Проверить, что данные {data.name} не отображены")
    private void assertDataIsNotDisplayed(KPIOrFTE data) {
        systemSleep(5); //Метод используется в неактуальных тестах
        switch (data) {
            case FTE:
                ap.rightGraphicDiagramForm().fteLine()
                        .should("Линия ресурсной потребности всё еще отображена", not(DisplayedMatcher.displayed()), 20);
                break;
            case KPI_HISTORY:
                ap.rightGraphicDiagramForm().kpiHistory().should("Столбцы с историческими данными не были скрыты",
                                                                 iterableWithSize(0));
                Allure.addAttachment("Проверка", "Было отображено "
                        + ap.rightGraphicDiagramForm().kpiHistory().size() + " столбцов исторических данных на графике");
                break;
            case KPI_FORECAST:
                ap.rightGraphicDiagramForm().kpiForecast().waitUntil("Столбцы с прогнозом не были скрыты",
                                                                     iterableWithSize(0));
                Allure.addAttachment("Проверка", "Было отображено "
                        + ap.rightGraphicDiagramForm().kpiForecast().size() + " столбцов прогноза на графике");
                break;
        }
    }

    @Step("Выбор из списка ОМ с названием: {certainOM}")
    private void certainOmFromListWithOutSearch(String certainOM) {
        ap.searchOmList().certainOm(certainOM).click();
        waitForClickable(ap.diagramSwitcher().leftGraph(), ap, 25);
        waitForClickable(ap.leftBarEdit().compareButton(), ap, 25);
    }

    @Step("Перейти в режим отображения \"Месяц\"")
    private void scopeSwitchMonth() {
        ap.dataNavSwitcher().monthScope().click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 20);
    }

    @Step("Изменение масштаба на день")
    private void scopeSwitchDay() {
        ap.dataNavSwitcher().dayScope().click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Изменение масштаба на год")
    private void scopeSwitchYear() {
        ap.dataNavSwitcher().yearScope().click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Нажать на стрелку влево в правом нижнем углу рабочей области и переключить график влево")
    private void diagramSlideLeft() {
        ap.diagramSwitcher().leftGraph().click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Переключить график вправо")
    private void diagramSlideRight() {
        ap.diagramSwitcher().rightGraph().click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    /**
     * @param shift     Указывается смешение относительно текущего года
     *                  пример: переключаемся с 2019 на 2018 указывается "1"
     *                  при проверке на отображение текущего года указывается "0"
     * @param operation Указывается смещение влево(вычитание) или вправо(сложение)
     *                  пример: переключаемся влево с 2019 указываем "false"
     */
    @Step("Проверка текущей даты: {shift, operation }")
    private void currentDateAssert(int shift, boolean operation) {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy");
        String formattedDate = myDateObj.format(myFormatObj);
        String shiftTime;
        final AtlasWebElement datePlaceHolder = ap.diagramChart().currentChartDate();
        if (shift == 0) {
            datePlaceHolder.should("Неверная дата " + formattedDate, text(containsString(formattedDate)), 5);
        } else {
            if (operation) {
                shiftTime = String.valueOf(Integer.parseInt(formattedDate) + shift);
            } else {
                shiftTime = String.valueOf(Integer.parseInt(formattedDate) - shift);
            }
            datePlaceHolder.should("Неверная дата " + shiftTime, text(containsString(shiftTime)), 5);
        }
    }

    @Step("Кликнуть на значок \"карандаш\"")
    private void pencilButtonClick() {
        waitForClickable(ap.leftBarEdit().pencilButton(), ap, 25);
        ap.leftBarEdit().pencilButton().click();
        ap.correctionTable().should("Окно коррекции не открылось", DisplayedMatcher.displayed(), 5);
    }

    @Step("Перемещает слайдер вправо или влево на случайное значение")
    private void sliderMove(boolean swipeRight) {
        Random random = new Random();
        int x = 1 + random.nextInt(10);
        int counter = 0;
        String side = "";
        for (int i = 0; i < x; i++) {
            if (swipeRight) {
                ap.correctionSlider().slider().sendKeys(Keys.ARROW_RIGHT);
                side = "вправо";
                counter++;
            } else {
                ap.correctionSlider().slider().sendKeys(Keys.ARROW_LEFT);
                side = "влево";
                counter--;
            }
        }
        String visibleValue = ap.correctionSlider().fieldCoefficientCorrection().getAttribute("value").trim();
        int value = (int) (Double.parseDouble(visibleValue) * 10);
        Assert.assertEquals(counter, value);
        Allure.addAttachment("Значение смещения", "text/plain",
                             "Слайдер был перемещен" + side + "Значение коэффициента: " + visibleValue);
    }

    @Step("Нажать на значение коррекции {column} столбец")
    private void selectCorrectionCell(Column column, int cell) {
        AtlasWebElement element = ap.correctionTable().getColumnValues(column.getColumnNumber()).get(cell);
        waitForClickable(ap.diagramSwitcher().leftGraph(), ap, 25);
        new Actions(ap.getWrappedDriver()).moveToElement(element).perform();
        element.click();
        String dateValue = ap.correctionTable().getColumnValues(Column.DATE.getColumnNumber()).get(cell).getText();
        LOG.info("Было выбрано значение коррекции за {}", dateValue);
        Allure.addAttachment("Выбор даты", "Было выбрано значение коррекции за " + dateValue);
    }

    @Step("Нажать на значение коррекции прогноза, соотвествующее месяцу: {month}")
    private void selectForecastCorrectionCell(Month month) {
        int cell = month.getValue() - 1;
        new Actions(ap.getWrappedDriver()).moveToElement(ap.correctionTable().forecastCorrectionValues().get(cell)).perform();
        ap.correctionTable().forecastCorrectionValues().get(cell).click();
        String dateValue = ap.correctionTable().getColumnValues(Column.DATE.getColumnNumber()).get(cell).getText();
        LOG.info("Было выбрано значение коррекции за {}", dateValue);
    }

    @Step("Нажать на значение коррекции прогноза, соотвествующее {day} дню месяца")
    private void selectForecastCorrectionCell(int day) {
        int cell = day - 1;
        new Actions(ap.getWrappedDriver()).moveToElement(ap.correctionTable().forecastCorrectionValues().get(cell)).perform();
        ap.correctionTable().forecastCorrectionValues().get(cell).click();
        String dateValue = ap.correctionTable().getColumnValues(Column.DATE.getColumnNumber()).get(cell).getText();
        LOG.info("Было выбрано значение коррекции за {}", dateValue);
    }

    @Step("Нажать на значение коррекции ист. данных соотвествующее времени {time}")
    private void selectHistoryValueByTime(LocalTime time) {
        new Actions(ap.getWrappedDriver()).moveToElement(ap.correctionTable().historyCorrectionsOnTimeUnit(Column.CORR_3.getColumnNumber(), time.toString())).perform();
        ap.correctionTable().historyCorrectionsOnTimeUnit(Column.CORR_3.getColumnNumber(), time.toString()).click();
        LOG.info("Было выбрано значение коррекции ист. данных соответствующее времени {}", time.toString());
    }

    @Step("Нажать на значение коррекции прогноза соотвествующее времени {time}")
    private void selectForecastValueByTime(LocalTime time) {
        new Actions(ap.getWrappedDriver()).moveToElement(ap.correctionTable().forecastCorrectionsOnTimeUnit(time.toString())).perform();
        ap.correctionTable().forecastCorrectionsOnTimeUnit(time.toString()).click();
        LOG.info("Было выбрано значение коррекции прогноза соответствующее времени {}", time.toString());
    }

    @Step("Переключиться на {year} год на графике")
    private void switchToNeedYear(int year) {
        boolean switchLeft = LocalDateTools.now().getYear() >= year;
        int count = 0;
        while (!ap.diagramChart().currentChartDate().getText().equals(String.valueOf(year)) && count <= YEARS_MAXIMUM_COUNT) {
            if (switchLeft) {
                ap.diagramSwitcher().leftGraph().click();
            } else {
                ap.diagramSwitcher().rightGraph().click();
            }
            ap.spinnerLoader().grayLoadingBackground()
                    .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 60);
            systemSleep(1); //цикл
            count++;
        }
        Assert.assertEquals(String.valueOf(year), ap.diagramChart().currentChartDate().getText(),
                            "Не смогли переключиться на год: " + year);
    }

    @Step("Проверка того что кнопка \"Сохранить\" не активна")
    private void assertButtonSaveNotActive() {
        ap.correctionSlider().offOkButton()
                .should("Кнопка \"Сохранить\" активна", DisplayedMatcher.displayed(), 10);
    }

    @Step("Нажать значок троеточие")
    private void clickToChartMenuButton() {
        waitForClickable(ap.leftBarEdit().diagramChartMenuButton(), ap, 25);
        ap.leftBarEdit().diagramChartMenuButton().click();
        ap.editFrom().waitUntil("Форма троеточия не прогрузилась", DisplayedMatcher.displayed(), 10);
    }

    @Step("Кликнуть по значку сравнения графиков в боковом меню")
    private void compareButtonClick() {
        waitForClickable(ap.leftBarEdit().compareButton(), ap, 25);
        ap.leftBarEdit().compareButton().click();
    }

    @Step("Выбрать расчет \"{menuType.name}\"")
    private void selectChartMenuOption(TypeOfChartMenu menuType) {
        waitForClickable(ap.editFrom(), ap, 25);
        ap.editFrom().listOfChartMenu().waitUntil(Matchers.hasSize(Matchers.greaterThan(3)));
        ap.editFrom().chartMenuType(menuType.getName())
                .waitUntil("Нужный вариант в троеточии не прогрузился", DisplayedMatcher.displayed(), 10);
        ap.editFrom().chartMenuType(menuType.getName()).click();
    }

    @Step("Выбрать kpi из списка")
    private void selectKpiType(List<KpiList> kpiList) {
        ap.kpiForecastForm().kpiTypeList().click();
        for (KpiList kpi : kpiList) {
            if (!ap.kpiForecastForm().kpiCheckValueType(kpi.getName()).getAttribute("class").contains("check")) {
                //без этого часто выбирается что-то другое
                systemSleep(1);
                ap.kpiForecastForm().kpiType(kpi.getName()).click();
                ap.kpiForecastForm().kpiTypeList().click();
            }
        }
        if (kpiList.stream().filter(kpi -> kpi.getName().equals("Трафик")).collect(Collectors.toList()).size() == 0) {
            systemSleep(1);
            ap.kpiForecastForm().kpiType("Трафик").click();
            ap.kpiForecastForm().kpiTypeList().click();
        }
        ap.kpiForecastForm().kpiTypeList().click();
        Allure.addAttachment("Были выбраны Kpi",
                             kpiList.stream().map(KpiList::getName).collect(Collectors.joining(", ")));
    }

    @Step("Закрыть форму FTE")
    private void closeFTEForecastChanges() {
        ap.fteChanges().fteChangesCloseForm().click();
        ap.fteChanges().should("Форма \"Изменения в фактических данных\" не закрылась ",
                               Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Закрыть форму KPI")
    private void closeKPIForecastChanges() {
        ap.kpiForecastChanges().kpiForecastChangesCloseForm().click();
        ap.kpiForecastChanges().should("Форма \"Изменения в прогнозе\" не закрылась ",
                                       Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Выбрать дату {date} для пероида прогноза {kpiRange}")
    private void pickForecastRange(KPICalendarsPlace kpiRange, String date) {
        ap.kpiForecastForm().kpiForecastRangeList().get(kpiRange.ordinal()).click();
        ap.kpiForecastForm().kpiForecastRangeList().get(kpiRange.ordinal()).sendKeys(date);
    }

    @Step("Выбрать дату {date} в форме {listOfDataInputForm}")
    private void pickForecastRangeFTE(ListOfDataInputForm listOfDataInputForm, String date) {
        ap.fteForm().fteEvaluationRangeList(listOfDataInputForm.ordinal()).click();
        ap.fteForm().fteEvaluationRangeList(listOfDataInputForm.ordinal()).clear();
        ap.fteForm().fteEvaluationRangeList(listOfDataInputForm.ordinal()).sendKeys(date);
    }

    /***
     * Данный степ рандомно генерирует число либо меньше 8 знаков , либо больше 8 знаков
     * и отсылает их в даты.
     * 0 в dataForm соответствует дате начала
     * 1 в dataForm соответствует дате конца
     */
    private String generateIncorrectDate() {
        Random rnd = new Random();
        String date;
        boolean b = rnd.nextBoolean();
        if (b) {
            long max = 9999999L;
            date = Long.toString((long) (Math.random() * max));
        } else {
            long min = 100000000L;
            long max = 10000000000000000L;
            date = Long.toString((long) (Math.random() * (max - min))) + min;
        }
        return date;
    }

    private String generateBadDate() {
        int day = (int) (Math.random() * 67) + 32;
        int month = (int) (Math.random() * 86) + 13;
        int year = (int) (Math.random() * 2018);
        return Integer.toString(day) + month + year;
    }

    @Step("Проверка на что в поле : {listOfDataInputForm} введена некорректная дата ")
    private void fteFormDataIncorrectAssert(ListOfDataInputForm listOfDataInputForm, String incorrectDate) {
        ap.fteForm().fteFormDataIncorrect(listOfDataInputForm.ordinal() + 1)
                .waitUntil("Форма выбора даты не отображается", DisplayedMatcher.displayed(), 5);
        ap.fteForm().fteFormDataIncorrect(listOfDataInputForm.ordinal() + 1)
                .should("Индикация некорректной даты не отображается", DisplayedMatcher.displayed(), 5);
        Allure.addAttachment("Дата", "text/plain",
                             "В форму : " + listOfDataInputForm + " была введена некорректная дата : " + incorrectDate);
    }

    @Step("Проверка того что форма даты корректно отображается")
    private void fteFormDataCorrect() {
        ap.fteForm().fteFormDataIncorrectElement()
                .should("Поп-ап с надписью \"Некорректная дата\" отображается",
                        not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Проверка того что поп-ап с надписью \"Некорректная дата\"  отображается")
    private void fteFormDataIncorrectSolo() {
        ap.fteForm().fteFormDataIncorrectElement()
                .should("Поп-ап с надписью \"Некорректная дата\" не отображается",
                        DisplayedMatcher.displayed(), 5);
    }

    @Step("Ввести дату : {stringDate} в поле {listOfDataInputForm}")
    private void sendDataInFTEInput(ListOfDataInputForm listOfDataInputForm, String stringDate) {
        AtlasWebElement dateInputField = ap.fteForm().fteEvaluationRangeList(listOfDataInputForm.ordinal() + 1);
        dateInputField.click();
        dateInputField.clear();
        dateInputField.sendKeys(stringDate);
        ap.fteForm().fteEvaluationRangeList(2).sendKeys(Keys.ENTER);
        Allure.addAttachment("Дата", "text/plain", "Введенная дата : " + stringDate);
    }

    @Step("Кликнуть по значку календаря рядом с полем \"дата начала\"")
    private void clickCalendarStartDateButton(AtlasWebElement element) {
        element.click();
    }

    @Step("Кликнуть на значек календаря и выбрать дату и нажать кнопку \"Отменить\"")
    private void clickCalendarDateButtonAndCancel(LocalDate date, ListOfDataInputForm listOfDataInputForm) {
        DatePicker dp = new DatePicker(ap.datePickerForm());
        ap.fteForm().dateFormes().get(listOfDataInputForm.ordinal()).click();
        dp.pickDate(date);
        dp.cancelButtonClick();
    }

    @Step("Кликнуть на значек календаря и выбрать дату")
    private void clickCalendarDateButtonAndOk(LocalDate date, ListOfDataInputForm listOfDataInputForm) {
        DatePicker dp = new DatePicker(ap.datePickerForm());
        ap.fteForm().dateFormes().get(listOfDataInputForm.ordinal()).click();
        dp.pickDate(date);
        dp.okButtonClick();
    }

    @Step("Кликнуть по значку календаря рядом с полем \"дата окончания\"")
    private void clickCalendarEndDateButton(KPICalendarsPlace kpiCalendarsPlace) {
        ap.kpiForecastForm().calendarsList()
                .get(kpiCalendarsPlace.ordinal()).click();
    }

    @Step("Проверить совпадение введенной даты и появивишейся после нажатия на кнопку \"Ок\"")
    private void dateSelectionCheck(String dateBefore, String dateFromField) {
        Assert.assertEquals(dateBefore, dateFromField, "Дата в поле и выбранная в календаре не совпали");
        Allure.addAttachment("Проверка",
                             "Введенная дата: " + dateBefore + " , дата появившаяся в поле " + dateFromField);
    }

    @Step("Проверить совпадение даты в поле до и после нажатия на кнопку \"Отменить\"")
    private void dateDeselectionCheck(String dateBefore, String dateFromField) {
        Assert.assertEquals(dateBefore, dateFromField, "Дата в поле и выбранная в календаре не совпали");
        Allure.addAttachment("Проверка",
                             "Дата в поле до действий с календарем" + dateBefore
                                     + " , дата появившаяся в поле после его закрытия" + dateFromField);
    }

    @Step("Выбрать {orgUnitImport} для импорта")
    private void selectOrgUnitForImport(OrgUnitImport orgUnitImport) {
        ap.kpiForecastForm().orgUnitImport().click();
        ap.kpiForecastForm().orgUnitImportList().get(orgUnitImport.ordinal()).click();
    }

    @Step("Выбрать алгоритм KPI: {kpiAlgorithm}")
    private void selectForecastAlgorithm(KpiAlgorithm kpiAlgorithm) {
        ap.kpiForecastForm().kpiForecastAlgorithm().click();
        ap.kpiForecastForm().kpiForecastAlgorithmList().get(kpiAlgorithm.ordinal()).click();
    }

    @Step("Выбрать минимальное значение для прогноза: {minKpi}")
    private void pickMinKpi(String minKpi) {
        ap.kpiForecastForm().kpiMin().sendKeys(minKpi);
    }

    @Step("Выбрать максимальное значение для прогноза: {maxKpi}")
    private void pickMaxKpi(String maxKpi) {
        ap.kpiForecastForm().kpiMax().sendKeys(maxKpi);
    }

    @Step("Выбрать тренд прогноза {forecastTrend}")
    private void pickForecastTrend(String forecastTrend) {
        if (Integer.parseInt(forecastTrend) >= 0) {
            ap.kpiForecastForm().kpiTrendField().sendKeys(forecastTrend);
        } else {
            for (int i = 0; i < Integer.parseInt(forecastTrend.replace("-", "")); i++) {
                ap.kpiForecastForm().kpiTrendSlider().sendKeys(Keys.ARROW_DOWN);
            }
        }
    }

    @Step("Нажать кнопку \"Создать\" в форме прогноза")
    private void createForecast() {
        ap.kpiForecastForm().kpiForecastCreate().click();
    }

    @Step("KPI проверка закрытия формы")
    private void closeKpiAssert() {
        ap.kpiForecastForm().should(not(DisplayedMatcher.displayed()));
    }

    @Step("Нажать на кнопку закрытия формы KPI")
    private void closeForecastForm() {
        ap.kpiForecastForm().kpiForecastCloseFrom().click();
    }

    @Step("Нажать на кнопку закрытия формы публикации прогноза")
    private void closeKpiPublishedForm() {
        ap.kpiPublishedForm().kpiPublishedCloseForm().click();
    }

    @Step("Проверка того что форма публикации прогноза закрылась")
    private void kpiPublishedFormIsClosed() {
        ap.kpiPublishedForm().should(not(DisplayedMatcher.displayed()));
    }

    @Step("Выбрать тип KPI: {kpiType}")
    private void selectKpiType(KpiType kpiType) {
        ap.kpiForecastForm().kpiValueButton().click();
        ap.kpiForecastForm().kpiValueType(kpiType.getType()).waitUntil(DisplayedMatcher.displayed());
        //сначала проверяем галочку на ненужном параметре, если что снимаем ее
        KpiType anotherType = getAnotherType(kpiType);
        if (ap.kpiForecastForm().kpiCheckValueType(anotherType.getType()).getAttribute("class").contains("check")) {
            ap.kpiForecastForm().kpiValueType(anotherType.getType()).click();
            ap.kpiForecastForm().kpiValueButton().click();
        }
        //затем проверяем галочку на нужном параметре, если не стоит ставим, если стоит просто закрываем меню выбора
        if (!ap.kpiForecastForm().kpiCheckValueType(kpiType.getType()).getAttribute("class").contains("check")) {
            ap.kpiForecastForm().kpiValueType(kpiType.getType()).click();
        } else {
            ap.kpiForecastForm().kpiValueButton().click();
        }
    }

    @Step("Выбрать месяц для публикации прогноза: {date}")
    private void pickMonthToKpiPublish(LocalDate date) {
        ap.kpiPublishedForm().kpiPublishedMonth().click();
        DatePicker monthPicker = new DatePicker(ap.datePickerForm());
        monthPicker.pickMonth(date);
        monthPicker.okButtonClick();
    }

    @Step("Закрыть форму публикации FTE")
    private void ftePublishedCloseFormClick() {
        ap.ftePublishedForm().ftePublishedCloseForm().click();
    }

    @Step("Проверка того что форма публикации FTE закрыта")
    private void ftePublishedCloseAssert() {
        ap.ftePublishedForm().should("Форма публикации FTE отображается",
                                     not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Выбрать дату {date} для формы прогноза в окне {kpiCalendarsPlace}")
    private void pickMonthToKpiForecast(KPICalendarsPlace kpiCalendarsPlace, LocalDate date) {
        ap.kpiForecastForm().calendarsList().get(kpiCalendarsPlace.ordinal()).click();
        DatePicker datePicker = new DatePicker(ap.datePickerForm());
        datePicker.pickDate(date);
        datePicker.okButtonClick();
    }

    @Step("Ввести дату {date} для прогноза")
    private void enterDateForForecast(int order, DateTypeField dateType, LocalDate date) {
        ap.kpiForecastForm().dateInputFieldForKpiForecast(order, dateType.getName()).click();
        ap.kpiForecastForm().dateInputFieldForKpiForecast(order, dateType.getName()).clear();
        ap.kpiForecastForm().dateInputFieldForKpiForecast(order, dateType.getName()).sendKeys(date.format(UI_DOTS.getFormat()));
    }

    @Step("Очистить поле даты для формы прогноза в окне {kpiCalendarsPlace}")
    private void clearMonthToKpiForecast(KPICalendarsPlace kpiCalendarsPlace) {
        ap.kpiForecastForm().kpiForecastRangeList().get(kpiCalendarsPlace.ordinal()).click();
        ap.kpiForecastForm().kpiForecastRangeList().get(kpiCalendarsPlace.ordinal()).clear();
    }

    @Step("Нажать кнопку публкации прогноза")
    private void publishKpi() {
        ap.kpiPublishedForm().kpiPublishedSubmit().click();
    }

    @Step("Нажать кнопку публкации FTE")
    private void publishFTE() {
        ap.ftePublishedForm().ftePublish().click();
    }

    @Step("Нажать на кнопку рассчета FTE")
    private void evaluationFTE() {
        ap.fteForm().fteEvaluation().click();
    }

    @Step("Нажать на ссылку данных FTE")
    private void fteDataHref() {
        ap.fteForm().fteDataHref().click();
    }

    @Step("Проверить, что ссылка сформированная нами совпала с полученной в ходе действий юзера")
    private void assertForCorrectLink(FileDownloadCheckerForReport checker, String oldTabHandler) {
        CustomTools.removeFirstWindowHandler(ap);
        String urlAddress = ap.getWrappedDriver().getCurrentUrl();
        URI checkerUri;
        try {
            checkerUri = checker.downloadUrlFormer().build();
        } catch (URISyntaxException e) {
            throw new AssertionError("В билдере забилдилось что то неправильное");
        }
        Assert.assertEquals(urlAddress, RELEASE_URL + checkerUri.toString(),
                            "Полученная ссылка из браузера не совпала с ожидаемой");
    }

    @Step("Нажать на кнопку закрытия формы FTE")
    private void fteCloseForm() {
        ap.fteForm().fteCloseForm().click();
    }

    @Step("Проверка того что форма FTE закрыта")
    private void fteCloseFormAssert() {
        ap.fteForm().should("Форма FTE отображается", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Кликнуть на чекбокс \"Перерасчитать плановую численость\" в форме FTE")
    private void fteReEvalFlag() {
        ap.fteForm().fteReEvalFlag().click();
    }

    @Step("Нажать на кнопку выбора импорта подразделения в форме FTE")
    private void fteOrgUnitImportClick() {
        ap.fteForm().fteOrgUnitImport().click();
    }

    @Step("Нажать на поле выбора стратегии FTE")
    private void fteStrategyClick() {
        ap.fteForm().fteStrategy().click();
    }

    @Step("Нажать на поле выбора метода")
    private void methodButtonClick() {
        ap.fteForm().methodButton().click();
    }

    @Step("Нажать на поле выбора алгоритма FTE")
    private void fteAlgorithmClick() {
        ap.fteForm().fteAlgorithm().click();
    }

    @Step("Кликнуть по полю \"Алгоритм\" и выбрать позицию {algorithm}")
    private void fteAlgorithmList(AlgorithmList algorithm) {
        ap.fteForm().fteAlgorithmList(algorithm.getAlgorithm()).click();
    }

    @Step("Выбрать метод в форме FTE : {fteMethodTypes}")
    private void fteMethodsChoose(FteMethodTypes fteMethodTypes) {
        ap.fteForm().fteMethod(String.valueOf(fteMethodTypes.ordinal() + 1)).click();
    }

    @Step("Выбрать стратегию из листа в форме FTE: {strategy}")
    private void fteStrategyListChose(StrategyList strategy) {
        String needyStrategyName = strategy.getStrategy();
        ap.fteForm().fteStrategyList(needyStrategyName).click();
    }

    @Step("Вернуть график на текущую дату")
    private void diagramSlideReset() {
        ap.diagramSwitcher().resetButton().click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    private int getRandomMonthNumber() {
        return 1 + new Random().nextInt(12);
    }

    @Step("Выбрать месяц : {month} на диаграмме справа")
    private void selectRandomMonthOnTheRightChart(Month month) {
        final int montValue = month.getValue();
        ap.diagramChart().dateForDiagramForm(montValue)
                .waitUntil("Месяц " + month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru")) + " не отображен на диаграмме",
                           DisplayedMatcher.displayed(), 5);
        ap.diagramChart().dateForDiagramForm(montValue).click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Выбрать месяц : {month} на диаграмме слева")
    private void selectRandomMonthOnTheLeftChart(Month month) {
        ap.diagramChartLeft().dateForDiagramForm(month.getValue()).click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Выбрать {daysRandom} день месяца на правой диаграмме")
    private void selectRandomDayOnTheRightChart(int daysRandom) {
        ap.diagramChart().dateForDiagramForm(daysRandom)
                .waitUntil("Выбранный день " + daysRandom + " на диаграмме не отображается",
                           DisplayedMatcher.displayed(), 5);
        ap.diagramChart().dateForDiagramForm(daysRandom).click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Выбрать {daysRandom} день месяца на левой диаграмме")
    private void selectRandomDayOnTheLeftChart(int daysRandom) {
        ap.diagramChartLeft().dateForDiagramForm(daysRandom)
                .waitUntil("Выбранный день " + daysRandom + " на диаграмме не отображается",
                           DisplayedMatcher.displayed(), 5);
        ap.diagramChartLeft().dateForDiagramForm(daysRandom).click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Проверить, что была выбрана дата: {randomDate}")
    private void assertForChoosingDay(LocalDate randomDate) {
        String dateOnUI = ap.diagramChart().currentChartDate().getText();
        String date = getDateDisplay(randomDate).toLowerCase();
        Assert.assertEquals(dateOnUI, date, "Отобразилась не та дата");
    }

    @Step("Проверить, что форма редактирования открылась для {randomDate}")
    private void displayInscriptionAboveScheduleDay(LocalDate randomDate) {
        String date = getDateDisplay(randomDate);
        String dateOnUI = ap.diagramChart().currentChartDate().getText();
        Assert.assertEquals(date, dateOnUI, "Неправильный текущий месяц или не совпадет с выбранным");
        Allure.addAttachment("Сравнение", "text/plain",
                             "Ожидаемая дата: " + date + "\n\n" + "Дата на UI : " + dateOnUI);
    }

    //dd MMM., WW
    private String getDateDisplay(LocalDate date) {
        return date.getDayOfMonth() + " "
                + MonthsEnum.values()[date.getMonthValue() - 1].getDeclensionName().toLowerCase() + ", "
                + date.getDayOfWeek().getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru"));
    }

    @Step("Проверка того что отображается информация: {variant}")
    private void assertForInformationVariants(String variant) {
        ap.diagramChart().variantsOfInformation()
                .should("Вариант информации не отображается на диаграмме", text(containsString(variant)), 10);
        Allure.addAttachment("Проверка", "Индикатор с именем: " + "\"" + variant + "\"" + " был успешно активирован.");
    }

    @Step("Проверка наведения курсора на произвольную строку списка")
    private void assertForCheckElementFTE() {
        ap.fteChanges()
                .waitUntil("Изменения в фактических данных не загрузились",
                           DisplayedMatcher.displayed(), 5);
        Random rnd = new Random();
        int num = rnd.nextInt(ap.fteChanges().commentsColumnValue().size());
        new Actions(ap.getWrappedDriver()).moveToElement(ap.fteChanges().commentsColumnValue().get(num)).perform();
        ap.fteChanges().commentsColumnValue().get(num).isSelected();
        String hoveringComment = ap.fteChanges().commentsColumnValue().get(num).getText();
        Allure.addAttachment("Комментарий", "text/plain",
                             "Комментарий изменения , на который навелись : " + hoveringComment);
    }

    @Step("Проверка наведения курсора на произвольную строку списка")
    private void assertForCheckElementKPI() {
        ap.kpiForecastChanges()
                .waitUntil("Изменения в фактических данных не загрузились",
                           DisplayedMatcher.displayed(), 5);
        Random rnd = new Random();
        int num = rnd.nextInt(ap.kpiForecastChanges().checkingElements().size());
        new Actions(ap.getWrappedDriver()).moveToElement(ap.kpiForecastChanges().checkingElements().get(num)).perform();
        ap.kpiForecastChanges().checkingElements().get(num).isSelected();
        String hoveringComment = ap.kpiForecastChanges().checkingElements().get(num).getText();
        Allure.addAttachment("Комментарий", "text/plain",
                             "Комментарий изменения , на который навелись : " + hoveringComment);
    }

    @Step("Проверка на то , что масштаб изменился на год")
    private void assertForSwitchYear() {
        Date dateNow = new Date();
        SimpleDateFormat formatYear = new SimpleDateFormat("yyyy");
        String currentDateInfo = ap.diagramChart().currentChartDate().getText();
        Allure.addAttachment("Value", "text/plain",
                             "На диаграмме при измененнии масштаба на месяц отобразилось : " + currentDateInfo);
        Assert.assertEquals(formatYear.format(dateNow), currentDateInfo, "Года не совпали при изменении масштаба графика");
    }

    @Step("Проверка на то, что масштаб изменился на месяц")
    private void assertForSwitchMonth() {
        Date dateNow = new Date();
        Locale locale = new Locale("ru");
        SimpleDateFormat formatMonth = new SimpleDateFormat("MMMM", locale);
        SimpleDateFormat formatYear = new SimpleDateFormat("yyyy", locale);
        String currentDateOnUI = ap.diagramChart().currentChartDate().getText().toLowerCase();
        String currentMonth = formatMonth.format(dateNow).toLowerCase();
        String currentYear = formatYear.format(dateNow).toLowerCase();
        String currentDate = currentMonth + " " + currentYear;
        Allure.addAttachment("Value", "text/plain",
                             "На диаграмме при измененнии масштаба на месяц отобразилось : " + currentDateOnUI);
        Assert.assertEquals(currentDateOnUI, currentDate, "Даты не совпали при изменении масштаба графика");
    }

    @Step("Проверка на то , что масштаб изменился на день")
    private void assertForSwitchDay() {
        Date dateNow = new Date();
        SimpleDateFormat formatMonth = new SimpleDateFormat("MMM,");
        SimpleDateFormat formatNumDay = new SimpleDateFormat("d");
        SimpleDateFormat formatWeekDay = new SimpleDateFormat("EEEE");
        String currentDateInfo = ap.diagramChart().currentChartDate().getText();
        String month = formatMonth.format(dateNow).toLowerCase();
        String numDay = formatNumDay.format(dateNow).toLowerCase();
        String weekDay = formatWeekDay.format(dateNow).toLowerCase();
        String date;
        switch (month) {
            case ("март,"):
                month = "мар.,";
                date = numDay + " " + month + " " + weekDay;
                break;
            case ("май,"):
                month = "мая,";
                date = numDay + " " + month + " " + weekDay;
                break;
            case ("июнь,"):
                month = "июня,";
                date = numDay + " " + month + " " + weekDay;
                break;
            case ("июль,"):
                month = "июля,";
                date = numDay + " " + month + " " + weekDay;
                break;
            default:
                date = numDay + " " + month + " " + weekDay;
        }
        Allure.addAttachment("Value", "text/plain",
                             "На диаграмме при измененнии масштаба на день отобразилось : " + currentDateInfo
                                     + "\\n" + "Ожидалось , что отобразится : " + date);
        Assert.assertEquals(currentDateInfo, date, "Даты не совпали при изменении масштаба графика");
    }

    @Step("Отмена коррекции")
    private void cancelCorrection() {
        ap.correctionSlider().cancelButton().click();
    }

    @Step("Нажать на кнопку \"Сохранить\"")
    private void sliderOkButtonClick() {
        ap.correctionSlider().okButton().click();
        waitForClickable(ap.diagramSwitcher().leftGraph(), ap, 25);
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 180);
    }

    @Step("Проверка на то, что возникает pop-up возле колонки {direction} диаграммы при наведении")
    private void findingValueName(Direction direction) {
        ap.diagramChart().dateForDiagramForm(1).waitUntil("Страница не загрузилась",
                                                          DisplayedMatcher.displayed(), 5);
        String text = ap.analyticsPageForm().textOfNameParameter().getText();
        Pattern pattern = Pattern.compile("Знач.+(\\s)+([0-9]*)");
        Matcher matcher = pattern.matcher(text);
        boolean columnIsEmpty;
        if (direction == Direction.RIGHT) {
            columnIsEmpty = ap.rightGraphicDiagramForm().listColumnRightGraphicDiagram().isEmpty();
        } else {
            columnIsEmpty = ap.leftGraphicDiagramForm().listColumnLeftGraphicDiagram().isEmpty();
        }
        if (!columnIsEmpty) {
            if (!matcher.matches()) {
                Assert.fail("Значение не удовлетворяет регулярному выражению");
            }
        } else {
            Assert.fail("Нет колонок");
        }
        Allure.addAttachment("Значение", "text/plain",
                             "Значение pop-up сообщения возле колонки при наведении : " + text);
    }

    @Step("Проверка на то, что высветилась левая диаграмма")
    private void checkDisplayedLeftGraphic() {
        ap.leftGraphicDiagramForm().should("Compare mode not activated",
                                           DisplayedMatcher.displayed(), 5);
    }

    @Step("Проверка на то, что скрылась левая диаграмма")
    private void checkNotDisplayedOneLeftGraphic() {
        ap.leftGraphicDiagramForm().should("Compare mode not deactivated",
                                           Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Проверка на то, что при нажатии на колонку графика открывается окно редактирования")
    private void displayDataEditingWindow() {
        ap.correctionTable().should("Не было нажатия на колонку", DisplayedMatcher.displayed(), 5);
    }

    @Step("Проверка того, что год отображается раньше текущего")
    private void displayTheYearBeforeTheCurrentOne() {
        int year = LocalDate.now().getYear();
        int yearUI = Integer.parseInt(ap.diagramChart().currentChartDate().getText());
        Assert.assertEquals(year, yearUI + 1, "Год не на один раньше , чем текущий");
        Allure.addAttachment("Сравнение", "text/plain",
                             "Текущий год: " + year + "\n\n"
                                     + "Переключение года на один раньше. Год, отображаемый в форме редактирования : "
                                     + yearUI);
    }

    @Step("Проверка, что отобразился {year} год")
    private void displayTheCurrentYear(int year) {
        String elementText = ap.diagramChart().currentChartDate().getText();
        int yearUI = Integer.parseInt(elementText);
        Assert.assertEquals(yearUI, year, "Отображаемый год не равен текущему");
        Allure.addAttachment("Сравнение", "text/plain",
                             "Текущий год : " + year + "\n\n"
                                     + "Год на UI : "
                                     + elementText);
    }

    private LocalDate getRandomDate() {
        LocalDate dateNow = LocalDate.now();
        return dateNow.plusDays(new Random().nextInt(300) + 31).withYear(dateNow.getYear());
    }

    @Step("Проверка даты над графиком")
    private void displayInscriptionAboveScheduleMonth(LocalDate localDate) {
        int currentYear = localDate.getYear();
        int currentMonth = localDate.getMonthValue();
        LocalDate configDate = LocalDate.of(currentYear, currentMonth, 2);
        Locale locale = new Locale("ru");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLLL", locale);
        String month = configDate.format(formatter);
        String date = month + " " + currentYear;
        String dateOnUI = ap.diagramChart().currentChartDate().getText();
        Assert.assertEquals(dateOnUI, date, "Неправильный текущий месяц или не совпадет с выбранным");
        Allure.addAttachment("Сравнение", "text/plain",
                             "Ожидаемая дата: " + date + "\n\n" + "Дата на UI : " + dateOnUI);
    }

    @Step("Проверка на то, что форма редактирования открылась для месяца {localDate}")
    private void displayWordsSelectedMonthAboveSchedule(LocalDate localDate) {
        String date = localDate.format(DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru")));
        String dateOnUI = ap.diagramChart().currentChartDate().getText();
        Assert.assertEquals(date, dateOnUI, "Неправильный текущий месяц или не совпадет с выбранным");
        Allure.addAttachment("Сравнение", "text/plain",
                             "Ожидаемая дата: " + date + "\n\n" + "Дата на UI : " + dateOnUI);
    }

    @Step("Проверить, что дата на левом графике {dateLeft}, на правом {dateRight}")
    private void checkDatesMatches(ChronoUnit unit, LocalDate dateLeft, LocalDate dateRight) {
        String expectedDateLeft = getFormattedDate(unit, dateLeft);
        String expectedDateRight = getFormattedDate(unit, dateRight);
        ap.diagramChart().currentChartDate().should("Дата на правом графике не совпала с ожидаемой", text(containsString(expectedDateRight)), 1);
        ap.diagramChartLeft().currentStateValue().should("Дата на левом графике не совпала с ожидаемой", text(containsString(expectedDateLeft)), 1);
    }

    private int getRandomChronoValue() {
        return new Random().nextInt(5) + 1;
    }

    /**
     * Метод переключает влево/вправо левую/правую диаграмму и проверяет, чтобы даты над диаграммами
     * соответствовали ожидаемым
     */
    private void switchToRandomDate(ChronoUnit chronoUnit, int valueToSwitch, LocalDate current,
                                    boolean rightGraph, boolean rightSwitch) {
        AtlasWebElement webElementToClick = rightSwitch ? ap.diagramSwitcher().rightGraph()
                : ap.diagramSwitcher().leftGraph();
        String dontChangeDate = !rightGraph ? ap.diagramChart().currentChartDate().getText()
                : ap.diagramChartLeft().currentStateValue().getText();
        for (int i = 0; i < valueToSwitch; i++) {
            webElementToClick.click();
        }
        ap.rightGraphicDiagramForm().waitUntil("Страница не загрузилась", DisplayedMatcher.displayed(), 30);
        int summaryValue = rightSwitch ? valueToSwitch : -valueToSwitch;
        String result = getFormattedDate(chronoUnit, current.plus(summaryValue, chronoUnit));
        if (rightGraph) {
            ap.diagramChartLeft().currentStateValue().should("Дата на левом графике изменилась",
                                                             text(containsString(dontChangeDate)), 1);
            ap.diagramChart().currentChartDate().should("Дата на правом графике не совпала с ожидаемой",
                                                        text(containsString(result)), 2);
        } else {
            ap.diagramChart().currentChartDate().should("Дата на правом графике изменилась",
                                                        text(containsString(dontChangeDate)), 1);
            ap.diagramChartLeft().currentStateValue().should("Дата на левом графике не совпала с ожидаемой",
                                                             text(containsString(result)), 2);
        }
    }

    @Step("Нажать {valueToSwitch} раз на кнопку \">\".")
    private void switchRightToRandomDate(ChronoUnit chronoUnit, int valueToSwitch, LocalDate current, boolean rightGraph) {
        switchToRandomDate(chronoUnit, valueToSwitch, current, rightGraph, true);
    }

    @Step("Нажать {valueToSwitch} раз на кнопку \"<\".")
    private void switchLeftToRandomDate(ChronoUnit chronoUnit, int valueToSwitch, LocalDate current, boolean rightGraph) {
        switchToRandomDate(chronoUnit, valueToSwitch, current, rightGraph, false);
    }

    @Step("Кнопка переключения с правого графика на левый")
    private void leftChartButtonClick() {
        ap.diagramSwitcher().selectLeftChartButton().click();
    }

    @Step("Проверка того что число коррекции не изменяется при попытке ввода в окно коррекии")
    private void assertCorrectionFieldNotChange() {
        String send = "0," + (Math.random() * 9) + 1;
        ap.correctionSlider().fieldCoefficientCorrection().sendKeys(send);
        String visibleValue = ap.correctionSlider().fieldCoefficientCorrection().getAttribute("value").trim();
        Allure.addAttachment("Ввод в окно коррекции", "В окно коррекции было введено значение: " + send
                + ". В окне коррекции отображается: " + visibleValue);
        Assert.assertEquals(visibleValue, "0",
                            "Введенное число отображается в поле коэффициента коррекции");
    }

    @Step("Производится клик на случайную колонну левого графика указанного цвета")
    private void clickOnTheRandomRightColumns() {
        CheckScopeColumns checkScopeColumns = new CheckScopeColumns(ap, Direction.RIGHT);
        Map<String, AtlasWebElement> show = checkScopeColumns
                .certainColorBarReturner(ColorsColumns.PURPLE);
        AtlasWebElement column = show.values().stream().filter(Objects::nonNull)
                .findAny().orElseThrow(() -> new AssertException("Элементов такого цвета нет на графике"));
        waitForClickable(column, ap, 25);
        column.click();
        ap.correctionTable().waitUntil("Форма коррекции все еще не отобразилась", DisplayedMatcher.displayed(), 5);
    }

    private Map<String, Map<ColorsColumns, AtlasWebElement>> getRandomMap(CheckScopeColumns checkScopeColumns) {
        Map<String, Map<ColorsColumns, AtlasWebElement>> allBars = checkScopeColumns.allBarsReturner();
        checkScopeColumns.clearColumnMap(allBars);
        //случайный выбор месяца , в котором есть не нулевые веб элементы
        List<String> dates;
        String randomDates;
        Map<ColorsColumns, AtlasWebElement> bothColumns;
        dates = new ArrayList<>(allBars.keySet());
        randomDates = getRandomFromList(dates);
        bothColumns = allBars.get(randomDates);
        while (bothColumns.values().stream().noneMatch(Objects::nonNull)) {
            allBars.remove(randomDates);
            dates = new ArrayList<>(allBars.keySet());
            randomDates = getRandomFromList(dates);
            bothColumns = allBars.get(randomDates);
        }
        //создание map из месяца , цвета колонки и ей соответствующего веб элемента
        List<ColorsColumns> listColumns = new ArrayList<>(bothColumns.keySet());
        ColorsColumns randOneColorColumn = getRandomFromList(listColumns);
        AtlasWebElement webElementOfThisColumn = bothColumns.get(randOneColorColumn);
        Map<String, Map<ColorsColumns, AtlasWebElement>> tempMap = new HashMap<>();
        Map<ColorsColumns, AtlasWebElement> colorAndWebElement = new HashMap<>();
        colorAndWebElement.put(randOneColorColumn, webElementOfThisColumn);
        tempMap.put(randomDates, colorAndWebElement);
        return tempMap;
    }

    @Step("Нажать или навести на случайно выбранную колонку на {direction} диаграмме")
    private void clickOrHoverOnRandomColumnDiagram(Direction direction, boolean clickNotHover) {
        CheckScopeColumns checkScopeColumns = new CheckScopeColumns(ap, direction);
        Map<String, Map<ColorsColumns, AtlasWebElement>> map = getRandomMap(checkScopeColumns);
        String whatToDo;
        if (clickNotHover) {
            whatToDo = "нажатие";
            map.values().forEach(value -> value.values().forEach(AtlasWebElement::click));
        } else {
            whatToDo = "наведение";
            //map.values().forEach(value -> value.values().forEach(AtlasWebElement::hover));
        }
        String date = map.keySet().stream().findFirst().orElse(null);
        Map<ColorsColumns, AtlasWebElement> colorAndWeb = map.values().stream().findFirst().orElse(null);
        assert colorAndWeb != null;
        String color = colorAndWeb.keySet().stream().findFirst().toString();
        Allure.addAttachment(whatToDo,
                             "Было осуществлено " + whatToDo + " на столбик цвета " + color + " над " + date + " на диаграмме");
    }

    @Step("Клик по иконке 'Урна'")
    private void clickOnTheUrnIcon() {
        waitForClickable(ap.correctionTable().deleteCorrectionButton(), ap, 25);
        ap.correctionTable().deleteCorrectionButton().click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Ввод произвольного комментария в диалоговом окне")
    private void enterAnyComment(String anyComment) {
        ap.saveCommentForCorrectionForm().waitUntil("Не была нажата кнопка 'сохранить'", DisplayedMatcher.displayed(), 10);
        ap.saveCommentForCorrectionForm().inputComment().sendKeys(anyComment);
    }

    @Step("Кнопка сохранения комментария в диалоговом окне")
    private void clickButtonSaveForDialogForm() {
        ap.saveCommentForCorrectionForm().saveButton().click();
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Проверить коррекцию исторических данных KPI.")
    private void assertForHistoricalCorrections(Kpi kpiBefore, KpiRepository kpiRepository, String enteredValue) {
        assertKpiChanges(kpiBefore, kpiRepository, enteredValue, true);
    }

    @Step("Проверить отмену коррекции исторических данных KPI.")
    private void assertForHistoricalCancelChanges(Kpi kpiBefore, KpiRepository kpiRepository) {
        assertKpiChanges(kpiBefore, kpiRepository, "", false);
    }

    @Step("Проверить коррекцию прогноза KPI.")
    private void assertForForecastCorrections(Kpi kpiBefore, KpiRepository kpiRepository) {
        assertKpiChanges(kpiBefore, kpiRepository, "", true);
    }

    @Step("Проверить отмену коррекции исторических данных KPI.")
    private void assertForForecastCancelChanges(Kpi kpiBefore, KpiRepository kpiRepository) {
        assertKpiChanges(kpiBefore, kpiRepository, "", false);
    }

    @Step("Проверить коррекцию исторических данных KPI на масштабе дня")
    private void assertForHistoricalCorrectionOnTime(Kpi kpiBefore, KpiRepository kpiRepository, String enteredValue) {
        assertKpiTimeChanges(kpiBefore, kpiRepository, enteredValue, true);
    }

    @Step("Проверить отмену коррекции исторических данных KPI на масштабе дня")
    private void assertForHistoricalCancelChangesOnTime(Kpi kpiBefore, KpiRepository kpiRepository) {
        assertKpiTimeChanges(kpiBefore, kpiRepository, "", false);
    }

    @Step("Проверить коррекцию прогноза KPI на масштабе дня")
    private void assertForForecastCorrectionOnTime(Kpi kpiBefore, KpiRepository kpiRepository, String enteredValue) {
        assertKpiTimeChanges(kpiBefore, kpiRepository, enteredValue, true);
    }

    @Step("Проверить отмену коррекции прогноза KPI на масштабе дня")
    private void assertForForecastCancelChangesOnTime(Kpi kpiBefore, KpiRepository kpiRepository) {
        assertKpiTimeChanges(kpiBefore, kpiRepository, "", false);
    }

    private void assertKpiChanges(Kpi kpiBefore, KpiRepository kpiRepository, String enteredValue, boolean wasChanged) {
        waitForClickable(ap.diagramSwitcher().leftGraph(), ap, 25);
        int scopeIndex;
        LocalDate date = kpiBefore.getDateTime().toLocalDate();
        double adjustmentsColumn;
        double correctionColumn;
        Kpi kpiAfter;
        if (kpiRepository.getDateUnit() == DateUnit.DAY) {
            scopeIndex = date.getDayOfMonth() - 1;
        } else {
            scopeIndex = date.getMonthValue() - 1;
        }
        if (kpiRepository.isHistorical()) {
            correctionColumn = Double.parseDouble(ap.correctionTable()
                                                          .getColumnValues(Column.CORR_3.getColumnNumber()).get(scopeIndex).getText());
            adjustmentsColumn = Double.parseDouble(ap.correctionTable()
                                                           .getColumnValues(Column.DIAGNOSTICS.getColumnNumber()).get(scopeIndex).getText());
            kpiAfter = kpiRepository.getHistoricalValue(date);
        } else {
            correctionColumn = Double.parseDouble(ap.correctionTable()
                                                          .getColumnValues(Column.CORR_5.getColumnNumber()).get(scopeIndex).getText());
            adjustmentsColumn = Double.parseDouble(ap.correctionTable()
                                                           .getColumnValues(Column.PROGNOSIS.getColumnNumber()).get(scopeIndex).getText());
            kpiAfter = kpiRepository.getForecastValue(date);
        }
        SoftAssert softAssert = new SoftAssert();
        Allure.addAttachment("Проверка", "Параметры " + kpiRepository.getAllureString() + " KPI в API до: "
                + kpiBefore.getValuesToString()
                + "Параметры после: " + kpiAfter.getValuesToString());
        softAssert.assertEquals(kpiAfter.getValue(), adjustmentsColumn,
                                "Значение " + kpiRepository.getAllureString() + " в таблице не совпадает со значением из API");
        softAssert.assertEquals(kpiAfter.getDelta(), correctionColumn,
                                "Значение корректировки в таблице не совпадает со значением из API");
        if (wasChanged && kpiRepository.isHistorical()) {
            softAssert.assertTrue(kpiAfter.getDelta() >= Double.parseDouble(enteredValue) - 20 || kpiAfter.getDelta() <= Double.parseDouble(enteredValue) + 20,
                                  "Введенное значение корректировки не совпало с отображаемым в таблице");
        }
        softAssert.assertAll();
    }

    private void assertKpiTimeChanges(Kpi kpiBefore, KpiRepository kpiRepository, String enteredValue, boolean wasChanged) {
        waitForClickable(ap.diagramSwitcher().leftGraph(), ap, 25);
        String time = kpiBefore.getDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        double correctionColumn;
        double adjustmentsColumn;
        Kpi kpiAfter;
        if (kpiRepository.isHistorical()) {
            correctionColumn = Double.parseDouble(ap.correctionTable()
                                                          .historyCorrectionsOnTimeUnit(Column.CORR_3.getColumnNumber(), time).getText());
            adjustmentsColumn = Double.parseDouble(ap.correctionTable()
                                                           .historyCorrectionsOnTimeUnit(Column.DIAGNOSTICS.getColumnNumber(), time).getText());
            kpiAfter = kpiRepository.getHistoricalValue(kpiBefore.getDateTime());
        } else {
            correctionColumn = Double.parseDouble(ap.correctionTable()
                                                          .forecastCorrectionsOnTimeUnit(time).getText());
            adjustmentsColumn = Double.parseDouble(ap.correctionTable()
                                                           .forecastOnTimeUnit(time).getText());
            kpiAfter = kpiRepository.getForecastValue(kpiBefore.getDateTime());
        }
        SoftAssert softAssert = new SoftAssert();
        Allure.addAttachment("Проверка", "Параметры" + kpiRepository.getAllureString() + " KPI в API до: "
                + kpiBefore.getValuesToString()
                + "Параметры после: " + kpiAfter.getValuesToString());
        softAssert.assertEquals(kpiAfter.getValue(), adjustmentsColumn,
                                "Значение " + kpiRepository.getAllureString() + " в таблице не совпадает со значением из API");
        softAssert.assertEquals(kpiAfter.getDelta(), correctionColumn,
                                "Значение корректировки в таблице не совпадает со значением из API");
        if (wasChanged) {
            softAssert.assertEquals(kpiAfter.getDelta(), Double.parseDouble(enteredValue),
                                    "Введенное значение корректировки не совпало с отображаемым в таблице");
        }
        softAssert.assertAll();
    }

    @Step("Нажать на кнопку крест, закрытия формы")
    private void crossButtonClick() {
        ap.correctionTable().closeCorrectionTable().click();
    }

    @Step("Проверка того что окно коррекции было закрыто")
    private void assertDisplayedDataEditingWindow() {
        Assert.assertTrue(ap.correctionTable().getAttribute("class").contains("hide"),
                          "Окно коррекции не закрылось");
    }

    @Step("Проверка того что поле коррекции подсвечивается красным")
    private void correctionFieldIsRed() {
        final String attribute = ap.correctionSlider().fieldForCorrectionIndicateRed().getAttribute("class");
        if (!attribute.contains("is-invalid")) {
            Assert.fail("Поле коррекции не подсвечивается красным");
        }
    }

    @Step("Проверка того что поле коррекции пустое")
    private void correctionFieldIsEmpty() {
        final String sliderAttribute = ap.correctionSlider().fieldForCorrectionIndicateRed().getAttribute("class");
        if (sliderAttribute.contains("is-invalid")) {
            Assert.fail("Введен неправильный символ");
        } else {
            final String correctionAttribute = ap.correctionSlider().fieldForCorrection().getAttribute("value");
            if (!correctionAttribute.isEmpty()) {
                Assert.fail("Поле коррекции не пустое");
            }
        }
    }

    /**
     * Метод рандомно генерирует строку символов по заданным параметрам
     *
     * @param numbersSymbolsNotText Указывается набор генерации символов
     *                              true - набор генерирует символы, которые относятся к числовым, но не являются числами
     *                              false - набор генерирует текстовые символы
     * @param length                Указывается длина строки которая должна быть сгенерирована при помощи набора символов
     */
    private String generateSymbols(boolean numbersSymbolsNotText, int length) {
        String symbolSet;
        if (numbersSymbolsNotText) {
            symbolSet = "+-.,Ee";
        } else {
            symbolSet = "abcdfghijklmnopqrstuvwxyzABCDFGHIJKLMNOPQRSTUVWXYZ!@\"#№$;%:^&?*()_=~`{}[]|/'";
        }
        char[] symbol = new char[length];
        Random rnd = new Random();
        for (int i = 0; i < length; i++) {
            symbol[i] = symbolSet.charAt(rnd.nextInt(symbolSet.length()));
        }
        return new String(symbol);
    }

    @Step("Изменить тип распределения")
    private void changeDistribution(String typeDistribution) {
        int type = 0;
        if (typeDistribution.equals("С предыдущего месяца")) {
            type = 1;
        }
        ap.correctionSlider().fieldDistribution().click();
        ap.correctionSlider().variantsForDistribution().get(type).click();
    }

    @Step("Ввести число коррекции в поле \"Коррекция\" : {text}")
    private void sendValueForCorrection(String text) {
        ap.correctionSlider().fieldForCorrection()
                .waitUntil("Форма коррекции не отобразилась", DisplayedMatcher.displayed(), 5);
        ap.correctionSlider().fieldForCorrection().clear();
        //добавлено, потому что очистка селениумом оставляет окошко красным, а вручную все ок
        ap.correctionSlider().fieldForCorrection().sendKeys("0");
        ap.correctionSlider().fieldForCorrection().clear();
        ap.correctionSlider().fieldForCorrection().sendKeys(text);
        ap.correctionSlider().fieldForCorrection().sendKeys(Keys.ENTER);
    }

    @Step("Проверить, что у роли нет вариантов выбора меню")
    private void checkUiVariantsMatches(List<TypeOfChartMenu> unexpected) {
        List<String> uiList = ap.editFrom().listOfChartMenu().stream().map(WebElement::getText).collect(Collectors.toList());
        Allure.addAttachment("Отобразились следующие пункты меню:", uiList.toString()
                .replaceAll("\\[, ]", "").replaceAll(", ", "\n"));
        List<String> notVisibleVariants = unexpected.stream().map(TypeOfChartMenu::getName).collect(Collectors.toList());
        Assert.assertTrue(Collections.disjoint(uiList, notVisibleVariants), "Имеются лишние пункты меню:" +
                notVisibleVariants.stream().filter(uiList::contains).collect(Collectors.toList()));
    }

    @Step("Проверка того что в столбце \"Корр\" отображается значение {expected}")
    private void assertEnteredValueIsDisplayed(int cell, String expected) {
        ap.correctionTable().getColumnValues(Column.CORR_3.getColumnNumber()).get(cell)
                .should("Введденое ранее значение не совпадает с предпологаемым", text(containsString(expected)), 15);
    }

    @Step("Проверка того что в столбце \"Корр\" отображается значение {expected}")
    private void assertEnteredValueToTimeIsDisplayed(LocalTime time, String expected) {
        ap.correctionTable().historyCorrectionsOnTimeUnit(Column.CORR_3.getColumnNumber(), time.toString())
                .should("Введденое ранее значение не совпадает с предпологаемым", text(containsString(expected)), 15);
    }

    @Step("Проверка изменений при сокрытии столбцов")
    private void assertHiddenColumn(int cell, boolean clickFirstTime) {
        String scissorStatus = ap.correctionTable().diagnosticsScissorsColumn().get(cell).getAttribute("class");
        String columnStatus = ap.rightGraphicDiagramForm().allFirstColumn().get(cell).getAttribute("class");
        SoftAssert softAssert = new SoftAssert();
        if (clickFirstTime) {
            softAssert.assertFalse(scissorStatus.contains("grey"), "Значок ножниц не поменялся на более яркий");
            softAssert.assertTrue(columnStatus.contains("__grey"), "Столбец не стал серым");
        } else {
            softAssert.assertTrue(scissorStatus.contains("grey"), "Значок ножниц не поменялся на серый");
            softAssert.assertFalse(columnStatus.contains("__grey"), "Столбец не вернул свой цвет");
        }
        softAssert.assertAll();
    }

    @Step("Нажать на значок ножниц")
    private void clickScissorsCell(int cell) {
        ap.correctionTable().diagnosticsScissorsColumn()
                .waitUntil(Matchers.hasSize(Matchers.greaterThan(10))).get(cell).click();
        String dateValue = ap.correctionTable().getColumnValues(Column.DATE.getColumnNumber()).get(cell).getText();
        Allure.addAttachment("Выбор даты",
                             "Было выбрано нажать на значок ножниц напротив значения за " + dateValue);
    }

    @Step("Кликнуть на месяц с названием {month} в масштабе год")
    private void clickOnMonthSignature(Month month) {
        WebElement monthElement = ap.diagramChart().indexGraphYearOrMonth().get(month.getValue() - 1);
        waitForClickable(monthElement, ap, 25);
        monthElement.click();
        LOG.info("Кликнули на подпись столбца: {}",
                 month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru")));
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Кликнуть на {day} месяца под графиком")
    private void clickOnDaySignature(int day) {
        WebElement monthElement = ap.diagramChart().indexGraphYearOrMonth().get(day - 1);
        waitForClickable(monthElement, ap, 25);
        monthElement.click();
        LOG.info("Кликнули на {} столбец дня", day);
        ap.spinnerLoader().grayLoadingBackground()
                .waitUntil("График не загрузился", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    /**
     * Создает число в указанном диапазоне
     *
     * @param min           - минимальное значение
     * @param max           - максимальное значение
     * @param negativeValue - положительное или отрицательное число
     * @return число в виде строки
     */
    private String generateRandomNumber(int min, int max, boolean negativeValue) {
        int randomValue = new Random().nextInt((max - min) + 1) + min;
        String value = String.valueOf(randomValue);
        if (negativeValue) {
            value = "-" + value;
        }
        return value;
    }

    @Step("Выбрать месяц для публикации FTE: {date}")
    private void pickMonthToFTEPublish(LocalDate date) {
        ap.ftePublishedForm().ftePublishedMonth().click();
        DatePicker monthPicker = new DatePicker(ap.datePickerForm());
        monthPicker.pickMonth(date);
        monthPicker.okButtonClick();
    }

    @Test(groups = {"demo-7"})
    private void forecastClient() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        selectKpiType(KpiType.CLIENT_COUNT);
        pickForecastRange(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_START, "01.01.2019");
        pickForecastRange(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_END, "28.02.2019");
        pickMinKpi("10");
        pickMaxKpi("20");
        sendInTrendForm("20");
        createForecast();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"demo-7"})
    private void forecastPublication() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_PUBLISHED);
        pickMonthToKpiPublish(LocalDate.of(2019, 2, 2));
        publishKpi();
        checkForecast(ListOfNotification.APPROVE_KPI);
    }

    @Test(groups = {"demo-7"})
    private void fteForecast() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        pickForecastRangeFTE(ListOfDataInputForm.START_DATE_FTE, "01.01.2019");
        pickForecastRangeFTE(ListOfDataInputForm.START_DATE_FTE, "28.02.2019");
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"demo-7"})
    private void fteForecastPublication() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_PUBLISHED);
        pickMonthToFTEPublish(LocalDate.of(2019, 2, 1));
        publishFTE();
        checkForecast(ListOfNotification.APPROVE_FTE);
    }

    @Test(groups = "DT-1.1", description = "Выбор даты начала и конца расчета через значок календаря")
    private void dateTest() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        DatePicker dp = new DatePicker(ap.datePickerForm());
        int month = new Random().nextInt(12) + 1;
        LocalDate dateStart = LocalDateTools.getDate(LocalDateTools.THAT, month, LocalDateTools.RANDOM);
        LocalDate endDate = LocalDateTools.getDate(LocalDateTools.THAT, month, LocalDateTools.RANDOM);
        clickCalendarStartDateButton(ap.kpiForecastForm().calendarsList()
                                             .get(KPICalendarsPlace.KPI_FORECAST_DATE_START.ordinal()));
        dp.pickDate(dateStart);
        dp.okButtonClick();
        dateSelectionCheck(dateStart.format(UI.getFormat()), ap.kpiForecastForm().kpiForecastRangeList()
                .get(KPICalendarsPlace.KPI_FORECAST_DATE_START.ordinal()).getAttribute("value"));
        clickCalendarEndDateButton(KPICalendarsPlace.KPI_FORECAST_DATE_END);
        dp.pickDate(endDate);
        dp.okButtonClick();
        dateSelectionCheck(endDate.format(UI.getFormat()), ap.kpiForecastForm().kpiForecastRangeList()
                .get(KPICalendarsPlace.KPI_FORECAST_DATE_END.ordinal()).getAttribute("value"));
    }

    @Test(groups = {"SW-1"})
    public void yearScopeSwitch() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        currentDateAssert(0, true);
        diagramSlideLeft();
        currentDateAssert(1, false);
        diagramSlideRight();
        currentDateAssert(0, true);
    }

    @Test(groups = {"SW-2"})
    public void monthScopeSwitch() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        scopeSwitchMonth();
        diagramSlideLeft();
        diagramSlideRight();
    }

    @Test(groups = {"SW-3"})
    public void dayScopeSwitch() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        scopeSwitchDay();
        diagramSlideLeft();
        diagramSlideRight();
    }

    @Test(groups = {"F-1"})
    public void kpiForecastClients() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        selectKpiType(KpiType.CLIENT_COUNT);
        pickForecastRange(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_START, "01.01.2019");
        pickForecastRange(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_END, "28.02.2019");
        selectOrgUnitForImport(OrgUnitImport.ATRIUM_1);
        pickForecastRange(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_START, "01.01.2019");
        pickForecastRange(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_END, "28.02.2019");
        selectForecastAlgorithm(KpiAlgorithm.ARMA);
        pickMinKpi("10");
        pickMaxKpi("20");
        pickForecastTrend("34");
        closeForecastForm();
    }

    @Test(groups = {"DFS-1", "broken"})
    public void distributionGraphSlider() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        pencilButtonClick();
        selectCorrectionCell(Column.CORR_5, 1);
        sliderMove(true);
        sliderOkButtonClick();
    }

    @Test(groups = {"A-1", "add om"}, description = "Выбор подразделения")
    private void chooseSubunit() {
        goToAnalytics();
        omButtonClick();
        certainOmFromList(OrgUnitRepository.getRandomNotBrokenStore());
    }

    @Test(groups = {"A-3.1.1", "schedule"}, description = "Изменение масштаба отображения на День")
    private void changingScaleOfDisplayingGraphicsOnDayViaTab() {
        goToAnalytics(OrgUnitRepository.getRandomNotBrokenStore());
        scopeSwitchDay();
        assertForSwitchDay();
    }

    @Test(groups = {"A-3.1.2", "schedule"}, description = "Изменение масштаба отображения на Месяц")
    private void changingScaleOfDisplayingGraphicsOnMonthViaTab() {
        goToAnalytics(OrgUnitRepository.getRandomNotBrokenStore());
        scopeSwitchMonth();
        assertForSwitchMonth();
    }

    @Test(groups = {"A-3.1.3", "schedule"}, description = "Изменение масштаба отображения на Год")
    private void changingScaleOfDisplayingGraphicsOnYearViaTab() {
        goToAnalytics(OrgUnitRepository.getRandomNotBrokenStore());
        scopeSwitchMonth();
        scopeSwitchYear();
        assertForSwitchYear();
    }

    @Test(groups = {"A-3.1.4", "schedule"}, description = "Изменение масштаба отображения графика при клике по диаграмме")
    private void changingTheScaleOfDisplayingGraphicsWhenClickingAChart() {
        goToAnalytics(OrgUnitRepository.getRandomNotBrokenStore());
        Month month = Month.of(new Random().nextInt(12));
        selectRandomMonthOnTheRightChart(month);
        LocalDate now = LocalDate.now();
        LocalDate configDate = LocalDate.of(now.getYear(), month.getValue(), 1);
        int randomDayIndex = new Random().nextInt(configDate.lengthOfMonth());
        selectRandomDayOnTheRightChart(randomDayIndex + 1);
        assertForChoosingDay(LocalDate.of(now.getYear(), month.getValue(), randomDayIndex + 1));
    }

    @Test(groups = {"A-3.2.1"}, description = "Скрытие столбцов на диаграмме Год")
    private void hidingColumnsOnYearChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        Kpi values = kpi.getRandomHistory(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        int randomCell = date.getMonthValue() - 1;
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        pencilButtonClick();
        clickScissorsCell(randomCell);
        assertHiddenColumn(randomCell, true);
        clickScissorsCell(randomCell);
        assertHiddenColumn(randomCell, false);
    }

    @Test(groups = {"A-3.2.2"}, description = "Скрытие столбцов на диаграмме Месяц")
    private void hidingColumnsOnMonthChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.DAY);
        Kpi values = kpi.getRandomHistory(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        int randomCell = date.getDayOfMonth() - 1;
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        pencilButtonClick();
        clickScissorsCell(randomCell);
        assertHiddenColumn(randomCell, true);
        clickScissorsCell(randomCell);
        assertHiddenColumn(randomCell, false);
    }

    @Test(groups = {"А-4.1.1.1", G1, FTE2, ZOZO,
            "@Before turn off druid"},
            description = "Сохранение корректировок исторических данных на диаграмме в режим \"Год\"")
    @Link(name = "Корректировка исторических данных", url = "https://wiki.goodt.me/x/KQAuCw")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("61720")
    @Tag("А-4.1.1.1")
    @Tag(FTE2)
    private void correctionOfHistoricalDataInTheYearModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        Kpi values = kpi.getRandomHistory(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        pencilButtonClick();
        selectCorrectionCell(Column.CORR_3, date.getMonthValue() - 1);
        String randomValueForCorrection = generateRandomNumber(100, 1000, false);
        sendValueForCorrection(randomValueForCorrection);
        sliderOkButtonClick();
        saveCorrectionButton();
        String generatedString = RandomStringUtils.randomAlphanumeric(10);
        sendCorrectionComment(generatedString);
        saveCorrectionComment();
        assertForHistoricalCorrections(values, kpi, randomValueForCorrection);
    }

    @Test(groups = {"A-4.1.1.2"},
            description = "Отмена сохранения корректировок исторических данных на диаграмме в режим Год")
    @Link(name = "Корректировка исторических данных", url = "https://wiki.goodt.me/x/KQAuCw")
    private void cancelCorrectionOfHistoricalDataInTheYearModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        Kpi values = kpi.getRandomHistory(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        int randomCell = date.getMonthValue() - 1;
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        pencilButtonClick();
        selectCorrectionCell(Column.CORR_3, randomCell);
        String randomValueForCorrection = generateRandomNumber(100, 1000, false);
        sendValueForCorrection(randomValueForCorrection);
        sliderOkButtonClick();
        assertEnteredValueIsDisplayed(randomCell, randomValueForCorrection);
        selectCorrectionCell(Column.CORR_3, randomCell);
        String randomValueForCorrectionNegative = generateRandomNumber(100, 1000, true);
        sendValueForCorrection(randomValueForCorrectionNegative);
        sliderOkButtonClick();
        assertEnteredValueIsDisplayed(randomCell, "0");
        clickOnTheUrnIcon();
        assertForHistoricalCancelChanges(values, kpi);
    }

    @Test(groups = {"A-4.1.1.3"},
            description = "Сохранение корректировок исторических данных на диаграмме в режим Год с использованием распределения с предыдущего месяца")
    @Link(name = "Корректировка исторических данных", url = "https://wiki.goodt.me/x/KQAuCw")
    private void previousMonthCorrectionOfHistoricalDataInTheYearModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        Kpi values = kpi.getRandomHistory(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        int randomCell = date.getMonthValue() - 1;
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        pencilButtonClick();
        selectCorrectionCell(Column.CORR_3, randomCell);
        String randomValueForCorrection = generateRandomNumber(100, 1000, false);
        sendValueForCorrection(randomValueForCorrection);
        changeDistribution("С предыдущего месяца");
        sliderOkButtonClick();
        saveCorrectionButton();
        String generatedString = RandomStringUtils.randomAlphanumeric(10);
        sendCorrectionComment(generatedString);
        saveCorrectionComment();
        assertForHistoricalCorrections(values, kpi, randomValueForCorrection);
    }

    @Test(groups = {"A-4.1.1.4"}, description = "Отмена сохранения корректировок исторических данных на диаграмме в режим Год с использованием распределения с предыдущего месяца")
    private void cancelPreviousMonthCorrectionOfHistoricalDataInTheYearModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        Kpi values = kpi.getRandomHistory(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        int randomCell = date.getMonthValue() - 1;
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        pencilButtonClick();
        selectCorrectionCell(Column.CORR_3, randomCell);
        String randomValueForCorrectionNegative = generateRandomNumber(100, 1000, true);
        sendValueForCorrection(randomValueForCorrectionNegative);
        changeDistribution("С предыдущего месяца");
        sliderOkButtonClick();
        assertEnteredValueIsDisplayed(randomCell, "0");
        clickOnTheUrnIcon();
        assertForHistoricalCancelChanges(values, kpi);
    }

    @Test(groups = {"A-4.1.1.5"}, description = "Ввод недопустимого значения в поле Коррекция в режим Год")
    private void enteringInvalidValueCorrectionInTheYearModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        Kpi values = kpi.getRandomHistory(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        int randomCell = date.getMonthValue() - 1;
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        pencilButtonClick();
        selectCorrectionCell(Column.CORR_3, randomCell);
        String randomValueForCorrectionNegative = String.valueOf(new Random().nextDouble());
        sendValueForCorrection(randomValueForCorrectionNegative);
        correctionFieldIsRed();
        String randomStringForCorrectionNegative = generateSymbols(false, 50)
                + generateSymbols(true, 10);
        sendValueForCorrection(randomStringForCorrectionNegative);
        correctionFieldIsRed();
        assertButtonSaveNotActive();
    }

    @Test(groups = {"A-4.1.1.6"}, description = "Отмена внесения корректировок в режим Год")
    private void cancelCorrectionInTheYearModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        Kpi values = kpi.getRandomHistory(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        int randomCell = date.getMonthValue() - 1;
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        pencilButtonClick();
        selectCorrectionCell(Column.CORR_3, randomCell);
        String randomValueForCorrection = generateRandomNumber(100, 1000, false);
        sendValueForCorrection(randomValueForCorrection);
        cancelCorrection();
        assertForHistoricalCancelChanges(values, kpi);
    }

    @Test(groups = {"A-4.1.3", "incorrect values"}, description = "Закрытие окна внесения корректировок в режиме год")
    private void closingWindowMakingAdjustmentsInTheYearModeChart() {
        goToAnalytics(OrgUnitRepository.getRandomNotBrokenStore());
        pencilButtonClick();
        crossButtonClick();
        assertDisplayedDataEditingWindow();
    }

    @Test(groups = {"A-4.1.2.1"}, description = "Сохранение корректировок прогноза в режиме Год")
    private void savingForecastAdjustmentsInTheYearModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        pencilButtonClick();
        selectForecastCorrectionCell(date.getMonth());
        sliderMove(true);
        sliderOkButtonClick();
        saveCorrectionButton();
        String generatedString = RandomStringUtils.randomAlphanumeric(10);
        enterAnyComment(generatedString);
        clickButtonSaveForDialogForm();
        assertForForecastCorrections(values, kpi);
    }

    @Test(groups = {"A-4.1.2.2"}, description = "Отмена сохранения корректировок прогноза в режиме Год")
    private void cancelSavingForecastAdjustmentsInTheYearModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        pencilButtonClick();
        selectForecastCorrectionCell(date.getMonth());
        sliderMove(false);
        sliderOkButtonClick();
        clickOnTheUrnIcon();
        assertForForecastCancelChanges(values, kpi);
    }

    @Test(groups = {"A-4.1.2.3"}, description = "Отмена внесения корректировок в режиме Год")
    private void cancelForecastAdjustmentsInTheYearModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        pencilButtonClick();
        selectForecastCorrectionCell(date.getMonth());
        sliderMove(true);
        cancelCorrection();
        assertForForecastCancelChanges(values, kpi);
    }

    @Test(groups = {"A-4.1.2.4"}, description = "Внесение корректировок через поле в режиме Год")
    private void fieldCorrectAdjustmentsInTheYearModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        pencilButtonClick();
        selectForecastCorrectionCell(date.getMonth());
        assertCorrectionFieldNotChange();
    }

    @Test(groups = {"A-4.1.4", "incorrect values"}, description = "Внесение некоректных значений в поле Коррекция в режиме год")
    private void enteringUnreliableValuesInCorrectionFieldTheYearModeChart() {
        goToAnalytics(OrgUnitRepository.getRandomNotBrokenStore());
        pencilButtonClick();
        int monthNumber = getRandomMonthNumber();
        selectCorrectionCell(Column.CORR_3, monthNumber);
        sendValueForCorrection(generateSymbols(true, 50));
        correctionFieldIsRed();
        sendValueForCorrection(generateSymbols(false, 50));
        correctionFieldIsEmpty();
    }

    @Test(groups = {"A-4.2.1.1"}, description = "Сохранение корректировок исторических данных на диаграмме в режим Месяц")
    private void correctionOfHistoricalDataInTheMonthModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.DAY);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        int randomCell = date.getDayOfMonth() - 1;
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        pencilButtonClick();
        selectCorrectionCell(Column.CORR_3, randomCell);
        String randomValueForCorrection = generateRandomNumber(10, 100, false);
        sendValueForCorrection(randomValueForCorrection);
        sliderOkButtonClick();
        saveCorrectionButton();
        String generatedString = RandomStringUtils.randomAlphanumeric(10);
        sendCorrectionComment(generatedString);
        saveCorrectionComment();
        assertForHistoricalCorrections(values, kpi, randomValueForCorrection);
    }

    @Test(groups = {"A-4.2.1.2"}, description = "Отмена сохранения корректировок исторических данных на диаграмме в режим Месяц")
    private void cancelCorrectionOfHistoricalDataInTheMonthModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.DAY);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        int randomCell = date.getDayOfMonth() - 1;
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        pencilButtonClick();
        selectCorrectionCell(Column.CORR_3, randomCell);
        String randomValueForCorrection = generateRandomNumber(10, 100, false);
        sendValueForCorrection(randomValueForCorrection);
        sliderOkButtonClick();
        assertEnteredValueIsDisplayed(randomCell, randomValueForCorrection);
        selectCorrectionCell(Column.CORR_3, randomCell);
        String randomValueForCorrectionNegative = generateRandomNumber(10, 100, true);
        sendValueForCorrection(randomValueForCorrectionNegative);
        sliderOkButtonClick();
        assertEnteredValueIsDisplayed(randomCell, "0");
        clickOnTheUrnIcon();
        assertForHistoricalCancelChanges(values, kpi);
    }

    @Test(groups = {"A-4.2.1.3"}, description = "Отмена внесения корректировок исторических данных в режим Месяц")
    private void previousMonthCorrectionOfHistoricalDataInTheMonthModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.DAY);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        int randomCell = date.getDayOfMonth() - 1;
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        pencilButtonClick();
        selectCorrectionCell(Column.CORR_3, randomCell);
        String randomValueForCorrection = generateRandomNumber(10, 100, false);
        sendValueForCorrection(randomValueForCorrection);
        cancelCorrection();
        assertForHistoricalCancelChanges(values, kpi);
    }

    @Test(groups = {"A-4.2.2.1", "TEST-65"}, description = "Сохранение корректировок прогноза в режиме Месяц")
    private void saveForecastAdjustmentsInTheMonthMode() {
        KpiRepository kpi = new KpiRepository(DateUnit.DAY);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        clickOnTheRandomRightColumns();
        selectForecastCorrectionCell(date.getDayOfMonth());
        sliderMove(true);
        sliderOkButtonClick();
        saveCorrectionButton();
        String generatedString = RandomStringUtils.randomAlphanumeric(10);
        enterAnyComment(generatedString);
        clickButtonSaveForDialogForm();
        assertForForecastCorrections(values, kpi);
    }

    @Test(groups = {"A-4.2.2.2", "TEST-65"}, description = "Отмена сохранения корректировок прогноза в режиме Месяц")
    private void cancelForecastAdjustmentsInTheMonthMode() {
        KpiRepository kpi = new KpiRepository(DateUnit.DAY);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        clickOnTheRandomRightColumns();
        selectForecastCorrectionCell(date.getDayOfMonth());
        sliderMove(false);
        sliderOkButtonClick();
        clickOnTheUrnIcon();
        assertForForecastCancelChanges(values, kpi);
    }

    @Test(groups = {"A-4.2.2.3", "TEST-65"}, description = "Отмена внесения корректировок в режиме Месяц")
    private void cancellationOfAdjustmentsInTheMonthMode() {
        KpiRepository kpi = new KpiRepository(DateUnit.DAY);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        clickOnTheRandomRightColumns();
        selectForecastCorrectionCell(date.getDayOfMonth());
        sliderMove(true);
        cancelCorrection();
        assertForForecastCancelChanges(values, kpi);
    }

    @Test(groups = {"A-4.2.2.4", "TEST-65"}, description = "Внесение корректировок через поле в режиме Месяц")
    private void fieldCorrectAdjustmentsInTheMonthModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.DAY);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        clickOnTheRandomRightColumns();
        selectForecastCorrectionCell(date.getDayOfMonth());
        assertCorrectionFieldNotChange();
    }

    @Test(groups = {"A-4.2.3", "incorrect values"}, description = "Закрытие окна внесения корректировок в режиме месяц")
    private void closingWindowMakingAdjustmentsInTheMonthModeChart() {
        goToAnalytics(OrgUnitRepository.getRandomNotBrokenStore());
        scopeSwitchMonth();
        pencilButtonClick();
        crossButtonClick();
        assertDisplayedDataEditingWindow();
    }

    @Test(groups = {"A-4.2.4", "incorrect values"}, description = "Внесение некорректных значений в поле Коррекция в режиме месяц")
    private void enteringUnreliableValuesInCorrectionFieldTheMonthModeChart() {
        goToAnalytics(OrgUnitRepository.getRandomNotBrokenStore());
        scopeSwitchMonth();
        pencilButtonClick();
        selectCorrectionCell(Column.CORR_3, new Random().nextInt(LocalDate.now().lengthOfMonth() + 1));
        sendValueForCorrection(generateSymbols(true, 50));
        correctionFieldIsRed();
        sendValueForCorrection(generateSymbols(false, 50));
        correctionFieldIsEmpty();
    }

    @Test(groups = {"A-4.3.1.1"}, description = "Сохранение корректировок исторических данных в режим День")
    @Severity(SeverityLevel.NORMAL)
    private void correctionOfHistoricalDataInTheDayModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.HOUR);
        Kpi values = kpi.getRandomHistory(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        clickOnDaySignature(date.getDayOfMonth());
        pencilButtonClick();
        selectHistoryValueByTime(values.getDateTime().toLocalTime());
        String randomValueForCorrection = generateRandomNumber(5, 50, false);
        sendValueForCorrection(randomValueForCorrection);
        sliderOkButtonClick();
        saveCorrectionButton();
        String generatedString = RandomStringUtils.randomAlphanumeric(10);
        sendCorrectionComment(generatedString);
        saveCorrectionComment();
        assertForHistoricalCorrectionOnTime(values, kpi, randomValueForCorrection);
    }

    @Test(groups = {"A-4.3.1.2"}, description = "Отмена сохранения корректировок исторических данных в режим День")
    private void cancelCorrectionOfHistoricalDataInTheDayModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.HOUR);
        Kpi values = kpi.getRandomHistory(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        LocalTime time = values.getDateTime().toLocalTime();
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        clickOnDaySignature(date.getDayOfMonth());
        pencilButtonClick();
        selectHistoryValueByTime(time);
        String randomValueForCorrection = generateRandomNumber(5, 50, false);
        sendValueForCorrection(randomValueForCorrection);
        sliderOkButtonClick();
        assertEnteredValueToTimeIsDisplayed(time, randomValueForCorrection);
        selectHistoryValueByTime(time);
        String randomValueForCorrectionNegative = generateRandomNumber(5, 50, true);
        sendValueForCorrection(randomValueForCorrectionNegative);
        sliderOkButtonClick();
        assertEnteredValueToTimeIsDisplayed(time, "0");
        clickOnTheUrnIcon();
        assertForHistoricalCancelChangesOnTime(values, kpi);
    }

    @Test(groups = {"A-4.3.1.3"}, description = "Отмена внесения корректировок исторических данных в режиме День")
    private void previousMonthCorrectionOfHistoricalDataInTheDayModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.HOUR);
        Kpi values = kpi.getRandomHistory(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        LocalTime time = values.getDateTime().toLocalTime();
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        clickOnDaySignature(date.getDayOfMonth());
        pencilButtonClick();
        selectHistoryValueByTime(time);
        String randomValueForCorrection = generateRandomNumber(5, 50, false);
        sendValueForCorrection(randomValueForCorrection);
        cancelCorrection();
        assertForHistoricalCancelChangesOnTime(values, kpi);
    }

    @Test(groups = {"A-4.3.2.1", "TEST-65"}, description = "Сохранение корректировок прогноза в режиме День")
    private void saveForecastAdjustmentsInTheDayMode() {
        KpiRepository kpi = new KpiRepository(DateUnit.HOUR);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        clickOnDaySignature(date.getDayOfMonth());
        pencilButtonClick();
        selectForecastValueByTime(values.getDateTime().toLocalTime());
        String correction = generateRandomNumber(5, 50, false);
        sendValueForCorrection(correction);
        sliderOkButtonClick();
        saveCorrectionButton();
        String generatedString = RandomStringUtils.randomAlphanumeric(10);
        enterAnyComment(generatedString);
        clickButtonSaveForDialogForm();
        assertForForecastCorrectionOnTime(values, kpi, String.valueOf(correction));
    }

    @Test(groups = {"A-4.3.2.2", "TEST-65"}, description = "Отмена сохранения корректировок прогноза в режиме День")
    private void cancelForecastAdjustmentsInTheDayMode() {
        KpiRepository kpi = new KpiRepository(DateUnit.HOUR);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        clickOnDaySignature(date.getDayOfMonth());
        pencilButtonClick();
        selectForecastValueByTime(values.getDateTime().toLocalTime());
        String correction = generateRandomNumber(5, 50, false);
        sendValueForCorrection(correction);
        sliderOkButtonClick();
        clickOnTheUrnIcon();
        assertForForecastCancelChangesOnTime(values, kpi);
    }

    @Test(groups = {"A-4.3.2.3", "TEST-65"}, description = "Отмена внесения корректировок в режиме День")
    private void cancellationOfAdjustmentsInTheDayMode() {
        KpiRepository kpi = new KpiRepository(DateUnit.HOUR);
        Kpi values = kpi.getRandomForecast(true, false);
        LocalDate date = values.getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        clickOnDaySignature(date.getDayOfMonth());
        pencilButtonClick();
        selectForecastValueByTime(values.getDateTime().toLocalTime());
        String correction = generateRandomNumber(5, 50, false);
        sendValueForCorrection(correction);
        cancelCorrection();
        assertForForecastCancelChangesOnTime(values, kpi);
    }

    @Test(groups = {"A-4.3.3", "incorrect values"}, description = "Закрытие окна внесения корректировок в режиме день")
    private void closingWindowMakingAdjustmentsInTheDayModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.DAY);
        kpi.getRandomHistory(true, false);
        goToAnalytics(kpi.getOrgUnit());
        scopeSwitchDay();
        pencilButtonClick();
        crossButtonClick();
        assertDisplayedDataEditingWindow();
    }

    @Test(groups = {"A-4.3.4", "incorrect values"}, description = "Внесение некоректных значений в поле Коррекция в режиме день")
    private void enteringUnreliableValuesInCorrectionFieldTheDayModeChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        kpi.getRandomHistory(true, false);
        goToAnalytics(kpi.getOrgUnit());
        scopeSwitchDay();
        pencilButtonClick();
        int cellValue = new Random().nextInt(ap.correctionTable().getColumnValues(Column.DATE.getColumnNumber()).size() + 1);
        selectCorrectionCell(Column.CORR_3, cellValue);
        sendValueForCorrection(generateSymbols(true, 50));
        correctionFieldIsRed();
        sendValueForCorrection(generateSymbols(false, 50));
        correctionFieldIsEmpty();
        cancelCorrection();
        selectCorrectionCell(Column.CORR_5, cellValue);
        sendValueForCorrection(generateSymbols(true, 50));
        correctionFieldIsRed();
        sendValueForCorrection(generateSymbols(false, 50));
        correctionFieldIsEmpty();
    }

    @Test(groups = {"А-5.1", G1, FTE3, ZOZO,
            "@Before turn off druid"},
            description = "Активация/деактивация режима сравнения графиков")
    @Link(name = "Статья: \"Активация/деактивация\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=187564118")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("61754")
    @Tag("А-5.1")
    @Tag(FTE3)
    private void activationDeactivationOfGraphComparisonMode() {
        ImmutablePair<OrgUnit, LocalDate> omMonth = OrgUnitRepository.getOrgUnitOptions(true, KPIOrFTE.KPI_OR_FTE);
        goToAnalytics(omMonth.left);
        compareButtonClick();
        checkDisplayedLeftGraphic();
        compareButtonClick();
        checkNotDisplayedOneLeftGraphic();
    }

    @Test(groups = {"A-5.2.1", "schedule"},
            description = "Отображение значений при наведении на столбец диаграммы в режиме Год")
    private void hoveringOnTheChartColumnYear() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiOrFteForFiveYears().getDateTime().toLocalDate();
        int year = date.getYear();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(year);
        compareButtonClick();
        clickOrHoverOnRandomColumnDiagram(Direction.RIGHT, false);
        findingValueName(Direction.RIGHT);
        clickOrHoverOnRandomColumnDiagram(Direction.LEFT, false);
        findingValueName(Direction.LEFT);
    }

    @Test(groups = {"A-5.2.2", "schedule"},
            description = "Отображение значений при наведении на столбец диаграммы в режиме Месяц")
    private void hoveringOnTheChartColumnMonth() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiOrFteForFiveYears().getDateTime().toLocalDate();
        int year = date.getYear();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(year);
        clickOnMonthSignature(date.getMonth());
        compareButtonClick();
        clickOrHoverOnRandomColumnDiagram(Direction.RIGHT, false);
        findingValueName(Direction.RIGHT);
        clickOrHoverOnRandomColumnDiagram(Direction.LEFT, false);
        findingValueName(Direction.LEFT);
    }

    @Test(groups = {"A-5.2.3", "schedule"},
            description = "Отображение значений при наведении на столбец диаграммы в режиме День")
    private void hoveringOnTheChartColumnDay() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiOrFteForFiveYears().getDateTime().toLocalDate();
        int year = date.getYear();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(year);
        clickOnMonthSignature(date.getMonth());
        clickOnDaySignature(date.getDayOfMonth());
        compareButtonClick();
        clickOrHoverOnRandomColumnDiagram(Direction.RIGHT, false);
        findingValueName(Direction.RIGHT);
        clickOrHoverOnRandomColumnDiagram(Direction.LEFT, false);
        findingValueName(Direction.LEFT);
    }

    @Test(groups = {"A-5.3.1.1", "schedule"},
            description = "Переход в режим редактирования правой годовой диаграммы из режима сравнения")
    private void switchToTheEditModeOfTheRightAnnualChart() {
        ImmutablePair<OrgUnit, LocalDate> omMonth = OrgUnitRepository.getOrgUnitOptionsSpecial(true);
        goToAnalytics(omMonth.getLeft());
        compareButtonClick();
        diagramSlideLeft();
        clickOrHoverOnRandomColumnDiagram(Direction.RIGHT, true);
        displayDataEditingWindow();
        displayTheYearBeforeTheCurrentOne();
    }

    @Test(groups = {"A-5.3.1.2", "schedule"},
            description = "Переход в режим редактирования левой годовой диаграммы из режима сравнения")
    private void switchToTheEditModeOfTheLeftAnnualChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiOrFteForFiveYears().getDateTime().toLocalDate();
        int year = date.getYear();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(year);
        compareButtonClick();
        diagramSlideLeft();
        clickOrHoverOnRandomColumnDiagram(Direction.LEFT, true);
        displayDataEditingWindow();
        displayTheCurrentYear(year);
    }

    @Test(groups = {"A-5.3.2.1", "schedule"},
            description = "Переход в режим редактирования правой диаграммы Месяц из режима сравнения")
    private void switchEditingModeRightDiagramMonth() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiOrFteForFiveYears().getDateTime().toLocalDate();
        int year = date.getYear();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(year);
        compareButtonClick();
        clickOnMonthSignature(date.getMonth());
        clickOrHoverOnRandomColumnDiagram(Direction.RIGHT, true);
        displayDataEditingWindow();
        displayWordsSelectedMonthAboveSchedule(date);
    }

    @Test(groups = {"A-5.3.2.2", "schedule"},
            description = "Переход в режим редактирования левой диаграммы Месяц из режима сравнения")
    private void switchingEditModeFromComparisonModeForMonthChart() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiOrFteForFiveYears().getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(date.getYear());
        clickOnMonthSignature(date.getMonth());
        compareButtonClick();
        diagramSlideRight();
        checkDatesMatches(ChronoUnit.MONTHS, date, date.plusMonths(1));
        clickOrHoverOnRandomColumnDiagram(Direction.LEFT, true);
        displayDataEditingWindow();
        displayInscriptionAboveScheduleMonth(date);
    }

    @Test(groups = {"A-5.3.3.1", "schedule"},
            description = "Переход в режим редактирования из режима сравнения для диаграммы День")
    private void switchEditingModeRightDiagramDay() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiOrFteForFiveYears().getDateTime().toLocalDate();
        int year = date.getYear();
        goToAnalytics(kpi.getOrgUnit());
        switchToNeedYear(year);
        compareButtonClick();
        Month month = Month.of(new Random().nextInt(12) + 1);
        selectRandomMonthOnTheRightChart(month);
        LocalDate randomDate = getRandomDate();
        selectRandomDayOnTheRightChart(randomDate.getDayOfMonth());
        clickOrHoverOnRandomColumnDiagram(Direction.RIGHT, true);
        displayDataEditingWindow();
        displayInscriptionAboveScheduleDay(LocalDate.of(year, month.getValue(), randomDate.getDayOfMonth()));
    }

    @Test(groups = {"A-5.4.1.1", "schedule"}, description = "Переключение временного периода в режиме Год при активном правом графике")
    @Severity(SeverityLevel.NORMAL)
    private void switchingTimePeriodInYearModeWhileRightGraphActive() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        LocalDate now = LocalDate.now();
        goToAnalytics(orgUnit);
        compareButtonClick();
        switchRightToRandomDate(ChronoUnit.YEARS, getRandomChronoValue(), now, true);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.YEARS, now, now);
        switchLeftToRandomDate(ChronoUnit.YEARS, getRandomChronoValue(), now, true);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.YEARS, now, now);
    }

    @Test(groups = {"A-5.4.1.2", "schedule"},
            description = "Переключение временного периода в режиме Год при активном левом графике")
    private void switchingTimePeriodInYearModeWhileLeftGraphActive() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        compareButtonClick();
        LocalDate now = LocalDate.now();
        leftChartButtonClick();
        switchRightToRandomDate(ChronoUnit.YEARS, getRandomChronoValue(), now, false);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.YEARS, now, now);
        switchLeftToRandomDate(ChronoUnit.YEARS, getRandomChronoValue(), now, false);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.YEARS, now, now);
    }

    @Test(groups = {"A-5.4.2.1", "schedule"},
            description = "Переключение временного периода в режиме Месяц при активном правом графике")
    private void switchingTimePeriodInMonthModeWhileRightGraphActive() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        compareButtonClick();
        LocalDate now = LocalDate.now();
        scopeSwitchMonth();
        switchRightToRandomDate(ChronoUnit.MONTHS, getRandomChronoValue(), now, true);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.MONTHS, now, now);
        switchLeftToRandomDate(ChronoUnit.MONTHS, getRandomChronoValue(), now, true);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.MONTHS, now, now);
    }

    @Test(groups = {"A-5.4.2.2", "schedule"},
            description = "Переключение временного периода в режиме Месяц при активном левом графике")
    private void switchingTimePeriodInMonthModeWhileLeftGraphActive() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        LocalDate now = LocalDate.now();
        compareButtonClick();
        scopeSwitchMonth();
        leftChartButtonClick();
        switchRightToRandomDate(ChronoUnit.MONTHS, getRandomChronoValue(), now, false);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.MONTHS, now, now);
        switchLeftToRandomDate(ChronoUnit.MONTHS, getRandomChronoValue(), now, false);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.MONTHS, now, now);
    }

    @Test(groups = {"A-5.4.2.3", "schedule"},
            description = "Переключение временного периода в режиме Месяц (переход через график) при активном правом графике")
    private void switchingTimePeriodInMonthModeGoThroughTheChartWhileRightChartActive() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        compareButtonClick();
        LocalDate now = LocalDate.now();
        LocalDate randomDate = getRandomDate();
        selectRandomMonthOnTheRightChart(randomDate.getMonth());
        checkDatesMatches(ChronoUnit.MONTHS, now, randomDate);
        switchRightToRandomDate(ChronoUnit.MONTHS, getRandomChronoValue(), randomDate, true);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.MONTHS, now, now);
        switchLeftToRandomDate(ChronoUnit.MONTHS, getRandomChronoValue(), now, true);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.MONTHS, now, now);
    }

    @Test(groups = {"A-5.4.2.4", "schedule"},
            description = "Переключение временного периода в режиме Месяц (переход через график) при активном левом графике")
    private void switchingTimePeriodInMonthModeGoThroughTheChartWhileLeftChartActive() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        compareButtonClick();
        LocalDate now = LocalDate.now();
        LocalDate randomDate = getRandomDate();
        selectRandomMonthOnTheLeftChart(randomDate.getMonth());
        leftChartButtonClick();
        checkDatesMatches(ChronoUnit.MONTHS, randomDate, now);
        switchRightToRandomDate(ChronoUnit.MONTHS, getRandomChronoValue(), randomDate, false);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.MONTHS, now, now);
        switchLeftToRandomDate(ChronoUnit.MONTHS, getRandomChronoValue(), now, false);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.MONTHS, now, now);
    }

    @Test(groups = {"A-5.4.3.1", "schedule"},
            description = "Переключение временного периода в режиме День при активном правом графике")
    private void switchingTimePeriodInDayModeWhileRightGraphActive() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        LocalDate now = LocalDate.now();
        compareButtonClick();
        scopeSwitchDay();
        switchRightToRandomDate(ChronoUnit.DAYS, getRandomChronoValue(), now, true);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.DAYS, now, now);
        switchLeftToRandomDate(ChronoUnit.DAYS, getRandomChronoValue(), now, true);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.DAYS, now, now);
    }

    @Test(groups = {"A-5.4.3.2", "schedule"},
            description = "Переключение временного периода в режиме День при активном левом графике")
    private void switchingTimePeriodInDayModeWhileLeftGraphActive() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        compareButtonClick();
        LocalDate now = LocalDate.now();
        scopeSwitchDay();
        leftChartButtonClick();
        switchRightToRandomDate(ChronoUnit.DAYS, getRandomChronoValue(), now, false);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.DAYS, now, now);
        switchLeftToRandomDate(ChronoUnit.DAYS, getRandomChronoValue(), now, false);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.DAYS, now, now);
    }

    @Test(groups = {"A-5.4.3.3", "schedule"},
            description = "Переключение временного периода в режиме День (переход через график) при активном правом графике")
    private void switchingTimePeriodInDayModeGoThroughTheChartWhileRightChartActive() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        compareButtonClick();
        LocalDate now = LocalDate.now();
        LocalDate randomDate = getRandomDate();
        selectRandomMonthOnTheRightChart(randomDate.getMonth());
        checkDatesMatches(ChronoUnit.MONTHS, now, randomDate);
        selectRandomDayOnTheRightChart(randomDate.getDayOfMonth());
        checkDatesMatches(ChronoUnit.DAYS, now, randomDate);
        switchRightToRandomDate(ChronoUnit.DAYS, getRandomChronoValue(), randomDate, true);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.DAYS, now, now);
        switchLeftToRandomDate(ChronoUnit.DAYS, getRandomChronoValue(), now, true);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.DAYS, now, now);
    }

    @Test(groups = {"A-5.4.3.4", "schedule"},
            description = "Переключение временного периода в режиме \"День\" (переход через график) при активном левом графике")
    private void switchingTimePeriodInDayModeGoThroughTheChartWhileLeftChartActive() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        compareButtonClick();
        LocalDate now = LocalDate.now();
        LocalDate randomDate = getRandomDate();
        selectRandomMonthOnTheLeftChart(randomDate.getMonth());
        checkDatesMatches(ChronoUnit.MONTHS, randomDate, now);
        selectRandomDayOnTheLeftChart(randomDate.getDayOfMonth());
        checkDatesMatches(ChronoUnit.DAYS, randomDate, now);
        leftChartButtonClick();
        switchRightToRandomDate(ChronoUnit.DAYS, getRandomChronoValue(), randomDate, false);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.DAYS, now, now);
        switchLeftToRandomDate(ChronoUnit.DAYS, getRandomChronoValue(), now, false);
        diagramSlideReset();
        checkDatesMatches(ChronoUnit.DAYS, now, now);
    }

    @Test(groups = {"А-6.1.1", G0, FTE4, ZOZO,
            "@Before turn off druid"},
            description = "Расчет прогноза (KPI по умолчанию)")
    @Severity(SeverityLevel.CRITICAL)
    @Link(name = "Статья: \"Расчет прогноза\"", url = "https://wiki.goodt.me/x/EwAuCw")
    @Tag("А-6.1.1")
    @Tag(FTE4)
    private void checkTheForecastCalculationFunction() {
        List<LocalDate> dates = getDatesForForecast();
        KpiType kpiType = KpiType.randomKpi();
        List<KpiList> list = new ArrayList<>(Arrays.asList(KpiListRepository.getKpiByType(kpiType), KpiListRepository.getKpiByType(KpiType.CHECK_COUNT)));
        OrgUnit historicalUnit = OrgUnitRepository.getRandomOrgUnitWithHistoricalData(list, dates.get(0), dates.get(2), 13);
        PresetClass.makeKpiCorrection(historicalUnit, kpiType, dates);
        goToAnalyticsAsUser(Role.SENIOR_MANAGER, historicalUnit);
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        enterDateForForecast(1, DateTypeField.START_DATE, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
        enterDateForForecast(1, DateTypeField.END_DATE, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
        createForecast();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"А-6.1.2", G1, FTE4, ZOZO,
            "@Before turn off druid"},
            description = "Расчет прогноза по количеству чеков с выбором подразделения для импорта и тренда")
    @Link(name = "Расчет прогноза", url = "https://wiki.goodt.me/x/EwAuCw")
    @Severity(SeverityLevel.NORMAL)
    @Tag("А-6.1.2")
    @Tag(FTE4)
    private void checkTheForecastCalculationFunctionChecksWithImportAndTrend() {
        List<LocalDate> dates = getDatesForForecast();
        LocalDate now = LocalDate.now();
        KpiList checksCount = KpiListRepository.getKpiByType(KpiType.CHECK_COUNT);
        List<KpiList> list = Collections.singletonList(checksCount);
        LocalDate to = now.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        LocalDate from = to.withDayOfMonth(1).minusMonths(13);
        OrgUnit unit = OrgUnitRepository.getRandomStoreWithBusinessHours(ScheduleType.ANY_TYPE);
        OrgUnit historicalUnit = OrgUnitRepository.getRandomOrgUnitWithHistoricalData(list, from, to, 13);
        PresetClass.makeKpiCorrection(historicalUnit, KpiType.CHECK_COUNT, dates);
        goToAnalytics(unit);
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        selectKpiType(list);
        enterDateForForecast(1, DateTypeField.START_DATE, now.with(TemporalAdjusters.firstDayOfMonth()));
        enterDateForForecast(1, DateTypeField.END_DATE, now.with(TemporalAdjusters.lastDayOfMonth()));
        enterDateForForecast(5, DateTypeField.START_DATE, to.minusMonths(13));
        enterDateForForecast(5, DateTypeField.END_DATE, to);
        sendInTrendForm(String.valueOf(new Random().nextInt(99) + 1));
        getAnyCertainOmImportForecast(historicalUnit.getName());
        createForecast();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.1.3", "not actual"}, description = "Расчет прогноза по месяцам с выбором минимального и максимального значения и тренда")
    private void checkTheForecastCalculationFunctionChecksWithMinAndMaxTrend() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiAfter13Value(true, true).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        pickMonthToKpiForecast(KPICalendarsPlace.KPI_FORECAST_DATE_START, date);
        pickMonthToKpiForecast(KPICalendarsPlace.KPI_FORECAST_DATE_END, date.with(LAST_DAY));
        pickMinKpi(String.valueOf(new Random().nextInt(99) + 1));
        pickMaxKpi(String.valueOf(new Random().nextInt(99) + 1));
        sendInTrendForm(String.valueOf(new Random().nextInt(99) + 1));
        createForecast();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.1.4"}, description = "Расчет прогноза на день")
    private void checkTheForecastCalculationFunctionChecksOneDay() {
        KpiRepository kpi = new KpiRepository(DateUnit.DAY);
        LocalDate date = kpi.getRandomKpiAfter13Value(true, true).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        pickMonthToKpiForecast(KPICalendarsPlace.KPI_FORECAST_DATE_START, date);
        pickMonthToKpiForecast(KPICalendarsPlace.KPI_FORECAST_DATE_END, date.with(LAST_DAY));
        createForecast();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.1.5"}, description = "Расчет прогноза без выбора даты начала и окончания")
    private void checkTheForecastCalculationFunctionWithOutDate() {
        goToAnalytics(OrgUnitRepository.getRandomNotBrokenStore());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        selectKpiType(KpiType.CHECK_COUNT);
        clearMonthToKpiForecast(KPICalendarsPlace.KPI_FORECAST_DATE_START);
        clearMonthToKpiForecast(KPICalendarsPlace.KPI_FORECAST_DATE_END);
        createForecast();
        checkForecast(ListOfNotification.CALCULATION_FAILED);
    }

    @Test(groups = "A-6.1.6", description = "Закрытие окна Расчет прогноза")
    private void checkTheForecastCalculationFunctionClose() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        closeForecastForm();
        closeKpiAssert();
    }

    @Test(groups = "A-6.1.7", description = "Отмена выбора дат начала и окончания прогноза")
    private void deselectingTheStartAndEndDateOfKPIForecast() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        DatePicker dp = new DatePicker(ap.datePickerForm());
        LocalDate dateStart = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        LocalDate endDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        clickCalendarStartDateButton(ap.kpiForecastForm().calendarsList()
                                             .get(KPICalendarsPlace.KPI_FORECAST_DATE_START.ordinal()));
        String dateStartBefore = ap.kpiForecastForm().kpiForecastRangeList()
                .get(KPICalendarsPlace.KPI_FORECAST_DATE_START.ordinal()).getAttribute("value");
        dp.pickDate(dateStart);
        dp.cancelButtonClick();
        dateDeselectionCheck(dateStartBefore, ap.kpiForecastForm().kpiForecastRangeList()
                .get(KPICalendarsPlace.KPI_FORECAST_DATE_START.ordinal()).getAttribute("value"));
        clickCalendarEndDateButton(KPICalendarsPlace.KPI_FORECAST_DATE_END);
        String dateEndBefore = ap.kpiForecastForm().kpiForecastRangeList()
                .get(KPICalendarsPlace.KPI_FORECAST_DATE_END.ordinal()).getAttribute("value");
        dp.pickDate(endDate);
        dp.cancelButtonClick();
        dateDeselectionCheck(dateEndBefore, ap.kpiForecastForm().kpiForecastRangeList()
                .get(KPICalendarsPlace.KPI_FORECAST_DATE_END.ordinal()).getAttribute("value"));
    }

    @Test(groups = "A-6.1.8", description = "Отмена выбора дат начала и окончания исторических данных")
    private void deselectingTheStartAndEndDateOfKPIHistoricalData() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        DatePicker dp = new DatePicker(ap.datePickerForm());
        LocalDate dateStart = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        LocalDate endDate = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        clickCalendarStartDateButton(ap.kpiForecastForm().calendarsList()
                                             .get(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_START.ordinal()));
        String dateStartBefore = ap.kpiForecastForm().kpiForecastRangeList()
                .get(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_START.ordinal()).getAttribute("value");
        dp.pickDate(dateStart);
        dp.cancelButtonClick();
        dateDeselectionCheck(dateStartBefore, ap.kpiForecastForm().kpiForecastRangeList()
                .get(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_START.ordinal()).getAttribute("value"));
        clickCalendarEndDateButton(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_END);
        String dateEndBefore = ap.kpiForecastForm().kpiForecastRangeList()
                .get(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_END.ordinal()).getAttribute("value");
        dp.pickDate(endDate);
        dp.cancelButtonClick();
        dateDeselectionCheck(dateEndBefore, ap.kpiForecastForm().kpiForecastRangeList()
                .get(KPICalendarsPlace.KPI_FORECAST_HISTORICAL_DATE_END.ordinal()).getAttribute("value"));

    }

    @Test(groups = {"A-6.2.2", "design"}, description = "Публикация при отсутствии прогноза")
    private void publicationOfTheForecastForTheMonthWithOutFTE() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomForecast(false, false).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_PUBLISHED);
        pickMonthToKpiPublish(date);
        publishKpi();
        checkForecast(ListOfNotification.FAILED_KPI_APPROVE);
    }

    @Test(groups = "A-6.2.3", description = "Выбор года при публикации прогноза")
    private void selectingYearInForecastPublication() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_PUBLISHED);
        DatePicker dp = new DatePicker(ap.datePickerForm());
        clickCalendarStartDateButton(ap.kpiPublishedForm().kpiPublishedMonth());
        dp.rightYearSwitch();
        dp.leftYearSwitch();
        dp.leftYearSwitch();
    }

    @Test(groups = "A-6.2.4", description = "Отмена выбора периода при публикации прогноза")
    private void deselectingYearInKPIPublication() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_PUBLISHED);
        DatePicker dp = new DatePicker(ap.datePickerForm());
        String dateBefore = ap.kpiPublishedForm().dateInputField().getAttribute("value");
        clickCalendarStartDateButton(ap.kpiPublishedForm().kpiPublishedMonth());
        dp.cancelButtonClick();
        dateSelectionCheck(dateBefore, ap.kpiPublishedForm().dateInputField().getAttribute("value"));
    }

    @Test(groups = "A-6.2.5", description = "Закрытие окна публикации прогноза")
    private void closingTheForecastPublicationWindows() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_PUBLISHED);
        closeKpiPublishedForm();
        kpiPublishedFormIsClosed();
    }

    @Test(groups = {"А-6.3.1.1", G0, FTE7, ZOZO,
            "@Before turn off druid"},
            description = "Расчет ресурсной потребности на выбранный месяц")
    @Severity(SeverityLevel.CRITICAL)
    @Link(name = "Статья: \"Расчет ресурсной потребности\"", url = "https://wiki.goodt.me/x/AwAuCw")
    @TmsLink("61740")
    @Tag(FTE7)
    @Tag("А-6.3.1.1")
    private void calculationOfTheResourceNeedsForTheSelectedMonth() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = getRandomFromList(kpi.getForecast()).getDateTime().toLocalDate();
        goToAnalyticsAsUser(Role.SENIOR_MANAGER, kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.with(LAST_DAY).format(UI.getFormat()));
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.3.1.2", "design", "resource requirement"}, description = "Расчет ресурсной потребности на один день")
    private void calculationOfTheResourceNeedsPerDay() {
        KpiRepository kpi = new KpiRepository(DateUnit.DAY);
        LocalDate date = kpi.getRandomKpiAfter13Value(true, true).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.format(UI.getFormat()));
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.3.2.1", "design", "resource requirement"}, description = "Расчет ресурсной потребности при отсутствии исторических данных")
    private void calculationOfResourceNeedsInTheAbsenceOfHistoricalData() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiAfter13Value(false, true).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.with(LAST_DAY).format(UI.getFormat()));
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_ERROR);
    }

    @Test(groups = {"А-6.3.2.2", G1, FTE7, ZOZO,
            "@Before turn off druid"},
            description = "Расчет ресурсной потребности с выбором подразделения для импорта")
    @Link(name = "Расчет ресурсной потребности", url = "https://wiki.goodt.me/x/AwAuCw")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("61740")
    @Tag("А-6.3.2.2")
    @Tag(FTE7)
    private void calculationOfResourceNeedsWithTheChoiceOfUnitsForImport() {
        OrgUnit unit = OrgUnitRepository.getRandomNotBrokenStore();
        List<LocalDate> dates = getDatesForForecast();
        List<KpiList> list = new ArrayList<>(Arrays.asList(KpiListRepository.getKpiByType(KpiType.CLIENT_COUNT), KpiListRepository.getKpiByType(KpiType.CHECK_COUNT)));
        OrgUnit historicalUnit = OrgUnitRepository.getRandomOrgUnitWithHistoricalData(list, dates.get(0), dates.get(2), 13);
        PresetClass.makeKpiCorrection(historicalUnit, KpiType.CLIENT_COUNT, dates);
        PresetClass.makeKpiCorrection(historicalUnit, KpiType.CHECK_COUNT, dates);
        goToAnalytics(unit);
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, LocalDate.now().withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, LocalDate.now().with(LAST_DAY).format(UI.getFormat()));
        fteOrgUnitImportClick();
        getAnyCertainOmImportFte(historicalUnit.getName());
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.3.3.1", "design", "resource requirement"}, description = "Расчет ресурсной потребности при минимизации ФОТ и сохранении уровня сервиса")
    private void calculationOfResourceNeedsWhileMinimizingPayrollAndMaintainingTheLevelOfService() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiAfter13Value(true, true).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.with(LAST_DAY).format(UI.getFormat()));
        fteStrategyClick();
        fteStrategyListChose(StrategyList.FOT_MIN);
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.3.3.2", "design", "resource requirement"}, description = "Расчет ресурсной потребности при максимизации конверсии с увеличением ФОТ")
    private void calculationOfResourceNeedsWhileMaximizingPayrollAndMaintainingTheLevelOfService() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        kpi.getRandomKpiAfter13Value(true, true);
        LocalDate date = kpi.getRandomForecast(true, false).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.with(LAST_DAY).format(UI.getFormat()));
        fteStrategyClick();
        fteStrategyListChose(StrategyList.FOT_MAX);
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.3.4.1", "design", "resource requirement"}, description = "Расчет ресурсной потребности с использованием параметров.")
    private void calculationOfResourceNeedsUsingParameters() {
        OrgUnit orgUnit = OrgUnitRepository.getOrgUnitsWithRightMatchParameters();
        goToAnalytics(orgUnit);
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        methodButtonClick();
        fteMethodsChoose(FteMethodTypes.USE_PARAM);
        LocalDate now = LocalDate.now();
        int year = now.getYear() - 1;
        int month = now.getMonthValue();
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, LocalDateTools.getDate(year, month, 1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, LocalDateTools.getDate(year, month, LocalDateTools.LAST).format(UI.getFormat()));
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.3.4.2", "design", "resource requirement"},
            description = "Расчет ресурсной потребности с использованием параметров при отсутствии заданных параметров")
    private void calculationOfResourceUsingParametersInTheAbsenceOfSpecifiedParameters() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        kpi.getRandomHistory(true, false);
        PresetClass.clearMathParameters(kpi.getOrgUnit());
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        methodButtonClick();
        fteMethodsChoose(FteMethodTypes.USE_PARAM);
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_ERROR);
    }

    @Test(groups = {"A-6.3.5.1", "design", "resource requirement"}, description = "Расчет ресурсной потребности с учетом отработанных смен (приоритет)")
    private void calculationOfResourceNeedsTakingIntoAccountTheWorkedShifts() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomHistory(true, false).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        fteAlgorithmClick();
        fteAlgorithmList(AlgorithmList.PRIORITY);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.with(LAST_DAY).format(UI.getFormat()));
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.3.5.2", "design", "resource requirement"}, description = "Расчет ресурсной потребности с учетом отработанных смен (альтернативный)")
    private void calculationOfResourceNeedsTakingIntoAccountTheWorkedShiftsAlter() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomHistory(true, false).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        fteAlgorithmClick();
        fteAlgorithmList(AlgorithmList.ALTERNATIVE);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.with(LAST_DAY).format(UI.getFormat()));
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.3.5.3", "design", "resource requirement"}, description = "Расчет ресурсной потребности по производительности")
    private void calculationOfResourceNeedsTakingIntoAccountTheWorkedShiftsInHour() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomHistory(true, false).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        fteAlgorithmClick();
        fteAlgorithmList(AlgorithmList.BY_PERFORMANCE);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.with(LAST_DAY).format(UI.getFormat()));
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.3.5.4", "design", "resource requirement"}, description = "Расчет ресурсной потребности по группам")
    private void calculationOfResourceNeedsTakingIntoAccountTheWorkedShiftsInGroup() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomHistory(true, false).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        fteAlgorithmClick();
        fteAlgorithmList(AlgorithmList.BY_GROUPS);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.with(LAST_DAY).format(UI.getFormat()));
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.3.6", "design", "resource requirement"}, description = "Расчет ресурсной потребности с пересчетом плановой численности")
    private void calculationOfResourceRequirementsWithTheRecalculationOfThePlannedNumber() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomHistory(true, false).getDateTime().toLocalDate();
        goToAnalytics(kpi.getOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.with(LAST_DAY).format(UI.getFormat()));
        fteReEvalFlag();
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.3.7", "design", "resource requirement"}, description = "Просмотр данных для расчета")
    private void viewDataForCalculation() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        LocalDate dateStart = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        LocalDate endDate = dateStart.with(LAST_DAY);
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, dateStart.format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, endDate.format(UI.getFormat()));
        String oldTab = ap.getWrappedDriver().getWindowHandle();
        fteDataHref();
        DateInterval dates = new DateInterval(dateStart, endDate);
        FileDownloadCheckerForReport checker = new FileDownloadCheckerForReport(Role.ADMIN,
                                                                                Collections.singletonList(orgUnit), dates,
                                                                                TypeOfFiles.PDF, TypeOfReports.DATA_FOR_CALCULATION);
        assertForCorrectLink(checker, oldTab);
    }

    @Test(groups = "A-6.3.8", description = "Закрытие окна расчета ресурсной потребности")
    private void closingTheResourceRequirementCalculationWindow() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        fteCloseForm();
        fteCloseFormAssert();
    }

    @Test(groups = "A-6.3.9", description = "Отмена выбора даты начала и конца расчета ресурсной потребности")
    private void deselectingTheStartAndEndDateOfResourceCalculation() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        int randomMonth = new Random().nextInt(12) + 1;
        LocalDate dateStart = LocalDateTools.getDate(LocalDateTools.THAT, randomMonth, LocalDateTools.FIRST);
        LocalDate endDate = LocalDateTools.getDate(LocalDateTools.THAT, randomMonth, LocalDateTools.LAST);
        clickCalendarDateButtonAndCancel(dateStart, ListOfDataInputForm.START_DATE_FTE);
        String dateStartBefore = ap.fteForm()
                .fteEvaluationRangeList(ListOfDataInputForm.START_DATE_FTE.ordinal()).getAttribute("value");
        dateDeselectionCheck(dateStartBefore, ap.fteForm()
                .fteEvaluationRangeList(ListOfDataInputForm.START_DATE_FTE.ordinal()).getAttribute("value"));
        clickCalendarDateButtonAndCancel(endDate, ListOfDataInputForm.END_DATE_FTE);
        String dateEndBefore = ap.fteForm()
                .fteEvaluationRangeList(ListOfDataInputForm.END_DATE_FTE.ordinal()).getAttribute("value");
        dateDeselectionCheck(dateEndBefore, ap.fteForm()
                .fteEvaluationRangeList(ListOfDataInputForm.END_DATE_FTE.ordinal()).getAttribute("value"));
    }

    @Test(groups = "A-6.3.10.1", description = "Выбор даты начала и конца расчета ресурсной потребности")
    private void selectingTheStartAndEndDateOfResourceCalculation() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        LocalDate dateStart = LocalDate.now().minusMonths(new Random().nextInt(20));
        LocalDate endDate = dateStart.with(LAST_DAY);
        clickCalendarDateButtonAndOk(dateStart, ListOfDataInputForm.START_DATE_FTE);
        dateSelectionCheck(dateStart.format(UI.getFormat()), ap.fteForm()
                .fteEvaluationRangeList(ListOfDataInputForm.START_DATE_FTE.ordinal() + 1).getAttribute("value"));
        clickCalendarDateButtonAndOk(endDate, ListOfDataInputForm.END_DATE_FTE);
        dateSelectionCheck(endDate.format(UI.getFormat()), ap.fteForm()
                .fteEvaluationRangeList(ListOfDataInputForm.END_DATE_FTE.ordinal() + 1).getAttribute("value"));
    }

    @Test(groups = "A-6.3.10.2", description = "Ввод некорректных значений в поле дата начала и дата окончания")
    private void enteringIncorrectValues() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomNotBrokenStore();
        goToAnalytics(orgUnit);
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        String incorrectDate = generateIncorrectDate();
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, incorrectDate);
        fteFormDataIncorrectAssert(ListOfDataInputForm.START_DATE_FTE, incorrectDate);
        String badDate = generateBadDate();
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, badDate);
        fteFormDataIncorrectAssert(ListOfDataInputForm.END_DATE_FTE, badDate);
    }

    @Test(groups = "A-6.3.10.3")
    private void enteringIncorrectValuesLess() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        String date = Integer.toString((int) (10 + Math.random() * 17)) + (int) (10 + Math.random() * 2);
        pickForecastRangeFTE(ListOfDataInputForm.START_DATE_FTE, date);
        fteFormDataCorrect();
        String dateIncorrect = Integer.toString((int) (1 + Math.random() * 31)) + (int) (13 + Math.random() * 99);
        pickForecastRangeFTE(ListOfDataInputForm.END_DATE_FTE, dateIncorrect);
        fteFormDataIncorrectSolo();
    }

    @Test(groups = "A-6.3.10.4")
    private void enteringCorrectValuesMore() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        String date = Integer.toString((int) (10 + Math.random() * 17)) + (int) (10 + Math.random() * 2)
                + (int) (2000 + Math.random() * 15) + (int) (1111 + Math.random() * 11111);
        pickForecastRangeFTE(ListOfDataInputForm.START_DATE_FTE, date);
        fteFormDataCorrect();
        pickForecastRangeFTE(ListOfDataInputForm.END_DATE_FTE, date);
        fteFormDataCorrect();
    }

    @Test(groups = {"A-6.4.2", "design", "resource requirement"}, description = "Публикация при отсутствии ресурсной потребности")
    private void publicationInTheAbsenceOfResourceNeeds() {
        ImmutablePair<OrgUnit, LocalDate> omMonth = OrgUnitRepository.findMonthWithOutPublication(KPIOrFTE.FTE, false);
        goToAnalytics(omMonth.left);
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_PUBLISHED);
        pickMonthToFTEPublish(omMonth.right);
        publishFTE();
        checkForecast(ListOfNotification.APPROVE_FTE);
    }

    @Test(groups = {"A-6.4.1", "design", "resource requirement"}, description = "Публикация ресурсной потребности на месяц текущего года")
    @Severity(SeverityLevel.CRITICAL)
    private void publicationOfTheResourceNeedForTheMonthOfTheCurrentYear() {
        ImmutablePair<OrgUnit, LocalDate> omMonth = OrgUnitRepository.findMonthWithOutPublication(KPIOrFTE.KPI_HISTORY, true);
        goToAnalytics(omMonth.left);
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_PUBLISHED);
        pickMonthToFTEPublish(omMonth.right);
        publishFTE();
        checkForecast(ListOfNotification.APPROVE_FTE);
    }

    @Test(groups = "A-6.4.3", description = "Выбор года при публикации ресурсной потребности")
    private void selectingYearInFTEPublication() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_PUBLISHED);
        DatePicker dp = new DatePicker(ap.datePickerForm());
        clickCalendarStartDateButton(ap.ftePublishedForm().ftePublishedMonth());
        dp.rightYearSwitch();
        dp.leftYearSwitch();
        dp.leftYearSwitch();
    }

    @Test(groups = "A-6.4.4", description = "Отмена выбора периода при публикации ресурсной потребности")
    private void deselectingYearInFTEPublication() {
        goToAnalytics(OrgUnitRepository.getRandomAvailableOrgUnit());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_PUBLISHED);
        DatePicker dp = new DatePicker(ap.datePickerForm());
        String dateBefore = ap.ftePublishedForm().nameMonth().getAttribute("value");
        clickCalendarStartDateButton(ap.ftePublishedForm().ftePublishedMonth());
        dp.pickMonth(LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM).minusMonths(new Random().nextInt(11) + 1));
        dp.cancelButtonClick();
        dateDeselectionCheck(dateBefore, ap.ftePublishedForm().nameMonth().getAttribute("value"));
    }

    @Test(groups = "A-6.4.5", description = "Закрытие окна публикации ресурсной потребности")
    private void closingTheResourceRequirementPublishingWindow() {
        goToAnalytics(OrgUnitRepository.getRandomNotBrokenStore());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_PUBLISHED);
        ftePublishedCloseFormClick();
        ftePublishedCloseAssert();
    }

    @Test(groups = {"A-6.5"}, description = "Просмотр изменений в фактических данных")
    private void viewChangesInActualData() {
        goToAnalytics(OrgUnitRepository.getUnitWithNotZeroCorrectionNumber(KPIOrFTE.KPI_HISTORY));
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_CORRECTION);
        assertForCheckElementFTE();
        closeFTEForecastChanges();
    }

    @Test(groups = "A-6.6", description = "Просмотр изменений в прогнозе")
    private void viewForecastChanges() {
        goToAnalytics(OrgUnitRepository.getUnitWithNotZeroCorrectionNumber(KPIOrFTE.KPI_FORECAST));
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST_CORRECTION);
        assertForCheckElementKPI();
        closeKPIForecastChanges();
    }

    @Test(dataProvider = "KpiList", groups = "ADP-1", description = "Выбор KPI для отображения на графике")
    private void indicatorsCheck(String name) {
        goToAnalytics();
        omButtonClick();
        clickOnRandomOmFromList();
        indicatorButtonClick();
        chooseIndicator(name);
        assertForInformationVariants(name);
    }

    @Test(dataProvider = "DataList", groups = "ADP-2", description = "Выбор данных для отображения на графике")
    private void dataCheck(KPIOrFTE kpiOrFTE, OrgUnit orgUnit) {
        goToAnalytics(orgUnit);
        indicatorButtonClick();
        chooseIndicator(kpiOrFTE);
        assertDataIsNotDisplayed(kpiOrFTE);
        indicatorButtonClick();
        chooseIndicator(kpiOrFTE);
        assertDataIsDisplayed(kpiOrFTE);
    }

    @Test(groups = {"TK2654-1", G1, FTE4, ZOZO,
            "@Before turn off druid"},
            description = "Расчет прогноза по нескольким KPI")
    @Link(name = "Статья: \"2654_Запуск расчета прогноза одновременно для нескольких KPI (Суперюзер)\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=201065015")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("60814")
    @Tag("TK2654-1")
    @Tag(FTE4)
    private void twoKpiCalculate() {
        List<LocalDate> dates = getDatesForForecast();
        List<KpiList> list = new ArrayList<>(Arrays.asList(KpiListRepository.getKpiByType(KpiType.CLIENT_COUNT), KpiListRepository.getKpiByType(KpiType.CHECK_COUNT)));
        OrgUnit historicalUnit = OrgUnitRepository.getRandomOrgUnitWithHistoricalData(list, dates.get(0), dates.get(2), 13);
        PresetClass.makeKpiCorrection(historicalUnit, KpiType.CLIENT_COUNT, dates);
        PresetClass.makeKpiCorrection(historicalUnit, KpiType.CHECK_COUNT, dates);
        goToAnalytics(historicalUnit);
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        selectKpiType(list);
        enterDateForForecast(1, DateTypeField.START_DATE, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
        enterDateForForecast(1, DateTypeField.END_DATE, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
        createForecast();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"A-6.2.1", "design"}, description = "Публикация прогноза на текущий месяц")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("A-6.2.1")
    private void publicationOfTheForecastForTheMonthOfTheCurrentYear() {
        ImmutablePair<OrgUnit, LocalDate> omMonth = OrgUnitRepository.findMonthWithOutPublication(KPIOrFTE.KPI_HISTORY, true);
        goToAnalytics(omMonth.getLeft());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_PUBLISHED);
        pickMonthToKpiPublish(omMonth.right);
        publishKpi();
        checkForecast(ListOfNotification.APPROVE_KPI);
    }

    @Test(dataProvider = "RolesWithoutPermissions", groups = {"TK2045", "TEST-924"},
            description = "Проверка отображение вариантов выбора в меню троеточия")
    private void checkVariantsMatches(Role role, List<TypeOfChartMenu> unexpectedTypes) {
        new RoleWithCookies(ap.getWrappedDriver(), role).getPage(SECTION);
        omButtonClick();
        clickOnRandomOmFromList();
        clickToChartMenuButton();
        checkUiVariantsMatches(unexpectedTypes);
    }

    @Test(groups = {"TK2045-2.1", "TEST-924"}, description = "Расчет прогноза Роль 1")
    private void checkGoToReportsWithOutPermission1() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiAfter13Value(true, true).getDateTime().toLocalDate();
        new RoleWithCookies(ap.getWrappedDriver(), Role.FIRST, kpi.getOrgUnit()).getPage(SECTION);
        omButtonClick();
        certainOmFromListWithOutSearch(kpi.getOrgUnit().getName());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        pickMonthToKpiForecast(KPICalendarsPlace.KPI_FORECAST_DATE_START, date);
        pickMonthToKpiForecast(KPICalendarsPlace.KPI_FORECAST_DATE_END, date.with(LAST_DAY));
        createForecast();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"TK2045-2.4", "TEST-924"}, description = "Расчет прогноза Роль 4")
    private void checkGoToReportsWithOutPermission4() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiAfter13Value(true, true).getDateTime().toLocalDate();
        new RoleWithCookies(ap.getWrappedDriver(), Role.FOURTH, kpi.getOrgUnit()).getPage(SECTION);
        omButtonClick();
        certainOmFromListWithOutSearch(kpi.getOrgUnit().getName());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_FORECAST);
        pickMonthToKpiForecast(KPICalendarsPlace.KPI_FORECAST_DATE_START, date);
        pickMonthToKpiForecast(KPICalendarsPlace.KPI_FORECAST_DATE_END, date.with(LAST_DAY));
        createForecast();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"TK2045-3.1", "TEST-924"}, description = "Публикация kpi Роль 1")
    private void publicationOfTheForecastForTheMonthOfTheCurrentYear1() {
        ImmutablePair<OrgUnit, LocalDate> omMonth = OrgUnitRepository.findMonthWithOutPublication(KPIOrFTE.KPI_HISTORY, true);
        new RoleWithCookies(ap.getWrappedDriver(), Role.FIRST, omMonth.left).getPage(SECTION);
        omButtonClick();
        certainOmFromListWithOutSearch(omMonth.left.getName());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_PUBLISHED);
        pickMonthToKpiPublish(omMonth.right);
        publishKpi();
        checkForecast(ListOfNotification.APPROVE_KPI);
    }

    @Test(groups = {"TK2045-3.5", "TEST-924"}, description = "Публикация kpi Роль 5")
    private void publicationOfTheForecastForTheMonthOfTheCurrentYear5() {
        ImmutablePair<OrgUnit, LocalDate> omMonth = OrgUnitRepository.findMonthWithOutPublication(KPIOrFTE.KPI_HISTORY, true);
        new RoleWithCookies(ap.getWrappedDriver(), Role.FIFTH, omMonth.left).getPage(SECTION);
        omButtonClick();
        certainOmFromListWithOutSearch(omMonth.left.getName());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.KPI_PUBLISHED);
        pickMonthToKpiPublish(omMonth.right);
        publishKpi();
        checkForecast(ListOfNotification.APPROVE_KPI);
    }

    @Test(groups = {"TK2045-4.1", "TEST-924"}, description = "Расчет FTE Роль 1")
    private void calculationOfTheResourceNeedsForTheSelectedMonth1() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiAfter13Value(true, true).getDateTime().toLocalDate();
        new RoleWithCookies(ap.getWrappedDriver(), Role.FIRST, kpi.getOrgUnit()).getPage(SECTION);
        omButtonClick();
        certainOmFromListWithOutSearch(kpi.getOrgUnit().getName());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.with(LAST_DAY).format(UI.getFormat()));
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"TK2045-4.6", "TEST-924"}, description = "Расчет FTE Роль 6")
    private void calculationOfTheResourceNeedsForTheSelectedMonth6() {
        KpiRepository kpi = new KpiRepository(DateUnit.MONTH);
        LocalDate date = kpi.getRandomKpiAfter13Value(true, true).getDateTime().toLocalDate();
        new RoleWithCookies(ap.getWrappedDriver(), Role.SIXTH, kpi.getOrgUnit()).getPage(SECTION);
        omButtonClick();
        certainOmFromListWithOutSearch(kpi.getOrgUnit().getName());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_FORECAST);
        sendDataInFTEInput(ListOfDataInputForm.START_DATE_FTE, date.withDayOfMonth(1).format(UI.getFormat()));
        sendDataInFTEInput(ListOfDataInputForm.END_DATE_FTE, date.with(LAST_DAY).format(UI.getFormat()));
        evaluationFTE();
        checkForecast(ListOfNotification.CALCULATION_DONE);
    }

    @Test(groups = {"TK2045-5.1", "TEST-924"}, description = "Публикация FTE Роль 1")
    private void publicationOfTheResourceNeedForTheMonthOfTheCurrentYear1() {
        ImmutablePair<OrgUnit, LocalDate> omMonth = OrgUnitRepository.getOrgUnitOptions(false, KPIOrFTE.FTE);
        new RoleWithCookies(ap.getWrappedDriver(), Role.FIRST, omMonth.left).getPage(SECTION);
        omButtonClick();
        certainOmFromListWithOutSearch(omMonth.left.getName());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_PUBLISHED);
        pickMonthToFTEPublish(omMonth.right);
        publishFTE();
        checkForecast(ListOfNotification.APPROVE_FTE);
    }

    @Test(groups = {"TK2045-5.7", "TEST-924"}, description = "Публикация FTE Роль 7")
    private void publicationOfTheResourceNeedForTheMonthOfTheCurrentYear7() {
        ImmutablePair<OrgUnit, LocalDate> omMonth = OrgUnitRepository.getOrgUnitOptions(false, KPIOrFTE.FTE);
        new RoleWithCookies(ap.getWrappedDriver(), Role.SEVENTH, omMonth.left).getPage(SECTION);
        omButtonClick();
        certainOmFromListWithOutSearch(omMonth.left.getName());
        clickToChartMenuButton();
        selectChartMenuOption(TypeOfChartMenu.FTE_PUBLISHED);
        pickMonthToFTEPublish(omMonth.right);
        publishFTE();
        checkForecast(ListOfNotification.APPROVE_FTE);
    }
}