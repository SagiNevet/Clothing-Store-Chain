package common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final String CONFIG_FILE_NAME = "config.properties";

    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties settings;

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

            System.err.println("[AppConfig] failed to read " + CONFIG_FILE_NAME
                    + ", using default values: " + failureToReadFile.getMessage());
        }
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String getString(String key, String defaultValue) {
        String rawValue = settings.getProperty(key);
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return defaultValue;
        }
        return rawValue.trim();
    }

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

    public boolean getBoolean(String key, boolean defaultValue) {
        String rawValue = getString(key, null);
        if (rawValue == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(rawValue);
    }
}
