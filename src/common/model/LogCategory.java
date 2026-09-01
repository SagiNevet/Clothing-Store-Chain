package common.model;

public enum LogCategory {

    EMPLOYEES("employees.log"),

    CUSTOMERS("customers.log"),

    SALES("sales.log"),

    CHAT("chat.log");

    private final String fileName;

    LogCategory(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
