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

/**
 * Writes a report as a Word document, using nothing but a {@code StringBuilder}
 * and a {@code PrintWriter}.
 *
 * <h2>Why RTF, and why this is allowed</h2>
 * <p>
 * The project forbids every external library, so Apache POI and everything like
 * it is out of the question. RTF - Rich Text Format - solves that completely:
 * it is a format Microsoft designed, Word opens it natively, and a whole
 * document is <b>plain text</b> made of tags such as {@code \b} for bold and
 * {@code \par} for a new paragraph. Writing one is exactly the file writing that
 * was taught in class.
 * </p>
 *
 * <h2>Why not HTML saved as .doc</h2>
 * <p>
 * That is the other trick people use, and it works - but Word greets it with
 * the warning "the file is in a different format than the extension says",
 * which is the last thing anybody wants on the screen during a defence. Word
 * recognises RTF by its content and opens it without a word.
 * </p>
 *
 * <h2>What the RTF actually says</h2>
 * <pre>
 * {\rtf1\ansi        the header: this is RTF version 1
 * \b Title\b0        bold on, text, bold off
 * \par               end of paragraph
 * \trowd \cellx2000  begin a table row, first cell ends at 2000 twips
 * text\cell          the content of a cell
 * \row               end of the row
 * }                  end of the document
 * </pre>
 * <p>
 * A twip is one twentieth of a point, which is the unit RTF measures in.
 * </p>
 */
public class WordRtfExporter implements ReportExporter {

    /** The extension Word opens without asking any questions. */
    private static final String FILE_EXTENSION = ".rtf";

    /** The width of the first column, in twips. */
    private static final int GROUP_COLUMN_WIDTH = 4000;

    /** The width of each of the number columns, in twips. */
    private static final int NUMBER_COLUMN_WIDTH = 1800;

    /** The headings of the four number columns. */
    private static final String[] NUMBER_COLUMN_TITLES =
            {"Sales", "Items", "Revenue", "Discount"};

    /**
     * {@inheritDoc}
     */
    @Override
    public String export(ReportType reportType, List<ReportRow> rows) throws StorageException {
        StoragePaths.createDirectoriesIfMissing();
        String fileName = reportType.name().toLowerCase() + "_"
                + TimeUtil.nowForFileName() + FILE_EXTENSION;
        File reportFile = new File(StoragePaths.REPORTS_DIRECTORY, fileName);

        String rtfContent = buildRtfDocument(reportType, rows);

        // try-with-resources closes the writer whether the block ended normally
        // or with an exception, which is what a finally block would do by hand.
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(reportFile), StandardCharsets.US_ASCII))) {
            writer.print(rtfContent);
        } catch (IOException writeFailure) {
            throw new StorageException(reportFile.getPath(),
                    "Failed to write the Word report", writeFailure);
        }

        return reportFile.getAbsolutePath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFormatName() {
        return "Word";
    }

    /**
     * Builds the whole RTF document as one string.
     *
     * @param reportType the kind of report
     * @param rows       the lines of the report
     * @return the complete RTF text
     */
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

    /**
     * Writes the title and the date at the top of the document.
     *
     * @param document   the document being built
     * @param reportType the kind of report
     */
    private void appendTitle(StringBuilder document, ReportType reportType) {
        document.append("\\fs32\\b ")
                .append(escapeRtf("Clothing Store Chain - " + reportType.getTitle()))
                .append("\\b0\\fs20\\par ");
        document.append(escapeRtf("Generated on " + TimeUtil.nowForDisplay()))
                .append("\\par\\par ");
    }

    /**
     * Writes the table of the report.
     *
     * @param document   the document being built
     * @param reportType the kind of report, which names the first column
     * @param rows       the lines of the report
     */
    private void appendTable(StringBuilder document, ReportType reportType,
                             List<ReportRow> rows) {
        appendHeaderRow(document, reportType.getGroupColumnTitle());
        for (ReportRow row : rows) {
            appendDataRow(document, row);
        }
    }

    /**
     * Writes the heading row of the table, in bold.
     *
     * @param document         the document being built
     * @param groupColumnTitle the heading of the first column
     */
    private void appendHeaderRow(StringBuilder document, String groupColumnTitle) {
        beginRow(document);
        appendCell(document, groupColumnTitle, true);
        for (String columnTitle : NUMBER_COLUMN_TITLES) {
            appendCell(document, columnTitle, true);
        }
        document.append("\\row ");
    }

    /**
     * Writes one line of the report as a table row.
     *
     * @param document the document being built
     * @param row      the line to write
     */
    private void appendDataRow(StringBuilder document, ReportRow row) {
        beginRow(document);
        appendCell(document, row.getGroupName(), false);
        appendCell(document, String.valueOf(row.getNumberOfSales()), false);
        appendCell(document, String.valueOf(row.getItemsSold()), false);
        appendCell(document, formatMoney(row.getTotalRevenue()), false);
        appendCell(document, formatMoney(row.getTotalDiscount()), false);
        document.append("\\row ");
    }

    /**
     * Writes the summary line under the table.
     *
     * @param document the document being built
     * @param rows     the lines of the report
     */
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

    /**
     * Opens a table row and declares where each cell ends.
     * <p>
     * RTF has no concept of a column: a row simply states the position at which
     * every cell finishes, and the cells are written afterwards in the same
     * order. The positions must therefore grow from left to right.
     * </p>
     *
     * @param document the document being built
     */
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

    /**
     * Writes the content of one cell.
     *
     * @param document the document being built
     * @param text     the text of the cell
     * @param bold     whether the text should be bold
     */
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

    /**
     * Makes a piece of text safe to place inside an RTF document.
     * <p>
     * Three characters have a meaning of their own in RTF and must be escaped:
     * the backslash that starts every tag, and the two braces that group
     * things. Any character outside plain ASCII is written as
     * {@code \\uNNNN?}, which is how RTF carries a Unicode character - and that
     * is what lets a Hebrew product name reach Word correctly.
     * </p>
     *
     * @param text the text to escape
     * @return the text, safe to write into the document
     */
    private String escapeRtf(String text) {
        StringBuilder escaped = new StringBuilder();
        for (char character : text.toCharArray()) {
            if (character == '\\' || character == '{' || character == '}') {
                escaped.append('\\').append(character);
            } else if (character < 128) {
                escaped.append(character);
            } else {
                // The question mark is the character an old reader that does not
                // understand the tag should show instead.
                escaped.append("\\u").append((int) character).append('?');
            }
        }
        return escaped.toString();
    }

    /**
     * Formats an amount of money with exactly two decimal places.
     *
     * @param amount the amount to format
     * @return the amount as text
     */
    private String formatMoney(double amount) {
        return String.format("%.2f", amount);
    }
}
