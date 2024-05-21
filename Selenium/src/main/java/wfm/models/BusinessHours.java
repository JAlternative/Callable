package wfm.models;

import org.json.JSONObject;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.Format;
import wfm.HasLinks;
import wfm.components.schedule.ScheduleType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static utils.Links.DAYS;
import static utils.Params.*;
import static utils.tools.RequestFormers.getJsonFromUri;

/**
 * @author Evgeny Gurkin 30.07.2020
 */
public class BusinessHours implements HasLinks {

    DateInterval dateInterval;
    String type;
    JSONObject links;

    List<BusinessDays> businessDays;

    public BusinessHours(JSONObject jsonObject) {
        this.dateInterval = new DateInterval(jsonObject.getJSONObject(DATE_INTERVAL));
        this.type = jsonObject.getString(TYPE);
        this.links = jsonObject.getJSONObject(LINKS);
        this.businessDays = getBusinessDaysFromObject(jsonObject);
    }

    public String getType() {
        return type;
    }

    public ScheduleType getEnumType() {
        return ScheduleType.valueOf(type);
    }

    public DateInterval getDateInterval() {
        return dateInterval;
    }

    public Integer getOrgUnitId() {
        String orgHref = getLink(ORG_UNIT_JSON);
        return orgHref == null || orgHref.isEmpty() ? null : Integer.parseInt(orgHref.substring(orgHref.lastIndexOf('/') + 1));
    }

    /**
     * Возвращает отображаемый на UI временной интервал
     */
    public String getDisplayedTimePeriod() {
        LocalDate startDate = getDateInterval().getStartDate();
        LocalDate endDate = getDateInterval().getEndDate();
        DateTimeFormatter formatter = Format.API.getFormat();
        return startDate.format(formatter) + " – " + endDate.format(formatter);
    }

    private List<BusinessDays> getBusinessDaysFromObject(JSONObject obj) {
        String daysLink = obj.getJSONObject(LINKS).getJSONObject(DAYS).getString(HREF);
        JSONObject json = getJsonFromUri(Projects.WFM, daysLink);
        if (!json.has(EMBEDDED)) {
            return new ArrayList<>();
        }
        return CustomTools.getListFromJsonArray(json.getJSONObject(EMBEDDED).getJSONArray(DAYS), BusinessDays.class);
    }

    public List<BusinessDays> getBusinessDays() {
        return businessDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BusinessHours businessHours = (BusinessHours) o;
        return businessHours.links.toString().equals(this.links.toString())
                && businessHours.getType().equals(this.getType())
                && businessHours.getDateInterval().toString().equals(this.getDateInterval().toString());
    }

    @Override
    public String toString() {
        return String.format("Период действия: %s, тип: %s", getDisplayedTimePeriod(), type);
    }

}
