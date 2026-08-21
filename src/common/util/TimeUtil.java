package common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Formats dates and times in one consistent way across the whole system.
 * <p>
 * Every log line, every chat message and every generated report file uses the
 * formats defined here, so the timestamps of two different features can always
 * be compared by eye.
 * </p>
 * <p>
 * {@link DateTimeFormatter} is immutable and thread safe, which is why the
 * formatters can be shared as {@code static final} constants between all the
 * threads of the server. The old {@code SimpleDateFormat} class is <b>not</b>
 * thread safe and would have needed a new instance per call.
 * </p>
 */
public final class TimeUtil {

    /** Human readable timestamp used inside log lines and chat messages. */
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** Timestamp used inside file names, with no characters that Windows forbids. */
    private static final DateTimeFormatter FILE_NAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Prevents instantiation. This class only exposes static utility methods.
     */
    private TimeUtil() {
    }

    /**
     * Returns the current time formatted for display.
     *
     * @return the current time as {@code dd/MM/yyyy HH:mm:ss}
     */
    public static String nowForDisplay() {
        return DISPLAY_FORMATTER.format(LocalDateTime.now());
    }

    /**
     * Returns the current time formatted for use inside a file name.
     *
     * @return the current time as {@code yyyyMMdd_HHmmss}
     */
    public static String nowForFileName() {
        return FILE_NAME_FORMATTER.format(LocalDateTime.now());
    }

    /**
     * Formats a given point in time for display.
     *
     * @param pointInTime the time to format, must not be {@code null}
     * @return the given time as {@code dd/MM/yyyy HH:mm:ss}
     */
    public static String formatForDisplay(LocalDateTime pointInTime) {
        return DISPLAY_FORMATTER.format(pointInTime);
    }
}
