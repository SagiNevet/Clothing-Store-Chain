package common.protocol;

import common.model.Branch;
import common.util.IdGenerator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long requestId;

    private final ActionType actionType;

    private final String senderEmployeeNumber;

    private final Branch senderBranch;

    private final Map<String, Object> payload;

    public Request(ActionType actionType) {
        this(actionType, null, null);
    }

    public Request(ActionType actionType, String senderEmployeeNumber, Branch senderBranch) {
        this.requestId = IdGenerator.nextRequestId();
        this.actionType = actionType;
        this.senderEmployeeNumber = senderEmployeeNumber;
        this.senderBranch = senderBranch;
        this.payload = new HashMap<>();
    }

    public Request withParameter(String key, Object value) {
        payload.put(key, value);
        return this;
    }

    public long getRequestId() {
        return requestId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getSenderEmployeeNumber() {
        return senderEmployeeNumber;
    }

    public Branch getSenderBranch() {
        return senderBranch;
    }

    public Object getParameter(String key) {
        return payload.get(key);
    }

    public String getString(String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }

    public int getInt(String key) {
        Object value = payload.get(key);
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "Request parameter " + key + " is missing or is not a number");
        }
        return ((Number) value).intValue();
    }

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
