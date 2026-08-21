package test;

import client.net.ClientEventDispatcher;
import client.net.ServerEventListener;
import client.net.ServerConnection;
import common.exception.ConnectionException;
import common.model.Branch;
import common.model.Employee;
import common.model.Role;
import common.protocol.ActionType;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import common.protocol.ServerEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.core.ChainServer;
import server.core.DataSeeder;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the network layer of the client against a real server.
 * <p>
 * These tests cover the part of the client that has no window: opening the
 * connection, matching every answer to the request that is waiting for it, and
 * delivering pushed events to the screens. The Swing screens themselves are not
 * tested automatically - they are demonstrated by hand - but everything they
 * rely on is.
 * </p>
 */
public class ClientConnectionTest {

    /** The port used by the server of this test class. */
    private static final int TEST_PORT = 5052;

    /** How long a test waits for something that happens on another thread. */
    private static final int WAIT_TIMEOUT_SECONDS = 5;

    /** The employee number of the demonstration shift manager of Tel Aviv. */
    private static final String MANAGER_NUMBER = "1001";

    /** The employee number of the demonstration seller of Jerusalem. */
    private static final String SELLER_NUMBER = "2002";

    /** The server under test. */
    private static ChainServer server;

    /** The thread the blocking accept loop runs on. */
    private static Thread serverThread;

    /** The connection under test, closed after every test method. */
    private ServerConnection connection;

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
        }, "test-server-client-suite");
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

    /**
     * Closes the connection of the test that has just finished, so the next
     * test starts from a clean state and no session is left open.
     */
    @AfterEach
    public void closeConnection() {
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
    }

    @Test
    @DisplayName("Connecting starts the listener thread")
    public void connectingStartsTheListener() throws Exception {
        connection = new ServerConnection();

        connection.connect("localhost", TEST_PORT);

        assertTrue(connection.isConnected());
    }

    @Test
    @DisplayName("Connecting to a port nobody listens on fails with a clear message")
    public void connectingToAClosedPortFails() {
        connection = new ServerConnection();

        ConnectionException failure = assertThrows(ConnectionException.class,
                () -> connection.connect("localhost", 1));

        assertTrue(failure.getMessage().contains("Could not connect"),
                "unexpected message: " + failure.getMessage());
    }

    @Test
    @DisplayName("A request receives the answer that belongs to it")
    public void aRequestReceivesItsOwnAnswer() throws Exception {
        connection = new ServerConnection();
        connection.connect("localhost", TEST_PORT);

        Request loginRequest = new Request(ActionType.LOGIN)
                .withParameter(ProtocolKeys.EMPLOYEE_NUMBER, MANAGER_NUMBER)
                .withParameter(ProtocolKeys.PASSWORD, DataSeeder.getDemoPassword());
        Response response = connection.send(loginRequest);

        assertEquals(loginRequest.getRequestId(), response.getRequestId(),
                "the answer must carry the number of the request it answers");
        assertTrue(response.isSuccess());
        Employee employee = (Employee) response.getPayload(ProtocolKeys.EMPLOYEE);
        assertNotNull(employee);
        assertEquals(Role.SHIFT_MANAGER, employee.getRole());
    }

    @Test
    @DisplayName("Several requests sent at once each receive their own answer")
    public void concurrentRequestsAreMatchedCorrectly() throws Exception {
        connection = new ServerConnection();
        connection.connect("localhost", TEST_PORT);
        connection.send(new Request(ActionType.LOGIN)
                .withParameter(ProtocolKeys.EMPLOYEE_NUMBER, MANAGER_NUMBER)
                .withParameter(ProtocolKeys.PASSWORD, DataSeeder.getDemoPassword()));

        int numberOfRequests = 8;
        List<Request> sentRequests = new ArrayList<>();
        List<Response> receivedResponses = new ArrayList<>();
        CountDownLatch allRequestsFinished = new CountDownLatch(numberOfRequests);

        // Every thread sends its own request and stores the answer it received.
        // This is what proves the request number really does the matching: if the
        // answers were simply read in arrival order, the pairs would come out mixed.
        for (int requestIndex = 0; requestIndex < numberOfRequests; requestIndex++) {
            Request request = new Request(ActionType.GET_INVENTORY,
                    MANAGER_NUMBER, Branch.TEL_AVIV);
            synchronized (sentRequests) {
                sentRequests.add(request);
            }
            new Thread(() -> {
                try {
                    Response response = connection.send(request);
                    synchronized (receivedResponses) {
                        receivedResponses.add(response);
                    }
                } catch (ConnectionException sendFailure) {
                    fail("a concurrent request failed: " + sendFailure.getMessage());
                } finally {
                    allRequestsFinished.countDown();
                }
            }, "concurrent-request-" + requestIndex).start();
        }

        assertTrue(allRequestsFinished.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "not every request received an answer in time");
        assertEquals(numberOfRequests, receivedResponses.size());

        // Each answer must carry the number of one of the requests, and no two
        // answers may carry the same number.
        List<Long> answeredRequestIds = new ArrayList<>();
        for (Response response : receivedResponses) {
            assertFalse(answeredRequestIds.contains(response.getRequestId()),
                    "the same request was answered twice");
            answeredRequestIds.add(response.getRequestId());
        }
        for (Request request : sentRequests) {
            assertTrue(answeredRequestIds.contains(request.getRequestId()),
                    "request #" + request.getRequestId() + " never received an answer");
        }
    }

    @Test
    @DisplayName("Two clients of two different branches are served at the same time")
    public void twoBranchesAreServedTogether() throws Exception {
        ServerConnection telAvivClient = new ServerConnection();
        ServerConnection jerusalemClient = new ServerConnection();
        try {
            telAvivClient.connect("localhost", TEST_PORT);
            jerusalemClient.connect("localhost", TEST_PORT);

            Response telAvivLogin = telAvivClient.send(loginRequestFor(MANAGER_NUMBER));
            Response jerusalemLogin = jerusalemClient.send(loginRequestFor(SELLER_NUMBER));

            assertTrue(telAvivLogin.isSuccess(), telAvivLogin.getMessage());
            assertTrue(jerusalemLogin.isSuccess(), jerusalemLogin.getMessage());

            // This is the demonstration scope of the project: two branches, two
            // clients, one server, both connected and served at the same moment.
            Employee telAvivEmployee = (Employee) telAvivLogin.getPayload(ProtocolKeys.EMPLOYEE);
            Employee jerusalemEmployee =
                    (Employee) jerusalemLogin.getPayload(ProtocolKeys.EMPLOYEE);
            assertEquals(Branch.TEL_AVIV, telAvivEmployee.getBranch());
            assertEquals(Branch.JERUSALEM, jerusalemEmployee.getBranch());
        } finally {
            telAvivClient.disconnect();
            jerusalemClient.disconnect();
        }
    }

    /**
     * Builds a login request for one of the demonstration accounts.
     *
     * @param employeeNumber the employee number to log in
     * @return the request, ready to be sent
     */
    private Request loginRequestFor(String employeeNumber) {
        return new Request(ActionType.LOGIN)
                .withParameter(ProtocolKeys.EMPLOYEE_NUMBER, employeeNumber)
                .withParameter(ProtocolKeys.PASSWORD, DataSeeder.getDemoPassword());
    }

    @Test
    @DisplayName("Sending before connecting fails instead of hanging")
    public void sendingWithoutAConnectionFails() {
        connection = new ServerConnection();

        assertThrows(ConnectionException.class,
                () -> connection.send(new Request(ActionType.GET_INVENTORY)));
    }

    @Test
    @DisplayName("Disconnecting stops the listener thread")
    public void disconnectingStopsTheListener() throws Exception {
        connection = new ServerConnection();
        connection.connect("localhost", TEST_PORT);

        connection.disconnect();

        assertFalse(connection.isConnected());
        assertThrows(ConnectionException.class,
                () -> connection.send(new Request(ActionType.GET_INVENTORY)));
    }

    @Test
    @DisplayName("A pushed event reaches every subscribed screen")
    public void pushedEventsReachTheSubscribers() throws Exception {
        ClientEventDispatcher dispatcher = new ClientEventDispatcher();
        CountDownLatch eventReceived = new CountDownLatch(1);
        List<ServerEvent> eventsSeenByTheScreen = new ArrayList<>();

        dispatcher.subscribe(event -> {
            eventsSeenByTheScreen.add(event);
            eventReceived.countDown();
        });

        dispatcher.publish(new ServerEvent(EventType.INVENTORY_UPDATED));

        assertTrue(eventReceived.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "the event never reached the screen");
        assertEquals(1, eventsSeenByTheScreen.size());
        assertEquals(EventType.INVENTORY_UPDATED,
                eventsSeenByTheScreen.get(0).getEventType());
    }

    @Test
    @DisplayName("A screen that unsubscribed stops receiving events")
    public void unsubscribedScreensStopReceivingEvents() throws Exception {
        ClientEventDispatcher dispatcher = new ClientEventDispatcher();
        List<ServerEvent> eventsSeenByTheScreen = new ArrayList<>();
        ServerEventListener screen = eventsSeenByTheScreen::add;

        dispatcher.subscribe(screen);
        dispatcher.unsubscribe(screen);
        dispatcher.publish(new ServerEvent(EventType.CUSTOMERS_UPDATED));

        // The dispatcher delivers on the Swing thread, so the test waits for that
        // queue to drain before deciding that nothing arrived.
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(0, eventsSeenByTheScreen.size());
        assertEquals(0, dispatcher.getListenerCount());
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
}
