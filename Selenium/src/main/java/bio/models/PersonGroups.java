package bio.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static utils.Params.NAME;

public class PersonGroups {
    private final int id;
    private final boolean external;
    private final Integer parentId;
    private final List<Map<String, String>> personPositions;
    private final String name;
    private final List<String> personPositionsList;

    public PersonGroups(JSONObject jsonObject) {
        this.id = jsonObject.getInt("id");
        this.name = jsonObject.getString(NAME);
        this.external = jsonObject.getBoolean("external");
        if (!jsonObject.isNull("parentId"))
            this.parentId = jsonObject.getInt("parentId");
        else
            parentId = null;
        this.personPositions = getAllPersonPositions(jsonObject.getJSONArray("personPositions"));
        this.personPositionsList = getPersonPositions(jsonObject.getJSONArray("personPositions"));

    }

    private List<Map<String, String>> getAllPersonPositions(JSONArray jsonArray) {
        List<Map<String, String>> personPositions = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Map<String, String> tempMap = new HashMap<>();
            JSONObject temp = jsonArray.getJSONObject(i);
            if (!temp.isNull("personId")) {
                String personId = temp.getString("personId");
                String positionName = temp.getString("positionName");
                tempMap.put(personId, positionName);
                personPositions.add(tempMap);
            }
        }
        return personPositions;
    }

    public int getId() {
        return id;
    }

    public boolean isExternal() {
        return external;
    }

    public Integer getParentId() {
        return parentId;
    }

    public List<Map<String, String>> getPersonPositions() {
        return personPositions;
    }

    public List<String> getPersonPositionsList() {
        return personPositionsList;
    }

    public String getName() {
        return name;
    }

    private List<String> getPersonPositions(JSONArray jsonArray) {
        List<String> personPositions = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject temp = jsonArray.getJSONObject(i);
            if (!temp.isNull("personId")) {
                String personId = temp.getString("personId");
                personPositions.add(personId);
            }
        }
        return personPositions;
    }
}
