package server.service;

import common.exception.DuplicateLoginException;
import common.model.Employee;
import server.core.ConnectedClient;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The table of the employees that are logged in right now, and the guard that
 * prevents the same employee from being connected twice.
 * <p>
 * The class is a <b>Singleton</b>. This is not a stylistic choice: two session
 * tables would mean two different answers to the question "is this employee
 * already connected", and the whole rule would collapse.
 * </p>
 * <p>
 * <b>The race condition this class exists to prevent:</b> two computers send a
 * login for employee 1001 at the very same instant. Written naively the check
 * would be
 * </p>
 * <pre>
 *     if (!sessions.containsKey(employeeNumber)) {   // both threads pass here
 *         sessions.put(employeeNumber, client);      // and both log in
 *     }
 * </pre>
 * <p>
 * Between the {@code containsKey} and the {@code put} the second thread can run
 * and see an empty table, so both logins succeed. This is the classic
 * check-then-act race. {@link ConcurrentHashMap#putIfAbsent} performs both
 * steps as <b>one atomic operation</b>: exactly one of the two threads gets
 * {@code null} back and wins the session, the other one receives the winning
 * connection and is refused. No lock is needed, and nothing waits.
 * </p>
 */
public final class SessionManager {

    /** The single instance, created when the class is first loaded. */
    private static final SessionManager INSTANCE = new SessionManager();

    /** The connection of every logged in employee, keyed by employee number. */
    private final ConcurrentHashMap<String, ConnectedClient> activeSessions =
            new ConcurrentHashMap<>();

    /**
     * Prevents anybody from building a second session table.
     */
    private SessionManager() {
    }

    /**
     * Returns the single session manager instance.
     *
     * @return the singleton instance, never {@code null}
     */
    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /**
     * Opens a session for an employee that has just been authenticated.
     *
     * @param employee the employee that passed authentication
     * @param client   the connection the employee logged in from
     * @throws DuplicateLoginException if that employee already has an open
     *                                 session on another connection
     */
    public void openSession(Employee employee, ConnectedClient client)
            throws DuplicateLoginException {
        String employeeNumber = employee.getEmployeeNumber();
        ConnectedClient clientThatWonTheSession =
                activeSessions.putIfAbsent(employeeNumber, client);
        if (clientThatWonTheSession != null) {
            throw new DuplicateLoginException(employeeNumber);
        }
    }

    /**
     * Closes the session of an employee, on logout or when the connection drops.
     * <p>
     * The connection is passed as well so that the session is removed only when
     * it really belongs to that connection. Without this check a dropped old
     * connection could delete the session of a newer, legitimate login.
     * </p>
     *
     * @param employeeNumber the employee whose session should be closed
     * @param client         the connection that is closing the session
     */
    public void closeSession(String employeeNumber, ConnectedClient client) {
        if (employeeNumber == null) {
            return;
        }
        activeSessions.remove(employeeNumber, client);
    }

    /**
     * Indicates whether an employee is logged in right now.
     *
     * @param employeeNumber the employee number to look for
     * @return {@code true} if that employee has an open session
     */
    public boolean isLoggedIn(String employeeNumber) {
        return activeSessions.containsKey(employeeNumber);
    }

    /**
     * Returns the connection an employee is logged in from.
     *
     * @param employeeNumber the employee number to look for
     * @return the connection of that employee, or {@code null} when not logged in
     */
    public ConnectedClient getSessionOf(String employeeNumber) {
        return activeSessions.get(employeeNumber);
    }

    /**
     * Returns the employee numbers of everybody logged in right now.
     *
     * @return an unmodifiable view of the logged in employee numbers
     */
    public Set<String> getLoggedInEmployeeNumbers() {
        return Collections.unmodifiableSet(activeSessions.keySet());
    }

    /**
     * Returns how many employees are logged in right now.
     *
     * @return the number of open sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}
