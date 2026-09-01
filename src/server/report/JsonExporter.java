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

public class JsonExporter implements ReportExporter {

    private static final String FILE_EXTENSION = ".json";

    private static final String INDENT = "  ";

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

    @Override
    public String getFormatName() {
        return "JSON";
    }

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

    private void appendTextField(StringBuilder json, String name, String value,
                                 boolean moreToFollow) {
        json.append(INDENT).append("\"").append(name).append("\": \"")
                .append(escapeJson(value)).append("\"")
                .append(moreToFollow ? "," : "").append(System.lineSeparator());
    }

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
