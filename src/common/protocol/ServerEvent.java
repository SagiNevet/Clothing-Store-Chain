package common.protocol;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * A message the server pushes to a client without having been asked.
 * <p>
 * This is the class that makes the system live: when one employee sells a
 * shirt, the server publishes an {@link EventType#INVENTORY_UPDATED} event and
 * every other employee of that branch sees the new quantity immediately.
 * </p>
 * <p>
 * <b>Why this is a separate class and not a {@link Response}:</b> the client
 * receives all the incoming objects on one listening thread. When that thread
 * reads a {@code Response} it looks for the caller waiting for that request
 * number; when it reads a {@code ServerEvent} there is no caller waiting, so it
 * passes the object to the screens instead. Telling the two apart by their type
 * keeps the routing simple and impossible to get wrong.
 * </p>
 */
public class ServerEvent implements Serializable {

    /** Serialization version, so both sides agree on the shape of the class. */
    private static final long serialVersionUID = 1L;

    /** What happened. */
    private final EventType eventType;

    /** When it happened, according to the clock of the server. */
    private final LocalDateTime publishedAt;

    /** The details of the event, keyed by the constants of {@link ProtocolKeys}. */
    private final Map<String, Object> payload;

    /**
     * Creates an event with an empty payload.
     *
     * @param eventType what happened
     */
    public ServerEvent(EventType eventType) {
        this.eventType = eventType;
        this.publishedAt = LocalDateTime.now();
        this.payload = new HashMap<>();
    }

    /**
     * Adds one value to the payload and returns the event itself, so that
     * several values can be added one after the other in a single statement.
     *
     * @param key   one of the constants of {@link ProtocolKeys}
     * @param value the value, which must be serializable
     * @return this same event object
     */
    public ServerEvent withPayload(String key, Object value) {
        payload.put(key, value);
        return this;
    }

    /**
     * Returns what happened.
     *
     * @return the event type, never {@code null}
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Returns when the event was published by the server.
     *
     * @return the publication time
     */
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    /**
     * Reads one value of the payload.
     *
     * @param key one of the constants of {@link ProtocolKeys}
     * @return the value, or {@code null} when it is not part of this event
     */
    public Object getPayload(String key) {
        return payload.get(key);
    }

    @Override
    public String toString() {
        return "ServerEvent " + eventType + " with values " + payload.keySet();
    }
}
