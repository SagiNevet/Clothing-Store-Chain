package server.storage;

import common.model.LogCategory;
import common.model.LogEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Appends log lines to the text file of their category.
 * <p>
 * The logs are plain text and not serialized objects on purpose: the whole
 * point of a log is that a human can open it and read it, during the defence,
 * without running the program.
 * </p>
 * <p>
 * <b>Thread safety and the size of the locked area:</b> several client threads
 * write log lines at the same moment. Instead of one lock for the whole class,
 * this writer keeps a separate lock object per {@link LogCategory}. A thread
 * recording a sale and a thread recording a new employee therefore never wait
 * for one another - they touch different files and hold different locks. Only
 * two threads writing to the <b>same</b> file are serialized, which is the
 * minimum needed to keep a file from being interleaved. That is exactly the
 * principle of locking the smallest area that still keeps the data correct.
 * </p>
 */
public class LogWriter {

    /** One lock object per category, so different files never block each other. */
    private final Map<LogCategory, Object> categoryLocks = new EnumMap<>(LogCategory.class);

    /**
     * Creates the writer and prepares one lock per log category.
     */
    public LogWriter() {
        StoragePaths.createDirectoriesIfMissing();
        for (LogCategory category : LogCategory.values()) {
            categoryLocks.put(category, new Object());
        }
    }

    /**
     * Appends one entry to the log file of its category.
     * <p>
     * A failure to write a log line must never stop a business action: if the
     * disk is full, a shirt should still be sold. The failure is therefore
     * reported to the console and swallowed, and this method declares no
     * checked exception - which is a deliberate exception to the usual rule of
     * this project.
     * </p>
     *
     * @param entry the entry to append, must not be {@code null}
     */
    public void write(LogEntry entry) {
        LogCategory category = entry.getCategory();
        File logFile = new File(StoragePaths.LOGS_DIRECTORY, category.getFileName());

        synchronized (categoryLocks.get(category)) {
            // The second argument of the FileOutputStream turns on append mode, so
            // the file keeps growing instead of being replaced on every line. The
            // encoding is stated explicitly as UTF-8, because the default encoding
            // of the machine would turn Hebrew customer names into question marks.
            try (PrintWriter fileWriter = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile, true), StandardCharsets.UTF_8))) {
                fileWriter.println(entry.toLogLine());
            } catch (IOException writeFailure) {
                System.err.println("[LogWriter] failed to write to " + logFile.getPath()
                        + ": " + writeFailure.getMessage());
            }
        }
    }
}
