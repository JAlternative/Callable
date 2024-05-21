package wfm.components.analytics;

import java.util.Arrays;
import java.util.Random;

public enum KpiType {
    CLIENT_COUNT("Трафик", 1),
    CHECK_COUNT("Количество чеков", 4);

    private final String type;
    private final Integer value;

    KpiType(final String type, final Integer value) {
        this.type = type;
        this.value = value;

    }

    public static KpiType getAnotherType(KpiType actualType) {
        if (actualType == KpiType.CHECK_COUNT) {
            return KpiType.CLIENT_COUNT;
        } else {
            return KpiType.CHECK_COUNT;
        }
    }

    public static KpiType getValue(String value) {
        return Arrays.stream(KpiType.values()).filter(kpiType -> kpiType.type.equalsIgnoreCase(value)).findAny()
                .orElseThrow(() -> new EnumConstantNotPresentException(KpiType.class, value));
    }

    public final String getType() {
        return type;
    }

    public final Integer getValue() {
        return value;
    }

    public static KpiType randomKpi() {
        int pick = new Random().nextInt(KpiType.values().length);
        return KpiType.values()[pick];
    }
}
