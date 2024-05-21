package utils.tools;

import io.qameta.allure.Allure;
import io.qameta.atlas.webdriver.WebPage;

//import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.Reporter;
import utils.BuildInfo;
import wfm.HasLinks;
import wfm.PresetClass;
import wfm.components.schedule.ScheduleRequestType;
import wfm.components.schedule.SystemProperties;
import wfm.components.systemlists.IntervalType;
import wfm.models.AdditionalWork;
import wfm.models.ScheduleRequestAlias;
import wfm.models.SystemProperty;
import wfm.repository.ScheduleRequestAliasRepository;
import wfm.repository.SystemPropertyRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static utils.Params.*;

/**
 * Набоор всякой всячины для работы с тестами
 */
public class CustomTools {

    private static final Logger LOG = LoggerFactory.getLogger(CustomTools.class);
    private static final String SYSTEM_PROPERTY_CHANGED = "System property changed for test";
    private static final String SYSTEM_PROPERTY_FOR_REPORT = "System property required for test";
    private static final String SYSTEM_LIST_ADDED_CONTEXT_PREFIX = "SystemListAddedForTest_";
    private static final String VALUE_CHANGED = "Значение изменено с %s на %s";
    private static final Random RANDOM = new Random();

    private CustomTools() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Метод создает случайную буквенную строку из символов от "a" до "z", длиной 10 знаков
     * перед случайной строкой имеется символ "-"
     */
    public static String stringGenerator() {
        RandomStringGenerator generator = new RandomStringGenerator.Builder()
                .withinRange('a', 'z').build();
        return "-" + generator.generate(10);
    }

    /**
     * Создает случайный емайл
     */
    public static String generateRandomEmail() {
        int length1 = RANDOM.nextInt(10) + 3;
        int length2 = RANDOM.nextInt(10) + 3;
        int length3 = RANDOM.nextInt(3) + 2;
        String symbolLeft = RandomStringUtils.randomAlphanumeric(length1);
        String symbolRight = RandomStringUtils.randomAlphanumeric(length2);
        String symbolDomain = RandomStringUtils.randomAlphabetic(length3);
        return symbolLeft + "@" + symbolRight + "." + symbolDomain;
    }

    /**
     * Берет случайный фрагмент из текста длиной не меньше 3 символов
     */
    public static String getRandomPartOfName(String name) {
        int length = name.length();
        if (length < 4) {
            return name;
        }
        int firstLetter;
        int lastLetter;
        if (length < 7) {
            firstLetter = RANDOM.ints(0, length - 3 + 1).iterator().next();
        } else {
            firstLetter = RANDOM.ints(0, length / 2 + 1).iterator().next();
        }
        lastLetter = RANDOM.ints(firstLetter + 3, length + 1).iterator().next();
        return name.substring(firstLetter, lastLetter);
    }

    public static String encoder(File file) {
        String base64Image = "";
        try (FileInputStream imageInFile = new FileInputStream(file)) {
            byte[] imageData = new byte[(int) file.length()];
            imageInFile.read(imageData);
            base64Image = Base64.getEncoder().encodeToString(imageData);
        } catch (FileNotFoundException e) {
            LOG.info("Изображение не было найдено", e);
        } catch (IOException ioe) {
            LOG.info("Не удалось прочитать изображение {}", ioe);
        }
        return base64Image;
    }

    /**
     * Медленный ввод текста в строку
     *
     * @param webElement - куда вводим
     * @param keysToSend - текст который вводим
     */
    public static void slowSendKeys(AtlasWebElement webElement, String keysToSend) {
        char[] chars = keysToSend.toCharArray();
        for (char c : chars) {
            webElement.sendKeys(String.valueOf(c));
            CustomTools.systemSleep(0.15); //цикл
        }
    }

    /**
     * Задать ожидание системы в секундах.
     *
     * @param sec - секунд ожидания, может быть дробным числом.
     */
    public static void systemSleep(double sec) {
        try {
            Thread.sleep((long) sec * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ждет пока элемент не будет кликабельным.
     *
     * @param element - элемент который ждем
     * @param webPage - драйвер страницы
     * @param seconds - сколько ждем
     */
    public static void waitForClickable(WebElement element, WebPage webPage, int seconds) {
        WebDriverWait waitForTask = new WebDriverWait(webPage.getWrappedDriver(), seconds);
        waitForTask.until(ExpectedConditions.elementToBeClickable(element));
    }

    /**
     * Переключает окно браузера на новую страницу и закрывает старую
     *
     * @param webPage - актуальная страница
     */
    public static void removeFirstWindowHandler(WebPage webPage) {
        String oldTab = webPage.getWrappedDriver().getWindowHandle();
        ArrayList<String> newTab = new ArrayList<>(webPage.getWrappedDriver().getWindowHandles());
        newTab.remove(oldTab);
        webPage.getWrappedDriver().close();
        webPage.getWrappedDriver().switchTo().window(newTab.get(0));
    }

    /**
     * отправляем лист, получаем случайное значение из листа
     */
    public static <T> T getRandomFromList(List<T> list) {
        if (list.isEmpty()) {
            throw new AssertionError("Не удалось выбрать случайный элемент из списка, так как список пуст");
        }
        return list.get(RANDOM.nextInt(list.size()));
    }

    /**
     * Возвращает заданное количество случайных значений из списка
     */
    public static <T> List<T> getRandomFromList(List<T> list, int numberOfItems) {
        int length = list.size();
        if (length < numberOfItems) {
            throw new AssertionError("Запрошено больше элементов, чем есть в списке");
        }
        for (int i = length - 1; i >= length - numberOfItems; --i) {
            Collections.swap(list, i, RANDOM.nextInt(i + 1));
        }
        return list.subList(length - numberOfItems, length);
    }

    /**
     * Сделано для получения случайного значения из стрима вставлается в коллектор по типу stream.collect(randomItem());
     * если лист пустой то вернет нулл
     */
    public static <T> Collector<T, List<T>, T> randomItem() {
        return Collector.of(ArrayList::new, List::add, ListUtils::union,
                            list -> list.isEmpty() ? null : list.get(RANDOM.nextInt(list.size())));
    }

    /**
     * Возвращает список классов образованных от массива джисон
     * !Если класс который будем конструировать является подклассом, то в параметр tClass нужно указать и основной класс
     * например Person.PersonGroupPositions
     *
     * @param jsonArray - массив джисон объектов
     * @param tClass    - класс который мы хотим сделать
     * @return список объектов класса с данными из джисона
     */
    public static <T> List<T> getListFromJsonArray(JSONArray jsonArray, Class<T> tClass) {
        List<T> tArrayList = new ArrayList<>();
        if (jsonArray != null && jsonArray.length() > 0) {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                T classObjectFromJson = getClassObjectFromJson(tClass, jsonObject);
                tArrayList.add(classObjectFromJson);
            }
        }
        return tArrayList;
    }

    /**
     * Возвращает список классов образованных от объекта джисон у которого внутри есть JSONArray
     * !Если класс который будем конструировать является подклассом, то в параметр tClass нужно указать и основной класс
     * например Person.PersonGroupPositions
     *
     * @param jsonObject - джисон объект с массивом джисон
     * @param tClass     - класс который мы хотим сделать
     * @return список объектов класса с данными из джисона,
     * если массива внутри объекта не было, просто вернет пустой лист
     */
    public static <T> List<T> getListFromJsonObject(JSONObject jsonObject, Class<T> tClass) {
        JSONArray jsonArray = getJsonArrayFromJsonObject(jsonObject);
        return getListFromJsonArray(jsonArray, tClass);
    }

    /**
     * Конструирует любой класс от джисон объекта
     *
     * @param clazz - класс объект которого хотим сконструировать
     * @param json  - джисон объект с экземпляром класса описанного в models
     * @return экземпляр класс с данными из джисона
     */
    public static <T> T getClassObjectFromJson(Class<T> clazz, JSONObject json) {
        try {
            return clazz.getConstructor(JSONObject.class).newInstance(json);
        } catch (InstantiationException e) {
            throw new AssertionError("Для класса: " + clazz.getName()
                                             + " нет конструктора по умолчанию или объект класса является абстрактным, интерфейсом, массивом," +
                                             " примитивом, или void. \nОшибка:" + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new AssertionError("Вероятно нет доступа к полям или к самому классу: " + clazz.getName() +
                                             "\nОшибка:" + e.getMessage());
        } catch (InvocationTargetException e) {
            e.getTargetException().printStackTrace();
            throw new AssertionError("Неправильно описан класс сущности: " + clazz.getName() + ".\n" +
                                             "Взято несуществующее поле, для поля JSON объекта выбран не тот ключ или берется не тот тип данных: \n"
                                             + e.getCause().getMessage());
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Приватный конструктор или нет конструктора от JSONObject для " +
                                             clazz.getName() + ".\nОшибка: " + e.getMessage());
        }
    }

    /**
     * Проходится по ключам джисон объекта и берет первый же джисон массив, возвращает его,
     * если не находит массив, то возвращает null
     */
    public static JSONArray getJsonArrayFromJsonObject(JSONObject jsonObject) {
        jsonObject.remove(LINKS);
        JSONArray jsonArray = null;
        for (String key : jsonObject.keySet()) {
            if (jsonArray == null) {
                Class<?> jsonClass = jsonObject.opt(key).getClass();
                if (jsonClass.isAssignableFrom(JSONArray.class)) {
                    jsonArray = jsonObject.getJSONArray(key);
                }
                if (jsonObject.optJSONObject(key) != null) {
                    jsonArray = getJsonArrayFromJsonObject(jsonObject.getJSONObject(key));
                }
            } else {
                break;
            }
        }
        return jsonArray;
    }

    public static <T> void changeProperty(SystemProperties prop, T newValue) {
        changeProperty(prop, newValue, Reporter.getCurrentTestResult().getTestContext());
    }

    /**
     * Меняет значение системной настройки. Записывает сделанные и несделанные (если настройка уже имеет нужное значение)
     * в соответствующие мэпы в тестовом контексте.
     *
     * @param prop     Системная настройка, которую нужно изменить
     * @param newValue новое значение настройки
     * @param c        тестовый контекст
     */
    public static <T> void changeProperty(SystemProperties prop, T newValue, ITestContext c) {
        SystemProperty property = SystemPropertyRepository.getSystemProperty(prop);
        String key = property.getKey();
        T value = (T) property.getValue();
        if (!Objects.equals(newValue, value)) {
            recordSystemSettingInContext(c, SYSTEM_PROPERTY_CHANGED, prop, value);
            recordSystemSettingInContext(c, SYSTEM_PROPERTY_FOR_REPORT, key, newValue);
            LOG.info("Значение системной настройки {} изменено с \"{}\" на \"{}\"", key, value, newValue);
            PresetClass.setSystemPropertyValue(prop, newValue);
        } else {
            recordSystemSettingInContext(c, SYSTEM_PROPERTY_FOR_REPORT, key, value);
        }
    }

    /**
     * Записывает системную настройку и ее значение в меп в тестовом контексте.
     * Нужно для дальнейшего восстановления исходного значения настройки и отображения в отчете актуального для теста значения.
     *
     * @param c            тестовый контекст
     * @param settingGroup название атрибута, в который нужно записать настройку
     * @param key          ключ (для записи в отчет) или енам (для восстановления) настройки
     * @param value        значение настройки
     */
    private static <T> void recordSystemSettingInContext(ITestContext c, String settingGroup, T key, T value) {
        Map<T, T> changedProperties = c.getAttribute(settingGroup) != null
                ? (Map<T, T>) c.getAttribute(settingGroup) : new HashMap<>();
        if ((key.equals(SystemProperties.ROSTER_QUIT_TAB_NOTICE.getKey()) || key.equals(SystemProperties.TWO_FACTOR_AUTH.getKey()))
                && settingGroup.equals(SYSTEM_PROPERTY_FOR_REPORT)) {
            changedProperties.put(key, value);
        }
        changedProperties.putIfAbsent(key, value);
        c.setAttribute(settingGroup, changedProperties);
    }

    /**
     * Записывает в отчет все настройки, значения которых были изменены или проверялись до прогона.
     */
    public static <T> void recordSystemPropertiesInReport(ITestContext c, BuildInfo info) {
        Map<String, T> requiredSettings = (Map<String, T>) c.getAttribute(SYSTEM_PROPERTY_FOR_REPORT);
        StringBuilder allureSettings = new StringBuilder();
        if (info != null) {
            allureSettings.append("Стенд: ").append(info.getShortName())
                    .append(" (")
                    .append(info.getVersion())
                    .append(")")
                    .append("\n\n");
        }
        if (requiredSettings != null) {
            allureSettings.append("Системные настройки:\n\n");
            for (Map.Entry<String, T> entry : requiredSettings.entrySet()) {
                allureSettings.append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("\n\n");
            }
        }
        Allure.description(String.valueOf(allureSettings));
        c.removeAttribute(SYSTEM_PROPERTY_FOR_REPORT);
    }

    /**
     * Восстанавливает системные настройки, измененные в ходе теста.
     */
    public static <T> void revertChangedSystemProperties(ITestContext c) {
        Map<SystemProperties, T> changedProperties = c.getAttribute(SYSTEM_PROPERTY_CHANGED) != null
                ? (Map<SystemProperties, T>) c.getAttribute(SYSTEM_PROPERTY_CHANGED) : new HashMap<>();
        if (!changedProperties.isEmpty()) {
            for (Map.Entry<SystemProperties, T> entry : changedProperties.entrySet()) {
                changeProperty(entry.getKey(), entry.getValue(), c);
            }
            c.removeAttribute(SYSTEM_PROPERTY_CHANGED);
            c.removeAttribute(SYSTEM_PROPERTY_FOR_REPORT);
        }
    }

    /**
     * Устанавливает настройку "Отображать" для типа запроса расписания
     *
     * @param type     тип запроса расписания
     * @param newValue новое значение настройки "Отображать"
     */
    public static ScheduleRequestAlias changeSystemListEnableValue(ScheduleRequestType type, boolean newValue) {
        return changeSystemListEnableValue(type, null, newValue);
    }

    public static ScheduleRequestAlias changeSystemListEnableValue(ScheduleRequestType type, IntervalType intervalType, boolean newValue) {
        ITestContext c = Reporter.getCurrentTestResult().getTestContext();
        ScheduleRequestAlias alias = null;
        if (type.equals(ScheduleRequestType.NON_APPEARANCE) && intervalType != null) {
            alias = ScheduleRequestAliasRepository.getAliasTypeRequestSchedule(type, intervalType);
        } else {
            alias = ScheduleRequestAliasRepository.getAliasTypeRequestSchedule(type);
        }
        if (Objects.isNull(alias)) {
            c.setAttribute(SYSTEM_LIST_ADDED_CONTEXT_PREFIX + type.getName(), "");
            alias = PresetClass.addScheduleRequestType(type, intervalType, true);
            LOG.info("Создан тип запроса расписания {} с названием {}", type, type.getName());
        }
        if (alias.isEnabled() != newValue) {
            PresetClass.changeRequestAliasProperties(alias, alias.copy().setEnabled(newValue));
            LOG.info("Значение типа запроса расписания {} изменено с {} на {}", type.getName(), alias.isEnabled(), newValue);
            Allure.addAttachment(String.format("Изменение значения типа запроса расписания \"%s\"", type.getName()),
                                 String.format(VALUE_CHANGED, alias.isEnabled(), newValue));
        }
        return alias;
    }

    /**
     * Устанавливает настройку "Автоподтверждение запроса" для типа запроса расписания
     *
     * @param type     тип запроса расписания
     * @param newValue новое значение настройки "Автоподтверждение"
     */
    public static void changeSystemListAutoApproveValue(ScheduleRequestType type, boolean newValue) {
        ScheduleRequestAlias oldAlias = ScheduleRequestAliasRepository.getAliasByName(type.getName());
        if (oldAlias.isAutoApprove() != newValue) {
            PresetClass.changeRequestAliasProperties(oldAlias, oldAlias.copy().setAutoApprove(newValue));
            LOG.info("Значение настройки \"Автоподтверждение запроса\" типа запроса расписания {} изменено с {} на {}", type.getName(),
                     oldAlias.isAutoApprove(), newValue);
            Allure.addAttachment(String.format("Изменение значения настройки \"Автоподтверждение\" типа запроса расписания \"%s\"", type.getName()),
                                 String.format(VALUE_CHANGED, oldAlias.isAutoApprove(), newValue));

        }
    }

    /**
     * Устанавливает настройку "Требует согласования" для типа запроса расписания
     *
     * @param type     тип запроса расписания
     * @param newValue новое значение настройки "Требует согласования"
     */
    public static void changeSystemListRequireApprovalValue(ScheduleRequestType type, boolean newValue) {
        ITestContext c = Reporter.getCurrentTestResult().getTestContext();
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getAliasByName(type.getName());
        if (alias.getRequireApproval() != newValue) {
            PresetClass.changeRequestAliasProperties(alias, alias.copy().setRequireApproval(newValue));
            LOG.info("Значение настройки \"Требует подтверждения\" типа запроса расписания {} изменено с {} на {}", type.getName(), alias.getRequireApproval(), newValue);
            Allure.addAttachment(String.format("Изменение значения настройки \"Требует подтверждения\" типа запроса расписания \"%s\"", type.getName()),
                                 String.format(VALUE_CHANGED, alias.getRequireApproval(), newValue));

        }
    }

    /**
     * Устанавливает настройку "Отдавать смену на биржу" для типа запроса расписания
     *
     * @param type     тип запроса расписания
     * @param newValue новое значение настройки "Отдавать смену на биржу"
     */
    public static void changeSystemListMoveToExchangeValue(ScheduleRequestType type, boolean newValue) {
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getAlias(type);
        if (alias.getMoveToExchange() != newValue) {
            PresetClass.changeRequestAliasProperties(alias, alias.copy().setMoveToExchange(newValue));
            LOG.info("Значение настройки \"Отдавать смену на биржу\" типа запроса расписания {} изменено с {} на {}", type.getName(),
                     alias.getMoveToExchange(), newValue);
            Allure.addAttachment(String.format("Изменение значения настройки \"Отдавать смену на биржу\" типа запроса расписания \"%s\"", type.getName()),
                                 String.format(VALUE_CHANGED, alias.getMoveToExchange(), newValue));

        }
    }

    /**
     * Устанавливает настройку "Привязка" для типа запроса расписания
     *
     * @param type     тип запроса расписания
     * @param newValue новое значение настройки "Привязка"
     */
    public static void changeSystemListBindValue(ScheduleRequestType type, boolean newValue) {
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getAliasByName(type.getName());
        if (alias.getBindToPosition() != newValue) {
            PresetClass.changeRequestAliasProperties(alias, alias.copy().setBindToPosition(newValue));
            LOG.info("Значение настройки \"Привязка к назначению\" типа запроса расписания {} изменено с {} на {}", type.getName(),
                     alias.getBindToPosition(), newValue);
            Allure.addAttachment(String.format("Изменение значения настройки \"Отдавать смену на биржу\" типа запроса расписания \"%s\"", type.getName()),
                                 String.format(VALUE_CHANGED, alias.getBindToPosition(), newValue));

        }
    }

    /**
     * Удаляет доп. работы, созданные для теста
     */
    public static void revertAdditionalWorksAndRules() {
        ITestContext c = Reporter.getCurrentTestResult().getTestContext();
        List<String> workAttributes = c.getAttributeNames().stream()
                .filter(a -> a.startsWith("Additional_work"))
                .collect(Collectors.toList());
        if (workAttributes.isEmpty()) {
            return;
        }
        for (String attribute : workAttributes) {
            AdditionalWork work = (AdditionalWork) c.getAttribute(attribute);
            PresetClass.deleteAdditionalWorkAndRules(work);
        }
    }

    /**
     * Берет у объекта, имплементирующего интерфейс HasLinks, ссылку на себя
     */
    public static <T extends HasLinks> String getSelfLink(T object) {
        try {
            return (String) object.getClass().getMethod("getSelfLink").invoke(object);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void checkBrowserAvailability(WebDriver driver) {
        Assert.assertFalse(driver.getWindowHandle().contains("session timed out or not found"), "Браузер недоступен");
    }

    /**
     * @return пароль, содержащий спец символы, цифры и буквы в верхнем и нижнем регистрах
     */
    public static String generatePassword() {
        StringBuilder numbers = new Random().ints(3, 48, 57).collect(StringBuilder::new,
                                                                     StringBuilder::appendCodePoint, StringBuilder::append);
        StringBuilder upperCase = new Random().ints(3, 65, 90).collect(StringBuilder::new,
                                                                       StringBuilder::appendCodePoint, StringBuilder::append);
        StringBuilder lowerCase = new Random().ints(3, 97, 122).collect(StringBuilder::new,
                                                                        StringBuilder::appendCodePoint, StringBuilder::append);
        StringBuilder specialSymbols = new Random().ints(3, 33, 47).collect(StringBuilder::new,
                                                                            StringBuilder::appendCodePoint, StringBuilder::append);

        String password = String.valueOf(numbers.append(specialSymbols).append(upperCase).append(lowerCase));
        LOG.info("Password generated: {}", password);
        return password;

    }
}
