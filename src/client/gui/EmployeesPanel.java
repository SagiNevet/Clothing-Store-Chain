package client.gui;

import client.controller.EmployeeController;
import common.model.Branch;
import common.model.Employee;
import common.model.PasswordPolicy;
import common.model.Role;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.ServerEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * The employee management screen, available to a shift manager only.
 * <p>
 * It does the two things the requirements ask of the administration screen:
 * creating employee accounts, and defining the password policy those accounts
 * must satisfy.
 * </p>
 * <p>
 * <b>This screen is one half of the parallelism demonstration.</b> Creating an
 * account here and selling a shirt in another window are handed to two
 * different threads of the business pool, and the server console prints both
 * with overlapping timestamps.
 * </p>
 */
public class EmployeesPanel extends ServerBackedPanel {

    /** Serialization version, required because Swing components are serializable. */
    private static final long serialVersionUID = 1L;

    /** The smallest minimum password length a manager may configure. */
    private static final int SMALLEST_MINIMUM_LENGTH = 4;

    /** The largest minimum password length a manager may configure. */
    private static final int LARGEST_MINIMUM_LENGTH = 32;

    /** The data behind the table. */
    private final transient EmployeeTableModel tableModel = new EmployeeTableModel();

    /** The table showing the employee accounts. */
    private final JTable employeesTable = new JTable(tableModel);

    /** The line at the bottom reporting what happened. */
    private final JLabel statusLabel = new JLabel(" ");

    /** The controller that performs the employee actions. */
    private final transient EmployeeController employeeController = new EmployeeController();

    /**
     * Builds the employee management screen and loads the accounts.
     */
    public EmployeesPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        employeesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        employeesTable.setAutoCreateRowSorter(true);

        add(createToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(employeesTable), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        refreshEmployees();
    }

    /**
     * Builds the row of buttons above the table.
     *
     * @return the toolbar panel
     */
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(actionEvent -> refreshEmployees());
        toolbar.add(refreshButton);

        JButton addButton = new JButton("Create account");
        addButton.addActionListener(actionEvent -> openAddEmployeeDialog());
        toolbar.add(addButton);

        JButton policyButton = new JButton("Password policy");
        policyButton.addActionListener(actionEvent -> openPasswordPolicyDialog());
        toolbar.add(policyButton);

        return toolbar;
    }

    /**
     * Loads the employee list from the server.
     */
    private void refreshEmployees() {
        runInBackground("employees-refresh", () -> {
            List<Employee> employees = employeeController.loadEmployees();
            SwingUtilities.invokeLater(() -> {
                tableModel.setEmployees(employees);
                showStatus(employees.size() + " employee accounts in the chain");
            });
        });
    }

    /**
     * Fetches the current policy and then shows the account creation dialog.
     * <p>
     * The policy is fetched first so the dialog can display the rules next to
     * the password field. The user should not have to guess them and be refused.
     * </p>
     */
    private void openAddEmployeeDialog() {
        runInBackground("policy-for-new-account", () -> {
            PasswordPolicy policy = employeeController.loadPasswordPolicy();
            SwingUtilities.invokeLater(() -> askForEmployeeDetails(policy));
        });
    }

    /**
     * Shows the account creation dialog and sends the new account.
     *
     * @param policy the password policy the chosen password must satisfy
     */
    private void askForEmployeeDetails(PasswordPolicy policy) {
        JTextField employeeNumberField = new JTextField();
        JTextField fullNameField = new JTextField();
        JTextField idNumberField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField bankAccountField = new JTextField();
        JComboBox<Branch> branchBox = new JComboBox<>(Branch.values());
        JComboBox<Role> roleBox = new JComboBox<>(Role.values());
        JPasswordField passwordField = new JPasswordField();

        JPanel dialogContent = new JPanel(new GridLayout(0, 2, 6, 6));
        dialogContent.add(new JLabel("Employee number:"));
        dialogContent.add(employeeNumberField);
        dialogContent.add(new JLabel("Full name:"));
        dialogContent.add(fullNameField);
        dialogContent.add(new JLabel("Identity number:"));
        dialogContent.add(idNumberField);
        dialogContent.add(new JLabel("Phone:"));
        dialogContent.add(phoneField);
        dialogContent.add(new JLabel("Bank account:"));
        dialogContent.add(bankAccountField);
        dialogContent.add(new JLabel("Branch:"));
        dialogContent.add(branchBox);
        dialogContent.add(new JLabel("Role:"));
        dialogContent.add(roleBox);
        dialogContent.add(new JLabel("Password:"));
        dialogContent.add(passwordField);
        dialogContent.add(new JLabel("Rules:"));
        dialogContent.add(new JLabel("<html>" + policy.describe() + "</html>"));

        int userChoice = JOptionPane.showConfirmDialog(this, dialogContent,
                "Create an employee account", JOptionPane.OK_CANCEL_OPTION);
        if (userChoice != JOptionPane.OK_OPTION) {
            return;
        }

        String employeeNumber = employeeNumberField.getText().trim();
        String fullName = fullNameField.getText().trim();
        if (employeeNumber.isEmpty() || fullName.isEmpty()) {
            showError("An employee number and a full name are required.");
            return;
        }

        String idNumber = idNumberField.getText().trim();
        String phone = phoneField.getText().trim();
        String bankAccount = bankAccountField.getText().trim();
        Branch branch = (Branch) branchBox.getSelectedItem();
        Role role = (Role) roleBox.getSelectedItem();
        String password = new String(passwordField.getPassword());

        runInBackground("add-employee", () -> {
            Employee createdEmployee = employeeController.addEmployee(employeeNumber, fullName,
                    idNumber, phone, bankAccount, branch, role, password);
            SwingUtilities.invokeLater(() -> {
                tableModel.addEmployee(createdEmployee);
                showStatus("Created the account of " + createdEmployee.getFullName()
                        + " (" + createdEmployee.getRole().getDisplayName() + ")");
            });
        });
    }

    /**
     * Fetches the current policy and then shows the policy dialog.
     */
    private void openPasswordPolicyDialog() {
        runInBackground("policy-load", () -> {
            PasswordPolicy policy = employeeController.loadPasswordPolicy();
            SwingUtilities.invokeLater(() -> askForPasswordPolicy(policy));
        });
    }

    /**
     * Shows the policy dialog and sends the new rules.
     *
     * @param currentPolicy the policy currently in force
     */
    private void askForPasswordPolicy(PasswordPolicy currentPolicy) {
        JSpinner minimumLengthSpinner = new JSpinner(new SpinnerNumberModel(
                currentPolicy.getMinimumLength(), SMALLEST_MINIMUM_LENGTH,
                LARGEST_MINIMUM_LENGTH, 1));
        JCheckBox digitBox = new JCheckBox("At least one digit",
                currentPolicy.isDigitRequired());
        JCheckBox upperCaseBox = new JCheckBox("At least one capital letter",
                currentPolicy.isUpperCaseLetterRequired());
        JCheckBox lowerCaseBox = new JCheckBox("At least one small letter",
                currentPolicy.isLowerCaseLetterRequired());
        JCheckBox specialBox = new JCheckBox("At least one special character",
                currentPolicy.isSpecialCharacterRequired());

        JPanel dialogContent = new JPanel(new GridLayout(0, 1, 6, 6));
        JPanel lengthRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lengthRow.add(new JLabel("Minimum length:"));
        lengthRow.add(minimumLengthSpinner);
        dialogContent.add(lengthRow);
        dialogContent.add(digitBox);
        dialogContent.add(upperCaseBox);
        dialogContent.add(lowerCaseBox);
        dialogContent.add(specialBox);
        dialogContent.add(new JLabel("<html><i>Applies to accounts created from now on. "
                + "Existing passwords are already hashed and are not affected.</i></html>"));

        int userChoice = JOptionPane.showConfirmDialog(this, dialogContent,
                "Password policy", JOptionPane.OK_CANCEL_OPTION);
        if (userChoice != JOptionPane.OK_OPTION) {
            return;
        }

        PasswordPolicy newPolicy = new PasswordPolicy(
                (Integer) minimumLengthSpinner.getValue(),
                digitBox.isSelected(), upperCaseBox.isSelected(),
                lowerCaseBox.isSelected(), specialBox.isSelected());

        runInBackground("update-policy", () -> {
            employeeController.updatePasswordPolicy(newPolicy);
            SwingUtilities.invokeLater(() ->
                    showStatus("The password policy is now: " + newPolicy.describe()));
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * Called on the Swing thread by the dispatcher when another manager creates
     * an account.
     * </p>
     */
    @Override
    public void onServerEvent(ServerEvent event) {
        if (event.getEventType() != EventType.EMPLOYEES_UPDATED) {
            return;
        }
        Employee createdEmployee = (Employee) event.getPayload(ProtocolKeys.EMPLOYEE);
        if (createdEmployee == null) {
            return;
        }
        tableModel.addEmployee(createdEmployee);
        showStatus("A new account was created by another manager: "
                + createdEmployee.getFullName());
    }

    /**
     * Writes a line in the status area.
     *
     * @param message the text to show
     */
    private void showStatus(String message) {
        statusLabel.setText(message);
    }
}
