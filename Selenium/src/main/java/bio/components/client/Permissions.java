package bio.components.client;

import java.util.ArrayList;
import java.util.List;

import static utils.tools.CustomTools.getRandomFromList;

public enum Permissions {
    TERMINALS_VIEW("Просмотр раздела терминалов"),
    TERMINALS_EDIT("Редактирование раздела терминалов"),
    PERSONS_VIEW("Просмотр раздела персонал"),
    PERSONS_EDIT("Редактирование раздела персонал"),
    JOURNAL_VIEW("Просмотр журнала событий"),
    LICENSE_VIEW("Просмотр раздела лицензии"),
    LICENSE_EDIT("Редактирование разделя лицензия"),
    USERS_VIEW("USERS_VIEW"),
    USERS_EDIT("USERS_EDIT");

    private final String permissionText;

    Permissions(String permissionText) {
        this.permissionText = permissionText;
    }

    public static Permissions randomPermission() {
        List<Permissions> tempList = new ArrayList<>();
        tempList.add(Permissions.TERMINALS_VIEW);
        tempList.add(Permissions.TERMINALS_EDIT);
        tempList.add(Permissions.PERSONS_VIEW);
        tempList.add(Permissions.PERSONS_EDIT);
        return getRandomFromList(tempList);
    }

    public String getPermissionText() {
        return permissionText;
    }
}
