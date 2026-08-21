package client.controller;

import common.exception.ChainStoreException;
import common.model.Branch;
import common.model.ChatMessage;
import common.model.ChatSessionInfo;
import common.protocol.ActionType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;

import java.util.List;

/**
 * Turns what the chat screen wants into requests, and the answers back into
 * model objects.
 * <p>
 * <b>Threading:</b> every method blocks while it waits for the server, so all
 * of them must be called from a background thread. That matters more here than
 * anywhere else: a chat request can block for a few seconds on the server while
 * it waits for a free employee, and on the Swing thread those seconds would be
 * a frozen window.
 * </p>
 */
public class ChatController {

    /**
     * Asks to open a conversation with a free employee of another branch.
     *
     * @param targetBranch the branch to talk to
     * @return the conversation that was opened
     * @throws ChainStoreException if nobody is free - in which case the request
     *                             is now in the queue - or if the server cannot
     *                             be reached
     */
    public ChatSessionInfo requestChat(Branch targetBranch) throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.CHAT_REQUEST)
                .withParameter(ProtocolKeys.TARGET_BRANCH, targetBranch));
        return (ChatSessionInfo) response.getPayload(ProtocolKeys.CHAT_SESSION);
    }

    /**
     * Sends one message inside an open conversation.
     *
     * @param sessionId the conversation to write in
     * @param text      the text of the message
     * @throws ChainStoreException if the conversation is closed, the employee is
     *                             not part of it, or the server cannot be reached
     */
    public void sendMessage(String sessionId, String text) throws ChainStoreException {
        sendAndVerify(newRequest(ActionType.CHAT_SEND)
                .withParameter(ProtocolKeys.CHAT_SESSION_ID, sessionId)
                .withParameter(ProtocolKeys.MESSAGE_TEXT, text));
    }

    /**
     * Fetches the conversations that are open right now.
     *
     * @return the open conversations
     * @throws ChainStoreException if the role is not allowed or the server
     *                             cannot be reached
     */
    @SuppressWarnings("unchecked")
    public List<ChatSessionInfo> loadOpenChats() throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.GET_ACTIVE_CHATS));
        return (List<ChatSessionInfo>) response.getPayload(ProtocolKeys.CHAT_SESSION_LIST);
    }

    /**
     * Joins a conversation that is already open. Shift managers only.
     *
     * @param sessionId the conversation to join
     * @return everything that was said before joining
     * @throws ChainStoreException if the role is not allowed, the conversation
     *                             is closed, or the server cannot be reached
     */
    @SuppressWarnings("unchecked")
    public List<ChatMessage> joinChat(String sessionId) throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.CHAT_JOIN)
                .withParameter(ProtocolKeys.CHAT_SESSION_ID, sessionId));
        return (List<ChatMessage>) response.getPayload(ProtocolKeys.CHAT_HISTORY);
    }

    /**
     * Closes a conversation and frees both sides.
     *
     * @param sessionId the conversation to close
     * @throws ChainStoreException if the employee is not part of it or the
     *                             server cannot be reached
     */
    public void closeChat(String sessionId) throws ChainStoreException {
        sendAndVerify(newRequest(ActionType.CHAT_CLOSE)
                .withParameter(ProtocolKeys.CHAT_SESSION_ID, sessionId));
    }

    /**
     * Builds a request already stamped with the employee and the branch of this
     * client.
     *
     * @param actionType the action to request
     * @return the request, ready for its parameters
     */
    private Request newRequest(ActionType actionType) {
        ClientSession session = ClientSession.getInstance();
        return new Request(actionType,
                session.getCurrentEmployee().getEmployeeNumber(),
                session.getBranch());
    }

    /**
     * Sends a request and turns a refusal into an exception.
     *
     * @param request the request to send
     * @return the successful answer
     * @throws ChainStoreException if the server refused the request or could not
     *                             be reached
     */
    private Response sendAndVerify(Request request) throws ChainStoreException {
        Response response = ClientSession.getInstance().getConnection().send(request);
        if (!response.isSuccess()) {
            throw new ChainStoreException(response.getMessage());
        }
        return response;
    }
}
