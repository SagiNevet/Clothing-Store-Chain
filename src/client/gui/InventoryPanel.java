package client.gui;

import client.controller.ClientSession;
import client.controller.CustomerController;
import client.controller.InventoryController;
import common.model.Customer;
import common.model.Product;
import common.model.ProductCategory;
import common.model.Sale;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.ServerEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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

public class InventoryPanel extends ServerBackedPanel {

    private static final long serialVersionUID = 1L;

    private static final int MAXIMUM_QUANTITY_PER_ACTION = 999;

    private final transient ProductTableModel tableModel = new ProductTableModel();

    private final JTable inventoryTable = new JTable(tableModel);

    private final JLabel statusLabel = new JLabel(" ");

    private final transient InventoryController inventoryController = new InventoryController();

    private final transient CustomerController customerController = new CustomerController();

    public InventoryPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventoryTable.setAutoCreateRowSorter(true);

        add(createToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        refreshInventory();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(actionEvent -> refreshInventory());
        toolbar.add(refreshButton);

        JButton sellButton = new JButton("Sell to customer");
        sellButton.addActionListener(actionEvent -> openSellDialog());
        toolbar.add(sellButton);

        JButton restockButton = new JButton("Receive delivery");
        restockButton.addActionListener(actionEvent -> openRestockDialog());
        toolbar.add(restockButton);

        if (ClientSession.getInstance().getRole().canManageEmployees()) {
            JButton addProductButton = new JButton("Add product");
            addProductButton.addActionListener(actionEvent -> openAddProductDialog());
            toolbar.add(addProductButton);
        }

        return toolbar;
    }

    private void refreshInventory() {
        runInBackground("inventory-refresh", () -> {
            List<Product> stock = inventoryController.loadInventory();
            SwingUtilities.invokeLater(() -> {
                tableModel.setProducts(stock);
                showStatus(stock.size() + " products in "
                        + ClientSession.getInstance().getBranch().getDisplayName());
            });
        });
    }

    private void openSellDialog() {
        Product selectedProduct = getSelectedProduct();
        if (selectedProduct == null) {
            return;
        }

        runInBackground("sell-prepare", () -> {
            List<Customer> customers = customerController.loadCustomers();
            SwingUtilities.invokeLater(() -> askForSaleDetails(selectedProduct, customers));
        });
    }

    private void askForSaleDetails(Product selectedProduct, List<Customer> customers) {
        if (customers.isEmpty()) {
            showError("There are no customers yet. Register a customer first, "
                    + "in the Customers tab.");
            return;
        }

        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(
                1, 1, MAXIMUM_QUANTITY_PER_ACTION, 1));
        JComboBox<Customer> customerBox = new JComboBox<>(customers.toArray(new Customer[0]));

        JPanel dialogContent = new JPanel(new GridLayout(0, 2, 6, 6));
        dialogContent.add(new JLabel("Product:"));
        dialogContent.add(new JLabel(selectedProduct.getName()
                + "  (" + selectedProduct.getQuantity() + " in stock)"));
        dialogContent.add(new JLabel("Quantity:"));
        dialogContent.add(quantitySpinner);
        dialogContent.add(new JLabel("Customer:"));
        dialogContent.add(customerBox);

        int userChoice = JOptionPane.showConfirmDialog(this, dialogContent,
                "Sell " + selectedProduct.getName(), JOptionPane.OK_CANCEL_OPTION);
        if (userChoice != JOptionPane.OK_OPTION) {
            return;
        }

        int quantity = (Integer) quantitySpinner.getValue();
        Customer chosenCustomer = (Customer) customerBox.getSelectedItem();

        runInBackground("sell-product", () -> {
            Sale sale = inventoryController.sellProduct(selectedProduct.getProductId(),
                    quantity, chosenCustomer.getIdNumber());
            SwingUtilities.invokeLater(() -> showSaleResult(sale));
        });
    }

    private void showSaleResult(Sale sale) {
        showInfo("Sold " + sale.getQuantity() + " x " + sale.getProductName()
                + System.lineSeparator()
                + "Price before discount: " + sale.getTotalBeforeDiscount()
                + System.lineSeparator()
                + "Paid: " + sale.getFinalPrice()
                + System.lineSeparator()
                + "Saved by the " + sale.getCustomerTypeAtSale().getDisplayName()
                + " plan: " + sale.getDiscountAmount());
        showStatus("Sale " + sale.getSaleId() + " completed");
    }

    private void openRestockDialog() {
        Product selectedProduct = getSelectedProduct();
        if (selectedProduct == null) {
            return;
        }

        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(
                1, 1, MAXIMUM_QUANTITY_PER_ACTION, 1));
        int userChoice = JOptionPane.showConfirmDialog(this, quantitySpinner,
                "How many items of " + selectedProduct.getName() + " arrived?",
                JOptionPane.OK_CANCEL_OPTION);
        if (userChoice != JOptionPane.OK_OPTION) {
            return;
        }

        int quantity = (Integer) quantitySpinner.getValue();
        runInBackground("restock-product", () -> {
            Product updatedProduct = inventoryController.restockProduct(
                    selectedProduct.getProductId(), quantity);
            SwingUtilities.invokeLater(() -> showStatus(updatedProduct.getName()
                    + " now has " + updatedProduct.getQuantity() + " items in stock"));
        });
    }

    private void openAddProductDialog() {
        JTextField productIdField = new JTextField();
        JTextField nameField = new JTextField();
        JComboBox<ProductCategory> categoryBox =
                new JComboBox<>(ProductCategory.values());
        JTextField priceField = new JTextField();
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(
                0, 0, MAXIMUM_QUANTITY_PER_ACTION, 1));

        JPanel dialogContent = new JPanel(new GridLayout(0, 2, 6, 6));
        dialogContent.add(new JLabel("Product id:"));
        dialogContent.add(productIdField);
        dialogContent.add(new JLabel("Name:"));
        dialogContent.add(nameField);
        dialogContent.add(new JLabel("Category:"));
        dialogContent.add(categoryBox);
        dialogContent.add(new JLabel("Price:"));
        dialogContent.add(priceField);
        dialogContent.add(new JLabel("Starting stock:"));
        dialogContent.add(quantitySpinner);

        int userChoice = JOptionPane.showConfirmDialog(this, dialogContent,
                "Add a product to the catalogue", JOptionPane.OK_CANCEL_OPTION);
        if (userChoice != JOptionPane.OK_OPTION) {
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException invalidPrice) {
            
            showError("The price must be a number, for example 89.90");
            return;
        }

        String productId = productIdField.getText().trim();
        String name = nameField.getText().trim();
        ProductCategory category = (ProductCategory) categoryBox.getSelectedItem();
        int startingStock = (Integer) quantitySpinner.getValue();

        runInBackground("add-product", () -> {
            Product storedProduct = inventoryController.addProduct(
                    productId, name, category, price, startingStock);
            SwingUtilities.invokeLater(() ->
                    showStatus("Added " + storedProduct.getName() + " to the catalogue"));
        });
    }

    private Product getSelectedProduct() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select a product in the table first.");
            return null;
        }

        int modelRow = inventoryTable.convertRowIndexToModel(selectedRow);
        return tableModel.getProductAt(modelRow);
    }

    @Override
    public void onServerEvent(ServerEvent event) {
        if (event.getEventType() != EventType.INVENTORY_UPDATED) {
            return;
        }
        Product changedProduct = (Product) event.getPayload(ProtocolKeys.PRODUCT);
        if (changedProduct == null) {
            return;
        }
        tableModel.updateProduct(changedProduct);
        showStatus(changedProduct.getName() + " was updated by another employee: "
                + changedProduct.getQuantity() + " in stock");
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }
}
