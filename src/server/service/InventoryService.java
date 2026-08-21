package server.service;

import common.exception.ChainStoreException;
import common.exception.EntityNotFoundException;
import common.exception.InsufficientStockException;
import common.exception.StorageException;
import common.model.Branch;
import common.model.Product;
import server.core.ServerContext;
import server.storage.InventoryRepository;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the stock of every branch and performs the sales and the deliveries.
 * <p>
 * <b>The stock lives in memory and is mirrored to the files.</b> Each branch has
 * its own {@link LinkedHashMap} of products, keyed by the product identifier so
 * that finding a product costs one lookup instead of a scan, and ordered so that
 * the table on screen always shows the products in the same order. After every
 * change the branch is written back to its own file, so a restart of the server
 * loses nothing.
 * </p>
 * <p>
 * <b>The locking is the heart of this class.</b> Two employees of the same
 * branch can try to sell the last shirt at the very same moment. The check "is
 * there enough stock" and the subtraction that follows it must happen inside one
 * lock, otherwise both threads pass the check and the stock goes below zero.
 * The lock chosen here is <b>the product object itself</b>, which is the
 * smallest area that still makes the operation correct:
 * </p>
 * <ul>
 *   <li>Two employees selling the <b>same</b> shirt are serialized, as they
 *       must be.</li>
 *   <li>Two employees selling <b>different</b> products never wait for each
 *       other at all, because they lock different objects.</li>
 *   <li>The two branches never meet, because they hold different maps.</li>
 * </ul>
 * <p>
 * Locking the whole method instead would have forced every sale in the chain
 * into a single queue, which is exactly the mistake the lecturer warned about.
 * </p>
 */
public class InventoryService {

    /** The stock of each branch, keyed by product identifier. */
    private final Map<Branch, Map<String, Product>> stockByBranch = new EnumMap<>(Branch.class);

    /**
     * Loads the stock of every branch from the files into memory.
     *
     * @throws StorageException if an inventory file cannot be read
     */
    public InventoryService() throws StorageException {
        for (Branch branch : Branch.values()) {
            Map<String, Product> stockOfBranch = new LinkedHashMap<>();
            for (Product product : repositoryOf(branch).loadAll()) {
                stockOfBranch.put(product.getProductId(), product);
            }
            stockByBranch.put(branch, stockOfBranch);
        }
    }

    /**
     * Returns the whole stock of one branch.
     * <p>
     * The products are returned as <b>copies</b>. A client must never hold a
     * reference to the object the server is about to change, and a copy also
     * makes sure the quantity the client displays is the one that was true at
     * the moment of the request.
     * </p>
     *
     * @param branch the branch whose stock is wanted
     * @return a list of copies of the products of that branch
     */
    public List<Product> getStockOf(Branch branch) {
        Map<String, Product> stockOfBranch = stockByBranch.get(branch);
        List<Product> stockCopy = new ArrayList<>();
        synchronized (stockOfBranch) {
            for (Product product : stockOfBranch.values()) {
                stockCopy.add(product.copy());
            }
        }
        return stockCopy;
    }

    /**
     * Finds one product in the stock of a branch.
     *
     * @param branch    the branch to look in
     * @param productId the product identifier to look for
     * @return the product object held by the server, not a copy
     * @throws EntityNotFoundException if that branch does not carry the product
     */
    public Product findProduct(Branch branch, String productId) throws EntityNotFoundException {
        Map<String, Product> stockOfBranch = stockByBranch.get(branch);
        Product product;
        synchronized (stockOfBranch) {
            product = stockOfBranch.get(productId);
        }
        if (product == null) {
            throw new EntityNotFoundException("Product", productId);
        }
        return product;
    }

    /**
     * Removes items from the stock of a branch as part of a sale.
     *
     * @param branch    the branch selling the items
     * @param productId the product being sold
     * @param quantity  how many items are being sold
     * @return a copy of the product as it looks after the sale
     * @throws EntityNotFoundException    if that branch does not carry the product
     * @throws InsufficientStockException if the branch holds fewer items than requested
     * @throws StorageException           if the inventory file cannot be written
     */
    public Product sell(Branch branch, String productId, int quantity)
            throws EntityNotFoundException, InsufficientStockException, StorageException {
        Product product = findProduct(branch, productId);

        // The whole read-check-write sequence happens inside one lock on this
        // single product. This is the race condition of the project: without the
        // lock, two sellers could both see "one left" and both sell it.
        synchronized (product) {
            product.decreaseQuantity(quantity);
        }

        persistBranch(branch);
        return product.copy();
    }

    /**
     * Adds items to the stock of a branch after a delivery from a supplier.
     *
     * @param branch    the branch receiving the delivery
     * @param productId the product being restocked
     * @param quantity  how many items arrived
     * @return a copy of the product as it looks after the delivery
     * @throws EntityNotFoundException if that branch does not carry the product
     * @throws StorageException        if the inventory file cannot be written
     */
    public Product restock(Branch branch, String productId, int quantity)
            throws EntityNotFoundException, StorageException {
        Product product = findProduct(branch, productId);

        synchronized (product) {
            product.increaseQuantity(quantity);
        }

        persistBranch(branch);
        return product.copy();
    }

    /**
     * Adds a brand new product to the catalogue of a branch.
     *
     * @param branch     the branch receiving the new product
     * @param newProduct the product to add
     * @return a copy of the product that was stored
     * @throws ChainStoreException if the branch already carries that product
     *                             identifier, or if the file cannot be written
     */
    public Product addProduct(Branch branch, Product newProduct) throws ChainStoreException {
        Map<String, Product> stockOfBranch = stockByBranch.get(branch);

        // The check and the insertion are locked together, so two managers adding
        // the same identifier at the same moment cannot both succeed.
        synchronized (stockOfBranch) {
            if (stockOfBranch.containsKey(newProduct.getProductId())) {
                throw new ChainStoreException("The branch already carries a product with the id "
                        + newProduct.getProductId());
            }
            stockOfBranch.put(newProduct.getProductId(), newProduct);
        }

        persistBranch(branch);
        return newProduct.copy();
    }

    /**
     * Writes the stock of one branch back to its own file.
     * <p>
     * Only the branch that changed is written. The other branch is not touched,
     * so a sale in Tel Aviv never makes an employee in Jerusalem wait for a file.
     * </p>
     *
     * @param branch the branch to write
     * @throws StorageException if the inventory file cannot be written
     */
    private void persistBranch(Branch branch) throws StorageException {
        Map<String, Product> stockOfBranch = stockByBranch.get(branch);
        List<Product> productsToSave;
        synchronized (stockOfBranch) {
            productsToSave = new ArrayList<>(stockOfBranch.values());
        }
        repositoryOf(branch).saveAll(productsToSave);
    }

    /**
     * Returns the shared repository of one branch.
     *
     * @param branch the branch whose repository is needed
     * @return the inventory repository of that branch
     */
    private InventoryRepository repositoryOf(Branch branch) {
        return ServerContext.getInstance().getInventoryRepository(branch);
    }
}
