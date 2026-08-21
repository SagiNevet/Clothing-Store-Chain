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

/**
 * The chat screen: opens a conversation with another branch, shows the
 * messages, and lets a shift manager join a conversation that is already open.
 * <p>
 * <b>Everything on this screen arrives as a pushed event.</b> Messages, the
 * invitation to a conversation somebody else opened, the notice that a manager
 * joined, and the notification that a colleague became free - none of them is
 * ever asked for. They arrive on the listener thread and reach this panel
 * through the dispatcher, which is the reason the chat feels immediate.
 * </p>
 */
public class ChatPanel extends ServerBackedPanel {

    /** Serialization version, required because Swing components are serializable. */
    private static final long serialVersionUID = 1L;

    /** The number of rows the conversation area is sized for. */
    private static final int CONVERSATION_ROWS = 18;

    /** The number of characters the message field is sized for. */
    private static final int MESSAGE_FIELD_COLUMNS = 40;

    /** The area showing the conversation. */
    private final JTextArea conversationArea = new JTextArea(CONVERSATION_ROWS, 0);

    /** The field the user types a message into. */
    private final JTextField messageField = new JTextField(MESSAGE_FIELD_COLUMNS);

    /** The line at the top describing the current conversation. */
    private final JLabel sessionLabel = new JLabel("No conversation is open");

    /** The button that sends a message. */
    private final JButton sendButton = new JButton("Send");

    /** The button that closes the conversation. */
    private final JButton closeButton = new JButton("Close conversation");

    /** The controller that performs the chat actions. */
    private final transient ChatController chatController = new ChatController();

    /**
     * The conversation this screen is showing, or {@code null} when none is
     * open. Declared {@code volatile} because a background thread sets it after
     * the server answers, while the Swing thread reads it.
     */
    private volatile String currentSessionId;

    /**
     * Builds the chat screen.
     */
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

    /**
     * Builds the strip of buttons and the label describing the conversation.
     *
     * @return the top panel
     */
    private JPanel createTopPanel() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<Branch> targetBranchBox = new JComboBox<>(Branch.values());
        // The other branch is the sensible default, since the feature exists for
        // talking between branches.
        selectOtherBranch(targetBranchBox);

        JButton startButton = new JButton("Start conversation with");
        startButton.addActionListener(actionEvent ->
                startConversation((Branch) targetBranchBox.getSelectedItem()));

        toolbar.add(startButton);
        toolbar.add(targetBranchBox);

        // Joining a conversation that is already open belongs to a shift manager
        // alone. The server refuses it for anybody else in any case; the button
        // is simply not built for a role that cannot use it.
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

    /**
     * Builds the row where a message is typed and sent.
     *
     * @return the message panel
     */
    private JPanel createMessagePanel() {
        JPanel messagePanel = new JPanel(new BorderLayout(6, 0));
        sendButton.addActionListener(actionEvent -> sendCurrentMessage());
        // Pressing Enter in the field sends the message, which is what anybody
        // expects from a chat.
        messageField.addActionListener(actionEvent -> sendCurrentMessage());
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        return messagePanel;
    }

    /**
     * Selects the branch the user does not work in, as the default target.
     *
     * @param branchBox the combo box to preselect
     */
    private void selectOtherBranch(JComboBox<Branch> branchBox) {
        Branch ownBranch = ClientSession.getInstance().getBranch();
        for (Branch branch : Branch.values()) {
            if (branch != ownBranch) {
                branchBox.setSelectedItem(branch);
                return;
            }
        }
    }

    /**
     * Asks the server to open a conversation with a free employee of a branch.
     */
    private void startConversation(Branch targetBranch) {
        appendLine("Looking for a free employee in " + targetBranch.getDisplayName() + "...");

        runInBackground("chat-request", () -> {
            ChatSessionInfo session = chatController.requestChat(targetBranch);
            SwingUtilities.invokeLater(() -> enterConversation(session,
                    "Conversation opened with " + session.getPartnerName()));
        });
    }

    /**
     * Sends whatever is typed in the message field.
     */
    private void sendCurrentMessage() {
        String sessionId = currentSessionId;
        String text = messageField.getText().trim();
        if (sessionId == null || text.isEmpty()) {
            return;
        }
        messageField.setText("");

        runInBackground("chat-send", () -> chatController.sendMessage(sessionId, text));
    }

    /**
     * Shows the list of open conversations and joins the chosen one.
     */
    private void openJoinDialog() {
        runInBackground("chat-list", () -> {
            List<ChatSessionInfo> openSessions = chatController.loadOpenChats();
            SwingUtilities.invokeLater(() -> askWhichConversationToJoin(openSessions));
        });
    }

    /**
     * Asks which conversation to join and joins it.
     *
     * @param openSessions the conversations that are open right now
     */
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

    /**
     * Closes the conversation that is open.
     */
    private void closeConversation() {
        String sessionId = currentSessionId;
        if (sessionId == null) {
            return;
        }
        runInBackground("chat-close", () -> chatController.closeChat(sessionId));
    }

    /**
     * Puts this screen into a conversation.
     *
     * @param session         the conversation that was opened or joined
     * @param openingSentence the line to show in the conversation area
     */
    private void enterConversation(ChatSessionInfo session, String openingSentence) {
        currentSessionId = session.getSessionId();
        sessionLabel.setText(session.describeParticipants());
        conversationArea.setText("");
        appendLine(openingSentence);
        setConversationControlsEnabled(true);
    }

    /**
     * Takes this screen out of a conversation.
     *
     * @param closingSentence the line to show in the conversation area
     */
    private void leaveConversation(String closingSentence) {
        currentSessionId = null;
        sessionLabel.setText("No conversation is open");
        appendLine(closingSentence);
        setConversationControlsEnabled(false);
    }

    /**
     * Turns the message field and the buttons on or off.
     *
     * @param enabled {@code true} while a conversation is open
     */
    private void setConversationControlsEnabled(boolean enabled) {
        messageField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        closeButton.setEnabled(enabled);
    }

    /**
     * Adds one line to the conversation area and scrolls to it.
     *
     * @param line the line to add
     */
    private void appendLine(String line) {
        conversationArea.append(line + System.lineSeparator());
        conversationArea.setCaretPosition(conversationArea.getDocument().getLength());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Called on the Swing thread by the dispatcher. Every kind of chat event is
     * handled here, which is why this screen never has to ask the server
     * anything after the conversation has started.
     * </p>
     */
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
                appendLine("-- " + event.getPayload(ProtocolKeys.FULL_NAME)
                        + " (shift manager) joined this conversation --");
                break;
            case CHAT_CLOSED:
                if (isCurrentSession(event)) {
                    leaveConversation("-- the conversation was closed --");
                }
                break;
            case CHAT_PEER_AVAILABLE:
                handlePeerAvailable(event);
                break;
            default:
                // Every other event belongs to another screen.
                break;
        }
    }

    /**
     * Enters the conversation somebody else opened with this employee.
     *
     * @param event the invitation event
     */
    private void handleInvitation(ServerEvent event) {
        ChatSessionInfo session = (ChatSessionInfo) event.getPayload(ProtocolKeys.CHAT_SESSION);
        if (session == null) {
            return;
        }
        enterConversation(session, session.getInitiatorName()
                + " of " + session.getInitiatorBranch().getDisplayName()
                + " started a conversation with you.");
    }

    /**
     * Shows a message that arrived in the open conversation.
     *
     * @param event the message event
     */
    private void handleIncomingMessage(ServerEvent event) {
        if (!isCurrentSession(event)) {
            return;
        }
        ChatMessage message = (ChatMessage) event.getPayload(ProtocolKeys.CHAT_MESSAGE);
        if (message != null) {
            appendLine(message.toDisplayLine());
        }
    }

    /**
     * Offers a call back when somebody the employee was waiting for becomes
     * free.
     * <p>
     * This is the other half of the queue: the request was put in the waiting
     * list because nobody was available, and this event is the answer to it.
     * </p>
     *
     * @param event the availability event
     */
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

    /**
     * Checks whether an event belongs to the conversation this screen is showing.
     *
     * @param event the event that arrived
     * @return {@code true} if it concerns the open conversation
     */
    private boolean isCurrentSession(ServerEvent event) {
        Object sessionId = event.getPayload(ProtocolKeys.CHAT_SESSION_ID);
        return sessionId != null && sessionId.equals(currentSessionId);
    }
}
