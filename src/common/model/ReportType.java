package common.model;

/**
 * The kinds of sales report the system can build.
 * <p>
 * The two required by the project are the first two. The third groups by
 * category, which the requirement mentions alongside grouping by product and
 * which costs almost nothing once the grouping is written once.
 * </p>
 */
public enum ReportType {

    /** How many sales each branch made. */
    SALES_BY_BRANCH("Sales by branch", "Branch"),

    /** How much of each product was sold. */
    SALES_BY_PRODUCT("Sales by product", "Product"),

    /** How much of each category was sold. */
    SALES_BY_CATEGORY("Sales by category", "Category");

    /** The title printed at the top of the report. */
    private final String title;

    /** The heading of the first column of the table. */
    private final String groupColumnTitle;

    /**
     * Creates a report kind.
     *
     * @param title            the title printed at the top of the report
     * @param groupColumnTitle the heading of the grouping column
     */
    ReportType(String title, String groupColumnTitle) {
        this.title = title;
        this.groupColumnTitle = groupColumnTitle;
    }

    /**
     * Returns the title printed at the top of the report.
     *
     * @return the report title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the heading of the grouping column.
     *
     * @return the column heading, for example {@code Branch}
     */
    public String getGroupColumnTitle() {
        return groupColumnTitle;
    }

    @Override
    public String toString() {
        return title;
    }
}
