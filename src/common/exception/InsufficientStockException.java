package common.exception;

public class InsufficientStockException extends ChainStoreException {

    private static final long serialVersionUID = 1L;

    private final String productId;

    private final int requestedQuantity;

    private final int availableQuantity;

    public InsufficientStockException(String productId, int requestedQuantity, int availableQuantity) {
        super("Not enough stock for product " + productId
                + ": requested " + requestedQuantity + ", available " + availableQuantity);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
