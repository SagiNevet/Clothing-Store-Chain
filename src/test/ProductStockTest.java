package test;

import common.exception.InsufficientStockException;
import common.model.Product;
import common.model.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the stock rules of a product, including the attempt to sell more items
 * than the branch actually holds.
 */
public class ProductStockTest {

    /** The quantity every test starts from. */
    private static final int INITIAL_QUANTITY = 10;

    /** The product under test, rebuilt before every test method. */
    private Product shirt;

    /**
     * Builds a fresh product before each test, so no test can be affected by
     * the changes of another one.
     */
    @BeforeEach
    public void createShirt() {
        shirt = new Product("P-100", "Blue Shirt", ProductCategory.SHIRTS, 89.90, INITIAL_QUANTITY);
    }

    @Test
    @DisplayName("Selling items lowers the stock by exactly that amount")
    public void sellingLowersTheStock() throws InsufficientStockException {
        shirt.decreaseQuantity(3);

        assertEquals(7, shirt.getQuantity());
    }

    @Test
    @DisplayName("A delivery raises the stock")
    public void restockingRaisesTheStock() {
        shirt.increaseQuantity(5);

        assertEquals(15, shirt.getQuantity());
    }

    @Test
    @DisplayName("Selling more than the stock is refused and the stock stays untouched")
    public void sellingMoreThanTheStockIsRefused() {
        InsufficientStockException failure = assertThrows(
                InsufficientStockException.class,
                () -> shirt.decreaseQuantity(INITIAL_QUANTITY + 1));

        assertEquals("P-100", failure.getProductId());
        assertEquals(INITIAL_QUANTITY + 1, failure.getRequestedQuantity());
        assertEquals(INITIAL_QUANTITY, failure.getAvailableQuantity());
        // The most important assertion of this test: a refused sale must leave
        // the stock exactly as it was, never in a half changed state.
        assertEquals(INITIAL_QUANTITY, shirt.getQuantity());
    }

    @Test
    @DisplayName("Selling the whole stock is allowed and leaves zero")
    public void sellingTheEntireStockIsAllowed() throws InsufficientStockException {
        shirt.decreaseQuantity(INITIAL_QUANTITY);

        assertEquals(0, shirt.getQuantity());
        assertFalse(shirt.hasEnoughStock(1));
    }

    @Test
    @DisplayName("A quantity of zero or less is a programming mistake, not a business failure")
    public void nonPositiveQuantitiesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> shirt.decreaseQuantity(0));
        assertThrows(IllegalArgumentException.class, () -> shirt.decreaseQuantity(-2));
        assertThrows(IllegalArgumentException.class, () -> shirt.increaseQuantity(0));
    }

    @Test
    @DisplayName("A product cannot be created with a negative price or quantity")
    public void invalidProductValuesAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new Product("P-1", "Bad", ProductCategory.PANTS, -5.0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Product("P-2", "Bad", ProductCategory.PANTS, 5.0, -1));
    }

    @Test
    @DisplayName("A copy holds the same values but is a separate object")
    public void copyIsIndependentOfTheOriginal() throws InsufficientStockException {
        Product copyOfShirt = shirt.copy();

        copyOfShirt.decreaseQuantity(4);

        assertNotSame(shirt, copyOfShirt);
        assertEquals(INITIAL_QUANTITY, shirt.getQuantity());
        assertEquals(6, copyOfShirt.getQuantity());
        // equals compares the catalogue identifier, so the copy is still
        // considered the same catalogue product.
        assertTrue(shirt.equals(copyOfShirt));
    }
}
