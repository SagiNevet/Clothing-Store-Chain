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

public final class ServerContext {

    private static final ServerContext INSTANCE = new ServerContext();

    private final EmployeeRepository employeeRepository;

    private final CustomerRepository customerRepository;

    private final SalesRepository salesRepository;

    private final PasswordPolicyRepository passwordPolicyRepository;

    private final Map<Branch, InventoryRepository> inventoryRepositories;

    private InventoryService inventoryService;

    private CustomerService customerService;

    private EmployeeService employeeService;

    private BusinessTaskExecutor businessTaskExecutor;

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

    public static ServerContext getInstance() {
        return INSTANCE;
    }

    public EmployeeRepository getEmployeeRepository() {
        return employeeRepository;
    }

    public CustomerRepository getCustomerRepository() {
        return customerRepository;
    }

    public SalesRepository getSalesRepository() {
        return salesRepository;
    }

    public PasswordPolicyRepository getPasswordPolicyRepository() {
        return passwordPolicyRepository;
    }

    public InventoryRepository getInventoryRepository(Branch branch) {
        return inventoryRepositories.get(branch);
    }

    public synchronized void initializeServices() throws StorageException {

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

    public synchronized BusinessTaskExecutor getBusinessTaskExecutor() {
        if (businessTaskExecutor == null) {
            throw new IllegalStateException(
                    "The services are not ready. ChainServer.start() must run first.");
        }
        return businessTaskExecutor;
    }

    public synchronized void shutdownBusinessExecutor() {
        if (businessTaskExecutor != null) {
            businessTaskExecutor.shutdown();
        }
    }

    public InventoryService getInventoryService() {
        if (inventoryService == null) {
            throw new IllegalStateException(
                    "The services are not ready. ChainServer.start() must run first.");
        }
        return inventoryService;
    }

    public CustomerService getCustomerService() {
        if (customerService == null) {
            throw new IllegalStateException(
                    "The services are not ready. ChainServer.start() must run first.");
        }
        return customerService;
    }

    public EmployeeService getEmployeeService() {
        if (employeeService == null) {
            throw new IllegalStateException(
                    "The services are not ready. ChainServer.start() must run first.");
        }
        return employeeService;
    }
}
