package common.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The answer the server sends back for one {@link Request}.
 * <p>
 * The response repeats the request identifier it answers, which is how the
 * listening thread of the client hands the answer to the caller that is waiting
 * for it. A response is never pushed on its own initiative - a message the
 * server sends without being asked is a {@link ServerEvent} instead, and the
 * client tells the two apart by their type.
 * </p>
 */
public class Response implements Serializable {

    /** Serialization version, so both sides agree on the shape of the class. */
    private static final long serialVersionUID = 1L;

    /** The empty message used when an action succeeded and has nothing to explain. */
    private static final String NO_MESSAGE = "";

    /** The identifier of the request this response answers. */
    private final long requestId;

    /** Whether the action succeeded or failed. */
    private final ResponseStatus status;

    /** The explanation shown to the user, mainly used when the action failed. */
    private final String message;

    /** The result of the action, keyed by the constants of {@link ProtocolKeys}. */
    private final Map<String, Object> payload;

    /**
     * Creates a response. Use the static factory methods instead of calling
     * this constructor directly.
     *
     * @param requestId the identifier of the request being answered
     * @param status    whether the action succeeded
     * @param message   the explanation for the user
     */
    private Response(long requestId, ResponseStatus status, String message) {
        this.requestId = requestId;
        this.status = status;
        this.message = message;
        this.payload = new HashMap<>();
    }

    /**
     * Creates a successful response with an empty payload.
     *
     * @param requestId the identifier of the request being answered
     * @return a response marked as successful
     */
    public static Response success(long requestId) {
        return new Response(requestId, ResponseStatus.SUCCESS, NO_MESSAGE);
    }

    /**
     * Creates a failed response carrying the reason for the failure.
     *
     * @param requestId the identifier of the request being answered
     * @param message   the reason, which the client displays to the user
     * @return a response marked as failed
     */
    public static Response failure(long requestId, String message) {
        return new Response(requestId, ResponseStatus.FAILURE, message);
    }

    /**
     * Adds one value to the payload and returns the response itself, so that
     * several values can be added one after the other in a single statement.
     *
     * @param key   one of the constants of {@link ProtocolKeys}
     * @param value the value, which must be serializable
     * @return this same response object
     */
    public Response withPayload(String key, Object value) {
        payload.put(key, value);
        return this;
    }

    /**
     * Returns the identifier of the request this response answers.
     *
     * @return the request identifier
     */
    public long getRequestId() {
        return requestId;
    }

    /**
     * Returns whether the action succeeded.
     *
     * @return the status of the response, never {@code null}
     */
    public ResponseStatus getStatus() {
        return status;
    }

    /**
     * Indicates whether the action succeeded.
     *
     * @return {@code true} when the status is {@link ResponseStatus#SUCCESS}
     */
    public boolean isSuccess() {
        return status == ResponseStatus.SUCCESS;
    }

    /**
     * Returns the explanation for the user.
     *
     * @return the message, empty when the action succeeded
     */
    public String getMessage() {
        return message;
    }

    /**
     * Reads one value of the payload.
     *
     * @param key one of the constants of {@link ProtocolKeys}
     * @return the value, or {@code null} when it is not part of this response
     */
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
