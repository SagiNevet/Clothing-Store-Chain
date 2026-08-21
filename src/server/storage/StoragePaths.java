package server.storage;

import common.model.Branch;

import java.io.File;

/**
 * The places on disk where the server keeps its files, and the code that
 * creates those folders on the first run.
 * <p>
 * Every path used by the system is defined here and nowhere else. If the whole
 * system had to be moved to a different folder tomorrow, this is the only file
 * that would change.
 * </p>
 * <p>
 * The paths are relative to the folder the server is started from, which is
 * what makes the project run on the laptop of every member of the group
 * without any change.
 * </p>
 */
public final class StoragePaths {

    /**
     * The name of the system property that moves every folder somewhere else.
     * <p>
     * Used by {@code run_tests.bat}, which starts the tests with
     * {@code -Dchainstore.workDir=test-workspace}. Without it the tests would
     * write into the real {@code data} folder, and every run would leave test
     * products and test customers inside the demonstration data - which is
     * exactly what the lecturer would see during the defence.
     * </p>
     */
    private static final String WORK_DIRECTORY_PROPERTY = "chainstore.workDir";

    /** The folder every other folder is created inside, empty for the current one. */
    private static final String WORK_DIRECTORY =
            System.getProperty(WORK_DIRECTORY_PROPERTY, "");

    /** The folder holding the serialized data files. */
    public static final String DATA_DIRECTORY = insideWorkDirectory("data");

    /** The folder holding the text log files. */
    public static final String LOGS_DIRECTORY = insideWorkDirectory("logs");

    /** The folder holding the generated Word reports. */
    public static final String REPORTS_DIRECTORY = insideWorkDirectory("reports");

    /** The file holding every employee account. */
    public static final String EMPLOYEES_FILE = "employees.dat";

    /** The file holding the customer list shared by the whole chain. */
    public static final String CUSTOMERS_FILE = "customers.dat";

    /** The file holding every completed sale. */
    public static final String SALES_FILE = "sales.dat";

    /** The file holding the password policy. */
    public static final String PASSWORD_POLICY_FILE = "password_policy.dat";

    /** The beginning of the name of a per branch inventory file. */
    private static final String INVENTORY_FILE_PREFIX = "inventory_";

    /** The ending of the name of a per branch inventory file. */
    private static final String INVENTORY_FILE_SUFFIX = ".dat";

    /**
     * Prevents instantiation. This class only holds constants and static methods.
     */
    private StoragePaths() {
    }

    /**
     * Places one folder inside the working folder, when one was configured.
     *
     * @param folderName the name of the folder, for example {@code data}
     * @return the folder name, prefixed by the working folder when there is one
     */
    private static String insideWorkDirectory(String folderName) {
        if (WORK_DIRECTORY.isEmpty()) {
            return folderName;
        }
        return WORK_DIRECTORY + File.separator + folderName;
    }

    /**
     * Builds the name of the inventory file of a branch.
     * <p>
     * The name is derived from {@link Branch#name()}, which is why the enum
     * constants must never be renamed once data files exist.
     * </p>
     *
     * @param branch the branch whose inventory file is needed
     * @return the file name, for example {@code inventory_TEL_AVIV.dat}
     */
    public static String inventoryFileFor(Branch branch) {
        return INVENTORY_FILE_PREFIX + branch.name() + INVENTORY_FILE_SUFFIX;
    }

    /**
     * Creates the data, log and report folders if they do not exist yet.
     * <p>
     * Called once when the server starts, so that no other class has to worry
     * about a missing folder.
     * </p>
     */
    public static void createDirectoriesIfMissing() {
        createDirectoryIfMissing(DATA_DIRECTORY);
        createDirectoryIfMissing(LOGS_DIRECTORY);
        createDirectoryIfMissing(REPORTS_DIRECTORY);
    }

    /**
     * Creates one folder if it does not exist yet.
     *
     * @param directoryName the name of the folder to create
     */
    private static void createDirectoryIfMissing(String directoryName) {
        File directory = new File(directoryName);
        if (!directory.exists() && directory.mkdirs()) {
            System.out.println("[Storage] created folder " + directory.getAbsolutePath());
        }
    }
}
