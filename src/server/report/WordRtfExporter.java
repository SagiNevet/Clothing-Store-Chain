package server.report;

import common.exception.StorageException;
import common.model.ReportRow;
import common.model.ReportType;
import common.util.TimeUtil;
import server.storage.StoragePaths;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WordRtfExporter implements ReportExporter {

    private static final String FILE_EXTENSION = ".rtf";

    private static final int GROUP_COLUMN_WIDTH = 4000;

    private static final int NUMBER_COLUMN_WIDTH = 1800;

    private static final String[] NUMBER_COLUMN_TITLES =
            {"Sales", "Items", "Revenue", "Discount"};

    @Override
    public String export(ReportType reportType, List<ReportRow> rows) throws StorageException {
        StoragePaths.createDirectoriesIfMissing();
        String fileName = reportType.name().toLowerCase() + "_"
                + TimeUtil.nowForFileName() + FILE_EXTENSION;
        File reportFile = new File(StoragePaths.REPORTS_DIRECTORY, fileName);

        String rtfContent = buildRtfDocument(reportType, rows);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(reportFile), StandardCharsets.US_ASCII))) {
            writer.print(rtfContent);
        } catch (IOException writeFailure) {
            throw new StorageException(reportFile.getPath(),
                    "Failed to write the Word report", writeFailure);
        }

        return reportFile.getAbsolutePath();
    }

    @Override
    public String getFormatName() {
        return "Word";
    }

    private String buildRtfDocument(ReportType reportType, List<ReportRow> rows) {
        StringBuilder document = new StringBuilder();

        document.append("{\\rtf1\\ansi\\deff0");
        document.append("{\\fonttbl{\\f0 Calibri;}}");

        appendTitle(document, reportType);
        appendTable(document, reportType, rows);
        appendTotals(document, rows);

        document.append("}");
        return document.toString();
    }

    private void appendTitle(StringBuilder document, ReportType reportType) {
        document.append("\\fs32\\b ")
                .append(escapeRtf("Clothing Store Chain - " + reportType.getTitle()))
                .append("\\b0\\fs20\\par ");
        document.append(escapeRtf("Generated on " + TimeUtil.nowForDisplay()))
                .append("\\par\\par ");
    }

    private void appendTable(StringBuilder document, ReportType reportType,
                             List<ReportRow> rows) {
        appendHeaderRow(document, reportType.getGroupColumnTitle());
        for (ReportRow row : rows) {
            appendDataRow(document, row);
        }
    }

    private void appendHeaderRow(StringBuilder document, String groupColumnTitle) {
        beginRow(document);
        appendCell(document, groupColumnTitle, true);
        for (String columnTitle : NUMBER_COLUMN_TITLES) {
            appendCell(document, columnTitle, true);
        }
        document.append("\\row ");
    }

    private void appendDataRow(StringBuilder document, ReportRow row) {
        beginRow(document);
        appendCell(document, row.getGroupName(), false);
        appendCell(document, String.valueOf(row.getNumberOfSales()), false);
        appendCell(document, String.valueOf(row.getItemsSold()), false);
        appendCell(document, formatMoney(row.getTotalRevenue()), false);
        appendCell(document, formatMoney(row.getTotalDiscount()), false);
        document.append("\\row ");
    }

    private void appendTotals(StringBuilder document, List<ReportRow> rows) {
        int totalSales = 0;
        int totalItems = 0;
        double totalRevenue = 0.0;
        double totalDiscount = 0.0;
        for (ReportRow row : rows) {
            totalSales += row.getNumberOfSales();
            totalItems += row.getItemsSold();
            totalRevenue += row.getTotalRevenue();
            totalDiscount += row.getTotalDiscount();
        }

        document.append("\\par\\b ")
                .append(escapeRtf("Total: " + totalSales + " sales, " + totalItems + " items, "
                        + formatMoney(totalRevenue) + " revenue, "
                        + formatMoney(totalDiscount) + " given as discounts"))
                .append("\\b0\\par ");
    }

    private void beginRow(StringBuilder document) {
        document.append("\\trowd\\trgaph100");
        int cellEdge = GROUP_COLUMN_WIDTH;
        document.append("\\cellx").append(cellEdge);
        for (int columnIndex = 0; columnIndex < NUMBER_COLUMN_TITLES.length; columnIndex++) {
            cellEdge += NUMBER_COLUMN_WIDTH;
            document.append("\\cellx").append(cellEdge);
        }
        document.append(' ');
    }

    private void appendCell(StringBuilder document, String text, boolean bold) {
        if (bold) {
            document.append("\\b ");
        }
        document.append(escapeRtf(text));
        if (bold) {
            document.append("\\b0");
        }
        document.append("\\cell ");
    }

    private String escapeRtf(String text) {
        StringBuilder escaped = new StringBuilder();
        for (char character : text.toCharArray()) {
            if (character == '\\' || character == '{' || character == '}') {
                escaped.append('\\').append(character);
            } else if (character < 128) {
                escaped.append(character);
            } else {
                
                escaped.append("\\u").append((int) character).append('?');
            }
        }
        return escaped.toString();
    }

    private String formatMoney(double amount) {
        return String.format("%.2f", amount);
    }
}
