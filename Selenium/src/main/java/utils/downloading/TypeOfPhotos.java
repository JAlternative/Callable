package utils.downloading;

public enum TypeOfPhotos {
    JOURNAL("journal", "recognition"),
    FACE_DESCRIPTORS("face-descriptors", "photos");
    private final String type;
    private final String fileName;

    TypeOfPhotos(String type, String fileName) {
        this.type = type;
        this.fileName = fileName;
    }

    public String getType() {
        return type;
    }

    public String getFileName() {
        return fileName;
    }
}
