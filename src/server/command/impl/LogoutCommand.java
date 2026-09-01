package server.command.impl;

import common.exception.ChainStoreException;
import common.model.Branch;
import common.protocol.Request;
import common.protocol.Response;
import server.command.Command;
import server.core.ConnectedClient;
import server.service.LogManager;
import server.service.SessionManager;

public class LogoutCommand implements Command {

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
