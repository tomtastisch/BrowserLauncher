package org.browser.automation.exception.config;

import org.browser.automation.exception.base.LocalizedExceptionBase;

/**
 * Exception thrown when a specified configuration key is not found in the configuration file.
 * This exception is designed to indicate that the given configuration key was expected
 * but could not be found during the retrieval process.
 */
public class ConfigKeyNotFoundException extends LocalizedExceptionBase {

    /**
     * Constructs a new {@code ConfigKeyNotFoundException} with a detailed message
     * indicating that the specified configuration key could not be found.
     *
     * @param configKey the configuration key that could not be found.
     */
    public ConfigKeyNotFoundException(String configKey) {
        super("exception.config.key.not.found", configKey);
    }

    /**
     * Constructs a new {@code ConfigKeyNotFoundException} with a custom message
     * and the specified configuration key that could not be found.
     *
     * @param configKey     the configuration key that could not be found.
     * @param customMessage a custom message explaining the cause of the exception.
     */
    public ConfigKeyNotFoundException(String configKey, String customMessage) {
        super(customMessage, "exception.config.key.not.found.custom", configKey);
    }
}