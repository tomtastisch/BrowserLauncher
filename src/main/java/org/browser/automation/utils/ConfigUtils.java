package org.browser.automation.utils;

import com.typesafe.config.Config;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for configuration operations.
 * <p>
 * The {@code ConfigUtils} class provides utility methods for working with configuration data
 * using the Typesafe Config library. It offers static methods to simplify common configuration
 * tasks, such as retrieving all key-value pairs from a configuration instance.
 * </p>
 * <p>
 * This class is designed to be a stateless utility, providing convenience methods that operate
 * on {@code Config} instances. The methods in this class are static and do not require an instance
 * of the class to be created. This design makes it easy to use the utility methods without
 * needing to instantiate {@code ConfigUtils}.
 * </p>
 * <p>
 * Key Features:
 * <ul>
 *   <li>Static methods for configuration operations, eliminating the need for object instantiation.</li>
 *   <li>Integration with the Typesafe Config library to handle configuration data.</li>
 *   <li>Robust error handling with logging to aid in troubleshooting configuration issues.</li>
 * </ul>
 * </p>
 * <p>
 * Example Usage:
 * <pre>{@code
 * // Load a configuration from a file
 * Config config = ConfigFactory.load("application");
 *
 * // Retrieve all key-value pairs from the configuration
 * Map<String, String> keyValues = ConfigUtils.getKeyValues(config);
 *
 * // Print out all key-value pairs
 * keyValues.forEach((key, value) -> System.out.println(key + ": " + value));
 * }</pre>
 * </p>
 */
@Slf4j
@UtilityClass
public class ConfigUtils {

    /**
     * Retrieves all top-level keys from the provided configuration.
     * <p>
     * This method retrieves the top-level keys directly from the root of the provided {@code Config}.
     * It returns a set containing the names of these top-level keys. This method helps in identifying
     * the primary sections or categories present in the configuration file.
     * </p>
     * <p>
     * For example, given a configuration with keys like "win", "mac", and "nix" for commands or
     * browser paths, this method will return a set containing these keys.
     * </p>
     *
     * @param config the {@code Config} instance from which to retrieve top-level keys.
     * @return a {@code Set<String>} containing all top-level keys from the configuration.
     *         If an error occurs during retrieval, an empty set is returned.
     */
    public static Set<String> getTopLevelKeys(Config config) {
        try {
            // Retrieve all top-level keys from the root of the configuration
            return config.entrySet().stream()
                    .map(Map.Entry::getKey) // Extract the key from the Map.Entry
                    .collect(Collectors.toSet()); // Collect into a Set
        } catch (Exception e) {
            // Log the error and return an empty set if something goes wrong
            log.error("Failed to retrieve top-level keys from the configuration. Error: {}", e.getMessage());
            return Set.of(); // Return an empty set in case of an error
        }
    }
}
