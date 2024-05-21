package bio.models;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.RequestFormers;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.RequestFormers.makePath;
import static utils.tools.RequestFormers.setUrlAndInitiateForApi;

public class Person {
    private final String id;
    private final boolean external;
    private final boolean recognizable;
    private final String firstName;
    private final String patronymicName;
    private final String lastName;
    private final JSONArray personGroupPositions;

    private static final String BIO_URL = getTestProperty("central");
    private final String personExceptionType;
    private final JSONObject user;

    public Person(JSONObject jsonObject) {
        this.id = jsonObject.getString("id");
        this.external = jsonObject.getBoolean("external");
        this.recognizable = jsonObject.getBoolean("recognizable");
        this.firstName = jsonObject.optString(FIRST_NAME);
        this.lastName = jsonObject.optString(LAST_NAME);
        this.personExceptionType = jsonObject.optString("personExceptionType", null);
        this.user = jsonObject.optJSONObject(USER);
        this.patronymicName = jsonObject.optString(PATRONYMIC_NAME);
        this.personGroupPositions = jsonObject.getJSONArray("personGroupPositions");
    }

    public String getId() {
        return id;
    }

    public boolean isExternal() {
        return external;
    }

    public String getFullName() {
        return (getLastName() + " " + getFirstName() + " " + getPatronymicName()).trim();
    }

    public HashMap<String, List<Integer>> getPermission() {
        String urlEnding = makePath(PERSONS, getId(), PERMISSIONS);
        URI uri = RequestFormers.setUri(Projects.BIO, BIO_URL, urlEnding);
        JSONArray temp = new JSONArray(setUrlAndInitiateForApi(uri, Projects.BIO));
        HashMap<String, List<Integer>> resMap = new HashMap<>();
        if (!temp.toList().isEmpty()) {
            for (int i = 0; i < temp.length(); i++) {
                JSONObject tempObj = temp.getJSONObject(i);
                List<Integer> orgIds = tempObj.getJSONObject("value").getJSONArray(PERSON_GROUP_IDS).toList()
                        .stream().map(Object::toString).map(Integer::valueOf).collect(Collectors.toList());
                resMap.put(tempObj.get("key").toString(), orgIds);

            }
        }
        return resMap;
    }

    public List<PersonGroupPositions> getPersonGroupPositions() {
        if (personGroupPositions.length() == 0) {
            return null;
        } else {
            return CustomTools.getListFromJsonArray(personGroupPositions, Person.PersonGroupPositions.class);
        }
    }

    public List<String> getAllPersonGroupName() {
        return getPersonGroupPositions().stream().map(PersonGroupPositions::getPersonGroupName).collect(Collectors.toList());
    }

    public List<Integer> getAllPersonGroupId() {
        return getPersonGroupPositions().stream().map(PersonGroupPositions::getPersonGroupId).collect(Collectors.toList());
    }

    public List<String> getAllPositionName() {
        return getPersonGroupPositions().stream().map(PersonGroupPositions::getPositionName).collect(Collectors.toList());
    }

    public boolean isRecognizable() {
        return recognizable;
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

    public boolean isUserNotNull() {
        return user != null;
    }

    public String getUsername() {
        return user != null ? user.getString("username") : null;
    }

    public Integer getUserId() {
        return user != null ? user.getInt("id") : null;
    }

    public Boolean isActive() {
        return user != null ? user.getBoolean(ACTIVE) : null;
    }

    public Object getPassword() {
        return user != null ? user.get("password") : null;
    }

    public Boolean getAdministrator() {
        return user != null ? user.getBoolean("administrator") : null;
    }

    public String getPersonExceptionType() {
        return personExceptionType;
    }

    public static class PersonGroupPositions {

        private final String personGroupId;
        private final String personGroupName;
        private final String positionName;

        public PersonGroupPositions(JSONObject jsonObject) {
            this.personGroupId = jsonObject.getString("personGroupId");
            this.personGroupName = jsonObject.optString("personGroupName");
            this.positionName = jsonObject.getString("positionName");
        }

        public Integer getPersonGroupId() {
            return Integer.valueOf(personGroupId);
        }

        public String getPersonGroupName() {
            return personGroupName;
        }

        public String getPositionName() {
            return positionName;
        }
    }
}
