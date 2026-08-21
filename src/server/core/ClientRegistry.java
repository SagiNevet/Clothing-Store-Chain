package server.core;

import common.model.Branch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The list of every client currently connected to the server.
 * <p>
 * This is the structure the lecturer described as a static
 * {@code Vector<SocketData>} living at the level of the server. The idea is the
 * same; the collection is different, and the choice is worth explaining.
 * </p>
 * <p>
 * <b>Why {@link CopyOnWriteArrayList} and not {@code Vector}:</b>
 * </p>
 * <ul>
 *   <li>{@code Vector} synchronizes every single method, so even reading the
 *       list blocks other threads. In this system reading is by far the most
 *       common operation: every sale walks the whole list to push an event,
 *       while the list itself only changes when somebody connects or
 *       disconnects.</li>
 *   <li>A {@code Vector} is safe method by method, but <b>iterating</b> one is
 *       still not safe: if a client disconnects in the middle of a broadcast,
 *       the loop throws {@code ConcurrentModificationException}.
 *       {@code CopyOnWriteArrayList} hands each loop a private snapshot, so a
 *       broadcast can never be interrupted by a disconnection.</li>
 * </ul>
 * <p>
 * The price is that every connect and disconnect copies the array. With a
 * handful of employees that is nothing, and it buys lock free broadcasting.
 * </p>
 * <p>
 * The class is a <b>Singleton</b>, because there is exactly one list of
 * connections in a running server.
 * </p>
 */
public final class ClientRegistry {

    /** The single instance, created when the class is first loaded. */
    private static final ClientRegistry INSTANCE = new ClientRegistry();

    /** Every client currently connected, in the order they connected. */
    private final List<ConnectedClient> connectedClients = new CopyOnWriteArrayList<>();

    /**
     * Prevents anybody from building a second registry.
     */
    private ClientRegistry() {
    }

    /**
     * Returns the single registry instance.
     *
     * @return the singleton instance, never {@code null}
     */
    public static ClientRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Adds a newly accepted connection to the registry.
     *
     * @param client the connection to add
     */
    public void add(ConnectedClient client) {
        connectedClients.add(client);
    }

    /**
     * Removes a connection that has been closed.
     *
     * @param client the connection to remove
     */
    public void remove(ConnectedClient client) {
        connectedClients.remove(client);
    }

    /**
     * Returns every connected client.
     *
     * @return a copy of the list of connections, safe to iterate
     */
    public List<ConnectedClient> getAllClients() {
        return new ArrayList<>(connectedClients);
    }

    /**
     * Returns every connected client whose employee belongs to one branch.
     * <p>
     * Used when an event concerns a single branch, such as a change in the
     * inventory of that branch.
     * </p>
     *
     * @param branch the branch to filter by
     * @return the connections of the employees of that branch
     */
    public List<ConnectedClient> getClientsOfBranch(Branch branch) {
        List<ConnectedClient> clientsOfBranch = new ArrayList<>();
        for (ConnectedClient currentClient : connectedClients) {
            if (currentClient.isLoggedIn() && currentClient.getBranch() == branch) {
                clientsOfBranch.add(currentClient);
            }
        }
        return clientsOfBranch;
    }

    /**
     * Finds the connection of one logged in employee.
     *
     * @param employeeNumber the employee number to look for
     * @return the connection of that employee, or {@code null} when the
     *         employee is not connected
     */
    public ConnectedClient findByEmployeeNumber(String employeeNumber) {
        for (ConnectedClient currentClient : connectedClients) {
            if (currentClient.isLoggedIn()
                    && currentClient.getEmployeeNumber().equals(employeeNumber)) {
                return currentClient;
            }
        }
        return null;
    }

    /**
     * Returns how many clients are connected right now, including those that
     * have not logged in yet.
     *
     * @return the number of open connections
     */
    public int getConnectionCount() {
        return connectedClients.size();
    }
}
