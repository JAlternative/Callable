package wfm.components.calculation;

import java.util.Random;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public
enum CalculationType {
    KPI(1, "Бизнес-драйверы"),
    FTE(2, "FTE"),
    FTE_BY_MONTH(3, "FTE"),
    SHIFT(4, "Смены"),
    TARGET_NUMBER(5, "Плановая численность");

    private final int batchCalculationType;
    private final String batchCalculation;

    CalculationType(int valueElement, String batchCalculation) {
        this.batchCalculationType = valueElement;
        this.batchCalculation = batchCalculation;
    }

    public static CalculationType randomCalculationType() {
        return CalculationType.values()[new Random().nextInt(CalculationType.values().length)];
    }

    public int getListBatchCalculationType() {
        return batchCalculationType;
    }

    public String getBatchCalculation() {
        return batchCalculation;
    }
}
