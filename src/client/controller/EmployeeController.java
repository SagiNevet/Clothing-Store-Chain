package client.controller;

import common.exception.ChainStoreException;
import common.model.Branch;
import common.model.Employee;
import common.model.PasswordPolicy;
import common.model.Role;
import common.protocol.ActionType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;

import java.util.List;

/**
 * Turns what the employees screen wants into requests, and the answers back
 * into model objects.
 * <p>
 * Every action here is refused by the server for anybody who is not a shift
 * manager, so the screen that uses this controller is only built for that role.
 * </p>
 * <p>
 * <b>Threading:</b> every method blocks while it waits for the server, so all of
 * them must be called from a background thread.
 * </p>
 */
public class EmployeeController {

    /** The value sent in the credential fields of the carrier object. */
    private static final String NO_CREDENTIALS = "";

    /**
     * Fetches every employee account of the chain.
     *
     * @return the employee list, without credentials
     * @throws ChainStoreException if the server refused the request or could not
     *                             be reached
     */
    @SuppressWarnings("unchecked")
    public List<Employee> loadEmployees() throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.GET_EMPLOYEES));
        return (List<Employee>) response.getPayload(ProtocolKeys.EMPLOYEE_LIST);
    }

    /**
     * Creates a new employee account.
     * <p>
     * The password travels to the server in its own parameter and is hashed
     * there. It is never stored anywhere on the client.
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
     * @return the account that was created
     * @throws ChainStoreException if the password breaks the policy, the number
     *                             is taken, or the server cannot be reached
     */
    public Employee addEmployee(String employeeNumber, String fullName, String idNumber,
                                String phone, String bankAccountNumber, Branch branch,
                                Role role, String plainPassword) throws ChainStoreException {
        // The credentials of this carrier object are left empty on purpose: the
        // real password travels in its own parameter and is hashed by the server.
        Employee employeeDetails = new Employee(employeeNumber, fullName, idNumber, phone,
                bankAccountNumber, branch, role, NO_CREDENTIALS, NO_CREDENTIALS);

        Response response = sendAndVerify(newRequest(ActionType.ADD_EMPLOYEE)
                .withParameter(ProtocolKeys.EMPLOYEE, employeeDetails)
                .withParameter(ProtocolKeys.PASSWORD, plainPassword));
        return (Employee) response.getPayload(ProtocolKeys.EMPLOYEE);
    }

    /**
     * Fetches the password policy currently in force.
     *
     * @return the password policy
     * @throws ChainStoreException if the server cannot be reached
     */
    public PasswordPolicy loadPasswordPolicy() throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.GET_PASSWORD_POLICY));
        return (PasswordPolicy) response.getPayload(ProtocolKeys.PASSWORD_POLICY);
    }

    /**
     * Replaces the password policy of the system.
     *
     * @param newPolicy the policy defined by the administrator
     * @throws ChainStoreException if the role is not allowed or the server
     *                             cannot be reached
     */
    public void updatePasswordPolicy(PasswordPolicy newPolicy) throws ChainStoreException {
        sendAndVerify(newRequest(ActionType.UPDATE_PASSWORD_POLICY)
                .withParameter(ProtocolKeys.PASSWORD_POLICY, newPolicy));
    }

    /**
     * Builds a request already stamped with the employee and the branch of this
     * client.
     *
     * @param actionType the action to request
     * @return the request, ready for its parameters
     */
    private Request newRequest(ActionType actionType) {
        ClientSession session = ClientSession.getInstance();
        return new Request(actionType,
                session.getCurrentEmployee().getEmployeeNumber(),
                session.getBranch());
    }

    /**
     * Sends a request and turns a refusal into an exception.
     *
     * @param request the request to send
     * @return the successful answer
     * @throws ChainStoreException if the server refused the request or could not
     *                             be reached
     */
    private Response sendAndVerify(Request request) throws ChainStoreException {
        Response response = ClientSession.getInstance().getConnection().send(request);
        if (!response.isSuccess()) {
            throw new ChainStoreException(response.getMessage());
        }
        return response;
    }
}
