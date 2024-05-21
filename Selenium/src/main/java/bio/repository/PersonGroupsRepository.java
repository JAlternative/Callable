package bio.repository;

import bio.models.PersonGroups;
import com.mchange.util.AssertException;
import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.Pairs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static bio.repository.CommonBioRepository.BIO_URL;
import static utils.Links.PERSON_GROUPS;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class PersonGroupsRepository {

    /**
     * Берет все оргюниты
     */
    public static List<PersonGroups> getPersonGroups() {
        List<NameValuePair> pairs = Pairs.newBioBuilder().size(100000).build();
        JSONObject temp = getJsonFromUri(Projects.BIO, BIO_URL, PERSON_GROUPS, pairs);
        return getListFromJsonObject(temp, PersonGroups.class);
    }

    /**
     * Берет все оргюниты которые написаны в строке ";" через  на UI
     */
    public static PersonGroups getRandOmOrgNameFromRestriction(String restrictedOrgName) {
        List<String> allOrgName = new ArrayList<>(Arrays.asList(restrictedOrgName.split(";")));
        String randomFromList = getRandomFromList(allOrgName);
        return getPersonGroups().stream().filter(personGroups -> personGroups.getName().equals(randomFromList))
                .findAny()
                .orElseThrow(() -> new AssertException("На нашли в апи группу с названием: " + randomFromList));
    }

    /**
     * Берет подразделение по его айди
     */
    public static PersonGroups getOrgUnitByHisId(int id) {
        String urlEnding = makePath(PERSON_GROUPS, id);
        JSONObject tempObj = getJsonFromUri(Projects.BIO, BIO_URL, urlEnding);
        return new PersonGroups(tempObj);
    }

    /**
     * Возвращает список подразделений по списку айди подразделений
     */
    public static List<PersonGroups> getAllOfCurrentAttachedOrgUnitsNames(List<Integer> orgUnitsIds) {
        List<PersonGroups> personGroups = new ArrayList<>();
        for (Integer orgUnitsId : orgUnitsIds) {
            personGroups.add(getOrgUnitByHisId(orgUnitsId));
        }
        return personGroups;
    }

    /**
     * Берет оргюнит по его имени
     */
    public static PersonGroups getOrgUnitIdByName(String name) {
        List<PersonGroups> personGroups = getPersonGroups();
        return personGroups.stream()
                .filter(personGroups1 -> personGroups1.getName().equals(name))
                .findFirst().orElseThrow(() -> new AssertionError("Не был найден PersonGroups с именем " + name));
    }
}
