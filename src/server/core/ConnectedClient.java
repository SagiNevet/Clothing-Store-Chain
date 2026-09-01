package server.core;

import common.model.Branch;
import common.model.Employee;
import common.protocol.Response;
import common.protocol.ServerEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConnectedClient {

    private final Socket socket;

    private final ObjectOutputStream outputStream;

    private final ObjectInputStream inputStream;

    private volatile Employee employee;

    public ConnectedClient(Socket socket) throws IOException {
        this.socket = socket;

        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(socket.getInputStream());
    }

    public Object receive() throws IOException, ClassNotFoundException {
        return inputStream.readObject();
    }

    public void sendResponse(Response response) throws IOException {
        send(response);
    }

    public void sendEvent(ServerEvent event) throws IOException {
        send(event);
    }

    private void send(Object message) throws IOException {
        synchronized (outputStream) {
            outputStream.writeObject(message);
            outputStream.flush();
            outputStream.reset();
        }
    }

    public void attachEmployee(Employee employee) {
        this.employee = employee;
    }

    public void detachEmployee() {
        this.employee = null;
    }

    public Employee getEmployee() {
        return employee;
    }

    public boolean isLoggedIn() {
        return employee != null;
    }

    public Branch getBranch() {
        return employee == null ? null : employee.getBranch();
    }

    public String getEmployeeNumber() {
        return employee == null ? null : employee.getEmployeeNumber();
    }

    public void close() {
        try {
            inputStream.close();
        } catch (IOException ignoredFailure) {
        }
        try {
            outputStream.close();
        } catch (IOException ignoredFailure) {
        }
        try {
            socket.close();
        } catch (IOException ignoredFailure) {
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
