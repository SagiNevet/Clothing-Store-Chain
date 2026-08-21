package server.command.impl;

import common.exception.ChainStoreException;
import common.model.Branch;
import common.model.Employee;
import common.model.Role;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import common.protocol.ServerEvent;
import server.command.Command;
import server.core.ConnectedClient;
import server.core.ServerContext;
import server.observer.EventPublisher;
import server.service.LogManager;

/**
 * Creates a new employee account.
 * <p>
 * This is one half of the parallelism demonstration the lecturer asked for. It
 * touches the employees file only, so it has nothing at all in common with a
 * sale - and the two can therefore run on two different threads of the business
 * pool at the very same moment.
 * </p>
 * <p>
 * Restricted to a shift manager by {@code ActionType.ADD_EMPLOYEE}.
 * </p>
 */
public class AddEmployeeCommand implements Command {

    /**
     * {@inheritDoc}
     */
    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        Employee employeeDetails = (Employee) request.getParameter(ProtocolKeys.EMPLOYEE);
        String plainPassword = request.getString(ProtocolKeys.PASSWORD);
        if (employeeDetails == null) {
            throw new ChainStoreException("The request did not carry the employee details");
        }

        Employee createdEmployee = ServerContext.getInstance().getEmployeeService().addEmployee(
                employeeDetails.getEmployeeNumber(),
                employeeDetails.getFullName(),
                employeeDetails.getIdNumber(),
                employeeDetails.getPhone(),
                employeeDetails.getBankAccountNumber(),
                employeeDetails.getBranch(),
                employeeDetails.getRole(),
                plainPassword);

        LogManager.getInstance().logEmployeeAction(client.getEmployeeNumber(), client.getBranch(),
                "ADD_EMPLOYEE", "Created account " + createdEmployee.getEmployeeNumber()
                        + " for " + createdEmployee.getFullName()
                        + " as " + createdEmployee.getRole().getDisplayName()
                        + " in " + createdEmployee.getBranch().getDisplayName());

        publishToManagers(createdEmployee);

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.EMPLOYEE, createdEmployee);
    }

    /**
     * Tells the shift managers of the chain that the employee list changed.
     *
     * @param createdEmployee the account that was just created
     */
    private void publishToManagers(Employee createdEmployee) {
        EventPublisher.getInstance().publishToAll(new ServerEvent(EventType.EMPLOYEES_UPDATED)
                .withPayload(ProtocolKeys.EMPLOYEE, createdEmployee));
    }
}
