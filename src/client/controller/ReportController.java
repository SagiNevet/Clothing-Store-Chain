package client.controller;

import common.exception.ChainStoreException;
import common.model.ProductCategory;
import common.model.ReportRow;
import common.model.ReportType;
import common.protocol.ActionType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns what the reports screen wants into requests, and the answers back into
 * model objects.
 * <p>
 * <b>Threading:</b> both methods block while they wait for the server, so they
 * must be called from a background thread.
 * </p>
 */
public class ReportController {

    /**
     * Builds a report and returns its rows, without writing any file.
     *
     * @param reportType     the kind of report to build
     * @param productFilter  the products to include, or {@code null} for all
     * @param categoryFilter the categories to include, or {@code null} for all
     * @return the lines of the report
     * @throws ChainStoreException if the role is not allowed or the server
     *                             cannot be reached
     */
    @SuppressWarnings("unchecked")
    public List<ReportRow> buildReport(ReportType reportType, List<String> productFilter,
                                       List<ProductCategory> categoryFilter)
            throws ChainStoreException {
        Response response = sendAndVerify(reportRequest(
                actionFor(reportType), reportType, productFilter, categoryFilter));
        return (List<ReportRow>) response.getPayload(ProtocolKeys.REPORT_ROWS);
    }

    /**
     * Builds a report and asks the server to write it to a file.
     *
     * @param reportType     the kind of report to export
     * @param productFilter  the products to include, or {@code null} for all
     * @param categoryFilter the categories to include, or {@code null} for all
     * @param formatName     {@code Word} or {@code JSON}
     * @return the full path of the file the server wrote
     * @throws ChainStoreException if the role is not allowed, the file cannot be
     *                             written, or the server cannot be reached
     */
    public String exportReport(ReportType reportType, List<String> productFilter,
                               List<ProductCategory> categoryFilter, String formatName)
            throws ChainStoreException {
        Request request = reportRequest(ActionType.EXPORT_REPORT, reportType,
                productFilter, categoryFilter)
                .withParameter(ProtocolKeys.EXPORT_FORMAT, formatName);
        Response response = sendAndVerify(request);
        return (String) response.getPayload(ProtocolKeys.FILE_PATH);
    }

    /**
     * Chooses the action that builds a kind of report.
     * <p>
     * Two of the three kinds have an action of their own in the protocol,
     * because the requirements name them separately. The third reuses the
     * product action, since the server tells them apart by the report kind that
     * travels inside the request.
     * </p>
     *
     * @param reportType the kind of report
     * @return the action to send
     */
    private ActionType actionFor(ReportType reportType) {
        if (reportType == ReportType.SALES_BY_BRANCH) {
            return ActionType.SALES_BY_BRANCH_REPORT;
        }
        return ActionType.SALES_BY_PRODUCT_REPORT;
    }

    /**
     * Builds a request carrying the kind of report and the filters.
     *
     * @param actionType     the action to send
     * @param reportType     the kind of report
     * @param productFilter  the products to include, or {@code null} for all
     * @param categoryFilter the categories to include, or {@code null} for all
     * @return the request, ready to send
     */
    private Request reportRequest(ActionType actionType, ReportType reportType,
                                  List<String> productFilter,
                                  List<ProductCategory> categoryFilter) {
        ClientSession session = ClientSession.getInstance();
        Request request = new Request(actionType,
                session.getCurrentEmployee().getEmployeeNumber(), session.getBranch())
                .withParameter(ProtocolKeys.REPORT_TYPE, reportType);

        // The lists are copied into ArrayList because whatever the screen holds
        // must be serializable, and a list produced by a stream is not.
        if (productFilter != null && !productFilter.isEmpty()) {
            request.withParameter(ProtocolKeys.PRODUCT_FILTER, new ArrayList<>(productFilter));
        }
        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            request.withParameter(ProtocolKeys.CATEGORY_FILTER, new ArrayList<>(categoryFilter));
        }
        return request;
    }

    /**
     * Sends a request and turns a refusal into an exception.
     *
     * @param request the request to send
     * @return the successful answer
     * @throws ChainStoreException if the server refused the request or could not
     *                             be reached
     */
    private Response sendAndVerify(Request request) throws ChainStoreException {
        Response response = ClientSession.getInstance().getConnection().send(request);
        if (!response.isSuccess()) {
            throw new ChainStoreException(response.getMessage());
        }
        return response;
    }
}
