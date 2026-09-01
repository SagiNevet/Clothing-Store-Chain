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

public class EmployeeController {

    private static final String NO_CREDENTIALS = "";

    @SuppressWarnings("unchecked")
    public List<Employee> loadEmployees() throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.GET_EMPLOYEES));
        return (List<Employee>) response.getPayload(ProtocolKeys.EMPLOYEE_LIST);
    }

    public Employee addEmployee(String employeeNumber, String fullName, String idNumber,
                                String phone, String bankAccountNumber, Branch branch,
                                Role role, String plainPassword) throws ChainStoreException {

        Employee employeeDetails = new Employee(employeeNumber, fullName, idNumber, phone,
                bankAccountNumber, branch, role, NO_CREDENTIALS, NO_CREDENTIALS);

        Response response = sendAndVerify(newRequest(ActionType.ADD_EMPLOYEE)
                .withParameter(ProtocolKeys.EMPLOYEE, employeeDetails)
                .withParameter(ProtocolKeys.PASSWORD, plainPassword));
        return (Employee) response.getPayload(ProtocolKeys.EMPLOYEE);
    }

    public PasswordPolicy loadPasswordPolicy() throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.GET_PASSWORD_POLICY));
        return (PasswordPolicy) response.getPayload(ProtocolKeys.PASSWORD_POLICY);
    }

    public void updatePasswordPolicy(PasswordPolicy newPolicy) throws ChainStoreException {
        sendAndVerify(newRequest(ActionType.UPDATE_PASSWORD_POLICY)
                .withParameter(ProtocolKeys.PASSWORD_POLICY, newPolicy));
    }

    private Request newRequest(ActionType actionType) {
        ClientSession session = ClientSession.getInstance();
        return new Request(actionType,
                session.getCurrentEmployee().getEmployeeNumber(),
                session.getBranch());
    }

    private Response sendAndVerify(Request request) throws ChainStoreException {
        Response response = ClientSession.getInstance().getConnection().send(request);
        if (!response.isSuccess()) {
            throw new ChainStoreException(response.getMessage());
        }
        return response;
    }
}
