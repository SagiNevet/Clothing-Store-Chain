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

/**
 * Creates employee accounts and keeps the password policy of the system.
 * <p>
 * <b>This is the service the parallelism demonstration uses.</b> Registering an
 * employee touches the employees file and nothing else; selling a shirt touches
 * an inventory file, the sales file and the customers file. The two operations
 * share no data at all, which is exactly why they may run at the same moment -
 * and why the lecturer chose them as the example.
 * </p>
 * <p>
 * The password of a new account is checked against the current
 * {@link PasswordPolicy} and then hashed. The clear password exists only inside
 * this method, and only for as long as the hashing takes.
 * </p>
 */
public class EmployeeService {

    /**
     * The current password policy, kept in memory so that every new account does
     * not have to read the file. Declared {@code volatile} because one thread
     * may replace it while another is reading it.
     */
    private volatile PasswordPolicy passwordPolicy;

    /**
     * Loads the password policy from its file, or builds the default one.
     *
     * @throws StorageException if the policy file cannot be read
     */
    public EmployeeService() throws StorageException {
        this.passwordPolicy = policyRepository().loadPolicy();
    }

    /**
     * Returns every employee account, without any credentials attached.
     *
     * @return the employee list, safe to send to a client
     * @throws StorageException if the employees file cannot be read
     */
    public List<Employee> getAllEmployees() throws StorageException {
        List<Employee> safeCopies = new ArrayList<>();
        for (Employee employee : employeeRepository().loadAll()) {
            safeCopies.add(employee.withoutCredentials());
        }
        return safeCopies;
    }

    /**
     * Creates a new employee account.
     * <p>
     * The whole method is {@code synchronized}, and this is one of the few
     * places where locking the whole method is right: the check that the
     * employee number is free and the writing of the file must not be split by
     * another thread, or two managers could create the same employee number at
     * the same instant. The operation is rare and short, so nothing is lost by
     * serializing it.
     * </p>
     *
     * @param employeeNumber    the employee number, used as the login name
     * @param fullName          the full name of the employee
     * @param idNumber          the national identity number
     * @param phone             the phone number
     * @param bankAccountNumber the bank account number
     * @param branch            the branch the employee works in
     * @param role              the single role the employee holds
     * @param plainPassword     the password chosen for the account
     * @return the created account, without credentials
     * @throws InvalidPasswordPolicyException if the password breaks the policy
     * @throws ChainStoreException            if the employee number is taken or
     *                                        the file cannot be written
     */
    public synchronized Employee addEmployee(String employeeNumber, String fullName,
                                             String idNumber, String phone,
                                             String bankAccountNumber, Branch branch,
                                             Role role, String plainPassword)
            throws ChainStoreException {
        // Throws with the complete list of violations when the password is weak.
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

    /**
     * Returns the password policy currently in force.
     *
     * @return the password policy, never {@code null}
     */
    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    /**
     * Replaces the password policy of the system.
     * <p>
     * Existing accounts are not affected: their passwords were already hashed
     * and cannot be checked against the new rules. The policy applies to every
     * account created from now on.
     * </p>
     *
     * @param newPolicy the policy defined by the administrator
     * @throws StorageException if the policy file cannot be written
     */
    public synchronized void updatePasswordPolicy(PasswordPolicy newPolicy)
            throws StorageException {
        policyRepository().savePolicy(newPolicy);
        this.passwordPolicy = newPolicy;
    }

    /**
     * Returns the shared repository of employee accounts.
     *
     * @return the employee repository
     */
    private EmployeeRepository employeeRepository() {
        return ServerContext.getInstance().getEmployeeRepository();
    }

    /**
     * Returns the shared repository of the password policy.
     *
     * @return the password policy repository
     */
    private PasswordPolicyRepository policyRepository() {
        return ServerContext.getInstance().getPasswordPolicyRepository();
    }
}
