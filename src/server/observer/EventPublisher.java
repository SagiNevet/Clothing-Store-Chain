package server.observer;

import common.model.Branch;
import common.protocol.ServerEvent;
import server.core.ClientRegistry;
import server.core.ConnectedClient;

import java.io.IOException;
import java.util.List;

/**
 * Pushes events from the server to the clients that need to know about them.
 * <p>
 * This class is the <b>subject</b> of the Observer pattern on the server side.
 * The observers are the connected clients, kept in the {@link ClientRegistry},
 * and they are notified without the business services knowing anything about
 * sockets, streams or windows: a service simply announces "the inventory of Tel
 * Aviv changed" and this class decides who hears it.
 * </p>
 * <p>
 * <b>Why the events are targeted and not simply broadcast to everybody:</b>
 * </p>
 * <ul>
 *   <li>The inventory is kept per branch, so a sale in Tel Aviv is of no
 *       interest to an employee in Jerusalem - {@link #publishToBranch}.</li>
 *   <li>The customer list is shared by the whole chain, so a new customer must
 *       reach every connected employee - {@link #publishToAll}.</li>
 * </ul>
 * <p>
 * <b>One client must never break the delivery to the others.</b> A client whose
 * network died in the middle of a broadcast makes the write throw, so every
 * delivery is wrapped in its own {@code try}. The failure is reported and the
 * loop continues to the next client; the broken connection will be cleaned up
 * by its own handler thread anyway.
 * </p>
 */
public final class EventPublisher {

    /** The single instance, created when the class is first loaded. */
    private static final EventPublisher INSTANCE = new EventPublisher();

    /**
     * Prevents anybody from building a second publisher.
     */
    private EventPublisher() {
    }

    /**
     * Returns the single publisher instance.
     *
     * @return the singleton instance, never {@code null}
     */
    public static EventPublisher getInstance() {
        return INSTANCE;
    }

    /**
     * Pushes an event to every employee connected from one branch.
     *
     * @param branch the branch whose employees should be notified
     * @param event  the event to push
     */
    public void publishToBranch(Branch branch, ServerEvent event) {
        List<ConnectedClient> recipients =
                ClientRegistry.getInstance().getClientsOfBranch(branch);
        deliver(recipients, event);
    }

    /**
     * Pushes an event to every logged in employee of the whole chain.
     *
     * @param event the event to push
     */
    public void publishToAll(ServerEvent event) {
        List<ConnectedClient> recipients = ClientRegistry.getInstance().getAllClients();
        deliver(recipients, event);
    }

    /**
     * Pushes an event to one single employee, if that employee is connected.
     * <p>
     * Used by the chat feature, where an invitation or a notification concerns
     * exactly one person.
     * </p>
     *
     * @param employeeNumber the employee that should be notified
     * @param event          the event to push
     * @return {@code true} if the employee was connected and the event was sent
     */
    public boolean publishToEmployee(String employeeNumber, ServerEvent event) {
        ConnectedClient recipient =
                ClientRegistry.getInstance().findByEmployeeNumber(employeeNumber);
        if (recipient == null) {
            return false;
        }
        return sendToOne(recipient, event);
    }

    /**
     * Sends an event to a list of clients, one after another.
     *
     * @param recipients the clients that should receive the event
     * @param event      the event to send
     */
    private void deliver(List<ConnectedClient> recipients, ServerEvent event) {
        for (ConnectedClient currentClient : recipients) {
            if (currentClient.isLoggedIn()) {
                sendToOne(currentClient, event);
            }
        }
    }

    /**
     * Sends an event to one client and swallows a failure.
     *
     * @param recipient the client to send to
     * @param event     the event to send
     * @return {@code true} if the event was written successfully
     */
    private boolean sendToOne(ConnectedClient recipient, ServerEvent event) {
        try {
            recipient.sendEvent(event);
            return true;
        } catch (IOException deliveryFailure) {
            // This one connection is broken. Its own handler thread will notice
            // and clean it up; here the important thing is that the remaining
            // clients still receive the event.
            System.err.println("[EventPublisher] could not deliver "
                    + event.getEventType() + " to " + recipient
                    + ": " + deliveryFailure.getMessage());
            return false;
        }
    }
}
