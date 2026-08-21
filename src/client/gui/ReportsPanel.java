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

/**
 * The reports screen, available to a shift manager only.
 * <p>
 * It builds the two reports the requirements ask for - sales per branch, and
 * sales per product or category - and exports them to a Word file with no
 * external library at all.
 * </p>
 * <p>
 * This screen is not a {@link ServerEvent} listener in any meaningful way: a
 * report is a picture of the past, and there is nothing to push to it. It still
 * extends {@link ServerBackedPanel} because it needs the background thread
 * helper and the single place that shows failures.
 * </p>
 */
public class ReportsPanel extends ServerBackedPanel {

    /** Serialization version, required because Swing components are serializable. */
    private static final long serialVersionUID = 1L;

    /** The number of rows the category list is sized for. */
    private static final int CATEGORY_LIST_ROWS = 4;

    /** The data behind the report table. */
    private final transient ReportTableModel tableModel = new ReportTableModel();

    /** The table showing the report. */
    private final JTable reportTable = new JTable(tableModel);

    /** The chooser of the kind of report. */
    private final JComboBox<ReportType> reportTypeBox =
            new JComboBox<>(ReportType.values());

    /** The chooser of the categories to include. */
    private final JList<ProductCategory> categoryList =
            new JList<>(ProductCategory.values());

    /** The line at the bottom reporting what happened. */
    private final JLabel statusLabel = new JLabel(" ");

    /** The controller that builds and exports the reports. */
    private final transient ReportController reportController = new ReportController();

    /**
     * Builds the reports screen.
     */
    public ReportsPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        categoryList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        categoryList.setVisibleRowCount(CATEGORY_LIST_ROWS);

        add(createToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(reportTable), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * Builds the controls above the table.
     *
     * @return the toolbar panel
     */
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

    /**
     * Builds the chosen report and shows it in the table.
     */
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

    /**
     * Tells the user why a report came out empty.
     * <p>
     * An empty table on its own is the worst possible answer: it looks exactly
     * like a broken screen. There are only two reasons a report can be empty,
     * and naming the right one turns a puzzle into an instruction.
     * </p>
     *
     * @param categoryFilter the categories the user had selected
     */
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

    /**
     * Describes the filter for the status line, or nothing when there is none.
     *
     * @param categoryFilter the categories the user selected
     * @return a sentence naming the filter, or an empty text
     */
    private String describeFilter(List<ProductCategory> categoryFilter) {
        if (categoryFilter.isEmpty()) {
            return " (every category)";
        }
        return " (only " + describeCategories(categoryFilter) + ")";
    }

    /**
     * Joins the names of the selected categories.
     *
     * @param categoryFilter the categories the user selected
     * @return the names, separated by commas
     */
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

    /**
     * Exports the chosen report to a file on the machine of the server.
     *
     * @param formatName {@code Word} or {@code JSON}
     */
    private void exportReport(String formatName) {
        ReportType reportType = (ReportType) reportTypeBox.getSelectedItem();
        List<ProductCategory> categoryFilter = getSelectedCategories();

        runInBackground("export-report", () -> {
            // The report is built first and shown in the table, so the user always
            // sees exactly what is about to be written. Exporting without looking
            // was how an empty file got written by mistake: the sales had simply
            // not been made yet, and nothing on the screen said so.
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

    /**
     * Asks whether an empty report should really be written to a file.
     * <p>
     * An empty report is sometimes exactly what a manager wants - proof that a
     * category sold nothing. So it is not refused, only confirmed.
     * </p>
     * <p>
     * The dialog is shown with {@code invokeAndWait} because this method is
     * called from a background thread and has to come back with the answer of
     * the user before the export continues.
     * </p>
     *
     * @param categoryFilter the categories the user had selected
     * @return {@code true} if the user wants the empty file anyway
     */
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

    /**
     * Returns the categories the user selected, or an empty list for all of them.
     *
     * @return the selected categories
     */
    private List<ProductCategory> getSelectedCategories() {
        return new ArrayList<>(categoryList.getSelectedValuesList());
    }

    /**
     * {@inheritDoc}
     * <p>
     * A report describes what already happened, so no pushed event changes it.
     * </p>
     */
    @Override
    public void onServerEvent(ServerEvent event) {
        // Nothing to do: a report is built when the user asks for it.
    }

    /**
     * Writes a line in the status area.
     *
     * @param message the text to show
     */
    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * The data behind the report table.
     * <p>
     * The heading of the first column changes with the kind of report - Branch,
     * Product or Category - which is why the model keeps the report kind
     * alongside the rows.
     * </p>
     */
    private static final class ReportTableModel extends AbstractTableModel {

        /** Serialization version, required because Swing models are serializable. */
        private static final long serialVersionUID = 1L;

        /** The headings of the four number columns. */
        private static final String[] NUMBER_COLUMN_TITLES =
                {"Sales", "Items sold", "Revenue", "Discount given"};

        /** The lines currently displayed. */
        private final List<ReportRow> rows = new ArrayList<>();

        /** The kind of report displayed, which names the first column. */
        private ReportType reportType = ReportType.SALES_BY_BRANCH;

        /**
         * Replaces every line of the table.
         *
         * @param newRows       the lines to display
         * @param newReportType the kind of report they came from
         */
        private void setRows(List<ReportRow> newRows, ReportType newReportType) {
            rows.clear();
            rows.addAll(newRows);
            reportType = newReportType;
            // The heading of the first column changes as well, so the whole
            // structure of the table is announced and not only its contents.
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
