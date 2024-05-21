package testutils.cleanOldResults;

public enum ResultFileType {

    ATTACHMENT("attachment"),
    RESULT("result"),
    CONTAINER("container");

    private final String resultType;

    ResultFileType(String resultType) {
        this.resultType = resultType;
    }

    public String getResultType() {
        return resultType;
    }
}
