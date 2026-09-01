package server.command.impl;

import common.exception.ChainStoreException;
import common.model.Branch;
import common.model.Product;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import common.protocol.ServerEvent;
import server.command.Command;
import server.core.ConnectedClient;
import server.core.ServerContext;
import server.observer.EventPublisher;
import server.service.LogManager;

public class RestockProductCommand implements Command {

    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        Branch branch = client.getBranch();
        String productId = request.getString(ProtocolKeys.PRODUCT_ID);
        int quantity = request.getInt(ProtocolKeys.QUANTITY);

        Product productAfterDelivery = ServerContext.getInstance()
                .getInventoryService()
                .restock(branch, productId, quantity);

        LogManager.getInstance().logSalesAction(client.getEmployeeNumber(), branch,
                "RESTOCK_PRODUCT", productAfterDelivery.getName() + " received " + quantity
                        + " items, stock is now " + productAfterDelivery.getQuantity());

        EventPublisher.getInstance().publishToBranch(branch,
                new ServerEvent(EventType.INVENTORY_UPDATED)
                        .withPayload(ProtocolKeys.PRODUCT, productAfterDelivery));

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.PRODUCT, productAfterDelivery);
    }
}
