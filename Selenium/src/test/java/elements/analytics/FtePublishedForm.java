package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface FtePublishedForm extends AtlasWebElement {

    @Name("Кнопка Закрыть")
    @FindBy("//h5[@t='dialogs.ftePublished.title']/ancestor::div[2]/button")
    AtlasWebElement ftePublishedCloseForm();

    @Name("Кнопка Опубликовать")
    @FindBy("//h5[@t='dialogs.ftePublished.title']/ancestor::div[3]//button[@t='common.actions.publish']")
    AtlasWebElement ftePublish();

    @Name("Кнопка Выбрать месяц")
    @FindBy("//h5[@t='dialogs.ftePublished.title']/ancestor::div[3]//button/i[contains(@class, 'calendar')]")
    AtlasWebElement ftePublishedMonth();

    @Name("Название месяца и года")
    @FindBy("//h5[@t='dialogs.ftePublished.title']/ancestor::div[3]/div[3]//*[@ref]")
    AtlasWebElement nameMonth();

}
