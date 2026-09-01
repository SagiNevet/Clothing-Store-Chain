package common.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String NO_MESSAGE = "";

    private final long requestId;

    private final ResponseStatus status;

    private final String message;

    private final Map<String, Object> payload;

    private Response(long requestId, ResponseStatus status, String message) {
        this.requestId = requestId;
        this.status = status;
        this.message = message;
        this.payload = new HashMap<>();
    }

    public static Response success(long requestId) {
        return new Response(requestId, ResponseStatus.SUCCESS, NO_MESSAGE);
    }

    public static Response failure(long requestId, String message) {
        return new Response(requestId, ResponseStatus.FAILURE, message);
    }

    public Response withPayload(String key, Object value) {
        payload.put(key, value);
        return this;
    }

    public long getRequestId() {
        return requestId;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == ResponseStatus.SUCCESS;
    }

    public String getMessage() {
        return message;
    }

    public Object getPayload(String key) {
        return payload.get(key);
    }

    @Override
    public String toString() {
        return "Response to #" + requestId + " " + status
                + (message.isEmpty() ? "" : " (" + message + ")")
                + " with values " + payload.keySet();
    }
}
