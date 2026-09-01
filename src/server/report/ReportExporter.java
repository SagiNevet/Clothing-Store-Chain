package server.report;

import common.exception.StorageException;
import common.model.ReportRow;
import common.model.ReportType;

import java.util.List;

public interface ReportExporter {

    String export(ReportType reportType, List<ReportRow> rows) throws StorageException;

    String getFormatName();
}
