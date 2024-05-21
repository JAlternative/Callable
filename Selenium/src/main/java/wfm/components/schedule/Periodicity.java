package wfm.components.schedule;

import java.time.LocalDate;

public enum Periodicity {
    NON_REPEAT("Не повторять", "null", 0),
    DAILY("Ежедневно", "DAILY", 1),
    WEEKLY("Еженедельно", "WEEKLY", 7),
    MONTHLY("Ежемесячно", "MONTHLY", LocalDate.now().lengthOfMonth()),
    ANNUALLY("Ежегодно", "ANNUALLY", LocalDate.now().lengthOfYear()),
    CUSTOM("Настраиваемый формат...", "", -1);

    private final String repeatType;
    private final String repeatTypeInApi;
    private final int repeatEveryValues;

    Periodicity(String repeatType, String repeatTypeInApi, int repeatEveryValues) {
        this.repeatType = repeatType;
        this.repeatTypeInApi = repeatTypeInApi;
        this.repeatEveryValues = repeatEveryValues;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public String getRepeatTypeInApi() {
        return repeatTypeInApi;
    }

    public int getRepeatEveryValues() {
        return repeatEveryValues;
    }
}

