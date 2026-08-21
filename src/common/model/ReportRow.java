package common.model;

import java.io.Serializable;

/**
 * One line of a sales report.
 * <p>
 * The same class serves every report of the system. A report by branch fills
 * {@link #getGroupName()} with the name of a branch, a report by product fills
 * it with the name of a product, and a report by category fills it with the
 * name of a category. The numbers mean the same thing in all three cases, so
 * one class and one table are enough.
 * </p>
 * <p>
 * The alternative - a class per report - would have meant three nearly
 * identical classes, three table models and three export methods. Here the
 * export code is written once and works for every report the chain will ever
 * add.
 * </p>
 */
public class ReportRow implements Serializable {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** What this line groups by: a branch, a product or a category. */
    private final String groupName;

    /** How many sales are counted in this line. */
    private final int numberOfSales;

    /** How many items were sold in this line. */
    private final int itemsSold;

    /** The total the customers paid, after their purchase plans. */
    private final double totalRevenue;

    /** The total the customers saved thanks to their purchase plans. */
    private final double totalDiscount;

    /**
     * Creates one line of a report.
     *
     * @param groupName     what this line groups by
     * @param numberOfSales how many sales are counted
     * @param itemsSold     how many items were sold
     * @param totalRevenue  the total actually paid
     * @param totalDiscount the total saved by the purchase plans
     */
    public ReportRow(String groupName, int numberOfSales, int itemsSold,
                     double totalRevenue, double totalDiscount) {
        this.groupName = groupName;
        this.numberOfSales = numberOfSales;
        this.itemsSold = itemsSold;
        this.totalRevenue = totalRevenue;
        this.totalDiscount = totalDiscount;
    }

    /**
     * Returns what this line groups by.
     *
     * @return the branch, product or category name
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Returns how many sales are counted in this line.
     *
     * @return the number of sales
     */
    public int getNumberOfSales() {
        return numberOfSales;
    }

    /**
     * Returns how many items were sold in this line.
     *
     * @return the number of items
     */
    public int getItemsSold() {
        return itemsSold;
    }

    /**
     * Returns the total the customers actually paid.
     *
     * @return the revenue of this line
     */
    public double getTotalRevenue() {
        return totalRevenue;
    }

    /**
     * Returns the total the customers saved thanks to their purchase plans.
     *
     * @return the discount given in this line
     */
    public double getTotalDiscount() {
        return totalDiscount;
    }

    @Override
    public String toString() {
        return groupName + ": " + numberOfSales + " sales, " + itemsSold + " items, "
                + totalRevenue + " paid, " + totalDiscount + " discount";
    }
}
