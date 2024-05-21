package testutils;

import elements.analytics.DataNavSwitcher;
import elements.analytics.DiagramChart;
import elements.analytics.DiagramChartLeft;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import pages.AnalyticsPage;
import wfm.components.utils.ColorsColumns;
import wfm.components.utils.Direction;
import wfm.components.utils.GraphScope;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Класс для работы с барами на диаграмме в модуле "Аналитика"
 */
public class CheckScopeColumns {

    private static final Logger LOG = LoggerFactory.getLogger(CheckScopeColumns.class);
    private final DataNavSwitcher dataNavSwitcher;
    private final AnalyticsPage ap;
    private final Direction direction;
    private DiagramChart diagramChart;
    private DiagramChartLeft diagramChartLeft;

    public CheckScopeColumns(AnalyticsPage ap, Direction direction) {
        this.ap = ap;
        this.dataNavSwitcher = ap.dataNavSwitcher();
        this.direction = direction;
        if (direction == Direction.LEFT) {
            this.diagramChartLeft = ap.diagramChartLeft();
        } else if (direction == Direction.RIGHT) {
            this.diagramChart = ap.diagramChart();
        }
    }

    /**
     * Определение текущего масштаба на графике, проверка на активность элемента
     *
     * @return текущий масштаб графика
     */
    private GraphScope scopeDetector() {
        List<AtlasWebElement> tempList = new ArrayList<>();
        tempList.add(dataNavSwitcher.dayScope());
        tempList.add(dataNavSwitcher.monthScope());
        tempList.add(dataNavSwitcher.yearScope());
        String some = tempList.stream()
                .filter(element -> element.getAttribute("class").contains("active"))
                .map(element -> element.getAttribute("data-index"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Нет элементов в списке"));
        return Arrays.stream(GraphScope.values()).filter(graphScope -> graphScope.ordinal() == Integer.parseInt(some))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Нет элементов в списке"));
    }

    /**
     * Утилитный метод, определяет есть у текущего элемента требуемый атрибут
     *
     * @param attributeName - название атрибута
     * @param element       - текущий элемент
     * @return возвращает false если у текущего элемента нет требуемого атрибута, иначе true
     */
    private boolean isAttributePresent(String attributeName, AtlasWebElement element) {
        boolean result = false;
        try {
            String value = element.getAttribute(attributeName);
            if (value != null) {
                result = true;
            }
        } catch (Exception ex) {
            LOG.info("У текущего элемента {} нет атрибута: {}", element.getTagName(), attributeName);
        }
        return result;
    }

    /**
     * Добавление элементов в одну мапу с учетом требуемого цвета и текущего масштаба
     *
     * @param certainColor - требуемый цвет
     * @return мапа элементов требуемого цвета взависимости от текущего масштаба
     */
    public Map<String, AtlasWebElement> certainColorBarReturner(ColorsColumns certainColor) {
        ap.getWrappedDriver().manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

        Map<String, AtlasWebElement> currentColorMap = new HashMap<>();
        ElementsCollection<AtlasWebElement> indexListDefault = null;
        ElementsCollection<AtlasWebElement> indexListLeft = null;
        boolean leftSideFlag;
        leftSideFlag = direction == Direction.LEFT;

        if (scopeDetector() == GraphScope.DAY) {
            if (leftSideFlag) {
                indexListLeft = diagramChartLeft.indexGraphDay();
            } else {
                indexListDefault = diagramChart.indexGraphDay();
            }
        } else {
            if (leftSideFlag) {
                indexListLeft = diagramChartLeft.indexGraphYearOrMonth();
            } else {
                indexListDefault = diagramChart.indexGraphYearOrMonth();
            }
        }

        if (leftSideFlag) {
            if (!diagramChartLeft.colorColumnsNum(certainColor.getColorName()).isEmpty()) {
                for (int i = 0; i < indexListLeft.size(); i++) {
                    List<AtlasWebElement> tempElementList = new ArrayList<>(diagramChartLeft.barsFromCertainPosition(i)
                                                                                    .filter(element -> isAttributePresent("height", element))
                                                                                    .filter(element -> Integer.parseInt(element.getAttribute("height").split("\\.")[0]) != 0)
                                                                                    .filter(element -> element.getAttribute("class").split("__")[1]
                                                                                            .split("--")[0].contains(certainColor.getColorName())));
                    if (tempElementList.size() == 0) {
                        currentColorMap.put(indexListLeft.get(i).getText(), null);
                    } else if (tempElementList.size() == 1) {
                        currentColorMap.put(indexListLeft.get(i).getText(), tempElementList.get(0));
                    } else if (tempElementList.size() == 2) {
                        double size1 = Double.parseDouble(tempElementList.get(0).getAttribute("height"));
                        double size2 = Double.parseDouble(tempElementList.get(1).getAttribute("height"));
                        if (size1 < size2) {
                            currentColorMap.put(indexListLeft.get(i).getText(), tempElementList.get(1));
                        } else {
                            currentColorMap.put(indexListLeft.get(i).getText(), tempElementList.get(0));
                        }
                    } else {
                        Assert.fail("Больше двух столбцов одинакового цвета");
                    }
                }
            } else {
                for (AtlasWebElement extendedWebElement : indexListLeft) {
                    currentColorMap.put(extendedWebElement.getText(), null);
                }
            }
        } else {
            if (!diagramChart.colorColumnsNum(certainColor.getColorName()).isEmpty()) {
                for (int i = 0; i < indexListDefault.size(); i++) {
                    List<AtlasWebElement> tempElementList = new ArrayList<>(diagramChart.barsFromCertainPosition(i)
                                                                                    .filter(element -> isAttributePresent("height", element))
                                                                                    .filter(element -> Integer.parseInt(element.getAttribute("height").split("\\.")[0]) != 0)
                                                                                    .filter(element -> element.getAttribute("class").split("__")[1]
                                                                                            .split("--")[0].contains(certainColor.getColorName())));
                    if (tempElementList.size() == 0) {
                        currentColorMap.put(indexListDefault.get(i).getText(), null);
                    } else if (tempElementList.size() == 1) {
                        currentColorMap.put(indexListDefault.get(i).getText(), tempElementList.get(0));
                    } else if (tempElementList.size() == 2) {
                        double size1 = Double.parseDouble(tempElementList.get(0).getAttribute("height"));
                        double size2 = Double.parseDouble(tempElementList.get(1).getAttribute("height"));
                        if (size1 < size2) {
                            currentColorMap.put(indexListDefault.get(i).getText(), tempElementList.get(1));
                        } else {
                            currentColorMap.put(indexListDefault.get(i).getText(), tempElementList.get(0));
                        }
                    } else {
                        Assert.fail("Больше двух столбцов одинакового цвета");
                    }
                }
            } else {
                for (AtlasWebElement extendedWebElement : indexListDefault) {
                    currentColorMap.put(extendedWebElement.getText(), null);
                }
            }
        }
        ap.getWrappedDriver().manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        return currentColorMap;
    }

    /**
     * Для работы со всеми барами на диаграмме
     *
     * @return все бары соответствующего цвета по месяцам
     */
    public Map<String, Map<ColorsColumns, AtlasWebElement>> allBarsReturner() {
        Map<String, AtlasWebElement> greenMap = certainColorBarReturner(ColorsColumns.GREEN);
        Map<String, AtlasWebElement> orangeMap = certainColorBarReturner(ColorsColumns.ORANGE);
        Map<String, AtlasWebElement> greyMap = certainColorBarReturner(ColorsColumns.GREY);
        Map<String, AtlasWebElement> purpleMap = certainColorBarReturner(ColorsColumns.PURPLE);
        Map<String, AtlasWebElement> blueMap = certainColorBarReturner(ColorsColumns.BLUE_GREY);
        Map<String, Map<ColorsColumns, AtlasWebElement>> mainMap = new HashMap<>();
        for (String s : greenMap.keySet()) {
            Map<ColorsColumns, AtlasWebElement> tempMap = new HashMap<>();
            tempMap.put(ColorsColumns.GREEN, greenMap.get(s));
            tempMap.put(ColorsColumns.ORANGE, orangeMap.get(s));
            tempMap.put(ColorsColumns.GREY, greyMap.get(s));
            tempMap.put(ColorsColumns.PURPLE, purpleMap.get(s));
            tempMap.put(ColorsColumns.BLUE_GREY, blueMap.get(s));
            mainMap.put(s, tempMap);
        }
        return mainMap;
    }

    /**
     * Очищает представления всех баров без нулов. update.
     *
     * @param allBars - веб элементы всех цветов
     */
    public void clearColumnMap(Map<String, Map<ColorsColumns, AtlasWebElement>> allBars) {
        allBars.forEach((key, value) -> value.values().removeIf(Objects::isNull));
    }

}

