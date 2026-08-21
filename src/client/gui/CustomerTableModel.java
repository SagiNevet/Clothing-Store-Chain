package client.gui;

import common.model.Customer;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * The data behind the customers table.
 * <p>
 * <b>The last column of this table is the polymorphism of the project, visible
 * on screen.</b> It shows the purchase plan of every customer, and it is filled
 * by one single call:
 * </p>
 * <pre>
 *     customer.getPurchasePlanDescription()
 * </pre>
 * <p>
 * Three different sentences appear in that column, because three different
 * classes answer the call - and this model does not contain a single
 * {@code if} about the kind of customer. The objects arrived over the socket
 * and kept their real classes, which is what makes it work.
 * </p>
 */
public class CustomerTableModel extends AbstractTableModel {

    /** Serialization version, required because Swing models are serializable. */
    private static final long serialVersionUID = 1L;

    /** The titles of the columns, in the order they are displayed. */
    private static final String[] COLUMN_TITLES =
            {"Identity number", "Full name", "Phone", "Customer kind",
                    "Purchases", "Total spent", "Purchase plan"};

    /** The position of the identity number column. */
    private static final int COLUMN_ID_NUMBER = 0;

    /** The position of the name column. */
    private static final int COLUMN_FULL_NAME = 1;

    /** The position of the phone column. */
    private static final int COLUMN_PHONE = 2;

    /** The position of the customer kind column. */
    private static final int COLUMN_CUSTOMER_TYPE = 3;

    /** The position of the purchase count column. */
    private static final int COLUMN_PURCHASE_COUNT = 4;

    /** The position of the accumulated spending column. */
    private static final int COLUMN_TOTAL_SPENT = 5;

    /** The position of the purchase plan column. */
    private static final int COLUMN_PURCHASE_PLAN = 6;

    /** The customers currently displayed, one per row. */
    private final List<Customer> customers = new ArrayList<>();

    /**
     * Replaces every row with a freshly loaded customer list.
     *
     * @param newCustomers the customers to display
     */
    public void setCustomers(List<Customer> newCustomers) {
        customers.clear();
        customers.addAll(newCustomers);
        fireTableDataChanged();
    }

    /**
     * Returns the customer displayed in one row.
     *
     * @param rowIndex the row the user selected
     * @return the customer of that row, or {@code null} when the index is invalid
     */
    public Customer getCustomerAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= customers.size()) {
            return null;
        }
        return customers.get(rowIndex);
    }

    /**
     * Returns every customer currently displayed.
     *
     * @return a copy of the displayed customer list
     */
    public List<Customer> getCustomers() {
        return new ArrayList<>(customers);
    }

    @Override
    public int getRowCount() {
        return customers.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_TITLES.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return COLUMN_TITLES[columnIndex];
    }

    /**
     * Returns the value shown in one cell.
     *
     * @param rowIndex    the row of the cell
     * @param columnIndex the column of the cell
     * @return the value to display in that cell
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Customer customer = customers.get(rowIndex);
        switch (columnIndex) {
            case COLUMN_ID_NUMBER:
                return customer.getIdNumber();
            case COLUMN_FULL_NAME:
                return customer.getFullName();
            case COLUMN_PHONE:
                return customer.getPhone();
            case COLUMN_CUSTOMER_TYPE:
                return customer.getCustomerType().getDisplayName();
            case COLUMN_PURCHASE_COUNT:
                return customer.getPurchaseCount();
            case COLUMN_TOTAL_SPENT:
                return customer.getTotalSpent();
            case COLUMN_PURCHASE_PLAN:
                // The polymorphic call: three classes, three different answers.
                return customer.getPurchasePlanDescription();
            default:
                return "";
        }
    }
}
