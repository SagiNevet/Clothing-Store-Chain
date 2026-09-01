package client.controller;

import client.net.ServerConnection;
import common.model.Branch;
import common.model.Employee;
import common.model.Role;

public final class ClientSession {

    private static final ClientSession INSTANCE = new ClientSession();

    private final ServerConnection connection = new ServerConnection();

    private volatile Employee currentEmployee;

    private ClientSession() {
    }

    public static ClientSession getInstance() {
        return INSTANCE;
    }

    public ServerConnection getConnection() {
        return connection;
    }

    public Employee getCurrentEmployee() {
        return currentEmployee;
    }

    public void setCurrentEmployee(Employee employee) {
        this.currentEmployee = employee;
    }

    public void clearCurrentEmployee() {
        this.currentEmployee = null;
    }

    public boolean isLoggedIn() {
        return currentEmployee != null;
    }

    public Branch getBranch() {
        return currentEmployee == null ? null : currentEmployee.getBranch();
    }

    public Role getRole() {
        return currentEmployee == null ? null : currentEmployee.getRole();
    }
}
