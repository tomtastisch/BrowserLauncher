package org.browser.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the application configuration by loading and providing access to configuration values.
 * This class utilizes the Typesafe Config library for managing configuration settings.
 */
@Slf4j
@UtilityClass
public class BrowserConfig {

    private static final Config CONFIG = ConfigFactory.load();

    /**
     * Retrieves the entire configuration instance.
     *
     * @return the root configuration instance
     */
    public Config getConfig() {
        return CONFIG;
    }

    /**
     * Retrieves a specific configuration section based on the given key.
     *
     * @param key the key for the configuration section
     * @return the configuration section as a Config object
     */
    public Config getConfigSection(String key) {
        try {
            return CONFIG.getConfig(key);
        } catch (Exception e) {
            log.error("Configuration section '{}' could not be found.", key, e);
            return ConfigFactory.empty();
        }
    }

    /**
     * Retrieves a configuration value as a string based on the given key.
     *
     * @param key the key for the configuration value
     * @return the configuration value as a string
     */
    public String getString(String key) {
        try {
            return CONFIG.getString(key);
        } catch (Exception e) {
            log.error("Configuration value for key '{}' could not be found.", key, e);
            return "";
        }
    }

    /**
     * Retrieves a configuration value as an integer based on the given key.
     *
     * @param key the key for the configuration value
     * @return the configuration value as an integer
     */
    public int getInt(String key) {
        try {
            return CONFIG.getInt(key);
        } catch (Exception e) {
            log.error("Configuration value for key '{}' could not be found.", key, e);
            return -1;
        }
    }
}