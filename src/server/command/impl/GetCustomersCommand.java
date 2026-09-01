package server.command.impl;

import common.exception.ChainStoreException;
import common.model.Customer;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import server.command.Command;
import server.core.ConnectedClient;
import server.core.ServerContext;

import java.util.ArrayList;
import java.util.List;

public class GetCustomersCommand implements Command {

    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        List<Customer> customers = ServerContext.getInstance()
                .getCustomerService()
                .getAllCustomers();

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.CUSTOMER_LIST, new ArrayList<>(customers));
    }
}
