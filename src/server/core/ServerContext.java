package server.core;

import common.exception.StorageException;
import common.model.Branch;
import server.service.BusinessTaskExecutor;
import server.service.CustomerService;
import server.service.EmployeeService;
import server.service.InventoryService;
import server.storage.CustomerRepository;
import server.storage.EmployeeRepository;
import server.storage.InventoryRepository;
import server.storage.PasswordPolicyRepository;
import server.storage.SalesRepository;
import server.storage.StoragePaths;

import java.util.EnumMap;
import java.util.Map;

/**
 * The single place that creates the repositories of the server.
 * <p>
 * <b>Why this class is necessary and not merely convenient:</b> the methods of
 * {@link server.storage.FileRepository} are {@code synchronized}, and a
 * {@code synchronized} method locks <b>the object it is called on</b>. If every
 * command created its own {@code new EmployeeRepository()}, each thread would
 * lock a different object, the lock would protect nothing at all, and two
 * threads could write the same file at the same moment. Sharing one repository
 * object per file is what makes the locking real.
 * </p>
 * <p>
 * The class is a <b>Singleton</b> built exactly like {@link common.util.AppConfig}:
 * a {@code static final} field initialised when the class is loaded, which the
 * Java class loader performs once and in a thread safe manner.
 * </p>
 */
public final class ServerContext {

    /** The single instance, created when the class is first loaded. */
    private static final ServerContext INSTANCE = new ServerContext();

    /** The shared repository of employee accounts. */
    private final EmployeeRepository employeeRepository;

    /** The shared repository of the customer list of the whole chain. */
    private final CustomerRepository customerRepository;

    /** The shared repository of completed sales. */
    private final SalesRepository salesRepository;

    /** The shared repository of the password policy. */
    private final PasswordPolicyRepository passwordPolicyRepository;

    /** One shared inventory repository per branch, because the stock is per branch. */
    private final Map<Branch, InventoryRepository> inventoryRepositories;

    /**
     * The shared stock of every branch, held in memory.
     * <p>
     * Created by {@link #initializeServices()} and not by the constructor,
     * because of an ordering rule that is easy to get wrong: the service loads
     * the inventory files into memory when it is built, so it must be built
     * <b>after</b> {@link DataSeeder} has written those files. Building it in
     * the constructor would leave the server with an empty stock on the very
     * first run.
     * </p>
     */
    private InventoryService inventoryService;

    /** The shared customer list of the chain, held in memory. */
    private CustomerService customerService;

    /** The shared employee service, which also holds the password policy. */
    private EmployeeService employeeService;

    /**
     * The pool the business operations run in.
     * <p>
     * Recreated every time a server starts, because an executor that has been
     * shut down cannot be started again.
     * </p>
     */
    private BusinessTaskExecutor businessTaskExecutor;

    /**
     * Creates the repositories and makes sure the folders they need exist.
     * Private, so nobody can build a second set of repositories.
     */
    private ServerContext() {
        StoragePaths.createDirectoriesIfMissing();
        this.employeeRepository = new EmployeeRepository();
        this.customerRepository = new CustomerRepository();
        this.salesRepository = new SalesRepository();
        this.passwordPolicyRepository = new PasswordPolicyRepository();
        this.inventoryRepositories = new EnumMap<>(Branch.class);
        for (Branch branch : Branch.values()) {
            inventoryRepositories.put(branch, new InventoryRepository(branch));
        }
    }

    /**
     * Returns the single context instance.
     *
     * @return the singleton instance, never {@code null}
     */
    public static ServerContext getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the shared repository of employee accounts.
     *
     * @return the employee repository
     */
    public EmployeeRepository getEmployeeRepository() {
        return employeeRepository;
    }

    /**
     * Returns the shared repository of the customer list.
     *
     * @return the customer repository
     */
    public CustomerRepository getCustomerRepository() {
        return customerRepository;
    }

    /**
     * Returns the shared repository of completed sales.
     *
     * @return the sales repository
     */
    public SalesRepository getSalesRepository() {
        return salesRepository;
    }

    /**
     * Returns the shared repository of the password policy.
     *
     * @return the password policy repository
     */
    public PasswordPolicyRepository getPasswordPolicyRepository() {
        return passwordPolicyRepository;
    }

    /**
     * Returns the shared inventory repository of one branch.
     *
     * @param branch the branch whose stock is needed
     * @return the inventory repository of that branch, never {@code null}
     */
    public InventoryRepository getInventoryRepository(Branch branch) {
        return inventoryRepositories.get(branch);
    }

    /**
     * Builds the services that keep data in memory, after the data files exist.
     * <p>
     * Called once by {@link ChainServer#start()}, right after
     * {@link DataSeeder} has made sure the files are not empty. Calling it a
     * second time does nothing, so a test that starts a second server in the
     * same program does not reload everything.
     * </p>
     *
     * @throws StorageException if the inventory or customer files cannot be read
     */
    public synchronized void initializeServices() throws StorageException {
        // The thread pool is checked separately from the data services. The data
        // is loaded once and stays valid, but a pool that was shut down by a
        // previous server can never be used again and has to be replaced.
        if (businessTaskExecutor == null || businessTaskExecutor.isShutdown()) {
            businessTaskExecutor = new BusinessTaskExecutor();
        }
        if (inventoryService != null) {
            return;
        }
        inventoryService = new InventoryService();
        customerService = new CustomerService();
        employeeService = new EmployeeService();
    }

    /**
     * Returns the pool the business operations run in.
     *
     * @return the business task executor, never {@code null}
     * @throws IllegalStateException if the server has not been started yet
     */
    public synchronized BusinessTaskExecutor getBusinessTaskExecutor() {
        if (businessTaskExecutor == null) {
            throw new IllegalStateException(
                    "The services are not ready. ChainServer.start() must run first.");
        }
        return businessTaskExecutor;
    }

    /**
     * Stops the thread pool when a server shuts down.
     * <p>
     * The data services are deliberately left alone: they only hold data read
     * from the files, and a server started later can go on using them.
     * </p>
     */
    public synchronized void shutdownBusinessExecutor() {
        if (businessTaskExecutor != null) {
            businessTaskExecutor.shutdown();
        }
    }

    /**
     * Returns the shared stock service of the chain.
     *
     * @return the inventory service, never {@code null}
     * @throws IllegalStateException if the server has not been started yet
     */
    public InventoryService getInventoryService() {
        if (inventoryService == null) {
            throw new IllegalStateException(
                    "The services are not ready. ChainServer.start() must run first.");
        }
        return inventoryService;
    }

    /**
     * Returns the shared customer service of the chain.
     *
     * @return the customer service, never {@code null}
     * @throws IllegalStateException if the server has not been started yet
     */
    public CustomerService getCustomerService() {
        if (customerService == null) {
            throw new IllegalStateException(
                    "The services are not ready. ChainServer.start() must run first.");
        }
        return customerService;
    }

    /**
     * Returns the shared employee service of the chain.
     *
     * @return the employee service, never {@code null}
     * @throws IllegalStateException if the server has not been started yet
     */
    public EmployeeService getEmployeeService() {
        if (employeeService == null) {
            throw new IllegalStateException(
                    "The services are not ready. ChainServer.start() must run first.");
        }
        return employeeService;
    }
}
