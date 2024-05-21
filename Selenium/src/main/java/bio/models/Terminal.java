package bio.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

import static utils.Params.DESCRIPTION;
import static utils.Params.PERSON_GROUP_IDS;

public class Terminal {
    private final String id;
    private final String serialNumber;
    private final JSONArray personGroupIds;
    private final String description;
    private final String blockingStatus;
    private final int blockTimeout;
    private final String pin;

    public Terminal(JSONObject jsonObject) {
        this.id = jsonObject.getString("id");
        this.serialNumber = jsonObject.get("serialNumber").toString();
        this.description = jsonObject.get(DESCRIPTION).toString();
        this.blockingStatus = jsonObject.get("blockingStatus").toString();
        this.blockTimeout = jsonObject.getInt("blockTimeout");
        if (!jsonObject.get("pin").toString().equals("null"))
            this.pin = jsonObject.get("pin").toString();
        else
            this.pin = null;
        this.personGroupIds = jsonObject.getJSONArray(PERSON_GROUP_IDS);
    }

    public String getId() {
        return id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getDescription() {
        return description;
    }

    public String getBlockingStatus() {
        return blockingStatus;
    }

    public String getPin() {
        return pin;
    }

    public List<Integer> getPersonGroupIds() {
        return personGroupIds.toList().stream().map(Object::toString).map(Integer::valueOf).collect(Collectors.toList());
    }

    public int getBlockTimeout() {
        return blockTimeout;
    }
}
