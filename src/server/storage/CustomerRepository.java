package server.storage;

import common.model.Customer;

import java.io.File;

public class CustomerRepository extends FileRepository<Customer> {

    public CustomerRepository() {
        super(StoragePaths.CUSTOMERS_FILE);
    }

    public CustomerRepository(File directory) {
        super(directory, StoragePaths.CUSTOMERS_FILE);
    }
}
