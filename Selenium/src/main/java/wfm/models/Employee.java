package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.LinksAnnotation;
import utils.Projects;
import wfm.HasLinks;
import wfm.PresetClass;
import wfm.repository.CommonRepository;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static utils.Links.TAGS;
import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Employee implements EmployeeEssence, HasLinks {

    private String outerId;
    private String firstName;
    private String patronymicName;
    private String lastName;
    private String startWorkDate;
    private String endWorkDate;
    private String gender;
    private String email;
    private boolean needMentor;
    private boolean virtual;
    private int id;
    private String snils;
    @LinksAnnotation
    private JSONObject links;

    public Employee() {
    }

    public Employee(JSONObject jsonObject) {
        this.outerId = jsonObject.optString(OUTER_ID, null);
        this.firstName = jsonObject.optString(FIRST_NAME);
        this.patronymicName = jsonObject.optString(PATRONYMIC_NAME);
        this.lastName = jsonObject.optString(LAST_NAME);
        this.startWorkDate = jsonObject.optString("startWorkDate", null);
        this.endWorkDate = jsonObject.optString("endWorkDate", null);
        this.gender = jsonObject.optString("gender", null);
        this.email = jsonObject.optString("email", null);
        this.needMentor = jsonObject.optBoolean("needMentor");
        this.virtual = jsonObject.getBoolean("virtual");
        this.id = jsonObject.getInt(ID);
        this.snils = jsonObject.optString("snils");
        this.links = jsonObject.getJSONObject(LINKS);
    }

    public String getFullName() {
        return String.join(" ", lastName, firstName, patronymicName).trim();
    }

    public String getShortName() {
        return String.join(" ", lastName, firstName).trim();
    }

    /**
     * Для почты, т.к. если у сотрудника две ставки в одном ОПС имя отображается в формате Фамилия И. О.
     *
     * @return Иванов И. И.
     */
    public String getLastNameInitials() {
        //Ожидание сотрудника без имени и отчества
        String initialName = getFirstName().equals("") ? "" : getFirstName().substring(0, 1).concat(".");
        String initialPatronymicName = getPatronymicName().equals("") ? "" : getPatronymicName().substring(0, 1).concat(".");
        return String.join(" ", getLastName(), initialName, initialPatronymicName);
    }

    public String getOuterId() {
        return outerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getPatronymicName() {
        return patronymicName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getStartWorkDate() {
        return startWorkDate != null ? LocalDate.parse(startWorkDate) : null;
    }

    public LocalDate getEndWorkDate() {
        return endWorkDate != null ? LocalDate.parse(endWorkDate) : null;
    }

    public String getGender() {
        return gender;
    }

    public String getEmail() {
        return email;
    }

    public boolean isNeedMentor() {
        return needMentor;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public int getId() {
        return id;
    }

    public String getSnils() { return snils; }

    @Override
    public Employee getEmployee() {
        return this;
    }

    @Override
    public EmployeePosition getEmployeePosition() {
        String link = this.getLink(REL_EMPLOYEE_POSITIONS);
        if (link == null) return null;

        String url = link.split("\\{")[0];
        JSONObject json = getJsonFromUri(Projects.WFM, url);
        if (json == null) return null;

        JSONObject embedded = json.optJSONObject(EMBEDDED);
        if (embedded == null) return null;

        JSONArray jsonArray = embedded.optJSONArray(REL_EMPLOYEE_POSITIONS);
        if (jsonArray == null) return null;

        for (Object obj : jsonArray) {
            JSONObject employee = ((JSONObject) obj).optJSONObject(EMBEDDED).optJSONObject(EMPLOYEE);
            if (employee != null && employee.optInt(ID) == this.id) {
                return new EmployeePosition((JSONObject) obj);
            }
        }
        return null;
    }

    @Override
    public Position getPosition() {
        return null;
    }

    public List<Position> getPositions() {
        return getListFromJsonObject(getJsonFromUri(Projects.WFM, links.getJSONObject("positions").toMap().get("href").toString()), Position.class);
    }

    /**
     * Возвращает пользователя, привязанного к сотруднику.
     * Если пользователя нет, создает и возвращает его.
     */
    public User getUser() {
        String uri = refreshEmployee().getLink(REL_ACCOUNT);
        URI account;
        try {
            account = URI.create(uri);
        } catch (NullPointerException e) {
            PresetClass.addUser(this);
            account = URI.create(this.refreshEmployee().getLink(REL_ACCOUNT));
        }
        return new User(getJsonFromUri(Projects.WFM, account));
    }

    public List<String> getActualTags() {
        URI tags = URI.create(getLink(TAGS));
        String employeeTags = getJsonFromUri(Projects.WFM, tags).getString(TAGS);
        return employeeTags.equals("") ? Collections.emptyList() : Arrays.asList(employeeTags.split(", "));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Employee employee = (Employee) o;
        return Objects.equals(employee.getOuterId(), this.getOuterId())
                && employee.getId() == this.getId();
    }

    /**
     * обновление информации о сотруднике
     */
    public Employee refreshEmployee() {
        return new Employee(getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, id)));
    }

    public List<EmployeeStatus> getStatuses() {
        return getListFromJsonObject(getJsonFromUri(Projects.WFM, getLink("statuses")), EmployeeStatus.class);
    }

    @Override
    public String toString() {
        return lastName + " " + firstName;
    }

    public void setOuterId(String outerId) {
        this.outerId = outerId;
    }
}