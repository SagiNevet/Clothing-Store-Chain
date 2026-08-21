package server.storage;

import common.exception.StorageException;
import common.model.Employee;

import java.io.File;
import java.util.List;

/**
 * Reads and writes the employee accounts, including their password salts and
 * hashes.
 * <p>
 * The class adds nothing to {@link FileRepository} except the name of the file
 * and a typed lookup helper. That is the whole benefit of the generic base
 * class: every new entity costs a few lines.
 * </p>
 */
public class EmployeeRepository extends FileRepository<Employee> {

    /**
     * Creates the repository over {@code data/employees.dat}.
     */
    public EmployeeRepository() {
        super(StoragePaths.EMPLOYEES_FILE);
    }

    /**
     * Creates the repository over an employees file inside a folder chosen by
     * the caller. Used by the unit tests, so that running them never touches
     * the real data of the system.
     *
     * @param directory the folder holding the employees file
     */
    public EmployeeRepository(File directory) {
        super(directory, StoragePaths.EMPLOYEES_FILE);
    }

    /**
     * Finds one employee account by its employee number.
     *
     * @param employeeNumber the employee number to look for
     * @return the matching employee, or {@code null} when no such account exists
     * @throws StorageException if the employees file cannot be read
     */
    public Employee findByEmployeeNumber(String employeeNumber) throws StorageException {
        List<Employee> allEmployees = loadAll();
        for (Employee currentEmployee : allEmployees) {
            if (currentEmployee.getEmployeeNumber().equals(employeeNumber)) {
                return currentEmployee;
            }
        }
        return null;
    }
}
