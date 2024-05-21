package utils.downloading;

public enum TypeOfAcceptContent {

    BASIC("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"),
    PDF_XLSX("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");

    private final String acceptContent;

    TypeOfAcceptContent(String acceptContent) {
        this.acceptContent = acceptContent;
    }

    public String getAcceptContent() {
        return acceptContent;
    }

}
