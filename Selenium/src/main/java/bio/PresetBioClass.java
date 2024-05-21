package bio;

import bio.components.client.EmployeeStatus;
import bio.components.client.LicenseType;
import bio.components.client.TerminalStatus;
import bio.components.terminal.CheckBoxAndStatus;
import bio.models.*;
import bio.repository.FaceDescriptorsRepository;
import bio.repository.JournalRepository;
import bio.repository.PersonGroupsRepository;
import bio.repository.PersonRepository;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import utils.Projects;
import utils.authorization.ClientReturners;
import utils.tools.Pairs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static utils.Links.*;
import static utils.Params.DESCRIPTION;
import static utils.Params.PERSON_GROUP_IDS;
import static utils.tools.CustomTools.encoder;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.RequestFormers.*;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class PresetBioClass {

    private static final String BIO_URL = getTestProperty("central");
    private static final Logger LOG = LoggerFactory.getLogger(PresetBioClass.class);

    /**
     * Добавляет разрешение для пользователя
     *
     * @param userID     - айди сотрудника
     * @param permission - разрешение
     * @param orgIds     - список оргюнитов для которых будет установленно разрешение
     */
    public static void addPermissionsPreset(String userID, String permission, Integer[] orgIds) {
        String urlEnding = makePath(PERSONS, userID, PERMISSIONS);
        URI uri = setUri(Projects.BIO, BIO_URL, urlEnding);
        JSONArray miniArray = new JSONArray();
        JSONObject miniObject = new JSONObject();
        miniObject.put("key", permission);
        JSONObject value = new JSONObject();
        if (orgIds != null) {
            value.put(PERSON_GROUP_IDS, orgIds);
        } else
            value.put(PERSON_GROUP_IDS, ((Object) null));
        miniObject.put("value", value);
        miniArray.put(miniObject);
        requestMaker(uri, miniArray, RequestBuilder.post(),
                ContentType.create("application/json", Consts.UTF_8), Projects.BIO);
        LOG.info("На добавление разрешения: {} был отправлен запрос: {} ", permission, uri);
    }

    /**
     * Удалает у пользователя список разрешений
     *
     * @param userID      - айди пользователя
     * @param permissions - список разрешений
     */
    public static void deletePermissionsPreset(String userID, List<String> permissions) {
        String urlEnding = makePath(PERSONS, userID, PERMISSIONS);
        URI uri = setUri(Projects.BIO, BIO_URL, urlEnding);
        for (String text : permissions) {
            JSONArray miniArray = new JSONArray();
            JSONObject miniObject = new JSONObject();
            miniObject.put("permission", text);
            miniArray.put(miniObject);
            requestMaker(uri, miniArray, RequestBuilder.delete(),
                    ContentType.create("application/hal+json", Consts.UTF_8), Projects.BIO);
            LOG.info("На удаление разрешения {} был отправлен запрос {} на адрес {} ", text, miniArray.toString(), uri);
        }
    }

    /**
     * Загружает для сотрудника фотографию
     */
    public static void uploadPhotoPreset(Person person) {
        String urlEnding = makePath(FACE_DESCRIPTORS, ENCODING);
        URI uri = setUri(Projects.BIO, BIO_URL, urlEnding);
        JSONObject miniObject = new JSONObject();
        miniObject.put("personId", person.getId());
        File folder = new File("src/test/resources/img/");
        File[] files = folder.listFiles();
        miniObject.put("image", encoder(files != null ? files[new Random().nextInt(files.length)] : new File("")));
        miniObject.put("tolerance", 0.0);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(),
                ContentType.create("application/hal+json", Consts.UTF_8), Projects.BIO);
        assertStatusCode(response, uri.toString());
    }

    /**
     * Подтверждает создание дескриптора распознавания
     *
     * @param ids - айди дескрипторов для подтверждения
     */
    public static void confirmPreset(List<Integer> ids) {
        String urlEnding = makePath(FACE_DESCRIPTORS, "confirm");
        List<NameValuePair> pairs = Pairs.newBioBuilder()
                .ids(ids.stream().map(String::valueOf).collect(Collectors.joining(","))).build();
        URI uri = setUri(Projects.BIO, BIO_URL, urlEnding, pairs);
        HttpUriRequest some = RequestBuilder.post()
                .setUri(uri)
                .build();
        HttpResponse response = null;
        try {
            response = ClientReturners.httpClientReturner(Projects.BIO).execute(some);
            LOG.info("POST {}  {}", response.getStatusLine().getStatusCode(), uri.toString());
        } catch (IOException e) {
            LOG.error("Не выполнился запрос", e);
        }
        assertStatusCode(response, uri.toString());
    }

    @Step("Отправить файл {type.licenseName} лицензии через post запрос")
    public static String uploadFileGetResponse(LicenseType type) {
        File testUploadFile = new File("src/test/resources/bio_license/" + type.getFileName() + ".lic");
        String urlEnding = makePath(LICENSE, EVALUATE);
        URI uri = setUri(Projects.BIO, BIO_URL, urlEnding);
        HttpEntity postData = MultipartEntityBuilder.create()
                .addBinaryBody("license", testUploadFile)
                .build();
        HttpUriRequest postRequest = RequestBuilder
                .post()
                .setUri(uri)
                .setEntity(postData)
                .build();
        HttpResponse response = null;
        String jsonObject = "";
        try {
            response = ClientReturners.httpClientReturner(Projects.BIO).execute(postRequest);
            HttpEntity values = response.getEntity();
            jsonObject = EntityUtils.toString(values);

        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(response != null ? response.getStatusLine().getStatusCode() : 0, 200,
                "Не удалось загрузить лицензию");
        Allure.addAttachment("Выгрузка файла", "Файл был успешно добавлен");
        return jsonObject;
    }

    /**
     * Пресет для добавления или удаления сотрудника с терминала/на терминал
     *
     * @param status     - добавить или удалить
     * @param terminalId - айди терминала
     * @param employeeId - айди сотрудника
     */
    public static void presetForMakeEmployeeWithType(EmployeeStatus status, String terminalId, String employeeId) {
        String path = makePath(TERMINALS, terminalId);
        Pairs.BioBuilder pairs = Pairs.newBioBuilder();
        switch (status) {
            case ADMIN:
                path = makePath(path, ADD_PERSON, employeeId);
                pairs.exception("ADMIN");
                break;
            case REMOVED:
                path = makePath(path, REMOVE_PERSON, employeeId);
                break;
            case INCLUDE:
                path = makePath(path, ADD_PERSON, employeeId);
                pairs.exception("INCLUDE");
                break;
        }
        URI uri = setUri(Projects.BIO, BIO_URL, path, pairs.build());
        HttpResponse response = requestMaker(uri, new JSONObject(), RequestBuilder.post(),
                ContentType.create("application/hal+json", Consts.UTF_8), Projects.BIO);
        assertStatusCode(response, uri.toString());
    }

    @Step("Пресет. Проверить что текущий статус терминала удовлетворяет условию {status}, если нет, то изменить.")
    public static void presetForManageTerminalStatus(TerminalStatus status, String terminalId) {
        String urlEnding = makePath(TERMINALS, terminalId);
        URI uri = setUri(Projects.BIO, BIO_URL, urlEnding);
        JSONObject temp = new JSONObject(setUrlAndInitiateForApi(uri, Projects.BIO));
        Terminal terminal = new Terminal(temp);
        if (!terminal.getBlockingStatus().contains(status.toString())) {
            JSONObject miniObject = new JSONObject();
            miniObject.put("id", terminalId);
            miniObject.put("serialNumber", terminal.getSerialNumber());
            miniObject.put(DESCRIPTION, terminal.getDescription());
            miniObject.put("blockingStatus", status.toString());
            miniObject.put(PERSON_GROUP_IDS, terminal.getPersonGroupIds());
            Allure.addAttachment("Пресет",
                    "Статус терминала был сменен в пресете на: " + status.toString());
            HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(),
                    ContentType.create("application/hal+json", Consts.UTF_8), Projects.BIO);
            assertStatusCode(response, uri.toString());
        } else
            Allure.addAttachment("Пресет",
                    "Статус терминала не был сменен в пресете. Он уже удовлетворяет условию: " + status.toString());
    }

    @Step("Пресет. Выбрать случайного сотрудника  проверить что его текущий статус в терминале {status}," +
            " если его статус не такой, то поменять в пресете")
    public static ImmutablePair<Person, PersonGroups> presetForNeedTypeOfEmployee(EmployeeStatus status, String terminalId) {
        Person temp = PersonRepository.getRandomEmployeeId();
        presetForMakeEmployeeWithType(status, terminalId, temp.getId());
        Allure.addAttachment("Пресет",
                "Был выбран сотрудник с именем " + temp.getFullName() + ", его текущий статус " + status.getStatus());
        PersonGroups orgUnitByHisId = PersonGroupsRepository.getOrgUnitByHisId(temp.getPersonGroupPositions().get(0).getPersonGroupId());
        //  orgUnitByHisId.getName();
        return new ImmutablePair<>(temp, orgUnitByHisId);
    }

    /**
     * Проверяет что у сотрудника есть дескрипторы. Если их нет, то загружает фото
     *
     * @param person  - сотрудник
     * @param isPhoto - с фотографией
     */
    public static int checkPositiveNumberOfDescriptors(Person person, boolean isPhoto) {
        long number = FaceDescriptorsRepository.getDescriptors(person).stream()
                .filter(descriptor -> descriptor.getSrcUrl() != null || !isPhoto).count();
        if (number == 0) {
            uploadPhotoPreset(person);
            number = 1;
        }
        List<Integer> notConfirmedIds = FaceDescriptorsRepository.getDescriptors(person).stream()
                .filter(d -> !d.isConfirmed()).map(FaceDescriptors::getId).collect(Collectors.toList());
        if (notConfirmedIds.size() > 0) confirmPreset(notConfirmedIds);
        return Math.toIntExact(number);
    }

    @Step("ПРЕСЕТ. Выбрать случайный оргюнит с параметрами")
    public static HashMap<String, List<CheckBoxAndStatus>> getRandomOrgNameWithParameters(LocalDate startDate,
                                                                                          LocalDate endDate) {
        List<Journal> allUsersInJournal = JournalRepository.getJournals(startDate, endDate,
                Collections.singletonList(CheckBoxAndStatus.ALL), "");
        Journal model = allUsersInJournal
                .stream().filter(j -> j.getPersonGroups().size() > 1 |
                        (!j.getPersonGroups().contains("1") && j.getPersonGroups().size() == 1)).findFirst()
                .orElseThrow(() -> new AssertionError("Не были найдены оргЮниты в указанную дату"));
        List<String> personGroups = model.getPersonGroups();
        personGroups.remove("1");
        String personGroupId = getRandomFromList(personGroups);
        List<Journal> needJournal = JournalRepository.getJournals(startDate, endDate,
                Collections.singletonList(CheckBoxAndStatus.ALL), personGroupId);
        Set<String> strings = needJournal.stream().map(Journal::getPurpose).collect(Collectors.toSet());
        List<CheckBoxAndStatus> statuses = strings.stream().map(CheckBoxAndStatus::getByApiStatus)
                .filter(checkBoxAndStatus -> checkBoxAndStatus != CheckBoxAndStatus.ALL)
                .collect(Collectors.toList());
        String orgName = PersonGroupsRepository.getOrgUnitByHisId(Integer.parseInt(personGroupId)).getName();
        LOG.info("Выбран орюнит под названием {}", orgName);
        Allure.addAttachment("Выбор оргюнита", "В соответствии с параметрами были выбраны: " +
                "\nОргюнит: " + orgName +
                "\nДата от: " + startDate.toString() +
                "\nДата до: " + endDate.toString() +
                "\nПараметры: " + CheckBoxAndStatus.getStatusesAttachment(statuses));
        HashMap<String, List<CheckBoxAndStatus>> temp = new HashMap<>();
        temp.put(orgName, statuses);
        return temp;
    }
}
