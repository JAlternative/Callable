package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface PublishForm extends AtlasWebElement {

    @Name("Кнопка календарь")
    @FindBy(".//i[contains(@class, 'calendar')]/../../button[@click.trigger]")
    AtlasWebElement buttonPublishCalendar();

    @Name("Поле с текущим периодом")
    @FindBy("//div[@show.bind='showShiftsPublishedDialog' and not(contains(@class, 'hide'))]//i[contains(@class, 'calendar')]/..//button")
    AtlasWebElement datePeriodField();

    @Name("Кнопка закрыть форму")
    @FindBy("//div[@show.bind='showShiftsPublishedDialog' and not(contains(@class, 'hide'))]//i[contains(@class, 'close')]/..")
    AtlasWebElement closeButton();

    @Name("Кнопка \"Опубликовать\"")
    @FindBy(".//button[@class='mdl-button mdl-button--primary au-target mdl-js-button'][contains(text(),'Опубликовать')]")
    AtlasWebElement buttonPublish();

    @Name("Кнопка \"Отклонить\"")
    @FindBy(".//button[@class='mdl-button mdl-button--primary au-target mdl-js-button'][contains(text(),'Отклонить')]")
    AtlasWebElement buttonRejectPublish();

    @Name("Сообщение статуса публикации")
    @FindBy(".//span[@t.bind='state']")
    AtlasWebElement elementPublishAssertSign();

    @Name("Снэкбар после нажатия на кнопку опубликовать")
    @FindBy("//div[@class='mdl-snackbar au-target mdl-snackbar--active']/div[contains(@class, 'text')]")
    AtlasWebElement elementSnackbar();

    @Name("Дата, время и статус публикации")
    @FindBy(".//div[contains(@class.bind,'roster')]//span[2]")
    AtlasWebElement dateOfPublish();
}
