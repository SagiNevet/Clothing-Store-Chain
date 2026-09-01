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

public class ChatController {

    public ChatSessionInfo requestChat(Branch targetBranch) throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.CHAT_REQUEST)
                .withParameter(ProtocolKeys.TARGET_BRANCH, targetBranch));
        return (ChatSessionInfo) response.getPayload(ProtocolKeys.CHAT_SESSION);
    }

    public void sendMessage(String sessionId, String text) throws ChainStoreException {
        sendAndVerify(newRequest(ActionType.CHAT_SEND)
                .withParameter(ProtocolKeys.CHAT_SESSION_ID, sessionId)
                .withParameter(ProtocolKeys.MESSAGE_TEXT, text));
    }

    @SuppressWarnings("unchecked")
    public List<ChatSessionInfo> loadOpenChats() throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.GET_ACTIVE_CHATS));
        return (List<ChatSessionInfo>) response.getPayload(ProtocolKeys.CHAT_SESSION_LIST);
    }

    @SuppressWarnings("unchecked")
    public List<ChatMessage> joinChat(String sessionId) throws ChainStoreException {
        Response response = sendAndVerify(newRequest(ActionType.CHAT_JOIN)
                .withParameter(ProtocolKeys.CHAT_SESSION_ID, sessionId));
        return (List<ChatMessage>) response.getPayload(ProtocolKeys.CHAT_HISTORY);
    }

    public void closeChat(String sessionId) throws ChainStoreException {
        sendAndVerify(newRequest(ActionType.CHAT_CLOSE)
                .withParameter(ProtocolKeys.CHAT_SESSION_ID, sessionId));
    }

    private Request newRequest(ActionType actionType) {
        ClientSession session = ClientSession.getInstance();
        return new Request(actionType,
                session.getCurrentEmployee().getEmployeeNumber(),
                session.getBranch());
    }

    private Response sendAndVerify(Request request) throws ChainStoreException {
        Response response = ClientSession.getInstance().getConnection().send(request);
        if (!response.isSuccess()) {
            throw new ChainStoreException(response.getMessage());
        }
        return response;
    }
}
