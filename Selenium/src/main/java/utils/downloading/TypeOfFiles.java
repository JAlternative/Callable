package utils.downloading;

public enum TypeOfFiles {
    XLSX(8080, "xlsx", "xlsx"),
    CSV(null, "csv", "csv"),
    ZIP(null, "xlsx-zip", "zip"),
    CSV_GRID(null, "csv-grid", "csv"),
    IMAGES(null, "images", ""),
    PDF(9000, "pdf", "pdf"),
    PDF_ONLY_SCHEDULE(9000, null, "pdf"),
    JSON(null, "json", "json"),
    HTML(null, "html", "html"),
    ONE_C(null, "dbf", "dbf");

    private final Integer portForType;
    private final String fileFormat;
    private final String fileExtension;


    TypeOfFiles(Integer portForType, String fileFormat, String fileExtension) {
        this.portForType = portForType;
        this.fileFormat = fileFormat;
        this.fileExtension = fileExtension;
    }

    public Integer getPortForType() {
        return portForType;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}

