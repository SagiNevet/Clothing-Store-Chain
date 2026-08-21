package test;

import common.exception.ChainStoreException;
import common.exception.InvalidPasswordPolicyException;
import common.model.Branch;
import common.model.Employee;
import common.model.PasswordPolicy;
import common.model.Role;
import server.core.ServerContext;
import server.service.BusinessTaskExecutor;
import server.service.EmployeeService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that independent business operations really do run at the same moment.
 * <p>
 * <b>This is the test for the example the lecturer gave twice:</b> registering a
 * new employee and selling a shirt have no dependency on one another, so there
 * is no reason for one to wait for the other. Saying so is easy; the test below
 * measures it.
 * </p>
 * <p>
 * <b>How overlap is measured.</b> Each operation records the moment it started
 * and the moment it ended. Two operations ran in parallel if one started before
 * the other ended. If the server had run them one after the other, the windows
 * would not overlap and the test would fail.
 * </p>
 */
public class ParallelBusinessOperationsTest {

    /** How long a test waits for the operations to finish. */
    private static final int WAIT_TIMEOUT_SECONDS = 10;

    /** A delay long enough that a sequential run could never look parallel. */
    private static final long WORK_DURATION_MILLIS = 400;

    /**
     * Makes sure the services are loaded before the tests run.
     * <p>
     * The other test classes start a whole server; this one only needs the
     * services, so it initialises them directly.
     * </p>
     *
     * @throws Exception if the data files cannot be read
     */
    @BeforeAll
    public static void prepareServices() throws Exception {
        server.storage.StoragePaths.createDirectoriesIfMissing();
        server.core.DataSeeder.seedIfEmpty();
        ServerContext.getInstance().initializeServices();
    }

    @Test
    @DisplayName("Registering an employee and selling a shirt run at the same moment")
    public void employeeRegistrationAndSaleOverlapInTime() throws Exception {
        BusinessTaskExecutor executor = ServerContext.getInstance().getBusinessTaskExecutor();
        assertTrue(executor.getPoolSize() >= 2,
                "the pool must have at least two threads for anything to run in parallel");

        AtomicLong registrationStart = new AtomicLong();
        AtomicLong registrationEnd = new AtomicLong();
        AtomicLong saleStart = new AtomicLong();
        AtomicLong saleEnd = new AtomicLong();
        CountDownLatch bothFinished = new CountDownLatch(2);

        // Both operations are submitted from two different threads, exactly as
        // two clients in two branches would submit them.
        startOperation("employee-registration", executor, registrationStart, registrationEnd,
                bothFinished);
        startOperation("shirt-sale", executor, saleStart, saleEnd, bothFinished);

        assertTrue(bothFinished.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "the two operations did not finish in time");

        boolean windowsOverlap = registrationStart.get() < saleEnd.get()
                && saleStart.get() < registrationEnd.get();
        assertTrue(windowsOverlap,
                "the two operations ran one after the other instead of together: "
                        + "registration [" + registrationStart.get() + ", " + registrationEnd.get()
                        + "], sale [" + saleStart.get() + ", " + saleEnd.get() + "]");
    }

    @Test
    @DisplayName("A failure inside a pooled operation reaches the caller unchanged")
    public void aBusinessFailureIsNotWrappedByThePool() {
        BusinessTaskExecutor executor = ServerContext.getInstance().getBusinessTaskExecutor();

        // The pool wraps whatever a task throws inside an ExecutionException. If
        // it were not unwrapped, a "not enough stock" failure would reach the
        // client as a meaningless wrapper with no message the user could act on.
        ChainStoreException failure = assertThrows(ChainStoreException.class,
                () -> executor.runAndWait("failing-operation", () -> {
                    throw new ChainStoreException("the original business message");
                }));

        assertEquals("the original business message", failure.getMessage());
    }

    @Test
    @DisplayName("An employee really is created by the pooled operation")
    public void theRegistrationActuallyCreatesAnAccount() throws Exception {
        EmployeeService employeeService = ServerContext.getInstance().getEmployeeService();
        String employeeNumber = "T" + System.nanoTime();

        Employee createdEmployee = ServerContext.getInstance().getBusinessTaskExecutor().runAndWait(
                "create-account", () -> employeeService.addEmployee(employeeNumber,
                        "Parallel Test Employee", "300000099", "050-9999999",
                        "12-345-999999", Branch.TEL_AVIV, Role.SELLER, "Chain@2026"));

        assertEquals(employeeNumber, createdEmployee.getEmployeeNumber());
        // The account travelled back without credentials, as every account must.
        assertEquals("", createdEmployee.getPasswordHash());
    }

    @Test
    @DisplayName("A weak password is refused before any account is created")
    public void aWeakPasswordIsRefused() throws Exception {
        EmployeeService employeeService = ServerContext.getInstance().getEmployeeService();
        String employeeNumber = "T" + System.nanoTime();

        assertThrows(InvalidPasswordPolicyException.class,
                () -> employeeService.addEmployee(employeeNumber, "Weak Password Employee",
                        "300000098", "050-9999998", "12-345-999998",
                        Branch.TEL_AVIV, Role.CASHIER, "abc"));

        // Nothing must have been written: the account does not exist.
        boolean accountExists = employeeService.getAllEmployees().stream()
                .anyMatch(employee -> employee.getEmployeeNumber().equals(employeeNumber));
        assertTrue(!accountExists, "a refused account must not be created");
    }

    @Test
    @DisplayName("A changed password policy applies to the next account")
    public void aChangedPolicyAppliesImmediately() throws Exception {
        EmployeeService employeeService = ServerContext.getInstance().getEmployeeService();
        PasswordPolicy originalPolicy = employeeService.getPasswordPolicy();

        try {
            employeeService.updatePasswordPolicy(new PasswordPolicy(12, true, true, true, true));

            assertThrows(InvalidPasswordPolicyException.class,
                    () -> employeeService.addEmployee("T" + System.nanoTime(), "Policy Test",
                            "300000097", "050-9999997", "12-345-999997",
                            Branch.TEL_AVIV, Role.CASHIER, "Chain@2026"));
        } finally {
            // The policy is shared by the whole server, so it is put back even if
            // the assertion above failed - otherwise the next test would inherit it.
            employeeService.updatePasswordPolicy(originalPolicy);
        }
    }

    /**
     * Submits one operation that takes a measurable amount of time and records
     * when it started and when it ended.
     *
     * @param operationName the name printed by the executor
     * @param executor      the pool the operation is submitted to
     * @param startedAt     where to record the starting moment
     * @param endedAt       where to record the ending moment
     * @param finishedLatch counted down when the operation is over
     */
    private void startOperation(String operationName, BusinessTaskExecutor executor,
                                AtomicLong startedAt, AtomicLong endedAt,
                                CountDownLatch finishedLatch) {
        new Thread(() -> {
            try {
                executor.runAndWait(operationName, () -> {
                    startedAt.set(System.currentTimeMillis());
                    // Stands for the real work: reading a file, changing it and
                    // writing it back. The duration is what makes the overlap
                    // measurable rather than a matter of luck.
                    Thread.sleep(WORK_DURATION_MILLIS);
                    endedAt.set(System.currentTimeMillis());
                    return null;
                });
            } catch (ChainStoreException operationFailure) {
                System.err.println("[Test] " + operationName + " failed: " + operationFailure);
            } finally {
                finishedLatch.countDown();
            }
        }, "submitter-" + operationName).start();
    }

}
