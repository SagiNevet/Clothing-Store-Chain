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

public class ParallelBusinessOperationsTest {

    private static final int WAIT_TIMEOUT_SECONDS = 10;

    private static final long WORK_DURATION_MILLIS = 400;

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

            employeeService.updatePasswordPolicy(originalPolicy);
        }
    }

    private void startOperation(String operationName, BusinessTaskExecutor executor,
                                AtomicLong startedAt, AtomicLong endedAt,
                                CountDownLatch finishedLatch) {
        new Thread(() -> {
            try {
                executor.runAndWait(operationName, () -> {
                    startedAt.set(System.currentTimeMillis());
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
