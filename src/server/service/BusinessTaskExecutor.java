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

public final class BusinessTaskExecutor {

    private static final String CONFIG_KEY_POOL_SIZE = "business.threadPool.size";

    private static final int DEFAULT_POOL_SIZE = 4;

    private static final String CONFIG_KEY_DEMO_DELAY = "demo.taskDelayMillis";

    private static final int NO_DEMO_DELAY = 0;

    private static final int TASK_TIMEOUT_SECONDS = 30;

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final ExecutorService businessPool;

    private final int poolSize;

    private final int demonstrationDelayMillis;

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

    public <T> T runAndWait(String taskDescription, Callable<T> task)
            throws ChainStoreException {
        Future<T> pendingResult = businessPool.submit(() -> runWithTrace(taskDescription, task));

        try {
            return pendingResult.get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (ExecutionException taskFailed) {

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
            
            Thread.currentThread().interrupt();
            throw new ChainStoreException("The operation " + taskDescription
                    + " was interrupted", waitInterrupted);
        }
    }

    private <T> T runWithTrace(String taskDescription, Callable<T> task) throws Exception {
        String threadName = Thread.currentThread().getName();
        long startedAt = System.currentTimeMillis();
        System.out.println("[Executor] START " + taskDescription + " on " + threadName);

        try {

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

    public int getPoolSize() {
        return poolSize;
    }

    public boolean isShutdown() {
        return businessPool.isShutdown();
    }

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
