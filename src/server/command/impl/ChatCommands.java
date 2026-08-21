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

/**
 * The five commands of the chat feature, gathered in one file.
 * <p>
 * Each of them is a tiny class whose only job is to read the parameters of a
 * request and call {@link ChatService}. Keeping them together makes the whole
 * feature readable at a glance, which is worth more here than one file per
 * class: not one of them is longer than a dozen lines.
 * </p>
 */
public final class ChatCommands {

    /**
     * Prevents instantiation. This class only groups the command classes.
     */
    private ChatCommands() {
    }

    /**
     * Asks to open a conversation with a free employee of another branch.
     * <p>
     * When nobody is free, {@code ChatService} throws
     * {@code ChatUnavailableException} with its queued flag raised, and the
     * handler turns that into a failed response whose message tells the user
     * they are in the queue. Nothing is lost: the request stays in the queue.
     * </p>
     */
    public static class RequestChat implements Command {

        /**
         * {@inheritDoc}
         */
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

    /**
     * Sends one message inside an open conversation.
     */
    public static class SendMessage implements Command {

        /**
         * {@inheritDoc}
         */
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

    /**
     * Lets a shift manager join a conversation that is already open, and hands
     * back everything that was said before they arrived.
     */
    public static class JoinChat implements Command {

        /**
         * {@inheritDoc}
         */
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

    /**
     * Closes a conversation and frees both employees.
     */
    public static class CloseChat implements Command {

        /**
         * {@inheritDoc}
         */
        @Override
        public Response execute(Request request, ConnectedClient client)
                throws ChainStoreException {
            String sessionId = request.getString(ProtocolKeys.CHAT_SESSION_ID);

            ChatService.getInstance().closeChat(client.getEmployee(), sessionId);

            return Response.success(request.getRequestId());
        }
    }

    /**
     * Returns the conversations that are open right now, so a shift manager can
     * choose one to join.
     */
    public static class ListOpenChats implements Command {

        /**
         * {@inheritDoc}
         */
        @Override
        public Response execute(Request request, ConnectedClient client)
                throws ChainStoreException {
            List<ChatSessionInfo> openSessions = ChatService.getInstance().getOpenSessions();

            return Response.success(request.getRequestId())
                    .withPayload(ProtocolKeys.CHAT_SESSION_LIST, new ArrayList<>(openSessions));
        }
    }
}
