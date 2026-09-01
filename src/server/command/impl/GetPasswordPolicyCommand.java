package server.command.impl;

import common.exception.ChainStoreException;
import common.model.PasswordPolicy;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import server.command.Command;
import server.core.ConnectedClient;
import server.core.ServerContext;

public class GetPasswordPolicyCommand implements Command {

    @Override
    public Response execute(Request request, ConnectedClient client) throws ChainStoreException {
        PasswordPolicy policy = ServerContext.getInstance()
                .getEmployeeService()
                .getPasswordPolicy();

        return Response.success(request.getRequestId())
                .withPayload(ProtocolKeys.PASSWORD_POLICY, policy);
    }
}
