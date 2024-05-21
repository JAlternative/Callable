package wfm.components.analytics;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public
enum AlgorithmList {
    DEFAULT_ALGORITHM("По умолчанию"),
    PRIORITY("С учётом отработанных смен (приоритетный)"),
    ALTERNATIVE("С учётом отработанных смен (альтернативный)"),
    BY_PERFORMANCE("Расчёт по производительности по трафику в час"),
    BY_GROUPS("По группам");

    private final String algorithm;

    AlgorithmList(String s) {
        algorithm = s;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
