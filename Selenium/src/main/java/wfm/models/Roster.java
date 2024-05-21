package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.json.JSONObject;
import utils.LinksAnnotation;
import utils.Projects;
import wfm.HasLinks;
import wfm.repository.CommonRepository;

import java.time.LocalDateTime;

import static utils.Links.ROSTERS;
import static utils.Params.*;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Roster implements HasLinks {
    private int organizationUnitId;
    private int version;
    private boolean active;
    private String creationTime;
    private String publicationTime;
    private String onApprovalTime;
    private String description;
    private int calculatedRosterVersion;
    private boolean published;
    private boolean edited;
    private boolean onApproval;
    private boolean worked;
    private int id;
    private String workedApprove;
    @LinksAnnotation
    private JSONObject links;

    public Roster(JSONObject jsonObject) {
        this.active = jsonObject.getBoolean(ACTIVE);
        this.creationTime = jsonObject.optString("creationTime");
        this.onApprovalTime = jsonObject.optString("onApprovalTime");
        this.publicationTime = jsonObject.optString("publicationTime");
        this.calculatedRosterVersion = jsonObject.getInt("calculatedRosterVersion");
        this.edited = jsonObject.getBoolean("edited");
        this.onApproval = jsonObject.getBoolean("onApproval");
        this.organizationUnitId = jsonObject.getInt("organizationUnitId");
        this.published = jsonObject.getBoolean(PUBLISHED);
        this.worked = jsonObject.getBoolean("worked");
        this.id = jsonObject.getInt(ID);
        this.version = jsonObject.getInt(VERSION);
        this.description = jsonObject.optString(DESCRIPTION, null);
        this.links = jsonObject.getJSONObject(LINKS);
        this.workedApprove = jsonObject.optString("workedApprove");
    }

    public Roster() {
    }

    public int getOrganizationUnitId() {
        return organizationUnitId;
    }

    public int getVersion() {
        return version;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreationTime() {
        return LocalDateTime.parse(creationTime);
    }

    public int getCalculatedRosterVersion() {
        return calculatedRosterVersion;
    }

    public boolean isPublished() {
        return published;
    }

    public Roster setPublished(boolean published) {
        this.published = published;
        return this;
    }

    public boolean isEdited() {
        return edited;
    }

    public boolean isOnApproval() {
        return onApproval;
    }

    public Roster setOnApproval(boolean onApproval) {
        this.onApproval = onApproval;
        return this;
    }

    public boolean isWorked() {
        return worked;
    }

    public int getId() {
        return id;
    }

    public LocalDateTime getPublicationTime() {
        return !publicationTime.equals("") ? LocalDateTime.parse(publicationTime) : null;
    }

    public LocalDateTime getOnApprovalTime() {
        return !onApprovalTime.equals("") ? LocalDateTime.parse(onApprovalTime) : null;
    }

    public String getDescription() {
        return description;
    }

    public String getWorkedApprove() {
        return workedApprove;
    }

    public JSONObject getLinks() {
        return links;
    }

    /**
     * обновление информации о ростере
     */
    public Roster refreshRoster() {
        String urlEnding = makePath(ROSTERS, id);
        return new Roster(getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Roster roster = (Roster) o;

        return roster.getId() == this.getId();
    }
}
