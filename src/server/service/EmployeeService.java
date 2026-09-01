package server.service;

import common.exception.ChainStoreException;
import common.exception.InvalidPasswordPolicyException;
import common.exception.StorageException;
import common.model.Branch;
import common.model.Employee;
import common.model.PasswordPolicy;
import common.model.Role;
import common.util.PasswordHasher;
import server.core.ServerContext;
import server.storage.EmployeeRepository;
import server.storage.PasswordPolicyRepository;

import java.util.ArrayList;
import java.util.List;

public class EmployeeService {

    private volatile PasswordPolicy passwordPolicy;

    public EmployeeService() throws StorageException {
        this.passwordPolicy = policyRepository().loadPolicy();
    }

    public List<Employee> getAllEmployees() throws StorageException {
        List<Employee> safeCopies = new ArrayList<>();
        for (Employee employee : employeeRepository().loadAll()) {
            safeCopies.add(employee.withoutCredentials());
        }
        return safeCopies;
    }

    public synchronized Employee addEmployee(String employeeNumber, String fullName,
                                             String idNumber, String phone,
                                             String bankAccountNumber, Branch branch,
                                             Role role, String plainPassword)
            throws ChainStoreException {
        
        passwordPolicy.validate(plainPassword);

        EmployeeRepository repository = employeeRepository();
        List<Employee> allEmployees = repository.loadAll();
        for (Employee existingEmployee : allEmployees) {
            if (existingEmployee.getEmployeeNumber().equals(employeeNumber)) {
                throw new ChainStoreException("An employee with the number "
                        + employeeNumber + " already exists");
            }
        }

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(plainPassword, salt);
        Employee newEmployee = new Employee(employeeNumber, fullName, idNumber, phone,
                bankAccountNumber, branch, role, salt, hash);

        allEmployees.add(newEmployee);
        repository.saveAll(allEmployees);

        return newEmployee.withoutCredentials();
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public synchronized void updatePasswordPolicy(PasswordPolicy newPolicy)
            throws StorageException {
        policyRepository().savePolicy(newPolicy);
        this.passwordPolicy = newPolicy;
    }

    private EmployeeRepository employeeRepository() {
        return ServerContext.getInstance().getEmployeeRepository();
    }

    private PasswordPolicyRepository policyRepository() {
        return ServerContext.getInstance().getPasswordPolicyRepository();
    }
}
