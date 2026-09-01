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

public class ServerLoginIntegrationTest {

    private static final int TEST_PORT = 5051;

    private static final int RESPONSE_TIMEOUT_MILLIS = 5000;

    private static final int STARTUP_TIMEOUT_MILLIS = 5000;

    private static final String MANAGER_NUMBER = "1001";

    private static final String CASHIER_NUMBER = "1002";

    private static ChainServer server;

    private static Thread serverThread;

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
        
        crashingComputer.close();

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

    private static final class TestClient implements AutoCloseable {

        private final Socket socket;

        private final ObjectOutputStream outputStream;

        private final ObjectInputStream inputStream;

        private TestClient() throws IOException {
            this.socket = new Socket("localhost", TEST_PORT);
            this.socket.setSoTimeout(RESPONSE_TIMEOUT_MILLIS);
            
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.outputStream.flush();
            this.inputStream = new ObjectInputStream(socket.getInputStream());
        }

        private Response login(String employeeNumber, String password) throws Exception {
            return send(new Request(ActionType.LOGIN)
                    .withParameter(ProtocolKeys.EMPLOYEE_NUMBER, employeeNumber)
                    .withParameter(ProtocolKeys.PASSWORD, password));
        }

        private Response logout() throws Exception {
            return send(new Request(ActionType.LOGOUT));
        }

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
            }
        }
    }
}
