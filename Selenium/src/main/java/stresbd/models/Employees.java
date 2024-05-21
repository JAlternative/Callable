package stresbd.models;

public class Employees {

    private String action;
    private String active;
    private String birthday;
    private String email;
    private String firstName;
    private String gender;
    private String inn;
    private String lastName;
    private String outerId;
    private String patronymicName;
    private String snils;
    private final String stringFromFile;

    public Employees(String stringFromFile) {
        this.stringFromFile = stringFromFile;
    }

    public String getAction() {
        return lineSeparator()[0];
    }

    public String getActive() {
        return lineSeparator()[1];
    }

    public String getBirthday() {
        return lineSeparator()[2];
    }

    public String getEmail() {
        return lineSeparator()[3];
    }

    public String getFirstName() {
        return lineSeparator()[4];
    }

    public String getGender() {
        return lineSeparator()[5];
    }

    public String getInn() {
        return lineSeparator()[6];
    }

    public String getLastName() {
        return lineSeparator()[7];
    }

    public String getOuterId() {
        return lineSeparator()[8];
    }

    public String getPatronymicName() {
        return lineSeparator()[9];
    }

    public String getSnils() {
        return lineSeparator()[10];
    }

    private String[] lineSeparator() {
        return getStringFromFile().split(",");
    }

    private String getStringFromFile() {
        return stringFromFile;
    }

}
