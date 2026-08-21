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

/**
 * Opens and closes the session of this client against the server.
 * <p>
 * The controller sits between the screens and the network: {@code LoginFrame}
 * collects two text fields and calls {@link #login(String, String)}, and
 * everything about requests, responses and payload keys stays here. A screen
 * that knew how to build a {@link Request} would be much harder to change
 * later, and impossible to test without a window.
 * </p>
 * <p>
 * <b>Threading:</b> both methods block while they wait for the server, so they
 * must be called from a background thread and never from the Swing event
 * dispatch thread.
 * </p>
 */
public class LoginController {

    /** Configuration key holding the address of the server. */
    private static final String CONFIG_KEY_HOST = "server.host";

    /** Configuration key holding the port of the server. */
    private static final String CONFIG_KEY_PORT = "server.port";

    /** The address used when the configuration file is missing. */
    private static final String DEFAULT_HOST = "localhost";

    /** The port used when the configuration file is missing. */
    private static final int DEFAULT_PORT = 5000;

    /**
     * Connects if needed, then sends the credentials to the server.
     *
     * @param employeeNumber the employee number typed by the user
     * @param plainPassword  the password typed by the user
     * @return the authenticated employee, without any credentials attached
     * @throws ChainStoreException if the server cannot be reached, or if it
     *                             refused the login - a wrong password, or an
     *                             employee that is already connected elsewhere
     */
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
            // The server already decided why the login failed and wrote a message
            // for the user. The screen only has to display it.
            throw new ChainStoreException(response.getMessage());
        }

        Employee authenticatedEmployee = (Employee) response.getPayload(ProtocolKeys.EMPLOYEE);
        if (authenticatedEmployee == null) {
            throw new AuthenticationException();
        }

        session.setCurrentEmployee(authenticatedEmployee);
        return authenticatedEmployee;
    }

    /**
     * Closes the session on the server and forgets the employee locally.
     * <p>
     * The connection itself stays open, so the login screen can be used again
     * without reconnecting.
     * </p>
     *
     * @throws ChainStoreException if the server cannot be reached
     */
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
