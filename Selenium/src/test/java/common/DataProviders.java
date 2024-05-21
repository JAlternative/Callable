package common;

import org.testng.annotations.DataProvider;
import wfm.components.utils.Role;

public class DataProviders {

    @DataProvider(name = "true/false")
    private static Object[][] trueFalse() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{true};
        array[1] = new Object[]{false};
        return array;
    }

    @DataProvider(name = "true/false/null")
    private static Object[][] trueFalseNull() {
        Object[][] array = new Object[3][];
        array[0] = new Object[]{true};
        array[1] = new Object[]{false};
        array[2] = new Object[]{null};
        return array;
    }

    @DataProvider(name = "roles 3, 4, 6-8")
    private static Object[][] publication() {
        Object[][] array = new Object[5][];
        array[0] = new Object[]{Role.THIRD};
        array[1] = new Object[]{Role.FOURTH};
        array[2] = new Object[]{Role.SIXTH};
        array[3] = new Object[]{Role.SEVENTH};
        array[4] = new Object[]{Role.EIGHTH};
        return array;
    }

    @DataProvider(name = "roles 1, 4")
    private static Object[][] dutyWithPermission() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{Role.FIRST};
        array[1] = new Object[]{Role.FOURTH};
        return array;
    }

    @DataProvider(name = "roles 1, 5")
    private static Object[][] overtimeWithPermission() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{Role.FIRST};
        array[1] = new Object[]{Role.FIFTH};
        return array;
    }

    @DataProvider(name = "roles 1, 6")
    private static Object[][] dpRolesFiveSix() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{Role.FIRST};
        array[1] = new Object[]{Role.SIXTH};
        return array;
    }

    @DataProvider(name = "roles 3, 4")
    private static Object[][] overtimeWithoutPermission() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{Role.THIRD};
        array[1] = new Object[]{Role.FOURTH};
        return array;
    }

    @DataProvider(name = "roles 3, 5")
    private static Object[][] dutyWithoutPermission() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{Role.THIRD};
        array[1] = new Object[]{Role.FIFTH};
        return array;
    }

    @DataProvider(name = "roles 3, 5-7")
    private static Object[][] calculate() {
        Object[][] array = new Object[4][];
        array[0] = new Object[]{Role.THIRD};
        array[1] = new Object[]{Role.FIFTH};
        array[2] = new Object[]{Role.SIXTH};
        array[3] = new Object[]{Role.SEVENTH};
        return array;
    }


    @DataProvider(name = "roles 5-10")
    private static Object[][] involveInCalculation() {
        Object[][] array = new Object[6][];
        array[0] = new Object[]{Role.FIFTH};
        array[1] = new Object[]{Role.SIXTH};
        array[2] = new Object[]{Role.SEVENTH};
        array[3] = new Object[]{Role.EIGHTH};
        array[4] = new Object[]{Role.NINTH};
        array[5] = new Object[]{Role.TENTH};
        return array;
    }

    @DataProvider(name = "roles 4-8")
    private static Object[][] positionStartDateChangingWithoutField() {
        Object[][] array = new Object[5][];
        array[0] = new Object[]{Role.FOURTH};
        array[1] = new Object[]{Role.FIFTH};
        array[2] = new Object[]{Role.SIXTH};
        array[3] = new Object[]{Role.SEVENTH};
        array[4] = new Object[]{Role.EIGHTH};
        return array;
    }

    @DataProvider(name = "roles 7-8")
    private static Object[][] startJobNotActive() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{Role.SEVENTH};
        array[1] = new Object[]{Role.EIGHTH};
        return array;
    }

    @DataProvider(name = "roles 4, 9-10")
    private static Object[][] bothJobsNotActive() {
        Object[][] array = new Object[3][];
        array[0] = new Object[]{Role.FOURTH};
        array[1] = new Object[]{Role.NINTH};
        array[2] = new Object[]{Role.TENTH};
        return array;
    }

    @DataProvider(name = "roles 1, 3")
    private static Object[][] kpiWithPermission() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{Role.FIRST};
        array[1] = new Object[]{Role.THIRD};
        return array;
    }

    @DataProvider(name = "roles 5-6")
    private static Object[][] endJobNotActive() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{Role.FIFTH};
        array[1] = new Object[]{Role.SIXTH};
        return array;
    }

    @DataProvider(name = "roles 3-4")
    private static Object[][] editScheduleDates() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{Role.THIRD};
        array[1] = new Object[]{Role.FOURTH};
        return array;
    }

    @DataProvider(name = "roles 3-5, 7")
    private static Object[][] downloadXLSX() {
        Object[][] array = new Object[4][];
        array[0] = new Object[]{Role.THIRD};
        array[1] = new Object[]{Role.FOURTH};
        array[2] = new Object[]{Role.FIFTH};
        array[3] = new Object[]{Role.SEVENTH};
        return array;
    }

    @DataProvider(name = "roles 3-6")
    private static Object[][] request() {
        Object[][] array = new Object[4][];
        array[0] = new Object[]{Role.THIRD};
        array[1] = new Object[]{Role.FOURTH};
        array[2] = new Object[]{Role.FIFTH};
        array[3] = new Object[]{Role.SIXTH};
        return array;
    }

}
