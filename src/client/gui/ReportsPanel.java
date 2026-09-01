package client.gui;

import client.controller.ReportController;
import common.model.ProductCategory;
import common.model.ReportRow;
import common.model.ReportType;
import common.protocol.ServerEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public class ReportsPanel extends ServerBackedPanel {

    private static final long serialVersionUID = 1L;

    private static final int CATEGORY_LIST_ROWS = 4;

    private final transient ReportTableModel tableModel = new ReportTableModel();

    private final JTable reportTable = new JTable(tableModel);

    private final JComboBox<ReportType> reportTypeBox =
            new JComboBox<>(ReportType.values());

    private final JList<ProductCategory> categoryList =
            new JList<>(ProductCategory.values());

    private final JLabel statusLabel = new JLabel(" ");

    private final transient ReportController reportController = new ReportController();

    public ReportsPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        categoryList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        categoryList.setVisibleRowCount(CATEGORY_LIST_ROWS);

        add(createToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(reportTable), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createToolbar() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        controls.add(new JLabel("Report:"));
        controls.add(reportTypeBox);

        controls.add(new JLabel("Categories (none = all):"));
        controls.add(new JScrollPane(categoryList));

        JButton clearFilterButton = new JButton("All categories");
        clearFilterButton.addActionListener(actionEvent -> {
            categoryList.clearSelection();
            showStatus("The filter was cleared: the report will cover every category.");
        });
        controls.add(clearFilterButton);

        JButton buildButton = new JButton("Build report");
        buildButton.addActionListener(actionEvent -> buildReport());
        controls.add(buildButton);

        JButton wordButton = new JButton("Export to Word");
        wordButton.addActionListener(actionEvent -> exportReport("Word"));
        controls.add(wordButton);

        JButton jsonButton = new JButton("Export to JSON");
        jsonButton.addActionListener(actionEvent -> exportReport("JSON"));
        controls.add(jsonButton);

        return controls;
    }

    private void buildReport() {
        ReportType reportType = (ReportType) reportTypeBox.getSelectedItem();
        List<ProductCategory> categoryFilter = getSelectedCategories();

        runInBackground("build-report", () -> {
            List<ReportRow> rows =
                    reportController.buildReport(reportType, null, categoryFilter);
            SwingUtilities.invokeLater(() -> {
                tableModel.setRows(rows, reportType);
                if (rows.isEmpty()) {
                    explainEmptyReport(categoryFilter);
                    return;
                }
                showStatus(rows.size() + " lines in the report " + reportType.getTitle()
                        + describeFilter(categoryFilter));
            });
        });
    }

    private void explainEmptyReport(List<ProductCategory> categoryFilter) {
        if (categoryFilter.isEmpty()) {
            showStatus("The report is empty: no sales have been recorded yet. "
                    + "Sell something in the Inventory tab and build the report again.");
            return;
        }
        showStatus("The report is empty: no sales match the categories you selected ("
                + describeCategories(categoryFilter)
                + "). Press \"All categories\" to see everything.");
    }

    private String describeFilter(List<ProductCategory> categoryFilter) {
        if (categoryFilter.isEmpty()) {
            return " (every category)";
        }
        return " (only " + describeCategories(categoryFilter) + ")";
    }

    private String describeCategories(List<ProductCategory> categoryFilter) {
        StringBuilder names = new StringBuilder();
        for (ProductCategory category : categoryFilter) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(category.getDisplayName());
        }
        return names.toString();
    }

    private void exportReport(String formatName) {
        ReportType reportType = (ReportType) reportTypeBox.getSelectedItem();
        List<ProductCategory> categoryFilter = getSelectedCategories();

        runInBackground("export-report", () -> {

            List<ReportRow> rows =
                    reportController.buildReport(reportType, null, categoryFilter);

            SwingUtilities.invokeLater(() -> tableModel.setRows(rows, reportType));

            if (rows.isEmpty() && !confirmEmptyExport(categoryFilter)) {
                SwingUtilities.invokeLater(() -> explainEmptyReport(categoryFilter));
                return;
            }

            String writtenPath = reportController.exportReport(
                    reportType, null, categoryFilter, formatName);
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        "The report was written on the server machine:"
                                + System.lineSeparator() + writtenPath,
                        "Report exported", JOptionPane.INFORMATION_MESSAGE);
                showStatus("Exported " + rows.size() + " lines to " + writtenPath);
            });
        });
    }

    private boolean confirmEmptyExport(List<ProductCategory> categoryFilter) {
        final boolean[] userAgreed = {false};
        try {
            SwingUtilities.invokeAndWait(() -> {
                int answer = JOptionPane.showConfirmDialog(this,
                        "This report has no lines" + describeFilter(categoryFilter) + "."
                                + System.lineSeparator()
                                + "Export an empty file anyway?",
                        "The report is empty", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                userAgreed[0] = answer == JOptionPane.YES_OPTION;
            });
        } catch (InterruptedException waitInterrupted) {
            Thread.currentThread().interrupt();
        } catch (java.lang.reflect.InvocationTargetException dialogFailure) {
            System.err.println("[Client] could not ask about the empty report: " + dialogFailure);
        }
        return userAgreed[0];
    }

    private List<ProductCategory> getSelectedCategories() {
        return new ArrayList<>(categoryList.getSelectedValuesList());
    }

    @Override
    public void onServerEvent(ServerEvent event) {
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private static final class ReportTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private static final String[] NUMBER_COLUMN_TITLES =
                {"Sales", "Items sold", "Revenue", "Discount given"};

        private final List<ReportRow> rows = new ArrayList<>();

        private ReportType reportType = ReportType.SALES_BY_BRANCH;

        private void setRows(List<ReportRow> newRows, ReportType newReportType) {
            rows.clear();
            rows.addAll(newRows);
            reportType = newReportType;

            fireTableStructureChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return NUMBER_COLUMN_TITLES.length + 1;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return reportType.getGroupColumnTitle();
            }
            return NUMBER_COLUMN_TITLES[columnIndex - 1];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ReportRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return row.getGroupName();
                case 1:
                    return row.getNumberOfSales();
                case 2:
                    return row.getItemsSold();
                case 3:
                    return row.getTotalRevenue();
                case 4:
                    return row.getTotalDiscount();
                default:
                    return "";
            }
        }
    }
}
