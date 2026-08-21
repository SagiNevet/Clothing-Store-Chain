package server.command.impl;

import common.exception.ChainStoreException;
import common.model.Employee;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import server.command.Command;
import server.core.ConnectedClient;
import server.service.AuthenticationService;
import server.service.LogManager;
import server.service.SessionManager;

/**
 * Verifies an employee number and a password, and opens a session.
 * <p>
 * The command performs three steps in a fixed order, and each step can refuse
 * the login with its own exception:
 * </p>
 * <ol>
 *   <li>{@link AuthenticationService} checks the credentials and throws
 *       {@code AuthenticationException} when they are wrong.</li>
 *   <li>{@link SessionManager} opens the session and throws
 *       {@code DuplicateLoginException} when that employee is already connected
 *       from another computer.</li>
 *   <li>The employee is attached to the connection, so every later request on
 *       this socket already knows who is sending it.</li>
 * </ol>
 * <p>
 * Notice what is <b>not</b> here: there is no {@code if} checking whether the
 * password is empty, whether the employee exists, or whether the fields are
 * valid. Each of those situations throws its own exception from the place that
 * discovers it, and the handler catches them. This is the separation between
 * the logic and the error handling that the project requires.
 * </p>
 */
public class LoginCommand implements Command {

    /** The service that checks the credentials. */
    private final AuthenticationService authenticationService = new AuthenticationService();

    /**
     * {@inheritDoc}
     * <p>
     * On success the answer carries a copy of the employee <b>without</b> the
     * password salt and hash, so the credentials never travel over the network.
     * </p>
     */
    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        if (client.isLoggedIn()) {
            throw new ChainStoreException("This connection is already logged in as "
                    + client.getEmployeeNumber());
        }

        String employeeNumber = request.getString(ProtocolKeys.EMPLOYEE_NUMBER);
        String plainPassword = request.getString(ProtocolKeys.PASSWORD);

        Employee authenticatedEmployee =
                authenticationService.authenticate(employeeNumber, plainPassword);
        SessionManager.getInstance().openSession(authenticatedEmployee, client);
        client.attachEmployee(authenticatedEmployee);

        LogManager.getInstance().logEmployeeAction(employeeNumber,
                authenticatedEmployee.getBranch(), "LOGIN",
                "Logged in as " + authenticatedEmployee.getRole().getDisplayName());

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.EMPLOYEE, authenticatedEmployee.withoutCredentials());
    }
}
