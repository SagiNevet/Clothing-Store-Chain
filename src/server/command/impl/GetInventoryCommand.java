package server.command.impl;

import common.exception.ChainStoreException;
import common.model.Product;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import server.command.Command;
import server.core.ConnectedClient;
import server.core.ServerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Returns the stock of the branch the requesting employee works in.
 * <p>
 * The branch is taken from the connection and never from the request. An
 * employee of Jerusalem cannot ask for the stock of Tel Aviv by editing the
 * message, because the server simply ignores any branch the client might send.
 * </p>
 */
public class GetInventoryCommand implements Command {

    /**
     * {@inheritDoc}
     */
    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        List<Product> stockOfBranch = ServerContext.getInstance()
                .getInventoryService()
                .getStockOf(client.getBranch());

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.PRODUCT_LIST, new ArrayList<>(stockOfBranch));
    }
}
