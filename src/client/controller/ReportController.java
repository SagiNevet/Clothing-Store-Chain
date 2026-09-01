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

public class ReportController {

    @SuppressWarnings("unchecked")
    public List<ReportRow> buildReport(ReportType reportType, List<String> productFilter,
                                       List<ProductCategory> categoryFilter)
            throws ChainStoreException {
        Response response = sendAndVerify(reportRequest(
                actionFor(reportType), reportType, productFilter, categoryFilter));
        return (List<ReportRow>) response.getPayload(ProtocolKeys.REPORT_ROWS);
    }

    public String exportReport(ReportType reportType, List<String> productFilter,
                               List<ProductCategory> categoryFilter, String formatName)
            throws ChainStoreException {
        Request request = reportRequest(ActionType.EXPORT_REPORT, reportType,
                productFilter, categoryFilter)
                .withParameter(ProtocolKeys.EXPORT_FORMAT, formatName);
        Response response = sendAndVerify(request);
        return (String) response.getPayload(ProtocolKeys.FILE_PATH);
    }

    private ActionType actionFor(ReportType reportType) {
        if (reportType == ReportType.SALES_BY_BRANCH) {
            return ActionType.SALES_BY_BRANCH_REPORT;
        }
        return ActionType.SALES_BY_PRODUCT_REPORT;
    }

    private Request reportRequest(ActionType actionType, ReportType reportType,
                                  List<String> productFilter,
                                  List<ProductCategory> categoryFilter) {
        ClientSession session = ClientSession.getInstance();
        Request request = new Request(actionType,
                session.getCurrentEmployee().getEmployeeNumber(), session.getBranch())
                .withParameter(ProtocolKeys.REPORT_TYPE, reportType);

        if (productFilter != null && !productFilter.isEmpty()) {
            request.withParameter(ProtocolKeys.PRODUCT_FILTER, new ArrayList<>(productFilter));
        }
        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            request.withParameter(ProtocolKeys.CATEGORY_FILTER, new ArrayList<>(categoryFilter));
        }
        return request;
    }

    private Response sendAndVerify(Request request) throws ChainStoreException {
        Response response = ClientSession.getInstance().getConnection().send(request);
        if (!response.isSuccess()) {
            throw new ChainStoreException(response.getMessage());
        }
        return response;
    }
}
