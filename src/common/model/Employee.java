package common.model;

import java.io.Serializable;

/**
 * An employee of the chain, and the account used to log into the system.
 * <p>
 * An employee belongs to exactly one {@link Branch} and holds exactly one
 * {@link Role}. The employee number is the login name and the unique key of the
 * object, which is why {@link #equals(Object)} and {@link #hashCode()} are based
 * on it.
 * </p>
 * <p>
 * <b>Security note:</b> the object stores only the salt and the hash of the
 * password, never the password itself. Before an employee object is sent to a
 * client, the server calls {@link #withoutCredentials()} so that not even the
 * hash travels over the network.
 * </p>
 */
public class Employee implements Serializable {

    /** Serialization version. Kept explicit so old data files stay readable. */
    private static final long serialVersionUID = 1L;

    /** The value stored in the credential fields once they have been removed. */
    private static final String HIDDEN_CREDENTIAL = "";

    /** The employee number, which is both the login name and the unique key. */
    private final String employeeNumber;

    /** The full name of the employee. */
    private String fullName;

    /** The national identity number of the employee. */
    private String idNumber;

    /** The phone number of the employee. */
    private String phone;

    /** The bank account number used to pay the salary. */
    private String bankAccountNumber;

    /** The branch this employee works in. */
    private Branch branch;

    /** The single role this employee holds. */
    private Role role;

    /** The random salt used when hashing the password of this employee. */
    private String passwordSalt;

    /** The SHA-256 hash of the salted password. */
    private String passwordHash;

    /**
     * Creates an employee account.
     *
     * @param employeeNumber    the employee number, used as the login name
     * @param fullName          the full name of the employee
     * @param idNumber          the national identity number of the employee
     * @param phone             the phone number of the employee
     * @param bankAccountNumber the bank account number of the employee
     * @param branch            the branch the employee works in
     * @param role              the single role the employee holds
     * @param passwordSalt      the random salt used to hash the password
     * @param passwordHash      the hash of the salted password
     */
    public Employee(String employeeNumber, String fullName, String idNumber, String phone,
                    String bankAccountNumber, Branch branch, Role role,
                    String passwordSalt, String passwordHash) {
        this.employeeNumber = employeeNumber;
        this.fullName = fullName;
        this.idNumber = idNumber;
        this.phone = phone;
        this.bankAccountNumber = bankAccountNumber;
        this.branch = branch;
        this.role = role;
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
    }

    /**
     * Creates a copy of this employee whose password salt and hash are empty.
     * <p>
     * The server sends the result of this method to the clients. A client needs
     * the name, the branch and the role in order to build the right screen, but
     * it has no reason to ever receive the credentials.
     * </p>
     *
     * @return a new employee object holding the same details without credentials
     */
    public Employee withoutCredentials() {
        return new Employee(employeeNumber, fullName, idNumber, phone, bankAccountNumber,
                branch, role, HIDDEN_CREDENTIAL, HIDDEN_CREDENTIAL);
    }

    /**
     * Returns the employee number, which is the login name and the unique key.
     *
     * @return the employee number, never {@code null}
     */
    public String getEmployeeNumber() {
        return employeeNumber;
    }

    /**
     * Returns the full name of the employee.
     *
     * @return the full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Updates the full name of the employee.
     *
     * @param fullName the new full name
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Returns the national identity number of the employee.
     *
     * @return the identity number
     */
    public String getIdNumber() {
        return idNumber;
    }

    /**
     * Updates the national identity number of the employee.
     *
     * @param idNumber the new identity number
     */
    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    /**
     * Returns the phone number of the employee.
     *
     * @return the phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Updates the phone number of the employee.
     *
     * @param phone the new phone number
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Returns the bank account number of the employee.
     *
     * @return the bank account number
     */
    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    /**
     * Updates the bank account number of the employee.
     *
     * @param bankAccountNumber the new bank account number
     */
    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    /**
     * Returns the branch this employee works in.
     *
     * @return the branch, never {@code null}
     */
    public Branch getBranch() {
        return branch;
    }

    /**
     * Moves this employee to another branch.
     *
     * @param branch the new branch
     */
    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    /**
     * Returns the single role this employee holds.
     *
     * @return the role, never {@code null}
     */
    public Role getRole() {
        return role;
    }

    /**
     * Replaces the role of this employee. An employee always holds exactly one
     * role, so the previous role is dropped.
     *
     * @param role the new role
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * Returns the salt used when hashing the password of this employee.
     *
     * @return the Base64 salt, or an empty text on a copy created by
     *         {@link #withoutCredentials()}
     */
    public String getPasswordSalt() {
        return passwordSalt;
    }

    /**
     * Returns the hash of the salted password.
     *
     * @return the Base64 hash, or an empty text on a copy created by
     *         {@link #withoutCredentials()}
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Replaces the stored credentials, used when an administrator resets a
     * password.
     *
     * @param passwordSalt the new random salt
     * @param passwordHash the hash of the new salted password
     */
    public void updateCredentials(String passwordSalt, String passwordHash) {
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
    }

    /**
     * Compares employees by their employee number only, because that number is
     * the unique key of an account.
     *
     * @param other the object to compare with
     * @return {@code true} if both objects describe the same account
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Employee)) {
            return false;
        }
        Employee otherEmployee = (Employee) other;
        return employeeNumber.equals(otherEmployee.employeeNumber);
    }

    /**
     * Returns a hash code derived from the employee number.
     * <p>
     * Overridden together with {@link #equals(Object)} because employees are
     * kept inside hash based collections, such as the map of active sessions on
     * the server.
     * </p>
     *
     * @return the hash code of the employee number
     */
    @Override
    public int hashCode() {
        return employeeNumber.hashCode();
    }

    /**
     * Returns a readable description of the employee.
     * <p>
     * The credentials are deliberately left out, so that an accidental print of
     * an employee object can never leak a password hash into the console or
     * into a log file.
     * </p>
     *
     * @return a text describing the employee
     */
    @Override
    public String toString() {
        return "Employee " + employeeNumber + " - " + fullName
                + " (" + role.getDisplayName() + ", " + branch.getDisplayName() + ")";
    }
}
