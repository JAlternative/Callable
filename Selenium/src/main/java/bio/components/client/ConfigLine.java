package bio.components.client;

public enum ConfigLine {
    CENTRAL_SERVER("centralServer"),
    TERMINAL_ID("terminalId"),
    DELAY_BEFORE_CAPTURE("terminalclient.app.delaybeforecapture"),
    EVENT_SELECTOR_MODE("terminalclient.app.eventselectormode"),
    WORKING_MODE("terminalclient.app.workingMode"),
    RECOGNITION_METHOD("terminalclient.app.recognitionMethod"),
    SHOW_CONNECTION_STATUS("terminalclient.app.showconnectionstatus"),
    USE_DELAY("terminalclient.resultpage.usedelay"),
    DELAY_COUNT("terminalclient.resultpage.delaycount"),
    ROTATED1("terminalclient.resultpage.delaycount"),
    ROTATED2("terminalclient.resultpage.delaycount"),
    SUPPORT_EMAIL("terminalclient.supportEmail"),
    SHOW_ONE_CAM_ONLY("terminalclient.app.forceshowonecamonly"),
    BOTTOM_FRAUD("bottomLandmark"),
    IMAGES_JOURNAL_DELIVERY("terminalclient.images.journalDelivery"),
    ;
    private final String name;

    ConfigLine(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
