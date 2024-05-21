package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface CommentsForm extends AtlasWebElement {

    @Name("Комментарии напротив {{ index }} дня")
    @FindBy(".//div[@style.bind]/div/div[{{ index }}]//input[@disabled.bind=\"isUser\"]")
    AtlasWebElement spaceComments(@Param("index") int index);

    @Name("Кнопка \"Изменить\"")
    @FindBy(".//button[contains(text(), 'Изменить')]")
    AtlasWebElement changeButton();

    @Name("Кнопка, удаляющая комментарий к версиям, номер которого соответствует параметру локатора")
    @FindBy(".//input[@id = 'roster-version-{{ index }}']/../../../../button")
    AtlasWebElement cleanVersionButton(@Param("index") int index);

    @Name("Кнопка, удаляющая комментарий к дням, номер которого соответствует параметру локатора")
    @FindBy(".//div[contains(@class, 'flat')][{{ index }}]//button[contains(@click.trigger, 'clean')]")
    AtlasWebElement cleanCommentDayButton(@Param("index") int index);

    @Name("Поле ввода комментария с его порядковым номером для версии расчета")
    @FindBy(".//div[contains(@class, 'flat')][{{ index }}]//input[contains(@id, 'roster-description-')]")
    AtlasWebElement inputFieldCommentRoster(@Param("index") int index);

    @Name("Поле ввода комментария с его порядковым номером для дня")
    @FindBy(".//div[contains(@class, 'flat')][{{ index }}]//input[contains(@id, 'comment-text-')]")
    AtlasWebElement inputFieldCommentDay(@Param("index") int index);

    @Name("Пустые поля для комментариев")
    @FindBy(".//div[contains(@class, 'flat')]//button[@style='visibility: hidden;']/..//input[contains(@id, 'comment-text-')]")
    List<AtlasWebElement> inputFieldCommentsDay();

    @Name("Пустые поля для комментариев версии расчета")
    @FindBy(".//div[contains(@class, 'flat')]//button[@style='visibility: hidden;']/..//input[contains(@id, 'roster-description-')]")
    List<AtlasWebElement> inputFieldCommentsRosters();

    @Name("Поля с версиями графиков")
    @FindBy("//input[contains(@id, 'roster-version-')]")
    ElementsCollection<AtlasWebElement> rosterVersionsList();

    @Name("\"Крестик\" в правом верхнем углу")
    @FindBy(".//button[@click.trigger='close()']")
    AtlasWebElement closeCommentsFormButton();

    @Name("Поле ввода комментария {version} версии расписания")
    @FindBy(".//input[@value.bind ='roster.description']")
    ElementsCollection<AtlasWebElement> inputsCommentaryRoster();


}
