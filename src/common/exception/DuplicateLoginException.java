package common.exception;

public class DuplicateLoginException extends ChainStoreException {

    private static final long serialVersionUID = 1L;

    private final String employeeNumber;

    public DuplicateLoginException(String employeeNumber) {
        super("Employee " + employeeNumber + " is already logged in from another computer");
        this.employeeNumber = employeeNumber;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }
}
