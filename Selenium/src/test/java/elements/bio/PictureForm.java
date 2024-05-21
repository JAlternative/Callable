package elements.bio;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface PictureForm extends AtlasWebElement {
    @Name("Фсе фотографии с биометрией")
    @FindBy("//img")
    List<AtlasWebElement> photos();

    @Name("Крестики на фотографиях")
    @FindBy("//a[@class=\"btn pos-abs au-target\"]")
    List<AtlasWebElement> deleteCrossListPhotos();

    @Name("Кнопка \"Да\" во второй появившейся строчке")
    @FindBy("//body//div[@class='au-target pad-3']//div//div[2]//div[3]")
    AtlasWebElement deletePhoto();

    @Name("Кнопка \"Да\" в первой появившейся строчке")
    @FindBy("//div[@class='pos-abs text-center']//div[1]//div[3]")
    AtlasWebElement deleteDescriptor();
}
