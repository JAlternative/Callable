package stresbd.models;

public class MathParam {

    private String action;
    private String entityOuterId;
    private String entityType;
    private String mathParameterOuterId;
    private String value;
    private final String stringFromFile;

    public MathParam(String stringFromFile) {
        this.stringFromFile = stringFromFile;
    }

    public String getAction() {
        return lineSeparator()[0];
    }

    public String getEntityOuterId() {
        return lineSeparator()[1];
    }

    public String getEntityType() {
        return lineSeparator()[2];
    }

    public String getMathParameterOuterId() {
        return lineSeparator()[3];
    }

    public String getValue() {
        return lineSeparator()[4];
    }

    private String[] lineSeparator() {
        return getStringFromFile().split(",");
    }

    private String getStringFromFile() {
        return stringFromFile;
    }

}
