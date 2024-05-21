package utils.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель файла JSON с для аллюр отчета с результатами теста
 */
public class AllureResultModel {

    private final long start;
    private final String uuid;
    private final String historyId;
    private final Path path;
    private final List<String> attachments;

    public AllureResultModel(JSONObject jsonObject, Path path) {
        this.start = jsonObject.getLong("start");
        this.uuid = jsonObject.getString("uuid");
        this.historyId = jsonObject.getString("historyId");
        this.path = path;
        this.attachments = getAllAttachments(jsonObject);
    }

    private List<String> getAllAttachments(JSONObject jsonObject) {
        List<String> attachmentsList = new ArrayList<>();
        JSONArray stepsArray = jsonObject.getJSONArray("steps");
        for (int i = 0; i < stepsArray.length(); i++) {
            JSONArray attachmentArray = stepsArray.getJSONObject(i).getJSONArray("attachments");
            for (int j = 0; j < attachmentArray.length(); j++) {
                String source = attachmentArray.getJSONObject(j).getString("source");
                attachmentsList.add(source);
            }
        }
        JSONArray attachmentArray = jsonObject.getJSONArray("attachments");
        for (int k = 0; k < attachmentArray.length(); k++) {
            String source = attachmentArray.getJSONObject(k).getString("source");
            attachmentsList.add(source);
        }
        return attachmentsList;
    }

    public long getStart() {
        return start;
    }

    public String getUuid() {
        return uuid;
    }

    public String getHistoryId() {
        return historyId;
    }

    public Path getPath() {
        return path;
    }

    public List<String> getAttachments() {
        return attachments;
    }
}
