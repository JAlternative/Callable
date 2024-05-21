package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface PictureForm extends AtlasWebElement {

    @Name("Фсе фотографии с биометрией, не считая пустых дескрипторов")
    @FindBy("//img[contains(@src, 'bio-images')]")
    ElementsCollection<AtlasWebElement> photos();

    @Name("Только пустые дескрипторы")
    @FindBy("//img")
    ElementsCollection<AtlasWebElement> descriptors();

    @Name("Кнопки \"Корзин\" на фотографиях, не включая дескрипторы")
    @FindBy("//img[contains(@src, 'bio-images')]/../div//i")
    ElementsCollection<AtlasWebElement> onlyPhotosTrashButtons();

    @Name("Кнопки \"Корзин\" только дескрипторы")
    @FindBy("//img[contains(@src, 'image')]/../div//i")
    ElementsCollection<AtlasWebElement> deleteCrossListDescriptors();

    @Name("Кнопка \"Да\" во второй появившейся строчке")
    @FindBy("//span[contains(@class, 'mdi-image-off')]/../../div[3]")
    AtlasWebElement deletePhoto();

    @Name("Кнопка \"Да\" в первой появившейся строчке")
    @FindBy("//span[contains(@class, 'mdi-account-remove')]/../../div[3]")
    AtlasWebElement deleteDescriptor();

    @Name("Хедер с имененм выбранного сотрудника")
    @FindBy(".//div[@class='text-h2']")
    AtlasWebElement userName();

    @FindBy("//div[@class='au-target pad-3 is-loading']")
    AtlasWebElement loadingPanel();

    @Name("Чекбокс, выделяющий всех")
    @FindBy("//div[@class='cf']//i")
    AtlasWebElement buttonToSelectAll();

    @Name("Кнопка \"Удалить фото\"")
    @FindBy("//span[@click.delegate='deleteBioPhotos()']")
    AtlasWebElement deleteBioPhotosButton();

}
