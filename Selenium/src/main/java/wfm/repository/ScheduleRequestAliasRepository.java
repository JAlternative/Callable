package wfm.repository;

import org.json.JSONObject;
import utils.Links;
import utils.Projects;
import utils.tools.CustomTools;
import wfm.components.schedule.ScheduleRequestType;
import wfm.components.systemlists.IntervalType;
import wfm.models.ScheduleRequestAlias;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.tools.CustomTools.randomItem;
import static utils.tools.RequestFormers.*;

public abstract class ScheduleRequestAliasRepository {

    private ScheduleRequestAliasRepository() {
    }

    private static final String NOT_FOUND_MESSAGE = NO_TEST_DATA + "Тип запроса \"%s\" не найден";

    public static List<ScheduleRequestAlias> getAllAliases() {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(Links.SCHEDULE_REQUESTS, Links.ALIAS));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        return CustomTools.getListFromJsonObject(json, ScheduleRequestAlias.class);
    }

    public static ScheduleRequestAlias getAlias(ScheduleRequestType requestType) {
        List<ScheduleRequestAlias> types = getAllAliases().stream()
                .filter(t -> t.getTitle().equals(requestType.getName()))
                .collect(Collectors.toList());
        if (types.isEmpty()) {
            throw new NoSuchElementException(String.format(NOT_FOUND_MESSAGE, requestType.getName()));
        }
        ScheduleRequestAlias type = types.stream()
                .filter(e -> e.getType().equals(requestType.toString()))
                .findFirst()
                .orElse(types.get(0));
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(Links.SCHEDULE_REQUESTS, Links.ALIAS, type.getAlias()));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        return CustomTools.getClassObjectFromJson(ScheduleRequestAlias.class, json);
    }
    
    public static ScheduleRequestAlias getAliasByName(String name) {
        return getAllAliases().stream()
                .filter(e -> e.getTitle().equals(name))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format(NOT_FOUND_MESSAGE, name)));
    }

    public static ScheduleRequestAlias getRandomEnabledAlias() {
        return getAllAliases()
                .stream()
                .filter(ScheduleRequestAlias::isEnabled)
                .collect(randomItem());
    }

    /**
     * Проверить, существует ли в системных списках "Тип запроса расписания"
     * с таким же типом и названием, получаем ответ или null
     */
    public static ScheduleRequestAlias getAliasTypeRequestSchedule(ScheduleRequestType type) {
        return ScheduleRequestAliasRepository.getAllAliases().stream()
                .filter(t -> t.getType().equals(type.toString()))
                .filter(n -> n.getTitle().equals(type.getName()))
                .findFirst().orElse(null);
    }

    public static ScheduleRequestAlias getAliasTypeRequestSchedule(ScheduleRequestType type, IntervalType intervalType) {
        return ScheduleRequestAliasRepository.getAllAliases().stream()
                .filter(t -> t.getType().equals(type.toString()))
                .filter(n -> n.getTitle().equals(type.getName() + "_" + intervalType.toString()))
                .filter(i -> i.getIntervalType().equals(intervalType.toString()))
                .findFirst().orElse(null);
    }
}
