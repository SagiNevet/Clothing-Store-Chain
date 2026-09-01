package client.gui;

import client.controller.ChatController;
import client.controller.ClientSession;
import common.model.Branch;
import common.model.ChatMessage;
import common.model.ChatSessionInfo;
import common.protocol.ProtocolKeys;
import common.protocol.ServerEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

public class ChatPanel extends ServerBackedPanel {

    private static final long serialVersionUID = 1L;

    private static final int CONVERSATION_ROWS = 18;

    private static final int MESSAGE_FIELD_COLUMNS = 40;

    private final JTextArea conversationArea = new JTextArea(CONVERSATION_ROWS, 0);

    private final JTextField messageField = new JTextField(MESSAGE_FIELD_COLUMNS);

    private final JLabel sessionLabel = new JLabel("No conversation is open");

    private final JButton sendButton = new JButton("Send");

    private final JButton closeButton = new JButton("Close conversation");

    private final transient ChatController chatController = new ChatController();

    private volatile String currentSessionId;

    public ChatPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        conversationArea.setEditable(false);
        conversationArea.setLineWrap(true);
        conversationArea.setWrapStyleWord(true);

        add(createTopPanel(), BorderLayout.NORTH);
        add(new JScrollPane(conversationArea), BorderLayout.CENTER);
        add(createMessagePanel(), BorderLayout.SOUTH);

        setConversationControlsEnabled(false);
    }

    private JPanel createTopPanel() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<Branch> targetBranchBox = new JComboBox<>(Branch.values());
        
        selectOtherBranch(targetBranchBox);

        JButton startButton = new JButton("Start conversation with");
        startButton.addActionListener(actionEvent ->
                startConversation((Branch) targetBranchBox.getSelectedItem()));

        toolbar.add(startButton);
        toolbar.add(targetBranchBox);

        if (ClientSession.getInstance().getRole().canJoinExistingChat()) {
            JButton joinButton = new JButton("Join an open conversation");
            joinButton.addActionListener(actionEvent -> openJoinDialog());
            toolbar.add(joinButton);
        }

        closeButton.addActionListener(actionEvent -> closeConversation());
        toolbar.add(closeButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolbar, BorderLayout.NORTH);
        topPanel.add(sessionLabel, BorderLayout.SOUTH);
        return topPanel;
    }

    private JPanel createMessagePanel() {
        JPanel messagePanel = new JPanel(new BorderLayout(6, 0));
        sendButton.addActionListener(actionEvent -> sendCurrentMessage());
        
        messageField.addActionListener(actionEvent -> sendCurrentMessage());
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        return messagePanel;
    }

    private void selectOtherBranch(JComboBox<Branch> branchBox) {
        Branch ownBranch = ClientSession.getInstance().getBranch();
        for (Branch branch : Branch.values()) {
            if (branch != ownBranch) {
                branchBox.setSelectedItem(branch);
                return;
            }
        }
    }

    private void startConversation(Branch targetBranch) {
        appendLine("Looking for a free employee in " + targetBranch.getDisplayName() + "...");

        runInBackground("chat-request", () -> {
            ChatSessionInfo session = chatController.requestChat(targetBranch);
            SwingUtilities.invokeLater(() -> enterConversation(session,
                    "Conversation opened with " + session.getPartnerName()));
        });
    }

    private void sendCurrentMessage() {
        String sessionId = currentSessionId;
        String text = messageField.getText().trim();
        if (sessionId == null || text.isEmpty()) {
            return;
        }
        messageField.setText("");

        runInBackground("chat-send", () -> chatController.sendMessage(sessionId, text));
    }

    private void openJoinDialog() {
        runInBackground("chat-list", () -> {
            List<ChatSessionInfo> openSessions = chatController.loadOpenChats();
            SwingUtilities.invokeLater(() -> askWhichConversationToJoin(openSessions));
        });
    }

    private void askWhichConversationToJoin(List<ChatSessionInfo> openSessions) {
        if (openSessions.isEmpty()) {
            showInfo("No conversation is open right now.");
            return;
        }

        ChatSessionInfo chosenSession = (ChatSessionInfo) JOptionPane.showInputDialog(this,
                "Which conversation would you like to join?", "Join a conversation",
                JOptionPane.QUESTION_MESSAGE, null,
                openSessions.toArray(new ChatSessionInfo[0]), openSessions.get(0));
        if (chosenSession == null) {
            return;
        }

        runInBackground("chat-join", () -> {
            List<ChatMessage> history = chatController.joinChat(chosenSession.getSessionId());
            SwingUtilities.invokeLater(() -> {
                enterConversation(chosenSession, "Joined the conversation of "
                        + chosenSession.describeParticipants());
                for (ChatMessage message : history) {
                    appendLine(message.toDisplayLine());
                }
            });
        });
    }

    private void closeConversation() {
        String sessionId = currentSessionId;
        if (sessionId == null) {
            return;
        }
        runInBackground("chat-close", () -> chatController.closeChat(sessionId));
    }

    private void enterConversation(ChatSessionInfo session, String openingSentence) {
        currentSessionId = session.getSessionId();
        sessionLabel.setText(session.describeParticipants());
        conversationArea.setText("");
        appendLine(openingSentence);
        setConversationControlsEnabled(true);
    }

    private void leaveConversation(String closingSentence) {
        currentSessionId = null;
        sessionLabel.setText("No conversation is open");
        appendLine(closingSentence);
        setConversationControlsEnabled(false);
    }

    private void setConversationControlsEnabled(boolean enabled) {
        messageField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        closeButton.setEnabled(enabled);
    }

    private void appendLine(String line) {
        conversationArea.append(line + System.lineSeparator());
        conversationArea.setCaretPosition(conversationArea.getDocument().getLength());
    }

    @Override
    public void onServerEvent(ServerEvent event) {
        switch (event.getEventType()) {
            case CHAT_INVITE:
                handleInvitation(event);
                break;
            case CHAT_MESSAGE:
                handleIncomingMessage(event);
                break;
            case CHAT_MANAGER_JOINED:
                appendLine("*** " + event.getPayload(ProtocolKeys.FULL_NAME)
                        + " (shift manager) joined this conversation ***");
                break;
            case CHAT_CLOSED:
                if (isCurrentSession(event)) {
                    leaveConversation("*** the conversation was closed ***");
                }
                break;
            case CHAT_PEER_AVAILABLE:
                handlePeerAvailable(event);
                break;
            default:
                break;
        }
    }

    private void handleInvitation(ServerEvent event) {
        ChatSessionInfo session = (ChatSessionInfo) event.getPayload(ProtocolKeys.CHAT_SESSION);
        if (session == null) {
            return;
        }
        enterConversation(session, session.getInitiatorName()
                + " of " + session.getInitiatorBranch().getDisplayName()
                + " started a conversation with you.");
    }

    private void handleIncomingMessage(ServerEvent event) {
        if (!isCurrentSession(event)) {
            return;
        }
        ChatMessage message = (ChatMessage) event.getPayload(ProtocolKeys.CHAT_MESSAGE);
        if (message != null) {
            appendLine(message.toDisplayLine());
        }
    }

    private void handlePeerAvailable(ServerEvent event) {
        Branch targetBranch = (Branch) event.getPayload(ProtocolKeys.TARGET_BRANCH);
        String freeEmployeeName = (String) event.getPayload(ProtocolKeys.FULL_NAME);

        int answer = JOptionPane.showConfirmDialog(this,
                freeEmployeeName + " of " + targetBranch.getDisplayName()
                        + " is free now. Would you like to start the conversation?",
                "Somebody is available", JOptionPane.YES_NO_OPTION);

        if (answer == JOptionPane.YES_OPTION) {
            startConversation(targetBranch);
        }
    }

    private boolean isCurrentSession(ServerEvent event) {
        Object sessionId = event.getPayload(ProtocolKeys.CHAT_SESSION_ID);
        return sessionId != null && sessionId.equals(currentSessionId);
    }
}
