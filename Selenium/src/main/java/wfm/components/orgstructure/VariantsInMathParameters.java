package wfm.components.orgstructure;

import java.util.Random;

public enum VariantsInMathParameters {
    ON("Вкл."),
    OFF("Выкл."),
    INHERITED_VALUE("Наследуемое значение");

    private final String name;

    VariantsInMathParameters(String name) {
        this.name = name;
    }

    public static VariantsInMathParameters getRandomVariant() {
        return VariantsInMathParameters.values()[new Random().nextInt(VariantsInMathParameters.values().length)];
    }

    public String getName() {
        return name;
    }
}
