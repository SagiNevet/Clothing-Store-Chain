package server.service;

import common.exception.StorageException;
import common.model.Branch;
import common.model.ProductCategory;
import common.model.ReportRow;
import common.model.ReportType;
import common.model.Sale;
import server.core.ServerContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the sales reports out of the recorded sales.
 * <p>
 * <b>One grouping routine serves every report.</b> The three reports differ
 * only in the question "which key does this sale belong to": the branch, the
 * product, or the category. Everything after that - counting the sales, adding
 * up the items, the money and the discounts - is identical, so it is written
 * once in {@link #groupSales}.
 * </p>
 * <p>
 * The reports are built from {@code Sale} objects and never from the current
 * inventory or the current customer list. A sale froze the price, the product
 * name and the kind of customer at the moment it happened, which is what makes
 * a report of last month still correct after a price change.
 * </p>
 */
public class ReportService {

    /** The number of digits kept when rounding a money amount. */
    private static final double MONEY_ROUNDING_FACTOR = 100.0;

    /**
     * Builds a report of the requested kind.
     *
     * @param reportType       the kind of report to build
     * @param productIdFilter  the products to include, or {@code null} for all
     * @param categoryFilter   the categories to include, or {@code null} for all
     * @return the lines of the report, in a stable order
     * @throws StorageException if the sales file cannot be read
     */
    public List<ReportRow> buildReport(ReportType reportType, List<String> productIdFilter,
                                       List<ProductCategory> categoryFilter)
            throws StorageException {
        List<Sale> allSales = ServerContext.getInstance().getSalesRepository().loadAll();
        List<Sale> selectedSales = applyFilters(allSales, productIdFilter, categoryFilter);
        return groupSales(selectedSales, reportType);
    }

    /**
     * Keeps only the sales the user asked to see.
     * <p>
     * An empty or missing filter means "everything", which is what makes the
     * unfiltered report a special case of the filtered one instead of a
     * separate method.
     * </p>
     *
     * @param allSales        every recorded sale
     * @param productIdFilter the products to include, or {@code null} for all
     * @param categoryFilter  the categories to include, or {@code null} for all
     * @return the sales that passed both filters
     */
    private List<Sale> applyFilters(List<Sale> allSales, List<String> productIdFilter,
                                    List<ProductCategory> categoryFilter) {
        boolean filterByProduct = productIdFilter != null && !productIdFilter.isEmpty();
        boolean filterByCategory = categoryFilter != null && !categoryFilter.isEmpty();

        List<Sale> selectedSales = new ArrayList<>();
        for (Sale sale : allSales) {
            if (filterByProduct && !productIdFilter.contains(sale.getProductId())) {
                continue;
            }
            if (filterByCategory && !categoryFilter.contains(sale.getCategory())) {
                continue;
            }
            selectedSales.add(sale);
        }
        return selectedSales;
    }

    /**
     * Adds the sales up into one line per group.
     * <p>
     * A {@link LinkedHashMap} is used rather than a {@code HashMap} so the rows
     * come out in a stable order. A report whose rows jump around between two
     * runs looks broken even when the numbers are right.
     * </p>
     *
     * @param sales      the sales to summarise
     * @param reportType the kind of report, which decides the grouping key
     * @return one line per group
     */
    private List<ReportRow> groupSales(List<Sale> sales, ReportType reportType) {
        Map<String, int[]> countsByGroup = new LinkedHashMap<>();
        Map<String, double[]> moneyByGroup = new LinkedHashMap<>();

        for (Sale sale : sales) {
            String groupKey = groupKeyOf(sale, reportType);

            int[] counts = countsByGroup.computeIfAbsent(groupKey, key -> new int[2]);
            counts[0] += 1;
            counts[1] += sale.getQuantity();

            double[] money = moneyByGroup.computeIfAbsent(groupKey, key -> new double[2]);
            money[0] += sale.getFinalPrice();
            money[1] += sale.getDiscountAmount();
        }

        List<ReportRow> rows = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : countsByGroup.entrySet()) {
            String groupKey = entry.getKey();
            double[] money = moneyByGroup.get(groupKey);
            rows.add(new ReportRow(groupKey, entry.getValue()[0], entry.getValue()[1],
                    roundToCents(money[0]), roundToCents(money[1])));
        }
        return rows;
    }

    /**
     * Decides which group one sale belongs to.
     *
     * @param sale       the sale to place
     * @param reportType the kind of report being built
     * @return the name of the group this sale belongs to
     */
    private String groupKeyOf(Sale sale, ReportType reportType) {
        switch (reportType) {
            case SALES_BY_BRANCH:
                return sale.getBranch().getDisplayName();
            case SALES_BY_PRODUCT:
                return sale.getProductName() + " (" + sale.getProductId() + ")";
            case SALES_BY_CATEGORY:
                return sale.getCategory().getDisplayName();
            default:
                return "Unknown";
        }
    }

    /**
     * Makes sure every branch appears in a report by branch, even one that sold
     * nothing at all.
     * <p>
     * A missing row is easy to misread as "the report is broken". A row of
     * zeros says clearly that the branch sold nothing.
     * </p>
     *
     * @param rows the rows produced by the grouping
     * @return the rows, with a line of zeros added for every silent branch
     */
    public List<ReportRow> addBranchesWithNoSales(List<ReportRow> rows) {
        List<ReportRow> completeRows = new ArrayList<>(rows);
        for (Branch branch : Branch.values()) {
            boolean branchAppears = rows.stream()
                    .anyMatch(row -> row.getGroupName().equals(branch.getDisplayName()));
            if (!branchAppears) {
                completeRows.add(new ReportRow(branch.getDisplayName(), 0, 0, 0.0, 0.0));
            }
        }
        return completeRows;
    }

    /**
     * Rounds an amount of money to the nearest cent.
     *
     * @param amount the amount to round
     * @return the amount rounded to two decimal places
     */
    private double roundToCents(double amount) {
        return Math.round(amount * MONEY_ROUNDING_FACTOR) / MONEY_ROUNDING_FACTOR;
    }
}
