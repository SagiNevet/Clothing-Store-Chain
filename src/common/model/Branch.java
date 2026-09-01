package common.model;

public enum Branch {

    TEL_AVIV("Tel Aviv"),

    JERUSALEM("Jerusalem");

    private final String displayName;

    Branch(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
