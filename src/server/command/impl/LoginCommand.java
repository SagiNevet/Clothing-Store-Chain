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

public class LoginCommand implements Command {

    private final AuthenticationService authenticationService = new AuthenticationService();

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
