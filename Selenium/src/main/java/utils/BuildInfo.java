package utils;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.json.JSONObject;
import wfm.repository.CommonRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BuildInfo {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yy HH:mm:ss")
    private LocalDateTime created;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm")
    private LocalDateTime serverTime;
    private String version;
    private String shortName;

    public BuildInfo(JSONObject json) {
        DateTimeFormatter createdFormatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");
        DateTimeFormatter serverTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        this.created = LocalDateTime.parse(json.getString("created"), createdFormatter);
        this.serverTime = LocalDateTime.parse(json.getString("serverTime"), serverTimeFormatter);
        this.version = json.getString("version");
        this.shortName = CommonRepository.URL_BASE.replace(".goodt.me", "").replace("https://", "");
    }
    public BuildInfo() {}

    public String getVersion() {
        return version;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    public String getShortName() {
        return shortName;
    }
}
