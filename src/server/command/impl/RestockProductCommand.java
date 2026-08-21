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

/**
 * Adds items to the stock of a branch after a delivery from a supplier.
 * <p>
 * This is the "buying" half of the requirement that the inventory screen must
 * allow both buying and selling. It is far simpler than a sale: no customer is
 * involved, no purchase plan applies, and nothing can refuse it except a
 * quantity that makes no sense.
 * </p>
 */
public class RestockProductCommand implements Command {

    /**
     * {@inheritDoc}
     */
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

        // Only the branch that received the delivery is told, because the stock
        // of the other branch did not change.
        EventPublisher.getInstance().publishToBranch(branch,
                new ServerEvent(EventType.INVENTORY_UPDATED)
                        .withPayload(ProtocolKeys.PRODUCT, productAfterDelivery));

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.PRODUCT, productAfterDelivery);
    }
}
