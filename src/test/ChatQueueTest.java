package test;

import common.exception.ChainStoreException;
import common.exception.PermissionDeniedException;
import common.model.Branch;
import common.model.ChatMessage;
import common.model.Employee;
import common.model.Role;
import common.protocol.ActionType;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import client.net.ServerConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.chat.ChatQueueManager;
import server.chat.ChatService;
import server.core.ChainServer;
import server.core.DataSeeder;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the chat feature: opening a conversation, the queue that holds requests
 * nobody could answer, the notification when somebody becomes free, and the
 * rule that only a shift manager may join a conversation already in progress.
 * <p>
 * These tests drive real connections, because the queue only means anything
 * when several employees are connected at the same moment.
 * </p>
 */
public class ChatQueueTest {

    /** The port used by the server of this test class. */
    private static final int TEST_PORT = 5054;

    /** How long a test waits for something that happens on another thread. */
    private static final int WAIT_TIMEOUT_SECONDS = 15;

    /** The demonstration shift manager of Tel Aviv. */
    private static final String TEL_AVIV_MANAGER = "1001";

    /** The demonstration cashier of Tel Aviv. */
    private static final String TEL_AVIV_CASHIER = "1002";

    /** The demonstration shift manager of Jerusalem. */
    private static final String JERUSALEM_MANAGER = "2001";

    /** The demonstration seller of Jerusalem. */
    private static final String JERUSALEM_SELLER = "2002";

    /** The server under test. */
    private static ChainServer server;

    /** The thread the blocking accept loop runs on. */
    private static Thread serverThread;

    /**
     * Starts the server once for the whole test class.
     *
     * @throws InterruptedException if the wait is interrupted
     */
    @BeforeAll
    public static void startServer() throws InterruptedException {
        server = new ChainServer(TEST_PORT);
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception serverFailure) {
                System.err.println("[Test] the test server stopped: " + serverFailure);
            }
        }, "test-server-chat-suite");
        serverThread.setDaemon(true);
        serverThread.start();
        waitUntilServerAcceptsConnections();
    }

    /**
     * Stops the server after the whole class has finished.
     *
     * @throws InterruptedException if the wait for the thread is interrupted
     */
    @AfterAll
    public static void stopServer() throws InterruptedException {
        server.stop();
        serverThread.join(WAIT_TIMEOUT_SECONDS * 1000L);
    }

    @Test
    @DisplayName("A conversation opens when somebody in the other branch is free")
    public void aConversationOpensWhenSomebodyIsFree() throws Exception {
        try (TestClient telAviv = new TestClient(TEL_AVIV_CASHIER);
             TestClient jerusalem = new TestClient(JERUSALEM_SELLER)) {

            CountDownLatch invitationArrived = new CountDownLatch(1);
            jerusalem.connection.getEventDispatcher().subscribe(event -> {
                if (event.getEventType() == EventType.CHAT_INVITE) {
                    invitationArrived.countDown();
                }
            });

            Response response = telAviv.requestChat(Branch.JERUSALEM);

            assertTrue(response.isSuccess(), response.getMessage());
            assertNotNull(response.getPayload(ProtocolKeys.CHAT_SESSION_ID));
            // The employee who was chosen must be told, so their window can open
            // the conversation by itself.
            assertTrue(invitationArrived.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "the chosen employee never received the invitation");

            telAviv.closeChat((String) response.getPayload(ProtocolKeys.CHAT_SESSION_ID));
        }
    }

    @Test
    @DisplayName("A message reaches the other side of the conversation")
    public void aMessageReachesTheOtherSide() throws Exception {
        try (TestClient telAviv = new TestClient(TEL_AVIV_CASHIER);
             TestClient jerusalem = new TestClient(JERUSALEM_SELLER)) {

            CountDownLatch messageArrived = new CountDownLatch(1);
            AtomicReference<ChatMessage> receivedMessage = new AtomicReference<>();
            jerusalem.connection.getEventDispatcher().subscribe(event -> {
                if (event.getEventType() == EventType.CHAT_MESSAGE) {
                    receivedMessage.set(
                            (ChatMessage) event.getPayload(ProtocolKeys.CHAT_MESSAGE));
                    messageArrived.countDown();
                }
            });

            String sessionId = telAviv.openChatWith(Branch.JERUSALEM);
            telAviv.sendMessage(sessionId, "Do you have the blue shirt in size M?");

            assertTrue(messageArrived.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "the message never arrived at the other side");
            assertEquals("Do you have the blue shirt in size M?",
                    receivedMessage.get().getContent());
            assertEquals(Branch.TEL_AVIV, receivedMessage.get().getSenderBranch());

            telAviv.closeChat(sessionId);
        }
    }

    @Test
    @DisplayName("When nobody is free the request goes into the queue")
    public void whenNobodyIsFreeTheRequestIsQueued() throws Exception {
        // Only one employee of Jerusalem is connected, and the first request
        // takes them. The second request therefore has nobody left to talk to.
        try (TestClient firstTelAviv = new TestClient(TEL_AVIV_CASHIER);
             TestClient secondTelAviv = new TestClient(TEL_AVIV_MANAGER);
             TestClient jerusalem = new TestClient(JERUSALEM_SELLER)) {

            String firstSessionId = firstTelAviv.openChatWith(Branch.JERUSALEM);

            Response secondResponse = secondTelAviv.requestChat(Branch.JERUSALEM);

            assertFalse(secondResponse.isSuccess(),
                    "there was nobody left to answer, so the request must be refused");
            assertTrue(secondResponse.getMessage().contains("queue"),
                    "the message should say the request is queued, but was: "
                            + secondResponse.getMessage());

            firstTelAviv.closeChat(firstSessionId);
        }
    }

    @Test
    @DisplayName("Somebody waiting in the queue is notified when an employee becomes free")
    public void aWaitingRequesterIsNotifiedWhenSomebodyBecomesFree() throws Exception {
        try (TestClient firstTelAviv = new TestClient(TEL_AVIV_CASHIER);
             TestClient waitingTelAviv = new TestClient(TEL_AVIV_MANAGER);
             TestClient jerusalem = new TestClient(JERUSALEM_SELLER)) {

            CountDownLatch availabilityArrived = new CountDownLatch(1);
            waitingTelAviv.connection.getEventDispatcher().subscribe(event -> {
                if (event.getEventType() == EventType.CHAT_PEER_AVAILABLE) {
                    availabilityArrived.countDown();
                }
            });

            String firstSessionId = firstTelAviv.openChatWith(Branch.JERUSALEM);
            // This request finds nobody free and stays in the queue.
            waitingTelAviv.requestChat(Branch.JERUSALEM);

            // Closing the first conversation frees the employee of Jerusalem,
            // which is exactly the moment the queue exists for.
            firstTelAviv.closeChat(firstSessionId);

            assertTrue(availabilityArrived.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "the waiting employee was never told that somebody became free");
        }
    }

    @Test
    @DisplayName("A cashier may not join a conversation that is already open")
    public void aCashierMayNotJoinAnOpenConversation() throws Exception {
        Employee cashier = new Employee(TEL_AVIV_CASHIER, "Ron Levi", "300000002",
                "050-1000002", "12-345-100002", Branch.TEL_AVIV, Role.CASHIER, "", "");

        // The rule is enforced by the service itself, so it holds no matter which
        // path reaches it - not only because a button is hidden in the window.
        assertThrows(PermissionDeniedException.class,
                () -> ChatService.getInstance().joinChat(cashier, "any-session"));
    }

    @Test
    @DisplayName("A shift manager may join and receives the history")
    public void aManagerMayJoinAndSeesTheHistory() throws Exception {
        try (TestClient telAviv = new TestClient(TEL_AVIV_CASHIER);
             TestClient jerusalem = new TestClient(JERUSALEM_SELLER);
             TestClient manager = new TestClient(JERUSALEM_MANAGER)) {

            String sessionId = telAviv.openChatWith(Branch.JERUSALEM);
            telAviv.sendMessage(sessionId, "first message");
            telAviv.sendMessage(sessionId, "second message");

            Response joinResponse = manager.send(new Request(ActionType.CHAT_JOIN,
                    JERUSALEM_MANAGER, Branch.JERUSALEM)
                    .withParameter(ProtocolKeys.CHAT_SESSION_ID, sessionId));

            assertTrue(joinResponse.isSuccess(), joinResponse.getMessage());
            @SuppressWarnings("unchecked")
            List<ChatMessage> history =
                    (List<ChatMessage>) joinResponse.getPayload(ProtocolKeys.CHAT_HISTORY);
            assertEquals(2, history.size(),
                    "the manager must receive everything that was said before joining");

            telAviv.closeChat(sessionId);
        }
    }

    @Test
    @DisplayName("The same employee cannot hold two conversations at once")
    public void anEmployeeCannotHoldTwoConversations() throws Exception {
        try (TestClient telAviv = new TestClient(TEL_AVIV_CASHIER);
             TestClient jerusalem = new TestClient(JERUSALEM_SELLER);
             TestClient jerusalemManager = new TestClient(JERUSALEM_MANAGER)) {

            String sessionId = telAviv.openChatWith(Branch.JERUSALEM);

            Response secondRequest = telAviv.requestChat(Branch.JERUSALEM);

            assertFalse(secondRequest.isSuccess());
            assertTrue(secondRequest.getMessage().contains("already in a conversation"),
                    "unexpected message: " + secondRequest.getMessage());

            telAviv.closeChat(sessionId);
        }
    }

    @Test
    @DisplayName("Closing a conversation frees both employees")
    public void closingAConversationFreesBothSides() throws Exception {
        try (TestClient telAviv = new TestClient(TEL_AVIV_CASHIER);
             TestClient jerusalem = new TestClient(JERUSALEM_SELLER)) {

            String sessionId = telAviv.openChatWith(Branch.JERUSALEM);
            ChatQueueManager queueManager = ChatQueueManager.getInstance();
            assertTrue(queueManager.isBusy(TEL_AVIV_CASHIER));
            assertTrue(queueManager.isBusy(JERUSALEM_SELLER));

            telAviv.closeChat(sessionId);

            assertFalse(queueManager.isBusy(TEL_AVIV_CASHIER));
            assertFalse(queueManager.isBusy(JERUSALEM_SELLER));
        }
    }

    @Test
    @DisplayName("A conversation with a branch nobody is connected from is queued")
    public void aRequestToAnEmptyBranchIsQueued() throws Exception {
        try (TestClient telAviv = new TestClient(TEL_AVIV_CASHIER)) {
            // Nobody of Jerusalem is connected in this test at all.
            Response response = telAviv.requestChat(Branch.JERUSALEM);

            assertFalse(response.isSuccess());
            assertTrue(response.getMessage().contains("queue"),
                    "unexpected message: " + response.getMessage());
        } finally {
            // The request stays in the queue and would notify this employee in a
            // later test, so it is dropped here.
            ChatQueueManager.getInstance().removeRequestsOf(TEL_AVIV_CASHIER);
        }
    }

    @Test
    @DisplayName("Opening a chat marks a session that the service can find")
    public void theServiceListsItsOpenSessions() throws Exception {
        try (TestClient telAviv = new TestClient(TEL_AVIV_CASHIER);
             TestClient jerusalem = new TestClient(JERUSALEM_SELLER)) {

            String sessionId = telAviv.openChatWith(Branch.JERUSALEM);

            boolean sessionIsListed = ChatService.getInstance().getOpenSessions().stream()
                    .anyMatch(info -> info.getSessionId().equals(sessionId));
            assertTrue(sessionIsListed, "the open conversation was not listed");

            telAviv.closeChat(sessionId);

            boolean stillListed = ChatService.getInstance().getOpenSessions().stream()
                    .anyMatch(info -> info.getSessionId().equals(sessionId));
            assertFalse(stillListed, "a closed conversation must not be listed");
        }
    }

    /**
     * Waits until the test server is really accepting connections.
     *
     * @throws InterruptedException if the wait is interrupted
     */
    private static void waitUntilServerAcceptsConnections() throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_SECONDS * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try (Socket probeSocket = new Socket("localhost", TEST_PORT)) {
                return;
            } catch (IOException notListeningYet) {
                Thread.sleep(50);
            }
        }
        fail("The test server did not start listening on port " + TEST_PORT);
    }

    /**
     * A small client built on the real {@code ServerConnection}, already logged
     * in as one of the demonstration employees.
     */
    private static final class TestClient implements AutoCloseable {

        /** The real client connection under test. */
        private final ServerConnection connection = new ServerConnection();

        /** The employee logged in on this connection. */
        private final Employee employee;

        /**
         * Connects and logs in.
         *
         * @param employeeNumber the demonstration employee to log in
         * @throws ChainStoreException if the connection or the login fails
         */
        private TestClient(String employeeNumber) throws ChainStoreException {
            connection.connect("localhost", TEST_PORT);
            Response response = connection.send(new Request(ActionType.LOGIN)
                    .withParameter(ProtocolKeys.EMPLOYEE_NUMBER, employeeNumber)
                    .withParameter(ProtocolKeys.PASSWORD, DataSeeder.getDemoPassword()));
            if (!response.isSuccess()) {
                throw new ChainStoreException(response.getMessage());
            }
            employee = (Employee) response.getPayload(ProtocolKeys.EMPLOYEE);
        }

        /**
         * Asks to open a conversation with a branch.
         *
         * @param targetBranch the branch to talk to
         * @return the answer of the server, successful or not
         * @throws ChainStoreException if the connection fails
         */
        private Response requestChat(Branch targetBranch) throws ChainStoreException {
            return send(new Request(ActionType.CHAT_REQUEST,
                    employee.getEmployeeNumber(), employee.getBranch())
                    .withParameter(ProtocolKeys.TARGET_BRANCH, targetBranch));
        }

        /**
         * Opens a conversation and fails the test if it could not be opened.
         *
         * @param targetBranch the branch to talk to
         * @return the identifier of the conversation
         * @throws ChainStoreException if the connection fails
         */
        private String openChatWith(Branch targetBranch) throws ChainStoreException {
            Response response = requestChat(targetBranch);
            if (!response.isSuccess()) {
                fail("the conversation should have opened but was refused: "
                        + response.getMessage());
            }
            return (String) response.getPayload(ProtocolKeys.CHAT_SESSION_ID);
        }

        /**
         * Sends one message inside a conversation.
         *
         * @param sessionId the conversation to write in
         * @param text      the text to send
         * @throws ChainStoreException if the connection fails
         */
        private void sendMessage(String sessionId, String text) throws ChainStoreException {
            send(new Request(ActionType.CHAT_SEND,
                    employee.getEmployeeNumber(), employee.getBranch())
                    .withParameter(ProtocolKeys.CHAT_SESSION_ID, sessionId)
                    .withParameter(ProtocolKeys.MESSAGE_TEXT, text));
        }

        /**
         * Closes a conversation.
         *
         * @param sessionId the conversation to close
         * @throws ChainStoreException if the connection fails
         */
        private void closeChat(String sessionId) throws ChainStoreException {
            send(new Request(ActionType.CHAT_CLOSE,
                    employee.getEmployeeNumber(), employee.getBranch())
                    .withParameter(ProtocolKeys.CHAT_SESSION_ID, sessionId));
        }

        /**
         * Sends one request and waits for its answer.
         *
         * @param request the request to send
         * @return the answer of the server
         * @throws ChainStoreException if the connection fails
         */
        private Response send(Request request) throws ChainStoreException {
            return connection.send(request);
        }

        @Override
        public void close() {
            connection.disconnect();
        }
    }
}
