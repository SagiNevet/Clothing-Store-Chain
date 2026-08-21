package test;

import common.model.Customer;
import common.model.CustomerFactory;
import common.model.CustomerType;
import common.model.NewCustomer;
import common.model.ReturningCustomer;
import common.model.VipCustomer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the three purchase plans and the movement of a customer from one kind
 * to another.
 * <p>
 * These are the most important tests of the project, because the purchase plans
 * are where the polymorphism requirement is implemented.
 * </p>
 */
public class CustomerPricingTest {

    /** The tolerance used when comparing money amounts held in a double. */
    private static final double MONEY_TOLERANCE = 0.001;

    /** A price used by several tests. */
    private static final double SHIRT_PRICE = 100.0;

    @Test
    @DisplayName("A new customer receives 10 percent off on the first purchase")
    public void newCustomerGetsWelcomeDiscountOnFirstPurchase() {
        Customer customer = new NewCustomer("111", "Dana Levi", "050-1111111");

        double finalPrice = customer.calculateFinalPrice(SHIRT_PRICE, 1);

        assertEquals(90.0, finalPrice, MONEY_TOLERANCE);
    }

    @Test
    @DisplayName("A new customer who somehow already bought pays the full price")
    public void newCustomerWithHistoryPaysFullPrice() {
        Customer customer = new NewCustomer("111", "Dana Levi", "050-1111111", 1, 90.0);

        double finalPrice = customer.calculateFinalPrice(SHIRT_PRICE, 1);

        assertEquals(100.0, finalPrice, MONEY_TOLERANCE);
    }

    @Test
    @DisplayName("A returning customer receives 5 percent off on a small purchase")
    public void returningCustomerGetsLoyaltyDiscount() {
        Customer customer = new ReturningCustomer("222", "Yossi Cohen", "050-2222222", 1, 90.0);

        double finalPrice = customer.calculateFinalPrice(SHIRT_PRICE, 2);

        assertEquals(190.0, finalPrice, MONEY_TOLERANCE);
    }

    @Test
    @DisplayName("A returning customer receives 8 percent off from three items")
    public void returningCustomerGetsBulkDiscount() {
        Customer customer = new ReturningCustomer("222", "Yossi Cohen", "050-2222222", 1, 90.0);

        double finalPrice = customer.calculateFinalPrice(SHIRT_PRICE, 3);

        assertEquals(276.0, finalPrice, MONEY_TOLERANCE);
    }

    @Test
    @DisplayName("A VIP customer receives 15 percent off on a regular order")
    public void vipCustomerGetsVipDiscount() {
        Customer customer = new VipCustomer("333", "Rina Bar", "050-3333333", 6, 1200.0);

        double finalPrice = customer.calculateFinalPrice(SHIRT_PRICE, 2);

        assertEquals(170.0, finalPrice, MONEY_TOLERANCE);
    }

    @Test
    @DisplayName("A VIP customer receives 20 percent off above the large order threshold")
    public void vipCustomerGetsLargeOrderDiscount() {
        Customer customer = new VipCustomer("333", "Rina Bar", "050-3333333", 6, 1200.0);

        double finalPrice = customer.calculateFinalPrice(SHIRT_PRICE, 6);

        assertEquals(480.0, finalPrice, MONEY_TOLERANCE);
    }

    @Test
    @DisplayName("The same purchase costs a different price for each customer kind")
    public void everyCustomerKindProducesADifferentPrice() {
        Customer newCustomer = new NewCustomer("111", "Dana", "050-1111111");
        Customer returningCustomer = new ReturningCustomer("222", "Yossi", "050-2222222", 1, 90.0);
        Customer vipCustomer = new VipCustomer("333", "Rina", "050-3333333", 6, 1200.0);

        // This is the polymorphism the project is graded on: the very same call
        // on three different objects runs three different purchase plans, and
        // the calling code does not ask any of them what kind of customer it is.
        double priceForNew = newCustomer.calculateFinalPrice(SHIRT_PRICE, 2);
        double priceForReturning = returningCustomer.calculateFinalPrice(SHIRT_PRICE, 2);
        double priceForVip = vipCustomer.calculateFinalPrice(SHIRT_PRICE, 2);

        assertEquals(180.0, priceForNew, MONEY_TOLERANCE);
        assertEquals(190.0, priceForReturning, MONEY_TOLERANCE);
        assertEquals(170.0, priceForVip, MONEY_TOLERANCE);
    }

    @Test
    @DisplayName("A new customer becomes a returning customer after the first purchase")
    public void newCustomerBecomesReturningAfterFirstPurchase() {
        Customer customer = CustomerFactory.createNewCustomer("444", "Amit Shani", "050-4444444");

        customer.registerPurchase(90.0);
        Customer upgradedCustomer = CustomerFactory.upgradeIfNeeded(customer);

        assertEquals(CustomerType.RETURNING, upgradedCustomer.getCustomerType());
        assertNotSame(customer, upgradedCustomer);
        assertEquals(1, upgradedCustomer.getPurchaseCount());
        assertEquals(90.0, upgradedCustomer.getTotalSpent(), MONEY_TOLERANCE);
    }

    @Test
    @DisplayName("A returning customer becomes VIP after five purchases")
    public void returningCustomerBecomesVipByPurchaseCount() {
        Customer customer = new ReturningCustomer("555", "Noa Gil", "050-5555555", 4, 200.0);

        customer.registerPurchase(50.0);
        Customer upgradedCustomer = CustomerFactory.upgradeIfNeeded(customer);

        assertEquals(CustomerType.VIP, upgradedCustomer.getCustomerType());
        assertEquals(5, upgradedCustomer.getPurchaseCount());
    }

    @Test
    @DisplayName("A returning customer becomes VIP after spending 1000")
    public void returningCustomerBecomesVipByTotalSpent() {
        Customer customer = new ReturningCustomer("666", "Tal Aviv", "050-6666666", 2, 900.0);

        customer.registerPurchase(150.0);
        Customer upgradedCustomer = CustomerFactory.upgradeIfNeeded(customer);

        assertEquals(CustomerType.VIP, upgradedCustomer.getCustomerType());
        assertTrue(upgradedCustomer.getTotalSpent() >= 1000.0);
    }

    @Test
    @DisplayName("A customer who did not reach a threshold keeps the very same object")
    public void customerBelowThresholdIsNotReplaced() {
        Customer customer = new ReturningCustomer("777", "Gal Dor", "050-7777777", 2, 300.0);

        Customer resultCustomer = CustomerFactory.upgradeIfNeeded(customer);

        assertSame(customer, resultCustomer);
    }

    @Test
    @DisplayName("An upgraded customer is still equal to the original one")
    public void upgradedCustomerRemainsEqualToTheOriginal() {
        Customer customer = CustomerFactory.createNewCustomer("888", "Omer Katz", "050-8888888");
        customer.registerPurchase(120.0);

        Customer upgradedCustomer = CustomerFactory.upgradeIfNeeded(customer);

        // equals compares the identity number only, so a list holding the old
        // object can locate it and replace it with the upgraded one.
        assertEquals(customer, upgradedCustomer);
        assertEquals(customer.hashCode(), upgradedCustomer.hashCode());
    }
}
