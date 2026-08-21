package server.storage;

import common.exception.StorageException;
import common.model.Sale;

import java.io.File;
import java.util.List;

/**
 * Reads and writes every completed sale of the chain.
 * <p>
 * Sales of both branches are kept in one file, because the reports have to
 * compare the branches with one another. Each {@link Sale} carries its own
 * branch, so a report can group the rows by branch, by product or by category
 * without any extra file.
 * </p>
 */
public class SalesRepository extends FileRepository<Sale> {

    /**
     * Creates the repository over {@code data/sales.dat}.
     */
    public SalesRepository() {
        super(StoragePaths.SALES_FILE);
    }

    /**
     * Creates the repository over a sales file inside a folder chosen by the
     * caller. Used by the unit tests, so that running them never touches the
     * real data of the system.
     *
     * @param directory the folder holding the sales file
     */
    public SalesRepository(File directory) {
        super(directory, StoragePaths.SALES_FILE);
    }

    /**
     * Adds one sale to the file, keeping every sale already stored.
     * <p>
     * The read and the write happen inside one {@code synchronized} block, so
     * two sales completed at the same instant in the two branches cannot
     * overwrite each other: without the lock both threads could read the same
     * list of ten sales, and each would write a list of eleven - losing one of
     * the two new sales.
     * </p>
     *
     * @param sale the completed sale to store
     * @throws StorageException if the sales file cannot be read or written
     */
    public void append(Sale sale) throws StorageException {
        synchronized (this) {
            List<Sale> allSales = loadAll();
            allSales.add(sale);
            saveAll(allSales);
        }
    }
}
