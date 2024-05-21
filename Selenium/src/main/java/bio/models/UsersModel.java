package bio.models;

import org.json.JSONException;
import org.json.JSONObject;

public class UsersModel {
    private final int id;
    private final boolean active;
    private final String username;
    private String password;
    private final boolean administrator;

    public UsersModel(JSONObject jsonObject) {
        this.id = jsonObject.getInt("id");
        this.active = jsonObject.getBoolean("active");
        this.username = jsonObject.getString("username");
        try {
            this.password = jsonObject.getString("password");
        } catch (JSONException e) {
            this.password = null;
        }
        this.administrator = jsonObject.getBoolean("administrator");
    }

    public int getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAdministrator() {
        return administrator;
    }
}
