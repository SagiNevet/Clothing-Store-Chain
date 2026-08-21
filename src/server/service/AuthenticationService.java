package server.service;

import common.exception.AuthenticationException;
import common.exception.StorageException;
import common.model.Employee;
import common.util.PasswordHasher;
import server.core.ServerContext;
import server.storage.EmployeeRepository;

/**
 * Checks an employee number and a password against the employees file.
 * <p>
 * The service never compares passwords: it hashes the password that was typed
 * with the salt stored for that employee, and compares the two hashes. The
 * original password exists only inside the memory of this method, for the few
 * microseconds the check takes.
 * </p>
 */
public class AuthenticationService {

    /** The shared repository of employee accounts. */
    private final EmployeeRepository employeeRepository;

    /**
     * Creates the service over the shared employee repository.
     */
    public AuthenticationService() {
        this.employeeRepository = ServerContext.getInstance().getEmployeeRepository();
    }

    /**
     * Verifies a login attempt.
     * <p>
     * An unknown employee number and a wrong password produce the very same
     * exception with the very same message. That is deliberate: a different
     * message for each case would let somebody discover which employee numbers
     * exist by simply reading the error.
     * </p>
     *
     * @param employeeNumber the employee number typed in the login screen
     * @param plainPassword  the password typed in the login screen
     * @return the authenticated employee, with the credentials still attached
     * @throws AuthenticationException if the employee number is unknown or the
     *                                 password does not match
     * @throws StorageException        if the employees file cannot be read
     */
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
