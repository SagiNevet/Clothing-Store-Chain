package client.controller;

import common.exception.ChainStoreException;
import common.model.Customer;
import common.protocol.ActionType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;

import java.util.List;

public class CustomerController {

    @SuppressWarnings("unchecked")
    public List<Customer> loadCustomers() throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.GET_CUSTOMERS));
        return (List<Customer>) response.getPayload(ProtocolKeys.CUSTOMER_LIST);
    }

    public Customer addCustomer(String idNumber, String fullName, String phone)
            throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.ADD_CUSTOMER)
                .withParameter(ProtocolKeys.CUSTOMER_ID, idNumber)
                .withParameter(ProtocolKeys.FULL_NAME, fullName)
                .withParameter(ProtocolKeys.PHONE, phone));
        return (Customer) response.getPayload(ProtocolKeys.CUSTOMER);
    }

    public Customer updateCustomer(String idNumber, String fullName, String phone)
            throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.UPDATE_CUSTOMER)
                .withParameter(ProtocolKeys.CUSTOMER_ID, idNumber)
                .withParameter(ProtocolKeys.FULL_NAME, fullName)
                .withParameter(ProtocolKeys.PHONE, phone));
        return (Customer) response.getPayload(ProtocolKeys.CUSTOMER);
    }

    private Request newRequest(ActionType actionType) {
        ClientSession session = ClientSession.getInstance();
        return new Request(actionType,
                session.getCurrentEmployee().getEmployeeNumber(),
                session.getBranch());
    }

    private Response sendAndVerify(Request request) throws ChainStoreException {
        Response response = ClientSession.getInstance().getConnection().send(request);
        if (!response.isSuccess()) {
            throw new ChainStoreException(response.getMessage());
        }
        return response;
    }
}
