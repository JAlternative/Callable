package bio.components.terminal;

public enum Side {
    LEFT("слева"),
    RIGHT("справа");

    private final String name;

    Side(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
