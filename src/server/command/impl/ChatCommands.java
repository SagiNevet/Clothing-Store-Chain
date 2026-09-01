package server.command.impl;

import common.exception.ChainStoreException;
import common.model.Branch;
import common.model.ChatMessage;
import common.model.ChatSessionInfo;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import server.chat.ChatService;
import server.chat.ChatSession;
import server.command.Command;
import server.core.ConnectedClient;

import java.util.ArrayList;
import java.util.List;

public final class ChatCommands {

    private ChatCommands() {
    }

    public static class RequestChat implements Command {

        @Override
        public Response execute(Request request, ConnectedClient client)
                throws ChainStoreException {
            Branch targetBranch = (Branch) request.getParameter(ProtocolKeys.TARGET_BRANCH);
            if (targetBranch == null) {
                throw new ChainStoreException("The request did not name a branch to talk to");
            }

            ChatSession session = ChatService.getInstance()
                    .openChat(client.getEmployee(), targetBranch);

            return Response.success(request.getRequestId())
                    .withPayload(ProtocolKeys.CHAT_SESSION_ID, session.getSessionId())
                    .withPayload(ProtocolKeys.CHAT_SESSION, session.toSessionInfo());
        }
    }

    public static class SendMessage implements Command {

        @Override
        public Response execute(Request request, ConnectedClient client)
                throws ChainStoreException {
            String sessionId = request.getString(ProtocolKeys.CHAT_SESSION_ID);
            String text = request.getString(ProtocolKeys.MESSAGE_TEXT);

            ChatMessage message = ChatService.getInstance()
                    .sendMessage(client.getEmployee(), sessionId, text);

            return Response.success(request.getRequestId())
                    .withPayload(ProtocolKeys.CHAT_MESSAGE, message);
        }
    }

    public static class JoinChat implements Command {

        @Override
        public Response execute(Request request, ConnectedClient client)
                throws ChainStoreException {
            String sessionId = request.getString(ProtocolKeys.CHAT_SESSION_ID);

            List<ChatMessage> history = ChatService.getInstance()
                    .joinChat(client.getEmployee(), sessionId);

            return Response.success(request.getRequestId())
                    .withPayload(ProtocolKeys.CHAT_HISTORY, new ArrayList<>(history));
        }
    }

    public static class CloseChat implements Command {

        @Override
        public Response execute(Request request, ConnectedClient client)
                throws ChainStoreException {
            String sessionId = request.getString(ProtocolKeys.CHAT_SESSION_ID);

            ChatService.getInstance().closeChat(client.getEmployee(), sessionId);

            return Response.success(request.getRequestId());
        }
    }

    public static class ListOpenChats implements Command {

        @Override
        public Response execute(Request request, ConnectedClient client)
                throws ChainStoreException {
            List<ChatSessionInfo> openSessions = ChatService.getInstance().getOpenSessions();

            return Response.success(request.getRequestId())
                    .withPayload(ProtocolKeys.CHAT_SESSION_LIST, new ArrayList<>(openSessions));
        }
    }
}
