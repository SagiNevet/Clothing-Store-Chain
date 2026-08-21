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

/**
 * Returns the customer list of the whole chain.
 * <p>
 * Unlike the inventory, the answer is not filtered by branch: the requirement
 * states that the customer list is shared by every branch of the chain.
 * </p>
 * <p>
 * The customer objects travel to the client as they are, each one still an
 * instance of its own subclass. That is what lets the customers table show the
 * purchase plan of every customer by calling
 * {@code getPurchasePlanDescription()} - the polymorphism survives the trip
 * over the socket.
 * </p>
 */
public class GetCustomersCommand implements Command {

    /**
     * {@inheritDoc}
     */
    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        List<Customer> customers = ServerContext.getInstance()
                .getCustomerService()
                .getAllCustomers();

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.CUSTOMER_LIST, new ArrayList<>(customers));
    }
}
