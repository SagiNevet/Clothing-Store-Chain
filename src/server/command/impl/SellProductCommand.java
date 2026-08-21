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

/**
 * Sells items to a customer: applies the purchase plan of that customer, takes
 * the items out of the stock of the branch, records the sale, updates the
 * purchase history and tells every screen that needs to know.
 * <p>
 * <b>This class is where the polymorphism requirement is actually used.</b> The
 * price is produced by one single call:
 * </p>
 * <pre>
 *     double finalPrice = customer.calculateFinalPrice(unitPrice, quantity);
 * </pre>
 * <p>
 * There is no {@code if} and no {@code switch} on the kind of customer anywhere
 * in this class. Whether the object is a {@code NewCustomer}, a
 * {@code ReturningCustomer} or a {@code VipCustomer} is decided by Java at run
 * time, and adding a fourth kind of customer tomorrow would not change a single
 * line here.
 * </p>
 * <p>
 * <b>The order of the steps is deliberate.</b> The price is calculated first
 * because it only reads data and cannot fail. The stock is taken next, because
 * that is the step that may refuse the whole sale with
 * {@code InsufficientStockException} - and at that point nothing has been
 * changed yet, so a refused sale leaves no trace. Only afterwards is the sale
 * recorded and the purchase history updated.
 * </p>
 */
public class SellProductCommand implements Command {

    /**
     * {@inheritDoc}
     */
    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        Branch branch = client.getBranch();
        String productId = request.getString(ProtocolKeys.PRODUCT_ID);
        String customerId = request.getString(ProtocolKeys.CUSTOMER_ID);
        int quantity = request.getInt(ProtocolKeys.QUANTITY);

        ServerContext context = ServerContext.getInstance();
        InventoryService inventoryService = context.getInventoryService();
        CustomerService customerService = context.getCustomerService();

        // Both lookups throw EntityNotFoundException when they fail, so there is
        // no null check anywhere below this point.
        Customer customer = customerService.findCustomer(customerId);
        Product productBeforeSale = inventoryService.findProduct(branch, productId).copy();
        CustomerType customerTypeAtSale = customer.getCustomerType();

        // The one polymorphic call the whole customer hierarchy exists for.
        double finalPrice = customer.calculateFinalPrice(productBeforeSale.getPrice(), quantity);

        // The step that can refuse the sale. Nothing has changed until it succeeds.
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

    /**
     * Writes the sale to the sales log, and a second line to the customers log
     * when the sale moved the customer to another kind.
     *
     * @param client             the connection that performed the sale
     * @param sale               the sale that was completed
     * @param customerTypeAtSale the kind of customer before the sale
     * @param customerAfterSale  the customer as it looks after the sale
     */
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

    /**
     * Tells the connected clients what changed.
     * <p>
     * Two different audiences, exactly as the requirements describe: the stock
     * belongs to one branch, so only that branch is told; the customer list
     * belongs to the whole chain, so everybody is told.
     * </p>
     *
     * @param branch           the branch the sale happened in
     * @param productAfterSale the product as it looks after the sale
     * @param customerService  the service holding the customer list
     */
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
