package server.command.impl;

import common.exception.ChainStoreException;
import common.model.Branch;
import common.protocol.Request;
import common.protocol.Response;
import server.command.Command;
import server.core.ConnectedClient;
import server.service.LogManager;
import server.service.SessionManager;

/**
 * Closes the session of the employee logged in on a connection.
 * <p>
 * The socket itself stays open, so the login screen can be used again on the
 * same running client. Releasing the session here is what allows the same
 * employee to log in from another computer straight away.
 * </p>
 */
public class LogoutCommand implements Command {

    /**
     * {@inheritDoc}
     */
    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        String employeeNumber = client.getEmployeeNumber();
        Branch branch = client.getBranch();

        SessionManager.getInstance().closeSession(employeeNumber, client);
        client.detachEmployee();

        LogManager.getInstance().logEmployeeAction(employeeNumber, branch,
                "LOGOUT", "Session closed by the employee");

        return Response.success(request.getRequestId());
    }
}
