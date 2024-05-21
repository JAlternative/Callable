package wfm.components.schedule;

import java.util.ArrayList;
import java.util.List;

import static utils.tools.CustomTools.getRandomFromList;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public enum EmployeeParams {
    REPLACES_EMPLOYEES("Заменяет сотрудников в отгуле/отпуске", 66),
    SCHEDULE_OPTIONS("Варианты графиков работы сотрудников (5-2, 5-2 с выходными,2-2…)", 65),
    TESTING_PARAMS("Параметры для тестирования", 198),
    SCHEDULE_DELETE("Разрешить удаление лишних смен", 160),
    FIXES_DAYS("Фиксированные дни недели, через запятую", 192),
    TABLE_MODE_CREATE("Тип формирования табеля (из планового графика, или из био)", 309);
    private final String name;
    private final int id;

    EmployeeParams(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public static EmployeeParams getRandomSimpleParam() {
        List<EmployeeParams> params = new ArrayList<>();
        params.add(EmployeeParams.SCHEDULE_DELETE);
        params.add(EmployeeParams.FIXES_DAYS);
        params.add(EmployeeParams.TESTING_PARAMS);
        params.add(EmployeeParams.REPLACES_EMPLOYEES);
        params.add(EmployeeParams.SCHEDULE_OPTIONS);
        return getRandomFromList(params);
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}
