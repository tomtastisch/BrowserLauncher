package org.browser.automation.exception;

import org.browser.automation.exception.base.LocalizedExceptionBase;

/**
 * Exception thrown when a specified configuration file cannot be found.
 * This exception is designed to indicate that the given configuration file
 * was expected to be loaded but could not be located.
 */
public class ConfigFileNotFoundException extends LocalizedExceptionBase {

    /**
     * Constructs a new {@code ConfigFileNotFoundException} with a detailed message
     * indicating that the specified configuration file could not be found.
     *
     * @param configFileName the name of the configuration file that could not be found.
     */
    public ConfigFileNotFoundException(String configFileName) {
        super("exception.config.file.not.found", configFileName);
    }

    /**
     * Constructs a new {@code ConfigFileNotFoundException} with a custom message
     * and the specified configuration file name that could not be found.
     *
     * @param configFileName the name of the configuration file that could not be found.
     * @param customMessage  a custom message explaining the cause of the exception.
     */
    public ConfigFileNotFoundException(String configFileName, String customMessage) {
        super(customMessage, "exception.config.file.not.found.custom", configFileName);
    }
}