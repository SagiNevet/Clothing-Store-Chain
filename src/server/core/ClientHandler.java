package server.core;

import common.exception.ChainStoreException;
import common.exception.PermissionDeniedException;
import common.model.Employee;
import common.protocol.ActionType;
import common.protocol.Request;
import common.protocol.Response;
import server.command.Command;
import server.chat.ChatService;
import server.command.CommandFactory;
import server.service.LogManager;
import server.service.SessionManager;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Serves one client, on a thread of its own, for as long as that client stays
 * connected.
 * <p>
 * This is the "thread per client" requirement. The accept loop of
 * {@link ChainServer} creates one of these for every socket it accepts and
 * starts it on a new thread, so a slow request from one branch can never block
 * the other branch.
 * </p>
 * <p>
 * <b>The life of the thread:</b> it opens the two object streams, registers the
 * connection, and then loops reading one {@link Request} after another until
 * the client disconnects. Every request is turned into a {@link Command} by the
 * factory and executed, and the answer is written back on the same connection.
 * </p>
 * <p>
 * <b>Stopping:</b> the loop is controlled by the {@code volatile} field
 * {@link #isHandlerRunning} and by the state of the socket. The deprecated
 * {@code Thread.stop()} is never used: it kills a thread in the middle of
 * whatever it was doing, which could leave a data file half written or a lock
 * held forever.
 * </p>
 */
public class ClientHandler implements Runnable {

    /** The socket accepted for this client. */
    private final Socket clientSocket;

    /** The connection object, created once the streams are open. */
    private ConnectedClient connectedClient;

    /**
     * Whether this handler should keep reading requests. Declared
     * {@code volatile} so that a change made by another thread is seen
     * immediately by the reading loop, instead of being hidden in the cache of
     * a processor core.
     */
    private volatile boolean isHandlerRunning = true;

    /**
     * Creates a handler for one accepted socket.
     *
     * @param clientSocket the socket returned by {@code accept()}
     */
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * Runs the request loop of this client until the connection is closed.
     */
    @Override
    public void run() {
        try {
            connectedClient = new ConnectedClient(clientSocket);
            ClientRegistry.getInstance().add(connectedClient);
            System.out.println("[Server] client connected: " + connectedClient
                    + " (open connections: "
                    + ClientRegistry.getInstance().getConnectionCount() + ")");

            readRequestsUntilDisconnected();

        } catch (EOFException | SocketException expectedDisconnection) {
            // Both of these mean the client closed its side of the connection.
            // That is the normal way a session ends, not a failure worth a stack trace.
            System.out.println("[Server] client disconnected: " + describeClient());
        } catch (IOException connectionFailure) {
            System.err.println("[Server] connection failure with " + describeClient()
                    + ": " + connectionFailure.getMessage());
        } catch (ClassNotFoundException unknownObject) {
            System.err.println("[Server] received an object of an unknown class from "
                    + describeClient() + ": " + unknownObject.getMessage());
        } finally {
            // Reached whatever happened above: a normal disconnection, a crash of
            // the client, or a failure of the server. The session must be released
            // and the socket must be closed in every one of those cases, which is
            // exactly what a finally block is for.
            releaseConnection();
        }
    }

    /**
     * Reads and serves one request after another until the client disconnects.
     *
     * @throws IOException            if the connection fails or is closed
     * @throws ClassNotFoundException if an object of an unknown class arrives
     */
    private void readRequestsUntilDisconnected() throws IOException, ClassNotFoundException {
        while (isHandlerRunning && !clientSocket.isClosed()) {
            Object incomingObject = connectedClient.receive();
            if (!(incomingObject instanceof Request)) {
                System.err.println("[Server] ignoring an object that is not a Request: "
                        + incomingObject);
                continue;
            }
            Request request = (Request) incomingObject;
            Response response = serveRequest(request);
            connectedClient.sendResponse(response);
        }
    }

    /**
     * Executes one request and turns any failure into a response the client can
     * display.
     * <p>
     * <b>This method is where the exception policy of the project becomes
     * visible.</b> The {@code try} block holds nothing but the business flow -
     * check the permission, find the command, run it. The {@code catch} blocks
     * hold nothing but the handling, and they are ordered from the most specific
     * to the most general, with {@code Exception} last as a safety net. A single
     * broken request can therefore never bring the whole client thread down.
     * </p>
     *
     * @param request the request that arrived from the client
     * @return the answer to send back, successful or failed
     */
    private Response serveRequest(Request request) {
        try {
            verifyAccess(request);
            Command command = CommandFactory.commandFor(request.getActionType());
            return executeCommand(request, command);

        } catch (PermissionDeniedException permissionFailure) {
            // Worth a log line of its own: somebody asked for something their role
            // does not allow, which is exactly the kind of event a manager wants to see.
            LogManager.getInstance().logEmployeeAction(connectedClient.getEmployeeNumber(),
                    connectedClient.getBranch(), "PERMISSION_DENIED",
                    permissionFailure.getAttemptedAction());
            return Response.failure(request.getRequestId(), permissionFailure.getMessage());

        } catch (ChainStoreException businessFailure) {
            // Every expected business failure of the system: wrong password, a
            // duplicate login, missing stock. The client shows the message to the user.
            return Response.failure(request.getRequestId(), businessFailure.getMessage());

        } catch (Exception unexpectedFailure) {
            // The safety net. A bug in one command must not kill the thread that
            // serves this client, so the failure is printed for the developers and
            // reported to the user in a general sentence.
            System.err.println("[Server] unexpected failure while serving "
                    + request + ": " + unexpectedFailure);
            unexpectedFailure.printStackTrace();
            return Response.failure(request.getRequestId(),
                    "The server failed to perform the action. Please try again.");
        }
    }

    /**
     * Runs one command, either directly or inside the pool of business threads.
     * <p>
     * <b>This is where the fixed thread pool of the project is used.</b> A
     * business operation - a sale, a delivery, a new employee - is handed to
     * the business thread pool, which runs at most a fixed number of them at
     * the same moment. Two operations that have nothing in common, such as
     * registering an employee and selling a shirt, therefore run on two
     * different threads at the same instant; the console prints both thread
     * names and their overlapping timestamps.
     * </p>
     * <p>
     * <b>Why logging in and out stay outside the pool:</b> they are session
     * operations, not business ones. If they went through the pool, a burst of
     * slow sales could fill every thread and leave an employee unable even to
     * log in - and worse, a logout could not release a session while the pool
     * was busy. Keeping them out costs nothing and removes that risk entirely.
     * </p>
     *
     * @param request the request that arrived from the client
     * @param command the command that performs it
     * @return the answer produced by the command
     * @throws ChainStoreException if the command failed for a business reason
     */
    private Response executeCommand(Request request, Command command) throws ChainStoreException {
        ActionType actionType = request.getActionType();
        if (actionType == ActionType.LOGIN || actionType == ActionType.LOGOUT) {
            return command.execute(request, connectedClient);
        }

        String taskDescription = actionType + " for employee "
                + connectedClient.getEmployeeNumber();
        return ServerContext.getInstance().getBusinessTaskExecutor()
                .runAndWait(taskDescription, () -> command.execute(request, connectedClient));
    }

    /**
     * Checks that the request may be performed on this connection.
     * <p>
     * Two rules are enforced here, in one place, for every action of the
     * protocol: a user must be logged in, and an action marked
     * {@link ActionType#isShiftManagerOnly()} may only be performed by a shift
     * manager. Hiding a button in the client is a convenience for the user;
     * this check is the actual security, because a modified client could send
     * any request it likes.
     * </p>
     *
     * @param request the request to check
     * @throws ChainStoreException     if nobody is logged in on this connection
     * @throws PermissionDeniedException if the role of the employee is not allowed
     */
    private void verifyAccess(Request request) throws ChainStoreException {
        ActionType actionType = request.getActionType();
        if (actionType == ActionType.LOGIN) {
            return;
        }

        Employee employee = connectedClient.getEmployee();
        if (employee == null) {
            throw new ChainStoreException("You must log in before performing this action");
        }

        if (actionType.isShiftManagerOnly() && !employee.getRole().canManageEmployees()) {
            throw new PermissionDeniedException(employee.getRole(), actionType.name());
        }
    }

    /**
     * Releases everything this connection holds: the session, the entry in the
     * registry and the socket itself.
     * <p>
     * Called from the {@code finally} block, so it runs after a normal
     * disconnection and after a crash alike. Releasing the session is the most
     * important part: if it were skipped, an employee whose client crashed
     * would stay "logged in" forever and could never connect again.
     * </p>
     */
    private void releaseConnection() {
        isHandlerRunning = false;
        if (connectedClient == null) {
            return;
        }
        // A dropped connection must also close any conversation this employee was
        // holding, and free them in the chat queue. Otherwise the other side
        // would keep talking to nobody, and the employee would stay "busy" for
        // ever and could never be offered a conversation again.
        ChatService.getInstance().handleDisconnection(connectedClient.getEmployee());
        SessionManager.getInstance()
                .closeSession(connectedClient.getEmployeeNumber(), connectedClient);
        ClientRegistry.getInstance().remove(connectedClient);
        connectedClient.close();
        System.out.println("[Server] connection released (open connections: "
                + ClientRegistry.getInstance().getConnectionCount() + ")");
    }

    /**
     * Asks this handler to stop reading requests, used when the server shuts
     * down.
     */
    public void stopHandler() {
        this.isHandlerRunning = false;
    }

    /**
     * Builds a short description of the client for log messages.
     *
     * @return the description of the connection, safe to call before login
     */
    private String describeClient() {
        return connectedClient == null
                ? clientSocket.getRemoteSocketAddress().toString()
                : connectedClient.toString();
    }
}
