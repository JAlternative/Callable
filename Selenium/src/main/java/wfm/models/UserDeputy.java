package wfm.models;

import org.json.JSONObject;

import java.net.URI;
import java.time.LocalDate;

import static utils.Links.EMPLOYEE;
import static utils.Params.*;

public class UserDeputy {
    private final String from;
    private final String to;
    private final JSONObject links;
    private final JSONObject employee;

    public UserDeputy(JSONObject jsonObject) {
        this.from = jsonObject.getString(FROM);
        this.to = jsonObject.optString(TO, null);
        this.links = jsonObject.getJSONObject(LINKS);
        this.employee = jsonObject.getJSONObject(EMBEDDED).getJSONObject(EMPLOYEE);
    }

    public DateInterval getDateInterval() {
        return new DateInterval(LocalDate.parse(from), to != null ? LocalDate.parse(to) : null);
    }

    public Employee getEmployee() {
        return new Employee(employee);
    }

    public URI getLink(String name) {
        JSONObject link = links.optJSONObject(name);
        return link != null ? URI.create(link.getString(HREF)) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDeputy userDeputy = (UserDeputy) o;
        return userDeputy.getEmployee().equals(this.getEmployee())
                && this.getDateInterval().equals(userDeputy.getDateInterval())
                && this.getLink(SELF).equals(userDeputy.getLink(SELF));
    }
}
