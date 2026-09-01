package client.net;

import common.protocol.ServerEvent;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientEventDispatcher {

    private final List<ServerEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(ServerEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(ServerEventListener listener) {
        listeners.remove(listener);
    }

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

    public int getListenerCount() {
        return listeners.size();
    }
}
