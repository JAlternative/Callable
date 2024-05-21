package wfm.components.schedule;

/**
 * @author Vasily Nazarenko.
 */
public enum RepeatType {
    NEVER("Никогда"),
    ALWAYS("Всегда"),
    TO_DATE("До даты"),
    SAME_TIMES("Несколько раз"),
    ;

    private final String name;

    RepeatType(final String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }
}
