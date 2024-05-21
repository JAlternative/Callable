package wfm.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.Params;
import utils.Projects;
import utils.deserialization.LinksToMapDeserializer;
import utils.tools.Pairs;
import wfm.repository.CommonRepository;
import wfm.repository.EmployeePositionRepository;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utils.Links.EMPLOYEE_POSITIONS;
import static utils.Links.POSITIONS;
import static utils.Links.TAGS;
import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.CustomTools.getClassObjectFromJson;
import static utils.tools.CustomTools.getListFromJsonArray;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrgUnit {
    private Boolean availableForCalculation;
    private int organizationUnitTypeId;
    private int id;
    private String outerId;
    private String name;
    private String dateFrom;
    private String dateTo;
    private String email;
    private String tags;
    @JsonAlias(LINKS)
    @JsonDeserialize(using = LinksToMapDeserializer.class)
    private Map<String, String> links;
    private JSONObject rosterEmbedded;
    private ZoneId timeZone;

    public OrgUnit(JSONObject jsonObject) {
        this.outerId = jsonObject.optString(OUTER_ID);
        this.availableForCalculation = jsonObject.optBoolean("availableForCalculation");
        this.name = jsonObject.optString(NAME);
        this.links = getAllLinks(jsonObject.optJSONObject(LINKS));
        this.dateFrom = jsonObject.optString(DATE_FROM);
        this.dateTo = jsonObject.optString(DATE_TO);
        this.email = jsonObject.optString(EMAIL);
        this.organizationUnitTypeId = jsonObject.optInt("organizationUnitTypeId");
        this.tags = jsonObject.optString(TAGS);
        this.id = jsonObject.optInt(ID);
        this.rosterEmbedded = jsonObject.optJSONObject(EMBEDDED);
        String zone = jsonObject.optString("timeZone");
        this.timeZone = zone.equals("") ? null: ZoneId.of(zone);
    }

    public OrgUnit() {}

    public List<String> getTags() {
        //иф чтобы лишний раз не лазить в оргюнит по айди
        if (!tags.isEmpty()) {
            return Arrays.asList(tags.split(", "));
        }
        JSONObject orgUnitObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, id));
        String tagLink = orgUnitObject.getJSONObject(LINKS).getJSONObject(TAGS).getString(HREF);
        String jsonTags = getJsonFromUri(Projects.WFM, URI.create(tagLink)).getString(TAGS);
        if (jsonTags.equals("")) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(Arrays.asList(jsonTags.split(", ")));
        }
    }

    private HashMap<String, String> getAllLinks(JSONObject jsonObject) {
        HashMap<String, String> tempLinks = new HashMap<>();
        if (jsonObject != null) {
            jsonObject.keys().forEachRemaining(k -> tempLinks.put(k, jsonObject.getJSONObject(k).getString(HREF)));
        }
        return tempLinks;
    }

    public String getOuterId() {
        return outerId;
    }

    public Boolean isAvailableForCalculation() {
        return availableForCalculation;
    }

    public String getName() {
        return name;
    }

    public int getChiefPosition() {
        String chief = getLinks().get("chief");
        if (chief != null && chief.endsWith("chief")) {
            chief = getLinks().get("chief-position");
        }
        if (chief != null) {
            return Integer.parseInt(chief.substring(chief.lastIndexOf('/') + 1));
        } else {
            return -1;
        }
    }

    /**
     * Возвращает назначение руководителя данного подразделения, если он есть. Иначе возвращает null
     *
     * @param date за какую дату нужно проверить
     */
    //todo возможно, getChiefPosition, можно сделать приватным?
    public EmployeePosition getChief(LocalDate date) {
        String uriPath = makePath(POSITIONS, getChiefPosition(), EMPLOYEE_POSITIONS);
        Pairs.Builder pairs = Pairs.newBuilder()
                .from(date)
                .to(date);
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, uriPath, pairs.build());
        if (!jsonObject.keySet().contains(EMBEDDED)) {
            return null;
        }
        return getListFromJsonArray(jsonObject.getJSONObject(EMBEDDED)
                                            .getJSONArray(Params.EMPLOYEE_POSITIONS),
                                    EmployeePosition.class)
                .stream()
                .findAny()
                .orElse(null);
    }

    public DateInterval getDateInterval() {
        return new DateInterval(!dateFrom.isEmpty() ? LocalDate.parse(dateFrom) : LocalDate.parse("1970-01-01"),
                !dateTo.isEmpty() ? LocalDate.parse(dateTo) : null);
    }

    public String getEmail() {
        return email;
    }

    public int getOrganizationUnitTypeId() {
        return organizationUnitTypeId;
    }

    public int getId() {
        return id;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public Integer getParentId() {
        String parentHref = this.links.get("parent");
        if (parentHref == null) {
            return null;
        }
        if (parentHref.contains("org-unit-parent")) {
            parentHref = parentHref.substring(0, parentHref.indexOf("org-unit-parent") - 1);
        }
        return Integer.parseInt(parentHref.substring(parentHref.lastIndexOf("/") + 1));
    }

    public RosterInOrgUnit getOnApprovalRoster() {
        if (rosterEmbedded == null || !rosterEmbedded.keySet().contains(REL_ON_APPROVAL_ROSTER)) {
            return null;
        }
        return new RosterInOrgUnit(rosterEmbedded.optJSONObject(REL_ON_APPROVAL_ROSTER));
    }

    public RosterInOrgUnit getActiveRoster() {
        if (rosterEmbedded == null || !rosterEmbedded.keySet().contains(REL_ACTIVE_ROSTER)) {
            return null;
        }
        return new RosterInOrgUnit(rosterEmbedded.optJSONObject(REL_ACTIVE_ROSTER));
    }

    public RosterInOrgUnit getPublishedRoster() {
        if (rosterEmbedded == null || !rosterEmbedded.keySet().contains(REL_PUBLISHED_ROSTER)) {
            return null;
        }
        return new RosterInOrgUnit(rosterEmbedded.optJSONObject(REL_PUBLISHED_ROSTER));
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    /**
     * обновление информации об оргюните
     */
    public OrgUnit refreshOrgUnit() {
        String urlEnding = makePath(ORGANIZATION_UNITS, id);
        return new OrgUnit(getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding));
    }

    /**
     * берет родительский оргюнит
     */
    public OrgUnit getParentOrgUnit() {
        Integer parentId = this.getParentId();
        if (parentId == null) {
            return null;
        }
        return new OrgUnit(getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, parentId)));
    }

     /**
      * Проверить сотрудников на совпадение одинаковых positionCategoryId.
      * Вернёт true, если в orgUnit есть хотя бы 2 сотрудника с одинаковым positionCategoryId.
      */
     public Boolean checkEmployeesForMatchOnPositionCategoryId(boolean boolCheckByPositionCategoryId) {
         if (boolCheckByPositionCategoryId) {
             OrgUnit orgUnit = refresh();
             List<EmployeePosition> emp = EmployeePositionRepository
                     .getEmployeePositions(orgUnit.getId()).stream().filter(e -> !e.isHidden()).collect(Collectors.toList());
             if (emp.size() < 2) {
                 return false;
             }
             JSONArray positionsArray = getJsonFromUri(Projects.WFM, orgUnit.getLinks().get(REL_ORGANIZATION_UNIT_CHIEFS_CHAIN))
                     .getJSONObject(EMBEDDED).getJSONArray(POSITIONS);
             Map<String, List<String>> positionCategoryIdCount = IntStream.range(0, positionsArray.length())
                     .mapToObj(e -> positionsArray.getJSONObject(e).get(Params.POSITION_CATEGORY_ID).toString())
                     .collect(Collectors.groupingBy(e -> e));
             return positionCategoryIdCount.entrySet().stream().anyMatch(e -> e.getValue().size() >= 2);
         } else {
             return true;
         }
     }
    public OrgUnit refresh(){
        return getClassObjectFromJson(OrgUnit.class, getJsonFromUri(Projects.WFM, getLinks().get(SELF)));
    }
    public static class RosterInOrgUnit {
        String name;
        boolean active;
        boolean published;
        boolean onApproval;
        Integer version;
        String onApprovalTime;
        String publicationTime;
        JSONObject links;
        Integer id;

        public RosterInOrgUnit(JSONObject jsonObject) {
            this.name = jsonObject.getString(NAME);
            this.active = jsonObject.getBoolean(ACTIVE);
            this.published = jsonObject.getBoolean(PUBLISHED);
            this.onApproval = jsonObject.getBoolean("onApproval");
            this.version = jsonObject.getInt(VERSION);
            this.onApprovalTime = jsonObject.optString("onApprovalTime");
            this.onApprovalTime = jsonObject.optString("publicationTime");
            this.links = jsonObject.getJSONObject(LINKS);
            this.id = jsonObject.getInt("id");
        }

        public String getName() {
            return name;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isPublished() {
            return published;
        }

        public boolean isOnApproval() {
            return onApproval;
        }

        public Integer getVersion() {
            return version;
        }

        public LocalDateTime getOnApprovalTime() {
            return LocalDateTime.parse(publicationTime);
        }

        public LocalDateTime getPublicationTime() {
            return LocalDateTime.parse(publicationTime);
        }

        public JSONObject getLinks() {
            return links;
        }

        public Integer getId() {
            return id;
        }

        @Override
        public String toString() {
            return "RosterInOrgUnit{" +
                    "name='" + name + '\'' + "\n" +
                    ", active=" + active + "\n" +
                    ", published=" + published + "\n" +
                    ", onApproval=" + onApproval + "\n" +
                    ", version=" + version + "\n" +
                    ", onApprovalTime='" + onApprovalTime + '\'' + "\n" +
                    ", publicationTime='" + publicationTime + '\'' + "\n" +
                    ", links=" + links + "\n" +
                    ", id=" + id +
                    '}';
        }
    }

}


