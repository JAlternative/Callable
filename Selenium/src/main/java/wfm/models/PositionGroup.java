package wfm.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.json.JSONObject;
import utils.LinksAnnotation;
import wfm.HasLinks;

import java.util.List;
import java.util.Objects;

import static utils.Params.*;
import static wfm.repository.PositionGroupRepository.getAllPositionGroups;
/**
 * На UI называется "Функциональная роль"
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionGroup implements HasLinks {

    private final String name;
    private final int id;
    private final boolean ftePositionGroup;
    private final boolean hidden;
    @LinksAnnotation
    private JSONObject links;

    public PositionGroup(JSONObject json) {
        this.name = json.getString(NAME);
        this.id = json.optInt(ID);
        this.ftePositionGroup = json.optBoolean(FTE_POSITION_GROUP);
        this.links = json.getJSONObject(LINKS);
        this.hidden = json.optBoolean("hidden");
    }

    public static PositionGroup getGroup(String roleName) {
        List <PositionGroup> allRoles = getAllPositionGroups();
        return allRoles.stream().filter(role -> role.getName().equals(roleName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Не удалось найти функциональную роль с именем: " + roleName));
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public boolean getFtePositionGroup() {
        return ftePositionGroup;
    }

    public JSONObject getLinks() {
        return links;
    }

    public PositionGroup setLinks(JSONObject links) {
        this.links = links;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PositionGroup posGroup = (PositionGroup) o;
        return Objects.equals(posGroup.getName(), this.getName())
                && posGroup.getId() == this.getId();
    }

    public boolean isHidden() {
        return hidden;
    }
}
