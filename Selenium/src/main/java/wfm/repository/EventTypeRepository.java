package wfm.repository;

import org.json.JSONObject;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.RequestFormers;
import wfm.components.schedule.EventType;

import java.net.URI;
import java.util.List;

import static utils.Links.ORGANIZATION_UNIT_EVENT_TYPES;

public class EventTypeRepository {

    private EventTypeRepository() {}

    public static List<EventType> getAllEventTypes() {
        URI uri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, ORGANIZATION_UNIT_EVENT_TYPES);
        JSONObject object = RequestFormers.getJsonFromUri(Projects.WFM, uri);
        return CustomTools.getListFromJsonObject(object, EventType.class);
    }
}
