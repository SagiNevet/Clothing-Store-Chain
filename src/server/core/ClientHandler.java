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

public class ClientHandler implements Runnable {

    private final Socket clientSocket;

    private ConnectedClient connectedClient;

    private volatile boolean isHandlerRunning = true;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

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

            System.out.println("[Server] client disconnected: " + describeClient());
        } catch (IOException connectionFailure) {
            System.err.println("[Server] connection failure with " + describeClient()
                    + ": " + connectionFailure.getMessage());
        } catch (ClassNotFoundException unknownObject) {
            System.err.println("[Server] received an object of an unknown class from "
                    + describeClient() + ": " + unknownObject.getMessage());
        } finally {

            releaseConnection();
        }
    }

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

    private Response serveRequest(Request request) {
        try {
            verifyAccess(request);
            Command command = CommandFactory.commandFor(request.getActionType());
            return executeCommand(request, command);

        } catch (PermissionDeniedException permissionFailure) {

            LogManager.getInstance().logEmployeeAction(connectedClient.getEmployeeNumber(),
                    connectedClient.getBranch(), "PERMISSION_DENIED",
                    permissionFailure.getAttemptedAction());
            return Response.failure(request.getRequestId(), permissionFailure.getMessage());

        } catch (ChainStoreException businessFailure) {

            return Response.failure(request.getRequestId(), businessFailure.getMessage());

        } catch (Exception unexpectedFailure) {

            System.err.println("[Server] unexpected failure while serving "
                    + request + ": " + unexpectedFailure);
            unexpectedFailure.printStackTrace();
            return Response.failure(request.getRequestId(),
                    "The server failed to perform the action. Please try again.");
        }
    }

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

    private void releaseConnection() {
        isHandlerRunning = false;
        if (connectedClient == null) {
            return;
        }

        ChatService.getInstance().handleDisconnection(connectedClient.getEmployee());
        SessionManager.getInstance()
                .closeSession(connectedClient.getEmployeeNumber(), connectedClient);
        ClientRegistry.getInstance().remove(connectedClient);
        connectedClient.close();
        System.out.println("[Server] connection released (open connections: "
                + ClientRegistry.getInstance().getConnectionCount() + ")");
    }

    public void stopHandler() {
        this.isHandlerRunning = false;
    }

    private String describeClient() {
        return connectedClient == null
                ? clientSocket.getRemoteSocketAddress().toString()
                : connectedClient.toString();
    }
}
