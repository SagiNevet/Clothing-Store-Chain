package common.protocol;

import common.model.Branch;
import common.util.IdGenerator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A request sent from a client to the server.
 * <p>
 * One envelope class serves all the actions of the protocol. The action itself
 * is an {@link ActionType} constant, and the parameters travel in a map whose
 * keys are the constants of {@link ProtocolKeys}. The alternative - a separate
 * class per action - would mean more than twenty tiny classes and a long
 * {@code instanceof} chain on the server.
 * </p>
 * <p>
 * <b>The request identifier is the heart of the asynchronous client.</b> The
 * client sends requests and receives answers on two different threads, so
 * answers can arrive in any order. Every request carries a number, the matching
 * {@link Response} carries the same number back, and the listening thread uses
 * it to hand the answer to the exact caller that is waiting for it.
 * </p>
 * <p>
 * Every value placed in the payload must itself be {@link Serializable},
 * otherwise the object stream will refuse to send the request.
 * </p>
 */
public class Request implements Serializable {

    /** Serialization version, so both sides agree on the shape of the class. */
    private static final long serialVersionUID = 1L;

    /** The number that ties this request to its response. */
    private final long requestId;

    /** The action the client asks the server to perform. */
    private final ActionType actionType;

    /** The employee number of the sender, or {@code null} before logging in. */
    private final String senderEmployeeNumber;

    /** The branch of the sender, or {@code null} before logging in. */
    private final Branch senderBranch;

    /** The parameters of the action, keyed by the constants of {@link ProtocolKeys}. */
    private final Map<String, Object> payload;

    /**
     * Creates a request that is sent before the sender is known, such as a
     * login request.
     *
     * @param actionType the action the client asks the server to perform
     */
    public Request(ActionType actionType) {
        this(actionType, null, null);
    }

    /**
     * Creates a request sent by a logged in employee.
     *
     * @param actionType           the action the client asks the server to perform
     * @param senderEmployeeNumber the employee number of the sender
     * @param senderBranch         the branch of the sender
     */
    public Request(ActionType actionType, String senderEmployeeNumber, Branch senderBranch) {
        this.requestId = IdGenerator.nextRequestId();
        this.actionType = actionType;
        this.senderEmployeeNumber = senderEmployeeNumber;
        this.senderBranch = senderBranch;
        this.payload = new HashMap<>();
    }

    /**
     * Adds one parameter to the request and returns the request itself, so that
     * several parameters can be added one after the other in a single statement.
     *
     * @param key   one of the constants of {@link ProtocolKeys}
     * @param value the value, which must be serializable
     * @return this same request object
     */
    public Request withParameter(String key, Object value) {
        payload.put(key, value);
        return this;
    }

    /**
     * Returns the number that ties this request to its response.
     *
     * @return the request identifier
     */
    public long getRequestId() {
        return requestId;
    }

    /**
     * Returns the action the client asked for.
     *
     * @return the action type, never {@code null}
     */
    public ActionType getActionType() {
        return actionType;
    }

    /**
     * Returns the employee number of the sender.
     *
     * @return the sender employee number, or {@code null} before logging in
     */
    public String getSenderEmployeeNumber() {
        return senderEmployeeNumber;
    }

    /**
     * Returns the branch of the sender.
     *
     * @return the sender branch, or {@code null} before logging in
     */
    public Branch getSenderBranch() {
        return senderBranch;
    }

    /**
     * Reads a raw parameter.
     *
     * @param key one of the constants of {@link ProtocolKeys}
     * @return the value, or {@code null} when the parameter was not sent
     */
    public Object getParameter(String key) {
        return payload.get(key);
    }

    /**
     * Reads a text parameter.
     *
     * @param key one of the constants of {@link ProtocolKeys}
     * @return the value as text, or {@code null} when the parameter was not sent
     */
    public String getString(String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * Reads a whole number parameter.
     *
     * @param key one of the constants of {@link ProtocolKeys}
     * @return the value as a whole number
     * @throws IllegalArgumentException if the parameter is missing or is not a number
     */
    public int getInt(String key) {
        Object value = payload.get(key);
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "Request parameter " + key + " is missing or is not a number");
        }
        return ((Number) value).intValue();
    }

    /**
     * Reads a decimal number parameter.
     *
     * @param key one of the constants of {@link ProtocolKeys}
     * @return the value as a decimal number
     * @throws IllegalArgumentException if the parameter is missing or is not a number
     */
    public double getDouble(String key) {
        Object value = payload.get(key);
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "Request parameter " + key + " is missing or is not a number");
        }
        return ((Number) value).doubleValue();
    }

    @Override
    public String toString() {
        return "Request #" + requestId + " " + actionType
                + " from " + senderEmployeeNumber
                + " with parameters " + payload.keySet();
    }
}
