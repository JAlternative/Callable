package wfm.components.positioncategories;

import java.util.ArrayList;
import java.util.List;

import static utils.tools.CustomTools.getRandomFromList;

public enum WorkGraphFilter {
    ALL("Весь список", ""),
    FLOATING("Плавающий", "DYNAMIC"),
    FIXED("Фиксированный", "STATIC");

    private final String filter;
    private final String parameterName;

    WorkGraphFilter(String filter, String parameterName) {
        this.filter = filter;
        this.parameterName = parameterName;
    }

    public static WorkGraphFilter randomFilter() {
        List<WorkGraphFilter> tempList = new ArrayList<>();
        tempList.add(WorkGraphFilter.FIXED);
        tempList.add(WorkGraphFilter.FLOATING);
        return getRandomFromList(tempList);
    }

    public String getFilter() {
        return filter;
    }

    public String getParameterName() {
        return parameterName;
    }
}
