package pages;

import io.qameta.atlas.webdriver.AtlasWebElement;

public interface TreeBehavior extends AtlasWebElement {

    AtlasWebElement checkBoxButton(String name);

    AtlasWebElement chevronButton(String name);

    AtlasWebElement chevronButtonForMaster(String name);

}
