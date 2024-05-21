package wfm.repository;

import org.json.JSONObject;
import utils.Projects;
import wfm.components.schedule.SystemProperties;
import wfm.models.SystemProperty;

import java.util.List;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.SYSTEM_PROPERTIES;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class SystemPropertyRepository {

    private SystemPropertyRepository() {}

    /**
     * Ищет в списке системных настроек нужную нам
     *
     * @param sysProp            настройка, которую ищем
     * @param systemPropertyList список системных настроек
     * @return - системная настройка
     */
    public static SystemProperty getSystemPropertyFromList(SystemProperties sysProp, List<SystemProperty> systemPropertyList) {
        return systemPropertyList.stream()
                .filter(systemProperty -> systemProperty.getKey().equals(sysProp.getKey()))
                .findAny()
                .orElseThrow(() -> new AssertionError(String.format("%sСистемная настройка %s не была найдена", NO_TEST_DATA, sysProp.getKey())));
    }

    /**
     * Взять все системные настройки
     */
    public static List<SystemProperty> getSystemProperties() {
        JSONObject system = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, SYSTEM_PROPERTIES);
        return getListFromJsonObject(system, SystemProperty.class);

    }

    /**
     * Возвращает указанную в енаме системную настройку
     */
    public static SystemProperty getSystemProperty(SystemProperties sysProp) {
        List<SystemProperty> systemPropertyList = getSystemProperties();
        return getSystemPropertyFromList(sysProp, systemPropertyList);
    }

}
