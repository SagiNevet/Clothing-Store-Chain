package test;

import common.exception.ChainStoreException;
import common.exception.EntityNotFoundException;
import common.exception.InsufficientStockException;
import common.model.Branch;
import common.model.Customer;
import common.model.CustomerType;
import common.model.Employee;
import common.model.Product;
import common.protocol.ActionType;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import common.protocol.ServerEvent;
import client.net.ServerConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.core.ChainServer;
import server.core.DataSeeder;
import server.core.ServerContext;
import server.service.CustomerService;
import server.service.InventoryService;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class InventoryAndSalesTest {

    private static final int TEST_PORT = 5053;

    private static final int WAIT_TIMEOUT_SECONDS = 10;

    private static final String MANAGER_NUMBER = "1001";

    private static final String CASHIER_NUMBER = "1002";

    private static final double MONEY_TOLERANCE = 0.001;

    private static ChainServer server;

    private static Thread serverThread;

    @BeforeAll
    public static void startServer() throws InterruptedException {
        server = new ChainServer(TEST_PORT);
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception serverFailure) {
                System.err.println("[Test] the test server stopped: " + serverFailure);
            }
        }, "test-server-inventory-suite");
        serverThread.setDaemon(true);
        serverThread.start();
        waitUntilServerAcceptsConnections();
    }

    @AfterAll
    public static void stopServer() throws InterruptedException {
        server.stop();
        serverThread.join(WAIT_TIMEOUT_SECONDS * 1000L);
    }

    @Test
    @DisplayName("Many sellers at once can never drive the stock below zero")
    public void concurrentSellersCannotDriveTheStockBelowZero() throws Exception {
        InventoryService inventoryService = ServerContext.getInstance().getInventoryService();
        String productId = uniqueProductId("RACE");
        int startingStock = 20;
        int sellersAtOnce = 40;
        inventoryService.addProduct(Branch.TEL_AVIV,
                new Product(productId, "Race Test Shirt",
                        common.model.ProductCategory.SHIRTS, 100.0, startingStock));

        AtomicInteger successfulSales = new AtomicInteger(0);
        AtomicInteger refusedSales = new AtomicInteger(0);
        CountDownLatch everyoneReady = new CountDownLatch(1);
        CountDownLatch everyoneFinished = new CountDownLatch(sellersAtOnce);

        for (int sellerIndex = 0; sellerIndex < sellersAtOnce; sellerIndex++) {
            new Thread(() -> {
                try {

                    everyoneReady.await();
                    inventoryService.sell(Branch.TEL_AVIV, productId, 1);
                    successfulSales.incrementAndGet();
                } catch (InsufficientStockException stockRanOut) {
                    refusedSales.incrementAndGet();
                } catch (Exception unexpectedFailure) {
                    fail("a seller thread failed unexpectedly: " + unexpectedFailure);
                } finally {
                    everyoneFinished.countDown();
                }
            }, "race-seller-" + sellerIndex).start();
        }

        everyoneReady.countDown();
        assertTrue(everyoneFinished.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "the selling threads did not finish in time");

        Product productAfterTheRace = inventoryService.findProduct(Branch.TEL_AVIV, productId);
        assertEquals(0, productAfterTheRace.getQuantity(),
                "the stock must end at exactly zero, never below it");
        assertEquals(startingStock, successfulSales.get(),
                "exactly as many sales as there were items must succeed");
        assertEquals(sellersAtOnce - startingStock, refusedSales.get(),
                "every seller beyond the stock must be refused");
    }

    @Test
    @DisplayName("Selling more than the stock leaves the stock untouched")
    public void arefusedSaleChangesNothing() throws Exception {
        InventoryService inventoryService = ServerContext.getInstance().getInventoryService();
        String productId = uniqueProductId("REFUSE");
        inventoryService.addProduct(Branch.TEL_AVIV,
                new Product(productId, "Refuse Test Pants",
                        common.model.ProductCategory.PANTS, 150.0, 3));

        assertThrows(InsufficientStockException.class,
                () -> inventoryService.sell(Branch.TEL_AVIV, productId, 4));

        assertEquals(3, inventoryService.findProduct(Branch.TEL_AVIV, productId).getQuantity());
    }

    @Test
    @DisplayName("The two branches keep separate stocks of the same product")
    public void branchesKeepSeparateStocks() throws Exception {
        InventoryService inventoryService = ServerContext.getInstance().getInventoryService();

        Product telAvivShirt = inventoryService.findProduct(Branch.TEL_AVIV, "P-100");
        Product jerusalemShirt = inventoryService.findProduct(Branch.JERUSALEM, "P-100");
        int telAvivStockBefore = telAvivShirt.getQuantity();
        int jerusalemStockBefore = jerusalemShirt.getQuantity();

        inventoryService.sell(Branch.TEL_AVIV, "P-100", 1);

        assertEquals(telAvivStockBefore - 1,
                inventoryService.findProduct(Branch.TEL_AVIV, "P-100").getQuantity());
        assertEquals(jerusalemStockBefore,
                inventoryService.findProduct(Branch.JERUSALEM, "P-100").getQuantity(),
                "a sale in Tel Aviv must not touch the stock of Jerusalem");
    }

    @Test
    @DisplayName("Asking for a product the branch does not carry fails clearly")
    public void unknownProductIsReported() {
        InventoryService inventoryService = ServerContext.getInstance().getInventoryService();

        assertThrows(EntityNotFoundException.class,
                () -> inventoryService.findProduct(Branch.TEL_AVIV, "NO-SUCH-PRODUCT"));
    }

    @Test
    @DisplayName("A sale applies the purchase plan of the customer and upgrades the kind")
    public void aSaleAppliesThePurchasePlanAndUpgradesTheCustomer() throws Exception {
        CustomerService customerService = ServerContext.getInstance().getCustomerService();
        String customerId = uniqueCustomerId();
        customerService.addCustomer(customerId, "Plan Test Customer", "050-0000000");

        try (TestClient client = new TestClient()) {
            client.login(CASHIER_NUMBER);

            Response firstSale = client.sell("P-400", 1, customerId);
            assertTrue(firstSale.isSuccess(), firstSale.getMessage());
            common.model.Sale sale = (common.model.Sale) firstSale.getPayload(ProtocolKeys.SALE);
            assertEquals(CustomerType.NEW, sale.getCustomerTypeAtSale());
            assertEquals(roundToCents(sale.getTotalBeforeDiscount() * 0.9), sale.getFinalPrice(),
                    MONEY_TOLERANCE);

            Customer customerAfterSale =
                    (Customer) firstSale.getPayload(ProtocolKeys.CUSTOMER);
            assertEquals(CustomerType.RETURNING, customerAfterSale.getCustomerType());

            Response secondSale = client.sell("P-400", 1, customerId);
            common.model.Sale secondSaleRecord =
                    (common.model.Sale) secondSale.getPayload(ProtocolKeys.SALE);
            assertEquals(CustomerType.RETURNING, secondSaleRecord.getCustomerTypeAtSale());
            assertEquals(roundToCents(secondSaleRecord.getTotalBeforeDiscount() * 0.95),
                    secondSaleRecord.getFinalPrice(), MONEY_TOLERANCE);
        }
    }

    private double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    @Test
    @DisplayName("A sale is pushed to the other employees of the same branch")
    public void aSaleIsPushedToTheSameBranch() throws Exception {
        CustomerService customerService = ServerContext.getInstance().getCustomerService();
        String customerId = uniqueCustomerId();
        customerService.addCustomer(customerId, "Event Test Customer", "050-0000001");

        try (TestClient sellingClient = new TestClient();
             TestClient watchingClient = new TestClient()) {

            sellingClient.login(CASHIER_NUMBER);
            watchingClient.login(MANAGER_NUMBER);

            CountDownLatch inventoryEventArrived = new CountDownLatch(1);
            AtomicInteger quantityReportedByTheEvent = new AtomicInteger(-1);

            watchingClient.connection.getEventDispatcher().subscribe(event -> {
                if (event.getEventType() == EventType.INVENTORY_UPDATED) {
                    Product changedProduct = (Product) event.getPayload(ProtocolKeys.PRODUCT);
                    if ("P-300".equals(changedProduct.getProductId())) {
                        quantityReportedByTheEvent.set(changedProduct.getQuantity());
                        inventoryEventArrived.countDown();
                    }
                }
            });

            InventoryService inventoryService = ServerContext.getInstance().getInventoryService();
            int stockBeforeSale =
                    inventoryService.findProduct(Branch.TEL_AVIV, "P-300").getQuantity();

            assertTrue(sellingClient.sell("P-300", 1, customerId).isSuccess());

            assertTrue(inventoryEventArrived.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "the watching employee never received the inventory event");
            assertEquals(stockBeforeSale - 1, quantityReportedByTheEvent.get(),
                    "the event must carry the quantity as it is after the sale");
        }
    }

    @Test
    @DisplayName("A new customer is pushed to every branch of the chain")
    public void aNewCustomerIsPushedToEveryBranch() throws Exception {
        try (TestClient telAvivClient = new TestClient();
             TestClient jerusalemClient = new TestClient()) {

            telAvivClient.login(CASHIER_NUMBER);
            jerusalemClient.login("2002");

            CountDownLatch customerEventArrived = new CountDownLatch(1);
            jerusalemClient.connection.getEventDispatcher().subscribe(event -> {
                if (event.getEventType() == EventType.CUSTOMERS_UPDATED) {
                    customerEventArrived.countDown();
                }
            });

            String customerId = uniqueCustomerId();
            Response addResponse = telAvivClient.send(new Request(ActionType.ADD_CUSTOMER,
                    CASHIER_NUMBER, Branch.TEL_AVIV)
                    .withParameter(ProtocolKeys.CUSTOMER_ID, customerId)
                    .withParameter(ProtocolKeys.FULL_NAME, "Cross Branch Customer")
                    .withParameter(ProtocolKeys.PHONE, "050-0000002"));

            assertTrue(addResponse.isSuccess(), addResponse.getMessage());
            
            assertTrue(customerEventArrived.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "the employee of the other branch never received the customer event");
        }
    }

    @Test
    @DisplayName("Registering the same identity number twice is refused")
    public void duplicateCustomerIsRefused() throws Exception {
        CustomerService customerService = ServerContext.getInstance().getCustomerService();
        String customerId = uniqueCustomerId();
        customerService.addCustomer(customerId, "First Registration", "050-0000003");

        ChainStoreException failure = assertThrows(ChainStoreException.class,
                () -> customerService.addCustomer(customerId, "Second Registration", "050-0000004"));

        assertTrue(failure.getMessage().contains("already registered"),
                "unexpected message: " + failure.getMessage());
    }

    @Test
    @DisplayName("The inventory a client receives holds only its own branch")
    public void aClientOnlyReceivesItsOwnBranch() throws Exception {
        try (TestClient jerusalemClient = new TestClient()) {
            Employee employee = jerusalemClient.login("2002");
            assertEquals(Branch.JERUSALEM, employee.getBranch());

            Response response = jerusalemClient.send(new Request(ActionType.GET_INVENTORY,
                    employee.getEmployeeNumber(), employee.getBranch()));

            @SuppressWarnings("unchecked")
            List<Product> stock = (List<Product>) response.getPayload(ProtocolKeys.PRODUCT_LIST);
            assertNotNull(stock);

            InventoryService inventoryService = ServerContext.getInstance().getInventoryService();
            int jerusalemStock =
                    inventoryService.findProduct(Branch.JERUSALEM, "P-100").getQuantity();
            for (Product product : stock) {
                if ("P-100".equals(product.getProductId())) {
                    assertEquals(jerusalemStock, product.getQuantity());
                }
            }
        }
    }

    private String uniqueProductId(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    private String uniqueCustomerId() {
        return "TEST-" + System.nanoTime();
    }

    private static void waitUntilServerAcceptsConnections() throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_SECONDS * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try (Socket probeSocket = new Socket("localhost", TEST_PORT)) {
                return;
            } catch (IOException notListeningYet) {
                Thread.sleep(50);
            }
        }
        fail("The test server did not start listening on port " + TEST_PORT);
    }

    private static final class TestClient implements AutoCloseable {

        private final ServerConnection connection = new ServerConnection();

        private Employee employee;

        private TestClient() throws ChainStoreException {
            connection.connect("localhost", TEST_PORT);
        }

        private Employee login(String employeeNumber) throws ChainStoreException {
            Response response = send(new Request(ActionType.LOGIN)
                    .withParameter(ProtocolKeys.EMPLOYEE_NUMBER, employeeNumber)
                    .withParameter(ProtocolKeys.PASSWORD, DataSeeder.getDemoPassword()));
            if (!response.isSuccess()) {
                throw new ChainStoreException(response.getMessage());
            }
            employee = (Employee) response.getPayload(ProtocolKeys.EMPLOYEE);
            return employee;
        }

        private Response sell(String productId, int quantity, String customerId)
                throws ChainStoreException {
            return send(new Request(ActionType.SELL_PRODUCT,
                    employee.getEmployeeNumber(), employee.getBranch())
                    .withParameter(ProtocolKeys.PRODUCT_ID, productId)
                    .withParameter(ProtocolKeys.QUANTITY, quantity)
                    .withParameter(ProtocolKeys.CUSTOMER_ID, customerId));
        }

        private Response send(Request request) throws ChainStoreException {
            return connection.send(request);
        }

        @Override
        public void close() {
            connection.disconnect();
        }
    }
}
