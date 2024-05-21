package wfm.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static utils.tools.CustomTools.getRandomFromList;

public class FteOperationValuesModel {

    private final List<String> eventAndKpi;
    private final List<String> functionalRoles;
    private final JSONArray rows;

    private List<String> eventNames;
    private List<KpiList> kpiList;


    public FteOperationValuesModel(JSONArray jsonArray) {
        this.eventAndKpi = jsonArrayToStringList(jsonArray.getJSONObject(0).getJSONArray("levelOneCaptions"));
        this.functionalRoles = jsonArrayToStringList(jsonArray.getJSONObject(0).getJSONArray("levelTwoCaptions"));
        this.rows = jsonArray.getJSONObject(0).getJSONArray("rows");
    }

    private List<String> jsonArrayToStringList (JSONArray jsonArray) {
        return jsonArray.toList().stream().map(Object::toString).collect(Collectors.toList());
    }

    public List<String> getEventAndKpi() {
        return eventAndKpi;
    }

    public List<String> getFunctionalRoles() {
        return functionalRoles;
    }

    public List<LocalDateTime> getDateTimes() {
        List<LocalDateTime> dateTimes = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject tempRow = rows.getJSONObject(i);
            String rowDateTime = tempRow.getString("dateTime");
            LocalDateTime rowLocalDateTime = LocalDateTime.parse(rowDateTime);
            dateTimes.add(rowLocalDateTime);
        }
        return dateTimes;
    }

    public List<String> getEventNames() {
        return eventNames;
    }

    public void setEventNames(List<String> eventNames) {
        this.eventNames = eventNames;
    }

    public List<KpiList> getKpiList() {
        return kpiList;
    }

    public void setKpiNames(List<KpiList> kpiList) {
        this.kpiList = kpiList;
    }

    /**
     * берет случайную дату из модели возвращает месячный диапазон который включает эту дату
     */
    public DateInterval getRandomDatesWithFte() {
        List<LocalDateTime> localDateTimes = getDateTimes();
        LocalDateTime randomDataTime = getRandomFromList(localDateTimes);
        LocalDate start = randomDataTime.toLocalDate().withDayOfMonth(1);
        LocalDate end = randomDataTime.toLocalDate().withDayOfMonth(randomDataTime.toLocalDate().lengthOfMonth());
        return new DateInterval(start, end);
    }
}
