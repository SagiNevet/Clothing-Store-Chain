package server.service;

import common.model.Branch;
import common.model.LogCategory;
import common.model.LogEntry;
import common.util.AppConfig;
import server.storage.LogWriter;

public final class LogManager {

    private static final LogManager INSTANCE = new LogManager();

    private static final String CONFIG_KEY_SAVE_CHAT_CONTENT = "chat.saveMessageContent";

    private static final boolean DEFAULT_SAVE_CHAT_CONTENT = false;

    private static final String CONTENT_HIDDEN = "(message content not saved)";

    private final LogWriter logWriter = new LogWriter();

    private LogManager() {
    }

    public static LogManager getInstance() {
        return INSTANCE;
    }

    public void logEmployeeAction(String actorEmployeeNumber, Branch branch,
                                  String action, String details) {
        logWriter.write(new LogEntry(LogCategory.EMPLOYEES, actorEmployeeNumber,
                branch, action, details));
    }

    public void logCustomerAction(String actorEmployeeNumber, Branch branch,
                                  String action, String details) {
        logWriter.write(new LogEntry(LogCategory.CUSTOMERS, actorEmployeeNumber,
                branch, action, details));
    }

    public void logSalesAction(String actorEmployeeNumber, Branch branch,
                               String action, String details) {
        logWriter.write(new LogEntry(LogCategory.SALES, actorEmployeeNumber,
                branch, action, details));
    }

    public void logChatAction(String actorEmployeeNumber, Branch branch, String action,
                              String participants, String messageContent) {
        String details = participants;
        if (messageContent != null) {
            details = details + " -> " + (isChatContentSaved()
                    ? messageContent
                    : CONTENT_HIDDEN);
        }
        logWriter.write(new LogEntry(LogCategory.CHAT, actorEmployeeNumber,
                branch, action, details));
    }

    public boolean isChatContentSaved() {
        return AppConfig.getInstance().getBoolean(
                CONFIG_KEY_SAVE_CHAT_CONTENT, DEFAULT_SAVE_CHAT_CONTENT);
    }
}
