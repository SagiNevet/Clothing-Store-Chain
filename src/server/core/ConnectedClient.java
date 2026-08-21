package server.core;

import common.model.Branch;
import common.model.Employee;
import common.protocol.Response;
import common.protocol.ServerEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * One open connection between the server and a client, together with the
 * employee that is logged in on it.
 * <p>
 * This is the class the lecturer called {@code SocketData}: the object that
 * holds a socket, its two streams and the identity of whoever is sitting behind
 * it. Every connected client of the server is one instance of this class, and
 * all of them live in the {@link ClientRegistry}.
 * </p>
 * <p>
 * <b>The order in which the streams are created matters.</b> The output stream
 * is created first and flushed immediately, and only then the input stream. The
 * reason is that the constructor of {@link ObjectInputStream} <b>blocks</b>
 * until it has read the small header that {@link ObjectOutputStream} writes at
 * the very beginning of a connection. If both sides created the input stream
 * first, each one would sit waiting for a header the other side has not sent
 * yet, and the connection would deadlock before a single message is exchanged.
 * </p>
 */
public class ConnectedClient {

    /** The socket of this connection. */
    private final Socket socket;

    /** The stream used to send responses and events to the client. */
    private final ObjectOutputStream outputStream;

    /** The stream used to read requests from the client. */
    private final ObjectInputStream inputStream;

    /** The employee logged in on this connection, or {@code null} before login. */
    private volatile Employee employee;

    /**
     * Wraps an accepted socket and opens the two object streams over it.
     *
     * @param socket the socket returned by {@code accept()}
     * @throws IOException if the streams cannot be opened over the socket
     */
    public ConnectedClient(Socket socket) throws IOException {
        this.socket = socket;
        // The output stream is created and flushed first, so the client can
        // finish building its own input stream. See the class documentation.
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Reads the next object sent by the client. Blocks until one arrives.
     *
     * @return the object read from the connection
     * @throws IOException            if the connection was closed or failed
     * @throws ClassNotFoundException if the object is of a class this side does not know
     */
    public Object receive() throws IOException, ClassNotFoundException {
        return inputStream.readObject();
    }

    /**
     * Sends an answer to a request of this client.
     *
     * @param response the answer to send
     * @throws IOException if the answer cannot be written to the connection
     */
    public void sendResponse(Response response) throws IOException {
        send(response);
    }

    /**
     * Pushes an event to this client, without the client having asked for it.
     *
     * @param event the event to push
     * @throws IOException if the event cannot be written to the connection
     */
    public void sendEvent(ServerEvent event) throws IOException {
        send(event);
    }

    /**
     * Writes one object to the client.
     * <p>
     * <b>Race condition and how it is prevented:</b> two different threads write
     * to this same stream - the thread handling the requests of this client, and
     * any other client thread that publishes an event to everybody. Two threads
     * writing at once would interleave their bytes and destroy the stream for
     * good. The {@code synchronized} block around the write is therefore
     * mandatory, and it is deliberately kept to the three lines that touch the
     * stream and nothing more.
     * </p>
     * <p>
     * <b>Why {@code reset()} is called:</b> an {@link ObjectOutputStream}
     * remembers every object it has already sent, and if the same object is sent
     * again it writes only a short reference to the first copy. That is a real
     * trap here: after selling a shirt the server sends the very same
     * {@code Product} object again, and without {@code reset()} the client would
     * receive the <b>old quantity</b> and the inventory would look frozen.
     * {@code reset()} clears that memory and forces the full object to be
     * written every time.
     * </p>
     *
     * @param message the object to write, which must be serializable
     * @throws IOException if the object cannot be written to the connection
     */
    private void send(Object message) throws IOException {
        synchronized (outputStream) {
            outputStream.writeObject(message);
            outputStream.flush();
            outputStream.reset();
        }
    }

    /**
     * Records that an employee has logged in on this connection.
     *
     * @param employee the employee that was authenticated
     */
    public void attachEmployee(Employee employee) {
        this.employee = employee;
    }

    /**
     * Clears the employee of this connection after a logout.
     */
    public void detachEmployee() {
        this.employee = null;
    }

    /**
     * Returns the employee logged in on this connection.
     *
     * @return the employee, or {@code null} when nobody is logged in yet
     */
    public Employee getEmployee() {
        return employee;
    }

    /**
     * Indicates whether an employee is logged in on this connection.
     *
     * @return {@code true} if somebody is logged in
     */
    public boolean isLoggedIn() {
        return employee != null;
    }

    /**
     * Returns the branch of the employee logged in on this connection.
     *
     * @return the branch, or {@code null} when nobody is logged in yet
     */
    public Branch getBranch() {
        return employee == null ? null : employee.getBranch();
    }

    /**
     * Returns the employee number of the employee logged in on this connection.
     *
     * @return the employee number, or {@code null} when nobody is logged in yet
     */
    public String getEmployeeNumber() {
        return employee == null ? null : employee.getEmployeeNumber();
    }

    /**
     * Closes the streams and the socket of this connection.
     * <p>
     * Each resource is closed inside its own {@code try}, because a failure to
     * close the first one must not prevent the others from being closed. The
     * failures themselves are ignored on purpose: the connection is being taken
     * down anyway, and there is nothing useful left to do about them.
     * </p>
     */
    public void close() {
        try {
            inputStream.close();
        } catch (IOException ignoredFailure) {
            // Nothing can be done while shutting a connection down.
        }
        try {
            outputStream.close();
        } catch (IOException ignoredFailure) {
            // Nothing can be done while shutting a connection down.
        }
        try {
            socket.close();
        } catch (IOException ignoredFailure) {
            // Nothing can be done while shutting a connection down.
        }
    }

    @Override
    public String toString() {
        String employeeDescription = employee == null
                ? "not logged in"
                : employee.getEmployeeNumber() + " (" + employee.getBranch().getDisplayName() + ")";
        return "Connection from " + socket.getRemoteSocketAddress() + " - " + employeeDescription;
    }
}
