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

public class AddProductCommand implements Command {

    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        Branch branch = client.getBranch();
        Product newProduct = (Product) request.getParameter(ProtocolKeys.PRODUCT);
        if (newProduct == null) {
            throw new ChainStoreException("The request did not carry a product to add");
        }

        Product storedProduct = ServerContext.getInstance()
                .getInventoryService()
                .addProduct(branch, newProduct);

        LogManager.getInstance().logSalesAction(client.getEmployeeNumber(), branch,
                "ADD_PRODUCT", "Added " + storedProduct.getName()
                        + " (" + storedProduct.getProductId() + ") with "
                        + storedProduct.getQuantity() + " items in stock");

        EventPublisher.getInstance().publishToBranch(branch,
                new ServerEvent(EventType.INVENTORY_UPDATED)
                        .withPayload(ProtocolKeys.PRODUCT, storedProduct));

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.PRODUCT, storedProduct);
    }
}
