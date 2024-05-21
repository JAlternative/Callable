package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.Pairs;
import wfm.PresetClass;
import wfm.models.OrganizationUnitEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static utils.Links.ORGANIZATION_UNIT_EVENTS;
import static utils.tools.LocalDateTools.getLastDate;
import static utils.tools.LocalDateTools.now;
import static utils.tools.RequestFormers.getJsonFromUri;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class OrganizationUnitEventRepository {

    private OrganizationUnitEventRepository() {}

    /**
     * Выбирает массив событий с текущей даты по последнюю дату месяца
     *
     * @param eventIsRepeatable выбирать повторяющееся или неповторяющееся событие
     */
    public static List<OrganizationUnitEvent> eventsChoice(boolean eventIsRepeatable, int omId) {
        List<OrganizationUnitEvent> eventsList = getOrganizationUnitEvents(now(), getLastDate(), omId)
                .stream()
                .filter(oue -> (eventIsRepeatable && oue.getRepeatRule() != null)
                        || (!eventIsRepeatable && oue.getRepeatRule() == null))
                .collect(Collectors.toList());
        if (eventsList.isEmpty()) {
            PresetClass.presetEvent(omId, eventIsRepeatable);
            eventsList = eventsChoice(eventIsRepeatable, omId);
        }
        return eventsList;
    }

    /**
     * Из массива событий формирует лист событий
     *
     * @param dateStart дата начала поиска событий
     * @param dateEnd   дата окончания поиска событий
     * @param omNumber  оргюнита
     */
    public static List<OrganizationUnitEvent> getOrganizationUnitEvents(LocalDate dateStart, LocalDate dateEnd, int omNumber) {
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .from(dateStart)
                .orgUnitIds(omNumber)
                .to(dateEnd)
                .build();
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, ORGANIZATION_UNIT_EVENTS, nameValuePairs);
        return CustomTools.getListFromJsonObject(someObject, OrganizationUnitEvent.class);
    }
}
