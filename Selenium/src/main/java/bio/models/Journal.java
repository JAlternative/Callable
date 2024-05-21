package bio.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Journal {
    private final int id;
    private final String purpose;
    private final List<String> personGroups;
    private final List<String> photoUrls;
    private final String error;
    private final String photoUrl;

    public Journal(JSONObject jsonObject) {
        this.photoUrls = getPhotoUrls(jsonObject.getJSONArray("photoUrls"));
        this.id = jsonObject.getInt("id");
        this.purpose = jsonObject.optString("purpose");
        this.error = jsonObject.optString("error", null);
        this.personGroups = getPhotoUrls(jsonObject.getJSONArray("personGroups"));
        this.photoUrl = jsonObject.optString("photoUrl", null);
    }

    private List<String> getPhotoUrls(JSONArray jsonArray) {
        List<String> photos = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            photos.add(jsonArray.get(i).toString());
        }
        return photos;
    }

    public int getId() {
        return id;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getError() {
        return error;
    }

    public List<String> getPersonGroups() {
        return personGroups;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}
