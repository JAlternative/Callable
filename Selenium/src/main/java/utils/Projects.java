package utils;

import static utils.Links.API_V1;

public enum Projects {
    WFM("wfm", API_V1),
    BIO("bio", "/bio-api"),
    JOBAPP("jobapp","jobapp/cron-jobs")
    ;

    private final String name;
    private final String api;

    Projects(String name, String api) {
        this.api = api;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getApi() {
        return api;
    }
}
