package server.service;

import common.exception.AuthenticationException;
import common.exception.StorageException;
import common.model.Employee;
import common.util.PasswordHasher;
import server.core.ServerContext;
import server.storage.EmployeeRepository;

public class AuthenticationService {

    private final EmployeeRepository employeeRepository;

    public AuthenticationService() {
        this.employeeRepository = ServerContext.getInstance().getEmployeeRepository();
    }

    public Employee authenticate(String employeeNumber, String plainPassword)
            throws AuthenticationException, StorageException {
        Employee storedEmployee = employeeRepository.findByEmployeeNumber(employeeNumber);
        if (storedEmployee == null) {
            throw new AuthenticationException();
        }
        boolean passwordMatches = PasswordHasher.matches(plainPassword,
                storedEmployee.getPasswordSalt(), storedEmployee.getPasswordHash());
        if (!passwordMatches) {
            throw new AuthenticationException();
        }
        return storedEmployee;
    }
}
