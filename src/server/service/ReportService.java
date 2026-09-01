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

public class ReportService {

    private static final double MONEY_ROUNDING_FACTOR = 100.0;

    public List<ReportRow> buildReport(ReportType reportType, List<String> productIdFilter,
                                       List<ProductCategory> categoryFilter)
            throws StorageException {
        List<Sale> allSales = ServerContext.getInstance().getSalesRepository().loadAll();
        List<Sale> selectedSales = applyFilters(allSales, productIdFilter, categoryFilter);
        return groupSales(selectedSales, reportType);
    }

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

    private double roundToCents(double amount) {
        return Math.round(amount * MONEY_ROUNDING_FACTOR) / MONEY_ROUNDING_FACTOR;
    }
}
