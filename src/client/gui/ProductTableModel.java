package client.gui;

import common.model.Product;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * The data behind the inventory table.
 * <p>
 * Swing separates the data from the drawing: a {@code JTable} knows how to
 * paint a grid and react to the mouse, but it holds no data of its own. It asks
 * a model how many rows there are and what sits in each cell. This class is
 * that model.
 * </p>
 * <p>
 * <b>Why this matters for the live updates:</b> when a sale arrives from another
 * employee, nothing repaints the table by hand. The model is updated and then
 * announces the change with {@code fireTableRowsUpdated}, and Swing repaints
 * exactly the row that changed. That announcement is the reason a table can
 * follow a server push without any timer or polling.
 * </p>
 */
public class ProductTableModel extends AbstractTableModel {

    /** Serialization version, required because Swing models are serializable. */
    private static final long serialVersionUID = 1L;

    /** The titles of the columns, in the order they are displayed. */
    private static final String[] COLUMN_TITLES =
            {"Product id", "Name", "Category", "Price", "In stock"};

    /** The position of the product identifier column. */
    private static final int COLUMN_PRODUCT_ID = 0;

    /** The position of the name column. */
    private static final int COLUMN_NAME = 1;

    /** The position of the category column. */
    private static final int COLUMN_CATEGORY = 2;

    /** The position of the price column. */
    private static final int COLUMN_PRICE = 3;

    /** The position of the quantity column. */
    private static final int COLUMN_QUANTITY = 4;

    /** The products currently displayed, one per row. */
    private final List<Product> products = new ArrayList<>();

    /**
     * Replaces every row with a freshly loaded stock.
     *
     * @param newProducts the products to display
     */
    public void setProducts(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);
        // The whole table changed, so every row is redrawn.
        fireTableDataChanged();
    }

    /**
     * Updates the row of one product, or adds a row when the product is new.
     * <p>
     * This is what an {@code INVENTORY_UPDATED} event calls. Only the row that
     * really changed is repainted, so the selection and the scrolling position
     * of the user are not disturbed by somebody else selling a shirt.
     * </p>
     *
     * @param changedProduct the product as it looks now
     */
    public void updateProduct(Product changedProduct) {
        int rowIndex = products.indexOf(changedProduct);
        if (rowIndex < 0) {
            products.add(changedProduct);
            fireTableRowsInserted(products.size() - 1, products.size() - 1);
            return;
        }
        products.set(rowIndex, changedProduct);
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    /**
     * Returns the product displayed in one row.
     *
     * @param rowIndex the row the user selected
     * @return the product of that row, or {@code null} when the index is invalid
     */
    public Product getProductAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= products.size()) {
            return null;
        }
        return products.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return products.size();
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
        Product product = products.get(rowIndex);
        switch (columnIndex) {
            case COLUMN_PRODUCT_ID:
                return product.getProductId();
            case COLUMN_NAME:
                return product.getName();
            case COLUMN_CATEGORY:
                return product.getCategory().getDisplayName();
            case COLUMN_PRICE:
                return product.getPrice();
            case COLUMN_QUANTITY:
                return product.getQuantity();
            default:
                return "";
        }
    }
}
