package server.storage;

import common.model.Branch;
import common.model.Product;

import java.io.File;

public class InventoryRepository extends FileRepository<Product> {

    private final Branch branch;

    public InventoryRepository(Branch branch) {
        super(StoragePaths.inventoryFileFor(branch));
        this.branch = branch;
    }

    public InventoryRepository(File directory, Branch branch) {
        super(directory, StoragePaths.inventoryFileFor(branch));
        this.branch = branch;
    }

    public Branch getBranch() {
        return branch;
    }
}
