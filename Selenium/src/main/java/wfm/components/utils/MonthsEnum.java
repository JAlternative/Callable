package wfm.components.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

public enum MonthsEnum {
    JAN("Январь", "Янв.", null, "01"),
    FEB("Февраль", "Февр.", null, "02"),
    MAR("Март", "Март", "мар.", "03"),
    APR("Апрель", "Апр.", null, "04"),
    MAY("Май", "Май", "мая", "05"),
    JUNE("Июнь", "Июнь", "июня", "06"),
    JULY("Июль", "Июль", "июля", "07"),
    AUG("Август", "Авг.", null, "08"),
    SEPT("Сентябрь", "Сент.", null, "09"),
    OCT("Октябрь", "Окт.", null, "10"),
    NOV("Ноябрь", "Нояб.", null, "11"),
    DEC("Декабрь", "Дек.", null, "12");

    private final String monthName;
    private final String shortName;
    private final String declensionName;
    private final String monthNumber;

    MonthsEnum(String monthName, String shortName, String declensionName, String monthNumber) {
        this.monthName = monthName;
        this.shortName = shortName;
        this.declensionName = declensionName;
        this.monthNumber = monthNumber;
    }

    public static List<String> returnMonthArrayTranslate() {
        return Arrays.stream(MonthsEnum.values()).map(MonthsEnum::getMonthName).collect(Collectors.toList());
    }

    public static MonthsEnum randomMonth() {
        return MonthsEnum.values()[new Random().nextInt(MonthsEnum.values().length)];
    }

    public String getDeclensionName() {
        if (Objects.equals(declensionName, null)) {
            return getShortName();
        }
        return declensionName;
    }

    public String getShortName() {
        if (Objects.equals(shortName, null)) {
            return getMonthName();
        }
        return shortName;
    }

    public String getMonthName() {
        return monthName;
    }

    public String getMonthNumber() {
        return monthNumber;
    }

}
