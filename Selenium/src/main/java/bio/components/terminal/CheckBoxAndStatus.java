package bio.components.terminal;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public enum CheckBoxAndStatus {
    START_SHIFT("Начало смены", "OPEN_SHIFT", "начало смены"),
    END_SHIFT("Конец смены", "CLOSE_SHIFT", "конец смены"),
    START_BREAK("Начало перерыва", "OPEN_BREAK", "начало перерыва"),
    END_BREAK("Конец перерыва", "CLOSE_BREAK", "конец перерыва"),
    AUTHORIZATION("Авторизация", "RECORD", "идентификация"),
    ADMIN_LOGIN("Вход в админку", "AUTHENTICATION", "авторизация"),
    ALL("", "", ""),
    CONTINUOUS_CAPTURE("Непрерывная сьемка", "CONTINUOUS_CAPTURE", "непрерывная съемка");

    private final String itemName;
    private final String apiStatus;
    private final String inTable;

    CheckBoxAndStatus(String checkBoxAndStatus, String apiStatus, String inTable) {
        this.itemName = checkBoxAndStatus;
        this.apiStatus = apiStatus;
        this.inTable = inTable;
    }

    public static CheckBoxAndStatus getByApiStatus(String apiStatus) {
        HashMap<String, CheckBoxAndStatus> temp = new HashMap<>();
        for (CheckBoxAndStatus status : CheckBoxAndStatus.values()) {
            temp.put(status.getApiStatus(), status);
        }
        return temp.get(apiStatus);
    }

    public static String getStatusesAttachment(List<CheckBoxAndStatus> list) {
        return list.stream().map(CheckBoxAndStatus::getItemName).collect(Collectors.joining(", "));
    }

    public String getItemName() {
        return itemName;
    }

    public String getApiStatus() {
        return apiStatus;
    }

    public String getInTable() {
        return inTable;
    }
}
