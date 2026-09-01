package client.gui;

import client.controller.CustomerController;
import common.model.Customer;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.ServerEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

public class CustomersPanel extends ServerBackedPanel {

    private static final long serialVersionUID = 1L;

    private final transient CustomerTableModel tableModel = new CustomerTableModel();

    private final JTable customersTable = new JTable(tableModel);

    private final JLabel statusLabel = new JLabel(" ");

    private final transient CustomerController customerController = new CustomerController();

    public CustomersPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        customersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customersTable.setAutoCreateRowSorter(true);

        add(createToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(customersTable), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        refreshCustomers();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(actionEvent -> refreshCustomers());
        toolbar.add(refreshButton);

        JButton addButton = new JButton("Register customer");
        addButton.addActionListener(actionEvent -> openAddCustomerDialog());
        toolbar.add(addButton);

        JButton editButton = new JButton("Edit details");
        editButton.addActionListener(actionEvent -> openEditCustomerDialog());
        toolbar.add(editButton);

        return toolbar;
    }

    private void refreshCustomers() {
        runInBackground("customers-refresh", () -> {
            List<Customer> customers = customerController.loadCustomers();
            SwingUtilities.invokeLater(() -> {
                tableModel.setCustomers(customers);
                showStatus(customers.size() + " customers in the chain");
            });
        });
    }

    private void openAddCustomerDialog() {
        JTextField idNumberField = new JTextField();
        JTextField fullNameField = new JTextField();
        JTextField phoneField = new JTextField();

        JPanel dialogContent = new JPanel(new GridLayout(0, 2, 6, 6));
        dialogContent.add(new JLabel("Identity number:"));
        dialogContent.add(idNumberField);
        dialogContent.add(new JLabel("Full name:"));
        dialogContent.add(fullNameField);
        dialogContent.add(new JLabel("Phone:"));
        dialogContent.add(phoneField);

        int userChoice = JOptionPane.showConfirmDialog(this, dialogContent,
                "Register a new customer", JOptionPane.OK_CANCEL_OPTION);
        if (userChoice != JOptionPane.OK_OPTION) {
            return;
        }

        String idNumber = idNumberField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String phone = phoneField.getText().trim();

        if (idNumber.isEmpty() || fullName.isEmpty()) {
            showError("An identity number and a full name are required.");
            return;
        }

        runInBackground("add-customer", () -> {
            Customer newCustomer = customerController.addCustomer(idNumber, fullName, phone);
            SwingUtilities.invokeLater(() -> showStatus("Registered " + newCustomer.getFullName()
                    + " as a " + newCustomer.getCustomerType().getDisplayName()));
        });
    }

    private void openEditCustomerDialog() {
        Customer selectedCustomer = getSelectedCustomer();
        if (selectedCustomer == null) {
            return;
        }

        JTextField fullNameField = new JTextField(selectedCustomer.getFullName());
        JTextField phoneField = new JTextField(selectedCustomer.getPhone());

        JPanel dialogContent = new JPanel(new GridLayout(0, 2, 6, 6));
        dialogContent.add(new JLabel("Identity number:"));
        dialogContent.add(new JLabel(selectedCustomer.getIdNumber()));
        dialogContent.add(new JLabel("Full name:"));
        dialogContent.add(fullNameField);
        dialogContent.add(new JLabel("Phone:"));
        dialogContent.add(phoneField);

        int userChoice = JOptionPane.showConfirmDialog(this, dialogContent,
                "Edit customer details", JOptionPane.OK_CANCEL_OPTION);
        if (userChoice != JOptionPane.OK_OPTION) {
            return;
        }

        String fullName = fullNameField.getText().trim();
        String phone = phoneField.getText().trim();

        runInBackground("update-customer", () -> {
            Customer updatedCustomer = customerController.updateCustomer(
                    selectedCustomer.getIdNumber(), fullName, phone);
            SwingUtilities.invokeLater(() ->
                    showStatus("Updated the details of " + updatedCustomer.getFullName()));
        });
    }

    private Customer getSelectedCustomer() {
        int selectedRow = customersTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select a customer in the table first.");
            return null;
        }
        int modelRow = customersTable.convertRowIndexToModel(selectedRow);
        return tableModel.getCustomerAt(modelRow);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onServerEvent(ServerEvent event) {
        if (event.getEventType() != EventType.CUSTOMERS_UPDATED) {
            return;
        }
        List<Customer> updatedCustomers =
                (List<Customer>) event.getPayload(ProtocolKeys.CUSTOMER_LIST);
        if (updatedCustomers == null) {
            return;
        }
        tableModel.setCustomers(updatedCustomers);
        showStatus("The customer list was updated: " + updatedCustomers.size() + " customers");
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }
}
