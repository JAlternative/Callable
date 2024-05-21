package pagemodel;

import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.Links;
import utils.Projects;
import utils.authorization.ClientReturners;
import utils.tools.Format;
import utils.tools.Pairs;
import wfm.models.OrgUnit;
import wfm.repository.OrgUnitRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

import static utils.Links.*;
import static utils.Params.EMBEDDED;
import static utils.tools.RequestFormers.makePath;
import static utils.tools.RequestFormers.setUri;

public class Integration {

    private static final String URL_I = Links.getTestProperty("release") + "/";
    private static final Logger LOG = LoggerFactory.getLogger(Integration.class);

    @DataProvider(name = "UnitList", parallel = true)
    private static Object[][] orgUnitToCheckData() {
        List<OrgUnit> unitList = OrgUnitRepository.getOrgUnitsNotClosedAndAllType()
                .stream()
                .filter(OrgUnit::isAvailableForCalculation)
                .filter(om -> om.getOrganizationUnitTypeId() == 5)
                .collect(Collectors.toList());
        int arrayLength = unitList.size();
        Object[][] array = new Object[arrayLength][];
        for (int i = 0; i < arrayLength; i++) {
            OrgUnit tempUnit = unitList.get(i);
            int id = tempUnit.getId();
            String omName = tempUnit.getName();
            array[i] = new Object[]{id, omName};
        }
        return array;
    }

    private Map<LocalDate, Double> historyChecker(String url, int unitId, String method,
                                                  LocalDate dateFrom, LocalDate dateTo) {
        String urlEnd = makePath(ORGANIZATION_UNITS, unitId, KPI, 1, method);
        List<NameValuePair> pairs = Pairs.newBuilder().from(dateFrom).to(dateTo).level(2).build();
        URI uri = setUri(Projects.WFM, url, urlEnd, pairs);
        HttpUriRequest httpUriRequest = RequestBuilder.get().setUri(uri).build();
        HttpResponse httpResponse = null;
        try {
            httpResponse = ClientReturners.httpClientReturner(Projects.WFM).execute(httpUriRequest);
        } catch (IOException e) {
            Assert.fail("Response не был получен для ОЮ " + unitId + ", по URL " + url, e);
        }
        String jsonValues = null;
        try {
            jsonValues = EntityUtils.toString(Objects.requireNonNull(httpResponse).getEntity());
        } catch (IOException e) {
            LOG.info("Не смогли преобразовать json в строку", e);
        }
        if (jsonValues != null) {
            JSONObject jsonObject = new JSONObject(jsonValues);
            JSONArray jsonArray = jsonObject.getJSONObject(EMBEDDED).getJSONArray("kpiValueGroupResourceList");
            HashMap<LocalDate, Double> historyMap = new HashMap<>();
            for (int i = 0; jsonArray.length() > i; i++) {
                JSONObject currentObject = jsonArray.getJSONObject(i);
                LocalDate tempDate = LocalDate.parse(
                        currentObject.getString("firstDate"),
                        Format.API_KPI_VALUE.getFormat()
                );
                historyMap.put(tempDate, Double.valueOf(currentObject.get("value").toString()));
            }
            return historyMap;
        }
        throw new AssertionError("Entity не был получен для ОМ " + unitId + ", по URL " + url);
    }

    @Step("Взяли исторические данные и сравнили с прогнозом для ОМ с id {unitId}")
    private void checkData(String url, int unitId, int countDayDelay, String unitName) {
        LocalDate dateTo = LocalDate.now();
        LocalDate dateFrom = dateTo.minusDays(countDayDelay);
        Period period = Period.between(dateFrom, dateTo);
        Map<LocalDate, Double> historyMap = historyChecker(url, unitId, "kpi-values", dateFrom, dateTo);
        Map<LocalDate, Double> forecastMap = historyChecker(url, unitId, "kpi-forecast-values", dateFrom, dateTo);
        Assert.assertEquals(forecastMap.size(), historyMap.size(), "Количество дней для сравнения не совпало");
        List<LocalDate> listOfProblem = new ArrayList<>();
        File directory = new File("integration");
        if (!directory.exists()) {
            directory.mkdir();
        }
        for (int g = 0; g < historyMap.size(); g++) {
            List<LocalDate> tempKeyHistory = new ArrayList<>(historyMap.keySet());
            if (historyMap.get(tempKeyHistory.get(g)) == 0.0) {
                listOfProblem.add((LocalDate) historyMap.keySet().toArray()[g]);
            }
            if (listOfProblem.size() >= period.getDays()) {
                File file = new File("integration/" + dateTo + "_integration.csv");
                String listOfDates = StringUtils.join(listOfProblem, ",");
                boolean exist = false;
                try {
                    for (String line : Files.readAllLines(Paths.get(file.getPath()))) {
                        String[] split = line.split(",");
                        if (!exist) {
                            if (split.length > 0) {
                                exist = unitId == Integer.parseInt(split[0]);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!exist) {
                    try (FileWriter fileWriter = new FileWriter(file, true)) {
                        String forWriter = String.format("%d,%s,%s%n", unitId, unitName, listOfDates);
                        fileWriter.write(forWriter);
                    } catch (IOException e) {
                        LOG.info("Файл для записи отсутствует", e);
                    }
                }
                String forMessage = String.format(
                        "ОЮ id %d с именем %s. Период без данных %s", unitId, unitName, listOfDates
                );
                Assert.fail(forMessage);
            }
        }
    }

    private Map<LocalDate, Double> onlyHistoryChecker(String url, int unitId,
                                                      LocalDate dateFrom, LocalDate dateTo) {
        String urlEndCorrection = makePath(ORGANIZATION_UNITS, unitId, KPI, 1, KPI_CORRECTION_SESSION);
        URI uriCorrection = setUri(Projects.WFM, url, urlEndCorrection);
        HttpUriRequest httpUriRequestCorrection = RequestBuilder.get().setUri(uriCorrection).build();
        HttpResponse httpResponseCorrection = null;
        try {
            httpResponseCorrection = ClientReturners.httpClientReturner(Projects.WFM).execute(httpUriRequestCorrection);
        } catch (IOException e) {
            Assert.fail("Response не был получен для коррекции " + unitId + ", по URL " + url, e);
        }
        String jsonValuesCorrection = null;
        try {
            jsonValuesCorrection = EntityUtils.toString(Objects.requireNonNull(httpResponseCorrection).getEntity());
        } catch (IOException e) {
            LOG.info("Не смогли преобразовать json в строку", e);
        }
        JSONObject jsonObjectCorrection;
        //Проверка на то, что корекшен сессия для ОМ существуют, по длине мапы будет происходить фильтрация
        try {
            jsonObjectCorrection = new JSONObject(jsonValuesCorrection);
        } catch (org.json.JSONException jsonException) {
            return new HashMap<>();
        }

        String[] sepUrl = jsonObjectCorrection.getJSONObject("_links")
                .getJSONObject("self")
                .getString("href")
                .split("/");
        int correctionId = Integer.parseInt(sepUrl[sepUrl.length - 1]);

        String urlEnd = makePath(KPI_CORRECTION_SESSIONS, correctionId, KPI_DIAGNOSTICS_VALUES);
        List<NameValuePair> pairs = Pairs.newBuilder().from(dateFrom).timeUnit("DAY").to(dateTo).build();
        URI uri = setUri(Projects.WFM, url, urlEnd, pairs);
        HttpUriRequest httpUriRequest = RequestBuilder.get().setUri(uri).build();
        HttpResponse httpResponse = null;
        try {
            httpResponse = ClientReturners.httpClientReturner(Projects.WFM).execute(httpUriRequest);
        } catch (IOException e) {
            Assert.fail("Response не был получен для ОЮ " + unitId + ", по URL " + url, e);
        }
        String jsonValues = null;
        try {
            jsonValues = EntityUtils.toString(Objects.requireNonNull(httpResponse).getEntity());
        } catch (IOException e) {
            LOG.info("Не смогли преобразовать json в строку", e);
        }
        if (jsonValues != null) {
            JSONObject jsonObject = new JSONObject(jsonValues);
            JSONArray jsonArray = jsonObject.getJSONObject(EMBEDDED).getJSONArray("kpiDiagnosticsValues");
            HashMap<LocalDate, Double> historyMap = new HashMap<>();
            for (int i = 0; jsonArray.length() > i; i++) {
                JSONObject currentObject = jsonArray.getJSONObject(i);
                LocalDate tempDate = LocalDate.parse(
                        currentObject.getString("datetime"),
                        Format.API_KPI_VALUE.getFormat()
                );
                historyMap.put(tempDate, Double.valueOf(currentObject.get("value").toString()));
            }
            return historyMap;
        }
        throw new AssertionError("Entity не был получен для ОМ " + unitId + ", по URL " + url);
    }

    /**
     * Для анализа данных в шеле и инветиве, задача понять временные отрезки по ОМ, где нет данных больше чем duration
     */
    private void checkData(String url, int unitId, String unitName, int duration) {
        LocalDate dateTo = LocalDate.now();
        LocalDate dateFrom = LocalDate.of(2020, 01, 01);
        Map<LocalDate, Double> historyMap = onlyHistoryChecker(url, unitId, dateFrom, dateTo);

        File directory = new File("integration");
        if (!directory.exists()) {
            directory.mkdir();
        }

        List<LocalDate> nullDays = historyMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
        List<String> reportString = durationNullListParser(nullDays, duration);

        if (!nullDays.isEmpty() && !reportString.isEmpty()) {
            File file = new File("integration/" + dateTo + "_integration.csv");
            String listOfDates = StringUtils.join(reportString, ",");
            boolean exist = false;
            try {
                for (String line : Files.readAllLines(Paths.get(file.getPath()))) {
                    String[] split = line.split(",");
                    if (!exist) {
                        if (split.length > 0) {
                            exist = unitId == Integer.parseInt(split[0]);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!exist) {
                try (FileWriter fileWriter = new FileWriter(file, true)) {
                    String forWriter = String.format("%d,%s,%s%n", unitId, unitName, listOfDates);
                    fileWriter.write(forWriter);
                } catch (IOException e) {
                    LOG.info("Файл для записи отсутствует", e);
                }
            }
        } else if (historyMap.isEmpty()) {
            File file = new File("integration/" + dateTo + "_integration.csv");
            boolean exist = false;
            try {
                for (String line : Files.readAllLines(Paths.get(file.getPath()))) {
                    String[] split = line.split(",");
                    if (!exist) {
                        if (split.length > 0) {
                            exist = unitId == Integer.parseInt(split[0]);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!exist) {
                try (FileWriter fileWriter = new FileWriter(file, true)) {
                    String forWriter = String.format("%d,%s,%s%n", unitId, unitName, dateFrom + "--" + dateTo.minusDays(1) + "_пометка");
                    fileWriter.write(forWriter);
                } catch (IOException e) {
                    LOG.info("Файл для записи отсутствует", e);
                }
            }
        }
    }

    /**
     * Для формирования списка дат  в формате с и по,
     * когда были пропуски данные за период не меньше чем указанная длительность
     *
     * @param nullDays список дней, в которых есть нулевые значения
     * @param duration количество дней, за которые нужно сделать выборку
     * @return
     */
    private List<String> durationNullListParser(List<LocalDate> nullDays, int duration) {
        int counter = 0;
        List<String> reportArray = new ArrayList<>();
        List<LocalDate> temp = new ArrayList<>();
        int sizeNullDays = nullDays.size();
        for (int i = 0; i < sizeNullDays - 1; i++) {
            if (Period.between(nullDays.get(i), nullDays.get(i + 1)).getDays() == 1) {
                temp.add(nullDays.get(i));
                counter++;
                if (counter == sizeNullDays - 1) {
                    reportArray.add(
                            temp.stream().min(LocalDate::compareTo).get() + "--" + nullDays.get(i)

                    );
                }
            } else if (counter >= duration) {
                reportArray.add(temp.stream().min(LocalDate::compareTo).get() + "--" + nullDays.get(i));
                counter = 0;
                temp.clear();
            } else {
                counter = 0;
                temp.clear();
            }
        }
        return reportArray;
    }

    @Test(dataProvider = "UnitList", groups = "provider", description = "Проверка исторических данных")
    private void apiCheck(int unitId, String omName) {
        //для ручной отладки нужно очищать куки
        checkData(URL_I, unitId, omName, 30);
        //        checkData(URL_I, unitId, 14, omName);
    }

}
