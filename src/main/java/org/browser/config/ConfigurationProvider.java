package org.browser.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.exception.ConfigFileNotFoundException;
import org.browser.automation.exception.ConfigKeyNotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code ConfigurationProvider} class is responsible for loading and providing
 * application configurations using the Typesafe Config library.
 * <br>
 * This class is designed as a Singleton to ensure only one instance exists for each
 * configuration file or custom configuration throughout the application lifecycle,
 * providing efficient and thread-safe access to configurations.
 *
 * <p>The class offers flexibility by allowing configurations to be loaded from standard
 * files (e.g., application.conf) or from custom {@code Config} objects. Additionally,
 * it provides enhanced error handling, logging, and cache management functionalities.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Thread-safe Singleton design pattern for managing configuration instances.</li>
 *   <li>Support for loading configurations from multiple sources (file-based or custom).</li>
 *   <li>Detailed logging and error handling during configuration loading.</li>
 *   <li>Methods to manage and clear the configuration cache dynamically.</li>
 * </ul>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Load configuration from a file
 * ConfigurationProvider configProvider = ConfigurationProvider.getInstance("application");
 * Config config = configProvider.getConfig();
 *
 * // Load a custom configuration
 * Config customConfig = ConfigFactory.parseString("custom.setting = true");
 * ConfigurationProvider customProvider = ConfigurationProvider.getInstance("customKey", customConfig);
 *
 * // Clear the entire cache
 * ConfigurationProvider.clearCache();
 *
 * // Remove a specific configuration from the cache
 * ConfigurationProvider.removeFromCache("application");
 * }</pre>
 */
@Slf4j
@Getter
public class ConfigurationProvider {

    private static final Map<String, ConfigurationProvider> instances = new ConcurrentHashMap<>();
    private final Config config;

    // Private constructor to load a configuration file by name
    @SneakyThrows
    private ConfigurationProvider(String configFileName) {
        this.config = loadConfig(configFileName);
    }

    // Private constructor to prevent external instantiation with a custom Config object
    private ConfigurationProvider(Config config) {
        this.config = config;
        log.info("Custom configuration loaded successfully.");
    }

    /**
     * Retrieves the Singleton instance of {@code ConfigurationProvider} for a given configuration file.
     * The instance is lazily loaded and thread-safe.
     *
     * @param configFileName the name of the configuration file (without the ".conf" extension).
     * @return the Singleton instance of {@code ConfigurationProvider} for the specified configuration file.
     */
    public static ConfigurationProvider getInstance(String configFileName) {
        return instances.computeIfAbsent(configFileName, ConfigurationProvider::new);
    }

    /**
     * Retrieves the Singleton instance of {@code ConfigurationProvider} for a custom {@code Config} object.
     * The instance is lazily loaded and thread-safe.
     *
     * @param customConfigKey a unique key representing the custom configuration instance.
     * @param customConfig    the custom {@code Config} instance to be used.
     * @return the Singleton instance for the specified custom configuration.
     */
    public static ConfigurationProvider getInstance(String customConfigKey, Config customConfig) {
        return instances.computeIfAbsent(customConfigKey, key -> new ConfigurationProvider(customConfig));
    }

    /**
     * Loads the configuration file using the provided file name. Handles potential errors during
     * the loading process and provides detailed logging if an issue occurs.
     *
     * @param configFileName the name of the configuration file (without the ".conf" extension).
     * @return the loaded {@code Config} object.
     * @throws ConfigFileNotFoundException if the configuration file cannot be found or loaded.
     */
    private Config loadConfig(String configFileName) throws ConfigFileNotFoundException {
        try {
            log.info("Attempting to load configuration from file: {}.conf", configFileName);
            return ConfigFactory.load(configFileName);
        } catch (ConfigException.Missing e) {
            log.error("Failed to load configuration from file: {}.conf. Error: {}", configFileName, e.getMessage());
            throw new ConfigFileNotFoundException(configFileName);
        }
    }

    /**
     * Retrieves a configuration value based on the provided key. Throws a custom exception
     * if the key cannot be found in the configuration.
     *
     * @param key the configuration key to be retrieved.
     * @return the configuration value associated with the specified key.
     * @throws ConfigKeyNotFoundException if the key cannot be found in the configuration.
     */
    public String getConfigValue(String key) throws ConfigKeyNotFoundException {
        try {
            return config.getString(key);
        } catch (ConfigException.Missing e) {
            log.error("Failed to retrieve configuration key: {}. Error: {}", key, e.getMessage());
            throw new ConfigKeyNotFoundException(key);
        }
    }

    /**
     * Clears the cached configuration instances. This method can be useful when
     * configurations need to be reloaded dynamically.
     */
    public static void clearCache() {
        log.info("Clearing the configuration cache.");
        instances.clear();
    }

    /**
     * Removes a specific configuration from the cache based on the provided key.
     * This can be used to reload a configuration dynamically if needed.
     *
     * @param configKey the key of the configuration to be removed from the cache.
     */
    public static void removeFromCache(String configKey) {
        if (instances.containsKey(configKey)) {
            log.info("Removing configuration with key: {} from cache.", configKey);
            instances.remove(configKey);
        } else {
            log.warn("Attempted to remove configuration with key: {}, but it was not found in the cache.", configKey);
        }
    }
}