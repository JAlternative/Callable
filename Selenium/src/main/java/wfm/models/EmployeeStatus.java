package wfm.models;

import org.json.JSONObject;
import utils.Params;
import wfm.HasLinks;

import java.time.LocalDate;
import java.util.Objects;

public class EmployeeStatus implements HasLinks {
    private final Integer id;
    private final LocalDate from;
    private final LocalDate to;
    private final EmployeeStatusType statusType;
    private final JSONObject links;

    public EmployeeStatus(JSONObject json) {
        this.id = json.optInt(Params.ID);
        this.from = LocalDate.parse(json.getString(Params.FROM));
        this.to = LocalDate.parse(json.getString(Params.TO));
        this.statusType = new EmployeeStatusType(json.getJSONObject(Params.STATUS_TYPE));
        this.links = json.optJSONObject(Params.LINKS);
    }

    public Integer getId() {
        return id;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    public EmployeeStatusType getStatusType() {
        return statusType;
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EmployeeStatus that = (EmployeeStatus) o;

        if (!from.equals(that.from)) {
            return false;
        }
        if (!Objects.equals(to, that.to)) {
            return false;
        }
        if (!links.toString().equals(that.links.toString())) {
            return false;
        }
        return statusType.getOuterId().equals(that.statusType.getOuterId());
    }

    @Override
    public int hashCode() {
        int result = from.hashCode();
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + statusType.hashCode();
        return result;
    }

}
