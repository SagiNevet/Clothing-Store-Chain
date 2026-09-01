package server.core;

import common.model.Branch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ClientRegistry {

    private static final ClientRegistry INSTANCE = new ClientRegistry();

    private final List<ConnectedClient> connectedClients = new CopyOnWriteArrayList<>();

    private ClientRegistry() {
    }

    public static ClientRegistry getInstance() {
        return INSTANCE;
    }

    public void add(ConnectedClient client) {
        connectedClients.add(client);
    }

    public void remove(ConnectedClient client) {
        connectedClients.remove(client);
    }

    public List<ConnectedClient> getAllClients() {
        return new ArrayList<>(connectedClients);
    }

    public List<ConnectedClient> getClientsOfBranch(Branch branch) {
        List<ConnectedClient> clientsOfBranch = new ArrayList<>();
        for (ConnectedClient currentClient : connectedClients) {
            if (currentClient.isLoggedIn() && currentClient.getBranch() == branch) {
                clientsOfBranch.add(currentClient);
            }
        }
        return clientsOfBranch;
    }

    public ConnectedClient findByEmployeeNumber(String employeeNumber) {
        for (ConnectedClient currentClient : connectedClients) {
            if (currentClient.isLoggedIn()
                    && currentClient.getEmployeeNumber().equals(employeeNumber)) {
                return currentClient;
            }
        }
        return null;
    }

    public int getConnectionCount() {
        return connectedClients.size();
    }
}
