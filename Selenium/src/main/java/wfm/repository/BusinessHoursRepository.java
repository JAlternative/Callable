package wfm.repository;

import io.qameta.allure.Allure;
import org.json.JSONObject;
import org.testng.Assert;
import utils.Projects;
import wfm.PresetClass;
import wfm.components.schedule.ScheduleType;
import wfm.models.BusinessHours;

import java.util.List;
import java.util.NoSuchElementException;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.BUSINESS_HOURS;
import static utils.Links.ORGANIZATION_UNITS;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class BusinessHoursRepository {

    private BusinessHoursRepository() {
    }

    /**
     * Возвращает айди графика выбранного типа, если нужного графика нет, то меняет тип одного из графиков
     *
     * @param type - тип графика который нам нужен SALE/SERVICE
     * @return айди в виде строки
     */
    public static BusinessHours getAnyScheduleWithTypeWithPreset(ScheduleType type, int omId) {
        List<BusinessHours> temp = scheduleType(omId);
        BusinessHours schedules;
        try {
            schedules = temp.stream().filter(s -> s.getEnumType() == type).findAny()
                    .orElseThrow(() -> new AssertionError(String.format("%s Расписания типа \"%s\" нет в списке",
                                                                        NO_TEST_DATA, type.getNameOfType())));
        } catch (NoSuchElementException e) {
            schedules = temp.stream().findAny()
                    .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "В списке нет расписания"));
            PresetClass.preChangeType(type, schedules.getOrgUnitId());
        }
        Allure.addAttachment("График", String.format("Выбран график с типом \"%s\", id графика: %s",
                                                     type.getNameOfType(), schedules.getOrgUnitId()));
        return schedules;
    }

    /**
     * Смотрит расписания для текущего оргюнита собирает их
     */
    public static List<BusinessHours> scheduleType(int omId) {
        String urlEnding = makePath(ORGANIZATION_UNITS, omId, BUSINESS_HOURS);
        JSONObject temp = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        return getListFromJsonObject(temp, BusinessHours.class);
    }

    /**
     * Проверяет есть ли расписания
     *
     * @return список расписаний
     */
    public static List<BusinessHours> checkForAvailability(int omId) {
        List<BusinessHours> schedules = scheduleType(omId);
        Assert.assertNotEquals(0, schedules.size(), "schedule message. У текущего ОМ нет графика работы");
        return schedules;
    }
}
