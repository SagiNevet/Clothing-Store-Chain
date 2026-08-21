package client.net;

import common.protocol.ServerEvent;

/**
 * Implemented by any screen that wants to be told when the server pushes an
 * event.
 * <p>
 * This interface is the client half of the <b>Observer</b> pattern. A panel
 * registers itself with the {@link ClientEventDispatcher}, and from that moment
 * the network layer notifies it whenever something changes on the server -
 * without the network layer knowing anything about Swing, panels or tables.
 * </p>
 * <p>
 * <b>Threading guarantee:</b> the dispatcher always calls this method on the
 * Swing event dispatch thread, so an implementation may touch its components
 * directly and safely.
 * </p>
 */
public interface ServerEventListener {

    /**
     * Called when the server pushed an event to this client.
     *
     * @param event the event that arrived, never {@code null}
     */
    void onServerEvent(ServerEvent event);
}
