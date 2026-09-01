package client.net;

import common.exception.ConnectionException;
import common.protocol.Request;
import common.protocol.Response;
import common.protocol.ServerEvent;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ServerConnection {

    private static final int RESPONSE_TIMEOUT_SECONDS = 15;

    private static final int MAILBOX_CAPACITY = 1;

    private final Map<Long, BlockingQueue<Response>> pendingResponses = new ConcurrentHashMap<>();

    private final ClientEventDispatcher eventDispatcher = new ClientEventDispatcher();

    private Socket socket;

    private ObjectOutputStream outputStream;

    private ObjectInputStream inputStream;

    private Thread listenerThread;

    private volatile boolean isListening;

    private volatile Runnable connectionLostHandler;

    public void connect(String host, int port) throws ConnectionException {
        if (isConnected()) {
            return;
        }
        try {
            socket = new Socket(host, port);

            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());

            isListening = true;
            listenerThread = new Thread(this::listenForIncomingObjects, "server-listener");
            
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("[Client] connected to " + host + ":" + port);

        } catch (IOException connectionFailure) {
            throw new ConnectionException("Could not connect to the server at "
                    + host + ":" + port + ". Make sure the server is running.",
                    connectionFailure);
        }
    }

    public Response send(Request request) throws ConnectionException {
        if (!isConnected()) {
            throw new ConnectionException("Not connected to the server");
        }

        BlockingQueue<Response> mailbox = new ArrayBlockingQueue<>(MAILBOX_CAPACITY);

        pendingResponses.put(request.getRequestId(), mailbox);

        try {
            writeRequest(request);
            Response response = mailbox.poll(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (response == null) {
                throw new ConnectionException("The server did not answer within "
                        + RESPONSE_TIMEOUT_SECONDS + " seconds");
            }
            return response;

        } catch (InterruptedException waitInterrupted) {
            
            Thread.currentThread().interrupt();
            throw new ConnectionException("The wait for the server was interrupted",
                    waitInterrupted);

        } finally {
            
            pendingResponses.remove(request.getRequestId());
        }
    }

    private void writeRequest(Request request) throws ConnectionException {
        try {
            synchronized (outputStream) {
                outputStream.writeObject(request);
                outputStream.flush();
                outputStream.reset();
            }
        } catch (IOException writeFailure) {
            throw new ConnectionException("The connection to the server was lost while sending "
                    + request.getActionType(), writeFailure);
        }
    }

    private void listenForIncomingObjects() {
        try {
            while (isListening) {
                Object incomingObject = inputStream.readObject();
                routeIncomingObject(incomingObject);
            }
        } catch (EOFException | SocketException connectionClosed) {

            if (isListening) {
                reportConnectionLost();
            }
        } catch (IOException readFailure) {
            if (isListening) {
                System.err.println("[Client] failed to read from the server: "
                        + readFailure.getMessage());
                reportConnectionLost();
            }
        } catch (ClassNotFoundException unknownObject) {
            System.err.println("[Client] the server sent an object of an unknown class: "
                    + unknownObject.getMessage());
        } finally {
            isListening = false;
            
            pendingResponses.clear();
        }
    }

    private void routeIncomingObject(Object incomingObject) {
        if (incomingObject instanceof Response) {
            Response response = (Response) incomingObject;
            BlockingQueue<Response> mailbox = pendingResponses.get(response.getRequestId());
            if (mailbox == null) {
                
                System.err.println("[Client] received a late answer for request #"
                        + response.getRequestId());
                return;
            }
            mailbox.offer(response);

        } else if (incomingObject instanceof ServerEvent) {

            eventDispatcher.publish((ServerEvent) incomingObject);

        } else {
            System.err.println("[Client] ignoring an unknown object from the server: "
                    + incomingObject);
        }
    }

    private void reportConnectionLost() {
        System.err.println("[Client] the connection to the server was lost");
        Runnable handler = connectionLostHandler;
        if (handler != null) {
            handler.run();
        }
    }

    public void disconnect() {
        isListening = false;
        closeQuietly();
        System.out.println("[Client] disconnected from the server");
    }

    private void closeQuietly() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignoredFailure) {
        }
        socket = null;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && isListening;
    }

    public ClientEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public void setConnectionLostHandler(Runnable connectionLostHandler) {
        this.connectionLostHandler = connectionLostHandler;
    }
}
