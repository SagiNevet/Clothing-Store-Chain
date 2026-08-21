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

/**
 * The connection of one client to the server: sends requests and listens for
 * incoming objects at the same time.
 * <p>
 * <b>This class is the answer to the requirement that the client must send and
 * listen simultaneously.</b> The simple example shown in class was synchronous:
 * write a line, then block reading the answer. That is not enough here, because
 * the server pushes messages the client never asked for - an inventory update
 * from another employee, a chat message, an invitation to a conversation. A
 * client that only reads right after writing would see those messages far too
 * late, or not at all.
 * </p>
 * <p>
 * <b>How it works.</b> One thread of its own does nothing but read objects from
 * the socket, forever. What it does with an object depends on the type:
 * </p>
 * <ul>
 *   <li>A {@link Response} is the answer to a request somebody is waiting for.
 *       The listener finds that caller by the request number and hands the
 *       answer over.</li>
 *   <li>A {@link ServerEvent} is a push nobody asked for, so it goes to the
 *       {@link ClientEventDispatcher} which updates the screens.</li>
 * </ul>
 * <p>
 * <b>How a caller waits for its answer.</b> Before sending, the caller puts an
 * empty mailbox - a {@link BlockingQueue} of size one - into
 * {@link #pendingResponses} under its request number, and then blocks on that
 * mailbox. The listener thread drops the answer into the right mailbox, which
 * wakes exactly the caller that was waiting for it. Answers may therefore
 * arrive in any order, and slow requests never block fast ones.
 * </p>
 */
public class ServerConnection {

    /** How long a caller waits for an answer before giving up. */
    private static final int RESPONSE_TIMEOUT_SECONDS = 15;

    /** A mailbox holds exactly one answer. */
    private static final int MAILBOX_CAPACITY = 1;

    /** The mailbox of every request that is still waiting for its answer. */
    private final Map<Long, BlockingQueue<Response>> pendingResponses = new ConcurrentHashMap<>();

    /** The object that delivers pushed events to the screens. */
    private final ClientEventDispatcher eventDispatcher = new ClientEventDispatcher();

    /** The socket connected to the server, or {@code null} before connecting. */
    private Socket socket;

    /** The stream used to send requests. */
    private ObjectOutputStream outputStream;

    /** The stream used to read responses and events. */
    private ObjectInputStream inputStream;

    /** The thread that reads incoming objects. */
    private Thread listenerThread;

    /**
     * Whether the listener thread should keep reading. Declared {@code volatile}
     * so that the change made by {@link #disconnect()} is seen immediately by
     * the listener thread.
     */
    private volatile boolean isListening;

    /** Called when the connection drops unexpectedly, may be {@code null}. */
    private volatile Runnable connectionLostHandler;

    /**
     * Opens the connection and starts the listener thread.
     *
     * @param host the address of the server, for example {@code localhost}
     * @param port the port the server listens on
     * @throws ConnectionException if the server cannot be reached
     */
    public void connect(String host, int port) throws ConnectionException {
        if (isConnected()) {
            return;
        }
        try {
            socket = new Socket(host, port);
            // Exactly the same order as the server: the output stream is created
            // and flushed first, because the constructor of ObjectInputStream
            // blocks until it reads the header the other side writes. Creating the
            // input stream first on both sides would deadlock the connection.
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());

            isListening = true;
            listenerThread = new Thread(this::listenForIncomingObjects, "server-listener");
            // A daemon thread does not keep the program alive on its own, so closing
            // the last window really ends the client.
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("[Client] connected to " + host + ":" + port);

        } catch (IOException connectionFailure) {
            throw new ConnectionException("Could not connect to the server at "
                    + host + ":" + port + ". Make sure the server is running.",
                    connectionFailure);
        }
    }

    /**
     * Sends a request and waits for the answer that belongs to it.
     * <p>
     * Called from a background thread of the client, never from the Swing event
     * dispatch thread: this method blocks, and blocking the Swing thread would
     * freeze the whole window.
     * </p>
     *
     * @param request the request to send
     * @return the answer of the server
     * @throws ConnectionException if the connection failed or no answer arrived
     *                             within {@value #RESPONSE_TIMEOUT_SECONDS} seconds
     */
    public Response send(Request request) throws ConnectionException {
        if (!isConnected()) {
            throw new ConnectionException("Not connected to the server");
        }

        BlockingQueue<Response> mailbox = new ArrayBlockingQueue<>(MAILBOX_CAPACITY);
        // The mailbox is registered BEFORE the request is sent. Doing it the other
        // way round would be a race: a fast server could answer before this line
        // ran, and the listener thread would find no mailbox and drop the answer.
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
            // The waiting thread was interrupted. The interrupted flag is restored
            // so that whoever asked for the interruption can still see it.
            Thread.currentThread().interrupt();
            throw new ConnectionException("The wait for the server was interrupted",
                    waitInterrupted);

        } finally {
            // Whatever happened, this request is no longer waiting for anything.
            pendingResponses.remove(request.getRequestId());
        }
    }

    /**
     * Writes one request to the socket.
     * <p>
     * The write is wrapped in a {@code synchronized} block because more than one
     * background thread may send a request at the same moment, and two threads
     * writing into the same stream would interleave their bytes. {@code reset()}
     * is called for the same reason as on the server: without it the stream
     * would send a reference to an older copy of an object that has changed.
     * </p>
     *
     * @param request the request to write
     * @throws ConnectionException if the request cannot be written
     */
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

    /**
     * The body of the listener thread: reads objects until the connection is
     * closed and routes each one according to its type.
     */
    private void listenForIncomingObjects() {
        try {
            while (isListening) {
                Object incomingObject = inputStream.readObject();
                routeIncomingObject(incomingObject);
            }
        } catch (EOFException | SocketException connectionClosed) {
            // Either the server shut down or disconnect() closed the socket under
            // this thread. Only the first case is worth reporting to the user.
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
            // Every mailbox still waiting will now time out instead of waiting
            // forever, because nothing will ever be delivered into it.
            pendingResponses.clear();
        }
    }

    /**
     * Sends one incoming object where it belongs.
     *
     * @param incomingObject the object read from the socket
     */
    private void routeIncomingObject(Object incomingObject) {
        if (incomingObject instanceof Response) {
            Response response = (Response) incomingObject;
            BlockingQueue<Response> mailbox = pendingResponses.get(response.getRequestId());
            if (mailbox == null) {
                // The caller already gave up and timed out. Nothing to do with it.
                System.err.println("[Client] received a late answer for request #"
                        + response.getRequestId());
                return;
            }
            mailbox.offer(response);

        } else if (incomingObject instanceof ServerEvent) {
            // Nobody is waiting for an event, so it goes to the screens. The
            // dispatcher moves it to the Swing thread on the way.
            eventDispatcher.publish((ServerEvent) incomingObject);

        } else {
            System.err.println("[Client] ignoring an unknown object from the server: "
                    + incomingObject);
        }
    }

    /**
     * Tells the client that the connection dropped on its own.
     */
    private void reportConnectionLost() {
        System.err.println("[Client] the connection to the server was lost");
        Runnable handler = connectionLostHandler;
        if (handler != null) {
            handler.run();
        }
    }

    /**
     * Closes the connection and stops the listener thread.
     * <p>
     * The flag is lowered first so the listener thread knows the closure was
     * intentional, and only then the socket is closed - which is what wakes the
     * thread out of its blocking read.
     * </p>
     */
    public void disconnect() {
        isListening = false;
        closeQuietly();
        System.out.println("[Client] disconnected from the server");
    }

    /**
     * Closes the socket and the streams, ignoring failures.
     */
    private void closeQuietly() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignoredFailure) {
            // The connection is going away anyway.
        }
        socket = null;
    }

    /**
     * Indicates whether the client is connected to the server.
     *
     * @return {@code true} while the socket is open
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && isListening;
    }

    /**
     * Returns the dispatcher screens subscribe to in order to receive pushed
     * events.
     *
     * @return the event dispatcher of this connection
     */
    public ClientEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Registers what to do when the connection drops without being asked to.
     *
     * @param connectionLostHandler the action to run, may be {@code null}
     */
    public void setConnectionLostHandler(Runnable connectionLostHandler) {
        this.connectionLostHandler = connectionLostHandler;
    }
}
