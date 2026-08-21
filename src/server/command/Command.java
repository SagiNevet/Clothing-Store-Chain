package server.command;

import common.exception.ChainStoreException;
import common.protocol.Request;
import common.protocol.Response;
import server.core.ConnectedClient;

/**
 * One action the server knows how to perform, wrapped as an object.
 * <p>
 * This interface is the <b>Command</b> pattern of the project. Every request
 * that arrives from a client is turned into the matching command object, and
 * the handler simply calls {@link #execute(Request, ConnectedClient)} without
 * knowing what the action actually does.
 * </p>
 * <p>
 * <b>What it buys us:</b>
 * </p>
 * <ul>
 *   <li>The permission check, the logging and the error handling are written
 *       <b>once</b> in the handler, around this one call, instead of being
 *       repeated inside every action.</li>
 *   <li>Adding an action means adding one class and one line in the factory.
 *       Without the pattern the handler would grow into a {@code switch} with
 *       twenty branches and hundreds of lines.</li>
 *   <li>Because an action is an object, it can be handed to a thread pool as
 *       easily as it is executed directly - which is exactly what the parallel
 *       business operations of stage 5 will do.</li>
 * </ul>
 */
public interface Command {

    /**
     * Performs the action described by a request.
     *
     * @param request the request that arrived from the client
     * @param client  the connection the request arrived on, which also holds
     *                the employee that is logged in
     * @return the answer to send back to the client
     * @throws ChainStoreException if the action fails for a business reason,
     *                             such as missing stock or a refused permission
     */
    Response execute(Request request, ConnectedClient client) throws ChainStoreException;
}
