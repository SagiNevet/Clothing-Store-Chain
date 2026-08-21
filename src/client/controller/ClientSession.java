package client.controller;

import client.net.ServerConnection;
import common.model.Branch;
import common.model.Employee;
import common.model.Role;

/**
 * Everything one running client knows about itself: its connection to the
 * server and the employee currently logged in.
 * <p>
 * The class is a <b>Singleton</b>, because a client process has exactly one
 * connection and one logged in employee. Every screen reaches the connection
 * through it, so no panel has to be handed a socket in its constructor.
 * </p>
 * <p>
 * Note that two clients started on the same laptop are two separate Java
 * processes, so each one has its own instance of this class - which is exactly
 * how one laptop can demonstrate two branches at once.
 * </p>
 */
public final class ClientSession {

    /** The single instance, created when the class is first loaded. */
    private static final ClientSession INSTANCE = new ClientSession();

    /** The connection of this client to the server. */
    private final ServerConnection connection = new ServerConnection();

    /**
     * The employee logged in right now, or {@code null} before login. Declared
     * {@code volatile} because it is written by a background thread after a
     * successful login and read by the Swing thread when a screen is built.
     */
    private volatile Employee currentEmployee;

    /**
     * Prevents anybody from building a second session.
     */
    private ClientSession() {
    }

    /**
     * Returns the single session instance.
     *
     * @return the singleton instance, never {@code null}
     */
    public static ClientSession getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the connection of this client.
     *
     * @return the server connection, never {@code null}
     */
    public ServerConnection getConnection() {
        return connection;
    }

    /**
     * Returns the employee logged in right now.
     *
     * @return the current employee, or {@code null} when nobody is logged in
     */
    public Employee getCurrentEmployee() {
        return currentEmployee;
    }

    /**
     * Records the employee that has just logged in.
     *
     * @param employee the employee returned by the server, without credentials
     */
    public void setCurrentEmployee(Employee employee) {
        this.currentEmployee = employee;
    }

    /**
     * Forgets the employee after a logout.
     */
    public void clearCurrentEmployee() {
        this.currentEmployee = null;
    }

    /**
     * Indicates whether somebody is logged in on this client.
     *
     * @return {@code true} when an employee is logged in
     */
    public boolean isLoggedIn() {
        return currentEmployee != null;
    }

    /**
     * Returns the branch of the employee logged in on this client.
     *
     * @return the branch, or {@code null} when nobody is logged in
     */
    public Branch getBranch() {
        return currentEmployee == null ? null : currentEmployee.getBranch();
    }

    /**
     * Returns the role of the employee logged in on this client.
     *
     * @return the role, or {@code null} when nobody is logged in
     */
    public Role getRole() {
        return currentEmployee == null ? null : currentEmployee.getRole();
    }
}
