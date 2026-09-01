package client.gui;

import common.model.Employee;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class EmployeeTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMN_TITLES =
            {"Employee no.", "Full name", "Identity number", "Phone",
                    "Bank account", "Branch", "Role"};

    private static final int COLUMN_EMPLOYEE_NUMBER = 0;

    private static final int COLUMN_FULL_NAME = 1;

    private static final int COLUMN_ID_NUMBER = 2;

    private static final int COLUMN_PHONE = 3;

    private static final int COLUMN_BANK_ACCOUNT = 4;

    private static final int COLUMN_BRANCH = 5;

    private static final int COLUMN_ROLE = 6;

    private final List<Employee> employees = new ArrayList<>();

    public void setEmployees(List<Employee> newEmployees) {
        employees.clear();
        employees.addAll(newEmployees);
        fireTableDataChanged();
    }

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
