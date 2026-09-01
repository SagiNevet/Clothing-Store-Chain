package client.gui;

import common.model.Customer;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class CustomerTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMN_TITLES =
            {"Identity number", "Full name", "Phone", "Customer kind",
                    "Purchases", "Total spent", "Purchase plan"};

    private static final int COLUMN_ID_NUMBER = 0;

    private static final int COLUMN_FULL_NAME = 1;

    private static final int COLUMN_PHONE = 2;

    private static final int COLUMN_CUSTOMER_TYPE = 3;

    private static final int COLUMN_PURCHASE_COUNT = 4;

    private static final int COLUMN_TOTAL_SPENT = 5;

    private static final int COLUMN_PURCHASE_PLAN = 6;

    private final List<Customer> customers = new ArrayList<>();

    public void setCustomers(List<Customer> newCustomers) {
        customers.clear();
        customers.addAll(newCustomers);
        fireTableDataChanged();
    }

    public Customer getCustomerAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= customers.size()) {
            return null;
        }
        return customers.get(rowIndex);
    }

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
                
                return customer.getPurchasePlanDescription();
            default:
                return "";
        }
    }
}
