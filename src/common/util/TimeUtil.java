package common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final DateTimeFormatter FILE_NAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private TimeUtil() {
    }

    public static String nowForDisplay() {
        return DISPLAY_FORMATTER.format(LocalDateTime.now());
    }

    public static String nowForFileName() {
        return FILE_NAME_FORMATTER.format(LocalDateTime.now());
    }

    public static String formatForDisplay(LocalDateTime pointInTime) {
        return DISPLAY_FORMATTER.format(pointInTime);
    }
}
