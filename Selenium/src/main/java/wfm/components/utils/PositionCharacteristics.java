package wfm.components.utils;

import static utils.Links.*;

public enum PositionCharacteristics {
    TYPE(POSITION_TYPES, REL_POSITION_TYPE),
    CATEGORY(POSITION_CATEGORIES, REL_POSITION_CATEGORY),
    GROUP(POSITION_GROUPS, REL_POSITION_GROUP);

    private final String linkPart;
    private final String JSONPart;

    PositionCharacteristics(String positionType, String JSONPart) {
        this.linkPart = positionType;
        this.JSONPart = JSONPart;
    }

    public String getLinkPart() {
        return linkPart;
    }

    public String getJSONPart() {
        return JSONPart;
    }

}
