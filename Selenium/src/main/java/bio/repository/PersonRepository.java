package bio.repository;

import bio.models.Person;
import io.qameta.allure.Allure;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Projects;
import utils.tools.Pairs;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static bio.repository.CommonBioRepository.BIO_URL;
import static utils.Links.PERSONS;
import static utils.Links.TERMINALS;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.*;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class PersonRepository {

    private static final Logger LOG = LoggerFactory.getLogger(PersonRepository.class);

    /**
     * Берет 1000 сотрудников
     */
    public static List<Person> getPersons() {
        List<NameValuePair> pairs = Pairs.newBioBuilder().active(true).size(1000).build();
        JSONObject temp = getJsonFromUri(Projects.BIO, BIO_URL, PERSONS, pairs);
        return getListFromJsonObject(temp, Person.class);
    }

    /**
     * Берет сотрудников терминала
     *
     * @param terminalId - айди терминала
     */
    public static List<Person> getTerminalPersons(String terminalId) {
        String urlEnding = makePath(TERMINALS, terminalId, PERSONS);
        List<NameValuePair> pairs = Pairs.newBioBuilder().size(1000).build();
        URI uri = setUri(Projects.BIO, BIO_URL, urlEnding, pairs);
        JSONArray temp = new JSONArray(setUrlAndInitiateForApi(uri, Projects.BIO));
        LOG.info("Количество людей: {}", temp.length());
        return getListFromJsonArray(temp, Person.class);
    }

    /**
     * Берет случайного сотрудника который прикреплен к оргюниту айди которого не 1 (там проблемы с ним)
     */
    public static Person getRandomUsersBio() {
        List<Person> people = getPersons();
        List<Person> users = people.stream().filter(person -> person.isRecognizable() && person.getPersonGroupPositions() != null)
                .filter(person -> !person.getId().equals("1")).collect(Collectors.toList());
        Person person = getRandomFromList(users);
        Allure.addAttachment("Выбор пользователя", "Был выбран пользователь с именем: " + person.getFullName() + "\n" +
                "Имеющий дискриптор и прикрепленный к оргюниту");
        return person;
    }

    /**
     * Берет сотрудника по его айди
     */
    public static Person getPersonById(String id) {
        String urlEnding = makePath(PERSONS, id);
        JSONObject temp = getJsonFromUri(Projects.BIO, BIO_URL, urlEnding);
        return new Person(temp);
    }

    /**
     * просто берет случаного сотрудника у которого есть фамилия и оргюнит
     */
    public static Person getRandomEmployeeId() {
        List<Person> people = getPersons();
        List<Person> newPersons = people.stream()
                .filter(person -> person.getPatronymicName() != null && person.getPersonGroupPositions() != null)
                .collect(Collectors.toList());
        return getRandomFromList(newPersons);
    }

    /**
     * Берет сотрудников и возвращает только тех у кого есть аккаунт пользователя
     */
    public static List<Person> getAllUsersWithUserName() {
        return getPersons().stream().filter(Person::isUserNotNull).collect(Collectors.toList());
    }

    /**
     * @return Возвращает всех сотрууднников, работающих в ом по их id
     */
    public static List<Person> getAllEmployeesOfOM(Map<String, List<String>> map) {
        String nameOfOm = map.keySet().stream().findFirst().orElse(null);
        return getPersons().stream()
                .filter(person -> person.getPersonGroupPositions() != null && person.getAllPersonGroupName().contains(nameOfOm))
                .collect(Collectors.toList());
    }
}
