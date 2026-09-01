package server.command;

import common.exception.ChainStoreException;
import common.protocol.Request;
import common.protocol.Response;
import server.core.ConnectedClient;

public interface Command {

    Response execute(Request request, ConnectedClient client) throws ChainStoreException;
}
