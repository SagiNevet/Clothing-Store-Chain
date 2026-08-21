package client.controller;

import common.exception.ChainStoreException;
import common.model.Product;
import common.model.ProductCategory;
import common.model.Sale;
import common.protocol.ActionType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;

import java.util.List;

/**
 * Turns what the inventory screen wants into requests, and the answers back
 * into model objects.
 * <p>
 * The screen never builds a {@link Request} and never reads a payload key. That
 * keeps the window free of protocol details, and it is what allowed the whole
 * network layer to be tested without opening a single window.
 * </p>
 * <p>
 * <b>Threading:</b> every method blocks while it waits for the server, so all of
 * them must be called from a background thread and never from the Swing event
 * dispatch thread.
 * </p>
 */
public class InventoryController {

    /**
     * Fetches the stock of the branch of the logged in employee.
     *
     * @return the products of that branch
     * @throws ChainStoreException if the server refused the request or could not
     *                             be reached
     */
    @SuppressWarnings("unchecked")
    public List<Product> loadInventory() throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.GET_INVENTORY));
        return (List<Product>) response.getPayload(ProtocolKeys.PRODUCT_LIST);
    }

    /**
     * Sells items of one product to one customer.
     *
     * @param productId  the product being sold
     * @param quantity   how many items are being sold
     * @param customerId the identity number of the buying customer
     * @return the sale recorded by the server, holding the final price
     * @throws ChainStoreException if the stock is too small, the customer is
     *                             unknown, or the server cannot be reached
     */
    public Sale sellProduct(String productId, int quantity, String customerId)
            throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.SELL_PRODUCT)
                .withParameter(ProtocolKeys.PRODUCT_ID, productId)
                .withParameter(ProtocolKeys.QUANTITY, quantity)
                .withParameter(ProtocolKeys.CUSTOMER_ID, customerId));
        return (Sale) response.getPayload(ProtocolKeys.SALE);
    }

    /**
     * Adds items to the stock after a delivery from a supplier.
     *
     * @param productId the product being restocked
     * @param quantity  how many items arrived
     * @return the product as it looks after the delivery
     * @throws ChainStoreException if the server refused the request or could not
     *                             be reached
     */
    public Product restockProduct(String productId, int quantity) throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.RESTOCK_PRODUCT)
                .withParameter(ProtocolKeys.PRODUCT_ID, productId)
                .withParameter(ProtocolKeys.QUANTITY, quantity));
        return (Product) response.getPayload(ProtocolKeys.PRODUCT);
    }

    /**
     * Adds a brand new product to the catalogue of the branch.
     *
     * @param productId the catalogue identifier of the new product
     * @param name      the name of the new product
     * @param category  the category of the new product
     * @param price     the catalogue price of one item
     * @param quantity  how many items are in stock to begin with
     * @return the product that was stored
     * @throws ChainStoreException if the identifier is already used, the role is
     *                             not allowed, or the server cannot be reached
     */
    public Product addProduct(String productId, String name, ProductCategory category,
                              double price, int quantity) throws ChainStoreException {
        Product newProduct = new Product(productId, name, category, price, quantity);
        Response response = sendAndVerify(newRequest(ActionType.ADD_PRODUCT)
                .withParameter(ProtocolKeys.PRODUCT, newProduct));
        return (Product) response.getPayload(ProtocolKeys.PRODUCT);
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
     * <p>
     * Doing this in one place means every method above can be written as if
     * nothing can fail, and the screen has exactly one {@code catch} for all of
     * them.
     * </p>
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
