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

public class AddCustomerCommand implements Command {

    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        String idNumber = request.getString(ProtocolKeys.CUSTOMER_ID);
        String fullName = request.getString(ProtocolKeys.FULL_NAME);
        String phone = request.getString(ProtocolKeys.PHONE);

        CustomerService customerService = ServerContext.getInstance().getCustomerService();
        Customer newCustomer = customerService.addCustomer(idNumber, fullName, phone);

        LogManager.getInstance().logCustomerAction(client.getEmployeeNumber(), client.getBranch(),
                "ADD_CUSTOMER", "Registered " + fullName + " (id " + idNumber
                        + ") as a " + newCustomer.getCustomerType().getDisplayName());

        EventPublisher.getInstance().publishToAll(new ServerEvent(EventType.CUSTOMERS_UPDATED)
                .withPayload(ProtocolKeys.CUSTOMER_LIST,
                        new ArrayList<>(customerService.getAllCustomers())));

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.CUSTOMER, newCustomer);
    }
}
