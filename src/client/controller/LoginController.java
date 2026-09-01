package client.controller;

import client.net.ServerConnection;
import common.exception.AuthenticationException;
import common.exception.ChainStoreException;
import common.model.Employee;
import common.protocol.ActionType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import common.util.AppConfig;

public class LoginController {

    private static final String CONFIG_KEY_HOST = "server.host";

    private static final String CONFIG_KEY_PORT = "server.port";

    private static final String DEFAULT_HOST = "localhost";

    private static final int DEFAULT_PORT = 5000;

    public Employee login(String employeeNumber, String plainPassword)
            throws ChainStoreException {
        ClientSession session = ClientSession.getInstance();
        ServerConnection connection = session.getConnection();

        AppConfig configuration = AppConfig.getInstance();
        connection.connect(configuration.getString(CONFIG_KEY_HOST, DEFAULT_HOST),
                configuration.getInt(CONFIG_KEY_PORT, DEFAULT_PORT));

        Request loginRequest = new Request(ActionType.LOGIN)
                .withParameter(ProtocolKeys.EMPLOYEE_NUMBER, employeeNumber)
                .withParameter(ProtocolKeys.PASSWORD, plainPassword);

        Response response = connection.send(loginRequest);
        if (!response.isSuccess()) {
            
            throw new ChainStoreException(response.getMessage());
        }

        Employee authenticatedEmployee = (Employee) response.getPayload(ProtocolKeys.EMPLOYEE);
        if (authenticatedEmployee == null) {
            throw new AuthenticationException();
        }

        session.setCurrentEmployee(authenticatedEmployee);
        return authenticatedEmployee;
    }

    public void logout() throws ChainStoreException {
        ClientSession session = ClientSession.getInstance();
        Employee employee = session.getCurrentEmployee();
        if (employee == null) {
            return;
        }

        Request logoutRequest = new Request(ActionType.LOGOUT,
                employee.getEmployeeNumber(), employee.getBranch());
        session.getConnection().send(logoutRequest);
        session.clearCurrentEmployee();
    }
}
