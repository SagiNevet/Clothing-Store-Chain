package client.controller;

import common.exception.ChainStoreException;
import common.model.Customer;
import common.protocol.ActionType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;

import java.util.List;

/**
 * Turns what the customers screen wants into requests, and the answers back
 * into model objects.
 * <p>
 * The customers that come back are still instances of their own subclasses -
 * {@code NewCustomer}, {@code ReturningCustomer} or {@code VipCustomer} - so
 * the table can ask each one for its own purchase plan without ever checking
 * what kind of customer it is holding.
 * </p>
 * <p>
 * <b>Threading:</b> every method blocks while it waits for the server, so all of
 * them must be called from a background thread.
 * </p>
 */
public class CustomerController {

    /**
     * Fetches the customer list of the whole chain.
     *
     * @return every customer of the chain
     * @throws ChainStoreException if the server refused the request or could not
     *                             be reached
     */
    @SuppressWarnings("unchecked")
    public List<Customer> loadCustomers() throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.GET_CUSTOMERS));
        return (List<Customer>) response.getPayload(ProtocolKeys.CUSTOMER_LIST);
    }

    /**
     * Registers a new customer of the chain.
     *
     * @param idNumber the identity number, the unique key of a customer
     * @param fullName the full name of the customer
     * @param phone    the phone number of the customer
     * @return the customer that was created, always a new customer to begin with
     * @throws ChainStoreException if the identity number is already registered
     *                             or the server cannot be reached
     */
    public Customer addCustomer(String idNumber, String fullName, String phone)
            throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.ADD_CUSTOMER)
                .withParameter(ProtocolKeys.CUSTOMER_ID, idNumber)
                .withParameter(ProtocolKeys.FULL_NAME, fullName)
                .withParameter(ProtocolKeys.PHONE, phone));
        return (Customer) response.getPayload(ProtocolKeys.CUSTOMER);
    }

    /**
     * Updates the name and the phone number of an existing customer.
     *
     * @param idNumber the customer to update
     * @param fullName the new full name
     * @param phone    the new phone number
     * @return the updated customer
     * @throws ChainStoreException if the customer does not exist or the server
     *                             cannot be reached
     */
    public Customer updateCustomer(String idNumber, String fullName, String phone)
            throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.UPDATE_CUSTOMER)
                .withParameter(ProtocolKeys.CUSTOMER_ID, idNumber)
                .withParameter(ProtocolKeys.FULL_NAME, fullName)
                .withParameter(ProtocolKeys.PHONE, phone));
        return (Customer) response.getPayload(ProtocolKeys.CUSTOMER);
    }

    /**
     * Builds a request already stamped with the employee and the branch of this
     * client.
     *
     * @param actionType the action to request
     * @return the request, ready for its parameters
     */
    private Request newRequest(ActionType actionType) {
        ClientSession session = ClientSession.getInstance();
        return new Request(actionType,
                session.getCurrentEmployee().getEmployeeNumber(),
                session.getBranch());
    }

    /**
     * Sends a request and turns a refusal into an exception.
     *
     * @param request the request to send
     * @return the successful answer
     * @throws ChainStoreException if the server refused the request or could not
     *                             be reached
     */
    private Response sendAndVerify(Request request) throws ChainStoreException {
        Response response = ClientSession.getInstance().getConnection().send(request);
        if (!response.isSuccess()) {
            throw new ChainStoreException(response.getMessage());
        }
        return response;
    }
}
