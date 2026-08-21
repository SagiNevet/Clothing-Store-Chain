package client.net;

import common.protocol.ServerEvent;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hands the events pushed by the server to the screens that asked to hear about
 * them.
 * <p>
 * This class is the <b>subject</b> of the Observer pattern on the client side,
 * and it is also the single bridge between two threads:
 * </p>
 * <ul>
 *   <li>The <b>listener thread</b> of {@link ServerConnection} reads an event
 *       from the socket and calls {@link #publish(ServerEvent)}.</li>
 *   <li>The <b>event dispatch thread</b> of Swing is where the screens are
 *       actually updated.</li>
 * </ul>
 * <p>
 * <b>Why {@link SwingUtilities#invokeLater} is mandatory here:</b> Swing is not
 * thread safe. Every component must be read and changed on one single thread,
 * the event dispatch thread. If the network thread touched a table directly the
 * program would work most of the time and then paint garbage or freeze at the
 * worst possible moment - during the defence, for example. {@code invokeLater}
 * puts the update in the queue of the event dispatch thread and returns
 * immediately, so the network thread goes straight back to reading the socket.
 * </p>
 * <p>
 * The list of listeners is a {@link CopyOnWriteArrayList} for the same reason
 * as in the server registry: it is read on every event and written only when a
 * screen opens or closes, and iterating it can never be broken by a screen that
 * unsubscribes in the middle of a notification.
 * </p>
 */
public class ClientEventDispatcher {

    /** The screens currently listening for events. */
    private final List<ServerEventListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Registers a screen to receive events.
     *
     * @param listener the screen that wants to be notified
     */
    public void subscribe(ServerEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Stops sending events to a screen, typically when it is closed.
     *
     * @param listener the screen that no longer wants to be notified
     */
    public void unsubscribe(ServerEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Delivers an event to every registered screen, on the Swing thread.
     * <p>
     * Called from the network listener thread. A failure inside one screen is
     * caught here so that it cannot stop the other screens from being updated.
     * </p>
     *
     * @param event the event that arrived from the server
     */
    public void publish(ServerEvent event) {
        SwingUtilities.invokeLater(() -> {
            for (ServerEventListener currentListener : listeners) {
                try {
                    currentListener.onServerEvent(event);
                } catch (Exception listenerFailure) {
                    System.err.println("[Client] a screen failed while handling "
                            + event.getEventType() + ": " + listenerFailure);
                }
            }
        });
    }

    /**
     * Returns how many screens are listening right now.
     *
     * @return the number of registered listeners
     */
    public int getListenerCount() {
        return listeners.size();
    }
}
