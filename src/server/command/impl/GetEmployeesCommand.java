package server.command.impl;

import common.exception.ChainStoreException;
import common.model.Employee;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import server.command.Command;
import server.core.ConnectedClient;
import server.core.ServerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Returns every employee account of the chain, for the management screen.
 * <p>
 * The accounts arrive without their password salt and hash, because
 * {@code EmployeeService} strips the credentials before handing them out. Not
 * even a shift manager receives them: the hash is of no use to any screen, and
 * anything that never leaves the server cannot leak from a client.
 * </p>
 */
public class GetEmployeesCommand implements Command {

    /**
     * {@inheritDoc}
     */
    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        List<Employee> employees = ServerContext.getInstance()
                .getEmployeeService()
                .getAllEmployees();

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.EMPLOYEE_LIST, new ArrayList<>(employees));
    }
}
