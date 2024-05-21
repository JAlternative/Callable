package elements.bioTerminal;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SettingsPage extends AtlasWebElement {

    @Name("Кнопка \"Выход\"")
    @FindBy("//span[contains(text(), 'Выход')]/..")
    AtlasWebElement exitButton();

    @Name("Кнопка настроек")
    @FindBy("//a[text()='Настройки']")
    AtlasWebElement clientSettingsTab();

    @Name("Кнопка настроек")
    @FindBy("//a[text()='Тест камер']")
    AtlasWebElement cameraTestTab();

    @Name("\"Основные настройки\"")
    @FindBy("./div[@class=\"grow\"]//div[@class=\"grow\"]")
    AtlasWebElement basicSettingsPanel();

    @Name("Слайдер {name}")
    @FindBy("//div[contains(text(), '{{ name }}')]/../../..//ux-switch")
    AtlasWebElement turnButton(@Param("name") String name);

    @Name("{index} слайдер {name}")
    @FindBy("(//td[contains(text(), '{{ name }}')]/..//ux-switch)[{{ index }}]")
    AtlasWebElement turnStreamSlider(@Param("name") String name, @Param("index") int index);

    @Name("{index}-я кнопка редактирования стрима")
    @FindBy("(//table//i[@class = 'mdi mdi-pencil'])[{{ index }}]")
    AtlasWebElement pencilStreamButton(@Param("index") int index);

    @Name("{index}-я кнопка редактирования стрима")
    @FindBy("//compose[@model.bind = 'getCamera{{ index }}()']//table//i[@class = 'mdi mdi-check']")
    AtlasWebElement acceptStreamButton(@Param("index") int index);

    @Name("{index}-я строка названия источника стрима")
    @FindBy("(//table//input[@type = 'text'])[{{ index }}]")
    AtlasWebElement streamInputField(@Param("index") int index);

    @Name("{index}-я строка названия источника стрима")
    @FindBy("//compose[@model.bind = 'getCamera{{ index }}()']//compose[@model.bind = 'videoinputMaskVariant']//select")
    AtlasWebElement maskSelectButton(@Param("index") int index);

    @Name("{index}-я строка названия источника стрима")
    @FindBy("//compose[@model.bind = 'getCamera{{ index }}()']//compose[@model.bind = 'videoinputMaskVariant']//select//option")
    ElementsCollection<AtlasWebElement> allMasks(@Param("index") int index);

    @Name("{index}-я строка названия источника стрима")
    @FindBy("//compose[@model.bind = 'getCamera{{ index }}()']//compose[@model.bind = 'cameraAlign']//select//option")
    ElementsCollection<AtlasWebElement> allCameraLocated(@Param("index") int index);

    @Name("{index}-я строка названия источника стрима")
    @FindBy("//compose[@model.bind = 'getCamera{{ index }}()']//compose[@model.bind = 'cameraAlign']//select")
    AtlasWebElement cameraLocatedSelectButton(@Param("index") int index);

    //3 для первой камеры 4 для второй
    @Name("Надпись с тектом \"Применён поворот на 180 градусов\"")
    @FindBy("//div[@class='grow']/div[3]/div[{{ num }}]//ux-switch/..")
    AtlasWebElement camerasTurnText(@Param("num") int num);

    @Name("Свитчер для автоматического перехода на главную страницу")
    @FindBy("//div[@class='grow']/div[2]/div[@class='columns'][2]//ux-switch")
    AtlasWebElement mainPageRedirectSwitcher();

    @Name("Кнопка маски выбранным названием")
    @FindBy("//div[text()=' {{ type }} ']/..")
    AtlasWebElement maskTypeButton(@Param("type") String type);

    @Name("Значок галочки, для принятия изменения поля ввода текста")
    @FindBy("//i[@class='mdi mdi-check']")
    AtlasWebElement acceptTextChangingButton();

    @Name("Значок карандаша для определенного блока")
    @FindBy("//div[contains(text(), '{{ name }}')]/../../..//i")
    AtlasWebElement pencilButton(@Param("name") String name);

    @Name("Активное поле ввода текста")
    @FindBy("//input[@type='text' and not (@disabled)]")
    AtlasWebElement textInputField();

    @Name("Иконка статуса подключения к центральному серверу")
    @FindBy("//div[contains(@class.bind, 'statusConnect')]")
    AtlasWebElement connectionStatusIcon();

    @Name("Список из всех активных полей ввода текста")
    @FindBy("//input[@type='text' and not (@disabled)]")
    ElementsCollection<AtlasWebElement> textInputFields();

    @Name("Кнопка метода работы: {method}")
    @FindBy("//div[contains(text(), '{{ method }}' )]/..")
    AtlasWebElement workingMethodButton(@Param("method") String method);

    @Name("Полоска для выставления времени")
    @FindBy("//div[contains(text(), '{{ name }}')]/../../..//input")
    AtlasWebElement sliderInput(@Param("name") String name);

    @Name("Текущее значение периода отображения результата распознования")
    @FindBy("//div[@class = 'columns'][{{ position }}]//div[contains(text(), 'Текущее значение:')]/span")
    AtlasWebElement currentTimeValue(@Param("position") int position);

    @Name("Кнопка управления выбранного типа")
    @FindBy("//div[contains(text(), '{{ name }}')]/..")
    AtlasWebElement controlButton(@Param("name") String name);

    @Name("Кнопка выбранного режима авторизации")
    @FindBy("//div[contains(text(), 'Варианты отметок')]/../../..//div[contains(@class, 'columns')]/div[contains(text(), '{{ type }}')]")
    AtlasWebElement authRadioButtonWithType(@Param("type") String type);

    @Name("Кнопка выбранного режима авторизации")
    @FindBy("//div[contains(text(), 'Варианты отметок')]/../../..//div[contains(@class, 'columns') and (contains(@class, 'selected'))]/div[contains(text(), '{{ type }}')]")
    AtlasWebElement authRadioButtonIsChecked(@Param("type") String type);

    @Name("Панель текста уведомления о скрытии камеры 2+")
    @FindBy("//div[contains(text(), ' Отображать только 1 камеру ')]/../../..//span[@class ='is-primary']")
    AtlasWebElement hiddenSecondCamText();

}
