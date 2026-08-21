package server.report;

import common.exception.StorageException;
import common.model.ReportRow;
import common.model.ReportType;

import java.util.List;

/**
 * Writes a report to a file in some format.
 * <p>
 * The interface exists so that the code that builds a report never knows which
 * format it will end up in. The Word export is required by the project; the
 * JSON export is a small extra, and it was written by adding one class rather
 * than by touching anything that already worked.
 * </p>
 */
public interface ReportExporter {

    /**
     * Writes a report to a file.
     *
     * @param reportType the kind of report, used for the title and the heading
     * @param rows       the lines of the report
     * @return the full path of the file that was written
     * @throws StorageException if the file cannot be written
     */
    String export(ReportType reportType, List<ReportRow> rows) throws StorageException;

    /**
     * Returns the name of the format, for the message shown to the user.
     *
     * @return the format name, for example {@code Word}
     */
    String getFormatName();
}
