package wfm.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.json.JSONException;
import org.json.JSONObject;
import utils.Links;
import utils.Params;
import utils.Projects;
import utils.deserialization.EmployeePositionDeserializer;
import wfm.HasLinks;
import wfm.repository.CommonRepository;
import wfm.repository.OrgUnitRepository;

import java.util.Objects;

import static utils.Params.*;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = EmployeePositionDeserializer.class)
public class EmployeePosition implements EmployeeEssence, HasLinks {

    private Position position;
    private OrgUnit orgUnit;
    private Employee employee;
    private DateInterval dateInterval;
    private int id;
    @JsonAlias(LINKS)
    private JSONObject links;
    private boolean temporary;
    private boolean hidden;
    private Double rate;
    private String cardNumber;

    public EmployeePosition(JSONObject jsonObject) {
        this.dateInterval = new DateInterval(jsonObject.getJSONObject(DATE_INTERVAL));
        JSONObject embedded = jsonObject.getJSONObject(EMBEDDED);
        this.position = new Position(embedded.getJSONObject(POSITION));
        this.employee = new Employee(embedded.getJSONObject(EMPLOYEE_JSON));
        try {
            JSONObject orgUnitJSON = embedded.getJSONObject(POSITION).getJSONObject(EMBEDDED).getJSONObject(ORG_UNIT_JSON);
            this.orgUnit = new OrgUnit(orgUnitJSON);
        } catch (JSONException e) {
            this.orgUnit = OrgUnitRepository.getOrgUnit(embedded.getJSONObject(POSITION).getInt(Params.ORGANIZATION_UNIT_ID));
            //todo оставить только эту часть после отделения 47 релиза
        }
        this.links = jsonObject.getJSONObject(LINKS);
        this.id = jsonObject.getInt(ID);
        this.hidden = jsonObject.optBoolean(HIDDEN);
        this.temporary = jsonObject.optBoolean(TEMPORARY);
        this.rate = jsonObject.optDouble(RATE);
        this.cardNumber = jsonObject.optString("cardNumber");
    }

    public EmployeePosition() {
    }

    public DateInterval getDateInterval() {
        return this.dateInterval;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    public OrgUnit getOrgUnit() {
        return orgUnit;
    }

    @Override
    public Employee getEmployee() {
        return employee;
    }

    @Override
    public EmployeePosition getEmployeePosition() {
        return this;
    }

    public int getId() {
        return id;
    }

    public Double getRate() {
        return rate;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void setOrgUnit(OrgUnit orgUnit) {
        this.orgUnit = orgUnit;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public void setDateInterval(DateInterval dateInterval) {
        this.dateInterval = dateInterval;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLinks(JSONObject links) {
        this.links = links;
    }

    public EmployeePosition setTemporary(boolean temporary) {
        this.temporary = temporary;
        return this;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * обновление информации о сотруднике
     */
    public EmployeePosition refreshEmployeePosition() {
        return new EmployeePosition(getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(Links.EMPLOYEE_POSITIONS, id)));
    }

    @Override
    public String toString() {
        return employee.getLastName() + " " + employee.getFirstName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmployeePosition employeePosition = (EmployeePosition) o;
        if (Objects.equals(employeePosition.getEmployee(), this.getEmployee())
                && this.rate != null
                && this.cardNumber != null) {
            return Objects.equals(employeePosition.getRate(), this.getRate())
                    && Objects.equals(employeePosition.getCardNumber(), this.getCardNumber());
        }
        return false;
    }
}