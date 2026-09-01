package server.command.impl;

import common.exception.ChainStoreException;
import common.model.Branch;
import common.model.Customer;
import common.model.CustomerType;
import common.model.Product;
import common.model.Sale;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import common.protocol.ServerEvent;
import common.util.IdGenerator;
import server.command.Command;
import server.core.ConnectedClient;
import server.core.ServerContext;
import server.observer.EventPublisher;
import server.service.CustomerService;
import server.service.InventoryService;
import server.service.LogManager;

import java.util.ArrayList;

public class SellProductCommand implements Command {

    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        Branch branch = client.getBranch();
        String productId = request.getString(ProtocolKeys.PRODUCT_ID);
        String customerId = request.getString(ProtocolKeys.CUSTOMER_ID);
        int quantity = request.getInt(ProtocolKeys.QUANTITY);

        ServerContext context = ServerContext.getInstance();
        InventoryService inventoryService = context.getInventoryService();
        CustomerService customerService = context.getCustomerService();

        Customer customer = customerService.findCustomer(customerId);
        Product productBeforeSale = inventoryService.findProduct(branch, productId).copy();
        CustomerType customerTypeAtSale = customer.getCustomerType();

        double finalPrice = customer.calculateFinalPrice(productBeforeSale.getPrice(), quantity);

        Product productAfterSale = inventoryService.sell(branch, productId, quantity);

        Sale sale = new Sale(IdGenerator.nextSaleId(), branch, client.getEmployeeNumber(),
                customerId, customerTypeAtSale, productBeforeSale, quantity, finalPrice);
        context.getSalesRepository().append(sale);

        Customer customerAfterSale = customerService.registerPurchase(customerId, finalPrice);

        writeLogLines(client, sale, customerTypeAtSale, customerAfterSale);
        publishUpdates(branch, productAfterSale, customerService);

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.SALE, sale)
                .withPayload(ProtocolKeys.PRODUCT, productAfterSale)
                .withPayload(ProtocolKeys.CUSTOMER, customerAfterSale);
    }

    private void writeLogLines(ConnectedClient client, Sale sale,
                               CustomerType customerTypeAtSale, Customer customerAfterSale) {
        LogManager logManager = LogManager.getInstance();
        logManager.logSalesAction(client.getEmployeeNumber(), client.getBranch(), "SELL_PRODUCT",
                sale.getProductName() + " x" + sale.getQuantity()
                        + " to customer " + sale.getCustomerIdNumber()
                        + " (" + customerTypeAtSale.getDisplayName() + ")"
                        + ", paid " + sale.getFinalPrice()
                        + " instead of " + sale.getTotalBeforeDiscount());

        if (customerAfterSale.getCustomerType() != customerTypeAtSale) {
            logManager.logCustomerAction(client.getEmployeeNumber(), client.getBranch(),
                    "CUSTOMER_UPGRADED",
                    "Customer " + customerAfterSale.getIdNumber() + " moved from "
                            + customerTypeAtSale.getDisplayName() + " to "
                            + customerAfterSale.getCustomerType().getDisplayName());
        }
    }

    private void publishUpdates(Branch branch, Product productAfterSale,
                                CustomerService customerService) {
        EventPublisher publisher = EventPublisher.getInstance();

        publisher.publishToBranch(branch, new ServerEvent(EventType.INVENTORY_UPDATED)
                .withPayload(ProtocolKeys.PRODUCT, productAfterSale));

        publisher.publishToAll(new ServerEvent(EventType.CUSTOMERS_UPDATED)
                .withPayload(ProtocolKeys.CUSTOMER_LIST,
                        new ArrayList<>(customerService.getAllCustomers())));
    }
}
