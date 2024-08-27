package org.browser.automation.exception.base;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Abstract base class for localized exceptions that leverage ResourceBundle and MessageFormat.
 * This class handles loading messages from a properties file and formatting them based on provided arguments.
 *
 * <p>The ResourceBundle is loaded only once and shared across all subclasses, improving efficiency
 * when multiple exceptions are thrown throughout the application.
 */
public abstract class LocalizedExceptionBase extends Exception {

    // Load the ResourceBundle only once and reuse it across all subclasses.
    private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("context.messages");

    /**
     * Constructs a new {@code LocalizedExceptionBase} with a formatted message based on the specified key and arguments.
     *
     * @param messageKey the key for the message in the properties file.
     * @param args       the arguments to format the message with.
     */
    public LocalizedExceptionBase(String messageKey, Object... args) {
        super(MessageFormat.format(MESSAGES.getString(messageKey), args));
    }

    /**
     * Constructs a new {@code LocalizedExceptionBase} with a custom message and the specified key and arguments.
     *
     * @param customMessage the custom message to use.
     * @param messageKey    the key for the message in the properties file.
     * @param args          the arguments to format the message with.
     */
    public LocalizedExceptionBase(String customMessage, String messageKey, Object... args) {
        super(MessageFormat.format(customMessage, args));
    }
}