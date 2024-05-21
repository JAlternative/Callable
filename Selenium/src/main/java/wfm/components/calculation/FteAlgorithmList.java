package wfm.components.calculation;

import java.util.Random;

public enum FteAlgorithmList {
    DEFAULT,
    IN_VIEW_OF_THE_WORKED_SHIFTS_PRIORITY,
    IN_VIEW_OF_THE_FULFILLED_SHIFTS_ALTERNATIVE,
    PERFORMANCE_CALCULATION_BY_HOUR_TRAFFIC,
    BY_GROUPS;

    public static FteAlgorithmList getRandomType() {
        return FteAlgorithmList.
                values()[new Random().nextInt(FteAlgorithmList.values().length)];
    }

}
