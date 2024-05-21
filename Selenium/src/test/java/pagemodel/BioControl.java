package pagemodel;

import bio.PresetBioClass;
import bio.components.client.*;
import bio.components.terminal.CheckBoxAndStatus;
import bio.models.Journal;
import bio.models.Person;
import bio.models.PersonGroups;
import bio.models.Terminal;
import bio.repository.*;
import com.google.inject.Inject;
import com.mchange.util.AssertException;
import guice.TestModule;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.HttpResponse;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.BioPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.BaseTest;
import testutils.TreeNavigate;
import utils.Projects;
import utils.authorization.CsvLoader;
import utils.downloading.FileDownloadCheckerForBio;
import utils.downloading.TypeOfFiles;
import utils.downloading.TypeOfPhotos;
import utils.tools.CustomTools;
import utils.tools.LocaleKeys;
import wfm.components.utils.Direction;
import wfm.components.utils.Role;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static common.Groups.*;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.Links.getTestProperty;
import static utils.Params.ORG_UNIT_JSON;
import static utils.authorization.CookieRW.getCookieWithCheck;
import static utils.downloading.FileDownloadChecker.getFileNameExtensionFromResponse;
import static utils.dropTestTools.DropLoginTestReadWriter.fileNameReturner;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.CustomTools.systemSleep;
import static utils.tools.RequestFormers.assertStatusCode;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class BioControl extends BaseTest {

    private static final String BIO_URL = getTestProperty("central");
    private static final Logger LOG = LoggerFactory.getLogger(BioControl.class);
    private static final String URL_LOGIN = BIO_URL + "/#/auth";
    private static final String URL_HOME = BIO_URL + "/#/workpanel";
    private static final String URL_TERMINAL = BIO_URL + "/#/manageterminals";
    private static final String URL_PERSONAL = BIO_URL + "/#/managepersons";
    private static final String URL_USERS = BIO_URL + "/#/manageusers";
    private static final String URL_JOURNAL = BIO_URL + "/#/detectionjournal";
    private static final String URL_LICENCE = BIO_URL + "/#/licencemanagment";

    @Inject
    private BioPage bp;

    @DataProvider(name = "licenseCheck")
    private static Object[][] licenseTests() {
        Object[][] array = new Object[4][];
        array[0] = new Object[]{LicenseType.EXPIRED};
        array[1] = new Object[]{LicenseType.FUTURE_ACTIVE};
        array[2] = new Object[]{LicenseType.NOT_FOUND};
        array[3] = new Object[]{LicenseType.ACTIVE};
        return array;
    }

    @DataProvider(name = "UnitList")
    private static Object[][] orgUnitToCheckData() {
        Object[][] array = new Object[27][2];
        array[0] = new Object[]{ConfigLine.CENTRAL_SERVER, RandomStringUtils.randomAlphabetic(8)};
        array[1] = new Object[]{ConfigLine.TERMINAL_ID, RandomStringUtils.randomAlphabetic(8)};
        array[2] = new Object[]{ConfigLine.DELAY_BEFORE_CAPTURE, new Random().nextInt(9) + 1};
        array[3] = new Object[]{ConfigLine.EVENT_SELECTOR_MODE, "ONE_EVENT"};
        array[4] = new Object[]{ConfigLine.EVENT_SELECTOR_MODE, "TWO_EVENT"};
        array[5] = new Object[]{ConfigLine.EVENT_SELECTOR_MODE, "FOUR_EVENT"};
        array[6] = new Object[]{ConfigLine.WORKING_MODE, "null"};
        array[7] = new Object[]{ConfigLine.WORKING_MODE, "automatic"};
        array[8] = new Object[]{ConfigLine.RECOGNITION_METHOD, "SIMPLE"};
        array[9] = new Object[]{ConfigLine.RECOGNITION_METHOD, "TWO_CAMERAS_FAST"};
        array[10] = new Object[]{ConfigLine.SHOW_CONNECTION_STATUS, true};
        array[11] = new Object[]{ConfigLine.SHOW_CONNECTION_STATUS, false};
        array[12] = new Object[]{ConfigLine.USE_DELAY, true};
        array[13] = new Object[]{ConfigLine.USE_DELAY, false};
        array[14] = new Object[]{ConfigLine.DELAY_COUNT, new Random().nextInt(9) + 1};
        array[15] = new Object[]{ConfigLine.ROTATED1, true};
        array[16] = new Object[]{ConfigLine.ROTATED1, false};
        array[17] = new Object[]{ConfigLine.ROTATED2, true};
        array[18] = new Object[]{ConfigLine.ROTATED2, false};
        array[19] = new Object[]{ConfigLine.SUPPORT_EMAIL, RandomStringUtils.randomAlphabetic(6) + "@mail.ru"};
        array[20] = new Object[]{ConfigLine.SHOW_ONE_CAM_ONLY, false};
        array[21] = new Object[]{ConfigLine.SHOW_ONE_CAM_ONLY, true};
        array[22] = new Object[]{ConfigLine.BOTTOM_FRAUD, "lips"};
        array[23] = new Object[]{ConfigLine.BOTTOM_FRAUD, "chin"};
        array[24] = new Object[]{ConfigLine.IMAGES_JOURNAL_DELIVERY, "all"};
        array[25] = new Object[]{ConfigLine.IMAGES_JOURNAL_DELIVERY, "none"};
        array[26] = new Object[]{ConfigLine.IMAGES_JOURNAL_DELIVERY, "error_only"};
        return array;
    }

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void tearDown() {
        bp.getWrappedDriver().close();
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void setUp() {
        bp.getWrappedDriver().manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    @Step("Перейти по URL: {url}")
    private void goToLogin(String url) {
        bp.getWrappedDriver().get(url);
        LOG.info("Перешли по URL {}", url);
    }

    @Step("Авторизоваться под пользователем: {username}, с паролем {password}")
    private void loginStep(String username, String password) {
        bp.bioLogin().loginField()
                .waitUntil("Форма для логина не отображена", DisplayedMatcher.displayed(), 40);
        bp.bioLogin().loginField().clear();
        bp.bioLogin().loginField().sendKeys(username);
        bp.bioLogin().passwordField().clear();
        bp.bioLogin().passwordField().sendKeys(password);
        LOG.info("Ввели логин {} и пароль {}", username, password);
    }

    @Step("Нажать на кнопку \"Войти\"")
    private void loginButtonClick() {
        bp.bioLogin().loginButton().waitUntil("Кнопка \"Войти\" недоступна",
                                              DisplayedMatcher.displayed(), 20);
        bp.bioLogin().loginButton().click();
        LOG.info("Нажали на кнопку \"Войти\"");
    }

    @Step("Проверить, что пользователь авторизовался")
    private void checkSuccessLogin() {
        Assert.assertTrue(!bp.home().sectionsList().isEmpty(), "На главной странице отсутствуют разделы");
        List<String> sectionNames = bp.home().sectionsList().stream()
                .map(s -> s.getAttribute("textContent"))
                .sorted()
                .collect(Collectors.toList());
        List<String> variants = Arrays.stream(VariantsSection.values())
                .map(VariantsSection::getSectionName)
                .sorted()
                .collect(Collectors.toList());
        LOG.info("Отображены следующие разделы: {}", sectionNames);
        Allure.addAttachment("Проверка авторизации", String.format("Отображены следующие разделы: %s", sectionNames));
        Assert.assertTrue(sectionNames.equals(variants), String.format("Разница между ожидаемыми и фактическими разделами: %s",
                                                                       new ArrayList<>(CollectionUtils.disjunction(variants, sectionNames))));
        LOG.info("Авторизация прошла успешно");
    }

    @Step("Очистка информации о старых запусках авторизации")
    private void cleanFile(String fileName) {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.print("");
            writer.flush();
        } catch (Exception e) {
            LOG.info("Очистка информации о старых запусках авторизации");
        }
    }

    @Step("Перейти в раздел  по URL: {url}")
    private void goToBioSection(String url, boolean withWait) {
        Cookie cookie = getCookieWithCheck(Projects.BIO);
        bp.open(url);
        bp.getWrappedDriver().manage().addCookie(cookie);
        bp.getWrappedDriver().navigate().refresh();
        if (withWait) {
            bp.bioHeader().waitUntil("Хедер не загрузился", DisplayedMatcher.displayed(), 60);
        }
        //bp.(Matchers.equalTo(url));
    }

    @Step("Нажать кнопку выхода")
    private void clickExitButton() {
        bp.bioHeader().exitButton().click();
        LOG.info("Нажали на кнопку выхода");
    }

    @Step("Проверить, что был осуществлен выход из системы")
    private void assertExit() {
        bp.bioLogin().should("Форма логина все еще не отображается", DisplayedMatcher.displayed(), 20);
        String currentUrl = bp.getWrappedDriver().getCurrentUrl();
        LOG.info("Проверяем, что вышли из системы");
        Assert.assertFalse(currentUrl.endsWith(URL_HOME), "Строка адреса не изменилась");
        Assert.assertTrue(currentUrl.endsWith(URL_LOGIN), "Строка адреса не изменилась на авторизацию");
        Allure.addAttachment("Изменение строки адреса", String.format("Строка адреса изменилась на %s", URL_LOGIN));

    }

    @Step("Выполнить переход по дереву")
    private void workWithTree(List<List<String>> pathsList, Direction direction) {
        bp.bioHeader().waitUntil("Хедер не загрузился", DisplayedMatcher.displayed(), 200);
        TreeNavigate navigate = new TreeNavigate(pathsList);
        navigate.workWithTree(bp.terminals(), direction);
    }

    @Step("Выполнить переход по дереву в поп-ап меню пользователей")
    private void workWithTreeInPopUpWindow(List<List<String>> pathsList, Direction direction) {
        bp.personPopUp()
                .waitUntil("Страница не успела подгрузиться", DisplayedMatcher.displayed(), 10);
        TreeNavigate navigate = new TreeNavigate(pathsList);
        navigate.workWithTree(bp.personPopUp(), direction);
    }

    /**
     * Проверяем наличие пользователя в АПИ по имени и логину.
     *
     * @param name  - имя пользователя
     * @param login - логин пользователя
     * @param s     - сообщение если не совпадает логин и юзернейм
     * @return true если нашли пользователя
     */
    private boolean userCheckInApi(String name, String login, String s) {
        boolean weCheckThat = false;
        List<Person> people = PersonRepository.getPersons();
        for (Person tempPerson : people) {
            String tempName = (tempPerson.getFullName());
            if (name.equals(tempName)) {
                if (tempPerson.isUserNotNull()) {
                    String username = tempPerson.getUsername();
                    Assert.assertEquals(username, login, s);
                    weCheckThat = true;
                    break;
                }
            }
        }
        return weCheckThat;
    }

    /**
     * Проверяем наличие пользователя на UI по имени и логину.
     *
     * @param name  - имя пользователя
     * @param login - логин пользователя
     * @param s     - сообщение если у найденного пользователя не совпадает логин и юзернейм
     * @return true если нашли пользователя
     */
    private boolean isUserCheckOnUi(String name, String login, String s) {
        ElementsCollection<AtlasWebElement> userName = bp.users().allUsersName();
        ElementsCollection<AtlasWebElement> userLogin = bp.users().allUserLogins();
        Assert.assertTrue(userName.size() > 0, "Таблица с пользователями не загрузилась");
        boolean userCheck = false;
        for (int i = 0; i < userName.size(); i++) {
            String user = userName.get(i).getText().trim();
            if (user.equals(name)) {
                String loginOnUi = userLogin.get(i).getText().trim();
                Assert.assertEquals(loginOnUi, login, s);
                userCheck = true;
                break;
            }
        }
        return userCheck;
    }

    @Step("Проверить что была отображена панель с оргЮнитами")
    private void assertPanelDisplayed(PersonGroups personGroups) {
        bp.terminals().terminalsPanel()
                .should("Панель с текущими терминалами не была отображена", DisplayedMatcher.displayed(), 5);
        List<Integer> omIdsWithHisChildren = CommonBioRepository.childSearch(personGroups, PersonGroupsRepository.getPersonGroups());

        List<Terminal> terminals = TerminalRepository.getTerminals().stream()
                .filter(terminal -> !Collections.disjoint(terminal.getPersonGroupIds(), omIdsWithHisChildren)).collect(Collectors.toList());
        List<String> namesApi = terminals.stream().map(Terminal::getDescription).collect(Collectors.toList());
        List<String> idsApi = terminals.stream().map(Terminal::getId).collect(Collectors.toList());
        List<String> statusApi = terminals.stream().map(Terminal::getBlockingStatus).collect(Collectors.toList());

        List<String> ids = bp.terminals().terminalsIdList()
                .stream().map(WebElement::getText).map(s -> s.split(" ")[1]).collect(Collectors.toList());
        List<String> names = bp.terminals().terminalsNamesList()
                .stream().map(WebElement::getText).collect(Collectors.toList());
        List<String> numbers = bp.terminals().sNumbersTerminalsList()
                .stream().map(WebElement::getText).map(s -> s.split(" ")[1]).collect(Collectors.toList());
        List<String> status = new ArrayList<>();
        for (WebElement element : bp.terminals().terminalsStatusList()) {
            String terminalStatus;
            try {
                terminalStatus = element.getAttribute("t");
                status.add(terminalStatus.substring(terminalStatus.lastIndexOf("s") + 1));
            } catch (NullPointerException e) {
                status.add("null");
            }
        }

        for (int i = 0; i < terminals.size(); i++) {
            String tempStatusApi = statusApi.get(i).toLowerCase();
            if (!tempStatusApi.equals("null") && status.size() != 0) {
                Assert.assertTrue(statusApi.get(i).toLowerCase()
                                          .contains(status.get(i)), "Статус " + i + " по счету терминала не совпали в апи на ui");
            }
        }
        List<String> sNumbersApi = terminals.stream().map(Terminal::getSerialNumber).collect(Collectors.toList());
        Assert.assertEquals(names, namesApi, "Текущие названия терминалов и названия терминалов в апи не совпали");
        Assert.assertEquals(ids, idsApi, "Текущие id терминалов и id терминалов в апи не совпали");
        Assert.assertEquals(numbers, sNumbersApi, "Текущие серийные номера терминалов и серийные номера терминалов в апи не совпали");
    }

    @Step("В блоке \"Биометрические данные\" нажать на \"корзинку\" в углу произвольной фотографии.")
    private void clickOnTrashButton(int numberToMatch) {
        //bp.pictureForm().descriptors().forEach(AtlasWebElement::hover);
        bp.pictureForm().onlyPhotosTrashButtons()
                .waitUntil("Количество фотографий у сотрудника оказалось отличным от " + numberToMatch,
                           Matchers.iterableWithSize(numberToMatch), 40);
        int num = new Random().nextInt(bp.pictureForm().onlyPhotosTrashButtons().size());
        bp.pictureForm().onlyPhotosTrashButtons().get(num).click();
    }

    @Step("В блоке \"Биометрические данные\" нажать на значок \"Корзины\" в углу произвольного дескриптора")
    private void clickOnDescriptorTrashButton(int sizeToMatch) {
        bp.pictureForm().deleteCrossListDescriptors().waitUntil("Количество дескрипторов на сайте не совпало с ожидаемым",
                                                                Matchers.iterableWithSize(sizeToMatch), 30);
        int num = new Random().nextInt(bp.pictureForm().deleteCrossListDescriptors().size());
        bp.pictureForm().deleteCrossListDescriptors().get(num).click();
    }

    @Step("Нажать \"Да\" во второй появившейся строчке")
    private void clickDeletePhoto() {
        bp.pictureForm().deletePhoto().click();
    }

    @Step("Нажать \"Да\" в первой появившейся строчке")
    private void clickDeleteDescriptor() {
        bp.pictureForm().deleteDescriptor().click();
    }

    @Step("Нажать на кнопку раздела")
    private void clickOnSectionButton(VariantsSection variantsSection) {
        bp.bioHeader().sectionButton(variantsSection.getSectionName())
                .waitUntil("Кнопка : " + variantsSection.getSectionName() + " не загрузилась", DisplayedMatcher.displayed(), 100);
        bp.bioHeader().sectionButton(variantsSection.getSectionName()).click();
    }

    @Step("Нажать на кнопку \"Выберите файл\". Загрузить файл")
    private void uploadFile() {
        bp.settings().waitUntil("Страница нестроек не загрузлась", DisplayedMatcher.displayed(), 200);
    }

    @Step("Проверить, что была произведена выгрузка {type.licenseName} лицензии")
    private void checkLicenseUploading(LicenseType type, String response) {
        bp.getWrappedDriver().navigate().refresh();
        bp.settings().waitUntil("Страница настроек не загрузилась", DisplayedMatcher.displayed(), 200);
        bp.settings().licenseStatus().should("Текст отображенный в поле не совпал с ожидаемым",
                                             text(Matchers.equalToIgnoringWhiteSpace(type.getUiText())), 5);
        JSONObject responseJson = new JSONObject(response);
        SoftAssert softAssert = new SoftAssert();
        String issueDate = bp.settings().dateField(2).getText();
        String validDate = bp.settings().dateField(3).getText();
        softAssert.assertEquals(responseJson.get("state").toString(), type.toString());
        softAssert.assertEquals(responseJson.optString("issueDate"), issueDate);
        softAssert.assertEquals(responseJson.optString("validDate"), validDate);
        softAssert.assertAll();
        Allure.addAttachment("Была загружена лицензия.", "Параметры, отобразившиеся на сайте:" +
                "\nСостояние: " + type.getUiText() +
                "\nОт: " + issueDate +
                "\nПо (включительно): " + validDate);
    }

    @Step("Проверка перехода на вкладку пользователи")
    private void assertTransitionToUsers() {
        bp.users().inputSearchUser()
                .should("Строка поиска пользователя не отобразилась", DisplayedMatcher.displayed(), 60);
        bp.users().operationWithUsersButton(VariantsOperation.ADD_USER.getVariant())
                .should("Кнопка создания нового пользователя не отобразилась", DisplayedMatcher.displayed(), 5);
        String stringForAssertion = bp.getWrappedDriver().getCurrentUrl();
        String lastWord = stringForAssertion.substring(stringForAssertion.lastIndexOf("/"));
        Assert.assertFalse(lastWord.contains("workpanel"), "Строка адреса не изменилась");
        Assert.assertTrue(lastWord.contains("manageusers"), "Строка адреса не изменилась на пользователей");
    }

    @Step("В поиск ввести оргюнит, у которого присутствуют Сотрудники, по имени : {om} и выбрать его")
    private void chooseOMBySearchField(String om) {
        bp.personal().inputSearch()
                .should("Строка поиска пользователя не отобразилась", DisplayedMatcher.displayed(), 60);
        bp.personal().inputSearch().clear();
        bp.personal().inputSearch().sendKeys(om);
        bp.personal().fieldsOM().get(0).click();
    }

    @Step("Выбрать сотрудника по имени : {name}")
    private void chooseUser(String name) {
        bp.employeeForm().empNames().stream().filter(element -> element.getText().trim().contains(name)).findFirst()
                .orElseThrow(() -> new AssertionError("Не было найден пользователь с именем: " + name))
                .click();
    }

    @Step("Нажать на \"Найти\" в журнале")
    private void clickOnFindButtonInJournal() {
        bp.journal().waitUntil("Журнал событий не загрузился", DisplayedMatcher.displayed(), 30);
        bp.journal().findButton().click();
    }

    @Step("Нажать на \"Как фотографии в ZIP\" в журнале")
    private void clickOnZipDownloadButton() {
        bp.journal().downloadZiButton().click();
    }

    @Step("Проверить, что файл типа {typeOfPhotos} скачивается")
    private void assertFileDownLoading(TypeOfPhotos typeOfPhotos, String personId) {
        HttpResponse response;
        if (typeOfPhotos == TypeOfPhotos.JOURNAL) {
            response = new FileDownloadCheckerForBio(Role.ADMIN, TypeOfFiles.IMAGES, typeOfPhotos)
                    .downloadResponse(Role.ADMIN, null);
        } else {
            response = new FileDownloadCheckerForBio(Role.ADMIN, TypeOfFiles.IMAGES, personId, typeOfPhotos)
                    .downloadResponse(Role.ADMIN, null);
        }
        assertStatusCode(response, 200, null);
        ImmutablePair<String, String> fromResponse = getFileNameExtensionFromResponse(response);
        TypeOfFiles files = TypeOfFiles.ZIP;
        Assert.assertEquals(fromResponse.getRight(), files.getFileExtension(), "Расширение файла не совпало с ZIP");
        Assert.assertEquals(fromResponse.getLeft(), typeOfPhotos.getFileName(), "Название файла не совпало с ожидаемым");
        Allure.addAttachment("Контент файла", "Началась загрузка файла:" + fromResponse.left + "." + fromResponse.right);
    }

    @Step("Проверка загрузки журнала")
    private void assertFindJournal() {
        int columnNumber = bp.journal().allColumnTitle().size();
        Assert.assertEquals(columnNumber, 11);
    }

    @Step("Ввод даты в поле поиска от даты {dateStart}")
    private void enterStartDateJournal(LocalDate dateStart) {
        DateTimeFormatter formatter;
        if (LocaleKeys.getAssertProperty("browser").equals("grid")) {
            formatter = DateTimeFormatter.ofPattern("MMddyyyy");
        } else {
            formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        }
        bp.journal().dateStartInput().waitUntil("", DisplayedMatcher.displayed(), 20);
        bp.journal().dateStartInput().sendKeys(dateStart.format(formatter));
        LOG.info("Была введена дата начала поиска: {}", dateStart);
    }

    @Step("В поиск по ОМ ввести {orgUnitName}, у которого есть привязанные терминалы и выбрать его.")
    private void goToOrgUnitByEnteringNameInField(String orgUnitName) {
        bp.bioHeader().waitUntil("Хедер не загрузился", DisplayedMatcher.displayed(), 200);
        bp.terminals().inputOrgUnitSearchField().waitUntil("Строка поиска не отобразилась", DisplayedMatcher.displayed(), 200);
        bp.terminals().inputOrgUnitSearchField().sendKeys(orgUnitName);
        bp.terminals().orgUnitPanelByName(orgUnitName)
                .waitUntil("Панель с именем оргЮнита не была отображена", DisplayedMatcher.displayed(), 5);
        bp.terminals().orgUnitPanelByName(orgUnitName).click();
    }

    @Step("Нажать на выбранный терминал с id: {id}")
    private void clickOnTerminal(String id) {
        bp.terminals().terminalButtonById(id).click();
    }

    @Step("Нажать на кнопку \"Добавить новый терминал\"")
    private void clickOnAddNewTerminalButton() {
        bp.terminals().addNewTerminalButton().click();
        systemSleep(1); //Метод используется в неактуальных тестах
    }

    @Step("В правой верхней части экрана нажать на значок \"Карандаша\"")
    private void clickOnTerminalPencilButton() {
        bp.terminalCardPanel().pencilTerminalButton().click();
    }

    @Step("В поле \"Статус\" в правой части экрана нажать на радиокнопку.")
    private void pullUpStatusRadioButton() {
        bp.terminalCardPanel().statusRadioButton().click();
        bp.terminalCardPanel().editMode()
                .waitUntil("Меню редактирования терминала не отобразилось", DisplayedMatcher.displayed(), 5);
        Allure.addAttachment("Статус радио-кнопки",
                             "После переключения кнопки статус изменился на: " +
                                     bp.terminalCardPanel().currentRadioButtonStatus().getText());
    }

    @Step("В поле \"Пинкод\" ввести значение {value}")
    private void enterPinValue(String value) {
        bp.terminalCardPanel().pinInputField().sendKeys(value);
    }

    @Step("В поле \"Автономная работа\" ввести значение {value}")
    private void enterBlockTimeoutValue(int value) {
        bp.terminalCardPanel().blockTimeoutInputFiled().click();
        bp.terminalCardPanel().blockTimeoutInputFiled().clear();
        bp.terminalCardPanel().blockTimeoutInputFiled().sendKeys(String.valueOf(value));
    }

    private int getRandomNumberForBlockTimeOut(String terminalId) {
        int current = TerminalRepository.getTerminalById(terminalId).getBlockTimeout();
        int next = new Random().nextInt(71) + 1;
        while (current == next) {
            next = new Random().nextInt(71) + 1;
        }
        return next;
    }

    @Step("Очистить поле \"Название\"")
    private void clearTerminalNameField() {
        bp.terminalCardPanel().terminalEditNameField().clear();
    }

    @Step("Ввести значение {value} в поле \"Название\" ")
    private void enterTerminalNameField(String value) {
        bp.terminalCardPanel().terminalEditNameField().sendKeys(value);
    }

    @Step("Очистить поле \"Серийный номер\"")
    private void clearTerminalSerialNumber() {
        bp.terminalCardPanel().terminalEditSNumberField().clear();
    }

    @Step("Ввести значение {value} в поле \"Серийный №\" ")
    private void enterTerminalSerialNumber(String value) {
        bp.terminalCardPanel().terminalEditSNumberField().sendKeys(value);
    }

    @Step("Проверить удаление человека с именем {name} с терминала с id: {terminalId}")
    private void checkRemovePersonFromTerminal(List<String> names, String terminalId, EmployeeStatus previouslyStatus) {
        List<String> namesUi = bp.terminalCardPanel().terminalPersonsList("")
                .stream().map(WebElement::getText).collect(Collectors.toList());
        List<Person> terminalPersons = PersonRepository.getTerminalPersons(terminalId);
        List<String> currentPersons = terminalPersons.stream()
                .filter(person -> person.getPersonExceptionType() != null && person.getPatronymicName() != null)
                .map(Person::getFullName)
                .collect(Collectors.toList());
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertFalse(namesUi.containsAll(names));
        softAssert.assertTrue(currentPersons.stream().noneMatch(names::contains),
                              "Сотрудник не был отвязан от терминала с id " + terminalId);
        softAssert.assertAll();
        Allure.addAttachment("Проверка",
                             "Сотрудник с именем " + names + " и статусом '" + previouslyStatus.getStatus() +
                                     "', был успешно отвязан от терминала с id: "
                                     + terminalId);
    }

    @Step("Проверить взамодействие человека с именем {name} и терминала с id: {terminalId}")
    private void checkActionWithEmployee(List<String> name, String terminalId, EmployeeStatus employeeStatus) {
        List<Person> terminalPersons = PersonRepository.getTerminalPersons(terminalId);
        SoftAssert softAssert = new SoftAssert();
        List<String> names = terminalPersons.stream()
                .filter(person -> person.getPersonExceptionType() != null && person.getPatronymicName() != null)
                .filter(person -> person.getPersonExceptionType().equals(employeeStatus.toString()))
                .map(Person::getFullName)
                .collect(Collectors.toList());
        List<String> namesCardPanel = bp.terminalCardPanel().terminalPersonsList(employeeStatus.getStatus())
                .stream().map(WebElement::getText).collect(Collectors.toList());
        if (employeeStatus == EmployeeStatus.INCLUDE) {
            softAssert.assertTrue(names.containsAll(name),
                                  "Сотрудник не был успешно привязан  к терминалу с id: " + terminalId);
            softAssert.assertTrue(namesCardPanel.containsAll(name));
            Allure.addAttachment("Проверка",
                                 "Сотрудник с именем " + name + " был успешно привязан  к терминалу с id:  " + terminalId);
        }
        if (employeeStatus == EmployeeStatus.EXCLUDE) {
            softAssert.assertTrue(names.containsAll(name),
                                  "Сотрудник не был отвязан от терминала с id " + terminalId);
            softAssert.assertTrue(namesCardPanel.containsAll(name));
            Allure.addAttachment("Проверка",
                                 "Сотрудник с именем " + name + " был успешно заблокирован у терминала с id: "
                                         + terminalId);
        }
        if (employeeStatus == EmployeeStatus.ADMIN) {
            softAssert.assertTrue(names.containsAll(name),
                                  "Сотрудник не был назначен администратором в терминале с id: " + terminalId);
            softAssert.assertTrue(namesCardPanel.containsAll(name));
            Allure.addAttachment("Проверка",
                                 "Сотрудник с именем " + name + " был успешно назначен администратором в терминал с id:  "
                                         + terminalId);
        }
        softAssert.assertAll();
    }

    @Step("В блоке \"Персонал\" нажать на значок карандаша.")
    private void terminalEmployeesEditPencilButton() {
        systemSleep(3); //Метод используется в неактуальных тестах
        bp.terminalCardPanel().terminalEmployeesEditPencilButton().click();
    }

    @Step("Нажать на кнопку \"Сохранить\"")
    private void clickTerminalInfoSaveButton() {
        bp.terminalCardPanel().terminalInfoSaveButton().click();
        bp.terminalCardPanel().editMode().waitUntil("Загрузка все еще не пропала",
                                                    Matchers.not(DisplayedMatcher.displayed()), 15);
    }

    @Step("Проверить изменение пин-кода у терминала с id: {terminalId}")
    private void assertPinCodeChange(String terminalId) {
        bp.terminalCardPanel().pinStatus().should("Пинкод не был установлен",
                                                  text(containsString("Установлено")), 5);
        Assert.assertNotNull(TerminalRepository.getTerminalById(terminalId).getPin(), "Пинкод не был установлен");
        Allure.addAttachment("Смена пинкода",
                             "Пинкод был успешно сменен, прежний пинкод и текущий не совпали");
    }

    @Step("Проверить изменение пин-кода у терминала с id: {terminalId}")
    private void assertBlockTimeoutChange(String terminalId, int newTimeout) {
        String statusText = bp.terminalCardPanel().batteryStatus().getText();
        Assert.assertEquals(statusText, "Установлено");
        int apiTimeout = TerminalRepository.getTerminalById(terminalId).getBlockTimeout();
        Assert.assertEquals(apiTimeout, newTimeout, "Время автономной работы не совпало");
        Allure.addAttachment("Проверка", "Время автономной работы было успешно усменено на: "
                + newTimeout + " Текущий статус на сайте: " + statusText);
    }

    @Step("Проверить добавление нового терминала с введенными ранее параметрами")
    private void checkTerminalCreation(String sNumber, String name, int orgUnitId) {
        Terminal terminals = TerminalRepository.getTerminals().stream()
                .filter(terminal -> terminal.getDescription().equals(name)
                        || terminal.getId().equals(name)
                        || terminal.getSerialNumber().equals(sNumber)).findFirst()
                .orElseThrow(() -> new AssertionError("Терминал с именем \"" + name + "\" не был найден в api"));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(Collections.singletonList(orgUnitId), terminals.getPersonGroupIds(),
                                "Не было найдено айди привязанного в тесте оргЮнита");
        String nameApi = terminals.getDescription();
        String sNumberApi = terminals.getSerialNumber();
        softAssert.assertEquals(sNumberApi, sNumber, "Номер введенный ранее не совпал с текущим в Api");
        softAssert.assertEquals(nameApi, name, "Имя введенное ранее не совпало с текущим в Api");
        softAssert.assertAll();
        Allure.addAttachment("Создание терминала",
                             "Был успешно создан терминал с id: " + terminals.getId() + ",  имя: " + name + ", серийный номер: " + sNumber);
    }

    @Step("Проверка переключения статуса терминала")
    private void checkTerminalStatusSwitch(TerminalStatus status, String id) {
        bp.terminalCardPanel().currentTerminalStatus(id).should("Статус терминала остался прежним",
                                                                text(containsString(status.getStatus())), 10);
        Terminal terminals = TerminalRepository.getTerminals().stream()
                .filter(terminal -> terminal.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Терминал с id" + id + " не был найден в api"));
        String currentStatusApi = terminals.getBlockingStatus();
        Assert.assertTrue(currentStatusApi.contains(status.toString()),
                          "Статус, выбранный в ходе теста \"" +
                                  status.toString() + "\" и текущий в api \"" + currentStatusApi + "\" не совпали");
        Allure.addAttachment("Проверка",
                             "У терминала с id: " + id + " был успешно сменен статус на " + status.getStatus());
    }

    @Step("Нажать на кнопку \"Сохранить\" формы добавления ОМ.")
    private void clickOrgUnitLinkInfoSaveButton() {
        bp.terminalCardPanel().selectOrgUnitSaveButton().click();
    }

    @Step("Нажать на кнопку \"Добавить оргюнит\"")
    private void clickAddOrgUnitButton() {
        bp.terminalCardPanel().addOrgunitButton().click();
    }

    @Step("Выбрать из выпадающей структуры ОМ.")
    private void clickOnUnitCheckBox(String orgUnitName) {
        bp.terminals().inputSearchOrgUnitPopUp().sendKeys(orgUnitName);
        try {
            bp.terminals().unitCheckBox(orgUnitName).click();
        } catch (NoSuchElementException e) {
            LOG.info("Чек бокс у {} уже был активирован", orgUnitName);
        }
    }

    @Step("Нажать на кнопку \"Закрыть\" в форме выбора событий")
    private void closeCheckBoxesForm() {
        bp.journal().closeEventBoxes().click();
    }

    @Step("Проверить изменения имени и серийного номера терминала")
    private void checkNameAndSNumberChange(String id, String name, String sNumber) {
        Terminal terminals = TerminalRepository.getTerminals().stream()
                .filter(terminal -> terminal.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Терминал с id" + id + " не был найден в api"));
        String nameApi = terminals.getDescription();
        String sNumberApi = terminals.getSerialNumber();
        Assert.assertEquals(sNumberApi, sNumber, "Номер введенный ранее не совпал с текущим в Api");
        Assert.assertEquals(nameApi, name, "Имя введенное ранее не совпало с текущим в Api");
        Allure.addAttachment("Проверка", "Текущее имя в api: " + nameApi + ", введенное нами имя: " + name +
                "Текущий серийный номер в api: " + sNumberApi + ", введенный нами номер: " + sNumber);
    }

    @Step("Проверить привязку оргЮнита к терминалу")
    private void checkAddingOrgUnitToTerminal(String terminalId, int orgUnitId) {
        Terminal terminals = TerminalRepository.getTerminals().stream()
                .filter(terminal -> terminal.getId().equals(terminalId)).findFirst()
                .orElseThrow(() -> new AssertionError("Терминал с id" + terminalId + " не был найден в api"));
        Assert.assertTrue(terminals.getPersonGroupIds().stream().anyMatch(s -> s == orgUnitId), "Не было найдено айди привязанного в тесте оргЮнита");
        Allure.addAttachment("Проверка", "Был привязан оргЮнит с id: " + orgUnitId + " , к терминалу с id: " + terminalId);
    }

    @Step("Ввод даты в поле поиска до даты {dateEnd}")
    private void enterEndDateJournal(LocalDate dateEnd) {
        DateTimeFormatter formatter;
        if (LocaleKeys.getAssertProperty("browser").equals("grid")) {
            formatter = DateTimeFormatter.ofPattern("MMddyyyy");
        } else {
            formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        }
        bp.journal().dateEndInput().sendKeys(dateEnd.format(formatter));
        systemSleep(3); //нужно что бы дата успевала поставиться
    }

    @Step("Вписать в поле Фамилия И.О. следующую строку: {name}")
    private void enterPersonName(String name) {
        bp.journal().nameInput().click();
        bp.journal().nameInput().sendKeys(name);
    }

    @Step("Нажать на троеточие у события журнала")
    private void pressOnTripleDotEventJournal() {
        bp.journal().tripleDotEventButton().click();
    }

    @Step("Нажать на троеточие у фильтра по оргюниту")
    private void pressOnTripleDotOrgNameInJournal() {
        bp.journal().tripleDotOrgNameButton().click();
    }

    @Step("Выбрать случайные чекбоксы событий")
    private void clickOnRandomNumberCheckBoxEvent(List<CheckBoxAndStatus> statuses) {
        for (CheckBoxAndStatus status : statuses) {
            bp.journal().checkBoxByName(status.getItemName()).click();
        }
        Allure.addAttachment("События", "Были выбраны события: "
                + CheckBoxAndStatus.getStatusesAttachment(statuses));
        LOG.info("Были выбраны события: {} ", statuses);
    }

    @Step("Проверка очистки журнала")
    private void assertClearJournal() {
        bp.journal().clearJournalButton().waitUntil("Кнопка очистки все еще отображается",
                                                    Matchers.not(DisplayedMatcher.displayed()), 10);
        int columnNumber = bp.journal().allColumnTitle().size();
        Assert.assertEquals(columnNumber, 0);
    }

    @Step("Проверка обновления журнала")
    private void assertRefreshJournal() {
        bp.journal().journalSpinner().waitUntil("Спиннер загрузки все еще отображается",
                                                Matchers.not(DisplayedMatcher.displayed()), 10);
        int columnNumber = bp.journal().allColumnTitle().size();
        Assert.assertEquals(columnNumber, 11);
    }

    @Step("Проверка на то, что отобразились сотрудники, относящиеся к этому ом")
    private void assertEmployees(List<Person> employeeAPI) {
        bp.employeeForm().waitUntil("Форма сотрудника не высветилась", DisplayedMatcher.displayed());
        List<String> employeeUI = new ArrayList<>();
        bp.employeeForm().empNames().forEach(element -> employeeUI.add(element.getText()));
        List<String> temp = employeeAPI.stream().map(Person::getFirstName).collect(Collectors.toList());
        temp.removeAll(employeeUI);
        Allure.addAttachment("Сотрудники", "text/plain", "Сотрудники на UI : " + employeeUI
                + "\n" + "Сотрудники в API : " + employeeAPI + "\n" + "Разница между ними : " + temp);
        Assert.assertEquals(employeeUI, employeeAPI);
    }

    @Step("Нажать на кнопку \"Выполнено, обновить\"")
    private void pressDoneRepeatButton() {
        bp.journal().doneRepeatButton().click();
    }

    @Step("Нажать на кнопку \"очистить журнал\"")
    private void pressClearJournalButton() {
        bp.journal().clearJournalButton().click();
    }

    @Step("Выбор орюнита {orgName}")
    private void sendInFindOrgMod(String orgName) {
        bp.journal().inputOrgModuleSearch().sendKeys(orgName);
        bp.journal().findingOrgNameCheckBox().click();
        LOG.info("Было введено название оргюнита: {}", orgName);
    }

    @Step("Выбор орюнита {orgName}")
    private void searchForOrgUnit(String orgName) {
        bp.personPopUp().searchOrgUnitInput().click();
        bp.personPopUp().searchOrgUnitInput().clear();
        bp.personPopUp().searchOrgUnitInput().sendKeys(orgName);
        bp.personPopUp().orgUnitPanelByName(orgName).click();
    }

    private String getTerminalId() {
        return bp.terminalCardPanel().currentTerminalIdField().getText();
    }

    @Step("У сотрудника с ролью {employeeStatus.status} нажать на кнопку \"Убрать исключение\".")
    private void deletePersonFromTerminal(EmployeeStatus employeeStatus, String name) {
        bp.personPopUp().actionWithUserButton(name, EmployeeStatus.REMOVED.getAction()).click();
    }

    @Step("Нажать \"Сохранить\"")
    private void saveAttachedPersonsButtonClick() {
        bp.personPopUp().savePersonAttachedButton().click();
    }

    @Step("У сотрудника c именем {name} нажать на кнопку {employeeStatus.status}")
    private void clickIdentifyButton(EmployeeStatus employeeStatus, String name) {
        bp.personPopUp().actionWithUserButton(name, employeeStatus.getAction()).click();
    }

    @Step("Нажать на кнопку \"Конфиг файл\" в правой части экрана")
    private void clickEditConfigButton() {
        bp.terminalCardPanel().settingsConfigButton().click();
    }

    @Step("Нажать на кнопку \"Редактировать\"")
    private void editButtonClick() {
        bp.terminals().editButton().click();
    }

    @Step("Нажать на кнопку \"Редактировать\"")
    private void saveButtonClick() {
        bp.terminals().saveButton().click();
    }

    @Step("Изменить значение {line.name}")
    private void editFile(ConfigLine line, Object input) {
        bp.terminals().configFile().waitUntil("Текст конфига не появился", DisplayedMatcher.displayed(), 10);
        JSONObject config = new JSONObject(bp.terminals().configFile().getText());
        config.put(line.getName(), input);
        bp.terminals().configFile().click();
        bp.terminals().configFile().clear();
        bp.terminals().configFile().sendKeys(config.toString());
        Allure.addAttachment("Изменения",
                             "Было изменено значение параметра " + line.getName() + " на " + input.toString());
    }

    @Step("Проверить изменения значения {line}")
    private void checkThatLinesMatches(String terminalId, ConfigLine line, Object inputValue) {
        Assert.assertEquals(CommonBioRepository.getConfigObject(terminalId, line), inputValue,
                            "Значения в api в файле на ui не совпали");
        Allure.addAttachment("Проверка",
                             "Значение параметра было успешно изменено на " + inputValue.toString());
    }

    @Step("Нажать на кнопку \"Сохранить\" в окне фильтра по орюниту")
    private void pressSaveInFilterButton() {
        bp.journal().saveInOrgNameFilterButton().click();
    }

    @Step("Проверка того, что в журнале отображаются записи соответствующие заданным параметрам")
    private void assertJournalWithParameters(LocalDate startDate, LocalDate endDate,
                                             HashMap<String, List<CheckBoxAndStatus>> matchMap) {
        systemSleep(3); //Метод используется в неактуальных тестах
        String orgName = matchMap.keySet().iterator().next();
        List<CheckBoxAndStatus> allMarked = matchMap.get(orgName);
        LocalDateTime startDateTime = startDate.atTime(0, 0, 0);
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy\nHH:mm").withLocale(Locale.ENGLISH);
        ElementsCollection<AtlasWebElement> allYourData = bp.journal().allYoursData();
        if (allYourData.size() == 0) {
            Assert.fail("Записи журнала под выбранные параметры не загрузились");
        }
        SoftAssert softAssert = new SoftAssert();
        List<Journal> journalList = JournalRepository.getJournals(startDate, endDate, allMarked,
                                                                  PersonGroupsRepository.getOrgUnitIdByName(orgName).getName());
        softAssert.assertEquals(allYourData.size(), journalList.size());
        softAssert.assertTrue(allYourData.stream().map(e -> LocalDateTime.parse(e.getText(), formatter))
                                      .allMatch(startDateTime::isBefore),
                              "В одной из строк \"Дата - время ваше\" дата была меньше, чем заданный диапазон");
        int size = bp.journal().allPlaceData().size();
        new Actions(bp.getWrappedDriver()).moveToElement(bp.journal().allPlaceData().get(size - 1)).perform();
        ElementsCollection<AtlasWebElement> allPlaceData = bp.journal().allPlaceData();
        softAssert.assertTrue(allPlaceData.stream().map(e -> LocalDateTime.parse(e.getText(), formatter))
                                      .allMatch(endDateTime::isAfter),
                              "В одной из строк \"Дата - время на месте\" дата была больше, чем заданный диапазон");
        ElementsCollection<AtlasWebElement> allTerminals = bp.journal().allTeminals();
        softAssert.assertTrue(allTerminals.stream().allMatch(element -> element.getText().contains(orgName)),
                              "В одной из строк \"Терминала\" не оказалось названия оргюнита");
        ArrayList<String> allParameters = new ArrayList<>();
        allMarked.forEach(checkBoxAndStatus -> allParameters.add(checkBoxAndStatus.getInTable()));
        ElementsCollection<AtlasWebElement> allEvents = bp.journal().allEvents();
        softAssert.assertTrue(allEvents.stream().map(e -> e.getText().trim()).allMatch(allParameters::contains),
                              "В одной из строк \"Событие\" не совпало ни с одним из доступных способов авторизации");
        softAssert.assertAll();
    }

    @Step("Проверка того, что в журнале не было отображено ни одно событие")
    private void assertEmptyJournalWithParameters(String enteredName) {
        bp.journal().findButton().waitUntil("Текст кнопки не изменился",
                                            text(containsString("Выполнено, обновить")), 20);
        int size = bp.journal().allYoursData().size();
        String name = bp.journal().nameInput().getAttribute("value");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(size, 0,
                                "Список событий не оказался пустым");
        softAssert.assertEquals(name, enteredName);
        softAssert.assertAll();
        Allure.addAttachment("Проверка", "Было загружено " + size + " событий." +
                "\nУ кнопки был отображен текст: " + bp.journal().findButton().getText() +
                "\nИмя отображенное в поисковой строке: " + name);
    }

    @Step("Проверка на то, что дескриптор был удален")
    private void assertForDeletingDescriptor(Person person, int numOfDescriptors) {
        bp.pictureForm().descriptors().waitUntil("Количество дескрипторов на UI не изменилось",
                                                 Matchers.iterableWithSize(numOfDescriptors - 1), 15);
        int numAPI = FaceDescriptorsRepository.getDescriptors(person).size();
        Assert.assertEquals(numOfDescriptors - 1, numAPI, "Количество дескрипторов в API не изменилось");
    }

    @Step("Выбор орюнита {orgName}")
    private void sendInFindOrgModUsers(String orgName) {
        bp.users().inputOrgModuleSearch()
                .waitUntil("Строка ввода орюнита не отобразилась", DisplayedMatcher.displayed(), 5);
        bp.users().inputOrgModuleSearch().sendKeys(orgName);
        bp.users().findingOrgNameCheckBox().click();
    }

    @Step("Нажать на кнопку \"Сохранить\" в окне фильтра по орюниту")
    private void pressSaveInFilterButtonUsers() {
        bp.users().saveInOrgNameFilterButton().click();
    }

    @Step("Нажать на кнопку \"{variantsOperation.variantsOperation}\"")
    private void pressUserButton(VariantsOperation variantsOperation) {
        bp.users().operationWithUsersButton(variantsOperation.getVariant()).click();
    }

    @Step("Выбрать блок добавления пользователя {variantsCreate.forAllure}")
    private void pressOnVariantsCreateNewUser(VariantsCreate variantsCreate) {
        bp.users().createNewUserVariantButton(variantsCreate.getVariant())
                .waitUntil("Кнопка добавления пользователя не отображается", DisplayedMatcher.displayed(), 10);
        bp.users().createNewUserVariantButton(variantsCreate.getVariant()).click();
        Allure.addAttachment("Выбор варианта создания", "Создание нового пользователя путем: "
                + variantsCreate.getForAllure());
    }

    @Step("Проверка на то, что окно с биометрией для сотрудника по имени : {chosenName} открылось")
    private void assertThatBiometricOpen(Person chosenName) {
        bp.pictureForm().userName()
                .waitUntil("Имя юзера не отобразилось", DisplayedMatcher.displayed(), 10);
        systemSleep(5); //Метод используется в неактуальных тестах
        String nameUI = bp.pictureForm().userName().getText();
        Assert.assertEquals(chosenName.getFullName(), nameUI, "Выбранный юзер : "
                + chosenName + " и юзер на UI : " + nameUI + " не совпали");
    }

    @Step("Нажать на чекбокс, выделяющий все фотографии")
    private void selectAll() {
        bp.pictureForm().loadingPanel().waitUntil("Все еще грузится", Matchers.not(DisplayedMatcher.displayed()), 10);
        bp.pictureForm().buttonToSelectAll().waitUntil("Чекбокс не прогрузился", DisplayedMatcher.displayed(), 10);
        bp.pictureForm().buttonToSelectAll().click();
    }

    @Step("Нажать на кнопку \"Удалить фото\"")
    private void clickOnDeletePhotoButton() {
        bp.pictureForm().deleteBioPhotosButton().waitUntil("Кнопка удалить фото не прогрузилась",
                                                           DisplayedMatcher.displayed(), 10);
        bp.pictureForm().deleteBioPhotosButton().click();
    }

    @Step("Проверка на удаление фотографии у юзера")
    private void assertForDeletingPhoto(Person person, int numOfPhotos) {
        bp.pictureForm().photos().waitUntil("Количество фотографий не изменилось",
                                            Matchers.iterableWithSize(numOfPhotos - 1), 30);
        int count = Math.toIntExact(FaceDescriptorsRepository.getDescriptors(person).stream().filter(faceDescriptors -> faceDescriptors.getSrcUrl() != null).count());
        Allure.addAttachment("Количество фотографий",
                             "Количество фотографий до удаления : " + numOfPhotos + " и после : " + count);
        Assert.assertEquals(numOfPhotos - 1, count, "Количество фотографий не изменилось");
    }

    @Step("Проверка на удаление всех фотографий у выбранного юзера")
    private void assertForDeletingAllPhotos(Person person) {
        bp.pictureForm().photos().waitUntil("Фотографии не были удалены", Matchers.emptyIterable(), 15);
        Assert.assertEquals(Math.toIntExact(FaceDescriptorsRepository.getDescriptors(person).stream().filter(faceDescriptors -> faceDescriptors.getSrcUrl() != null).count()),
                            0, "Не все фотографии удалились");
    }

    @Step("Нажать на кнопку \"сохранить\" в форме добавления нового сотрудника")
    private void pressOnSaveButtonInAddNewUserForm() {
        try {
            bp.users().chooseUser().click();
        } catch (org.openqa.selenium.NoSuchElementException e) {
            bp.users().chooseUser2().click();
        }
    }

    @Step("Проверка добавления пользователя с именем {name}")
    private void assertAddNewUser(String name, String login) {
         /*перезагрузка, потому что на UI не отображается сразу, в апи лезем после перезагрузки, затем чтобы
        туда успели залететь изменения и есть признаки того что попытка залезть в апи при загрузке,
         делает загрузку дольше по времени
         */
        bp.getWrappedDriver().navigate().refresh();
        bp.bioHeader().waitUntil("Хедер не загрузился", DisplayedMatcher.displayed(), 200);
        boolean weCheckThat = userCheckInApi(name, login, "Введенный логин не соответствует логину в API");
        Assert.assertTrue(weCheckThat, "В списке пользователей не нашли пользователя с выбранным именем");
        bp.bioHeader().waitUntil("Хедер не загрузился", DisplayedMatcher.displayed(), 200);
        boolean userCheck = isUserCheckOnUi(name, login, "Введеный логин не совпадает с отображаемым на странице");
        Assert.assertTrue(userCheck, "На странице не было найдено добавленного имени");
    }

    @Step("Проверка редактирования пользователя с именем {name}")
    private void assertEditUser(String name, String login) {
        boolean weCheckThat = userCheckInApi(name, login, "Введенный логин не соответствует логину в API");
        Assert.assertTrue(weCheckThat, "В списке пользователей не нашли пользователя с выбранным именем");
        boolean userCheck = isUserCheckOnUi(name, login, "Введеный логин не совпадает с отображаемым на странице");
        Assert.assertTrue(userCheck, "На странице не было найдено отредактированного пользователя");
    }

    @Step("Проверка удаления пользователя с именем {name}")
    private void assertDeleteUser(String name) {
        boolean weCheckThat = userCheckInApi(name, null, "Пользователь остался в апи");
        Assert.assertFalse(weCheckThat, "В списке пользователей нашли удаленного пользователя с выбранным именем");
        boolean userCheck = isUserCheckOnUi(name, null, "На UI отображатеся удаленный пользователь.");
        Assert.assertFalse(userCheck, "На странице найден удаленный пользователь");
    }

    @Step("Нажать кнопку Сохранить")
    private void clickSaveLoginAndPassButton() {
        bp.users().saveLoginAndPassButton().click();
    }

    @Step("Нажать кнопку закрытия формы логина и пароля")
    private void clickCloseLoginAndPassButton() {
        bp.users().closeLoginAndPassButton().click();
    }

    @Step("Ввести текст: {text} в строку {inputs.allureName}")
    private void sendTextInCreateNewUserInput(String text, Inputs inputs) {
        systemSleep(1.5); //Метод используется в неактуальных тестах
        bp.users().inputCreateNewUser(inputs.getInputs()).clear();
        bp.users().inputCreateNewUser(inputs.getInputs()).sendKeys(text);
    }

    @Step("Нажать на кнопку \"Неприсоединённые\"")
    private void pressUsersWithoutOrgButton() {
        bp.users().usersWithoutOrgButton().click();
    }

    @Step("Нажать на кнопку случайного сотрудника")
    private String clickOnRandomUser() {
        ElementsCollection<AtlasWebElement> allUsers = bp.users().allUsersWithoutOrg();
        AtlasWebElement user = getRandomFromList(allUsers);
        String targetUser = user.getText().trim();
        LOG.info("Был выбран сотрудник с именем {}", targetUser);
        Allure.addAttachment("Выбор сотрудника", "Был выбран сотрудник с именем " + targetUser);
        new Actions(bp.getWrappedDriver()).moveToElement(user).perform();
        user.click();
        bp.users().chooseUser().click();
        return targetUser;
    }

    @Step("Проверка манипуляций с разрешениями")
    private void assertManipulatePermission(Permissions permissions, String orgName, String userName,
                                            List<Person> allUsers, boolean addPermissionNotRemoved) {
        SoftAssert softAssert = new SoftAssert();
        systemSleep(5.5); //тред слип добавлен потому что изменение статуса немного запаздывает после закрытия окна
        HashMap<String, List<String>> allOrgNamePermissions = new HashMap<>();
        ElementsCollection<AtlasWebElement> allOrgNameUI = bp.users().tableUserOrgNamePermission();
        ElementsCollection<AtlasWebElement> allPermissions = bp.users().tableUserPermission();
        for (int i = 0; i < allPermissions.size(); i++) {
            String tempPermission = allPermissions.get(i).getText().trim();
            tempPermission = tempPermission.substring(0, tempPermission.indexOf("\n"));
            String tempOrgName = allOrgNameUI.get(i).getText();
            List<String> allOrgName = new ArrayList<>(Arrays.asList(tempOrgName.split(";")));
            allOrgNamePermissions.put(tempPermission, allOrgName);
        }
        String addedPermission = permissions.getPermissionText();
        List<Integer> orgIds = new ArrayList<>();
        if (addPermissionNotRemoved) {
            Person user = allUsers.stream()
                    .filter(person -> person.getFullName().contains(userName))
                    .findAny()
                    .orElseThrow(() -> new AssertException("Не нашли в апи юзера: " + userName));
            HashMap<String, List<Integer>> userPermissionsInApi = user.getPermission();
            orgIds = userPermissionsInApi.get(permissions.toString());
            if (orgName != null) {
                int sizeOrgIds = orgIds.size();
                softAssert.assertTrue(sizeOrgIds > 0, "У сотрудника в апи нет оргюнитов в добавленном разрешении");
            }
            softAssert.assertTrue(allOrgNamePermissions.containsKey(addedPermission),
                                  "У пользователя на сайте не отображается добавленное разрешение");
            softAssert.assertTrue(userPermissionsInApi.containsKey(permissions.toString()),
                                  "У пользователя нет добавленного разрешения");
        }
        if (orgName != null) {
            List<String> orgNames = PersonGroupsRepository.getAllOfCurrentAttachedOrgUnitsNames(orgIds)
                    .stream()
                    .map(PersonGroups::getName)
                    .collect(Collectors.toList());
            if (addPermissionNotRemoved) {
                softAssert.assertTrue(allOrgNamePermissions.get(addedPermission).contains(orgName),
                                      "У пользователя на сайте не отображается добавленный оргюнит");
                softAssert.assertTrue(orgNames.contains(orgName), "В API разрешениях сотрудника нет добавленного оргюнита");
                Allure.addAttachment("Проверка добавления разрешения", "Для сотрудника " + userName + " было успешно добавлено разрешение  \"" +
                        addedPermission + "\"  для оргюнита: " + orgName);
            } else {
                softAssert.assertFalse(allOrgNamePermissions.get(addedPermission).contains(orgName),
                                       "У пользователя на сайте отображается удаленный оргюнит");
                softAssert.assertFalse(orgNames.contains(orgName), "В API разрешениях сотрудника есть удаленный оргюнита");
                Allure.addAttachment("Проверка удаления оргюнита из списка", "Для сотрудника " + userName
                        + " был успешно удален оргюнит " + orgName + " из разрешения  \"" + addedPermission + "\"");
            }
        }
        softAssert.assertAll();
    }

    @Step("Нажать на кнопку \"Сохранить\" в форме разрешений")
    private void clickOnSavePermissionButton() {
        bp.users().savePermission().click();
        bp.users().tableUserOrgNamePermission().waitUntil(Matchers.hasSize(Matchers.greaterThan(0)));
    }

    @Step("Нажать на кнопку \"Изменить список оргюнитов\"")
    private void clickOnChangeListOrgNameButton() {
        bp.users().changeListOrgNameButton().click();
    }

    @Step("Нажать на пользователя {userName}")
    private void clickOnUser(String userName) {
        bp.users().userNameButton(userName).click();
    }

    @Step("Нажать на кнопку \"Добавить\" в форме сотрудника")
    private void clickOnAddPermissionButtonInUserForm() {
        bp.users().addPermissionButton().click();
    }

    @Step("Нажать на кнопку \"Управлять\" в форме сотрудника")
    private void clickOnPencilButtonInUserForm() {
        bp.users().pencilButton().click();
    }

    @Step("Нажать на разрешение {permissions} в форме сотрудника")
    private void clickOnPermissionsInUserForm(Permissions permissions) {
        bp.users().permissionButton(permissions.toString()).click();
        String permissionBlue = bp.users().permissionHighlightInBlue().getText();
        Assert.assertTrue(permissionBlue.contains(permissions.toString()),
                          "Выбранное разрешение не подсвечивается синим");
    }

    @Step("Нажать на кнопку \"Выбрать\" в форме разрешений")
    private void clickOnSelectPermissionButton() {
        bp.users().selectPermissionButton().click();
    }

    @Step("Пресет. Проверить, что выполняется условие для пользователя")
    private String findUserAndMakePreset(boolean userWithoutPermissions, List<Person> allUsers, boolean withOrgUnits) {
        Random rnd = new Random();
        List<String> userList = allUsers.stream().map(Person::getFullName).collect(Collectors.toList());
        List<String> userUi = bp.users().allUsersName().stream().map(WebElement::getText).collect(Collectors.toList());
        userList.retainAll(userUi);
        String userName = userList.get(rnd.nextInt(userList.size()));
        Person user = allUsers.stream().filter(person -> person.getFullName().contains(userName))
                .findAny()
                .orElseThrow(() -> new AssertException("Не нашли в апи пользователя с именем: " + userName));
        HashMap<String, List<Integer>> userPermissionsInApi = user.getPermission();
        if (userWithoutPermissions && userPermissionsInApi.size() > 0) {
            List<String> permissions = new ArrayList<>(userPermissionsInApi.keySet());
            PresetBioClass.deletePermissionsPreset(user.getId(), permissions);
            Allure.addAttachment("Пресет", "В результате работы пресета были удалены разрешения для " + userName);
        } else if (!userWithoutPermissions && userPermissionsInApi.size() == 0) {
            Integer[] ids = null;
            if (withOrgUnits) {
                ids = new Integer[]{getRandomFromList(PersonGroupsRepository.getPersonGroups()).getId()};
            }
            String permissionToAdd = Permissions.randomPermission().toString();
            PresetBioClass.addPermissionsPreset(user.getId(), permissionToAdd, ids);
            String end;
            if (withOrgUnits) {
                end = " для оргюнита №  " + Arrays.toString(ids);

            } else {
                end = " без ограничений на оргюниты.";
            }
            Allure.addAttachment("Пресет", "В результате работы пресета для сотрудника " + userName +
                    " было добавлено разрешение " + permissionToAdd + end);
        } else {
            Allure.addAttachment("Действие пресета",
                                 "Текущая ситуацияя удовлетворяет необходимым условиям, в пресете нет необходимости");
        }
        bp.bioHeader().waitUntil("Хедер не загрузился", DisplayedMatcher.displayed(), 200);
        return userName;
    }

    @Step("Проверка удаления разрешения")
    private void assertDeletePermission(Permissions permissions, String userName, List<Person> allUsers) {
        systemSleep(2); //тред слип добавлен потому что изменение статуса немного запаздывает после закрытия окна
        List<String> allOrgNamePermissions = new ArrayList<>();
        ElementsCollection<AtlasWebElement> allPermissions = bp.users().tableUserPermission();
        for (AtlasWebElement allPermission : allPermissions) {
            String tempPermission = allPermission.getText().trim();
            tempPermission = tempPermission.substring(0, tempPermission.indexOf("\n"));
            allOrgNamePermissions.add(tempPermission);
        }
        String addedPermission = permissions.getPermissionText();
        Person user = allUsers.stream().filter(person -> person.getFullName().contains(userName))
                .findAny()
                .orElseThrow(() -> new AssertException("Не нашли в апи пользователя с именем: " + userName));
        HashMap<String, List<Integer>> userPermissionsInApi = user.getPermission();
        Assert.assertFalse(allOrgNamePermissions.contains(addedPermission),
                           "У пользователя на сайте есть удаленное разрешение");
        Assert.assertFalse(userPermissionsInApi.containsKey(permissions.toString()),
                           "У пользователя в АПИ есть удаленное разрешение");
    }

    @Step("Проверка того что в столбце формы настройки разрешения не отображается ограничение оргюнита")
    private void assertInValueDisappearsDeactivatedOM(String orgName) {
        bp.users().inputOrgModuleSearch().waitUntil("Форма выбора оргюинита все еще отображается",
                                                    Matchers.not(DisplayedMatcher.displayed()), 2);
        String restrictedOrgName = bp.users().restrictedOrgName().getText();
        List<String> allOrgName = new ArrayList<>(Arrays.asList(restrictedOrgName.split(";")));
        Assert.assertFalse(allOrgName.contains(orgName), "Оргюнит все еще отображается в столбце значения");
    }

    /**
     * берет список оргюнитов у пользователя с UI
     */
    private String getRestrictedOrgNameFromUsers() {
        return bp.users().restrictedOrgName().getText().trim();
    }

    @Step("Нажать на кнопку \"Редактировать\" в форме разрешений юзера")
    private void clickOnEditInUserPermission() {
        bp.users().editInUserPermissionButton().click();
    }

    @Step("Нажать на кнопку \"Удалить\" в форме разрешений юзера")
    private void clickOnDeleteInUserPermission() {
        systemSleep(3); //Метод используется в неактуальных тестах
        bp.users().deleteInUserPermissionButton().click();
    }

    @Step("Нажать на произвольно выбранное разрешение")
    private Permissions pressOnRandomPermission(boolean withRestriction) {
        ElementsCollection<AtlasWebElement> elementsPermission = bp.users().tableUserPermission();
        AtlasWebElement targetPermission = getRandomFromList(elementsPermission);
        String permission = targetPermission.getText();
        String forAllure = "";
        if (withRestriction) {
            forAllure = "c ограничениями";
            while (permission.contains("Без ограничений")) {
                elementsPermission.remove(targetPermission);
                targetPermission = getRandomFromList(elementsPermission);
                permission = targetPermission.getText();
            }
        }
        permission = permission.substring(0, permission.indexOf("\n"));
        targetPermission.click();
        Allure.addAttachment("Выбор разрешения",
                             "Выбрано разрешение " + forAllure + " под именем: " + permission);
        String finalPermission = permission;
        return Arrays.stream(Permissions.values())
                .filter(v -> v.getPermissionText().equals(finalPermission))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Значение разрешения неизвестно или отсутствует"));
    }

    @Step("Нажать на ОМ \"Центральный офис\"")
    private void pressOnCentralOffice() {
        bp.terminals().unitPanel("Центральный офис").click();
    }

    @Step("Проверить, что открылась страница авторизации")
    private void checkLoginPageOpened() {
        LOG.info("Проверяем, что открылась страница авторизации");
        bp.bioLogin().should("Форма логина все еще не отображается", DisplayedMatcher.displayed(), 20);
        bp.bioLogin().loginField().should("Поле ввода логина не отобразилось", DisplayedMatcher.displayed(), 5);
        bp.bioLogin().passwordField().should("Поле ввода пароля не отобразилось", DisplayedMatcher.displayed(), 5);
        Allure.addAttachment("Страница авторизации", String.format("Открылась страница авторизации %s", URL_LOGIN));
    }

    /**
     * Возврашает логин и пароль из файла в виде массива из двух значений
     */
    private String[] getLoginData() {
        cleanFile(fileNameReturner());
        return CsvLoader.loginReturner(Projects.BIO, Role.ADMIN);
    }

    @Parameters({"projectString", "roleString"})
    @Test(groups = "login")
    public void loginTest(String projectString, String roleString) {
        cleanFile(fileNameReturner());
        Projects project = Projects.valueOf(projectString);
        Role role = Role.valueOf(roleString);
        String[] pairForLogin = CsvLoader.loginReturner(project, role);
        goToLogin(URL_LOGIN);
        loginStep(pairForLogin[0], pairForLogin[1]);
        loginButtonClick();
        checkSuccessLogin();
    }

    @Test(groups = {"ЦУ1-1", TSU1, G0, START_END_WORK},
            description = "Открытие системы")
    @Link(name = "Статья: \"ЦУ1 Начало и конец работы\"", url = "https://wiki.goodt.me/x/7Qb6D")
    @Tag("ЦУ1-1")
    @TmsLink("60681")
    @Tag(TSU1)
    @Owner(BUTINSKAYA)
    public void openSystem() {
        goToLogin(URL_LOGIN);
        checkLoginPageOpened();
    }

    @Test(groups = {"ЦУ1-2", TSU1, G0, START_END_WORK},
            description = "Авторизация суперюзера")
    @Link(name = "Статья: \"ЦУ1 Начало и конец работы\"", url = "https://wiki.goodt.me/x/7Qb6D")
    @Tag("ЦУ1-2")
    @TmsLink("60681")
    @Tag(TSU1)
    @Owner(BUTINSKAYA)
    public void superuserAuthorization() {
        String[] pairForLogin = getLoginData();
        goToLogin(URL_LOGIN);
        loginStep(pairForLogin[0], pairForLogin[1]);
        loginButtonClick();
        checkSuccessLogin();
    }

    @Test(groups = {"ЦУ1-3", TSU1, G0, START_END_WORK},
            description = "Выход из системы")
    @Link(name = "Статья: \"ЦУ1 Начало и конец работы\"", url = "https://wiki.goodt.me/x/7Qb6D")
    @Tag("ЦУ1-3")
    @TmsLink("60681")
    @Tag(TSU1)
    @Owner(BUTINSKAYA)
    public void exitSystem() {
        goToBioSection(URL_HOME, true);
        clickExitButton();
        assertExit();
    }

    @Test(groups = {"BI-3.1", "TEST-339"}, description = "Просмотр списка пользователей")
    public void viewUserList() {
        goToBioSection(URL_HOME, true);
        clickOnSectionButton(VariantsSection.USERS);
        assertTransitionToUsers();
    }

    @Test(groups = {"BI-3.2", "TEST-339"}, description = "Создание пользователя для сотрудника")
    @Severity(value = SeverityLevel.BLOCKER)
    public void createUserForEmployee() {
        goToBioSection(URL_USERS, true);
        pressUserButton(VariantsOperation.ADD_USER);
        pressOnVariantsCreateNewUser(VariantsCreate.BIND_USER);
        pressUsersWithoutOrgButton();
        String name = clickOnRandomUser();
        String login = RandomStringUtils.randomAlphanumeric(8);
        sendTextInCreateNewUserInput(login, Inputs.LOGIN);
        sendTextInCreateNewUserInput(login, Inputs.PASS);
        sendTextInCreateNewUserInput(login, Inputs.CONFIRM_PASS);
        clickSaveLoginAndPassButton();
        clickCloseLoginAndPassButton();
        assertAddNewUser(name, login);
    }

    @Test(groups = {"BI-3.3", "TEST-339"}, description = "Создание пользователя с созданием сотрудника")
    public void createUserWithCreateNewEmployee() {
        goToBioSection(URL_USERS, true);
        pressUserButton(VariantsOperation.ADD_USER);
        pressOnVariantsCreateNewUser(VariantsCreate.CREATE_NEW);
        String family = RandomStringUtils.randomAlphabetic(8);
        String name = RandomStringUtils.randomAlphabetic(8);
        String patronymic = RandomStringUtils.randomAlphabetic(8);
        sendTextInCreateNewUserInput(family, Inputs.FAMILY);
        sendTextInCreateNewUserInput(name, Inputs.NAME);
        sendTextInCreateNewUserInput(patronymic, Inputs.PATRONYMIC);
        pressOnSaveButtonInAddNewUserForm();
        String login = RandomStringUtils.randomAlphanumeric(8);
        sendTextInCreateNewUserInput(login, Inputs.LOGIN);
        sendTextInCreateNewUserInput(login, Inputs.PASS);
        sendTextInCreateNewUserInput(login, Inputs.CONFIRM_PASS);
        clickSaveLoginAndPassButton();
        clickCloseLoginAndPassButton();
        assertAddNewUser(family + " " + name + " " + patronymic, login);
    }

    @Test(groups = {"BI-3.4.1", "TEST-500"}, description = "Добавление разрешений")
    public void addPermissions() {
        goToBioSection(URL_USERS, true);
        List<Person> allUsers = PersonRepository.getAllUsersWithUserName();
        String userName = findUserAndMakePreset(true, allUsers, false);
        clickOnUser(userName);
        clickOnPencilButtonInUserForm();
        clickOnAddPermissionButtonInUserForm();
        Permissions permissions = Permissions.randomPermission();
        clickOnPermissionsInUserForm(permissions);
        clickOnSelectPermissionButton();
        clickOnSavePermissionButton();
        assertManipulatePermission(permissions, null, userName, allUsers, true);
    }

    @Test(groups = {"BI-3.4.2", "TEST-500"}, description = "Добавление разрешений с ограничением по ОМ")
    public void addPermissionsWithRestrictionOM() {
        goToBioSection(URL_USERS, true);
        List<Person> allUsers = PersonRepository.getAllUsersWithUserName();
        String userName = findUserAndMakePreset(true, allUsers, false);
        clickOnUser(userName);
        clickOnPencilButtonInUserForm();
        clickOnAddPermissionButtonInUserForm();
        Permissions permissions = Permissions.randomPermission();
        clickOnPermissionsInUserForm(permissions);
        clickOnSelectPermissionButton();
        clickOnChangeListOrgNameButton();
        PersonGroups personGroups = getRandomFromList(PersonGroupsRepository.getPersonGroups());
        String orgName = personGroups.getName();
        List<List<String>> firstPath = CommonBioRepository.getPathsList(Collections.singletonList(personGroups.getId()));
        workWithTree(firstPath, Direction.DOWN);
        pressSaveInFilterButtonUsers();
        clickOnSavePermissionButton();
        assertManipulatePermission(permissions, orgName, userName, allUsers, true);
    }

    @Test(groups = {"BI-3.4.3", "TEST-500"}, description = "Редактирование списка оргЮнитов в разрешении, при отсутствии оргЮнитов")
    public void editPermissionWithoutOms() {
        goToBioSection(URL_USERS, true);
        List<Person> allUsers = PersonRepository.getAllUsersWithUserName();
        String userName = findUserAndMakePreset(false, allUsers, false);
        clickOnUser(userName);
        clickOnPencilButtonInUserForm();
        Permissions permission = pressOnRandomPermission(true);
        clickOnEditInUserPermission();
        PersonGroups personGroups = getRandomFromList(PersonGroupsRepository.getPersonGroups());
        String orgName = personGroups.getName();
        clickOnChangeListOrgNameButton();
        List<List<String>> firstPath = CommonBioRepository.getPathsList(Collections.singletonList(personGroups.getId()));
        workWithTree(firstPath, Direction.DOWN);
        pressSaveInFilterButtonUsers();
        clickOnSavePermissionButton();
        assertManipulatePermission(permission, orgName, userName, allUsers, true);
    }

    @Test(groups = {"BI-3.4.4", "TEST-500"}, description = "Редактирование списка оргЮнитов в разрешении, при уже имеющихся оргЮнитах")
    public void editPermissionWithOms() {
        goToBioSection(URL_USERS, true);
        List<Person> allUsers = PersonRepository.getAllUsersWithUserName();
        String userName = findUserAndMakePreset(false, allUsers, true);
        clickOnUser(userName);
        clickOnPencilButtonInUserForm();
        Permissions permission = pressOnRandomPermission(true);
        clickOnEditInUserPermission();
        PersonGroups personGroups = getRandomFromList(PersonGroupsRepository.getPersonGroups());
        String orgName = personGroups.getName();
        clickOnChangeListOrgNameButton();
        List<List<String>> firstPath = CommonBioRepository.getPathsList(Collections.singletonList(personGroups.getId()));
        workWithTree(firstPath, Direction.DOWN);
        pressSaveInFilterButtonUsers();
        clickOnSavePermissionButton();
        assertManipulatePermission(permission, orgName, userName, allUsers, true);
    }

    @Test(groups = {"BI-3.4.5", "TEST-339"}, description = "Редактирование списка ОМ у добавленного разрешения (удаление)")
    public void editingOMListForAddedPermissionDelete() {
        goToBioSection(URL_USERS, true);
        List<Person> allUsers = PersonRepository.getAllUsersWithUserName();
        String userName = findUserAndMakePreset(false, allUsers, true);
        clickOnUser(userName);
        clickOnPencilButtonInUserForm();
        Permissions permission = pressOnRandomPermission(true);
        clickOnEditInUserPermission();
        String restrictedOrgName = getRestrictedOrgNameFromUsers();
        PersonGroups orgName = PersonGroupsRepository.getRandOmOrgNameFromRestriction(restrictedOrgName);
        clickOnChangeListOrgNameButton();
        List<List<String>> firstPath = CommonBioRepository.getPathsList(Collections.singletonList(orgName.getId()));
        workWithTree(firstPath, Direction.DOWN);
        pressSaveInFilterButtonUsers();
        assertInValueDisappearsDeactivatedOM(orgName.getName());
        clickOnSavePermissionButton();
        assertManipulatePermission(permission, orgName.getName(), userName, allUsers, false);
    }

    @Test(groups = {"BI-3.4.6", "TEST-339"}, description = "Удаление разрешения")
    public void deletePermission() {
        goToBioSection(URL_USERS, true);
        List<Person> allUsers = PersonRepository.getAllUsersWithUserName();
        String userName = findUserAndMakePreset(false, allUsers, true);
        clickOnUser(userName);
        clickOnPencilButtonInUserForm();
        Permissions permission = pressOnRandomPermission(false);
        clickOnDeleteInUserPermission();
        assertDeletePermission(permission, userName, allUsers);
    }

    @Test(groups = {"BI-3.5", "TEST-1416"}, description = "Редактирование пользователя")
    public void editUser() {
        goToBioSection(URL_USERS, true);
        List<Person> allUsers = PersonRepository.getAllUsersWithUserName();
        String userName = findUserAndMakePreset(false, allUsers, false);
        clickOnUser(userName);
        pressUserButton(VariantsOperation.EDIT);
        String login = RandomStringUtils.randomAlphanumeric(8);
        sendTextInCreateNewUserInput(login, Inputs.LOGIN);
        sendTextInCreateNewUserInput(login, Inputs.PASS);
        sendTextInCreateNewUserInput(login, Inputs.CONFIRM_PASS);
        clickSaveLoginAndPassButton();
        clickCloseLoginAndPassButton();
        assertEditUser(userName, login);
    }

    @Test(groups = {"BI-3.6", "TEST-1416"}, description = "Удаление пользователя")
    public void deleteUser() {
        goToBioSection(URL_USERS, true);
        List<Person> allUsers = PersonRepository.getAllUsersWithUserName();
        String userName = findUserAndMakePreset(true, allUsers, false);
        clickOnUser(userName);
        pressUserButton(VariantsOperation.DELETE);
        assertDeleteUser(userName);
    }

    @Test(groups = {"BI-6.1", "TEST-340"}, description = "Загрузка журнала событий без указаний параметров")
    @Severity(value = SeverityLevel.NORMAL)
    public void downloadJournal() {
        goToBioSection(URL_JOURNAL, true);
        clickOnFindButtonInJournal();
        assertFindJournal();
    }

    @Test(groups = {"BI-6.2", "TEST-340"}, description = "Загрузка журнала событий с указанием параметров")
    public void downloadJournalWithParameters() {
        goToBioSection(URL_JOURNAL, true);
        LocalDate endDate = LocalDate.now().minusDays(new Random().nextInt(15) + 1);
        LocalDate startDate = endDate.minusDays(30);
        HashMap<String, List<CheckBoxAndStatus>> temp = PresetBioClass.getRandomOrgNameWithParameters(startDate, endDate);
        String orgName = temp.keySet().iterator().next();
        enterStartDateJournal(startDate);
        enterEndDateJournal(endDate);
        pressOnTripleDotEventJournal();
        clickOnRandomNumberCheckBoxEvent(temp.get(orgName));
        closeCheckBoxesForm();
        pressOnTripleDotOrgNameInJournal();
        sendInFindOrgMod(orgName);
        pressSaveInFilterButton();
        clickOnFindButtonInJournal();
        assertJournalWithParameters(startDate, endDate, temp);
    }

    @Test(groups = {"BI-6.2.1", "TEST-750"}, description = "Загрузка журнала событий с указанием невалидных параметров")
    public void downloadJournalWithInvalidParameters() {
        goToBioSection(URL_JOURNAL, true);
        LocalDate endDate = LocalDate.now().minusDays(new Random().nextInt(15) + 1);
        LocalDate startDate = endDate.minusDays(30);
        HashMap<String, List<CheckBoxAndStatus>> temp = PresetBioClass.getRandomOrgNameWithParameters(startDate, endDate);
        String orgName = temp.keySet().iterator().next();
        enterStartDateJournal(startDate);
        enterEndDateJournal(endDate);
        pressOnTripleDotEventJournal();
        clickOnRandomNumberCheckBoxEvent(temp.get(orgName));
        closeCheckBoxesForm();
        pressOnTripleDotOrgNameInJournal();
        sendInFindOrgMod(orgName);
        pressSaveInFilterButton();
        String invalidName = RandomStringUtils.randomAlphabetic(15);
        enterPersonName(invalidName);
        clickOnFindButtonInJournal();
        assertEmptyJournalWithParameters(invalidName);
    }

    @Test(groups = {"BI-6.3", "TEST-340"}, description = "Обновление журнала событий")
    public void refreshEventJournal() {
        goToBioSection(URL_JOURNAL, true);
        clickOnFindButtonInJournal();
        pressDoneRepeatButton();
        assertRefreshJournal();
    }

    @Test(groups = {"BI-6.4", "TEST-340"}, description = "Очистка журнала событий")
    public void clearEventJournal() {
        goToBioSection(URL_JOURNAL, true);
        clickOnFindButtonInJournal();
        pressClearJournalButton();
        assertClearJournal();
    }

    @Test(groups = {"BI-4.1", "TEST-337"}, description = "Просмотр списка терминалов")
    public void viewTerminalList() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        goToOrgUnitByEnteringNameInField(orgUnitName);
        assertPanelDisplayed(PersonGroupsRepository.getOrgUnitIdByName(orgUnitName));
    }

    @Test(groups = {"BI-4.2", "TEST-337"}, description = "Редактирование карточки терминала")
    @Severity(value = SeverityLevel.NORMAL)
    public void editingTerminalCard() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
        clickOnTerminalPencilButton();
        String randomName = RandomStringUtils.randomAlphabetic(10);
        String randomNumber = RandomStringUtils.randomAlphabetic(9);
        clearTerminalNameField();
        enterTerminalNameField(randomName);
        clearTerminalSerialNumber();
        enterTerminalSerialNumber(randomNumber);
        clickTerminalInfoSaveButton();
        checkNameAndSNumberChange(id, randomName, randomNumber);
    }

    @Test(groups = {"BI-4.3", "TEST-337"}, description = "Прикрепление ОМ к терминалу")
    @Severity(value = SeverityLevel.CRITICAL)
    public void addOrgUnitToTerminal() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        goToOrgUnitByEnteringNameInField(orgUnitName);
        PersonGroups personGroups = getRandomFromList(PersonGroupsRepository.getPersonGroups());
        int randomOrgUnitId = personGroups.getId();
        String name = personGroups.getName();
        clickOnTerminal(id);
        clickOnTerminalPencilButton();
        clickAddOrgUnitButton();
        clickOnUnitCheckBox(name);
        clickOrgUnitLinkInfoSaveButton();
        clickTerminalInfoSaveButton();
        checkAddingOrgUnitToTerminal(id, randomOrgUnitId);
    }

    @Test(groups = {"BI-4.9", "TEST-337"}, description = "Блокировка терминала")
    public void deactivateTerminal() {
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        PresetBioClass.presetForManageTerminalStatus(TerminalStatus.ACTIVE, id);
        goToBioSection(URL_TERMINAL, true);
        goToOrgUnitByEnteringNameInField(orgUnitName);
        TerminalStatus result = TerminalStatus.BLOCKED;
        clickOnTerminal(id);
        clickOnTerminalPencilButton();
        pullUpStatusRadioButton();
        clickTerminalInfoSaveButton();
        checkTerminalStatusSwitch(result, id);
    }

    @Test(groups = {"BI-4.10", "TEST-337"}, description = "Активация терминала")
    public void activateTerminal() {
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        PresetBioClass.presetForManageTerminalStatus(TerminalStatus.BLOCKED, id);
        goToBioSection(URL_TERMINAL, true);
        goToOrgUnitByEnteringNameInField(orgUnitName);
        TerminalStatus result = TerminalStatus.ACTIVE;
        clickOnTerminal(id);
        clickOnTerminalPencilButton();
        pullUpStatusRadioButton();
        clickTerminalInfoSaveButton();
        checkTerminalStatusSwitch(result, id);
    }

    @Test(groups = {"BI-4.13", "TEST-337"}, description = "Установка pin-кода")
    public void pinSetting() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
        clickOnTerminalPencilButton();
        String randomValue = RandomStringUtils.randomAlphabetic(10);
        enterPinValue(randomValue);
        clickTerminalInfoSaveButton();
        assertPinCodeChange(id);
    }

    @Test(groups = {"BI-4.14", "TEST-337"}, description = "Добавление нового терминала")
    public void addNewTerminal() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnAddNewTerminalButton();
        String randomName = RandomStringUtils.randomAlphabetic(10);
        String randomNumber = RandomStringUtils.randomAlphabetic(10);
        enterTerminalNameField(randomName);
        enterTerminalSerialNumber(randomNumber);
        PersonGroups personGroups = getRandomFromList(PersonGroupsRepository.getPersonGroups());
        int randomOrgUnitId = personGroups.getId();
        String name = personGroups.getName();
        clickAddOrgUnitButton();
        clickOnUnitCheckBox(name);
        clickOrgUnitLinkInfoSaveButton();
        clickTerminalInfoSaveButton();
        checkTerminalCreation(randomNumber, randomName, randomOrgUnitId);
    }

    @Test(groups = {"BI-4.4", "TEST-337"}, description = "Прикрепление пользователя к терминалу")
    public void addEmployeeToTerminal() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        ImmutablePair<Person, PersonGroups> map = PresetBioClass.presetForNeedTypeOfEmployee(EmployeeStatus.REMOVED, id);
        String empName = map.getLeft().getFirstName();
        String orgName = map.getRight().getName();
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
        terminalEmployeesEditPencilButton();
        searchForOrgUnit(orgName);
        clickIdentifyButton(EmployeeStatus.INCLUDE, empName);
        saveAttachedPersonsButtonClick();
        checkActionWithEmployee(Collections.singletonList(empName), id, EmployeeStatus.INCLUDE);
    }

    @Test(groups = {"BI-4.5", "TEST-337"}, description = "Удаление пользователя с терминала")
    public void deleteEmployeeFromTerminal() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        ImmutablePair<Person, PersonGroups> map = PresetBioClass.presetForNeedTypeOfEmployee(EmployeeStatus.INCLUDE, id);
        String empName = map.getLeft().getFirstName();
        String orgName = map.getRight().getName();
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
        terminalEmployeesEditPencilButton();
        searchForOrgUnit(orgName);
        deletePersonFromTerminal(EmployeeStatus.INCLUDE, empName);
        saveAttachedPersonsButtonClick();
        checkRemovePersonFromTerminal(Collections.singletonList(empName), id, EmployeeStatus.INCLUDE);
    }

    @Test(groups = {"BI-4.11.1", "TEST-337"}, description = "Назначение одного администратора на терминал")
    public void addOneAdminToTerminal() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        ImmutablePair<Person, PersonGroups> map = PresetBioClass.presetForNeedTypeOfEmployee(EmployeeStatus.REMOVED, id);
        String empName = map.getLeft().getFirstName();
        String orgName = map.getRight().getName();
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
        terminalEmployeesEditPencilButton();
        searchForOrgUnit(orgName);
        clickIdentifyButton(EmployeeStatus.ADMIN, empName);
        saveAttachedPersonsButtonClick();
        checkActionWithEmployee(Collections.singletonList(empName), id, EmployeeStatus.ADMIN);
    }

    @Test(groups = {"BI-4.12.1", "TEST-337"}, description = "Удаление одного администратора с терминала")
    public void deleteOneAdminsFromTerminal() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        ImmutablePair<Person, PersonGroups> map = PresetBioClass.presetForNeedTypeOfEmployee(EmployeeStatus.ADMIN, id);
        String empName = map.getLeft().getFirstName();
        String orgName = map.getRight().getName();
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
        terminalEmployeesEditPencilButton();
        searchForOrgUnit(orgName);
        deletePersonFromTerminal(EmployeeStatus.ADMIN, empName);
        saveAttachedPersonsButtonClick();
        checkRemovePersonFromTerminal(Collections.singletonList(empName), id, EmployeeStatus.ADMIN);
    }

    @Test(groups = {"BI-4.15", "TEST-337"}, description = "Раскрытие дерева ОМ")
    public void treeDisclosureOm() {
        goToBioSection(URL_TERMINAL, true);
        List<List<String>> firstPath = CommonBioRepository.getPathsList(Collections.singletonList(getRandomFromList(PersonGroupsRepository.getPersonGroups()).getId()));
        pressOnCentralOffice();
        workWithTree(firstPath, Direction.DOWN);
        workWithTree(firstPath, Direction.UP);
    }

    @Test(groups = {"BI-4.16", "TEST-337"}, description = "Раскрытие дерева при добавлении сотрудников")
    public void workWithTreeInEmployees() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
        terminalEmployeesEditPencilButton();
        List<List<String>> firstPath = CommonBioRepository.getPathsList(Collections.singletonList(getRandomFromList(PersonGroupsRepository.getPersonGroups()).getId()));
        workWithTreeInPopUpWindow(firstPath, Direction.DOWN);
        workWithTreeInPopUpWindow(firstPath, Direction.UP);
    }

    @Test(groups = {"BI-5.1", "TEST-338"}, description = "Просмотр списка персонала")
    public void viewStaffList() {
        goToBioSection(URL_PERSONAL, true);
        Map<String, List<String>> name = CommonBioRepository.getOMWithEmployeesAndIdOfEmployees();
        chooseOMBySearchField(name.keySet().stream().findFirst().orElse(null));
        List<Person> employees = PersonRepository.getAllEmployeesOfOM(name);
        assertEmployees(employees);
    }

    @Test(groups = {"BI-5.2", "TEST-338"}, description = "Просмотр карточки сотрудника")
    @Severity(value = SeverityLevel.CRITICAL)
    public void viewEmployeeCard() {
        goToBioSection(URL_PERSONAL, false);
        Map<String, List<String>> name = CommonBioRepository.getOMWithEmployeesAndIdOfEmployees();
        String om = name.keySet().stream().findFirst().orElse(null);
        chooseOMBySearchField(om);
        List<Person> employees = PersonRepository.getAllEmployeesOfOM(name);
        Person rndName = getRandomFromList(employees);
        chooseUser(rndName.getFullName());
        assertThatBiometricOpen(rndName);
    }

    @Test(groups = {"BI-5.3", "TEST-338"}, description = "Удаление фотографии у сотрудника")
    public void deleteAPhotoFromAnEmployee() {
        goToBioSection(URL_PERSONAL, false);
        Person person = PersonRepository.getRandomUsersBio();
        int numberOfPhotos = PresetBioClass.checkPositiveNumberOfDescriptors(person, true);
        chooseOMBySearchField(person.getPersonGroupPositions().get(0).getPersonGroupName());
        String userFullName = PersonRepository.getPersonById(person.getId()).getFullName();
        chooseUser(userFullName);
        clickOnTrashButton(numberOfPhotos);
        clickDeletePhoto();
        assertForDeletingPhoto(person, numberOfPhotos);
    }

    @Test(groups = {"BI-5.4", "TEST-338"}, description = "Удаление дескриптора у сотрудника полностью")
    public void deletePhotoFromAnEmployee() {
        goToBioSection(URL_PERSONAL, false);
        Person person = PersonRepository.getRandomUsersBio();
        int descriptorsSize = PresetBioClass.checkPositiveNumberOfDescriptors(person, false);
        chooseOMBySearchField(person.getPersonGroupPositions().get(0).getPersonGroupName());
        String userFullName = PersonRepository.getPersonById(person.getId()).getFullName();
        chooseUser(userFullName);
        clickOnDescriptorTrashButton(descriptorsSize);
        clickDeleteDescriptor();
        assertForDeletingDescriptor(person, descriptorsSize);
    }

    @Test(groups = {"BI-5.5", "TEST-338"}, description = "Удаление всех фотографий у сотрудника")
    private void deleteAllPhotosFromAnEmployee() {
        goToBioSection(URL_PERSONAL, false);
        Person person = PersonRepository.getRandomUsersBio();
        PresetBioClass.checkPositiveNumberOfDescriptors(person, true);
        chooseOMBySearchField(person.getPersonGroupPositions().get(0).getPersonGroupName());
        String userFullName = PersonRepository.getPersonById(person.getId()).getFullName();
        chooseUser(userFullName);
        selectAll();
        clickOnDeletePhotoButton();
        assertForDeletingAllPhotos(person);
    }

    @Test(groups = {"BI-4.6", "TEST-501"}, description = "Блокировка пользователя")
    public void blockTerminalUser() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        ImmutablePair<Person, PersonGroups> map = PresetBioClass.presetForNeedTypeOfEmployee(EmployeeStatus.INCLUDE, id);
        String empName = map.getLeft().getFirstName();
        String orgName = map.getRight().getName();
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
        terminalEmployeesEditPencilButton();
        searchForOrgUnit(orgName);
        clickIdentifyButton(EmployeeStatus.EXCLUDE, empName);
        saveAttachedPersonsButtonClick();
        checkActionWithEmployee(Collections.singletonList(empName), id, EmployeeStatus.EXCLUDE);
    }

    @Test(groups = {"BI-4.7", "TEST-501"}, description = "Установка времени автономной работы терминала")
    public void setBlockTimeout() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
        clickOnTerminalPencilButton();
        int newTimeout = getRandomNumberForBlockTimeOut(id);
        enterBlockTimeoutValue(newTimeout);
        clickTerminalInfoSaveButton();
        assertBlockTimeoutChange(id, newTimeout);
    }

    @Test(groups = {"BI-4.11.2", "TEST-337"}, description = "Назначение нескольких администраторов на терминал")
    public void addTwoAdmins() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        ImmutablePair<Person, PersonGroups> map = PresetBioClass.presetForNeedTypeOfEmployee(EmployeeStatus.REMOVED, id);
        ImmutablePair<Person, PersonGroups> map1 = PresetBioClass.presetForNeedTypeOfEmployee(EmployeeStatus.REMOVED, id);
        List<String> employees = new ArrayList<>();
        employees.add(map.getLeft().getFullName());
        employees.add(map1.getLeft().getFullName());
        String orgName = map.getRight().getName();
        String orgName1 = map1.getRight().getName();
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
        terminalEmployeesEditPencilButton();
        searchForOrgUnit(orgName);
        clickIdentifyButton(EmployeeStatus.ADMIN, employees.get(0));
        searchForOrgUnit(orgName1);
        clickIdentifyButton(EmployeeStatus.ADMIN, employees.get(1));
        saveAttachedPersonsButtonClick();
        checkActionWithEmployee(employees, id, EmployeeStatus.ADMIN);
    }

    @Test(groups = {"BI-4.12.2", "TEST-337"}, description = "Удаление нескольких администраторов с терминала")
    public void deleteTwoAdmins() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        ImmutablePair<Person, PersonGroups> map = PresetBioClass.presetForNeedTypeOfEmployee(EmployeeStatus.ADMIN, id);
        ImmutablePair<Person, PersonGroups> map1 = PresetBioClass.presetForNeedTypeOfEmployee(EmployeeStatus.ADMIN, id);
        List<String> employees = new ArrayList<>();
        employees.add(map.getLeft().getFullName());
        employees.add(map1.getLeft().getFullName());
        String orgName = map.getRight().getName();
        String orgName1 = map1.getRight().getName();
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
        terminalEmployeesEditPencilButton();
        searchForOrgUnit(orgName);
        clickIdentifyButton(EmployeeStatus.REMOVED, employees.get(0));
        searchForOrgUnit(orgName1);
        clickIdentifyButton(EmployeeStatus.REMOVED, employees.get(1));
        saveAttachedPersonsButtonClick();
        checkRemovePersonFromTerminal(employees, id, EmployeeStatus.ADMIN);
    }

    @Test(dataProvider = "UnitList", groups = {"changeConfigInfo", "TEST-499"},
            description = "Изменение параметра в конфиг файле терминала.")
    private void configTest(ConfigLine line, Object input) {
        String terminalId = getTerminalId();
        clickEditConfigButton();
        editButtonClick();
        editFile(line, input);
        saveButtonClick();
        checkThatLinesMatches(terminalId, line, input);
    }

    @BeforeGroups(value = "changeConfigInfo", alwaysRun = true)
    private void beforeDataProvider() {
        goToBioSection(URL_TERMINAL, true);
        HashMap<String, String> terminalAndOrgUnit = CommonBioRepository.getOrgUnitNameAttachedToTerminalId();
        String id = terminalAndOrgUnit.get("id");
        String orgUnitName = terminalAndOrgUnit.get(ORG_UNIT_JSON);
        goToOrgUnitByEnteringNameInField(orgUnitName);
        clickOnTerminal(id);
    }

    @Test(groups = {"BI-12.1", "TEST-647"}, description = "Выгрузка фотографий с журнала событий zip-файлом")
    public void downloadZipPhoto() {
        goToBioSection(URL_JOURNAL, true);
        clickOnFindButtonInJournal();
        clickOnZipDownloadButton();
        assertFileDownLoading(TypeOfPhotos.JOURNAL, "0");
    }

    @Test(groups = {"BI-12.2", "TEST-647"},
            description = "Выгрузка фотографий созданных для формирования биометрического дескриптора zip-файлом")
    public void downloadZipPhotoZ() {
        goToBioSection(URL_PERSONAL, false);
        Person person = PersonRepository.getRandomUsersBio();
        assertFileDownLoading(TypeOfPhotos.FACE_DESCRIPTORS, person.getId());
    }

    //Выставлен параметр priority, тест должен выполняться последним в прогоне
    @Test(dataProvider = "licenseCheck", groups = {"TEST-685"}, priority = 99,
            description = "Проверка загрузки файлов лицензии разных типов")
    private void licenseCheck(LicenseType type) {
        goToBioSection(URL_LICENCE, false);
        uploadFile();
        String response = PresetBioClass.uploadFileGetResponse(type);
        checkLicenseUploading(type, response);
    }
}