package wfm.repository;

import io.qameta.allure.Allure;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Links;
import utils.Projects;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import wfm.components.analytics.KpiType;
import wfm.components.schedule.DateUnit;
import wfm.models.Kpi;
import wfm.models.OrgUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.ErrorMessagesForReport.NO_VALID_DATE;
import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.CustomTools.getListFromJsonArray;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

public class KpiRepository {
    private static final Logger LOG = LoggerFactory.getLogger(KpiRepository.class);
    private static final String URL_KPI_REP = Links.getTestProperty("release") + "/analytics";
    private static final Random RANDOM = new Random();
    private DateUnit unit;
    private OrgUnit orgUnit;
    private boolean historical;

    public KpiRepository(DateUnit unit) {
        this.unit = unit;
    }

    /**
     * @param isExist - нужен ли промежуток с существующими данными, или наоборот
     * @return Случайный Кпи (прогноз) за выбранный DateUnit, за последние 13 месяцев
     */
    public Kpi getRandomForecast(boolean isExist, boolean fullYearData) {
        setHistorical(false);
        if (unit == DateUnit.HOUR) {
            setDateUnit(DateUnit.DAY);
            LocalDate date = getRandomOgUnitKPI(isExist, true, fullYearData).getDateTime().toLocalDate();
            setDateUnit(DateUnit.HOUR);
            List<Kpi> list = getValues(date, date, true, 1);
            return getRandomFromList(list);
        }
        return getRandomOgUnitKPI(isExist, false, fullYearData);
    }

    /**
     * @param isExist      - нужен ли промежуток с существующими данными, или наоборот
     * @param fullYearData - данные просматриваются за последние 13 месяцев и возвращает только если все 13 соответствуют условиям
     * @return Случайный Кпи (исторические данные) за выбранный DateUnit, за последние 13 месяцев
     */
    public Kpi getRandomHistory(boolean isExist, boolean fullYearData) {
        setHistorical(true);
        if (unit == DateUnit.HOUR) {
            setDateUnit(DateUnit.DAY);
            LocalDate date = getRandomOgUnitKPI(isExist, false, fullYearData).getDateTime().toLocalDate();
            setDateUnit(DateUnit.HOUR);
            List<Kpi> list = getValues(date, date, false, 1);
            return getRandomFromList(list);
        }
        return getRandomOgUnitKPI(isExist, true, fullYearData);
    }

    /**
     * @param date дата в которую идет поиск прогноза
     * @return возвращает прогноз с его данными в указанную дату, в необходимом  промежутки времени
     */
    public Kpi getForecastValue(LocalDate date) {
        return getValues(date, date, false, 1).get(0);
    }

    /**
     * @param date дата в которую идет поиск исторических данных
     * @return возвращает исторические данные с его данными в указанную дату, в необходимом  промежутки времени
     */
    public Kpi getHistoricalValue(LocalDate date) {
        return getValues(date, date, true, 1).get(0);
    }

    /**
     * @return Метод аналогичный getHistoricalValue(LocalDate date), только для ситуаций, когда DateUnit
     * - день (с использованием времени, по часам)
     */
    public Kpi getHistoricalValue(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        LocalDate date = dateTime.toLocalDate();
        return getValues(date, date, true, 1).stream().filter(s -> s.getDateTime().toLocalTime()
                .compareTo(time) == 0).findAny().orElseThrow(() -> new AssertionError(NO_VALID_DATE + "Не было найдено нужное время"));
    }

    /**
     * @return Метод аналогичный getForecastValue(LocalDate date), только для ситуаций, когда DateUnit
     * - день (с использованием времени, по часам)
     */
    public Kpi getForecastValue(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        LocalDate date = dateTime.toLocalDate();
        return getValues(date, date, false, 1)
                .stream().filter(s -> s.getDateTime().toLocalTime().compareTo(time) == 0)
                .findAny().orElseThrow(() -> new AssertionError(NO_VALID_DATE + "Не было найдено нужное время"));
    }

    private List<Kpi> getValues(LocalDate from, LocalDate to, boolean historical, int kpiId) {
        String id;
        if (historical) {
            id = getId(makePath(ORGANIZATION_UNITS, orgUnit.getId(), KPI, kpiId != 0 ? kpiId : 1, KPI_CORRECTION_SESSION));
        } else {
            id = getId(makePath(ORGANIZATION_UNITS, orgUnit.getId(), KPI, kpiId != 0 ? kpiId : 1, KPI_FORECAST_CORRECTION_SESSION));
        }
        if (id != null) {
            String urlEnding;
            if (historical) {
                urlEnding = makePath(KPI_CORRECTION_SESSIONS, id, KPI_DIAGNOSTICS_VALUES);
            } else {
                urlEnding = makePath(KPI_FORECAST_CORRECTION_SESSIONS, id, KPI_FORECAST_DIAGNOSTICS_VALUES);
            }
            return getKpiValues(urlEnding, from, to, historical);
        }
        return new ArrayList<>();
    }

    private List<Kpi> getKpiValues(String urlEnding, LocalDate from, LocalDate to, boolean historical) {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .from(from)
                .to(to)
                .timeUnit(unit.toString())
                .build();
        String key;
        if (historical) {
            key = "kpiDiagnosticsValues";
        } else {
            key = "kpiForecastDiagnosticsValues";
        }
        JSONArray array = getJsonFromUri(Projects.WFM, URL_KPI_REP, urlEnding, pairs).getJSONObject(EMBEDDED)
                .getJSONArray(key);
        return getListFromJsonArray(array, Kpi.class);
    }

    private String getId(String urlEnding) {
        String selfHref;
        try {
            selfHref = getJsonFromUri(Projects.WFM, URL_KPI_REP, urlEnding).getJSONObject(LINKS)
                    .getJSONObject(SELF).getString(HREF);
            return selfHref.substring(selfHref.lastIndexOf("/") + 1);
        } catch (AssertionError ignored) {
            return null;
        }
    }

    private Kpi getRandomOgUnitKPI(boolean exist, boolean historical, boolean fullYearData) {
        int checkSizeValue;
        String dataRange;
        if (fullYearData) {
            checkSizeValue = 12;
            dataRange = "за последние 13 месяцев.";
        } else {
            checkSizeValue = 0;
            dataRange = "хотя бы за один месяц.";
        }
        List<OrgUnit> temp = OrgUnitRepository.getAllAvailableOrgUnits(true);
        Kpi omKpiValue = null;
        LocalDate last = LocalDateTools.getLastDate().minusMonths(1);
        LocalDate start = last.minusMonths(12).withDayOfMonth(1);
        while (!temp.isEmpty()) {
            int randomValueFromSize = RANDOM.nextInt(temp.size());
            OrgUnit randomUnit = temp.get(randomValueFromSize);
            if (!isBroken(randomUnit.getId())) {
                this.orgUnit = randomUnit;
                List<Kpi> kpiList = getValues(start, last, historical, 1).stream()
                        .filter(kpi1 -> kpi1.getValue() == 0.0 ^ exist).collect(Collectors.toList());
                if (kpiList.size() > checkSizeValue) {
                    omKpiValue = getRandomFromList(kpiList);
                    break;
                }
            }
            temp.remove(randomValueFromSize);
        }
        String status;
        if (exist) {
            status = "имеется";
        } else {
            status = "не имеется";
        }
        LOG.info("Был выбран оргЮнит {}, у которого {} набор исторических данных", orgUnit.getName(), status);
        Allure.addAttachment("Поиск", "Был найден оргЮнит: " + orgUnit.getName() +
                " у которого " + status + "исторические данные" + dataRange);
        return omKpiValue;
    }

    public OrgUnit getOrgUnit() {
        return orgUnit;
    }

    public void setOrgUnit(OrgUnit orgUnit) {
        this.orgUnit = orgUnit;
    }

    public DateUnit getDateUnit() {
        return unit;
    }

    private void setDateUnit(DateUnit unit) {
        this.unit = unit;
    }

    /**
     * @param id - айди оргюинта
     * @return Предварительно проверяет сломан ли оргюнит
     */
    private boolean isBroken(int id) {
        LocalDate last = LocalDateTools.getLastDate();
        LocalDate start = last.minusMonths(13).withDayOfMonth(1);
        String urlEnding = makePath(ORGANIZATION_UNITS, id, KPI, 1, KPI_FORECAST_VALUES);
        List<NameValuePair> pairs = Pairs.newBuilder()
                .kpiId(1)
                .from(start)
                .to(last)
                .level(0)
                .build();
        return getJsonFromUri(Projects.WFM, URL_KPI_REP, urlEnding, pairs).isNull(EMBEDDED);
    }

    public boolean isHistorical() {
        return historical;
    }

    private void setHistorical(boolean historical) {
        this.historical = historical;
    }

    public String getAllureString() {
        if (historical) {
            return "исторических данных";
        } else {
            return "прогноза";
        }
    }

    /**
     * Берет случайный объект KPI если выполняются условия за 13 месяцов до этого KPI
     *
     * @param exist      - наличие или отсутствие данных
     * @param historical - исторические данные или прогноз
     * @return - случайный объект KPI
     */
    public Kpi getRandomKpiAfter13Value(boolean exist, boolean historical, int kpiId) {
        if (unit == DateUnit.DAY) {
            setDateUnit(DateUnit.MONTH);
            //сначала проверяем что за 13 месяцев до этого все соответствует условиями
            List<Kpi> allKpi = getRandomOgUnitAllKPI(exist, historical, false);
            List<LocalDate> datesList = allKpi.stream().map(kpi -> kpi.getDateTime().toLocalDate())
                    .sorted().collect(Collectors.toList());
            LocalDate randomDateAfter = datesList.get(RANDOM.nextInt(datesList.size() - 12) + 13);
            //возвращаем день и берем случайный подходящий день из месяца
            setDateUnit(DateUnit.DAY);
            List<Kpi> list = getValues(randomDateAfter, randomDateAfter.plusDays(randomDateAfter.lengthOfMonth() - 1), historical, kpiId);
            return getRandomFromList(list);
        }
        List<Kpi> allKpi = getRandomOgUnitAllKPI(exist, historical, false);
        List<LocalDate> datesList = allKpi.stream().map(kpi -> kpi.getDateTime().toLocalDate())
                .sorted().collect(Collectors.toList());
        LocalDate randomDateAfter = datesList.get(RANDOM.nextInt(datesList.size() - 12) + 12);
        return allKpi.stream().filter(kpi -> kpi.getDateTime().toLocalDate().equals(randomDateAfter)).findFirst()
                .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "Не нашли дату в списке"));
    }

    public Kpi getRandomKpiAfter13Value(boolean exist, boolean historical) {
        return getRandomKpiAfter13Value(exist, historical, 1);
    }

    private List<Kpi> getRandomOgUnitAllKPI(boolean exist, boolean historical, boolean oneMonth) {
        return getRandomOgUnitAllKPI(exist, historical, oneMonth, 1);
    }

    public List<Kpi> getForecast() {
        return getRandomOgUnitAllKPI(true, false, true);
    }

    /**
     * Метод берет список KPI с учетом параметров, за последние 5 лет. За эти 5 лет должно быть 1 (если oneMonthTrue)
     * или 13 месяцев подходящим по параметрам
     */
    private List<Kpi> getRandomOgUnitAllKPI(boolean exist, boolean historical, boolean oneMonth, int kpiId) {
        List<OrgUnit> temp = OrgUnitRepository.getAllAvailableOrgUnits(true);
        List<Kpi> omKpiValue = new ArrayList<>();
        LocalDate last = LocalDateTools.getLastDate().minusMonths(1);
        LocalDate start = last.minusMonths(12).withDayOfMonth(1).minusYears(5);
        while (!temp.isEmpty()) {
            int randomValueFromSize = RANDOM.nextInt(temp.size());
            OrgUnit randomUnit = temp.get(randomValueFromSize);
            if (!isBroken(randomUnit.getId())) {
                this.orgUnit = randomUnit;
                List<Kpi> kpiList = getValues(start, last, historical, kpiId).stream()
                        .filter(kpi1 -> kpi1.getValue() == 0.0 ^ exist).collect(Collectors.toList());
                int months = 13;
                if (oneMonth) {
                    months = 1;
                }
                if (kpiList.size() >= months) {
                    omKpiValue = kpiList;
                    break;
                }
            }
            temp.remove(randomValueFromSize);
        }
        String status;
        if (exist) {
            status = "имеется";
        } else {
            status = "не имеется";
        }
        String dataType;
        if (historical) {
            dataType = " исторические данные.";
        } else {
            dataType = " данные прогноза";
        }
        LOG.info("Был выбран оргЮнит {}, у которого {} набор исторических данных", orgUnit.getName(), status);
        Allure.addAttachment("Поиск", "Был найден оргЮнит: " + orgUnit.getName() +
                " у которого " + status + dataType);
        return omKpiValue;
    }

    /**
     * Берет случайный объект KPI c наличием данных (прогноз и/или исторические данные) хотя бы за один месяц за последние 5 лет
     *
     * @return - случайный объект KPI
     */
    public Kpi getRandomKpiOrFteForFiveYears() {
        if (unit == DateUnit.DAY) {
            setDateUnit(DateUnit.MONTH);
            //сначала проверяем что есть данные в месяце
            List<Kpi> historyKpi = getRandomOgUnitAllKPI(true, true, true);
            List<Kpi> forecastKpi = getForecast();
            historyKpi.addAll(forecastKpi);
            List<LocalDate> datesList = historyKpi.stream().map(kpi -> kpi.getDateTime().toLocalDate())
                    .collect(Collectors.toList());
            LocalDate randomDate = datesList.get(RANDOM.nextInt(datesList.size()));
            //возвращаем день и берем случайный подходящий день из месяца
            setDateUnit(DateUnit.DAY);
            List<Kpi> listHistory = getValues(randomDate, randomDate.plusDays(randomDate.lengthOfMonth() - 1), true, 1);
            List<Kpi> listForecast = getValues(randomDate, randomDate.plusDays(randomDate.lengthOfMonth() - 1), false, 1);
            listHistory.addAll(listForecast);
            return listHistory.get(RANDOM.nextInt(listHistory.size()));
        }
        List<Kpi> historyKpi = getRandomOgUnitAllKPI(true, true, true);
        List<Kpi> forecastKpi = getForecast();
        historyKpi.addAll(forecastKpi);
        return historyKpi.get(RANDOM.nextInt(historyKpi.size()));
    }

    /**
     * @return лимит часов оргюнита на текущий месяц
     */
    public Kpi getHourLimit() {
        String path = makePath(ORGANIZATION_UNITS, orgUnit.getId(), KPI, 1, PUBLISHED_KPI_LIST);
        return getKpiValues(path,
                            LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonth(), 1),
                            LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonth(), LocalDate.now().lengthOfMonth()),
                            true)
                .get(0);
    }

    public List<Kpi> getValuesForKpi(LocalDate from, LocalDate to, boolean historical, int kpiId) {
        String id;
        if (historical) {
            id = getId(makePath(ORGANIZATION_UNITS, orgUnit.getId(), KPI, kpiId, KPI_CORRECTION_SESSION));
        } else {
            id = getId(makePath(ORGANIZATION_UNITS, orgUnit.getId(), KPI, kpiId, KPI_FORECAST_CORRECTION_SESSION));
        }
        if (id != null) {
            String urlEnding;
            if (historical) {
                urlEnding = makePath(KPI_CORRECTION_SESSIONS, id, KPI_DIAGNOSTICS_VALUES);
            } else {
                urlEnding = makePath(KPI_FORECAST_CORRECTION_SESSIONS, id, KPI_FORECAST_DIAGNOSTICS_VALUES);
            }
            return getKpiValues(urlEnding, from, to, historical);
        }
        return new ArrayList<>();
    }

    public static Integer getKpiCorrectionSessionId(OrgUnit unit, KpiType kpiType) {
        String path = makePath(ORGANIZATION_UNITS, unit.getId(), KPI, kpiType.getValue(), KPI_CORRECTION_SESSION);
        String link = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, path)
                .getJSONObject(LINKS).getJSONObject(SELF).getString(HREF);
        return Integer.parseInt(link.substring(link.lastIndexOf('/') + 1));
    }

}
