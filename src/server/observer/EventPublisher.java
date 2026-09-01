package server.observer;

import common.model.Branch;
import common.protocol.ServerEvent;
import server.core.ClientRegistry;
import server.core.ConnectedClient;

import java.io.IOException;
import java.util.List;

public final class EventPublisher {

    private static final EventPublisher INSTANCE = new EventPublisher();

    private EventPublisher() {
    }

    public static EventPublisher getInstance() {
        return INSTANCE;
    }

    public void publishToBranch(Branch branch, ServerEvent event) {
        List<ConnectedClient> recipients =
                ClientRegistry.getInstance().getClientsOfBranch(branch);
        deliver(recipients, event);
    }

    public void publishToAll(ServerEvent event) {
        List<ConnectedClient> recipients = ClientRegistry.getInstance().getAllClients();
        deliver(recipients, event);
    }

    public boolean publishToEmployee(String employeeNumber, ServerEvent event) {
        ConnectedClient recipient =
                ClientRegistry.getInstance().findByEmployeeNumber(employeeNumber);
        if (recipient == null) {
            return false;
        }
        return sendToOne(recipient, event);
    }

    private void deliver(List<ConnectedClient> recipients, ServerEvent event) {
        for (ConnectedClient currentClient : recipients) {
            if (currentClient.isLoggedIn()) {
                sendToOne(currentClient, event);
            }
        }
    }

    private boolean sendToOne(ConnectedClient recipient, ServerEvent event) {
        try {
            recipient.sendEvent(event);
            return true;
        } catch (IOException deliveryFailure) {

            System.err.println("[EventPublisher] could not deliver "
                    + event.getEventType() + " to " + recipient
                    + ": " + deliveryFailure.getMessage());
            return false;
        }
    }
}
