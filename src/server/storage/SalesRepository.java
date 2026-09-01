package server.storage;

import common.exception.StorageException;
import common.model.Sale;

import java.io.File;
import java.util.List;

public class SalesRepository extends FileRepository<Sale> {

    public SalesRepository() {
        super(StoragePaths.SALES_FILE);
    }

    public SalesRepository(File directory) {
        super(directory, StoragePaths.SALES_FILE);
    }

    public void append(Sale sale) throws StorageException {
        synchronized (this) {
            List<Sale> allSales = loadAll();
            allSales.add(sale);
            saveAll(allSales);
        }
    }
}
