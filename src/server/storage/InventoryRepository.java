package server.storage;

import common.model.Branch;
import common.model.Product;

import java.io.File;

/**
 * Reads and writes the inventory of one single branch.
 * <p>
 * The requirement states that every branch keeps its own separate stock, so one
 * repository object is created per branch and each one points at its own file:
 * {@code data/inventory_TEL_AVIV.dat} and
 * {@code data/inventory_JERUSALEM.dat}. Keeping the branches in separate files
 * means a sale in Tel Aviv never touches the file of Jerusalem, so the two
 * branches can be served at the same time without waiting for one another.
 * </p>
 */
public class InventoryRepository extends FileRepository<Product> {

    /** The branch whose stock this repository holds. */
    private final Branch branch;

    /**
     * Creates the repository over the inventory file of one branch.
     *
     * @param branch the branch whose stock this repository holds
     */
    public InventoryRepository(Branch branch) {
        super(StoragePaths.inventoryFileFor(branch));
        this.branch = branch;
    }

    /**
     * Creates the repository over an inventory file inside a folder chosen by
     * the caller. Used by the unit tests, so that running them never touches
     * the real data of the system.
     *
     * @param directory the folder holding the inventory file
     * @param branch    the branch whose stock this repository holds
     */
    public InventoryRepository(File directory, Branch branch) {
        super(directory, StoragePaths.inventoryFileFor(branch));
        this.branch = branch;
    }

    /**
     * Returns the branch whose stock this repository holds.
     *
     * @return the branch, never {@code null}
     */
    public Branch getBranch() {
        return branch;
    }
}
