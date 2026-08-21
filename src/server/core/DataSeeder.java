package server.core;

import common.exception.StorageException;
import common.model.Branch;
import common.model.Employee;
import common.model.Product;
import common.model.ProductCategory;
import common.model.Role;
import common.util.PasswordHasher;
import server.storage.EmployeeRepository;
import server.storage.InventoryRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Fills the data files with demonstration content the first time the server
 * runs on a clean machine.
 * <p>
 * Without this class the system would start with an empty employees file, and
 * nobody could ever log in - there would be no account to create the first
 * account with. Seeding also means every member of the group sees exactly the
 * same data on their own laptop during the defence.
 * </p>
 * <p>
 * Nothing is overwritten: each file is seeded only when it is still empty, so
 * restarting the server never touches data that has been created since.
 * </p>
 */
public final class DataSeeder {

    /** The password given to every demonstration account. */
    private static final String DEMO_PASSWORD = "Chain@2026";

    /**
     * Prevents instantiation. This class only exposes static methods.
     */
    private DataSeeder() {
    }

    /**
     * Creates the demonstration employees and inventory if they do not exist yet.
     *
     * @throws StorageException if a data file cannot be read or written
     */
    public static void seedIfEmpty() throws StorageException {
        seedEmployeesIfEmpty();
        for (Branch branch : Branch.values()) {
            seedInventoryIfEmpty(branch);
        }
    }

    /**
     * Creates one shift manager and one selling employee per branch.
     *
     * @throws StorageException if the employees file cannot be read or written
     */
    private static void seedEmployeesIfEmpty() throws StorageException {
        EmployeeRepository employeeRepository =
                ServerContext.getInstance().getEmployeeRepository();
        if (!employeeRepository.loadAll().isEmpty()) {
            return;
        }

        List<Employee> demonstrationEmployees = new ArrayList<>();
        demonstrationEmployees.add(createEmployee("1001", "Maya Shir", "300000001",
                "050-1000001", "12-345-100001", Branch.TEL_AVIV, Role.SHIFT_MANAGER));
        demonstrationEmployees.add(createEmployee("1002", "Ron Levi", "300000002",
                "050-1000002", "12-345-100002", Branch.TEL_AVIV, Role.CASHIER));
        demonstrationEmployees.add(createEmployee("2001", "Avi Dagan", "300000003",
                "050-2000001", "12-345-200001", Branch.JERUSALEM, Role.SHIFT_MANAGER));
        demonstrationEmployees.add(createEmployee("2002", "Tamar Ben Ari", "300000004",
                "050-2000002", "12-345-200002", Branch.JERUSALEM, Role.SELLER));

        employeeRepository.saveAll(demonstrationEmployees);
        System.out.println("[Seeder] created " + demonstrationEmployees.size()
                + " demonstration employees, all with the password " + DEMO_PASSWORD);
    }

    /**
     * Creates one employee account with a freshly hashed password.
     *
     * @param employeeNumber    the employee number, used as the login name
     * @param fullName          the full name of the employee
     * @param idNumber          the national identity number
     * @param phone             the phone number
     * @param bankAccountNumber the bank account number
     * @param branch            the branch the employee works in
     * @param role              the single role the employee holds
     * @return the employee account, ready to be saved
     */
    private static Employee createEmployee(String employeeNumber, String fullName,
                                           String idNumber, String phone,
                                           String bankAccountNumber, Branch branch, Role role) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(DEMO_PASSWORD, salt);
        return new Employee(employeeNumber, fullName, idNumber, phone,
                bankAccountNumber, branch, role, salt, hash);
    }

    /**
     * Creates a starting stock for one branch.
     * <p>
     * The two branches deliberately receive the same catalogue products with
     * different quantities, which makes the "separate inventory per branch"
     * requirement visible on screen during the demonstration.
     * </p>
     *
     * @param branch the branch to fill
     * @throws StorageException if the inventory file cannot be read or written
     */
    private static void seedInventoryIfEmpty(Branch branch) throws StorageException {
        InventoryRepository inventoryRepository =
                ServerContext.getInstance().getInventoryRepository(branch);
        if (!inventoryRepository.loadAll().isEmpty()) {
            return;
        }

        boolean isTelAviv = branch == Branch.TEL_AVIV;
        List<Product> startingStock = new ArrayList<>();
        startingStock.add(new Product("P-100", "Blue Cotton Shirt",
                ProductCategory.SHIRTS, 89.90, isTelAviv ? 25 : 12));
        startingStock.add(new Product("P-101", "White Linen Shirt",
                ProductCategory.SHIRTS, 129.90, isTelAviv ? 18 : 7));
        startingStock.add(new Product("P-200", "Black Slim Pants",
                ProductCategory.PANTS, 199.90, isTelAviv ? 14 : 20));
        startingStock.add(new Product("P-201", "Blue Jeans",
                ProductCategory.PANTS, 249.90, isTelAviv ? 10 : 15));
        startingStock.add(new Product("P-300", "Leather Sneakers",
                ProductCategory.SHOES, 349.90, isTelAviv ? 8 : 5));
        startingStock.add(new Product("P-400", "Leather Belt",
                ProductCategory.ACCESSORIES, 79.90, isTelAviv ? 30 : 22));

        inventoryRepository.saveAll(startingStock);
        System.out.println("[Seeder] created a starting stock of " + startingStock.size()
                + " products for " + branch.getDisplayName());
    }

    /**
     * Returns the password shared by every demonstration account, so the README
     * and the login screen can display it.
     *
     * @return the demonstration password
     */
    public static String getDemoPassword() {
        return DEMO_PASSWORD;
    }
}
