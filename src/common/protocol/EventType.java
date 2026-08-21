package common.protocol;

/**
 * Every kind of message the server pushes to a client without being asked.
 * <p>
 * These events are what turns the system into a live one. They are the
 * <b>Observer</b> pattern travelling over the socket: the server publishes a
 * change, and every client that is currently connected receives it and refreshes
 * its screen. Without them a client would have to ask the server again and
 * again whether anything changed.
 * </p>
 */
public enum EventType {

    /**
     * The inventory of a branch changed after a sale, a restock or a new
     * product. Delivered only to the employees connected to that branch,
     * because the inventory is per branch.
     */
    INVENTORY_UPDATED,

    /**
     * The customer list changed. Delivered to every connected employee of every
     * branch, because the customer list is shared by the whole chain.
     */
    CUSTOMERS_UPDATED,

    /** An employee account was created or updated. Delivered to shift managers. */
    EMPLOYEES_UPDATED,

    /** Somebody opened a chat with this employee. */
    CHAT_INVITE,

    /** A new message arrived in a chat session this employee takes part in. */
    CHAT_MESSAGE,

    /** A shift manager joined the chat session this employee takes part in. */
    CHAT_MANAGER_JOINED,

    /** The other side closed the chat session. */
    CHAT_CLOSED,

    /**
     * An employee who was busy earlier is free now, and this client had asked
     * for a chat while nobody was available. This is the notification the
     * waiting queue produces.
     */
    CHAT_PEER_AVAILABLE,

    /** The server is shutting down or the session was closed by the server. */
    FORCED_LOGOUT
}
