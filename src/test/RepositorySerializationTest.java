package test;

import common.exception.StorageException;
import common.model.Branch;
import common.model.Customer;
import common.model.CustomerFactory;
import common.model.CustomerType;
import common.model.Employee;
import common.model.PasswordPolicy;
import common.model.Product;
import common.model.ProductCategory;
import common.model.Role;
import common.model.Sale;
import common.util.IdGenerator;
import common.util.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.storage.CustomerRepository;
import server.storage.EmployeeRepository;
import server.storage.InventoryRepository;
import server.storage.PasswordPolicyRepository;
import server.storage.SalesRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that every kind of object survives a trip to a file and back.
 * <p>
 * Every test writes into a temporary folder created by JUnit, so running the
 * tests can never damage the real data of the system. The folder is deleted
 * automatically when the test finishes.
 * </p>
 */
public class RepositorySerializationTest {

    /** The tolerance used when comparing money amounts held in a double. */
    private static final double MONEY_TOLERANCE = 0.001;

    /** A temporary folder created and deleted by JUnit for each test. */
    @TempDir
    File temporaryDataFolder;

    @Test
    @DisplayName("Reading a file that does not exist yet returns an empty list")
    public void missingFileProducesAnEmptyList() throws StorageException {
        CustomerRepository repository = new CustomerRepository(temporaryDataFolder);

        List<Customer> customers = repository.loadAll();

        assertNotNull(customers);
        assertTrue(customers.isEmpty());
    }

    @Test
    @DisplayName("Each customer comes back from the file as the exact subclass it was saved as")
    public void customerSubclassesSurviveSerialization() throws StorageException {
        CustomerRepository repository = new CustomerRepository(temporaryDataFolder);
        List<Customer> customersToSave = new ArrayList<>();
        customersToSave.add(CustomerFactory.create(CustomerType.NEW, "111", "Dana", "050-1111111"));
        customersToSave.add(CustomerFactory.create(
                CustomerType.RETURNING, "222", "Yossi", "050-2222222", 2, 300.0));
        customersToSave.add(CustomerFactory.create(
                CustomerType.VIP, "333", "Rina", "050-3333333", 7, 1500.0));

        repository.saveAll(customersToSave);
        List<Customer> loadedCustomers = repository.loadAll();

        assertEquals(3, loadedCustomers.size());
        assertEquals(CustomerType.NEW, loadedCustomers.get(0).getCustomerType());
        assertEquals(CustomerType.RETURNING, loadedCustomers.get(1).getCustomerType());
        assertEquals(CustomerType.VIP, loadedCustomers.get(2).getCustomerType());
    }

    @Test
    @DisplayName("A customer loaded from a file still calculates its own purchase plan")
    public void loadedCustomerKeepsItsPurchasePlan() throws StorageException {
        CustomerRepository repository = new CustomerRepository(temporaryDataFolder);
        List<Customer> customersToSave = new ArrayList<>();
        customersToSave.add(CustomerFactory.create(
                CustomerType.VIP, "333", "Rina", "050-3333333", 7, 1500.0));
        repository.saveAll(customersToSave);

        Customer loadedCustomer = repository.loadAll().get(0);

        // This is the assertion that proves the polymorphism survives a restart
        // of the server: the object was rebuilt from bytes and still runs the
        // VIP purchase plan, 15 percent off, without anybody telling it to.
        assertInstanceOf(Customer.class, loadedCustomer);
        assertEquals(170.0, loadedCustomer.calculateFinalPrice(100.0, 2), MONEY_TOLERANCE);
    }

    @Test
    @DisplayName("An employee survives serialization together with the password hash")
    public void employeeSurvivesSerialization() throws StorageException {
        EmployeeRepository repository = new EmployeeRepository(temporaryDataFolder);
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash("Shirt2026", salt);
        Employee shiftManager = new Employee("1001", "Maya Shir", "300000001", "050-1000001",
                "12-345-678900", Branch.TEL_AVIV, Role.SHIFT_MANAGER, salt, hash);
        List<Employee> employeesToSave = new ArrayList<>();
        employeesToSave.add(shiftManager);

        repository.saveAll(employeesToSave);
        Employee loadedEmployee = repository.findByEmployeeNumber("1001");

        assertNotNull(loadedEmployee);
        assertEquals("Maya Shir", loadedEmployee.getFullName());
        assertEquals(Branch.TEL_AVIV, loadedEmployee.getBranch());
        assertEquals(Role.SHIFT_MANAGER, loadedEmployee.getRole());
        assertTrue(PasswordHasher.matches("Shirt2026",
                loadedEmployee.getPasswordSalt(), loadedEmployee.getPasswordHash()));
    }

    @Test
    @DisplayName("Looking for an employee number that does not exist returns null")
    public void unknownEmployeeNumberReturnsNull() throws StorageException {
        EmployeeRepository repository = new EmployeeRepository(temporaryDataFolder);
        repository.saveAll(new ArrayList<>());

        assertNull(repository.findByEmployeeNumber("9999"));
    }

    @Test
    @DisplayName("The copy sent to a client carries no password hash")
    public void copyForTheClientHasNoCredentials() {
        String salt = PasswordHasher.generateSalt();
        Employee cashier = new Employee("1002", "Ron Levi", "300000002", "050-1000002",
                "12-345-678901", Branch.JERUSALEM, Role.CASHIER,
                salt, PasswordHasher.hash("Shirt2026", salt));

        Employee safeCopy = cashier.withoutCredentials();

        assertEquals("Ron Levi", safeCopy.getFullName());
        assertEquals("", safeCopy.getPasswordHash());
        assertEquals("", safeCopy.getPasswordSalt());
    }

    @Test
    @DisplayName("Each branch keeps its own inventory file")
    public void eachBranchHasItsOwnInventoryFile() throws StorageException {
        InventoryRepository telAvivRepository =
                new InventoryRepository(temporaryDataFolder, Branch.TEL_AVIV);
        InventoryRepository jerusalemRepository =
                new InventoryRepository(temporaryDataFolder, Branch.JERUSALEM);

        List<Product> telAvivStock = new ArrayList<>();
        telAvivStock.add(new Product("P-100", "Blue Shirt", ProductCategory.SHIRTS, 89.90, 10));
        telAvivRepository.saveAll(telAvivStock);

        List<Product> jerusalemStock = new ArrayList<>();
        jerusalemStock.add(new Product("P-100", "Blue Shirt", ProductCategory.SHIRTS, 89.90, 3));
        jerusalemStock.add(new Product("P-200", "Black Pants", ProductCategory.PANTS, 149.90, 5));
        jerusalemRepository.saveAll(jerusalemStock);

        // The same catalogue product exists in both branches with a different
        // quantity, which is exactly what a separate file per branch means.
        assertEquals(1, telAvivRepository.loadAll().size());
        assertEquals(10, telAvivRepository.loadAll().get(0).getQuantity());
        assertEquals(2, jerusalemRepository.loadAll().size());
        assertEquals(3, jerusalemRepository.loadAll().get(0).getQuantity());
    }

    @Test
    @DisplayName("Appending a sale keeps every sale already stored")
    public void appendingASaleKeepsTheEarlierOnes() throws StorageException {
        SalesRepository repository = new SalesRepository(temporaryDataFolder);
        Product shirt = new Product("P-100", "Blue Shirt", ProductCategory.SHIRTS, 100.0, 10);

        repository.append(new Sale(IdGenerator.nextSaleId(), Branch.TEL_AVIV, "1001",
                "111", CustomerType.NEW, shirt, 1, 90.0));
        repository.append(new Sale(IdGenerator.nextSaleId(), Branch.JERUSALEM, "1002",
                "222", CustomerType.VIP, shirt, 2, 170.0));

        List<Sale> allSales = repository.loadAll();

        assertEquals(2, allSales.size());
        assertEquals(Branch.TEL_AVIV, allSales.get(0).getBranch());
        assertEquals(10.0, allSales.get(0).getDiscountAmount(), MONEY_TOLERANCE);
        assertEquals(Branch.JERUSALEM, allSales.get(1).getBranch());
        assertEquals(30.0, allSales.get(1).getDiscountAmount(), MONEY_TOLERANCE);
    }

    @Test
    @DisplayName("The default policy is returned before an administrator saved one")
    public void defaultPolicyIsReturnedWhenNoFileExists() throws StorageException {
        PasswordPolicyRepository repository = new PasswordPolicyRepository(temporaryDataFolder);

        PasswordPolicy policy = repository.loadPolicy();

        assertEquals(8, policy.getMinimumLength());
        assertTrue(policy.isDigitRequired());
    }

    @Test
    @DisplayName("A policy saved by the administrator is read back")
    public void savedPolicyIsReadBack() throws StorageException {
        PasswordPolicyRepository repository = new PasswordPolicyRepository(temporaryDataFolder);
        PasswordPolicy strictPolicy = new PasswordPolicy(12, true, true, true, true);

        repository.savePolicy(strictPolicy);
        PasswordPolicy loadedPolicy = repository.loadPolicy();

        assertEquals(12, loadedPolicy.getMinimumLength());
        assertTrue(loadedPolicy.isSpecialCharacterRequired());
    }

    @Test
    @DisplayName("Saving replaces the file rather than adding to it")
    public void savingReplacesTheWholeFile() throws StorageException {
        CustomerRepository repository = new CustomerRepository(temporaryDataFolder);
        List<Customer> firstList = new ArrayList<>();
        firstList.add(CustomerFactory.createNewCustomer("111", "Dana", "050-1111111"));
        firstList.add(CustomerFactory.createNewCustomer("222", "Yossi", "050-2222222"));
        repository.saveAll(firstList);

        List<Customer> secondList = new ArrayList<>();
        secondList.add(CustomerFactory.createNewCustomer("333", "Rina", "050-3333333"));
        repository.saveAll(secondList);

        assertEquals(1, repository.loadAll().size());
        assertEquals("333", repository.loadAll().get(0).getIdNumber());
    }
}
