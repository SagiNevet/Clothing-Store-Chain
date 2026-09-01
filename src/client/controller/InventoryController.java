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

public class InventoryController {

    @SuppressWarnings("unchecked")
    public List<Product> loadInventory() throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.GET_INVENTORY));
        return (List<Product>) response.getPayload(ProtocolKeys.PRODUCT_LIST);
    }

    public Sale sellProduct(String productId, int quantity, String customerId)
            throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.SELL_PRODUCT)
                .withParameter(ProtocolKeys.PRODUCT_ID, productId)
                .withParameter(ProtocolKeys.QUANTITY, quantity)
                .withParameter(ProtocolKeys.CUSTOMER_ID, customerId));
        return (Sale) response.getPayload(ProtocolKeys.SALE);
    }

    public Product restockProduct(String productId, int quantity) throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.RESTOCK_PRODUCT)
                .withParameter(ProtocolKeys.PRODUCT_ID, productId)
                .withParameter(ProtocolKeys.QUANTITY, quantity));
        return (Product) response.getPayload(ProtocolKeys.PRODUCT);
    }

    public Product addProduct(String productId, String name, ProductCategory category,
                              double price, int quantity) throws ChainStoreException {
        Product newProduct = new Product(productId, name, category, price, quantity);
        Response response = sendAndVerify(newRequest(ActionType.ADD_PRODUCT)
                .withParameter(ProtocolKeys.PRODUCT, newProduct));
        return (Product) response.getPayload(ProtocolKeys.PRODUCT);
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
