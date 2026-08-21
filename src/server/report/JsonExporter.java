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
 * Writes a report as a JSON file, built by hand with a {@code StringBuilder}.
 * <p>
 * <b>This exporter is a bonus, not a requirement.</b> The lecturer said in
 * class that JSON is not obligatory - "it can also be in a format other than
 * JSON, that is one of the assumptions I am making for you" - and that the Word
 * export is the one that matters. It is included because it costs one small
 * class and shows that the {@link ReportExporter} interface really does allow a
 * second format without touching anything that already worked.
 * </p>
 * <p>
 * No library is used. JSON is a small enough format to write by hand, as long
 * as the text values are escaped properly, which is what
 * {@link #escapeJson(String)} does.
 * </p>
 */
public class JsonExporter implements ReportExporter {

    /** The extension of a JSON file. */
    private static final String FILE_EXTENSION = ".json";

    /** Two spaces, the indentation of one level. */
    private static final String INDENT = "  ";

    /**
     * {@inheritDoc}
     */
    @Override
    public String export(ReportType reportType, List<ReportRow> rows) throws StorageException {
        StoragePaths.createDirectoriesIfMissing();
        String fileName = reportType.name().toLowerCase() + "_"
                + TimeUtil.nowForFileName() + FILE_EXTENSION;
        File reportFile = new File(StoragePaths.REPORTS_DIRECTORY, fileName);

        String jsonContent = buildJson(reportType, rows);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(reportFile), StandardCharsets.UTF_8))) {
            writer.print(jsonContent);
        } catch (IOException writeFailure) {
            throw new StorageException(reportFile.getPath(),
                    "Failed to write the JSON report", writeFailure);
        }

        return reportFile.getAbsolutePath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFormatName() {
        return "JSON";
    }

    /**
     * Builds the whole JSON document as one string.
     *
     * @param reportType the kind of report
     * @param rows       the lines of the report
     * @return the complete JSON text
     */
    private String buildJson(ReportType reportType, List<ReportRow> rows) {
        StringBuilder json = new StringBuilder();
        json.append("{").append(System.lineSeparator());
        appendTextField(json, "reportType", reportType.getTitle(), true);
        appendTextField(json, "generatedAt", TimeUtil.nowForDisplay(), true);
        json.append(INDENT).append("\"rows\": [").append(System.lineSeparator());

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            appendRow(json, rows.get(rowIndex), rowIndex < rows.size() - 1);
        }

        json.append(INDENT).append("]").append(System.lineSeparator());
        json.append("}").append(System.lineSeparator());
        return json.toString();
    }

    /**
     * Writes one line of the report as a JSON object.
     *
     * @param json         the document being built
     * @param row          the line to write
     * @param moreToFollow whether a comma is needed after this object
     */
    private void appendRow(StringBuilder json, ReportRow row, boolean moreToFollow) {
        json.append(INDENT).append(INDENT).append("{").append(System.lineSeparator());
        json.append(INDENT).append(INDENT).append(INDENT)
                .append("\"group\": \"").append(escapeJson(row.getGroupName())).append("\",")
                .append(System.lineSeparator());
        json.append(INDENT).append(INDENT).append(INDENT)
                .append("\"numberOfSales\": ").append(row.getNumberOfSales()).append(",")
                .append(System.lineSeparator());
        json.append(INDENT).append(INDENT).append(INDENT)
                .append("\"itemsSold\": ").append(row.getItemsSold()).append(",")
                .append(System.lineSeparator());
        json.append(INDENT).append(INDENT).append(INDENT)
                .append("\"totalRevenue\": ").append(row.getTotalRevenue()).append(",")
                .append(System.lineSeparator());
        json.append(INDENT).append(INDENT).append(INDENT)
                .append("\"totalDiscount\": ").append(row.getTotalDiscount())
                .append(System.lineSeparator());
        json.append(INDENT).append(INDENT).append("}")
                .append(moreToFollow ? "," : "").append(System.lineSeparator());
    }

    /**
     * Writes one text field of the top level object.
     *
     * @param json         the document being built
     * @param name         the name of the field
     * @param value        the value of the field
     * @param moreToFollow whether a comma is needed after this field
     */
    private void appendTextField(StringBuilder json, String name, String value,
                                 boolean moreToFollow) {
        json.append(INDENT).append("\"").append(name).append("\": \"")
                .append(escapeJson(value)).append("\"")
                .append(moreToFollow ? "," : "").append(System.lineSeparator());
    }

    /**
     * Makes a piece of text safe to place inside a JSON string.
     * <p>
     * A quotation mark would end the string early and a backslash starts an
     * escape sequence, so both have to be escaped. Without this the file would
     * be broken by the first product name containing a quotation mark - and a
     * broken JSON file is worse than no JSON file at all.
     * </p>
     *
     * @param text the text to escape
     * @return the text, safe to write inside a JSON string
     */
    private String escapeJson(String text) {
        StringBuilder escaped = new StringBuilder();
        for (char character : text.toCharArray()) {
            switch (character) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(character);
                    break;
            }
        }
        return escaped.toString();
    }
}
