package common.model;

import java.io.Serializable;

public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String HIDDEN_CREDENTIAL = "";

    private final String employeeNumber;

    private String fullName;

    private String idNumber;

    private String phone;

    private String bankAccountNumber;

    private Branch branch;

    private Role role;

    private String passwordSalt;

    private String passwordHash;

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

    public Employee withoutCredentials() {
        return new Employee(employeeNumber, fullName, idNumber, phone, bankAccountNumber,
                branch, role, HIDDEN_CREDENTIAL, HIDDEN_CREDENTIAL);
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void updateCredentials(String passwordSalt, String passwordHash) {
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
    }

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

    @Override
    public int hashCode() {
        return employeeNumber.hashCode();
    }

    @Override
    public String toString() {
        return "Employee " + employeeNumber + " - " + fullName
                + " (" + role.getDisplayName() + ", " + branch.getDisplayName() + ")";
    }
}
