package test;

import common.model.Branch;
import common.model.Employee;
import common.model.Role;
import common.protocol.ActionType;
import common.protocol.ProtocolKeys;
import common.protocol.Request;
import common.protocol.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.core.ChainServer;
import server.core.DataSeeder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Starts a real server on a port of its own and drives it through real sockets.
 * <p>
 * These are integration tests rather than unit tests: they prove that the
 * accept loop, the thread per client, the object streams, the command factory
 * and the session table all work together. The duplicate login rule in
 * particular cannot be proven any other way, because it only means anything
 * when two separate connections exist at the same moment.
 * </p>
 * <p>
 * The server runs on port {@value #TEST_PORT} so that it never collides with a
 * server the group left running on the normal port.
 * </p>
 */
public class ServerLoginIntegrationTest {

    /** The port used by the test server. */
    private static final int TEST_PORT = 5051;

    /** How long a client waits for an answer before the test gives up. */
    private static final int RESPONSE_TIMEOUT_MILLIS = 5000;

    /** How long the test waits for the server to start listening. */
    private static final int STARTUP_TIMEOUT_MILLIS = 5000;

    /** The employee number of the demonstration shift manager of Tel Aviv. */
    private static final String MANAGER_NUMBER = "1001";

    /** The employee number of the demonstration cashier of Tel Aviv. */
    private static final String CASHIER_NUMBER = "1002";

    /** The server under test. */
    private static ChainServer server;

    /** The thread the blocking accept loop runs on. */
    private static Thread serverThread;

    /**
     * Starts the server once for the whole test class and waits until it is
     * really accepting connections.
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
        }, "test-server");
        serverThread.setDaemon(true);
        serverThread.start();

        waitUntilServerAcceptsConnections();
    }

    /**
     * Stops the server after every test of the class has finished.
     *
     * @throws InterruptedException if the wait for the thread is interrupted
     */
    @AfterAll
    public static void stopServer() throws InterruptedException {
        server.stop();
        serverThread.join(STARTUP_TIMEOUT_MILLIS);
    }

    @Test
    @DisplayName("A correct employee number and password open a session")
    public void correctCredentialsAreAccepted() throws Exception {
        try (TestClient client = new TestClient()) {
            Response response = client.login(MANAGER_NUMBER, DataSeeder.getDemoPassword());

            assertTrue(response.isSuccess(), "the login should have succeeded");
            Employee loggedInEmployee = (Employee) response.getPayload(ProtocolKeys.EMPLOYEE);
            assertNotNull(loggedInEmployee);
            assertEquals(Role.SHIFT_MANAGER, loggedInEmployee.getRole());
            assertEquals(Branch.TEL_AVIV, loggedInEmployee.getBranch());
        }
    }

    @Test
    @DisplayName("The employee sent to the client carries no password hash")
    public void theClientNeverReceivesCredentials() throws Exception {
        try (TestClient client = new TestClient()) {
            Response response = client.login(CASHIER_NUMBER, DataSeeder.getDemoPassword());

            Employee loggedInEmployee = (Employee) response.getPayload(ProtocolKeys.EMPLOYEE);
            assertEquals("", loggedInEmployee.getPasswordHash());
            assertEquals("", loggedInEmployee.getPasswordSalt());
        }
    }

    @Test
    @DisplayName("A wrong password is refused with the same message as an unknown user")
    public void wrongPasswordIsRefused() throws Exception {
        try (TestClient client = new TestClient()) {
            Response wrongPasswordResponse = client.login(MANAGER_NUMBER, "NotThePassword1");
            assertFalse(wrongPasswordResponse.isSuccess());

            Response unknownUserResponse = client.login("9999", "NotThePassword1");
            assertFalse(unknownUserResponse.isSuccess());

            // The two answers must be identical, otherwise the error message
            // itself would reveal which employee numbers exist.
            assertEquals(wrongPasswordResponse.getMessage(), unknownUserResponse.getMessage());
        }
    }

    @Test
    @DisplayName("The same employee cannot be logged in from two computers at once")
    public void duplicateLoginIsRefused() throws Exception {
        try (TestClient firstComputer = new TestClient();
             TestClient secondComputer = new TestClient()) {

            Response firstLogin = firstComputer.login(MANAGER_NUMBER, DataSeeder.getDemoPassword());
            Response secondLogin = secondComputer.login(MANAGER_NUMBER, DataSeeder.getDemoPassword());

            assertTrue(firstLogin.isSuccess(), "the first computer should be let in");
            assertFalse(secondLogin.isSuccess(), "the second computer must be refused");
            assertTrue(secondLogin.getMessage().contains("already logged in"),
                    "the message should explain the refusal, but was: " + secondLogin.getMessage());
        }
    }

    @Test
    @DisplayName("After a logout the same employee can log in from another computer")
    public void logoutReleasesTheSession() throws Exception {
        try (TestClient firstComputer = new TestClient();
             TestClient secondComputer = new TestClient()) {

            assertTrue(firstComputer.login(CASHIER_NUMBER, DataSeeder.getDemoPassword()).isSuccess());
            assertTrue(firstComputer.logout().isSuccess());

            Response secondLogin = secondComputer.login(CASHIER_NUMBER, DataSeeder.getDemoPassword());

            assertTrue(secondLogin.isSuccess(),
                    "the session should have been released, but got: " + secondLogin.getMessage());
        }
    }

    @Test
    @DisplayName("Closing a connection releases the session even without a logout")
    public void aDroppedConnectionReleasesTheSession() throws Exception {
        TestClient crashingComputer = new TestClient();
        assertTrue(crashingComputer.login(CASHIER_NUMBER, DataSeeder.getDemoPassword()).isSuccess());
        // Simulates a client that was killed instead of logging out properly.
        crashingComputer.close();

        // The server notices the closed socket on its own thread, so the test
        // gives it a moment before checking that the session was released.
        Response secondLogin = loginWithRetry(CASHIER_NUMBER);

        assertTrue(secondLogin.isSuccess(),
                "the session of a dropped connection must be released, but got: "
                        + secondLogin.getMessage());
    }

    @Test
    @DisplayName("An action sent before logging in is refused")
    public void actionsBeforeLoginAreRefused() throws Exception {
        try (TestClient client = new TestClient()) {
            Response response = client.send(new Request(ActionType.GET_INVENTORY));

            assertFalse(response.isSuccess());
            assertTrue(response.getMessage().contains("log in"),
                    "unexpected message: " + response.getMessage());
        }
    }

    @Test
    @DisplayName("A cashier is refused an action reserved for a shift manager")
    public void cashierIsRefusedAManagerAction() throws Exception {
        try (TestClient client = new TestClient()) {
            Employee cashier = (Employee) client
                    .login(CASHIER_NUMBER, DataSeeder.getDemoPassword())
                    .getPayload(ProtocolKeys.EMPLOYEE);

            Response response = client.send(new Request(ActionType.GET_EMPLOYEES,
                    cashier.getEmployeeNumber(), cashier.getBranch()));

            assertFalse(response.isSuccess());
            assertTrue(response.getMessage().contains("not allowed"),
                    "unexpected message: " + response.getMessage());
        }
    }

    /**
     * Tries to log in several times, giving the server a moment to notice a
     * connection that was dropped without a logout.
     *
     * @param employeeNumber the employee to log in
     * @return the last answer received from the server
     * @throws Exception if the connection itself fails
     */
    private Response loginWithRetry(String employeeNumber) throws Exception {
        Response lastResponse = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try (TestClient client = new TestClient()) {
                lastResponse = client.login(employeeNumber, DataSeeder.getDemoPassword());
                if (lastResponse.isSuccess()) {
                    return lastResponse;
                }
            }
            Thread.sleep(100);
        }
        return lastResponse;
    }

    /**
     * Waits until the server answers a connection attempt, so the tests never
     * start before the accept loop is ready.
     *
     * @throws InterruptedException if the wait is interrupted
     */
    private static void waitUntilServerAcceptsConnections() throws InterruptedException {
        long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MILLIS;
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
     * A very small client used by the tests: it opens a socket, sends requests
     * and reads the answers.
     * <p>
     * It reads the answers on the calling thread, which is enough for these
     * tests because every request here has exactly one answer and no events are
     * pushed yet. The real client of stage 3 will read on a separate thread.
     * </p>
     */
    private static final class TestClient implements AutoCloseable {

        /** The socket connected to the test server. */
        private final Socket socket;

        /** The stream used to send requests. */
        private final ObjectOutputStream outputStream;

        /** The stream used to read answers. */
        private final ObjectInputStream inputStream;

        /**
         * Connects to the test server.
         *
         * @throws IOException if the connection cannot be opened
         */
        private TestClient() throws IOException {
            this.socket = new Socket("localhost", TEST_PORT);
            this.socket.setSoTimeout(RESPONSE_TIMEOUT_MILLIS);
            // Same order as the server: output first and flushed, then input.
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.outputStream.flush();
            this.inputStream = new ObjectInputStream(socket.getInputStream());
        }

        /**
         * Sends a login request.
         *
         * @param employeeNumber the employee number to send
         * @param password       the password to send
         * @return the answer of the server
         * @throws Exception if the connection fails
         */
        private Response login(String employeeNumber, String password) throws Exception {
            return send(new Request(ActionType.LOGIN)
                    .withParameter(ProtocolKeys.EMPLOYEE_NUMBER, employeeNumber)
                    .withParameter(ProtocolKeys.PASSWORD, password));
        }

        /**
         * Sends a logout request.
         *
         * @return the answer of the server
         * @throws Exception if the connection fails
         */
        private Response logout() throws Exception {
            return send(new Request(ActionType.LOGOUT));
        }

        /**
         * Sends one request and waits for its answer.
         *
         * @param request the request to send
         * @return the answer of the server
         * @throws Exception if the connection fails or the answer never arrives
         */
        private Response send(Request request) throws Exception {
            outputStream.writeObject(request);
            outputStream.flush();
            outputStream.reset();
            return (Response) inputStream.readObject();
        }

        @Override
        public void close() {
            try {
                socket.close();
            } catch (IOException ignoredFailure) {
                // The test is finished with this connection anyway.
            }
        }
    }
}
