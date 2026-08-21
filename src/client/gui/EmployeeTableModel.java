package client.gui;

import common.model.Employee;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * The data behind the employees table.
 * <p>
 * Notice which columns are missing: the password salt and the password hash.
 * They never leave the server at all, so there is nothing here to display even
 * by accident.
 * </p>
 */
public class EmployeeTableModel extends AbstractTableModel {

    /** Serialization version, required because Swing models are serializable. */
    private static final long serialVersionUID = 1L;

    /** The titles of the columns, in the order they are displayed. */
    private static final String[] COLUMN_TITLES =
            {"Employee no.", "Full name", "Identity number", "Phone",
                    "Bank account", "Branch", "Role"};

    /** The position of the employee number column. */
    private static final int COLUMN_EMPLOYEE_NUMBER = 0;

    /** The position of the name column. */
    private static final int COLUMN_FULL_NAME = 1;

    /** The position of the identity number column. */
    private static final int COLUMN_ID_NUMBER = 2;

    /** The position of the phone column. */
    private static final int COLUMN_PHONE = 3;

    /** The position of the bank account column. */
    private static final int COLUMN_BANK_ACCOUNT = 4;

    /** The position of the branch column. */
    private static final int COLUMN_BRANCH = 5;

    /** The position of the role column. */
    private static final int COLUMN_ROLE = 6;

    /** The employees currently displayed, one per row. */
    private final List<Employee> employees = new ArrayList<>();

    /**
     * Replaces every row with a freshly loaded employee list.
     *
     * @param newEmployees the employees to display
     */
    public void setEmployees(List<Employee> newEmployees) {
        employees.clear();
        employees.addAll(newEmployees);
        fireTableDataChanged();
    }

    /**
     * Adds one employee as a new row, unless that account is already displayed.
     * <p>
     * The check relies on {@code Employee.equals}, which compares employee
     * numbers, so an account cannot appear twice even if the event that carried
     * it arrives after a refresh that already included it.
     * </p>
     *
     * @param newEmployee the account that was just created
     */
    public void addEmployee(Employee newEmployee) {
        if (employees.contains(newEmployee)) {
            return;
        }
        employees.add(newEmployee);
        fireTableRowsInserted(employees.size() - 1, employees.size() - 1);
    }

    @Override
    public int getRowCount() {
        return employees.size();
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
        Employee employee = employees.get(rowIndex);
        switch (columnIndex) {
            case COLUMN_EMPLOYEE_NUMBER:
                return employee.getEmployeeNumber();
            case COLUMN_FULL_NAME:
                return employee.getFullName();
            case COLUMN_ID_NUMBER:
                return employee.getIdNumber();
            case COLUMN_PHONE:
                return employee.getPhone();
            case COLUMN_BANK_ACCOUNT:
                return employee.getBankAccountNumber();
            case COLUMN_BRANCH:
                return employee.getBranch().getDisplayName();
            case COLUMN_ROLE:
                return employee.getRole().getDisplayName();
            default:
                return "";
        }
    }
}
