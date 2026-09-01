package server.command.impl;

import common.exception.ChainStoreException;
import common.model.ProductCategory;
import common.model.ReportRow;
import common.model.ReportType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import server.command.Command;
import server.core.ConnectedClient;
import server.report.JsonExporter;
import server.report.ReportExporter;
import server.report.WordRtfExporter;
import server.service.LogManager;
import server.service.ReportService;

import java.util.ArrayList;
import java.util.List;

public final class ReportCommands {

    private static final ReportService REPORT_SERVICE = new ReportService();

    private ReportCommands() {
    }

    public static class BuildReport implements Command {

        @Override
        @SuppressWarnings("unchecked")
        public Response execute(Request request, ConnectedClient client)
                throws ChainStoreException {
            ReportType reportType = readReportType(request);
            List<String> productFilter =
                    (List<String>) request.getParameter(ProtocolKeys.PRODUCT_FILTER);
            List<ProductCategory> categoryFilter =
                    (List<ProductCategory>) request.getParameter(ProtocolKeys.CATEGORY_FILTER);

            List<ReportRow> rows =
                    REPORT_SERVICE.buildReport(reportType, productFilter, categoryFilter);
            if (reportType == ReportType.SALES_BY_BRANCH) {
                
                rows = REPORT_SERVICE.addBranchesWithNoSales(rows);
            }

            return Response.success(request.getRequestId())
                    .withPayload(ProtocolKeys.REPORT_ROWS, new ArrayList<>(rows))
                    .withPayload(ProtocolKeys.REPORT_TYPE, reportType);
        }
    }

    public static class ExportReport implements Command {

        @Override
        @SuppressWarnings("unchecked")
        public Response execute(Request request, ConnectedClient client)
                throws ChainStoreException {
            ReportType reportType = readReportType(request);
            List<String> productFilter =
                    (List<String>) request.getParameter(ProtocolKeys.PRODUCT_FILTER);
            List<ProductCategory> categoryFilter =
                    (List<ProductCategory>) request.getParameter(ProtocolKeys.CATEGORY_FILTER);
            String formatName = request.getString(ProtocolKeys.EXPORT_FORMAT);

            List<ReportRow> rows =
                    REPORT_SERVICE.buildReport(reportType, productFilter, categoryFilter);
            if (reportType == ReportType.SALES_BY_BRANCH) {
                rows = REPORT_SERVICE.addBranchesWithNoSales(rows);
            }

            ReportExporter exporter = exporterFor(formatName);
            String writtenFilePath = exporter.export(reportType, rows);

            LogManager.getInstance().logSalesAction(client.getEmployeeNumber(),
                    client.getBranch(), "EXPORT_REPORT",
                    reportType.getTitle() + " exported to " + exporter.getFormatName()
                            + ": " + writtenFilePath);

            return Response.success(request.getRequestId())
                    .withPayload(ProtocolKeys.FILE_PATH, writtenFilePath);
        }

        private ReportExporter exporterFor(String formatName) {
            if ("JSON".equalsIgnoreCase(formatName)) {
                return new JsonExporter();
            }
            return new WordRtfExporter();
        }
    }

    private static ReportType readReportType(Request request) throws ChainStoreException {
        ReportType reportType = (ReportType) request.getParameter(ProtocolKeys.REPORT_TYPE);
        if (reportType == null) {
            throw new ChainStoreException("The request did not name a kind of report");
        }
        return reportType;
    }
}
