package common.protocol;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ServerEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final EventType eventType;

    private final LocalDateTime publishedAt;

    private final Map<String, Object> payload;

    public ServerEvent(EventType eventType) {
        this.eventType = eventType;
        this.publishedAt = LocalDateTime.now();
        this.payload = new HashMap<>();
    }

    public ServerEvent withPayload(String key, Object value) {
        payload.put(key, value);
        return this;
    }

    public EventType getEventType() {
        return eventType;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Object getPayload(String key) {
        return payload.get(key);
    }

    @Override
    public String toString() {
        return "ServerEvent " + eventType + " with values " + payload.keySet();
    }
}
