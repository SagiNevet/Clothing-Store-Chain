package client.gui;

import common.model.Product;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ProductTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMN_TITLES =
            {"Product id", "Name", "Category", "Price", "In stock"};

    private static final int COLUMN_PRODUCT_ID = 0;

    private static final int COLUMN_NAME = 1;

    private static final int COLUMN_CATEGORY = 2;

    private static final int COLUMN_PRICE = 3;

    private static final int COLUMN_QUANTITY = 4;

    private final List<Product> products = new ArrayList<>();

    public void setProducts(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);
        fireTableDataChanged();
    }

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
