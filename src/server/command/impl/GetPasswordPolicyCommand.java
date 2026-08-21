package server.command.impl;

import common.exception.ChainStoreException;
import common.model.PasswordPolicy;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import server.command.Command;
import server.core.ConnectedClient;
import server.core.ServerContext;

/**
 * Returns the password policy currently in force.
 * <p>
 * Any employee may ask for it, because the rules have to be displayed next to
 * the password field on the screen that creates an account. Only a shift
 * manager may change them.
 * </p>
 */
public class GetPasswordPolicyCommand implements Command {

    /**
     * {@inheritDoc}
     */
    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        PasswordPolicy policy = ServerContext.getInstance()
                .getEmployeeService()
                .getPasswordPolicy();

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.PASSWORD_POLICY, policy);
    }
}
