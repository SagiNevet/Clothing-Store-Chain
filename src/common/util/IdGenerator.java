package common.util;

import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {

    private static final AtomicLong SALE_COUNTER = new AtomicLong(0);

    private static final AtomicLong CHAT_SESSION_COUNTER = new AtomicLong(0);

    private static final AtomicLong REQUEST_COUNTER = new AtomicLong(0);

    private static final String SALE_PREFIX = "SALE-";

    private static final String CHAT_SESSION_PREFIX = "CHAT-";

    private IdGenerator() {
    }

    public static String nextSaleId() {
        return SALE_PREFIX + TimeUtil.nowForFileName() + "-" + SALE_COUNTER.incrementAndGet();
    }

    public static String nextChatSessionId() {
        return CHAT_SESSION_PREFIX + TimeUtil.nowForFileName() + "-"
                + CHAT_SESSION_COUNTER.incrementAndGet();
    }

    public static long nextRequestId() {
        return REQUEST_COUNTER.incrementAndGet();
    }
}
