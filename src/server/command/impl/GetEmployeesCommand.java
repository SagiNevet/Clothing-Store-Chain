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

public class GetEmployeesCommand implements Command {

    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        List<Employee> employees = ServerContext.getInstance()
                .getEmployeeService()
                .getAllEmployees();

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.EMPLOYEE_LIST, new ArrayList<>(employees));
    }
}
