package common.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Produces unique identifiers for objects created while the server is running:
 * sales, chat sessions, chat requests and network requests.
 * <p>
 * <b>This class is a thread safety exercise in itself.</b> Several client
 * threads ask for an identifier at the same moment. A plain
 * {@code counter++} is not one single action - it reads the value, adds one and
 * writes it back - so two threads can read the same value and produce the same
 * identifier. {@link AtomicLong#incrementAndGet()} performs all three steps as
 * one indivisible operation at the processor level, which removes the race
 * without any lock at all.
 * </p>
 */
public final class IdGenerator {

    /** Counter behind every generated sale identifier. */
    private static final AtomicLong SALE_COUNTER = new AtomicLong(0);

    /** Counter behind every generated chat session identifier. */
    private static final AtomicLong CHAT_SESSION_COUNTER = new AtomicLong(0);

    /** Counter behind every generated client request identifier. */
    private static final AtomicLong REQUEST_COUNTER = new AtomicLong(0);

    /** Prefix of a sale identifier. */
    private static final String SALE_PREFIX = "SALE-";

    /** Prefix of a chat session identifier. */
    private static final String CHAT_SESSION_PREFIX = "CHAT-";

    /**
     * Prevents instantiation. This class only exposes static utility methods.
     */
    private IdGenerator() {
    }

    /**
     * Produces the next unique sale identifier.
     * <p>
     * The current time is part of the identifier so that identifiers stay
     * unique across restarts of the server, when the counter starts from zero
     * again.
     * </p>
     *
     * @return a unique sale identifier, for example {@code SALE-20260821_143012-7}
     */
    public static String nextSaleId() {
        return SALE_PREFIX + TimeUtil.nowForFileName() + "-" + SALE_COUNTER.incrementAndGet();
    }

    /**
     * Produces the next unique chat session identifier.
     *
     * @return a unique chat session identifier, for example {@code CHAT-20260821_143012-3}
     */
    public static String nextChatSessionId() {
        return CHAT_SESSION_PREFIX + TimeUtil.nowForFileName() + "-"
                + CHAT_SESSION_COUNTER.incrementAndGet();
    }

    /**
     * Produces the next request number, used by a client to match an incoming
     * response to the request that is waiting for it.
     *
     * @return a request number that is unique inside one running client
     */
    public static long nextRequestId() {
        return REQUEST_COUNTER.incrementAndGet();
    }
}
