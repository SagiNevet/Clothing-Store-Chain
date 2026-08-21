package common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Central read only access to the settings of the system, loaded once from
 * {@code config.properties}.
 * <p>
 * This class is a <b>Singleton</b>: there is exactly one configuration in a
 * running program, and every part of the code reads the same values. The
 * instance is created eagerly in a {@code static final} field, which is the
 * simplest thread safe way to write a singleton in Java - the class loader
 * guarantees that a static initializer runs once and only once, even when
 * several threads touch the class at the same moment. No {@code synchronized}
 * block and no double checked locking are needed here.
 * </p>
 * <p>
 * Every getter receives a default value, so the whole system still starts
 * correctly when the configuration file is missing. That matters for the unit
 * tests, which run without any file next to them.
 * </p>
 */
public final class AppConfig {

    /** The name of the configuration file, looked up in the working directory. */
    private static final String CONFIG_FILE_NAME = "config.properties";

    /**
     * The single instance. Created when the class is first loaded, which the
     * Java class loader performs exactly once in a thread safe manner.
     */
    private static final AppConfig INSTANCE = new AppConfig();

    /** The settings that were loaded, or an empty set when no file was found. */
    private final Properties settings;

    /**
     * Loads the configuration file if it exists. Private, so that nobody can
     * create a second configuration.
     */
    private AppConfig() {
        this.settings = new Properties();
        File configurationFile = new File(CONFIG_FILE_NAME);
        if (!configurationFile.exists()) {
            System.out.println("[AppConfig] " + CONFIG_FILE_NAME
                    + " not found, using built in default values");
            return;
        }
        try (InputStream fileStream = new FileInputStream(configurationFile)) {
            settings.load(fileStream);
            System.out.println("[AppConfig] loaded " + settings.size()
                    + " settings from " + CONFIG_FILE_NAME);
        } catch (IOException failureToReadFile) {
            // The configuration is optional by design, so a read failure must not
            // stop the program. It is reported and the default values are used.
            System.err.println("[AppConfig] failed to read " + CONFIG_FILE_NAME
                    + ", using default values: " + failureToReadFile.getMessage());
        }
    }

    /**
     * Returns the single configuration instance.
     *
     * @return the singleton instance, never {@code null}
     */
    public static AppConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Reads a text setting.
     *
     * @param key          the name of the setting
     * @param defaultValue the value to return when the setting is missing
     * @return the configured value, or {@code defaultValue} when it is missing
     */
    public String getString(String key, String defaultValue) {
        String rawValue = settings.getProperty(key);
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return defaultValue;
        }
        return rawValue.trim();
    }

    /**
     * Reads a whole number setting.
     *
     * @param key          the name of the setting
     * @param defaultValue the value to return when the setting is missing or
     *                     cannot be parsed as a number
     * @return the configured value, or {@code defaultValue}
     */
    public int getInt(String key, int defaultValue) {
        String rawValue = getString(key, null);
        if (rawValue == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException invalidNumber) {
            System.err.println("[AppConfig] setting " + key + " is not a whole number: "
                    + rawValue + ", using " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Reads a decimal number setting.
     *
     * @param key          the name of the setting
     * @param defaultValue the value to return when the setting is missing or
     *                     cannot be parsed as a number
     * @return the configured value, or {@code defaultValue}
     */
    public double getDouble(String key, double defaultValue) {
        String rawValue = getString(key, null);
        if (rawValue == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException invalidNumber) {
            System.err.println("[AppConfig] setting " + key + " is not a number: "
                    + rawValue + ", using " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Reads a yes or no setting, such as the flag that decides whether the
     * content of chat conversations is written to the log.
     *
     * @param key          the name of the setting
     * @param defaultValue the value to return when the setting is missing
     * @return the configured value, or {@code defaultValue}
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String rawValue = getString(key, null);
        if (rawValue == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(rawValue);
    }
}
