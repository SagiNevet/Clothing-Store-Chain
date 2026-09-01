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

public class GetInventoryCommand implements Command {

    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        List<Product> stockOfBranch = ServerContext.getInstance()
                .getInventoryService()
                .getStockOf(client.getBranch());

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.PRODUCT_LIST, new ArrayList<>(stockOfBranch));
    }
}
