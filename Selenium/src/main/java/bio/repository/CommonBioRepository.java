package bio.repository;

import bio.components.client.ConfigLine;
import bio.models.PersonGroups;
import bio.models.Terminal;
import io.qameta.allure.Allure;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import utils.Links;
import utils.Projects;
import utils.authorization.ClientReturners;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static utils.Links.*;
import static utils.Params.ORG_UNIT_JSON;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class CommonBioRepository {

    public static final String BIO_URL = getTestProperty("central");

    /**
     * Берет путь для дерева выбора для оргюнита по его айди
     */
    public static List<String> getPath(Integer objId) {
        Integer parentId = objId;
        List<String> path = new LinkedList<>();
        while (parentId != null) {
            String urlEnding = makePath(PERSON_GROUPS, parentId);
            JSONObject someEmployeePositions = getJsonFromUri(Projects.BIO, BIO_URL, urlEnding);
            PersonGroups person = new PersonGroups(someEmployeePositions);
            path.add(person.getName());
            parentId = person.getParentId();
        }
        return path;
    }

    /**
     * Метод для поиска всех чилдренов оргюнита и далее рекурсивно для каждого оргюнита.
     *
     * @param personGroups     - целевой оргюнит
     * @param personGroupsList - список всех PersonGroups
     * @return - список айди оргюнитов
     */
    public static List<Integer> childSearch(PersonGroups personGroups, List<PersonGroups> personGroupsList) {
        List<Integer> omIdsWithHisChildren = new ArrayList<>(Collections.singleton(personGroups.getParentId()));
        List<PersonGroups> tempChild = personGroupsList.stream()
                .filter(personGroups1 -> personGroups1.getParentId() != null
                        && personGroups1.getParentId().equals(personGroups.getParentId()))
                .collect(Collectors.toList());
        for (PersonGroups groups : tempChild) {
            List<Integer> deepChild = childSearch(groups, personGroupsList);
            omIdsWithHisChildren.addAll(deepChild);
        }
        omIdsWithHisChildren.addAll(tempChild.stream().map(PersonGroups::getId).collect(Collectors.toList()));
        return omIdsWithHisChildren;
    }

    /**
     * Собирает пути для списка оргнюнитов чтобы выбрать сразу несколько элементов в дереве
     * @param ids - список айди оргюнитов для которых ищем путь
     */
    public static List<List<String>> getPathsList(List<Integer> ids) {
        List<List<String>> tempList = new ArrayList<>();
        for (Integer id : ids) {
            List<String> pathList = getPath(id);
            tempList.add(pathList);
        }
        return tempList;
    }

    /**
     * Возвращает случайный оргюнит у которого есть терминал и айди терминала
     */
    public static HashMap<String, String> getOrgUnitNameAttachedToTerminalId() {
        List<Terminal> terminals = TerminalRepository.getTerminals().stream()
                .filter(terminal -> !terminal.getPersonGroupIds().isEmpty())
                .collect(Collectors.toList());
        Terminal terminal = getRandomFromList(terminals);
        List<PersonGroups> personGroupsList = PersonGroupsRepository.getAllOfCurrentAttachedOrgUnitsNames(terminal.getPersonGroupIds());
        PersonGroups groups = getRandomFromList(personGroupsList);
        Allure.addAttachment("Выбор оргЮнита с терминалами", "Был выбран оргЮнит с именем " + groups
                + ", у которого имеется терминал с названием: " + terminal.getDescription());
        HashMap<String, String> terminalIdAndOrgUnit = new HashMap<>();
        //TODO можно возвращать 2 объекта Терминал и ПерсонГрупс, в некоторых случаях не придется делать лишние запросы
        terminalIdAndOrgUnit.put("id", terminal.getId());
        terminalIdAndOrgUnit.put(ORG_UNIT_JSON, groups.getName());
        return terminalIdAndOrgUnit;
    }

    /**
     * Возвразщает оргюнит и список сотруднико для него
     */
    public static Map<String, List<String>> getOMWithEmployeesAndIdOfEmployees() {
        Map<String, List<String>> nameMap = new HashMap<>();
        List<PersonGroups> personGroups = PersonGroupsRepository.getPersonGroups().stream()
                .filter(personGroups1 -> !personGroups1.getPersonPositions().isEmpty()).collect(Collectors.toList());
        //находим имя рандомного орюнита с сотрудниками
        PersonGroups randomPersonGroup = getRandomFromList(personGroups);
        List<Map<String, String>> persons = randomPersonGroup.getPersonPositions();
        List<String> temp = new ArrayList<>();
        for (Map<String, String> person : persons) {
            temp.addAll(person.keySet());
        }
        nameMap.put(randomPersonGroup.getName(), temp);
        //TODO здесь скорее всего можно возвращать только PersonGroups чтобы не делать лишних действий
        return nameMap;
    }

    /**
     * Берет текущую версию апи и возвращает в виде строки
     */
    public static String getCurrentVersionApi() {
        HttpUriRequest requestValues = RequestBuilder.get().setUri(Links.getTestProperty("terminal") + "/version.txt").build();
        HttpResponse response = null;
        try {
            response = ClientReturners.httpClientReturner(Projects.BIO).execute(requestValues);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert response != null;
        HttpEntity values = response.getEntity();
        String string = null;
        try {
            string = EntityUtils.toString(values);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return string;
    }

    /**
     * берет объект конфига терминала
     * @param terminalId  - айди терминала
     * @param line - вариант конфига из енама
     */
    public static Object getConfigObject(String terminalId, ConfigLine line) {
        String urlEnding = makePath(TERMINALS, terminalId, CONFIG);
        JSONObject temp = getJsonFromUri(Projects.BIO, BIO_URL, urlEnding);
        return temp.get(line.getName());
    }
}
