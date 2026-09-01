package common.model;

public enum ReportType {

    SALES_BY_BRANCH("Sales by branch", "Branch"),

    SALES_BY_PRODUCT("Sales by product", "Product"),

    SALES_BY_CATEGORY("Sales by category", "Category");

    private final String title;

    private final String groupColumnTitle;

    ReportType(String title, String groupColumnTitle) {
        this.title = title;
        this.groupColumnTitle = groupColumnTitle;
    }

    public String getTitle() {
        return title;
    }

    public String getGroupColumnTitle() {
        return groupColumnTitle;
    }

    @Override
    public String toString() {
        return title;
    }
}
