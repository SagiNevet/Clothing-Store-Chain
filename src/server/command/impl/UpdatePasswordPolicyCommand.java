package server.command.impl;

import common.exception.ChainStoreException;
import common.model.PasswordPolicy;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import server.command.Command;
import server.core.ConnectedClient;
import server.core.ServerContext;
import server.service.LogManager;

/**
 * Replaces the password policy of the system.
 * <p>
 * Restricted to a shift manager by {@code ActionType.UPDATE_PASSWORD_POLICY}.
 * The change is written to its own file, so it survives a restart of the
 * server, and it applies to every account created from that moment on.
 * </p>
 */
public class UpdatePasswordPolicyCommand implements Command {

    /**
     * {@inheritDoc}
     */
    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        PasswordPolicy newPolicy =
                (PasswordPolicy) request.getParameter(ProtocolKeys.PASSWORD_POLICY);
        if (newPolicy == null) {
            throw new ChainStoreException("The request did not carry a password policy");
        }

        ServerContext.getInstance().getEmployeeService().updatePasswordPolicy(newPolicy);

        LogManager.getInstance().logEmployeeAction(client.getEmployeeNumber(), client.getBranch(),
                "UPDATE_PASSWORD_POLICY", "The policy is now: " + newPolicy.describe());

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.PASSWORD_POLICY, newPolicy);
    }
}
