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

public class LogWriter {

    private final Map<LogCategory, Object> categoryLocks = new EnumMap<>(LogCategory.class);

    public LogWriter() {
        StoragePaths.createDirectoriesIfMissing();
        for (LogCategory category : LogCategory.values()) {
            categoryLocks.put(category, new Object());
        }
    }

    public void write(LogEntry entry) {
        LogCategory category = entry.getCategory();
        File logFile = new File(StoragePaths.LOGS_DIRECTORY, category.getFileName());

        synchronized (categoryLocks.get(category)) {

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
