package server.service;

import common.exception.ChainStoreException;
import common.util.AppConfig;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs the business operations of the server inside a pool of a fixed number of
 * threads.
 * <p>
 * <b>Why a pool at all, when the server already has a thread per client?</b>
 * The thread per client exists so that one slow connection cannot block
 * another. This pool answers a different question: <i>how many business
 * operations may be in progress at the same moment</i>. Without it, a hundred
 * connected employees pressing "sell" together would mean a hundred threads all
 * touching the files at once. {@link Executors#newFixedThreadPool} puts a
 * ceiling on that: the extra work waits politely in a queue instead of
 * drowning the machine.
 * </p>
 * <p>
 * <b>Note what is not limited:</b> the number of clients that may connect. The
 * accept loop keeps accepting without any limit, exactly as the requirement
 * demands. Only the heavy operations are metered.
 * </p>
 * <p>
 * <b>The parallelism this makes visible.</b> Registering a new employee and
 * selling a shirt have nothing to do with one another - different files,
 * different data, no shared state. Handed to this pool they run on two
 * different threads at the same instant, and the console prints both thread
 * names with overlapping timestamps to prove it.
 * </p>
 * <p>
 * <b>The pool belongs to the server, not to the program.</b> It was a singleton
 * at first, and that turned out to be a real mistake: an
 * {@link ExecutorService} that has been shut down can <b>never</b> be started
 * again, so a second server started in the same program received a dead pool
 * and every business operation was rejected. The pool is therefore created by
 * {@code ServerContext} when a server starts and thrown away when it stops.
 * There is still exactly one pool per running server, which is what the ceiling
 * needs.
 * </p>
 */
public final class BusinessTaskExecutor {

    /** Configuration key holding how many business operations may run at once. */
    private static final String CONFIG_KEY_POOL_SIZE = "business.threadPool.size";

    /** The pool size used when the configuration file is missing. */
    private static final int DEFAULT_POOL_SIZE = 4;

    /**
     * Configuration key holding an artificial delay added to every business
     * operation, used only to make the parallelism visible during the defence.
     */
    private static final String CONFIG_KEY_DEMO_DELAY = "demo.taskDelayMillis";

    /** No artificial delay, which is the value used in normal operation. */
    private static final int NO_DEMO_DELAY = 0;

    /** How long a caller waits for its operation before giving up. */
    private static final int TASK_TIMEOUT_SECONDS = 30;

    /** How long the pool is given to finish its work when the server stops. */
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    /** The pool the business operations run in. */
    private final ExecutorService businessPool;

    /** How many operations may run at the same moment. */
    private final int poolSize;

    /** The artificial delay added to each operation, zero in normal operation. */
    private final int demonstrationDelayMillis;

    /**
     * Creates the pool with the size taken from the configuration file.
     */
    public BusinessTaskExecutor() {
        AppConfig configuration = AppConfig.getInstance();
        this.poolSize = configuration.getInt(CONFIG_KEY_POOL_SIZE, DEFAULT_POOL_SIZE);
        this.demonstrationDelayMillis =
                configuration.getInt(CONFIG_KEY_DEMO_DELAY, NO_DEMO_DELAY);
        this.businessPool = Executors.newFixedThreadPool(poolSize);
        System.out.println("[Executor] business thread pool ready with " + poolSize
                + " threads" + (demonstrationDelayMillis > NO_DEMO_DELAY
                ? ", demonstration delay " + demonstrationDelayMillis + " ms" : ""));
    }

    /**
     * Runs one business operation in the pool and waits for its result.
     * <p>
     * The calling thread - the one serving this client - blocks here while a
     * pool thread does the work. That is deliberate: the client is waiting for
     * an answer anyway, and blocking is what makes the pool a real ceiling. If
     * every thread of the pool is busy, this call simply waits its turn.
     * </p>
     *
     * @param taskDescription a short description printed to the console, so the
     *                        parallelism can be seen during the defence
     * @param task            the operation to perform
     * @param <T>             the type of result the operation produces
     * @return whatever the operation returned
     * @throws ChainStoreException if the operation failed for a business reason,
     *                             timed out, or the pool was interrupted
     */
    public <T> T runAndWait(String taskDescription, Callable<T> task)
            throws ChainStoreException {
        Future<T> pendingResult = businessPool.submit(() -> runWithTrace(taskDescription, task));

        try {
            return pendingResult.get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (ExecutionException taskFailed) {
            // The task threw. The exception it threw is wrapped inside, so it is
            // unwrapped here and handed on unchanged - otherwise a "not enough
            // stock" failure would reach the client as a meaningless wrapper.
            Throwable originalFailure = taskFailed.getCause();
            if (originalFailure instanceof ChainStoreException) {
                throw (ChainStoreException) originalFailure;
            }
            throw new ChainStoreException("The operation failed: "
                    + originalFailure.getMessage(), originalFailure);

        } catch (TimeoutException tookTooLong) {
            pendingResult.cancel(true);
            throw new ChainStoreException("The operation " + taskDescription
                    + " did not finish within " + TASK_TIMEOUT_SECONDS + " seconds");

        } catch (InterruptedException waitInterrupted) {
            // The interrupted flag is restored so that whoever interrupted this
            // thread can still see that it happened.
            Thread.currentThread().interrupt();
            throw new ChainStoreException("The operation " + taskDescription
                    + " was interrupted", waitInterrupted);
        }
    }

    /**
     * Runs one operation and prints when it started and when it ended.
     * <p>
     * The printed lines are what makes the parallelism visible: two operations
     * running together show two different thread names, and the start of the
     * second one appears before the end of the first.
     * </p>
     *
     * @param taskDescription a short description of the operation
     * @param task            the operation to perform
     * @param <T>             the type of result the operation produces
     * @return whatever the operation returned
     * @throws Exception if the operation failed
     */
    private <T> T runWithTrace(String taskDescription, Callable<T> task) throws Exception {
        String threadName = Thread.currentThread().getName();
        long startedAt = System.currentTimeMillis();
        System.out.println("[Executor] START " + taskDescription + " on " + threadName);

        try {
            // Only ever above zero when the configuration file asks for it, so
            // that the parallelism can be seen with the naked eye during the
            // defence. It is zero in normal operation.
            if (demonstrationDelayMillis > NO_DEMO_DELAY) {
                Thread.sleep(demonstrationDelayMillis);
            }
            return task.call();

        } finally {
            long durationMillis = System.currentTimeMillis() - startedAt;
            System.out.println("[Executor] END   " + taskDescription + " on " + threadName
                    + " after " + durationMillis + " ms");
        }
    }

    /**
     * Returns how many business operations may run at the same moment.
     *
     * @return the size of the pool
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Indicates whether this pool has already been shut down.
     * <p>
     * A pool that was shut down can never be revived, so the caller has to
     * create a new one instead.
     * </p>
     *
     * @return {@code true} once {@link #shutdown()} has been called
     */
    public boolean isShutdown() {
        return businessPool.isShutdown();
    }

    /**
     * Stops the pool when the server shuts down.
     * <p>
     * {@code shutdown} lets the operations already in progress finish, so a
     * sale that was halfway through is not abandoned. Only if they do not
     * finish in time is {@code shutdownNow} used to interrupt them.
     * </p>
     */
    public void shutdown() {
        businessPool.shutdown();
        try {
            if (!businessPool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                System.err.println("[Executor] some operations did not finish in time,"
                        + " interrupting them");
                businessPool.shutdownNow();
            }
        } catch (InterruptedException waitInterrupted) {
            businessPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
