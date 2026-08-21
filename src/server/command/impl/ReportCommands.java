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

/**
 * The commands that build and export the sales reports.
 * <p>
 * Every one of them is restricted to a shift manager by the flags on
 * {@code ActionType}, because the reports show the takings of the whole chain.
 * </p>
 */
public final class ReportCommands {

    /** The service that builds the reports, shared by the commands below. */
    private static final ReportService REPORT_SERVICE = new ReportService();

    /**
     * Prevents instantiation. This class only groups the command classes.
     */
    private ReportCommands() {
    }

    /**
     * Builds a report and sends its rows to the client, without writing a file.
     * <p>
     * The same command serves all three kinds of report, because the kind
     * travels as a parameter. That is what makes adding a fourth report a
     * matter of adding one enum constant.
     * </p>
     */
    public static class BuildReport implements Command {

        /**
         * {@inheritDoc}
         */
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
                // A branch that sold nothing must still appear, as a row of zeros,
                // so that an empty row is never mistaken for a broken report.
                rows = REPORT_SERVICE.addBranchesWithNoSales(rows);
            }

            return Response.success(request.getRequestId())
                    .withPayload(ProtocolKeys.REPORT_ROWS, new ArrayList<>(rows))
                    .withPayload(ProtocolKeys.REPORT_TYPE, reportType);
        }
    }

    /**
     * Builds a report and writes it to a file on the machine of the server.
     * <p>
     * The file is written by the <b>server</b> and its path is sent back as
     * text, because the server is the only side that owns a reports folder. In
     * the demonstration both run on the same laptop, so the manager finds the
     * file in {@code reports} next to the program.
     * </p>
     */
    public static class ExportReport implements Command {

        /**
         * {@inheritDoc}
         */
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

        /**
         * Chooses the exporter for a format name.
         * <p>
         * This is the small factory of the report package: the caller asks for
         * a format by name and never mentions a concrete exporter class.
         * </p>
         *
         * @param formatName the format the client asked for
         * @return the matching exporter, Word by default
         */
        private ReportExporter exporterFor(String formatName) {
            if ("JSON".equalsIgnoreCase(formatName)) {
                return new JsonExporter();
            }
            return new WordRtfExporter();
        }
    }

    /**
     * Reads the kind of report out of a request.
     *
     * @param request the request that arrived
     * @return the requested kind of report
     * @throws ChainStoreException if the request did not name one
     */
    private static ReportType readReportType(Request request) throws ChainStoreException {
        ReportType reportType = (ReportType) request.getParameter(ProtocolKeys.REPORT_TYPE);
        if (reportType == null) {
            throw new ChainStoreException("The request did not name a kind of report");
        }
        return reportType;
    }
}
