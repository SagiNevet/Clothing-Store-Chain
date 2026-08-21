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

/**
 * The inventory screen of one branch: shows the stock and allows selling and
 * restocking.
 * <p>
 * <b>This is the screen that demonstrates the Observer requirement.</b> When an
 * employee of the same branch sells a shirt, the server pushes an
 * {@link EventType#INVENTORY_UPDATED} event, and the quantity in this table
 * changes by itself - no refresh button, no timer. Two clients of the same
 * branch side by side make that visible in one second.
 * </p>
 * <p>
 * The table shows the stock of <b>this branch only</b>, because the server
 * answers according to the branch of the connection and ignores anything the
 * client might claim.
 * </p>
 */
public class InventoryPanel extends ServerBackedPanel {

    /** Serialization version, required because Swing components are serializable. */
    private static final long serialVersionUID = 1L;

    /** The largest quantity that may be sold or received in one action. */
    private static final int MAXIMUM_QUANTITY_PER_ACTION = 999;

    /** The data behind the table. */
    private final transient ProductTableModel tableModel = new ProductTableModel();

    /** The table showing the stock. */
    private final JTable inventoryTable = new JTable(tableModel);

    /** The line at the bottom reporting what happened. */
    private final JLabel statusLabel = new JLabel(" ");

    /** The controller that performs the inventory actions. */
    private final transient InventoryController inventoryController = new InventoryController();

    /** The controller used to fetch the customers for a sale. */
    private final transient CustomerController customerController = new CustomerController();

    /**
     * Builds the inventory screen and loads the stock.
     */
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

    /**
     * Builds the row of buttons above the table.
     *
     * @return the toolbar panel
     */
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

        // Adding a catalogue product is a management decision, so the button is
        // only built for a shift manager. The server refuses it for anybody else
        // in any case - this only avoids showing a button that would be refused.
        if (ClientSession.getInstance().getRole().canManageEmployees()) {
            JButton addProductButton = new JButton("Add product");
            addProductButton.addActionListener(actionEvent -> openAddProductDialog());
            toolbar.add(addProductButton);
        }

        return toolbar;
    }

    /**
     * Loads the stock of this branch from the server.
     */
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

    /**
     * Asks for a quantity and a customer, and performs the sale.
     */
    private void openSellDialog() {
        Product selectedProduct = getSelectedProduct();
        if (selectedProduct == null) {
            return;
        }

        // The customer list is fetched first, because a sale cannot happen
        // without choosing who is buying.
        runInBackground("sell-prepare", () -> {
            List<Customer> customers = customerController.loadCustomers();
            SwingUtilities.invokeLater(() -> askForSaleDetails(selectedProduct, customers));
        });
    }

    /**
     * Shows the sale dialog and sends the sale.
     *
     * @param selectedProduct the product the user selected in the table
     * @param customers       the customers that may buy
     */
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

    /**
     * Reports a completed sale, showing what the purchase plan of the customer
     * was worth.
     *
     * @param sale the sale recorded by the server
     */
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

    /**
     * Asks for a quantity and records a delivery from a supplier.
     */
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

    /**
     * Asks for the details of a new catalogue product and creates it.
     */
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
            // A typing mistake in a text field is not a business failure, so it is
            // handled right here instead of travelling to the server.
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

    /**
     * Returns the product selected in the table, complaining when none is.
     *
     * @return the selected product, or {@code null} when nothing is selected
     */
    private Product getSelectedProduct() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select a product in the table first.");
            return null;
        }
        // The row sorter lets the user sort the table, so the row on screen is
        // not necessarily the row of the model. This conversion is what keeps the
        // right product selected after sorting.
        int modelRow = inventoryTable.convertRowIndexToModel(selectedRow);
        return tableModel.getProductAt(modelRow);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Called on the Swing thread by the dispatcher. An inventory event carries
     * the product that changed, so only that one row is updated - the selection
     * and the scrolling position of the user stay exactly where they were.
     * </p>
     */
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

    /**
     * Writes a line in the status area.
     *
     * @param message the text to show
     */
    private void showStatus(String message) {
        statusLabel.setText(message);
    }
}
