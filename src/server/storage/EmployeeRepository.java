package server.storage;

import common.exception.StorageException;
import common.model.Employee;

import java.io.File;
import java.util.List;

public class EmployeeRepository extends FileRepository<Employee> {

    public EmployeeRepository() {
        super(StoragePaths.EMPLOYEES_FILE);
    }

    public EmployeeRepository(File directory) {
        super(directory, StoragePaths.EMPLOYEES_FILE);
    }

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
