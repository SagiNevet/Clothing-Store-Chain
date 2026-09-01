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

public class InventoryService {

    private final Map<Branch, Map<String, Product>> stockByBranch = new EnumMap<>(Branch.class);

    public InventoryService() throws StorageException {
        for (Branch branch : Branch.values()) {
            Map<String, Product> stockOfBranch = new LinkedHashMap<>();
            for (Product product : repositoryOf(branch).loadAll()) {
                stockOfBranch.put(product.getProductId(), product);
            }
            stockByBranch.put(branch, stockOfBranch);
        }
    }

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

    public Product sell(Branch branch, String productId, int quantity)
            throws EntityNotFoundException, InsufficientStockException, StorageException {
        Product product = findProduct(branch, productId);

        synchronized (product) {
            product.decreaseQuantity(quantity);
        }

        persistBranch(branch);
        return product.copy();
    }

    public Product restock(Branch branch, String productId, int quantity)
            throws EntityNotFoundException, StorageException {
        Product product = findProduct(branch, productId);

        synchronized (product) {
            product.increaseQuantity(quantity);
        }

        persistBranch(branch);
        return product.copy();
    }

    public Product addProduct(Branch branch, Product newProduct) throws ChainStoreException {
        Map<String, Product> stockOfBranch = stockByBranch.get(branch);

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

    private void persistBranch(Branch branch) throws StorageException {
        Map<String, Product> stockOfBranch = stockByBranch.get(branch);
        List<Product> productsToSave;
        synchronized (stockOfBranch) {
            productsToSave = new ArrayList<>(stockOfBranch.values());
        }
        repositoryOf(branch).saveAll(productsToSave);
    }

    private InventoryRepository repositoryOf(Branch branch) {
        return ServerContext.getInstance().getInventoryRepository(branch);
    }
}
