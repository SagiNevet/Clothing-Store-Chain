package server.storage;

import common.model.Customer;

import java.io.File;

/**
 * Reads and writes the customer list, which is shared by the whole chain and
 * therefore kept in one single file rather than one file per branch.
 * <p>
 * The repository is typed on the abstract {@link Customer} class, so the same
 * file holds a mixture of {@code NewCustomer}, {@code ReturningCustomer} and
 * {@code VipCustomer} objects. Java serialization writes the real class name of
 * every object, which is what makes a customer come back from the file as the
 * exact subclass it was saved as - and that is what keeps the polymorphic
 * price calculation working after a restart.
 * </p>
 */
public class CustomerRepository extends FileRepository<Customer> {

    /**
     * Creates the repository over {@code data/customers.dat}.
     */
    public CustomerRepository() {
        super(StoragePaths.CUSTOMERS_FILE);
    }

    /**
     * Creates the repository over a customers file inside a folder chosen by
     * the caller. Used by the unit tests, so that running them never touches
     * the real data of the system.
     *
     * @param directory the folder holding the customers file
     */
    public CustomerRepository(File directory) {
        super(directory, StoragePaths.CUSTOMERS_FILE);
    }
}
