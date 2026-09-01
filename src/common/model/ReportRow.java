package common.model;

import java.io.Serializable;

public class ReportRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String groupName;

    private final int numberOfSales;

    private final int itemsSold;

    private final double totalRevenue;

    private final double totalDiscount;

    public ReportRow(String groupName, int numberOfSales, int itemsSold,
                     double totalRevenue, double totalDiscount) {
        this.groupName = groupName;
        this.numberOfSales = numberOfSales;
        this.itemsSold = itemsSold;
        this.totalRevenue = totalRevenue;
        this.totalDiscount = totalDiscount;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getNumberOfSales() {
        return numberOfSales;
    }

    public int getItemsSold() {
        return itemsSold;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    @Override
    public String toString() {
        return groupName + ": " + numberOfSales + " sales, " + itemsSold + " items, "
                + totalRevenue + " paid, " + totalDiscount + " discount";
    }
}
