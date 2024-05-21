package wfm.components.analytics;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public
enum StrategyList {
    FOT_MIN("Минимизация ФОТ, сохранение уровня сервиса"),
    LVL_UP("Повышение уровня сервиса, сохранение ФОТ"),
    FOT_MAX("Максимизация конверсии с увеличением ФОТ");

    private final String strategy;

    StrategyList(String s) {
        strategy = s;
    }

    public String getStrategy() {
        return strategy;
    }
}
