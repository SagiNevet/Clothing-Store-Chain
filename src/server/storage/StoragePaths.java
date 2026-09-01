package server.storage;

import common.model.Branch;

import java.io.File;

public final class StoragePaths {

    private static final String WORK_DIRECTORY_PROPERTY = "chainstore.workDir";

    private static final String WORK_DIRECTORY =
            System.getProperty(WORK_DIRECTORY_PROPERTY, "");

    public static final String DATA_DIRECTORY = insideWorkDirectory("data");

    public static final String LOGS_DIRECTORY = insideWorkDirectory("logs");

    public static final String REPORTS_DIRECTORY = insideWorkDirectory("reports");

    public static final String EMPLOYEES_FILE = "employees.dat";

    public static final String CUSTOMERS_FILE = "customers.dat";

    public static final String SALES_FILE = "sales.dat";

    public static final String PASSWORD_POLICY_FILE = "password_policy.dat";

    private static final String INVENTORY_FILE_PREFIX = "inventory_";

    private static final String INVENTORY_FILE_SUFFIX = ".dat";

    private StoragePaths() {
    }

    private static String insideWorkDirectory(String folderName) {
        if (WORK_DIRECTORY.isEmpty()) {
            return folderName;
        }
        return WORK_DIRECTORY + File.separator + folderName;
    }

    public static String inventoryFileFor(Branch branch) {
        return INVENTORY_FILE_PREFIX + branch.name() + INVENTORY_FILE_SUFFIX;
    }

    public static void createDirectoriesIfMissing() {
        createDirectoryIfMissing(DATA_DIRECTORY);
        createDirectoryIfMissing(LOGS_DIRECTORY);
        createDirectoryIfMissing(REPORTS_DIRECTORY);
    }

    private static void createDirectoryIfMissing(String directoryName) {
        File directory = new File(directoryName);
        if (!directory.exists() && directory.mkdirs()) {
            System.out.println("[Storage] created folder " + directory.getAbsolutePath());
        }
    }
}
