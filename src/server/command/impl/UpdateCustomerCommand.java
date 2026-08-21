package server.command.impl;

import common.exception.ChainStoreException;
import common.model.Customer;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import common.protocol.ServerEvent;
import server.command.Command;
import server.core.ConnectedClient;
import server.core.ServerContext;
import server.observer.EventPublisher;
import server.service.CustomerService;
import server.service.LogManager;

import java.util.ArrayList;

/**
 * Updates the name and the phone number of an existing customer.
 * <p>
 * Notice what cannot be updated here: the kind of the customer. New, returning
 * and VIP are decided only by the purchase history through
 * {@code CustomerFactory}, so no employee can hand out a VIP discount by
 * editing a field.
 * </p>
 */
public class UpdateCustomerCommand implements Command {

    /**
     * {@inheritDoc}
     */
    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        String idNumber = request.getString(ProtocolKeys.CUSTOMER_ID);
        String fullName = request.getString(ProtocolKeys.FULL_NAME);
        String phone = request.getString(ProtocolKeys.PHONE);

        CustomerService customerService = ServerContext.getInstance().getCustomerService();
        Customer updatedCustomer = customerService.updateCustomer(idNumber, fullName, phone);

        LogManager.getInstance().logCustomerAction(client.getEmployeeNumber(), client.getBranch(),
                "UPDATE_CUSTOMER", "Updated the details of customer " + idNumber);

        EventPublisher.getInstance().publishToAll(new ServerEvent(EventType.CUSTOMERS_UPDATED)
                .withPayload(ProtocolKeys.CUSTOMER_LIST,
                        new ArrayList<>(customerService.getAllCustomers())));

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.CUSTOMER, updatedCustomer);
    }
}
