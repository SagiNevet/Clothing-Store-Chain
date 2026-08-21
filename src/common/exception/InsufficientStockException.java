package common.exception;

/**
 * Thrown when a sale is attempted for more items than the branch currently has.
 * <p>
 * This exception carries the numbers involved and not only a text message, so
 * the GUI can build its own sentence and the server can write a precise log
 * line without parsing strings.
 * </p>
 */
public class InsufficientStockException extends ChainStoreException {

    /** Serialization version, required because exceptions are serializable. */
    private static final long serialVersionUID = 1L;

    /** The identifier of the product that could not be sold. */
    private final String productId;

    /** The amount the employee asked to sell. */
    private final int requestedQuantity;

    /** The amount actually available in the branch at that moment. */
    private final int availableQuantity;

    /**
     * Creates a stock failure.
     *
     * @param productId         the identifier of the product that could not be sold
     * @param requestedQuantity the amount the employee asked to sell
     * @param availableQuantity the amount actually available in the branch
     */
    public InsufficientStockException(String productId, int requestedQuantity, int availableQuantity) {
        super("Not enough stock for product " + productId
                + ": requested " + requestedQuantity + ", available " + availableQuantity);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    /**
     * Returns the product that could not be sold.
     *
     * @return the product identifier
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Returns the quantity the employee asked to sell.
     *
     * @return the requested quantity
     */
    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    /**
     * Returns the quantity that was actually available.
     *
     * @return the available quantity
     */
    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
