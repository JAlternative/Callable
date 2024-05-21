package elements.messages;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface MessageListPanel extends AtlasWebElement {

    @Name("Список уведомлений с темой {{ subject }}")
    @FindBy("//td[contains(text(), '{{ subject }}')]/..//button[@click.delegate='expandMessage(message)']")
    ElementsCollection<AtlasWebElement> messageChevrons(@Param("subject") String subject);

    @Name("Часть текста уведомления")
    @FindBy(".//tr[@show.bind='messageExpanded[message.unique]']/td/p[contains(text(),'на привлечение {{ position }} {{ name }} на событие {{ request }}')]")
    AtlasWebElement message(@Param("position") String position, @Param("name") String name, @Param("request") String request);

    @Name("Кнопки для согласования запроса в уведомлении")
    @FindBy(".//a[text()='Согласовать']")
    ElementsCollection<AtlasWebElement> approveButtons();

    @Name("Кнопки для согласования запроса в уведомлении")
    @FindBy(".//a[text()='Запросить смену']")
    ElementsCollection<AtlasWebElement> askForShiftButtons();

    @Name("Все входящие сообщения")
    @FindBy(" //td[@innerhtml.bind='getMessageHtml(message)']")
    ElementsCollection<AtlasWebElement> allMessages();
}
