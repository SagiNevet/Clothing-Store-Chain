package server.service;

import common.exception.DuplicateLoginException;
import common.model.Employee;
import server.core.ConnectedClient;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final ConcurrentHashMap<String, ConnectedClient> activeSessions =
            new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void openSession(Employee employee, ConnectedClient client)
            throws DuplicateLoginException {
        String employeeNumber = employee.getEmployeeNumber();
        ConnectedClient clientThatWonTheSession =
                activeSessions.putIfAbsent(employeeNumber, client);
        if (clientThatWonTheSession != null) {
            throw new DuplicateLoginException(employeeNumber);
        }
    }

    public void closeSession(String employeeNumber, ConnectedClient client) {
        if (employeeNumber == null) {
            return;
        }
        activeSessions.remove(employeeNumber, client);
    }

    public boolean isLoggedIn(String employeeNumber) {
        return activeSessions.containsKey(employeeNumber);
    }

    public ConnectedClient getSessionOf(String employeeNumber) {
        return activeSessions.get(employeeNumber);
    }

    public Set<String> getLoggedInEmployeeNumbers() {
        return Collections.unmodifiableSet(activeSessions.keySet());
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}
